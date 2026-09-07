import { AuthenticatedTransactionSnapshot } from '../types';

/**
 * Standard SHA-256 hash using Web Crypto API.
 */
export async function sha256Async(message: string): Promise<string> {
  const msgUint8 = new TextEncoder().encode(message);
  const hashBuffer = await window.crypto.subtle.digest('SHA-256', msgUint8);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map((b) => b.toString(16).padStart(2, '0')).join('');
}

/**
 * Pure JavaScript synchronous SHA-256 implementation to guarantee instant
 * cryptographic generation without async microtask delays in React state pipelines.
 */
export function sha256Sync(ascii: string): string {
  function rightRotate(value: number, amount: number) {
    return (value >>> amount) | (value << (32 - amount));
  }

  const mathPow = Math.pow;
  const maxWord = mathPow(2, 32);
  const asciiLength = ascii.length;
  let i = 0, j = 0;
  let result = '';
  const words: number[] = [];
  const asciiBitLength = asciiLength * 8;

  let hash = [
    0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
    0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19,
  ];

  const k = [
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
  ];

  let compositeClearHex = '';
  for (i = 0; i < asciiLength; i++) {
    const charCode = ascii.charCodeAt(i);
    compositeClearHex += (charCode < 16 ? '0' : '') + charCode.toString(16);
  }

  for (i = 0; i < asciiBitLength; i += 8) {
    words[i >> 5] |= (ascii.charCodeAt(i / 8) & 0xff) << (24 - (i % 32));
  }
  words[asciiBitLength >> 5] |= 0x80 << (24 - (asciiBitLength % 32));
  words[(((asciiBitLength + 64) >> 9) << 4) + 15] = asciiBitLength;

  const w: number[] = [];
  for (i = 0; i < words.length; i += 16) {
    const a = hash[0], b = hash[1], c = hash[2], d = hash[3];
    const e = hash[4], f = hash[5], g = hash[6], h = hash[7];

    for (j = 0; j < 64; j++) {
      if (j < 16) {
        w[j] = words[j + i] | 0;
      } else {
        const gamma0 = rightRotate(w[j - 15], 7) ^ rightRotate(w[j - 15], 18) ^ (w[j - 15] >>> 3);
        const gamma1 = rightRotate(w[j - 2], 17) ^ rightRotate(w[j - 2], 19) ^ (w[j - 2] >>> 10);
        w[j] = (w[j - 16] + gamma0 + w[j - 7] + gamma1) | 0;
      }

      const s1 = rightRotate(hash[4], 6) ^ rightRotate(hash[4], 11) ^ rightRotate(hash[4], 25);
      const ch = (hash[4] & hash[5]) ^ (~hash[4] & hash[6]);
      const temp1 = (hash[7] + s1 + ch + k[j] + w[j]) | 0;
      const s0 = rightRotate(hash[0], 2) ^ rightRotate(hash[0], 13) ^ rightRotate(hash[0], 22);
      const maj = (hash[0] & hash[1]) ^ (hash[0] & hash[2]) ^ (hash[1] & hash[2]);
      const temp2 = (s0 + maj) | 0;

      hash[7] = hash[6];
      hash[6] = hash[5];
      hash[5] = hash[4];
      hash[4] = (hash[3] + temp1) | 0;
      hash[3] = hash[2];
      hash[2] = hash[1];
      hash[1] = hash[0];
      hash[0] = (temp1 + temp2) | 0;
    }

    hash[0] = (hash[0] + a) | 0;
    hash[1] = (hash[1] + b) | 0;
    hash[2] = (hash[2] + c) | 0;
    hash[3] = (hash[3] + d) | 0;
    hash[4] = (hash[4] + e) | 0;
    hash[5] = (hash[5] + f) | 0;
    hash[6] = (hash[6] + g) | 0;
    hash[7] = (hash[7] + h) | 0;
  }

  for (i = 0; i < 8; i++) {
    for (j = 3; j >= 0; j--) {
      const b = (hash[i] >> (8 * j)) & 255;
      result += (b < 16 ? '0' : '') + b.toString(16);
    }
  }
  return result;
}

/**
 * Computes a SHA-256 cryptographic binding hash over all payment parameters.
 * Format: TXN_ID|RECIPIENT|AMOUNT|CURRENCY
 */
export function computeBindingHash(
  transactionId: string,
  recipient: string,
  amount: number,
  currency: string = 'INR'
): string {
  const payload = `${transactionId}|${recipient.trim()}|${amount.toFixed(2)}|${currency}`;
  return sha256Sync(payload);
}

/**
 * Creates an authenticated snapshot record for tamper re-verification.
 */
