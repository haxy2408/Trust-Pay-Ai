import React from 'react';
import { Shield, User, Mail, Phone, Lock, Eye, EyeOff, Sun, Moon, ArrowRight, CheckCircle2 } from 'lucide-react';
import { RegisterFormState } from '../types';

interface RegisterScreenProps {
  form: RegisterFormState;
  isDarkTheme: boolean;
  onFullNameChanged: (value: string) => void;
  onEmailChanged: (value: string) => void;
  onMobileChanged: (value: string) => void;
  onPasswordChanged: (value: string) => void;
  onConfirmPasswordChanged: (value: string) => void;
  onTogglePasswordVisibility: () => void;
  onToggleConfirmPasswordVisibility: () => void;
  onRegister: () => void;
  onNavigateToLogin: () => void;
  onToggleTheme: () => void;
}

export const RegisterScreen: React.FC<RegisterScreenProps> = ({
  form,
  isDarkTheme,
  onFullNameChanged,
  onEmailChanged,
  onMobileChanged,
  onPasswordChanged,
  onConfirmPasswordChanged,
  onTogglePasswordVisibility,
  onToggleConfirmPasswordVisibility,
  onRegister,
  onNavigateToLogin,
  onToggleTheme,
}) => {
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onRegister();
  };

  return (
    <div
      className={`min-h-screen flex flex-col items-center justify-center p-4 transition-colors duration-200 ${
        isDarkTheme ? 'bg-slate-950 text-slate-100' : 'bg-slate-100 text-slate-900'
      }`}
    >
      {/* Top bar with theme toggle */}
      <div className="w-full max-w-lg flex items-center justify-between mb-4 px-2">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-cyan-500/20 border border-cyan-500/40 flex items-center justify-center text-cyan-400">
            <Shield className="w-5 h-5" />
          </div>
          <span className="font-extrabold tracking-wider text-sm text-cyan-500">TRUSTPAY</span>
        </div>

        <button
          id="btn_theme_toggle_reg"
          data-testid="btn_theme_toggle_reg"
          onClick={onToggleTheme}
          aria-label="Toggle Theme"
          className={`p-2 rounded-xl border transition-colors ${
            isDarkTheme
              ? 'bg-slate-900 border-slate-800 text-amber-400 hover:border-slate-700'
              : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-50'
          }`}
        >
          {isDarkTheme ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
        </button>
      </div>

      <div
        className={`w-full max-w-lg rounded-2xl border shadow-2xl p-6 md:p-8 transition-all ${
          isDarkTheme
            ? 'bg-slate-900/90 border-slate-800 text-white'
            : 'bg-white border-slate-200 text-slate-900'
        }`}
      >
        <div className="flex flex-col gap-6">
          {/* Header */}
          <div className="flex flex-col gap-1">
            <h2 className="text-xl font-extrabold tracking-tight">Create Secure Account</h2>
            <p className="text-xs text-slate-400">
              Register to enable fraud detection, velocity anomaly monitoring, and salted cryptographic verification.
            </p>
          </div>

          {/* Error Banner */}
          {form.errorMessage && (
            <div
              id="banner_register_error"
              data-testid="banner_register_error"
              className="p-3 rounded-xl bg-red-500/15 border border-red-500/40 text-red-300 text-xs font-medium"
            >
              {form.errorMessage}
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            {/* 1. Full Name */}
            <div>
              <label className="block text-xs font-semibold text-slate-400 mb-1.5">Full Name</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
                  <User className="w-4 h-4 text-cyan-500" />
                </div>
                <input
                  id="input_register_name"
                  data-testid="input_register_name"
                  type="text"
                  value={form.fullName}
                  onChange={(e) => onFullNameChanged(e.target.value)}
                  placeholder="e.g. Rahul Sharma"
                  className={`w-full pl-9 pr-3 py-2.5 rounded-xl text-sm border outline-none transition-colors ${
                    form.fullNameError
                      ? 'border-red-500 focus:border-red-400'
                      : isDarkTheme
                      ? 'bg-slate-950 border-slate-700 text-white placeholder-slate-500 focus:border-cyan-400'
                      : 'bg-slate-50 border-slate-300 text-slate-900 placeholder-slate-400 focus:border-sky-600'
                  }`}
                />
              </div>
              {form.fullNameError && (
                <div
                  id="error_register_name"
                  data-testid="error_register_name"
                  className="text-xs text-red-400 mt-1 pl-1"
                >
                  {form.fullNameError}
                </div>
              )}
            </div>

            {/* 2. Email */}
            <div>
              <label className="block text-xs font-semibold text-slate-400 mb-1.5">Email Address</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
                  <Mail className="w-4 h-4 text-cyan-500" />
                </div>
                <input
                  id="input_register_email"
                  data-testid="input_register_email"
                  type="email"
                  value={form.email}
                  onChange={(e) => onEmailChanged(e.target.value)}
                  placeholder="e.g. rahul@example.com"
                  className={`w-full pl-9 pr-3 py-2.5 rounded-xl text-sm border outline-none transition-colors ${
                    form.emailError
                      ? 'border-red-500 focus:border-red-400'
                      : isDarkTheme
                      ? 'bg-slate-950 border-slate-700 text-white placeholder-slate-500 focus:border-cyan-400'
                      : 'bg-slate-50 border-slate-300 text-slate-900 placeholder-slate-400 focus:border-sky-600'
                  }`}
                />
              </div>
              {form.emailError && (
                <div
                  id="error_register_email"
                  data-testid="error_register_email"
                  className="text-xs text-red-400 mt-1 pl-1"
                >
                  {form.emailError}
                </div>
              )}
            </div>

            {/* 3. Mobile Number */}
            <div>
              <label className="block text-xs font-semibold text-slate-400 mb-1.5">Mobile Number</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
                  <Phone className="w-4 h-4 text-cyan-500" />
                </div>
                <span className="absolute inset-y-0 left-9 flex items-center text-xs font-bold text-slate-400">
                  +91
                </span>
                <input
                  id="input_register_mobile"
                  data-testid="input_register_mobile"
                  type="tel"
                  maxLength={10}
                  value={form.mobileNumber}
                  onChange={(e) => onMobileChanged(e.target.value.replace(/\D/g, ''))}
                  placeholder="10-digit mobile number"
                  className={`w-full pl-16 pr-3 py-2.5 rounded-xl text-sm border outline-none transition-colors ${
                    form.mobileNumberError
                      ? 'border-red-500 focus:border-red-400'
                      : isDarkTheme
                      ? 'bg-slate-950 border-slate-700 text-white placeholder-slate-500 focus:border-cyan-400'
                      : 'bg-slate-50 border-slate-300 text-slate-900 placeholder-slate-400 focus:border-sky-600'
                  }`}
                />
              </div>
              {form.mobileNumberError && (
                <div
                  id="error_register_mobile"
                  data-testid="error_register_mobile"
                  className="text-xs text-red-400 mt-1 pl-1"
                >
                  {form.mobileNumberError}
                </div>
              )}
            </div>

            {/* 4. Password */}
            <div>
              <label className="block text-xs font-semibold text-slate-400 mb-1.5">Password</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
                  <Lock className="w-4 h-4 text-cyan-500" />
                </div>
                <input
                  id="input_register_password"
                  data-testid="input_register_password"
                  type={form.isPasswordVisible ? 'text' : 'password'}
                  value={form.password}
                  onChange={(e) => onPasswordChanged(e.target.value)}
                  placeholder="At least 8 characters"
                  className={`w-full pl-9 pr-10 py-2.5 rounded-xl text-sm border outline-none transition-colors ${
                    form.passwordError
                      ? 'border-red-500 focus:border-red-400'
                      : isDarkTheme
                      ? 'bg-slate-950 border-slate-700 text-white placeholder-slate-500 focus:border-cyan-400'
                      : 'bg-slate-50 border-slate-300 text-slate-900 placeholder-slate-400 focus:border-sky-600'
                  }`}
                />
                <button
                  id="btn_toggle_reg_password"
                  data-testid="btn_toggle_reg_password"
                  type="button"
                  onClick={onTogglePasswordVisibility}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-200"
                >
                  {form.isPasswordVisible ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {form.passwordError && (
                <div
                  id="error_register_password"
                  data-testid="error_register_password"
                  className="text-xs text-red-400 mt-1 pl-1"
                >
                  {form.passwordError}
                </div>
              )}
            </div>

            {/* 5. Confirm Password */}
            <div>
              <label className="block text-xs font-semibold text-slate-400 mb-1.5">Confirm Password</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
                  <Lock className="w-4 h-4 text-cyan-500" />
                </div>
                <input
                  id="input_register_confirm_password"
                  data-testid="input_register_confirm_password"
                  type={form.isConfirmPasswordVisible ? 'text' : 'password'}
                  value={form.confirmPassword}
                  onChange={(e) => onConfirmPasswordChanged(e.target.value)}
                  placeholder="Re-enter your password"
                  className={`w-full pl-9 pr-10 py-2.5 rounded-xl text-sm border outline-none transition-colors ${
                    form.confirmPasswordError
                      ? 'border-red-500 focus:border-red-400'
                      : isDarkTheme
                      ? 'bg-slate-950 border-slate-700 text-white placeholder-slate-500 focus:border-cyan-400'
                      : 'bg-slate-50 border-slate-300 text-slate-900 placeholder-slate-400 focus:border-sky-600'
                  }`}
                />
                <button
                  id="btn_toggle_reg_confirm_password"
                  data-testid="btn_toggle_reg_confirm_password"
                  type="button"
                  onClick={onToggleConfirmPasswordVisibility}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-200"
                >
                  {form.isConfirmPasswordVisible ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {form.confirmPasswordError && (
                <div
                  id="error_register_confirm_password"
                  data-testid="error_register_confirm_password"
                  className="text-xs text-red-400 mt-1 pl-1"
                >
                  {form.confirmPasswordError}
                </div>
              )}
            </div>

            {/* Security Guarantee Notice */}
            <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 text-[11px] text-slate-400 flex items-start gap-2">
              <Shield className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
              <span>
                Passwords are never saved in plain text. Securely protected with SHA-256 salted encryption.
              </span>
            </div>

            {/* Register Button */}
            <button
              id="btn_register"
              data-testid="btn_register"
              type="submit"
              className="w-full py-3 px-4 rounded-xl font-bold text-sm text-white bg-gradient-to-r from-sky-600 to-cyan-600 hover:from-sky-500 hover:to-cyan-500 shadow-lg shadow-cyan-900/30 transition-all flex items-center justify-center gap-2 mt-1"
            >
              REGISTER
              <ArrowRight className="w-4 h-4" />
            </button>
          </form>

          {/* Link to Login */}
          <div className="text-center text-xs text-slate-400">
            Already have an account?{' '}
            <button
              id="link_to_login"
              data-testid="link_to_login"
              type="button"
              onClick={onNavigateToLogin}
              className="font-bold text-cyan-400 hover:text-cyan-300 hover:underline"
            >
              Login
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
