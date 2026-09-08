package com.example.trustpay.backend

import com.example.trustpay.authentication.LoginManager
import com.example.trustpay.authentication.SessionSecurity

/**
 * Backend Authentication API.
 * Simulates server-side endpoints for authentication, session verification,
 * and device binding.
 */
object AuthenticationApi {

    data class AuthResponse(
        val success: Boolean,
        val user: LoginManager.UserAccount?,
        val sessionToken: String?,
        val errorMessage: String? = null
    )

    fun login(email: String, password: String, deviceId: String): AuthResponse {
        val result = LoginManager.authenticate(email, password)
        return if (result.isSuccess) {
            val user = result.getOrNull()!!
            val session = SessionSecurity.startSession(user.email, deviceId)
            SecurityApi.logSecurityEvent(
                eventType = "USER_LOGIN_SUCCESS",
                description = "User ${user.email} authenticated successfully from device $deviceId",
                severity = "INFO"
            )
            AuthResponse(true, user, session.token)
        } else {
            val err = result.exceptionOrNull()?.message ?: "Login failed"
            SecurityApi.logSecurityEvent(
                eventType = "USER_LOGIN_FAILED",
                description = "Failed login attempt for $email: $err",
                severity = "WARNING"
            )
            AuthResponse(false, null, null, err)
        }
    }

    fun registerDeviceToUser(userEmail: String, newDeviceId: String, newDeviceName: String): Boolean {
        LoginManager.updateUserDevice(userEmail, newDeviceId, newDeviceName)
        SecurityApi.logSecurityEvent(
            eventType = "DEVICE_REGISTERED",
            description = "New device $newDeviceName ($newDeviceId) bound to user $userEmail",
            severity = "INFO"
        )
        return true
    }

    fun logout(): Boolean {
        val session = SessionSecurity.getCurrentSession()
        if (session != null) {
            SecurityApi.logSecurityEvent(
                eventType = "USER_LOGOUT",
                description = "Session ${session.token} invalidated for ${session.userEmail}",
                severity = "INFO"
            )
            SessionSecurity.invalidateSession()
        }
        return true
    }
}
