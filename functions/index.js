/**
 * TrustPay - Firebase Cloud Functions
 * Zero APK Secrets Architecture: All HMAC signing, nonce generation,
 * and double-spend transaction locks are executed strictly on the server.
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();

// Server-only signing secret (never exposed to Android client or APK)
const QR_SIGNING_SECRET = process.env.QR_SIGNING_SECRET || "trustpay_server_hmac_secret_2026_isolated";

/**
 * Computes an HMAC-SHA256 signature over the token payload.
 */
function computeTokenSignature(tokenId, receiverId, amount, nonce, expiresAt) {
  const payload = `${tokenId}|${receiverId}|${amount != null ? Number(amount).toFixed(2) : "NONE"}|${nonce}|${expiresAt}`;
  return crypto.createHmac("sha256", QR_SIGNING_SECRET).update(payload).digest("hex");
}

/**
 * Cloud Function 1: createQrPaymentRequest
 * Generates a rotating 30-second QR payment request.
 */
exports.createQrPaymentRequest = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
  }

  const receiverId = context.auth.uid;
  const receiverName = data.receiverName || context.auth.token.name || context.auth.token.email || "Receiver";
  const amount = data.amount != null && !isNaN(data.amount) ? Number(data.amount) : null;
  const note = String(data.note || "").trim();

  const tokenId = "tp_qr_" + crypto.randomBytes(12).toString("hex");
  const nonce = crypto.randomBytes(16).toString("hex");
  const now = Date.now();
  const expiresAt = now + 30 * 1000; // 30-second TTL window

  const signature = computeTokenSignature(tokenId, receiverId, amount, nonce, expiresAt);
  const payloadUri = `trustpay://p2p/request?tokenId=${tokenId}&expiresAt=${expiresAt}&version=1`;

  const tokenRecord = {
    tokenId,
    receiverId,
    receiverName,
    amount,
    note,
    nonce,
    createdAt: now,
    expiresAt,
    status: "ACTIVE",
    signature,
    payloadUri
  };

  await db.collection("p2p_qr_tokens").doc(tokenId).set(tokenRecord);

  // Update admin metrics
  await db.collection("system_metrics").doc("qr_stats").set({
    totalGenerated: admin.firestore.FieldValue.increment(1),
    lastUpdated: now
  }, { merge: true });

  return {
    success: true,
    token: tokenRecord,
    ttlSeconds: 30
  };
});

/**
 * Cloud Function 2: validateQrToken
 * Verifies freshness, single-use state, and server signature.
 */
exports.validateQrToken = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
  }

  const tokenId = data.tokenId;
  if (!tokenId) {
    return { valid: false, error: "Missing tokenId parameter.", errorCode: "INVALID_PARAM" };
  }

  const tokenDoc = await db.collection("p2p_qr_tokens").doc(tokenId).get();
  if (!tokenDoc.exists) {
    return { valid: false, error: "QR token does not exist or was purged.", errorCode: "NOT_FOUND" };
  }

  const token = tokenDoc.data();
  const now = Date.now();

  // 1. Double-spend check
  if (token.status === "USED") {
    await db.collection("system_metrics").doc("qr_stats").set({
      replayBlockedCount: admin.firestore.FieldValue.increment(1)
    }, { merge: true });

    return {
      valid: false,
      error: "QR already used.",
      errorCode: "ALREADY_USED",
      request: token
    };
  }

  // 2. Freshness check (30-second window)
  if (now > token.expiresAt) {
    await tokenDoc.ref.update({ status: "EXPIRED" });
    return {
      valid: false,
      error: "QR has expired (30s window passed).",
      errorCode: "EXPIRED",
      request: token
    };
  }

  // 3. Cryptographic signature check
  const expectedSig = computeTokenSignature(token.tokenId, token.receiverId, token.amount, token.nonce, token.expiresAt);
  if (token.signature !== expectedSig) {
    await db.collection("system_metrics").doc("qr_stats").set({
      tamperBlockedCount: admin.firestore.FieldValue.increment(1)
    }, { merge: true });

    return {
      valid: false,
      error: "QR code signature verification failed. Possible payload tampering.",
      errorCode: "TAMPER_DETECTED"
    };
  }

  const remainingSeconds = Math.max(0, Math.floor((token.expiresAt - now) / 1000));
  return {
    valid: true,
    request: token,
    remainingSeconds
  };
});

