package com.example.trustpay.services

import com.example.trustpay.model.RiskAnalysisResult
import com.example.trustpay.security.CryptoUtils
import com.example.trustpay.storage.TrustPayStorage

object RiskEngineService {

    data class RiskFactors(
        val untrustedDevice: Boolean = false,
        val newRecipient: Boolean = false,
        val unusualContext: Boolean = false
    )

    fun evaluateTransactionRisk(
        transactionId: String,
        recipient: String,
        amount: Double,
        factors: RiskFactors,
        currency: String = "INR"
    ): RiskAnalysisResult {
        var score = 10 // baseline nominal risk
        val signals = mutableListOf<String>()

        // Check simulated device/recipient/context factors
        if (factors.untrustedDevice) {
            score += 25
            signals.add("Untrusted Hardware / Unrecognized Device Signature (+25)")
        }
        if (factors.newRecipient) {
            score += 20
            signals.add("First-Time / Unverified Beneficiary VPA (+20)")
        }
        if (factors.unusualContext) {
            score += 20
            signals.add("Out-of-Pattern Temporal or Geolocation Context (+20)")
        }

        // Amount thresholds
        if (amount > 50000.0) {
            score += 35
            signals.add("High-Value Transfer Exceeds ₹50,000 (+35)")
        } else if (amount > 10000.0) {
            score += 20
            signals.add("Elevated Amount Exceeds ₹10,000 (+20)")
        }

        // Velocity Anomaly check (5-minute rolling window)
        val rollingTxns = TrustPayStorage.getRollingWindowTransactions(5)
        val rollingTotal = TrustPayStorage.getRollingWindowTotalAmount(5)

        val isVelocityAnomaly = (rollingTxns.size >= 3) || (rollingTotal >= 50000.0)
        if (isVelocityAnomaly) {
            score += 35
            signals.add("Velocity Breach: ${rollingTxns.size} txns / ${CryptoUtils.formatIndianCurrency(rollingTotal)} in rolling 5m (+35)")
        }

        val clampedScore = score.coerceIn(0, 100)
        val requiresBiometric = clampedScore >= 35
        val requiresOtp = clampedScore >= 65 || isVelocityAnomaly

        val boundHash = CryptoUtils.computeBindingHash(transactionId, recipient, amount, currency)

        val aiInsights = generateAiRiskInsight(clampedScore, signals, isVelocityAnomaly)

        return RiskAnalysisResult(
            score = clampedScore,
            signals = signals,
            requiresBiometric = requiresBiometric,
            requiresOtp = requiresOtp,
            isVelocityAnomaly = isVelocityAnomaly,
            boundHash = boundHash,
            aiInsights = aiInsights
        )
    }

    private fun generateAiRiskInsight(
        score: Int,
        signals: List<String>,
        isVelocityAnomaly: Boolean
    ): String {
        return when {
            isVelocityAnomaly ->
                "CRITICAL VELOCITY TRIGGER: Rapid transaction volume or cumulative outflow detected in the last 5 minutes. Dual-factor authorization with cognitive intent verification is strictly enforced."
            score >= 65 ->
                "HIGH RISK DETECTED: Multiple threat vectors identified (${signals.size} active signals). Mandating step-up biometric verification and secure OTP confirmation."
            score >= 35 ->
                "MODERATE RISK ASSESSMENT: Transaction parameters deviate from standard baseline. Biometric designated finger confirmation required to seal cryptographic payload."
            else ->
                "LOW RISK PROFILE: Standard transaction baseline verified. One-click cryptographic seal enabled with zero friction."
        }
    }
}
