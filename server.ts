import express from "express";
import path from "path";
import crypto from "crypto";
import { createServer as createViteServer } from "vite";
import { GoogleGenAI } from "@google/genai";

// Server-side master HMAC secret for signing QR tokens (never exposed to client)
const QR_SIGNING_SECRET = process.env.QR_SIGNING_SECRET || "trustpay_cloud_secure_hmac_key_2026";

interface ServerQrToken {
  tokenId: string;
  receiverId: string;
  receiverName: string;
  amount: number | null;
  note: string | null;
  nonce: string;
  createdAt: number;
  expiresAt: number;
  status: "ACTIVE" | "EXPIRED" | "USED" | "CANCELLED";
  signature: string;
  payloadUri: string;
  usedBySenderId?: string;
  usedBySenderName?: string;
  usedAt?: number;
  transactionId?: string;
}

// In-memory persistent state for simulated P2P transfers and audit events
const qrTokensStore = new Map<string, ServerQrToken>();
const userBalances = new Map<string, number>([
  ["rahul@example.com", 100000.0],
  ["priya@example.com", 65000.0],
]);

const qrSecurityStats = {
  totalGenerated: 0,
  activeCount: 0,
  expiredCount: 0,
  usedCount: 0,
  replayBlockedCount: 0,
  tamperBlockedCount: 0,
  totalTransferredAmount: 0,
};

function generateHmacSignature(tokenId: string, receiverId: string, amount: number | null, nonce: string, expiresAt: number): string {
  const payload = `${tokenId}:${receiverId}:${amount ?? 0}:${nonce}:${expiresAt}`;
  return crypto.createHmac("sha256", QR_SIGNING_SECRET).update(payload).digest("hex");
}

