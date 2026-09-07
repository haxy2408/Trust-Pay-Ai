import React from 'react';
import { Shield, Lock, User, Eye, EyeOff, Sparkles, Sun, Moon, ArrowRight } from 'lucide-react';
import { LoginFormState } from '../types';

interface LoginScreenProps {
  form: LoginFormState;
  isDarkTheme: boolean;
  onIdentifierChanged: (value: string) => void;
  onPasswordChanged: (value: string) => void;
  onTogglePasswordVisibility: () => void;
  onLogin: () => void;
  onFillDemo: () => void;
  onNavigateToRegister: () => void;
  onToggleTheme: () => void;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({
  form,
  isDarkTheme,
  onIdentifierChanged,
  onPasswordChanged,
  onTogglePasswordVisibility,
  onLogin,
  onFillDemo,
  onNavigateToRegister,
  onToggleTheme,
}) => {
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onLogin();
  };

  return (
    <div
      className={`min-h-screen flex flex-col items-center justify-center p-4 transition-colors duration-200 ${
        isDarkTheme ? 'bg-slate-950 text-slate-100' : 'bg-slate-100 text-slate-900'
      }`}
    >
      {/* Top bar with theme toggle */}
      <div className="w-full max-w-md flex items-center justify-between mb-4 px-2">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-cyan-500/20 border border-cyan-500/40 flex items-center justify-center text-cyan-400">
            <Shield className="w-5 h-5" />
          </div>
          <span className="font-extrabold tracking-wider text-sm text-cyan-500">TRUSTPAY</span>
        </div>

        <button
          id="btn_theme_toggle_login"
          data-testid="btn_theme_toggle_login"
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
        className={`w-full max-w-md rounded-2xl border shadow-2xl p-6 md:p-8 transition-all ${
          isDarkTheme
            ? 'bg-slate-900/90 border-slate-800 text-white'
            : 'bg-white border-slate-200 text-slate-900'
        }`}
      >
        <div className="flex flex-col gap-6">
          {/* Header */}
          <div className="flex flex-col gap-1">
            <h2 className="text-xl font-extrabold tracking-tight">Welcome Back</h2>
            <p className="text-xs text-slate-400">
              Sign in to manage and verify SHA-256 cryptographically bound transactions.
            </p>
          </div>

          {/* Quick Demo Fill Banner */}
          <button
            id="btn_demo_fill_login"
            data-testid="btn_demo_fill_login"
            type="button"
            onClick={onFillDemo}
            className={`w-full p-3 rounded-xl border text-left flex items-center justify-between transition-colors ${
              isDarkTheme
                ? 'bg-cyan-500/10 border-cyan-500/30 text-cyan-300 hover:bg-cyan-500/20'
                : 'bg-sky-50 border-sky-200 text-sky-900 hover:bg-sky-100'
            }`}
          >
            <div className="flex items-center gap-2.5">
              <Sparkles className="w-4 h-4 text-cyan-400" />
              <div>
                <div className="text-xs font-bold">Quick Demo Login</div>
                <div className="text-[11px] text-slate-400">rahul@example.com • password123</div>
              </div>
            </div>
            <span className="text-xs font-semibold underline">Auto Fill</span>
          </button>

          {/* Error / Success Banners */}
          {form.errorMessage && (
            <div
              id="banner_login_error"
              data-testid="banner_login_error"
              className="p-3 rounded-xl bg-red-500/15 border border-red-500/40 text-red-300 text-xs font-medium"
            >
              {form.errorMessage}
            </div>
          )}

          {form.successMessage && (
            <div
              id="banner_login_success"
              data-testid="banner_login_success"
              className="p-3 rounded-xl bg-emerald-500/15 border border-emerald-500/40 text-emerald-300 text-xs font-medium"
            >
              {form.successMessage}
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            {/* Email or Mobile Identifier */}
            <div>
              <label className="block text-xs font-semibold text-slate-400 mb-1.5">
                Email or Mobile Number
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
                  <User className="w-4 h-4 text-cyan-500" />
                </div>
                <input
                  id="input_login_identifier"
                  data-testid="input_login_identifier"
                  type="text"
                  value={form.identifier}
                  onChange={(e) => onIdentifierChanged(e.target.value)}
                  placeholder="e.g. rahul@example.com or 9876543210"
                  className={`w-full pl-9 pr-3 py-2.5 rounded-xl text-sm border outline-none transition-colors ${
                    form.identifierError
                      ? 'border-red-500 focus:border-red-400'
                      : isDarkTheme
                      ? 'bg-slate-950 border-slate-700 text-white placeholder-slate-500 focus:border-cyan-400'
                      : 'bg-slate-50 border-slate-300 text-slate-900 placeholder-slate-400 focus:border-sky-600'
                  }`}
                />
              </div>
              {form.identifierError && (
                <div
                  id="error_login_identifier"
                  data-testid="error_login_identifier"
                  className="text-xs text-red-400 mt-1 pl-1"
                >
                  {form.identifierError}
                </div>
              )}
            </div>

            {/* Password */}
            <div>
              <label className="block text-xs font-semibold text-slate-400 mb-1.5">Password</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
                  <Lock className="w-4 h-4 text-cyan-500" />
                </div>
                <input
                  id="input_login_password"
                  data-testid="input_login_password"
                  type={form.isPasswordVisible ? 'text' : 'password'}
                  value={form.password}
                  onChange={(e) => onPasswordChanged(e.target.value)}
                  placeholder="Enter your password"
                  className={`w-full pl-9 pr-10 py-2.5 rounded-xl text-sm border outline-none transition-colors ${
                    form.passwordError
                      ? 'border-red-500 focus:border-red-400'
                      : isDarkTheme
                      ? 'bg-slate-950 border-slate-700 text-white placeholder-slate-500 focus:border-cyan-400'
                      : 'bg-slate-50 border-slate-300 text-slate-900 placeholder-slate-400 focus:border-sky-600'
                  }`}
                />
                <button
                  id="btn_toggle_password_visibility"
                  data-testid="btn_toggle_password_visibility"
                  type="button"
                  onClick={onTogglePasswordVisibility}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-200"
                >
                  {form.isPasswordVisible ? (
                    <EyeOff className="w-4 h-4" />
                  ) : (
                    <Eye className="w-4 h-4" />
                  )}
                </button>
              </div>
              {form.passwordError && (
                <div
                  id="error_login_password"
                  data-testid="error_login_password"
                  className="text-xs text-red-400 mt-1 pl-1"
                >
                  {form.passwordError}
                </div>
              )}
            </div>

            {/* Security Guarantee Notice */}
            <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 text-[11px] text-slate-400 flex items-start gap-2">
              <Shield className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
              <span>
                Protected with SHA-256 salted cryptographic hashing. Passwords are never transmitted or stored in plain text.
              </span>
            </div>

            {/* Submit Button */}
            <button
              id="btn_login"
              data-testid="btn_login"
              type="submit"
              className="w-full py-3 px-4 rounded-xl font-bold text-sm text-white bg-gradient-to-r from-sky-600 to-cyan-600 hover:from-sky-500 hover:to-cyan-500 shadow-lg shadow-cyan-900/30 transition-all flex items-center justify-center gap-2 mt-1"
            >
              Sign In
              <ArrowRight className="w-4 h-4" />
            </button>
          </form>

          {/* Link to Register */}
          <div className="text-center text-xs text-slate-400">
            Don&apos;t have an account?{' '}
            <button
              id="link_to_register"
              data-testid="link_to_register"
              type="button"
              onClick={onNavigateToRegister}
              className="font-bold text-cyan-400 hover:text-cyan-300 hover:underline"
            >
              Register
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
