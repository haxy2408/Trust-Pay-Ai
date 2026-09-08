package com.example.trustpay.authentication

import java.util.UUID

/**
 * Session Security Manager.
 * Protects active authentication sessions, enforces expiration (TTL),
 * and provides failed-attempt rate limiting.
 */
object SessionSecurity {

    data class SessionToken(
        val token: String,
        val userEmail: String,
        val createdAt: Long,
        val expiresAt: Long,
        val deviceId: String
    )

    private const val SESSION_TTL_MS = 30 * 60 * 1000L // 30 minutes
    private var currentSession: SessionToken? = null
    private val failedAttemptsTracker = mutableMapOf<String, Int>()

    fun startSession(userEmail: String, deviceId: String): SessionToken {
        val now = System.currentTimeMillis()
        val token = SessionToken(
            token = "SESS-" + UUID.randomUUID().toString(),
            userEmail = userEmail,
            createdAt = now,
            expiresAt = now + SESSION_TTL_MS,
            deviceId = deviceId
        )
        currentSession = token
        failedAttemptsTracker.remove(userEmail)
        return token
    }

    fun getCurrentSession(): SessionToken? {
        val session = currentSession ?: return null
        if (System.currentTimeMillis() > session.expiresAt) {
            invalidateSession()
            return null
        }
        return session
    }

    fun isSessionValid(): Boolean {
        return getCurrentSession() != null
    }

    fun refreshSession(): SessionToken? {
        val session = currentSession ?: return null
        val updated = session.copy(expiresAt = System.currentTimeMillis() + SESSION_TTL_MS)
        currentSession = updated
        return updated
    }

    fun invalidateSession() {
        currentSession = null
    }

    fun recordFailedAttempt(key: String): Int {
        val count = (failedAttemptsTracker[key] ?: 0) + 1
        failedAttemptsTracker[key] = count
        return count
    }

    fun resetFailedAttempts(key: String) {
        failedAttemptsTracker.remove(key)
    }

    fun getFailedAttempts(key: String): Int = failedAttemptsTracker[key] ?: 0
}
