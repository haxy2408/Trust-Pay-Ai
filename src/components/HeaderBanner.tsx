import React from 'react';
import { Shield, Sun, Moon, User, LogOut, BookOpen, Smartphone } from 'lucide-react';
import { UserAccount } from '../types';

interface HeaderBannerProps {
  isDarkTheme: boolean;
  currentUser: UserAccount | null;
  onToggleTheme: () => void;
  onOpenGuide: () => void;
  onOpenInstallAndroid?: () => void;
  onLogout: () => void;
}

export const HeaderBanner: React.FC<HeaderBannerProps> = ({
  isDarkTheme,
  currentUser,
  onToggleTheme,
  onOpenGuide,
  onOpenInstallAndroid,
  onLogout,
}) => {
  return (
    <div
      className={`rounded-2xl p-4 md:p-6 transition-colors duration-200 border ${
        isDarkTheme
          ? 'bg-slate-900/90 border-cyan-500/30 text-white shadow-xl shadow-cyan-950/20'
          : 'bg-slate-900 border-slate-700 text-white shadow-lg'
      }`}
    >
      <div className="flex flex-col gap-4">
        {/* Top bar with Badge, Scenario button, and Android Install */}
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-cyan-500/15 border border-cyan-400/40 text-xs font-bold tracking-wider text-cyan-300">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            HACKATHON DEMO • SIMULATED
          </div>

          <div className="flex items-center gap-2">
            {onOpenInstallAndroid && (
              <button
                onClick={onOpenInstallAndroid}
                className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-semibold text-emerald-400 hover:text-emerald-300 bg-emerald-500/10 hover:bg-emerald-500/20 border border-emerald-500/30 transition-colors"
                title="Install TrustPay on Android as APK or WebAPK"
              >
                <Smartphone className="w-3.5 h-3.5" />
                Install Android APK
              </button>
            )}

            <button
              onClick={onOpenGuide}
              className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-semibold text-cyan-400 hover:text-cyan-300 hover:bg-cyan-500/10 transition-colors"
            >
              <BookOpen className="w-3.5 h-3.5" />
              Scenario Walkthrough
            </button>
          </div>
        </div>

        {/* Tagline */}
        <div>
          <h2 className="text-lg md:text-xl font-semibold italic text-slate-100 leading-snug">
            “Secure every transaction.”
          </h2>
          <p className="mt-1 text-xs md:text-sm text-slate-300 leading-relaxed max-w-3xl">
            Cryptographically binds transaction details with SHA-256 and detects velocity anomalies across
            rolling 5-minute windows with 3-step verification.
          </p>
        </div>

        {/* Theme Switcher Banner */}
        <div
          id="banner_theme_toggle"
          data-testid="banner_theme_toggle"
          onClick={onToggleTheme}
          className={`flex items-center justify-between p-3 rounded-xl cursor-pointer border transition-all ${
            isDarkTheme
              ? 'bg-slate-950/80 border-cyan-500/40 hover:border-cyan-400/60'
              : 'bg-slate-800/80 border-slate-700 hover:border-slate-600'
          }`}
        >
          <div className="flex items-center gap-3">
            <div
              className={`p-2 rounded-lg ${
                isDarkTheme ? 'bg-cyan-500/20 text-cyan-400' : 'bg-amber-500/20 text-amber-300'
              }`}
            >
              {isDarkTheme ? <Moon className="w-4 h-4" /> : <Sun className="w-4 h-4" />}
            </div>
            <div>
              <div className="text-xs font-bold tracking-wider text-cyan-300 uppercase">
                {isDarkTheme ? 'Dark Security Mode' : 'Navy-Blue Light Mode'}
              </div>
              <div className="text-[11px] text-slate-400">
                {isDarkTheme
                  ? 'Accessible WCAG AAA Green & Red status tokens active'
                  : 'Clean navy-blue financial security canvas'}
              </div>
            </div>
          </div>

          <div
            id="switch_theme_mode"
            data-testid="switch_theme_mode"
            className={`w-11 h-6 flex items-center rounded-full p-1 cursor-pointer transition-colors ${
              isDarkTheme ? 'bg-cyan-600' : 'bg-slate-600'
            }`}
          >
            <div
              className={`bg-white w-4 h-4 rounded-full shadow-md transform transition-transform ${
                isDarkTheme ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </div>
        </div>

        {/* Active User Profile Pill */}
        {currentUser && (
          <div className="flex items-center justify-between p-3 rounded-xl bg-slate-950/70 border border-cyan-500/20">
            <div className="flex items-center gap-3 min-w-0">
              <div className="w-8 h-8 rounded-lg bg-cyan-500/20 flex items-center justify-center text-cyan-400 shrink-0">
                <User className="w-4 h-4" />
              </div>
              <div className="min-w-0">
                <div className="text-[10px] font-bold uppercase tracking-wider text-cyan-400">
                  Authenticated User
                </div>
                <div className="text-xs font-semibold text-slate-200 truncate">
                  {currentUser.fullName} • {currentUser.email}
                </div>
              </div>
            </div>

            <button
              id="btn_header_logout"
              data-testid="btn_header_logout"
              onClick={onLogout}
              className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-bold text-red-400 hover:text-red-300 hover:bg-red-500/10 transition-colors"
            >
              <LogOut className="w-3.5 h-3.5" />
              Logout
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
