package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.TransactionStorage
import com.example.model.AuthScreen
import com.example.model.FinalDecisionStatus
import com.example.model.VerificationStep
import com.example.viewmodel.TrustPayViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var context: Context
  private lateinit var storage: TransactionStorage
  private lateinit var viewModel: TrustPayViewModel

  @Before
  fun setUp() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    context = application
    storage = TransactionStorage(application)
    storage.clearHistory()
    storage.clearUsers()
    viewModel = TrustPayViewModel(application)
  }

  @Test
  fun `read string from context`() {
    val appName = context.getString(R.string.app_name)
    assertEquals("TrustPay AI", appName)
  }

  @Test
  fun `test initial state when no account exists opens Register screen`() {
    val state = viewModel.uiState.value
    assertEquals("When no account exists, app must open on REGISTER screen", AuthScreen.REGISTER, state.currentScreen)
    assertNull("No user should be authenticated initially", state.currentUser)
  }

  @Test
  fun `test registration validation requires all fields and valid formats`() {
    // Attempt registration with all empty fields
    val resultEmpty = viewModel.registerUser()
    assertFalse("Registration with empty fields must fail", resultEmpty)
    var form = viewModel.uiState.value.registerForm
    assertNotNull("Name error must be set", form.fullNameError)
    assertNotNull("Email error must be set", form.emailError)
    assertNotNull("Mobile error must be set", form.mobileNumberError)
    assertNotNull("Password error must be set", form.passwordError)
    assertNotNull("Confirm password error must be set", form.confirmPasswordError)

    // Invalid email
    viewModel.onRegisterFullNameChanged("Rahul Sharma")
    viewModel.onRegisterEmailChanged("invalid-email")
    viewModel.onRegisterMobileChanged("9876543210")
    viewModel.onRegisterPasswordChanged("SecurePass123")
    viewModel.onRegisterConfirmPasswordChanged("SecurePass123")
    assertFalse(viewModel.registerUser())
    assertTrue(viewModel.uiState.value.registerForm.emailError!!.contains("valid email"))

    // Mobile number not 10 digits
    viewModel.onRegisterEmailChanged("rahul@example.com")
    viewModel.onRegisterMobileChanged("12345") // Only 5 digits
    assertFalse(viewModel.registerUser())
    assertTrue(viewModel.uiState.value.registerForm.mobileNumberError!!.contains("10 digits"))

    // Password shorter than 8 characters
    viewModel.onRegisterMobileChanged("9876543210")
    viewModel.onRegisterPasswordChanged("short")
    viewModel.onRegisterConfirmPasswordChanged("short")
    assertFalse(viewModel.registerUser())
    assertTrue(viewModel.uiState.value.registerForm.passwordError!!.contains("at least 8 characters"))

    // Passwords do not match
    viewModel.onRegisterPasswordChanged("SecurePass123")
    viewModel.onRegisterConfirmPasswordChanged("DifferentPass123")
    assertFalse(viewModel.registerUser())
    assertTrue(viewModel.uiState.value.registerForm.confirmPasswordError!!.contains("must match"))
  }

  @Test
  fun `test successful registration securely hashes password and redirects to login`() {
    viewModel.onRegisterFullNameChanged("Priya Patel")
    viewModel.onRegisterEmailChanged("priya@trustpay.com")
    viewModel.onRegisterMobileChanged("9876543210")
    val plainPassword = "SuperSecurePassword123!"
    viewModel.onRegisterPasswordChanged(plainPassword)
    viewModel.onRegisterConfirmPasswordChanged(plainPassword)

    val registered = viewModel.registerUser()
    assertTrue("Valid registration must succeed", registered)

    // Account saved locally
    assertTrue("Local storage must now report accounts exist", storage.hasAnyUser())
    val savedUser = storage.findUserByIdentifier("priya@trustpay.com")
    assertNotNull("User account must be retrievable from local storage", savedUser)
    assertEquals("Priya Patel", savedUser!!.fullName)
    assertEquals("priya@trustpay.com", savedUser.email)
    assertEquals("9876543210", savedUser.mobileNumber)

    // Passwords must NEVER be stored as plain text
    assertNotEquals("Plain text password must never be stored in database", plainPassword, savedUser.passwordHash)
    assertTrue("Password hash must be 64-character SHA-256", savedUser.passwordHash.length == 64)
    assertNotNull("Salt must be present", savedUser.salt)

    // Redirect to Login screen with success message
    val state = viewModel.uiState.value
    assertEquals(AuthScreen.LOGIN, state.currentScreen)
    assertNotNull(state.loginForm.successMessage)
    assertEquals("priya@trustpay.com", state.loginForm.identifier)
  }

  @Test
  fun `test login fails with wrong credentials and succeeds with valid email or mobile`() {
    // 1. Register an account first
    viewModel.onRegisterFullNameChanged("Rahul Sharma")
    viewModel.onRegisterEmailChanged("rahul@example.com")
    viewModel.onRegisterMobileChanged("9123456780")
    viewModel.onRegisterPasswordChanged("Password123!")
    viewModel.onRegisterConfirmPasswordChanged("Password123!")
    viewModel.registerUser()

    // 2. Test wrong password
    viewModel.onLoginIdentifierChanged("rahul@example.com")
    viewModel.onLoginPasswordChanged("WrongPassword!")
    val wrongPassResult = viewModel.loginUser()
    assertFalse("Login with wrong password must fail", wrongPassResult)
    assertEquals("Invalid email/mobile number or password.", viewModel.uiState.value.loginForm.errorMessage)

    // 3. Test wrong identifier
    viewModel.onLoginIdentifierChanged("unknown@example.com")
    viewModel.onLoginPasswordChanged("Password123!")
    val wrongUserResult = viewModel.loginUser()
    assertFalse("Login with unknown identifier must fail", wrongUserResult)
    assertEquals("Invalid email/mobile number or password.", viewModel.uiState.value.loginForm.errorMessage)

    // 4. Test login with Email
    viewModel.onLoginIdentifierChanged("rahul@example.com")
    viewModel.onLoginPasswordChanged("Password123!")
    val loginWithEmailResult = viewModel.loginUser()
    assertTrue("Login with valid email and password must succeed", loginWithEmailResult)
    assertEquals(AuthScreen.DASHBOARD, viewModel.uiState.value.currentScreen)
    assertNotNull(viewModel.uiState.value.currentUser)
    assertEquals("Rahul Sharma", viewModel.uiState.value.currentUser!!.fullName)

    // 5. Test logout returns to login screen without wiping registered user
    viewModel.logoutUser()
    assertEquals(AuthScreen.LOGIN, viewModel.uiState.value.currentScreen)
    assertNull(viewModel.uiState.value.currentUser)
    assertTrue("Registered accounts must remain saved locally", storage.hasAnyUser())

    // 6. Test login with Mobile Number
    viewModel.onLoginIdentifierChanged("9123456780")
    viewModel.onLoginPasswordChanged("Password123!")
    val loginWithMobileResult = viewModel.loginUser()
    assertTrue("Login with valid mobile number and password must succeed", loginWithMobileResult)
    assertEquals(AuthScreen.DASHBOARD, viewModel.uiState.value.currentScreen)
    assertEquals("Rahul Sharma", viewModel.uiState.value.currentUser!!.fullName)
  }

  @Test
  fun `test high-frequency payment assigns random fingerprint challenge`() {
    // Fill rolling window to trigger velocity anomaly
    // 1st tx: ₹5,000
    viewModel.onRecipientChanged("test@okaxis")
    viewModel.onAmountChanged("5000")
    viewModel.analyseAndPay {}

    // 2nd tx: ₹5,000
    viewModel.analyseAndPay {}

    // 3rd tx: ₹5,000 -> triggers velocity anomaly (count 3, sum >= 15000)
    var biometricCallbackTriggered = false
    viewModel.analyseAndPay { biometricCallbackTriggered = true }

    val state = viewModel.uiState.value
    assertTrue("Biometric callback should be triggered", biometricCallbackTriggered)
    assertNotNull("VerificationState must be active", state.verificationState)
    assertTrue("Velocity anomaly flag must be true", state.verificationState!!.isVelocityAnomaly)
    assertNotNull("Random fingerprint challenge must be assigned", state.verificationState!!.designatedFinger)
    assertTrue(
      "Designated finger must be in allowed list",
      TrustPayViewModel.AVAILABLE_FINGERS.contains(state.verificationState!!.designatedFinger)
    )
    assertEquals(VerificationStep.BIOMETRIC, state.verificationState!!.currentStep)
  }

  @Test
  fun `test tamper attack demo blocks with Transaction tampering detected message`() {
    viewModel.onRecipientChanged("merchant@upi")
    viewModel.onAmountChanged("5000")
    viewModel.triggerTamperAttackDemo()

    val state = viewModel.uiState.value
    assertEquals(FinalDecisionStatus.TAMPER_DETECTED, state.finalDecision)
    assertNotNull(state.decisionMessage)
    assertTrue(
      "Decision message must contain 'Transaction tampering detected'",
      state.decisionMessage!!.contains("Transaction tampering detected")
    )
  }

  @Test
  fun `test theme toggle persists and switches state`() {
    val initialDark = viewModel.uiState.value.isDarkTheme
    viewModel.toggleTheme()
    assertEquals(!initialDark, viewModel.uiState.value.isDarkTheme)
    assertEquals(!initialDark, storage.isDarkTheme())

    // Toggle back
    viewModel.toggleTheme()
    assertEquals(initialDark, viewModel.uiState.value.isDarkTheme)
    assertEquals(initialDark, storage.isDarkTheme())
  }
}
