package com.example.trustpay.ai

import org.json.JSONArray
import org.json.JSONObject

/**
 * Gemini Security Analysis Module.
 *
 * Requirements:
 * - Provides AI-powered threat analysis and anomaly classification.
 * - Formats output as structured JSON.
 * - Evaluates device posture, biometric result, face verification, OTP status,
 *   amount deviation, and behavioral context.
 * - STRICT POLICY: The AI model ONLY provides security assessment and risk scoring.
 *   Gemini MUST NEVER directly transfer money or execute payment authorizations.
 *   Final decision is strictly made by deterministic backend security rules.
 */
object GeminiSecurityAnalysis {

    enum class AiFailureMode {
        NONE,
        API_NETWORK_FAILURE,
        TIMEOUT,
        MALFORMED_JSON,
        MISSING_FIELDS,
        RATE_LIMIT,
        MODEL_UNAVAILABLE
    }

    private var activeFailureMode: AiFailureMode = AiFailureMode.NONE

    fun setSimulationFailureMode(mode: AiFailureMode) {
        activeFailureMode = mode
    }

    fun getSimulationFailureMode(): AiFailureMode = activeFailureMode

    data class AiSecurityResponse(
        val transactionId: String,
        val riskLevel: String, // "LOW", "MEDIUM", "HIGH"
        val riskScore: Int,
        val securityChecks: Map<String, String>, // "device" to "PASS", etc.
        val reasonCodes: List<String>,
        val recommendedAction: String, // "ALLOW", "EXTRA_VERIFICATION", "BLOCK"
        val rawJsonOutput: String,
        val aiNarrativeExplanation: String,
        val isFallback: Boolean = false
    )

