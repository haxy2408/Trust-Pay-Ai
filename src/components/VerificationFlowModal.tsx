import React from 'react';
import { Fingerprint, CheckCircle2, ShieldAlert, KeyRound, AlertCircle, XCircle } from 'lucide-react';
import { VerificationState } from '../types';
import { formatIndianCurrency } from '../security/crypto';

interface VerificationFlowModalProps {
  state: VerificationState;
  onBiometricResult: (success: boolean) => void;
  onChallengeSubmit: (answer: string) => void;
  onOtpSubmit: (otp: string) => void;
  onCancel: () => void;
}

export const VerificationFlowModal: React.FC<VerificationFlowModalProps> = ({
  state,
  onBiometricResult,
  onChallengeSubmit,
  onOtpSubmit,
  onCancel,
}) => {
  const [challengeInput, setChallengeInput] = React.useState('');
  const [otpInput, setOtpInput] = React.useState('');

  const { currentStep, isVelocityAnomaly, designatedFinger, challengeData, demoOtp, challengeError, otpError } = state;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
      <div className="w-full max-w-lg rounded-2xl bg-slate-900 border border-cyan-500/50 text-white shadow-2xl p-6 flex flex-col gap-5">
        {/* Header */}
        <div className="flex items-start justify-between border-b border-slate-800 pb-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-amber-500/20 text-amber-400">
              <ShieldAlert className="w-6 h-6" />
            </div>
            <div>
              <div className="text-xs font-black tracking-wider uppercase text-amber-400">
                {isVelocityAnomaly ? 'Velocity Anomaly Detected' : 'Enhanced Security Verification'}
              </div>
              <h3 className="text-lg font-bold text-white">3-Step Authorization Protocol</h3>
            </div>
          </div>
        </div>

        {/* Stepper indicator */}
        <div className="flex items-center justify-between px-2">
          {/* Step 1 */}
          <div className="flex items-center gap-2">
            <div
              className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold ${
                currentStep === 'BIOMETRIC'
                  ? 'bg-cyan-500 text-slate-950 ring-2 ring-cyan-400 ring-offset-2 ring-offset-slate-900'
                  : currentStep === 'CHALLENGE' || currentStep === 'DEMO_OTP' || currentStep === 'COMPLETED'
                  ? 'bg-emerald-500 text-slate-950'
                  : 'bg-slate-800 text-slate-400'
              }`}
            >
              1
            </div>
            <span className="text-xs font-semibold text-slate-300">Biometric</span>
          </div>

          <div className="h-0.5 w-8 bg-slate-800" />

          {/* Step 2 */}
          <div className="flex items-center gap-2">
            <div
              className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold ${
                currentStep === 'CHALLENGE'
                  ? 'bg-cyan-500 text-slate-950 ring-2 ring-cyan-400 ring-offset-2 ring-offset-slate-900'
                  : currentStep === 'DEMO_OTP' || currentStep === 'COMPLETED'
                  ? 'bg-emerald-500 text-slate-950'
                  : 'bg-slate-800 text-slate-400'
              }`}
            >
              2
            </div>
            <span className="text-xs font-semibold text-slate-300">Challenge</span>
          </div>

          <div className="h-0.5 w-8 bg-slate-800" />

          {/* Step 3 */}
          <div className="flex items-center gap-2">
            <div
              className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold ${
                currentStep === 'DEMO_OTP'
                  ? 'bg-cyan-500 text-slate-950 ring-2 ring-cyan-400 ring-offset-2 ring-offset-slate-900'
                  : currentStep === 'COMPLETED'
                  ? 'bg-emerald-500 text-slate-950'
                  : 'bg-slate-800 text-slate-400'
              }`}
            >
              3
            </div>
            <span className="text-xs font-semibold text-slate-300">OTP</span>
          </div>
        </div>

        {/* Transaction Brief */}
        <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 text-xs flex justify-between items-center">
          <span className="text-slate-400">Target Payee: <b className="text-slate-200">{state.recipient}</b></span>
          <span className="text-cyan-400 font-extrabold text-sm">{formatIndianCurrency(state.amount)}</span>
        </div>

        {/* STEP 1: Biometric Verification with Designated Finger Challenge */}
        {currentStep === 'BIOMETRIC' && (
          <div className="flex flex-col items-center text-center gap-4 py-2">
            <div className="relative">
              <div className="w-16 h-16 rounded-2xl bg-cyan-500/10 border border-cyan-500/40 flex items-center justify-center text-cyan-400 animate-pulse">
                <Fingerprint className="w-10 h-10" />
              </div>
            </div>

            <div>
              <div className="text-xs font-bold uppercase tracking-wider text-cyan-400">
                Dynamic Biometric Challenge
              </div>
              <h4 className="text-base font-bold text-white mt-1">
                Designated Finger: <span className="text-amber-400 underline">{designatedFinger || 'Right Index Finger'}</span>
              </h4>
              <p className="text-xs text-slate-400 mt-1 max-w-sm">
                To prevent automated replay attacks during high-velocity bursts, TrustPay requires authentication using the dynamic designated finger above.
              </p>
            </div>

            <div className="flex flex-col sm:flex-row gap-2.5 w-full pt-2">
              <button
                type="button"
                onClick={() => onBiometricResult(true)}
                className="flex-1 py-2.5 px-4 rounded-xl font-bold text-xs bg-emerald-600 hover:bg-emerald-500 text-white transition-colors flex items-center justify-center gap-1.5"
              >
                <CheckCircle2 className="w-4 h-4" />
                Present {designatedFinger || 'Finger'} (Pass)
              </button>

              <button
                type="button"
                onClick={() => onBiometricResult(false)}
                className="py-2.5 px-4 rounded-xl font-bold text-xs bg-red-950/60 border border-red-500/40 text-red-300 hover:bg-red-900/40 transition-colors"
              >
                Wrong Finger (Fail)
              </button>
            </div>
          </div>
        )}

        {/* STEP 2: Detail Challenge */}
        {currentStep === 'CHALLENGE' && challengeData && (
          <div className="flex flex-col gap-4 py-2">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-xl bg-cyan-500/20 text-cyan-400">
                <KeyRound className="w-5 h-5" />
              </div>
              <div>
                <div className="text-xs font-bold text-cyan-400">Step 2: Transaction-Detail Challenge</div>
                <h4 className="text-sm font-bold text-white">Cognitive Intent Confirmation</h4>
              </div>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-300 leading-relaxed">
              {challengeData.question}
            </div>

            <div>
              <input
                type="text"
                value={challengeInput}
                onChange={(e) => setChallengeInput(e.target.value)}
                placeholder="Enter answer..."
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white placeholder-slate-500 text-sm focus:border-cyan-400 outline-none"
                onKeyDown={(e) => {
                  if (e.key === 'Enter') onChallengeSubmit(challengeInput);
                }}
              />
              {challengeError && (
                <div className="text-xs text-red-400 mt-1.5 flex items-center gap-1">
                  <AlertCircle className="w-3.5 h-3.5" />
                  <span>{challengeError}</span>
                </div>
              )}
            </div>

            <button
              type="button"
              onClick={() => onChallengeSubmit(challengeInput)}
              className="w-full py-2.5 px-4 rounded-xl font-bold text-xs bg-gradient-to-r from-sky-600 to-cyan-600 hover:from-sky-500 hover:to-cyan-500 text-white transition-all"
            >
              Verify Challenge Answer
            </button>
          </div>
        )}

        {/* STEP 3: Demo OTP */}
        {currentStep === 'DEMO_OTP' && (
          <div className="flex flex-col gap-4 py-2">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-xl bg-cyan-500/20 text-cyan-400">
                <KeyRound className="w-5 h-5" />
              </div>
              <div>
                <div className="text-xs font-bold text-cyan-400">Step 3: Demo Bank OTP Verification</div>
                <h4 className="text-sm font-bold text-white">Simulated 2-Factor Authentication</h4>
              </div>
            </div>

            {/* Simulated OTP Display Banner */}
            <div className="p-3 rounded-xl bg-cyan-500/10 border border-cyan-500/30 text-xs">
              <div className="font-bold text-cyan-300 mb-0.5">SIMULATED HACKATHON TEST OTP:</div>
              <div className="text-base font-black tracking-widest text-white font-mono">
                DEMO OTP: {demoOtp || '123456'}
              </div>
              <div className="text-[11px] text-slate-400 mt-1">
                Enter the simulated 6-digit code shown above to complete authorization.
              </div>
            </div>

            <div>
              <input
                type="text"
                maxLength={6}
                value={otpInput}
                onChange={(e) => setOtpInput(e.target.value.replace(/\D/g, ''))}
                placeholder="Enter 6-digit OTP"
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-center font-mono tracking-widest text-lg focus:border-cyan-400 outline-none"
                onKeyDown={(e) => {
                  if (e.key === 'Enter') onOtpSubmit(otpInput);
                }}
              />
              {otpError && (
                <div className="text-xs text-red-400 mt-1.5 flex items-center gap-1 justify-center">
                  <AlertCircle className="w-3.5 h-3.5" />
                  <span>{otpError}</span>
                </div>
              )}
            </div>

            <button
              type="button"
              onClick={() => onOtpSubmit(otpInput)}
              className="w-full py-2.5 px-4 rounded-xl font-bold text-xs bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white transition-all shadow-lg shadow-emerald-950/40"
            >
              Verify OTP & Authorize Payment
            </button>
          </div>
        )}

        {/* Footer: Cancel Verification */}
        <div className="border-t border-slate-800 pt-3 flex justify-end">
          <button
            type="button"
            onClick={onCancel}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold text-slate-400 hover:text-red-400 hover:bg-slate-800 transition-colors"
          >
            <XCircle className="w-4 h-4" />
            Cancel Verification (Blocks Payment)
          </button>
        </div>
      </div>
    </div>
  );
};
