# TrustPay AI

> **“Authenticate the transaction, not just the person.”**  
> *Hackathon Payment Security Prototype • Simulated Sandbox Environment*

TrustPay AI is a native Android security application written in Kotlin with Jetpack Compose. It demonstrates advanced transaction authentication principles designed to protect users against split-payment attacks, unauthorized high-value transfers, and client-side wire tampering.

---

## ⚠️ Safe Demo Notice

This application is strictly an **offline hackathon security demonstration prototype**. It does **NOT** connect to live bank networks, real UPI rails, credit/debit card gateways, external SMS gateways, or actual OTP servers. All payment flows, OTPs, and beneficiary accounts are simulated with safe test data.

---

## 🛡️ Core Security Architecture & Rules

TrustPay AI implements 9 key security rules:

1. **Low-Risk Direct Approvals**:  
   Normal everyday transactions (amount ≤ ₹40,000, no velocity anomaly, low risk score) are cryptographically bound and approved seamlessly without friction.

2. **High-Value Device/Biometric Authentication**:  
   Transactions strictly greater than **₹40,000** require system biometric or secure device credential authentication (Fingerprint, Face, or PIN/pattern/password) via AndroidX `BiometricPrompt`.

3. **SHA-256 Cryptographic Payload Binding & Anti-Tamper Engine**:  
   Each transaction binds `recipient`, `amount`, `currency` (INR), and a unique `transactionId` into a 64-character SHA-256 digest:  
   `SHA-256("$transactionId|$recipient|$amount|INR")`  
   If any parameter is mutated or tampered between analysis and commit, the verification engine detects the hash divergence and instantly halts the transaction with **“Transaction tampering detected”**.

4. **Persistent Transaction Store (`SharedPreferences`)**:  
   Successful transactions are committed to local private persistent storage via `SharedPreferences`. Restarting or killing the app cannot bypass the rolling 5-minute velocity window rule.

5. **Rolling 5-Minute Velocity Window & Split-Payment Detection**:  
   Tracks all successful transactions completed within the last 300 seconds (5 minutes). If a user attempts a **3rd transaction** and the total sum of transactions in that 5-minute window (including the pending attempt) is **≥ ₹15,000**, the system flags a **“Velocity Anomaly / Split-payment attack”** (independent of whether recipients are the same or different).

6. **Mandatory 3-Step Verification Protocol for Velocity Anomalies**:  
   When a velocity anomaly is triggered, payment is gated behind three sequential verification challenges:
   - **Step 1 — Biometric / Device Credential**: User must pass Android OS hardware authentication.
   - **Step 2 — Random Detail Challenge**: The engine randomly prompts either for the *exact transaction amount* or the *last 4 characters of the recipient UPI ID*.
   - **Step 3 — Demo Bank OTP**: The system generates a simulated 6-digit OTP clearly presented as `DEMO OTP: [XXXXXX]` for hackathon testing, which the user must confirm.

7. **Strict Fail-Closed Rule**:  
   If *any* step of the verification sequence is cancelled, fails, or receives incorrect user input, the transaction is immediately blocked and recorded in the audit trail.

8. **Atomic Success Recording**:  
   Transactions are *only* appended to the persistent history after **all** required security verifications have succeeded.

9. **Demo History Reset**:  
   Tapping **“Reset Demo History”** purges the locally stored transaction history from SharedPreferences, resets the rolling window counter to zero, and clears the active decision state.

---

## 🚀 Hackathon Demo Walkthrough Scenario

Follow this step-by-step path to test the entire security pipeline:

1. **Clean Slate**:
   - Tap **“Reset Demo History”**.
   - Observe that the rolling 5-minute counter resets to 0 transactions and ₹0.00.

2. **First Payment (₹5,000)**:
   - Recipient: `rahul@okhdfcbank`, Amount: `5000`.
   - Tap **“Analyse & Pay”**.
   - Result: Low risk (Score: 10/100). Approved directly.
   - Rolling window: 1 transaction (₹5,000 / ₹15,000).

3. **Second Payment (₹5,000)**:
   - Recipient: `priya@axisbank` (or keep default), Amount: `5000`.
   - Tap **“Analyse & Pay”**.
   - Result: Approved directly.
   - Rolling window: 2 transactions (₹10,000 / ₹15,000).

4. **Third Payment (₹5,000) — Velocity Anomaly Triggered**:
   - Recipient: `merchant@upi`, Amount: `5000`.
   - Tap **“Analyse & Pay”**.
   - Result: Total in 5-minute window reaches **3 transactions totaling ₹15,000**.
   - **“VELOCITY ANOMALY — 3-STEP VERIFICATION”** is triggered.
   - **Step 1**: Complete Android Biometric/Device lock authentication.
   - **Step 2**: Enter the correct challenge answer (e.g., amount `5000` or last 4 chars).
   - **Step 3**: Enter the displayed `DEMO OTP` code.
   - Result: All steps verified! Payment is authorized and recorded in history.

5. **Attack Demonstration (Tamper Amount)**:
   - Tap **“Attack Demo: Tamper Amount”**.
   - The app binds the original payload, then simulates memory modification altering the transaction amount from ₹5,000 to ₹50,000.
   - Result: SHA-256 verification fails! Final decision: **“TAMPERING DETECTED”**. Payment blocked instantly.

---

## 🛠️ Build & Run Instructions

### Prerequisites
- Android Studio Ladybug | 2024.2+ or Android Studio Koala / Hedgehog
- JDK 17 or JDK 21
- Android SDK 36 (compileSdk 36, minSdk 28)

### Building with Gradle
```bash
# Clean and compile debug APK
gradle assembleDebug

# Run unit tests
gradle :app:testDebugUnitTest
```

### Android Architecture
- **Language**: Kotlin 2.2.10
- **UI Toolkit**: Jetpack Compose with Material 3 (Navy Blue Security Theme)
- **Architecture**: MVVM with Kotlin StateFlow
- **Biometrics**: AndroidX Biometric 1.2.0 (`BiometricPrompt`)
- **Crypto**: `java.security.MessageDigest` SHA-256
- **Storage**: Android `SharedPreferences` (JSON payload persistence)