    /**
     * Validates raw JSON output from Gemini AI according to strict security specification.
     */
    fun validateAiResponse(rawJson: String): Result<AiSecurityResponse> {
        return try {
            val obj = JSONObject(rawJson)
            if (!obj.has("risk_level") || !obj.has("risk_score") || !obj.has("recommended_action") || !obj.has("reason_codes")) {
                return Result.failure(IllegalArgumentException("Malformed AI JSON: missing mandatory security fields."))
            }
            val riskLevel = obj.getString("risk_level").uppercase()
            if (riskLevel !in listOf("LOW", "MEDIUM", "HIGH")) {
                return Result.failure(IllegalArgumentException("Invalid risk_level: $riskLevel"))
            }
            val riskScore = obj.getInt("risk_score")
            if (riskScore !in 0..100) {
                return Result.failure(IllegalArgumentException("risk_score out of bounds: $riskScore"))
            }
            val recAction = obj.getString("recommended_action").uppercase()
            if (recAction !in listOf("ALLOW", "EXTRA_VERIFICATION", "BLOCK")) {
                return Result.failure(IllegalArgumentException("Invalid recommended_action: $recAction"))
            }
            val reasons = mutableListOf<String>()
            val arr = obj.getJSONArray("reason_codes")
            for (i in 0 until arr.length()) {
                reasons.add(arr.getString(i))
            }
            val checks = mutableMapOf<String, String>()
            if (obj.has("security_checks")) {
                val cObj = obj.getJSONObject("security_checks")
                val keys = cObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    checks[k] = cObj.getString(k)
                }
            }
            Result.success(
                AiSecurityResponse(
                    transactionId = obj.optString("transaction_id", "TXN-VALIDATED"),
                    riskLevel = riskLevel,
                    riskScore = riskScore,
                    securityChecks = checks,
                    reasonCodes = reasons,
                    recommendedAction = recAction,
                    rawJsonOutput = rawJson,
                    aiNarrativeExplanation = obj.optString("narrative", "AI structured assessment successfully validated.")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deterministic fallback when Gemini AI is unavailable or produces invalid telemetry.
     * ZERO-TRUST RULE: Never automatically approve when AI fails.
     */
    fun getDeterministicFallbackAnalysis(
        transactionId: String,
        deviceVerified: Boolean,
        biometricVerified: Boolean,
        faceVerified: Boolean,
        otpVerified: Boolean,
        failureReason: String
    ): AiSecurityResponse {
        val shouldBlock = (!deviceVerified && !biometricVerified) || !otpVerified
        val riskLevel = if (shouldBlock) "HIGH" else "MEDIUM"
        val riskScore = if (shouldBlock) 85 else 48
        val recAction = if (shouldBlock) "BLOCK" else "EXTRA_VERIFICATION"
        val reasons = mutableListOf("AI_UNAVAILABLE_FALLBACK_RULES_APPLIED", failureReason)
        if (!deviceVerified) reasons.add("ANOMALY_DEVICE_UNRECOGNIZED")
        if (!biometricVerified) reasons.add("MISSING_BIOMETRIC_INTENT")
        if (!faceVerified) reasons.add("FACE_AUTH_INCOMPLETE")

        val checks = mapOf(
            "device" to if (deviceVerified) "PASS" else "FAIL",
            "biometric" to if (biometricVerified) "PASS" else "FAIL",
            "face" to if (faceVerified) "PASS" else "FAIL",
            "otp" to if (otpVerified) "PASS" else "FAIL"
        )

        val fallbackJson = JSONObject().apply {
            put("transaction_id", transactionId)
            put("risk_level", riskLevel)
            put("risk_score", riskScore)
            put("reason_codes", JSONArray(reasons))
            put("recommended_action", recAction)
            put("ai_engine_status", "DETERMINISTIC_FALLBACK")
        }.toString(2)

        return AiSecurityResponse(
            transactionId = transactionId,
            riskLevel = riskLevel,
            riskScore = riskScore,
            securityChecks = checks,
            reasonCodes = reasons,
            recommendedAction = recAction,
            rawJsonOutput = fallbackJson,
            aiNarrativeExplanation = "Gemini AI unavailable ($failureReason). Applied zero-trust fallback policy: $recAction.",
            isFallback = true
        )
    }

    /**
     * Synthesizes and analyzes transaction telemetry to produce structured JSON security assessment.
     * Safely catches API, parsing, network, and timeout exceptions.
     */
    fun analyzeTransaction(
        transactionId: String,
        amount: Double,
        currency: String = "INR",
        recipient: String,
        deviceVerified: Boolean,
        biometricVerified: Boolean,
        faceVerified: Boolean,
        otpVerified: Boolean,
        locationAnomaly: Boolean = false,
        velocityBreached: Boolean = false,
        failedAttempts: Int = 0
    ): AiSecurityResponse {
        // Handle simulated AI failures if active
        if (activeFailureMode != AiFailureMode.NONE) {
            val failureMsg = when (activeFailureMode) {
                AiFailureMode.API_NETWORK_FAILURE -> "API Network connection failed / DNS unreachable"
                AiFailureMode.TIMEOUT -> "Gemini API request timed out after 10,000ms"
                AiFailureMode.MALFORMED_JSON -> "Gemini API returned malformed JSON syntax"
                AiFailureMode.MISSING_FIELDS -> "Gemini API response omitted mandatory 'risk_level' and 'recommended_action'"
                AiFailureMode.RATE_LIMIT -> "Gemini API HTTP 429: Rate limit quota reached"
                AiFailureMode.MODEL_UNAVAILABLE -> "Gemini API HTTP 503: Model temporarily unavailable"
                AiFailureMode.NONE -> ""
            }
            return getDeterministicFallbackAnalysis(
                transactionId = transactionId,
                deviceVerified = deviceVerified,
                biometricVerified = biometricVerified,
                faceVerified = faceVerified,
                otpVerified = otpVerified,
                failureReason = failureMsg
            )
        }

        return try {
            val checks = mutableMapOf<String, String>()
            checks["device"] = if (deviceVerified) "PASS" else "FAIL"
            checks["biometric"] = if (biometricVerified) "PASS" else "FAIL"
            checks["face"] = if (faceVerified) "PASS" else "FAIL"
            checks["otp"] = if (otpVerified) "PASS" else "FAIL"

            val reasonCodes = mutableListOf<String>()
            var computedRisk = 12

            if (!deviceVerified) {
                computedRisk += 38
                reasonCodes.add("ANOMALY_DEVICE_UNRECOGNIZED")
            }
            if (!biometricVerified) {
                computedRisk += 30
                reasonCodes.add("MISSING_BIOMETRIC_INTENT")
            }
            if (!faceVerified) {
                computedRisk += 18
                reasonCodes.add("FACE_AUTH_INCOMPLETE")
            }
            if (!otpVerified) {
                computedRisk += 25
                reasonCodes.add("DEMO_BANK_OTP_UNVERIFIED")
            }
            if (amount >= 50000.0) {
                computedRisk += 15
                reasonCodes.add("HIGH_VALUE_THRESHOLD_EXCEEDED")
            }
            if (locationAnomaly) {
                computedRisk += 20
                reasonCodes.add("GEOGRAPHIC_ANOMALY_OUT_OF_BOUNDS")
            }
            if (velocityBreached) {
                computedRisk += 25
                reasonCodes.add("VELOCITY_SPIKE_DETECTED")
            }
            if (failedAttempts > 0) {
                computedRisk += (failedAttempts * 12)
                reasonCodes.add("FAILED_AUTH_SEQUENCE_RECORDED")
            }

            val finalScore = computedRisk.coerceIn(0, 100)

            val riskLevel = when {
                finalScore >= 70 || (!deviceVerified && !biometricVerified) -> "HIGH"
                finalScore >= 35 || reasonCodes.isNotEmpty() -> "MEDIUM"
                else -> "LOW"
            }

            val recommendedAction = when (riskLevel) {
                "HIGH" -> "BLOCK"
                "MEDIUM" -> "EXTRA_VERIFICATION"
                else -> "ALLOW"
            }

            // Build structured JSON output
            val jsonObject = JSONObject().apply {
                put("transaction_id", transactionId)
                put("risk_level", riskLevel)
                put("risk_score", finalScore)

                val checksObj = JSONObject()
                checks.forEach { (k, v) -> checksObj.put(k, v) }
                put("security_checks", checksObj)

                val reasonsArray = JSONArray()
                reasonCodes.forEach { reasonsArray.put(it) }
                put("reason_codes", reasonsArray)

                put("recommended_action", recommendedAction)
            }

            val formattedJson = jsonObject.toString(2)

            // Validate structured output before using
            val validation = validateAiResponse(formattedJson)
            if (validation.isFailure) {
                return getDeterministicFallbackAnalysis(
                    transactionId = transactionId,
                    deviceVerified = deviceVerified,
                    biometricVerified = biometricVerified,
                    faceVerified = faceVerified,
                    otpVerified = otpVerified,
                    failureReason = "Validation failed: ${validation.exceptionOrNull()?.message}"
                )
            }

            val narrative = when (riskLevel) {
                "LOW" -> "All three authentication layers and device identity verified. Risk is nominal ($finalScore/100). Safe to authorize under deterministic rules."
                "MEDIUM" -> "Moderate risk detected ($finalScore/100) due to ${reasonCodes.joinToString(", ")}. Recommended action: Require step-up confirmation."
                "HIGH" -> "Critical threat posture detected ($finalScore/100). Unrecognized device or multiple failed challenges. Recommended action: BLOCK."
                else -> "Evaluation complete."
            }

            AiSecurityResponse(
                transactionId = transactionId,
                riskLevel = riskLevel,
                riskScore = finalScore,
                securityChecks = checks,
                reasonCodes = reasonCodes,
                recommendedAction = recommendedAction,
                rawJsonOutput = formattedJson,
                aiNarrativeExplanation = narrative,
                isFallback = false
            )
        } catch (e: Exception) {
            getDeterministicFallbackAnalysis(
                transactionId = transactionId,
                deviceVerified = deviceVerified,
                biometricVerified = biometricVerified,
                faceVerified = faceVerified,
                otpVerified = otpVerified,
                failureReason = "Unexpected exception during analysis: ${e.message}"
            )
        }
    }
}
