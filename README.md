# TrustPay

> **“Secure every transaction.”**  
> *Hackathon Payment Security Prototype • Simulated Sandbox Environment*

TrustPay is a payment security application rewritten from Android into **React 18, Vite, TypeScript, and Tailwind CSS**. It demonstrates advanced transaction authentication principles designed to protect users against split-payment attacks, unauthorized high-value transfers, and client-side wire tampering.

---

## ⚠️ Safe Demo Notice

This application is strictly an **offline hackathon security demonstration prototype**. It does **NOT** connect to live bank networks, real UPI rails, credit/debit card gateways, external SMS gateways, or actual OTP servers. All payment flows, OTPs, and beneficiary accounts are simulated in a secure browser sandbox.

---

## 🛡️ Core Security Architecture & Rules

TrustPay implements all core security rules preserved from the original application:

1. **Low-Risk Direct Approvals**:  
   Normal everyday transactions (amount ≤ ₹40,000, no velocity anomaly, low risk score) are cryptographically bound and approved seamlessly without friction.

2. **High-Value Device/Biometric Authentication**:  
   Transactions strictly greater than **₹25,000** or risk score ≥ 70 require biometric verification (simulating Fingerprint / Face authentication).

3. **SHA-256 Cryptographic Payload Binding & Anti-Tamper Engine**:  
   Each transaction binds `recipient`, `amount`, `currency` (INR), and a unique `transactionId` into a 64-character SHA-256 digest:  
   `SHA-256("$transactionId|$recipient|$amount|INR")`  
   If any parameter is mutated or tampered between analysis and commit, the verification engine detects the hash divergence and instantly halts the transaction with **“Transaction tampering detected”**.

4. **Persistent Transaction Store (`localStorage`)**:  
   Successful transactions, user accounts, demo balances, and audit logs are committed to local persistent storage. Refreshing the browser preserves the rolling 5-minute velocity window state and account balances.

5. **Rolling 5-Minute Velocity Window & Split-Payment Detection**:  
   Tracks all successful transactions completed within the last 300 seconds (5 minutes). If a user attempts a **3rd transaction** and the total sum of transactions in that 5-minute window (including the pending attempt) is **≥ ₹15,000**, the system flags a **“Velocity Anomaly / Split-payment attack”**.

6. **Mandatory 3-Step Verification Protocol for Velocity Anomalies**:  
   When a velocity anomaly is triggered, payment is gated behind three sequential verification challenges:
   - **Step 1 — Biometric Challenge with Dynamic Designated Finger**: Assigns a random designated finger (e.g., Right Index, Left Thumb) to defeat automated replay bots.
   - **Step 2 — Cognitive Detail Challenge**: Prompts for exact transaction parameter confirmation.
   - **Step 3 — Demo Bank OTP**: The system generates a simulated 6-digit OTP clearly presented as `DEMO OTP: [XXXXXX]` for hackathon testing, which the user confirms.

7. **Strict Fail-Closed Rule**:  
   If *any* step of the verification sequence is cancelled, fails, or receives incorrect user input, the transaction is immediately blocked and recorded in the audit trail.

8. **Atomic Success Recording & Balance Tracking**:  
   Transactions are *only* appended to the persistent history after **all** required security verifications have succeeded. Balance remaining after payment is tracked and displayed.

9. **Demo History & Sandbox Balance Reset**:  
   Tapping **“Reset Demo History”** purges stored transactions and resets the rolling window counter to zero. Tapping **“Demo Balance Reset”** restores the demo balance to ₹1,00,000.00.

---

## 🚀 Hackathon Demo Walkthrough Scenario

Follow this step-by-step path to test the entire security pipeline:

1. **Quick Login**:
   - On the login screen, click **“Quick Demo Login”** to auto-populate `rahul@example.com` / `password123`, then click **Sign In**.
2. **First Payment (₹5,000)**:
   - Recipient: `rahul@okhdfcbank`, Amount: `5000`.
   - Click **“Analyse & Pay”**.
   - Result: Low risk (Score: 10/100). Approved directly.
   - Rolling window: 1 transaction (₹5,000 / ₹15,000).
3. **Second Payment (₹5,000)**:
   - Click **“Analyse & Pay”**.
   - Result: Approved directly.
   - Rolling window: 2 transactions (₹10,000 / ₹15,000).
