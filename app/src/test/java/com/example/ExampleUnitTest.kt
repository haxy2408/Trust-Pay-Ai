package com.example

import com.example.model.AuthenticatedTransactionSnapshot
import com.example.security.CryptoHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testSha256BindingIntegrity() {
    val txId = "TXN-DEMO-001"
    val recipient = "rahul@okhdfcbank"
    val amount = 5000.0

    val hash = CryptoHelper.computeBindingHash(txId, recipient, amount, "INR")
    assertTrue("Hash must be 64-char hex string", hash.length == 64)

    // Verification with same params succeeds
    val isVerified = CryptoHelper.verifyBinding(hash, txId, recipient, amount, "INR")
    assertTrue("Binding verification should succeed with unchanged parameters", isVerified)
  }

  @Test
  fun testTamperAmountDetection() {
    val txId = "TXN-DEMO-002"
    val recipient = "rahul@okhdfcbank"
    val originalAmount = 5000.0
    val tamperedAmount = 50000.0

    val boundHash = CryptoHelper.computeBindingHash(txId, recipient, originalAmount, "INR")

    // Simulate tampering attack
    val isVerified = CryptoHelper.verifyBinding(boundHash, txId, recipient, tamperedAmount, "INR")
    assertFalse("Tampered amount must fail cryptographic verification", isVerified)
  }

  @Test
  fun testTamperRecipientDetection() {
    val txId = "TXN-DEMO-003"
    val legitimateRecipient = "merchant@upi"
    val attackerRecipient = "attacker@upi"
    val amount = 2500.0

    val boundHash = CryptoHelper.computeBindingHash(txId, legitimateRecipient, amount, "INR")

    // Simulate recipient hijack attack
    val isVerified = CryptoHelper.verifyBinding(boundHash, txId, attackerRecipient, amount, "INR")
    assertFalse("Tampered recipient must fail cryptographic verification", isVerified)
  }

  @Test
  fun testVerifyTransactionIntegrityAllParametersMatch() {
    val txId = "TXN-DEMO-AUTH-01"
    val recipient = "priya@okaxis"
    val amount = 15000.0
    val currency = "INR"
    val boundHash = CryptoHelper.computeBindingHash(txId, recipient, amount, currency)

    val snapshot = AuthenticatedTransactionSnapshot(
      transactionId = txId,
      recipient = recipient,
      amount = amount,
      currency = currency,
      boundHash = boundHash,
      authTimestamp = System.currentTimeMillis(),
      biometricVerified = true,
      designatedFinger = "Right Index Finger"
    )

    val result = CryptoHelper.verifyTransactionIntegrity(
      snapshot = snapshot,
      candidateTransactionId = txId,
      candidateRecipient = recipient,
      candidateAmount = amount,
      candidateCurrency = currency
    )

    assertTrue("Integrity verification must pass when all candidate parameters match snapshot", result.isValid)
    assertTrue("Discrepancy description should be null", result.discrepancyDescription == null)
  }

  @Test
  fun testVerifyTransactionIntegrityPostAuthAmountTampered() {
    val txId = "TXN-DEMO-AUTH-02"
    val recipient = "priya@okaxis"
    val amount = 15000.0
    val currency = "INR"
    val boundHash = CryptoHelper.computeBindingHash(txId, recipient, amount, currency)

    val snapshot = AuthenticatedTransactionSnapshot(
      transactionId = txId,
      recipient = recipient,
      amount = amount,
      currency = currency,
      boundHash = boundHash,
      authTimestamp = System.currentTimeMillis(),
      biometricVerified = true,
      designatedFinger = "Left Thumb"
    )

    // Attacker modifies amount after authentication
    val tamperedAmount = 150000.0
    val result = CryptoHelper.verifyTransactionIntegrity(
      snapshot = snapshot,
      candidateTransactionId = txId,
      candidateRecipient = recipient,
      candidateAmount = tamperedAmount,
      candidateCurrency = currency
    )

    assertFalse("Integrity check must fail when amount is altered post-auth", result.isValid)
    assertNotNull(result.discrepancyDescription)
    assertTrue(
      "Description must specify amount mismatch",
      result.discrepancyDescription!!.contains("Amount mismatch")
    )
  }

  @Test
  fun testVerifyTransactionIntegrityPostAuthRecipientTampered() {
    val txId = "TXN-DEMO-AUTH-03"
    val recipient = "priya@okaxis"
    val amount = 5000.0
    val currency = "INR"
    val boundHash = CryptoHelper.computeBindingHash(txId, recipient, amount, currency)

    val snapshot = AuthenticatedTransactionSnapshot(
      transactionId = txId,
      recipient = recipient,
      amount = amount,
      currency = currency,
      boundHash = boundHash,
      authTimestamp = System.currentTimeMillis(),
      biometricVerified = true,
      designatedFinger = "Right Thumb"
    )

    // Attacker redirects payee after biometric auth
    val hijackedRecipient = "attacker@fraudbank"
    val result = CryptoHelper.verifyTransactionIntegrity(
      snapshot = snapshot,
      candidateTransactionId = txId,
      candidateRecipient = hijackedRecipient,
      candidateAmount = amount,
      candidateCurrency = currency
    )

    assertFalse("Integrity check must fail when recipient UPI ID is altered post-auth", result.isValid)
    assertNotNull(result.discrepancyDescription)
    assertTrue(
      "Description must specify recipient mismatch",
      result.discrepancyDescription!!.contains("Recipient UPI ID mismatch")
    )
  }

  @Test
  fun testVerifyTransactionIntegrityCurrencyAndTxIdTampered() {
    val txId = "TXN-DEMO-AUTH-04"
    val recipient = "priya@okaxis"
    val amount = 5000.0
    val currency = "INR"
    val boundHash = CryptoHelper.computeBindingHash(txId, recipient, amount, currency)

    val snapshot = AuthenticatedTransactionSnapshot(
      transactionId = txId,
      recipient = recipient,
      amount = amount,
      currency = currency,
      boundHash = boundHash,
      authTimestamp = System.currentTimeMillis(),
      biometricVerified = true,
      designatedFinger = null
    )

    val currencyResult = CryptoHelper.verifyTransactionIntegrity(
      snapshot = snapshot,
      candidateTransactionId = txId,
      candidateRecipient = recipient,
      candidateAmount = amount,
      candidateCurrency = "USD"
    )
    assertFalse(currencyResult.isValid)
    assertTrue(currencyResult.discrepancyDescription!!.contains("Currency mismatch"))

    val txIdResult = CryptoHelper.verifyTransactionIntegrity(
      snapshot = snapshot,
      candidateTransactionId = "TXN-DIFF-99",
      candidateRecipient = recipient,
      candidateAmount = amount,
      candidateCurrency = currency
    )
    assertFalse(txIdResult.isValid)
    assertTrue(txIdResult.discrepancyDescription!!.contains("Transaction ID mismatch"))
  }

  @Test
  fun testAuthSecuritySaltAndHash() {
    val salt1 = com.example.security.AuthSecurityUtil.generateSalt()
    val salt2 = com.example.security.AuthSecurityUtil.generateSalt()
    assertNotNull(salt1)
    assertEquals(32, salt1.length) // 16 bytes in hex = 32 chars
    assertNotEquals("Two salts should be unique", salt1, salt2)

    val password = "SecurePassword123!"
    val hash1 = com.example.security.AuthSecurityUtil.hashPassword(password, salt1)
    val hash2 = com.example.security.AuthSecurityUtil.hashPassword(password, salt1)
    val hash3 = com.example.security.AuthSecurityUtil.hashPassword(password, salt2)

    assertEquals("Hash length for SHA-256 must be 64 characters", 64, hash1.length)
    assertEquals("Deterministic hashing for identical password & salt", hash1, hash2)
    assertNotEquals("Different salts must yield different hashes", hash1, hash3)

    // Verification
    assertTrue("Correct password verification must succeed",
      com.example.security.AuthSecurityUtil.verifyPassword(password, salt1, hash1))
    assertFalse("Incorrect password verification must fail",
      com.example.security.AuthSecurityUtil.verifyPassword("WrongPassword", salt1, hash1))
  }

  @Test
  fun testAuthValidationHelpers() {
    // Email validation
    assertTrue(com.example.security.AuthSecurityUtil.isValidEmail("user@example.com"))
    assertTrue(com.example.security.AuthSecurityUtil.isValidEmail("rahul.sharma@upi.bank"))
    assertFalse(com.example.security.AuthSecurityUtil.isValidEmail("plainaddress"))
    assertFalse(com.example.security.AuthSecurityUtil.isValidEmail("@missingusername.com"))
    assertFalse(com.example.security.AuthSecurityUtil.isValidEmail(""))

    // Mobile validation (must contain 10 digits)
    assertTrue(com.example.security.AuthSecurityUtil.isValidMobile("9876543210"))
    assertTrue(com.example.security.AuthSecurityUtil.isValidMobile("+91 98765 43210"))
    assertFalse(com.example.security.AuthSecurityUtil.isValidMobile("12345"))
    assertFalse(com.example.security.AuthSecurityUtil.isValidMobile("123456789012"))
    assertFalse(com.example.security.AuthSecurityUtil.isValidMobile(""))
  }
}

