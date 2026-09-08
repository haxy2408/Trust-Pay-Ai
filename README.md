# TrustPay Mobile (Android Native)

> **“Secure every transaction with Zero-Trust Cryptographic Assurance.”**  
> *Native Android Application • Jetpack Compose • Material 3 • Modern Clean Architecture*

TrustPay is a zero-trust payment security Android application built with **Kotlin and Jetpack Compose**. It implements client-side transaction authentication principles designed to protect users against wire tampering, payee substitution, split-payment velocity attacks, and unauthorized high-value transfers.

---

## 🛡️ Core Security Architecture & Features

TrustPay delivers end-to-end payment security through native Android components:

1. **SHA-256 Parameter Binding & Tamper Detection**:  
   Every transaction binds `recipient`, `amount`, `currency` (INR), and a unique `transactionId` into a cryptographic digest:  
   `SHA-256("$transactionId|$recipient|$amount|INR")`  
   Any alteration in-flight causes an instant digest divergence and blocks execution with an immediate audit flag.

2. **Rolling 5-Minute Velocity Anomaly Detection**:  
   Monitors transaction frequency and cumulative volume over a rolling 300-second window. Breaching either limit (3 transactions or ₹50,000) automatically triggers step-up verification.

3. **Biometric Intent Verification & Designated Sensor Challenges**:  
   High-value transfers require biometric authorization coupled with a dynamic challenge (e.g., "Right Index Finger", "Left Thumb") to defeat automated bot replays.

4. **Rotating P2P QR Payments (30s TTL)**:  
   P2P payment requests feature dynamic, rotating QR codes with 30-second time-to-live, cryptographic nonces, and single-use replay protection.

5. **Corporate Dual-Authorization Engine**:  
   Large corporate payments require two-person integrity. User 1 initiates the transaction, while an authorized second co-signer (CFO, Risk Officer, Treasury Director) must review and cryptographically sign before fund release.

6. **Haversine Geofencing Distance Classifier**:  
   Calculates precise spatial distances using the spherical Haversine formula and classifies destinations into four distinct security tiers:
   - **0–10 km**: Usual distance (Green)
   - **>10–20 km**: Medium distance (Yellow)
   - **>20–30 km**: Far distance (Orange)
   - **>30 km**: High distance (Red)

7. **Zero-Trust Security Operations Center (SOC) & Audit Trail**:  
   Live ledger of all transaction events, velocity breaches, tamper attempts, and status filters with complete SHA-256 verification seals.

---

## 🚀 Interactive Demo Scenarios

The app features an interactive scenario switcher via the header banner:
1. **Nominal Low-Risk Payment**: Transfer of ₹500 from a trusted device -> instant 1-click cryptographic release.
2. **Moderate Risk / Step-Up Biometric**: Transfer of ₹15,000 to a new merchant -> triggers designated finger biometric challenge.
3. **High-Risk Threat / Device Anomaly**: Untrusted hardware device -> triggers multi-factor verification.
4. **Tamper Attack Simulation**: Simulates in-flight payload modification -> intercepted and blocked by SHA-256 integrity mismatch.
5. **Velocity Anomaly Breach**: Rapid transfers exceeding the 5-minute threshold -> triggers 6-digit OTP intent verification.

---

## 🛠️ Technology Stack

- **Platform**: Android (minSdk 24, compileSdk 34)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose & Material 3
- **Theme**: Custom Dark High-Contrast Security Theme (`Navy900`, `TrustCyan`, `ElectricBlue`)
- **Barcode & QR**: ZXing (`core`) for monochrome QR matrix generation
- **State Architecture**: MVVM with thread-safe in-memory singleton repository (`TrustPayStorage`)
- **Cryptography**: Native Java `MessageDigest` SHA-256 parameter binding and PBKDF2-style salted password hashing
