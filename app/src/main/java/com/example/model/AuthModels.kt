package com.example.model

/**
 * Screen routing for authentication vs main payment dashboard.
 */
enum class AuthScreen {
    REGISTER,
    LOGIN,
    DASHBOARD
}

/**
 * User account stored locally with securely hashed credentials and salt.
 * Passwords are never stored as plain text.
 */
data class UserAccount(
    val fullName: String,
    val email: String,
    val mobileNumber: String,
    val passwordHash: String,
    val salt: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * State container for user registration form fields and validation errors.
 */
data class RegisterFormState(
    val fullName: String = "",
    val email: String = "",
    val mobileNumber: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullNameError: String? = null,
    val emailError: String? = null,
    val mobileNumberError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false
)

/**
 * State container for user login form fields and validation errors.
 */
data class LoginFormState(
    val identifier: String = "", // Email or Mobile Number
    val password: String = "",
    val identifierError: String? = null,
    val passwordError: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isPasswordVisible: Boolean = false
)