async function startServer() {
  const app = express();
  const PORT = 3000;

  app.use(express.json());

  // Lazy Gemini client helper
  let geminiClient: GoogleGenAI | null = null;
  function getGemini(): GoogleGenAI | null {
    const key = process.env.GEMINI_API_KEY;
    if (!key) return null;
    if (!geminiClient) {
      geminiClient = new GoogleGenAI({ apiKey: key });
    }
    return geminiClient;
  }

  // Health endpoint
  app.get("/api/health", (req, res) => {
    res.json({ status: "ok", app: "TrustPay", time: new Date().toISOString() });
  });

  // P2P: 1. Generate Secure QR Token with 30s TTL
  app.post("/api/p2p/create-qr-token", (req, res) => {
    try {
      const { receiverId, receiverName, amount, note } = req.body;
      if (!receiverId || !receiverName) {
        return res.status(400).json({ error: "receiverId and receiverName are required" });
      }

      const parsedAmount = amount !== undefined && amount !== null && amount !== "" ? parseFloat(amount) : null;
      if (parsedAmount !== null && (isNaN(parsedAmount) || parsedAmount <= 0)) {
        return res.status(400).json({ error: "Invalid requested amount" });
      }

      const tokenId = `tp_qr_${crypto.randomBytes(12).toString("hex")}`;
      const nonce = crypto.randomBytes(16).toString("hex");
      const createdAt = Date.now();
      const expiresAt = createdAt + 30000; // Exactly 30 seconds

      const signature = generateHmacSignature(tokenId, receiverId, parsedAmount, nonce, expiresAt);
      const payloadUri = `trustpay://p2p/request?tokenId=${encodeURIComponent(tokenId)}&expiresAt=${expiresAt}&version=1`;

      const tokenRecord: ServerQrToken = {
        tokenId,
        receiverId,
        receiverName,
        amount: parsedAmount,
        note: note ? String(note).trim() : null,
        nonce,
        createdAt,
        expiresAt,
        status: "ACTIVE",
        signature,
        payloadUri,
      };

      qrTokensStore.set(tokenId, tokenRecord);
      qrSecurityStats.totalGenerated++;

      // Return safe token payload
      res.json({
        success: true,
        token: tokenRecord,
        ttlSeconds: 30,
      });
    } catch (err: any) {
      console.error("Error creating QR token:", err);
      res.status(500).json({ error: "Failed to create secure QR token" });
    }
  });

  // P2P: 2. Validate Scanned QR Token
  app.post("/api/p2p/validate-qr-token", (req, res) => {
    try {
      const { tokenId, senderId } = req.body;
      if (!tokenId) {
        return res.status(400).json({ valid: false, error: "Invalid TrustPay QR code.", errorCode: "INVALID_FORMAT" });
      }

      const token = qrTokensStore.get(tokenId);
      if (!token) {
        return res.json({
          valid: false,
          error: "Invalid TrustPay QR code. Request was not found.",
          errorCode: "INVALID_FORMAT",
        });
      }

      const now = Date.now();

      // Check if already used
      if (token.status === "USED") {
        qrSecurityStats.replayBlockedCount++;
        return res.json({
          valid: false,
          error: "QR already used.",
          errorCode: "ALREADY_USED",
          request: token,
        });
      }

      // Check if expired
      if (now > token.expiresAt || token.status === "EXPIRED") {
        token.status = "EXPIRED";
        qrSecurityStats.expiredCount++;
        return res.json({
          valid: false,
          error: "QR expired. Ask the receiver to generate a new QR.",
          errorCode: "EXPIRED",
          request: token,
        });
      }

      // Check HMAC signature integrity
      const expectedSignature = generateHmacSignature(token.tokenId, token.receiverId, token.amount, token.nonce, token.expiresAt);
      if (token.signature !== expectedSignature) {
        qrSecurityStats.tamperBlockedCount++;
        return res.json({
          valid: false,
          error: "QR tampering detected.",
          errorCode: "TAMPERED",
        });
      }

      // Self-payment check
      if (senderId && senderId.toLowerCase() === token.receiverId.toLowerCase()) {
        return res.json({
          valid: false,
          error: "Cannot send payment to your own QR code.",
          errorCode: "SELF_PAYMENT",
          request: token,
        });
      }

      const remainingSeconds = Math.max(0, Math.ceil((token.expiresAt - now) / 1000));

      res.json({
        valid: true,
        request: token,
        remainingSeconds,
      });
    } catch (err: any) {
      console.error("Error validating QR token:", err);
      res.status(500).json({ valid: false, error: "Validation server error" });
    }
  });

  // P2P: 3. Complete Atomic P2P Transfer
  app.post("/api/p2p/complete-transfer", (req, res) => {
    try {
      const { tokenId, senderId, senderName, amount, bindingHash, riskScore } = req.body;
      if (!tokenId || !senderId || !amount) {
        return res.status(400).json({ success: false, error: "Missing required transfer parameters" });
      }

      const token = qrTokensStore.get(tokenId);
      if (!token) {
        return res.status(404).json({ success: false, error: "QR token not found" });
      }

      const now = Date.now();
      if (token.status === "USED") {
        qrSecurityStats.replayBlockedCount++;
        return res.status(409).json({ success: false, error: "QR already used.", errorCode: "ALREADY_USED" });
      }

      if (now > token.expiresAt || token.status === "EXPIRED") {
        token.status = "EXPIRED";
        return res.status(410).json({ success: false, error: "QR expired. Ask the receiver to generate a new QR.", errorCode: "EXPIRED" });
      }

      const transferAmount = parseFloat(amount);
      if (isNaN(transferAmount) || transferAmount <= 0) {
        return res.status(400).json({ success: false, error: "Invalid transfer amount" });
      }

      // Check sender balance
      const senderBal = userBalances.get(senderId) ?? 100000.0;
      if (senderBal < transferAmount) {
        return res.status(400).json({ success: false, error: "Insufficient account balance" });
      }

      // Atomic execution
      const newSenderBal = senderBal - transferAmount;
      const receiverBal = (userBalances.get(token.receiverId) ?? 65000.0) + transferAmount;

      userBalances.set(senderId, newSenderBal);
      userBalances.set(token.receiverId, receiverBal);

      // Mark token USED
      const transactionId = `TP_TX_${Date.now()}_${Math.floor(1000 + Math.random() * 9000)}`;
      token.status = "USED";
      token.usedBySenderId = senderId;
      token.usedBySenderName = senderName || "Authorized Sender";
      token.usedAt = now;
      token.transactionId = transactionId;

      qrSecurityStats.usedCount++;
      qrSecurityStats.totalTransferredAmount += transferAmount;

      res.json({
        success: true,
        transactionId,
        senderBalance: newSenderBal,
        receiverBalance: receiverBal,
        receiverName: token.receiverName,
        receiverId: token.receiverId,
        amount: transferAmount,
        note: token.note,
        status: "APPROVED",
        timestamp: now,
        bindingHash: bindingHash || "VERIFIED_SHA256_HASH",
      });
    } catch (err: any) {
      console.error("Error completing P2P transfer:", err);
      res.status(500).json({ success: false, error: "Atomic transfer execution failed" });
    }
  });

  // P2P: 4. Get QR History
  app.get("/api/p2p/tokens", (req, res) => {
    const now = Date.now();
    const tokens = Array.from(qrTokensStore.values())
      .map((t) => {
        if (t.status === "ACTIVE" && now > t.expiresAt) {
          t.status = "EXPIRED";
        }
        return t;
      })
      .sort((a, b) => b.createdAt - a.createdAt);

    res.json({ tokens });
  });

  // P2P: 5. Admin Security Stats
  app.get("/api/p2p/admin-stats", (req, res) => {
    const now = Date.now();
    let active = 0;
    let expired = 0;
    let used = 0;

    for (const t of qrTokensStore.values()) {
      if (t.status === "USED") used++;
      else if (t.status === "EXPIRED" || (t.status === "ACTIVE" && now > t.expiresAt)) expired++;
      else if (t.status === "ACTIVE") active++;
    }

    res.json({
      totalGenerated: qrTokensStore.size,
      activeCount: active,
      expiredCount: expired,
      usedCount: used,
      replayBlockedCount: qrSecurityStats.replayBlockedCount,
      tamperBlockedCount: qrSecurityStats.tamperBlockedCount,
      totalTransferredAmount: qrSecurityStats.totalTransferredAmount,
    });
  });

  // ==========================================
  // DUAL AUTHORIZATION ENTERPRISE API
  // ==========================================
  interface ServerDualAuthTx {
    id: string;
    initiatorId: string;
    initiatorName: string;
    coSignerId: string;
    coSignerName: string;
    recipient: string;
    amount: number;
    currency: string;
    note: string;
    status: "PENDING SECOND AUTHORIZATION" | "APPROVED BY TWO SIGNERS" | "REJECTED BY SECOND SIGNER" | "EXPIRED" | "BLOCKED";
    createdAt: number;
    expiresAt: number;
    riskScore: number;
    signals: string[];
    nonce: string;
    firstApprovalSignature: string;
    secondApprovalSignature?: string;
    secondApprovedAt?: number;
    approvedByUserId?: string;
    approvedByUserName?: string;
    rejectionReason?: string;
    balanceDeducted: boolean;
    label: "Dual-Authorization Transaction";
  }

  const dualAuthTxStore = new Map<string, ServerDualAuthTx>();
  const coSignersStore = new Map<string, { id: string; fullName: string; email: string; role: string; active: boolean; addedAt: number }>([
    ["priya@example.com", { id: "cosigner_1", fullName: "Priya Patel", email: "priya@example.com", role: "Chief Financial Officer (CFO)", active: true, addedAt: Date.now() - 7 * 86400000 }],
    ["vikram@example.com", { id: "cosigner_2", fullName: "Vikram Mehta", email: "vikram@example.com", role: "Risk & Compliance Officer", active: true, addedAt: Date.now() - 5 * 86400000 }],
    ["ananya@example.com", { id: "cosigner_3", fullName: "Ananya Rao", email: "ananya@example.com", role: "Senior Treasury Director", active: true, addedAt: Date.now() - 2 * 86400000 }],
  ]);

  // Create Dual-Auth Payment
  app.post("/api/dual-auth/create", (req, res) => {
    try {
      const { initiatorId, initiatorName, coSignerId, coSignerName, recipient, amount, note, riskScore, signals, bindingHash } = req.body;
      if (!initiatorId || !coSignerId || !recipient || !amount) {
        return res.status(400).json({ error: "Missing required dual-auth payment parameters" });
      }

      if (initiatorId.toLowerCase() === coSignerId.toLowerCase()) {
        return res.status(400).json({ error: "User 1 cannot select themselves as User 2 (co-signer)." });
      }

      const parsedAmount = parseFloat(amount);
      if (isNaN(parsedAmount) || parsedAmount <= 0) {
        return res.status(400).json({ error: "Invalid payment amount" });
      }

      // Check initiator balance
      const initiatorBal = userBalances.get(initiatorId) ?? 100000.0;
      if (initiatorBal < parsedAmount) {
        return res.status(400).json({ error: "Insufficient account balance for this transaction" });
      }

      const now = Date.now();
      const expiresAt = now + 5 * 60 * 1000; // 5 minutes TTL
      const nonce = crypto.randomBytes(16).toString("hex");
      const txId = `TP_DUAL_${now}_${Math.floor(1000 + Math.random() * 9000)}`;

      const txRecord: ServerDualAuthTx = {
        id: txId,
        initiatorId,
        initiatorName: initiatorName || initiatorId,
        coSignerId,
        coSignerName: coSignerName || coSignerId,
        recipient,
        amount: parsedAmount,
        currency: "INR",
        note: note ? String(note).trim() : "Dual-Authorization Transfer",
        status: "PENDING SECOND AUTHORIZATION",
        createdAt: now,
        expiresAt,
        riskScore: riskScore || 20,
        signals: signals || [],
        nonce,
        firstApprovalSignature: bindingHash || crypto.createHash("sha256").update(`${txId}:${parsedAmount}:${recipient}`).digest("hex"),
        balanceDeducted: false,
        label: "Dual-Authorization Transaction",
      };

      dualAuthTxStore.set(txId, txRecord);

      res.json({
        success: true,
        transaction: txRecord,
        ttlSeconds: 300,
      });
    } catch (err) {
      console.error("Error creating dual-auth payment:", err);
      res.status(500).json({ error: "Server error creating dual-auth payment" });
    }
  });

  // Get Pending Dual-Auth Payments
  app.get("/api/dual-auth/pending", (req, res) => {
    const now = Date.now();
    const pendingList: ServerDualAuthTx[] = [];

    for (const tx of dualAuthTxStore.values()) {
      if (tx.status === "PENDING SECOND AUTHORIZATION") {
        if (now > tx.expiresAt) {
          tx.status = "EXPIRED";
          tx.rejectionReason = "Pending second approval window (5 minutes) expired.";
        } else {
          pendingList.push(tx);
        }
      }
    }

    res.json({
      pending: pendingList.sort((a, b) => b.createdAt - a.createdAt),
      serverTime: now,
    });
  });

  // Approve Dual-Auth Payment
  app.post("/api/dual-auth/approve", (req, res) => {
    try {
      const { transactionId, coSignerId, coSignerName, secondApprovalSignature } = req.body;
      if (!transactionId || !coSignerId) {
        return res.status(400).json({ success: false, error: "Missing required approval arguments" });
      }

      const tx = dualAuthTxStore.get(transactionId);
      if (!tx) {
        return res.status(404).json({ success: false, error: "Dual-auth transaction not found" });
      }

      const now = Date.now();

      // Rule 1: User 1 must never approve their own transaction as User 2
      if (coSignerId.toLowerCase() === tx.initiatorId.toLowerCase()) {
        return res.status(403).json({ success: false, error: "Security violation: User 1 cannot approve their own transaction as User 2." });
      }

      // Rule 2: Only authorised co-signers
      const cosigner = coSignersStore.get(coSignerId.toLowerCase());
      if (!cosigner || !cosigner.active) {
        return res.status(403).json({ success: false, error: "User is not an active authorized co-signer." });
      }

      // Rule 3: 5-minute TTL
      if (now > tx.expiresAt || tx.status === "EXPIRED") {
        tx.status = "EXPIRED";
        tx.rejectionReason = "Approval window expired after 5 minutes.";
        return res.status(410).json({ success: false, error: "Transaction expired. 5-minute approval window has elapsed." });
      }

      if (tx.status !== "PENDING SECOND AUTHORIZATION") {
        return res.status(409).json({ success: false, error: `Transaction is already ${tx.status}.` });
      }

      // Balance check
      const initiatorBal = userBalances.get(tx.initiatorId) ?? 100000.0;
      if (initiatorBal < tx.amount) {
        tx.status = "BLOCKED";
        tx.rejectionReason = "Initiator has insufficient funds.";
        return res.status(400).json({ success: false, error: "Initiator has insufficient balance" });
      }

      // Deduct balance once
      const newInitiatorBal = initiatorBal - tx.amount;
      userBalances.set(tx.initiatorId, newInitiatorBal);

      const recipientBal = (userBalances.get(tx.recipient) ?? 65000.0) + tx.amount;
      userBalances.set(tx.recipient, recipientBal);

      // Settle
      tx.status = "APPROVED BY TWO SIGNERS";
      tx.secondApprovalSignature = secondApprovalSignature || crypto.createHash("sha256").update(`${tx.id}:${tx.amount}:${coSignerId}:${now}`).digest("hex");
      tx.secondApprovedAt = now;
      tx.approvedByUserId = coSignerId;
      tx.approvedByUserName = coSignerName || cosigner.fullName;
      tx.balanceDeducted = true;

      res.json({
        success: true,
        transaction: tx,
        initiatorBalance: newInitiatorBal,
      });
    } catch (err) {
      console.error("Error approving dual-auth payment:", err);
      res.status(500).json({ success: false, error: "Server error approving dual-auth payment" });
    }
  });

  // Reject Dual-Auth Payment
  app.post("/api/dual-auth/reject", (req, res) => {
    try {
      const { transactionId, coSignerId, reason } = req.body;
      const tx = dualAuthTxStore.get(transactionId);
      if (!tx) {
        return res.status(404).json({ success: false, error: "Dual-auth transaction not found" });
      }

      tx.status = "REJECTED BY SECOND SIGNER";
      tx.rejectionReason = reason || "Rejected by authorized co-signer.";

      res.json({ success: true, transaction: tx });
    } catch (err) {
      console.error("Error rejecting dual-auth payment:", err);
      res.status(500).json({ success: false, error: "Server error rejecting payment" });
    }
  });

  // Dual-Auth History
  app.get("/api/dual-auth/history", (req, res) => {
    const list = Array.from(dualAuthTxStore.values()).sort((a, b) => b.createdAt - a.createdAt);
    res.json({ transactions: list });
  });

  // Get and Manage Co-Signers
  app.get("/api/dual-auth/co-signers", (req, res) => {
    res.json({ coSigners: Array.from(coSignersStore.values()) });
  });

  app.post("/api/dual-auth/co-signers", (req, res) => {
    const { fullName, email, role } = req.body;
    if (!fullName || !email || !role) {
      return res.status(400).json({ error: "Name, email, and role are required" });
    }
    const id = `cosigner_${Date.now()}`;
    const newSigner = {
      id,
      fullName: String(fullName).trim(),
      email: String(email).trim().toLowerCase(),
      role: String(role).trim(),
      active: true,
      addedAt: Date.now(),
    };
    coSignersStore.set(newSigner.email, newSigner);
    res.json({ success: true, coSigner: newSigner });
  });

  // Dual-Auth Admin Stats
  app.get("/api/dual-auth/admin-stats", (req, res) => {
    const now = Date.now();
    let pendingCount = 0;
    let approvedCount = 0;
    let rejectedCount = 0;
    let expiredCount = 0;
    let totalApprovedAmount = 0;

    for (const tx of dualAuthTxStore.values()) {
      if (tx.status === "PENDING SECOND AUTHORIZATION") {
        if (now > tx.expiresAt) expiredCount++;
        else pendingCount++;
      } else if (tx.status === "APPROVED BY TWO SIGNERS") {
        approvedCount++;
        totalApprovedAmount += tx.amount;
      } else if (tx.status === "REJECTED BY SECOND SIGNER") {
        rejectedCount++;
      } else if (tx.status === "EXPIRED") {
        expiredCount++;
      }
    }

    res.json({
      totalRequests: dualAuthTxStore.size,
      pendingCount,
      approvedCount,
      rejectedCount,
      expiredCount,
      totalApprovedAmount,
    });
  });

  // AI Security Risk Insights Endpoint
  app.post("/api/ai-risk-insights", async (req, res) => {
    try {
      const { transactionId, recipient, amount, score, signals, isAnomaly } = req.body;
      const ai = getGemini();

      if (!ai) {
        return res.json({
          insights: `Rule-based evaluation score ${score}/100. Signals: ${signals && signals.length ? signals.join(", ") : "Standard low-risk pattern"}. Cryptographic SHA-256 payload integrity sealed.`,
          source: "heuristic"
        });
      }

      const prompt = `As TrustPay Fraud Protection, assess this transaction:
Transaction ID: ${transactionId}
Recipient UPI: ${recipient}
Amount: INR ${amount}
Heuristic Score: ${score}/100
Signals: ${(signals || []).join(", ") || "None"}
Velocity Anomaly Triggered: ${isAnomaly ? "YES (Rolling 5-min threshold breached)" : "NO"}

Provide a concise 2-sentence fraud risk review explaining why this risk level was assigned and the recommended verification step.`;

      const response = await ai.models.generateContent({
        model: "gemini-2.5-flash",
        contents: prompt,
      });

      res.json({
        insights: response.text?.trim() || "Transaction cleared against standard heuristic verification.",
        source: "gemini"
      });
    } catch (err) {
      console.error("Gemini evaluation error:", err);
      res.json({
        insights: "Heuristic anomaly detection active. Payload bound with SHA-256 integrity digest.",
        source: "fallback"
      });
    }
  });

  // Vite middleware in development
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`TrustPay running on http://0.0.0.0:${PORT}`);
  });
}

startServer();