4. **Third Payment (₹5,000) — Velocity Anomaly Triggered**:
   - Click **“Analyse & Pay”**.
   - Result: Total in 5-minute window reaches **3 transactions totaling ₹15,000**.
   - **“VELOCITY ANOMALY — 3-STEP VERIFICATION”** is triggered.
   - **Step 1**: Click **“Present [Designated Finger] (Pass)”**.
   - **Step 2**: Enter the exact transaction amount (`5000`) and click **Verify**.
   - **Step 3**: Enter the displayed `DEMO OTP` code and click **Verify OTP & Authorize**.
   - Result: All steps verified! Payment is authorized and recorded in history.
5. **Attack Demonstration (Tamper Amount)**:
   - Click **“Attack Demo: Tamper Amount”**.
   - The app binds the original payload, then simulates payload alteration from ₹5,000 to ₹99,999.
   - Result: SHA-256 verification fails! Final decision: **“TAMPERING DETECTED”**. Payment blocked instantly.
6. **Attack Demonstration (Tamper Recipient UPI)**:
   - Click **“Attack Demo: Tamper Recipient UPI”**.
   - Result: Mismatch caught by cryptographic re-verification. Payee substitution attack blocked.

---

## 🛠️ Stack & Architecture

- **Runtime**: Node.js 22
- **Framework**: React 18 with TypeScript and Vite
- **Styling**: Tailwind CSS with custom high-contrast security theme & WCAG AAA tokens
- **Icons**: Lucide React
- **Animations**: Motion (`motion/react`)
- **Backend API**: Express server (`server.ts`) running on port 3000 with deterministic rule-based fraud risk insights engine
- **Cryptography**: Synchronous and Web Crypto SHA-256 payload binding and salted password hashing
- **Persistence**: `localStorage` and simulated Firestore (User accounts, demo balances, transactions, audit logs)
- **P2P QR Engine**: 30-second rotating HMAC-SHA256 tokens, one-time nonces, double-spend prevention

---

## 📱 Native Android Kotlin & Firebase P2P QR Engine

TrustPay includes complete native Android Studio Kotlin code (`/app`) and Firebase Cloud Functions (`/functions`) implementing the zero-client-secret P2P payment flow:

### 1. Protocol Architecture
- **URI Protocol Format**: `trustpay://p2p/request?tokenId=<TOKEN_ID>&expiresAt=<TIMESTAMP>&version=1`
- **Automatic 30s Rotation**: The receiver's screen generates a fresh token every 30 seconds to prevent stale captures and shoulder-surfing.
- **Server-Only HMAC-SHA256 Signing**: Signing secrets and master keys remain strictly inside Firebase Cloud Functions (`process.env.QR_SIGNING_SECRET`) — never stored in the Android APK.
- **Atomic Double-Spend Lock**: `db.runTransaction()` locks the token, mutates sender/receiver balances, and transitions status to `USED`. Subsequent attempts trigger an immediate rejection: `"QR already used."`

### 2. Android Studio Code Structure
- `/app/src/main/AndroidManifest.xml`: Declares Camera and Internet permissions, and registered P2P activities.
- `/app/src/main/java/com/example/trustpay/model/`: `QrPaymentRequest.kt`, `PaymentTransaction.kt`, `UserAccount.kt`.
- `/app/src/main/java/com/example/trustpay/security/`: `QrProtocolHelper.kt` (URI parsing, SHA-256 payload binding, ZXing bitmap generator).
- `/app/src/main/java/com/example/trustpay/firebase/`: `FirebaseRepository.kt` (Cloud Functions invoker and real-time Firestore listeners).
- `/app/src/main/java/com/example/trustpay/ui/`:
  - `ReceiveMoneyQrActivity.kt`: 30-second countdown timer, automatic rotation, and real-time payment listener.
  - `ScanAndPayActivity.kt`: CameraX scanner with ML Kit, BiometricPrompt confirmation, and double-spend detection.
- `/app/build.gradle.kts`: Configured with CameraX, ML Kit Barcode Scanning, ZXing, BiometricPrompt, and Firebase BoM.
- `/functions/index.js`: Firebase Cloud Functions for `createQrPaymentRequest`, `validateQrToken`, and `completeP2PTransfer`.
- `/firestore.rules`: Cloud Firestore security rules enforcing read permissions and denying direct client balance mutations.

### 3. Firebase Setup Instructions
1. In the [Firebase Console](https://console.firebase.google.com/), create a project and enable **Firebase Authentication** (Email/Password or Anonymous) and **Cloud Firestore**.
2. Download `google-services.json` and place it inside the `app/` folder.
3. Deploy Cloud Functions and Security Rules:
   ```bash
   firebase deploy --only functions,firestore:rules
   ```
4. Build and run the Android app on two separate Android phones or emulators. Login as two different demo users, generate a QR on Device A, and scan on Device B to test real-time P2P transfers!

> **Simulation only - no real money transfer**