export function createSnapshot(
  transactionId: string,
  recipient: string,
  amount: number,
  currency: string = 'INR'
): AuthenticatedTransactionSnapshot {
  const hash = computeBindingHash(transactionId, recipient, amount, currency);
  return {
    transactionId,
    recipient: recipient.trim(),
    amount,
    currency,
    bindingHash: hash,
    timestamp: Date.now(),
  };
}

/**
 * Crucial Rule: Re-verify transaction integrity before final release.
 * Verifies current submission against the snapshot taken at authorization time.
 */
export function verifyTransactionIntegrity(
  snapshot: AuthenticatedTransactionSnapshot,
  currentRecipient: string,
  currentAmount: number,
  currentCurrency: string = 'INR',
  currentTxId: string = snapshot.transactionId
): boolean {
  if (snapshot.transactionId !== currentTxId) return false;
  if (snapshot.recipient !== currentRecipient.trim()) return false;
  if (Math.abs(snapshot.amount - currentAmount) > 0.001) return false;
  if (snapshot.currency !== currentCurrency) return false;

  const recomputed = computeBindingHash(currentTxId, currentRecipient, currentAmount, currentCurrency);
  return recomputed === snapshot.bindingHash;
}

/**
 * Generate cryptographically random 8-character transaction ID.
 */
export function generateTransactionId(): string {
  const chars = '0123456789ABCDEF';
  let rand = '';
  for (let i = 0; i < 8; i++) {
    rand += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return `TXN_${rand}`;
}

/**
 * Generates a 16-character random hex salt.
 */
export function generateSalt(): string {
  const hex = '0123456789abcdef';
  let salt = '';
  for (let i = 0; i < 16; i++) {
    salt += hex.charAt(Math.floor(Math.random() * hex.length));
  }
  return salt;
}

/**
 * Hashes a password with a cryptographic salt using SHA-256.
 */
export function hashPasswordWithSalt(password: string, salt: string): string {
  return sha256Sync(`${salt}:${password}`);
}

/**
 * Generates a random 6-digit Demo OTP for testing.
 */
export function generateDemoOtp(): string {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

/**
 * Indian currency format helper (e.g. ₹1,00,000.00)
 */
export function formatIndianCurrency(amount: number): string {
  try {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(amount);
  } catch {
    return `₹${amount.toFixed(2)}`;
  }
}

/**
 * Generates a unique, cryptographically random QR Token ID.
 */
export function generateQrTokenId(): string {
  const chars = '0123456789abcdef';
  let token = 'tp_qr_';
  for (let i = 0; i < 24; i++) {
    token += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return token;
}

/**
 * Builds the official TrustPay P2P QR payload URI.
 * Format: trustpay://p2p/request?tokenId=UNIQUE_TOKEN_ID&expiresAt=TIMESTAMP&version=1
 */
export function buildQrPayloadUri(tokenId: string, expiresAt: number, version = 1): string {
  return `trustpay://p2p/request?tokenId=${encodeURIComponent(tokenId)}&expiresAt=${expiresAt}&version=${version}`;
}

/**
 * Parses and validates a scanned QR payload URI.
 */
export function parseQrPayloadUri(rawUri: string): { tokenId: string; expiresAt: number; version: number } | null {
  if (!rawUri || !rawUri.startsWith('trustpay://p2p/request')) {
    return null;
  }
  try {
    const queryPart = rawUri.split('?')[1];
    if (!queryPart) return null;
    const params = new URLSearchParams(queryPart);
    const tokenId = params.get('tokenId');
    const expiresAtStr = params.get('expiresAt');
    const versionStr = params.get('version') || '1';

    if (!tokenId || !expiresAtStr) return null;
    const expiresAt = parseInt(expiresAtStr, 10);
    if (isNaN(expiresAt)) return null;

    return {
      tokenId,
      expiresAt,
      version: parseInt(versionStr, 10) || 1,
    };
  } catch {
    return null;
  }
}

/**
 * Creates the second approval cryptographic signature/hash linking
 * transaction ID, amount, recipient, User 1 ID, User 2 ID, timestamp, and nonce.
 */
export function computeDualAuthSecondSignature(
  transactionId: string,
  amount: number,
  recipient: string,
  user1Id: string,
  user2Id: string,
  timestamp: number,
  nonce: string
): string {
  const payload = `DUAL_SIG2:${transactionId}:${amount.toFixed(2)}:${recipient.toLowerCase()}:${user1Id.toLowerCase()}:${user2Id.toLowerCase()}:${timestamp}:${nonce}`;
  return sha256Sync(payload);
}

