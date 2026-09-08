package com.example.trustpay.authentication

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

/**
 * Authentication management for the secure payment prototype.
 * Handles user credentials, password hashing with salt, and failed attempt protection.
 *
 * NOTE: Educational/Demo implementation. Never store raw passwords or plain text credentials.
 */
object LoginManager {

    data class UserAccount(
        val userId: String,
        val email: String,
        val name: String,
        val passwordHash: String,
        val salt: String,
        val registeredDeviceId: String,
        val registeredDeviceName: String,
        val accountBalance: Double = 100000.0,
        val isLocked: Boolean = false,
        val failedLoginAttempts: Int = 0
    )

    private val userDatabase = mutableMapOf<String, UserAccount>()
    private val random = SecureRandom()

    init {
        // Seed default demo user accounts
        registerUser(
            email = "alex.pay@trustpay.demo",
            name = "Alex Vance (Treasury Lead)",
            plainPassword = "Password@123",
            deviceId = "DEV-PIXEL-7A-8942",
            deviceName = "Google Pixel 7a (Trusted Device)"
        )

        registerUser(
            email = "priya.sharma@trustpay.demo",
            name = "Priya Sharma (Merchant Operations)",
            plainPassword = "Password@123",
            deviceId = "DEV-GALAXY-S23-1102",
            deviceName = "Samsung Galaxy S23 (Trusted Device)"
        )
    }

    fun registerUser(
        email: String,
        name: String,
        plainPassword: String,
        deviceId: String,
        deviceName: String
    ): UserAccount {
        val salt = generateSalt()
        val hash = hashPassword(plainPassword, salt)
        val user = UserAccount(
            userId = "USR-" + UUID.randomUUID().toString().take(8).uppercase(),
            email = email.lowercase().trim(),
            name = name.trim(),
            passwordHash = hash,
            salt = salt,
            registeredDeviceId = deviceId,
            registeredDeviceName = deviceName
        )
        userDatabase[user.email] = user
        return user
    }

    fun authenticate(email: String, plainPassword: String): Result<UserAccount> {
        val cleanEmail = email.lowercase().trim()
        val user = userDatabase[cleanEmail] ?: return Result.failure(Exception("Account not found for email: $email"))

        if (user.isLocked) {
            return Result.failure(Exception("Account is temporarily locked due to repeated authentication failures."))
        }

        val computedHash = hashPassword(plainPassword, user.salt)
        return if (computedHash == user.passwordHash) {
            // Reset failed counter
            val updated = user.copy(failedLoginAttempts = 0)
            userDatabase[cleanEmail] = updated
            Result.success(updated)
        } else {
            val failedCount = user.failedLoginAttempts + 1
            val shouldLock = failedCount >= 5
            val updated = user.copy(failedLoginAttempts = failedCount, isLocked = shouldLock)
            userDatabase[cleanEmail] = updated
            val msg = if (shouldLock) "Account locked! Maximum failed attempts exceeded." else "Invalid credentials ($failedCount/5 attempts used)."
            Result.failure(Exception(msg))
        }
    }

    fun getUser(email: String): UserAccount? = userDatabase[email.lowercase().trim()]

    fun updateUserDevice(email: String, newDeviceId: String, newDeviceName: String) {
        val user = getUser(email) ?: return
        userDatabase[user.email] = user.copy(
            registeredDeviceId = newDeviceId,
            registeredDeviceName = newDeviceName
        )
    }

    fun updateBalance(email: String, newBalance: Double) {
        val user = getUser(email) ?: return
        userDatabase[user.email] = user.copy(accountBalance = newBalance)
    }

    fun getAllUsers(): List<UserAccount> = userDatabase.values.toList()

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$salt:$password"
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
