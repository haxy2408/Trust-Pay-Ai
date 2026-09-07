import React from 'react';
import { Fingerprint, CheckCircle2, XCircle } from 'lucide-react';
import { formatIndianCurrency } from '../security/crypto';

interface BiometricSimulationModalProps {
  recipient: string;
  amount: number;
  onSuccess: () => void;
  onFailure: () => void;
  onCancel: () => void;
}

export const BiometricSimulationModal: React.FC<BiometricSimulationModalProps> = ({
  recipient,
  amount,
  onSuccess,
  onFailure,
  onCancel,
}) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
      <div className="w-full max-w-md rounded-2xl bg-slate-900 border border-cyan-500/50 text-white shadow-2xl p-6 flex flex-col items-center text-center gap-5">
        {/* Biometric Icon */}
        <div className="w-16 h-16 rounded-2xl bg-cyan-500/15 border border-cyan-400/40 flex items-center justify-center text-cyan-400">
          <Fingerprint className="w-9 h-9 animate-pulse" />
        </div>

        <div>
          <div className="text-xs font-bold uppercase tracking-wider text-cyan-400">
            Biometric Authorization
          </div>
          <h3 className="text-lg font-bold text-white mt-1">Authenticate Transaction</h3>
          <p className="text-xs text-slate-400 mt-1 max-w-xs">
            Confirm payment of <b className="text-cyan-300">{formatIndianCurrency(amount)}</b> to{' '}
            <b className="text-slate-200">{recipient}</b> using registered fingerprint or face biometric.
          </p>
        </div>

        {/* Buttons */}
        <div className="flex flex-col gap-2.5 w-full pt-2">
          <button
            type="button"
            onClick={onSuccess}
            className="w-full py-2.5 px-4 rounded-xl font-bold text-xs bg-emerald-600 hover:bg-emerald-500 text-white transition-colors flex items-center justify-center gap-2 shadow-lg shadow-emerald-950/40"
          >
            <CheckCircle2 className="w-4 h-4" />
            Authenticate (Simulate Biometric Pass)
          </button>

          <button
            type="button"
            onClick={onFailure}
            className="w-full py-2.5 px-4 rounded-xl font-bold text-xs bg-red-950/60 border border-red-500/40 text-red-300 hover:bg-red-900/40 transition-colors"
          >
            Simulate Biometric Failure
          </button>

          <button
            type="button"
            onClick={onCancel}
            className="w-full py-2 px-4 rounded-xl font-semibold text-xs text-slate-400 hover:text-slate-200 transition-colors flex items-center justify-center gap-1.5"
          >
            <XCircle className="w-4 h-4" />
            Cancel Transaction
          </button>
        </div>
      </div>
    </div>
  );
};
