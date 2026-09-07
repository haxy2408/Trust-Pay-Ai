import React from 'react';
import { RotateCcw, AlertCircle, X } from 'lucide-react';
import { DEFAULT_DEMO_BALANCE } from '../storage/transactionStorage';
import { formatIndianCurrency } from '../security/crypto';

interface ResetBalanceModalProps {
  currentBalance: number;
  onConfirm: () => void;
  onCancel: () => void;
}

export const ResetBalanceModal: React.FC<ResetBalanceModalProps> = ({
  currentBalance,
  onConfirm,
  onCancel,
}) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
      <div
        id="dialog_reset_balance"
        data-testid="dialog_reset_balance"
        className="w-full max-w-md rounded-2xl bg-slate-900 border border-slate-700 text-white shadow-2xl p-6 flex flex-col gap-4"
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-cyan-500/20 text-cyan-400">
              <RotateCcw className="w-5 h-5" />
            </div>
            <h3 className="text-base font-bold text-white">Reset Demo Sandbox Balance</h3>
          </div>
          <button
            id="btn_cancel_balance_reset"
            data-testid="btn_cancel_balance_reset"
            onClick={onCancel}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-300 leading-relaxed flex flex-col gap-2">
          <div className="flex items-center gap-2 text-amber-400 font-semibold">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>Reset Confirmation</span>
          </div>
          <p>
            Your current demo balance is <b>{formatIndianCurrency(currentBalance)}</b>.
          </p>
          <p>
            Do you want to reset the balance back to the default hackathon sandbox allocation of{' '}
            <b className="text-cyan-400">{formatIndianCurrency(DEFAULT_DEMO_BALANCE)}</b>?
          </p>
        </div>

        <div className="flex items-center justify-end gap-2.5 pt-2">
          <button
            type="button"
            onClick={onCancel}
            className="py-2 px-3.5 rounded-xl text-xs font-semibold text-slate-300 hover:bg-slate-800 transition-colors"
          >
            Cancel
          </button>
          <button
            id="btn_confirm_balance_reset"
            data-testid="btn_confirm_balance_reset"
            type="button"
            onClick={onConfirm}
            className="py-2 px-4 rounded-xl text-xs font-bold bg-cyan-600 hover:bg-cyan-500 text-white transition-colors"
          >
            Reset to {formatIndianCurrency(DEFAULT_DEMO_BALANCE)}
          </button>
        </div>
      </div>
    </div>
  );
};
