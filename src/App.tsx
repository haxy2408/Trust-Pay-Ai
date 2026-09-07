import React, { useState } from 'react';
import { AuthScreen, LoginFormState, RegisterFormState, UserAccount } from './types';
import { TransactionStorage } from './storage/transactionStorage';
import { generateSalt, hashPasswordWithSalt } from './security/crypto';
import { LoginScreen } from './components/LoginScreen';
import { RegisterScreen } from './components/RegisterScreen';
import { TrustPayScreen } from './components/TrustPayScreen';

export const App: React.FC = () => {
  // Theme state
  const [isDarkTheme, setIsDarkTheme] = useState<boolean>(() =>
    TransactionStorage.isDarkTheme()
  );

  // Screen and User State
  const [currentUser, setCurrentUser] = useState<UserAccount | null>(() =>
    TransactionStorage.getActiveUser()
  );
  const [currentScreen, setCurrentScreen] = useState<AuthScreen>(() =>
    TransactionStorage.getActiveUser() ? 'DASHBOARD' : 'LOGIN'
  );

  // Login Form State
  const [loginForm, setLoginForm] = useState<LoginFormState>({
    identifier: '',
    password: '',
    isPasswordVisible: false,
    identifierError: null,
    passwordError: null,
    errorMessage: null,
    successMessage: null,
  });

  // Register Form State
  const [registerForm, setRegisterForm] = useState<RegisterFormState>({
    fullName: '',
    email: '',
    mobileNumber: '',
    password: '',
    confirmPassword: '',
    isPasswordVisible: false,
    isConfirmPasswordVisible: false,
    fullNameError: null,
    emailError: null,
    mobileNumberError: null,
    passwordError: null,
    confirmPasswordError: null,
    errorMessage: null,
  });

  // Theme Toggle
  const handleToggleTheme = () => {
    setIsDarkTheme((prev) => {
      const next = !prev;
      TransactionStorage.saveDarkTheme(next);
      return next;
    });
  };

  // Logout
  const handleLogout = () => {
    TransactionStorage.setActiveUser(null);
    setCurrentUser(null);
    setCurrentScreen('LOGIN');
    setLoginForm((prev) => ({
      ...prev,
      identifier: '',
      password: '',
      errorMessage: null,
      successMessage: 'You have been logged out securely.',
    }));
  };

  // Fill Demo Credentials
  const handleFillDemoLogin = () => {
    setLoginForm((prev) => ({
      ...prev,
      identifier: 'rahul@example.com',
      password: 'password123',
      identifierError: null,
      passwordError: null,
      errorMessage: null,
    }));
  };

  // Handle Login Submission
  const handleLoginSubmit = () => {
    const id = loginForm.identifier.trim();
    const pwd = loginForm.password;

    let hasError = false;
    let idErr: string | null = null;
    let pwdErr: string | null = null;

    if (!id) {
      idErr = 'Email or mobile number is required';
      hasError = true;
    }

    if (!pwd) {
      pwdErr = 'Password is required';
      hasError = true;
    }

    if (hasError) {
      setLoginForm((prev) => ({
        ...prev,
        identifierError: idErr,
        passwordError: pwdErr,
        errorMessage: null,
      }));
      return;
    }

    // Authenticate against TransactionStorage
    const users = TransactionStorage.getUsers();
    const matchedUser = users.find(
      (u) =>
        u.email.toLowerCase() === id.toLowerCase() ||
        u.mobileNumber === id.replace(/\D/g, '')
    );

    if (!matchedUser) {
      setLoginForm((prev) => ({
        ...prev,
        errorMessage: 'Account not found. Please check your credentials or register.',
      }));
      return;
    }

    const testHash = hashPasswordWithSalt(pwd, matchedUser.salt);
    if (testHash !== matchedUser.passwordHash) {
      setLoginForm((prev) => ({
        ...prev,
        errorMessage: 'Invalid credentials. Please try again.',
      }));
      return;
    }

    // Login successful
    TransactionStorage.setActiveUser(matchedUser);
    setCurrentUser(matchedUser);
    setCurrentScreen('DASHBOARD');
    setLoginForm((prev) => ({
      ...prev,
      errorMessage: null,
      successMessage: null,
    }));
  };

  // Handle Registration Submission
  const handleRegisterSubmit = () => {
    const name = registerForm.fullName.trim();
    const email = registerForm.email.trim();
    const mobile = registerForm.mobileNumber.trim();
    const pwd = registerForm.password;
    const confirmPwd = registerForm.confirmPassword;

    let hasError = false;
    let nameErr: string | null = null;
    let emailErr: string | null = null;
    let mobileErr: string | null = null;
    let pwdErr: string | null = null;
    let confirmErr: string | null = null;

    if (!name) {
      nameErr = 'Full name is required';
      hasError = true;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email || !emailRegex.test(email)) {
      emailErr = 'Valid email address is required';
      hasError = true;
    }

    const cleanMobile = mobile.replace(/\D/g, '');
    if (cleanMobile.length !== 10) {
      mobileErr = '10-digit mobile number is required';
      hasError = true;
    }

    if (!pwd || pwd.length < 8) {
      pwdErr = 'Password must be at least 8 characters';
      hasError = true;
    }

    if (pwd !== confirmPwd) {
      confirmErr = 'Passwords do not match';
      hasError = true;
    }

    if (hasError) {
      setRegisterForm((prev) => ({
        ...prev,
        fullNameError: nameErr,
        emailError: emailErr,
        mobileNumberError: mobileErr,
        passwordError: pwdErr,
        confirmPasswordError: confirmErr,
        errorMessage: null,
      }));
      return;
    }

    // Check unique constraints
    const users = TransactionStorage.getUsers();
    if (users.some((u) => u.email.toLowerCase() === email.toLowerCase())) {
      setRegisterForm((prev) => ({
        ...prev,
        errorMessage: 'An account with this email address already exists.',
      }));
      return;
    }

    if (users.some((u) => u.mobileNumber === cleanMobile)) {
      setRegisterForm((prev) => ({
        ...prev,
        errorMessage: 'An account with this mobile number already exists.',
      }));
      return;
    }

    // Create user with SHA-256 salted hash
    const salt = generateSalt();
    const passwordHash = hashPasswordWithSalt(pwd, salt);
    const newUser: UserAccount = {
      fullName: name,
      email,
      mobileNumber: cleanMobile,
      passwordHash,
      salt,
      registeredAt: Date.now(),
    };

    TransactionStorage.saveUser(newUser);

    // Switch to Login screen with success notice
    setLoginForm({
      identifier: email,
      password: '',
      isPasswordVisible: false,
      identifierError: null,
      passwordError: null,
      errorMessage: null,
      successMessage: 'Account created successfully! Please sign in with your credentials.',
    });
    setCurrentScreen('LOGIN');
  };

  return (
    <>
      {currentScreen === 'LOGIN' && (
        <LoginScreen
          form={loginForm}
          isDarkTheme={isDarkTheme}
          onIdentifierChanged={(val) =>
            setLoginForm((prev) => ({ ...prev, identifier: val, identifierError: null }))
          }
          onPasswordChanged={(val) =>
            setLoginForm((prev) => ({ ...prev, password: val, passwordError: null }))
          }
          onTogglePasswordVisibility={() =>
            setLoginForm((prev) => ({ ...prev, isPasswordVisible: !prev.isPasswordVisible }))
          }
          onLogin={handleLoginSubmit}
          onFillDemo={handleFillDemoLogin}
          onNavigateToRegister={() => {
            setRegisterForm({
              fullName: '',
              email: '',
              mobileNumber: '',
              password: '',
              confirmPassword: '',
              isPasswordVisible: false,
              isConfirmPasswordVisible: false,
              fullNameError: null,
              emailError: null,
              mobileNumberError: null,
              passwordError: null,
              confirmPasswordError: null,
              errorMessage: null,
            });
            setCurrentScreen('REGISTER');
          }}
          onToggleTheme={handleToggleTheme}
        />
      )}

      {currentScreen === 'REGISTER' && (
        <RegisterScreen
          form={registerForm}
          isDarkTheme={isDarkTheme}
          onFullNameChanged={(val) =>
            setRegisterForm((prev) => ({ ...prev, fullName: val, fullNameError: null }))
          }
          onEmailChanged={(val) =>
            setRegisterForm((prev) => ({ ...prev, email: val, emailError: null }))
          }
          onMobileChanged={(val) =>
            setRegisterForm((prev) => ({ ...prev, mobileNumber: val, mobileNumberError: null }))
          }
          onPasswordChanged={(val) =>
            setRegisterForm((prev) => ({ ...prev, password: val, passwordError: null }))
          }
          onConfirmPasswordChanged={(val) =>
            setRegisterForm((prev) => ({
              ...prev,
              confirmPassword: val,
              confirmPasswordError: null,
            }))
          }
          onTogglePasswordVisibility={() =>
            setRegisterForm((prev) => ({
              ...prev,
              isPasswordVisible: !prev.isPasswordVisible,
            }))
          }
          onToggleConfirmPasswordVisibility={() =>
            setRegisterForm((prev) => ({
              ...prev,
              isConfirmPasswordVisible: !prev.isConfirmPasswordVisible,
            }))
          }
          onRegister={handleRegisterSubmit}
          onNavigateToLogin={() => setCurrentScreen('LOGIN')}
          onToggleTheme={handleToggleTheme}
        />
      )}

      {currentScreen === 'DASHBOARD' && (
        <TrustPayScreen
          currentUser={currentUser}
          isDarkTheme={isDarkTheme}
          onToggleTheme={handleToggleTheme}
          onLogout={handleLogout}
        />
      )}
    </>
  );
};