/**
 * Cloud Function 3: completeP2PTransfer
 * Executes atomic Firestore transaction:
 * - Locks and marks token as USED
 * - Mutates sender and receiver balances
 * - Records transaction receipt and immutable audit log
 */
exports.completeP2PTransfer = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
  }

  const senderId = context.auth.uid;
  const senderName = data.senderName || context.auth.token.name || context.auth.token.email || "Sender";
  const tokenId = data.tokenId;
  const transferAmount = Number(data.amount);
  const bindingHash = data.bindingHash || "";

  if (!tokenId || isNaN(transferAmount) || transferAmount <= 0) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid transfer parameters.");
  }

  const tokenRef = db.collection("p2p_qr_tokens").doc(tokenId);
  const senderRef = db.collection("users").doc(senderId);

  const result = await db.runTransaction(async (transaction) => {
    const tokenDoc = await transaction.get(tokenRef);
    if (!tokenDoc.exists) {
      throw new functions.https.HttpsError("not-found", "QR token not found.");
    }

    const token = tokenDoc.data();
    const now = Date.now();

    // Prevent paying own QR
    if (token.receiverId === senderId) {
      throw new functions.https.HttpsError("failed-precondition", "Cannot pay your own payment request.");
    }

    // Atomic double-spend check
    if (token.status === "USED") {
      throw new functions.https.HttpsError("already-exists", "QR already used.");
    }

    // TTL check
    if (now > token.expiresAt) {
      transaction.update(tokenRef, { status: "EXPIRED" });
      throw new functions.https.HttpsError("deadline-exceeded", "QR has expired (30s window passed).");
    }

    // Amount match check if fixed amount was specified
    if (token.amount != null && Math.abs(token.amount - transferAmount) > 0.01) {
      throw new functions.https.HttpsError("invalid-argument", `Amount must match requested amount: ${token.amount}`);
    }

    const receiverRef = db.collection("users").doc(token.receiverId);
    const senderDoc = await transaction.get(senderRef);
    const receiverDoc = await transaction.get(receiverRef);

    const senderBalance = senderDoc.exists ? (senderDoc.data().balance || 100000.0) : 100000.0;
    const receiverBalance = receiverDoc.exists ? (receiverDoc.data().balance || 100000.0) : 100000.0;

    if (senderBalance < transferAmount) {
      throw new functions.https.HttpsError("resource-exhausted", "Insufficient account balance.");
    }

    const txId = "TP_TX_" + now + "_" + Math.floor(Math.random() * 10000);

    // 1. Mark token as USED
    transaction.update(tokenRef, {
      status: "USED",
      usedBySenderId: senderId,
      usedBySenderName: senderName,
      usedAt: now,
      transactionId: txId
    });

    // 2. Atomic balance mutation
    transaction.set(senderRef, { balance: senderBalance - transferAmount }, { merge: true });
    transaction.set(receiverRef, { balance: receiverBalance + transferAmount }, { merge: true });

    // 3. Append to transactions collection
    const txRecord = {
      id: txId,
      timestamp: now,
      senderId,
      senderName,
      receiverId: token.receiverId,
      receiverName: token.receiverName,
      amount: transferAmount,
      currency: "INR",
      status: "APPROVED",
      bindingHash,
      note: token.note || "",
      qrTokenId: tokenId,
      method: "P2P_QR"
    };
    transaction.set(db.collection("transactions").doc(txId), txRecord);

    // 4. Update aggregate statistics
    const statsRef = db.collection("system_metrics").doc("qr_stats");
    transaction.set(statsRef, {
      usedCount: admin.firestore.FieldValue.increment(1),
      totalTransferredAmount: admin.firestore.FieldValue.increment(transferAmount)
    }, { merge: true });

    return {
      success: true,
      transactionId: txId,
      receiverName: token.receiverName,
      amount: transferAmount,
      timestamp: now,
      status: "APPROVED",
      bindingHash
    };
  });

  return result;
});
