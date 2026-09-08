package com.example.trustpay

import com.example.trustpay.backend.SecondSignatureApi
import com.example.trustpay.model.SecondSignatureStatus
import com.example.trustpay.security.SecondSignatureEngine
import com.example.trustpay.storage.TrustPayStorage
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SecondSignatureEngineTest {

    @Before
    fun setUp() {
        // Ensure active user is initialized
        val users = TrustPayStorage.getUsers()
        TrustPayStorage.setActiveUser(users.first())
        TrustPayStorage.setBalance(100000.0, "rahul@example.com")
        TrustPayStorage.setBalance(65000.0, "priya@example.com")
    }

    @Test
    fun testFingerprintDeterminismAndBinding() {
        val fp1 = SecondSignatureEngine.computeTransactionFingerprint(
            transactionId = "TXN_TEST_001",
            senderId = "rahul@example.com",
            recipientId = "merchant@bank",
            amount = 50000.0,
            currency = "INR",
            createdAt = 1000000L,
            transactionType = "HIGH_VALUE_PAYMENT",
            transactionVersion = 1,
            nonce = "NONCE_ABC"
        )

        val fp2 = SecondSignatureEngine.computeTransactionFingerprint(
            transactionId = "TXN_TEST_001",
            senderId = "rahul@example.com",
            recipientId = "merchant@bank",
            amount = 50000.0,
            currency = "INR",
            createdAt = 1000000L,
            transactionType = "HIGH_VALUE_PAYMENT",
            transactionVersion = 1,
            nonce = "NONCE_ABC"
        )

        assertEquals("Reconstructed fingerprints must be deterministic", fp1, fp2)

        // Altering amount must produce different fingerprint
        val fpTampered = SecondSignatureEngine.computeTransactionFingerprint(
            transactionId = "TXN_TEST_001",
            senderId = "rahul@example.com",
            recipientId = "merchant@bank",
            amount = 99999.0, // Modified!
            currency = "INR",
            createdAt = 1000000L,
            transactionType = "HIGH_VALUE_PAYMENT",
            transactionVersion = 1,
            nonce = "NONCE_ABC"
        )

        assertNotEquals("Tampered amount must alter transaction fingerprint", fp1, fpTampered)
    }

    @Test
    fun testSuccessfulSecondSignatureAuthorization() {
        val createResult = SecondSignatureApi.createTransaction(
            senderEmail = "rahul@example.com",
            senderName = "Rahul Sharma",
            recipientId = "datacenter.corp@icici",
            amount = 40000.0,
            transactionType = "CORPORATE_PAYMENT",
            purposeDescription = "Server Migration",
            designatedSecondSignerId = "priya@example.com",
            designatedSecondSignerName = "Priya Patel",
            designatedSecondSignerRole = "CFO"
        )

        assertTrue(createResult.success)
        val tx = createResult.transaction
        assertNotNull(tx)
        assertEquals(SecondSignatureStatus.SECOND_SIGNATURE_REQUIRED, tx!!.status)
        assertNotNull(tx.firstSignature)

        // Second signer approves independently
        val approveResult = SecondSignatureApi.submitSecondSignature(
            transactionId = tx.id,
            signerEmail = "priya@example.com",
            signerName = "Priya Patel",
            signerRole = "CFO"
        )

        assertTrue("Approval by distinct co-signer must succeed", approveResult.success)
        assertEquals(SecondSignatureStatus.EXECUTED, approveResult.transaction?.status)
        assertNotNull(approveResult.transaction?.secondSignature)
    }

    @Test
    fun testBlockSameUserDoubleApproval() {
        val createResult = SecondSignatureApi.createTransaction(
            senderEmail = "rahul@example.com",
            senderName = "Rahul Sharma",
            recipientId = "vendor@hdfc",
            amount = 25000.0,
            transactionType = "HIGH_VALUE_PAYMENT",
            purposeDescription = "Hardware supply",
            designatedSecondSignerId = "priya@example.com",
            designatedSecondSignerName = "Priya Patel",
            designatedSecondSignerRole = "CFO"
        )

        assertTrue(createResult.success)
        val tx = createResult.transaction!!

        // Rahul attempts self-approval as second signer
        val selfApproveResult = SecondSignatureApi.submitSecondSignature(
            transactionId = tx.id,
            signerEmail = "rahul@example.com", // Same user!
            signerName = "Rahul Sharma"
        )

        assertFalse("Same user double approval must be blocked", selfApproveResult.success)
        assertEquals("SAME_USER_DOUBLE_APPROVAL_BLOCKED", selfApproveResult.errorCode)
    }

    @Test
    fun testDetectInFlightAmountTampering() {
        val createResult = SecondSignatureApi.createTransaction(
            senderEmail = "rahul@example.com",
            senderName = "Rahul Sharma",
            recipientId = "supplier@bank",
            amount = 30000.0,
            transactionType = "CORPORATE_PAYMENT",
            purposeDescription = "Logistics",
            designatedSecondSignerId = "priya@example.com",
            designatedSecondSignerName = "Priya Patel"
        )

        assertTrue(createResult.success)
        val tx = createResult.transaction!!

        // Simulate amount altered in-flight before second signature
        val tamperResult = SecondSignatureApi.submitSecondSignature(
            transactionId = tx.id,
            signerEmail = "priya@example.com",
            signerName = "Priya Patel",
            simulatedTamperedAmount = 90000.0 // Tampered!
        )

        assertFalse("Tampered amount must be rejected", tamperResult.success)
        assertEquals("TRANSACTION_TAMPERED", tamperResult.errorCode)
        assertEquals(SecondSignatureStatus.TAMPER_DETECTED, tamperResult.transaction?.status)
    }

    @Test
    fun testReplayAttackProtection() {
        val createResult = SecondSignatureApi.createTransaction(
            senderEmail = "rahul@example.com",
            senderName = "Rahul Sharma",
            recipientId = "escrow@bank",
            amount = 15000.0,
            transactionType = "JOINT_ACCOUNT",
            purposeDescription = "Joint escrow",
            designatedSecondSignerId = "priya@example.com",
            designatedSecondSignerName = "Priya Patel"
        )

        assertTrue(createResult.success)
        val tx = createResult.transaction!!

        // First approval succeeds
        val res1 = SecondSignatureApi.submitSecondSignature(
            transactionId = tx.id,
            signerEmail = "priya@example.com",
            signerName = "Priya Patel"
        )
        assertTrue(res1.success)

        // Attempt second approval on same transaction (Replay)
        val res2 = SecondSignatureApi.submitSecondSignature(
            transactionId = tx.id,
            signerEmail = "priya@example.com",
            signerName = "Priya Patel"
        )
        assertFalse("Replay attempt on executed transaction must fail", res2.success)
    }
}
