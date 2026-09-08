package com.example.trustpay

import com.example.trustpay.ai.GeminiSecurityAnalysis
import com.example.trustpay.security.DemoBankOtp
import com.example.trustpay.security.DeviceRecognition
import com.example.trustpay.transactions.TransactionCreation
import org.junit.Assert.*
import org.junit.Test

class SecurityPipelineTest {

    @Test
    fun testTransactionCreationValidation() {
        val validReq = TransactionCreation.CreationRequest(
            senderEmail = "alex.pay@trustpay.demo",
            recipientVpa = "merchant@icici",
            amount = 1500.0
        )
        val validRes = TransactionCreation.createTransaction(validReq)
        assertTrue(validRes.isValid)
        assertTrue(validRes.transactionId.startsWith("TXN-"))
        assertNotNull(validRes.bindingHash)

        val invalidAmtReq = TransactionCreation.CreationRequest(
            senderEmail = "alex.pay@trustpay.demo",
            recipientVpa = "merchant@icici",
            amount = -10.0
        )
        val invalidAmtRes = TransactionCreation.createTransaction(invalidAmtReq)
        assertFalse(invalidAmtRes.isValid)

        val invalidRecipientReq = TransactionCreation.CreationRequest(
            senderEmail = "alex.pay@trustpay.demo",
            recipientVpa = "invalid",
            amount = 100.0
        )
        val invalidRecipientRes = TransactionCreation.createTransaction(invalidRecipientReq)
        assertFalse(invalidRecipientRes.isValid)
    }

    @Test
    fun testDemoBankOtpLifecycle() {
        val session = DemoBankOtp.generateNewOtp()
        assertEquals(6, session.demoPlainOtpForDisplay.length)
        assertTrue(session.demoPlainOtpForDisplay.all { it.isDigit() })

        // Verify wrong OTP counts down attempts
        val wrongAttempt = DemoBankOtp.verifyOtp("000000")
        if (session.demoPlainOtpForDisplay != "000000") {
            assertFalse(wrongAttempt.otpVerified)
        }

        // Verify correct OTP succeeds
        val rightAttempt = DemoBankOtp.verifyOtp(session.demoPlainOtpForDisplay)
        assertTrue(rightAttempt.otpVerified)
    }

    @Test
    fun testAiJsonValidationAndFallback() {
        val validJson = """
            {
                "transaction_id": "TXN-12345",
                "risk_level": "LOW",
                "risk_score": 15,
                "reason_codes": [],
                "recommended_action": "ALLOW"
            }
        """.trimIndent()

        val validRes = GeminiSecurityAnalysis.validateAiResponse(validJson)
        assertTrue(validRes.isSuccess)
        assertEquals("LOW", validRes.getOrNull()?.riskLevel)
        assertEquals(15, validRes.getOrNull()?.riskScore)
        assertEquals("ALLOW", validRes.getOrNull()?.recommendedAction)

        val invalidJson = """
            {
                "transaction_id": "TXN-12345",
                "risk_level": "UNKNOWN_LEVEL"
            }
        """.trimIndent()
        val invalidRes = GeminiSecurityAnalysis.validateAiResponse(invalidJson)
        assertTrue(invalidRes.isFailure)

        // Fallback test
        val fallback = GeminiSecurityAnalysis.getDeterministicFallbackAnalysis(
            transactionId = "TXN-999",
            deviceVerified = false,
            biometricVerified = false,
            faceVerified = false,
            otpVerified = false,
            failureReason = "Test network timeout"
        )
        assertEquals("HIGH", fallback.riskLevel)
        assertEquals("BLOCK", fallback.recommendedAction)
        assertTrue(fallback.isFallback)
    }
}
