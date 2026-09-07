import React from 'react';
import { CreditCard, Landmark, Shield, AlertTriangle, RotateCcw } from 'lucide-react';
import { TrustPayUiState } from '../types';

interface PaymentInputCardProps {
  uiState: TrustPayUiState;
  onRecipientChanged: (value: string) => void;
  onAmountChanged: (value: string) => void;
  onToggleUntrustedDevice: (value: boolean) => void;
  onToggleNewRecipient: (value: boolean) => void;
  onToggleUnusualContext: (value: boolean) => void;
  onAnalyseAndPay: () => void;
  onTamperAttack: () => void;
  onTamperRecipientAttack: () => void;
  onResetHistory: () => void;
  onPrefill: (recipient: string, amount: string) => void;
}

export const PaymentInputCard: React.FC<PaymentInputCardProps> = ({
  uiState,
  onRecipientChanged,
  onAmountChanged,
  onToggleUntrustedDevice,
  onToggleNewRecipient,
  onToggleUnusualContext,
  onAnalyseAndPay,
  onTamperAttack,
  onTamperRecipientAttack,
  onResetHistory,
  onPrefill,
}) => {
  const isDark = uiState.isDarkTheme;

  return (
    <div
      className={`rounded-2xl p-5 md:p-6 border transition-all ${
        isDark
          ? 'bg-slate-900/90 border-slate-800 text-slate-100 shadow-xl'
          : 'bg-white border-slate-200 text-slate-900 shadow-md'
      }`}
    >
      <div className="flex flex-col gap-5">
        {/* Title */}
        <div className="flex items-center gap-2.5">
          <CreditCard className="w-5 h-5 text-sky-500" />
          <h3 className="text-base font-bold text-slate-100">Initiate Payment</h3>
        </div>

        {/* Inputs */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Recipient UPI ID */}
          <div>
            <label className="block text-xs font-semibold text-slate-400 mb-1.5">
              Recipient UPI ID
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
                <Landmark className="w-4 h-4" />
              </div>
              <input
                id="input_recipient"
                data-testid="input_recipient"
                type="text"
                value={uiState.recipientInput}
                onChange={(e) => onRecipientChanged(e.target.value)}
                placeholder="e.g. rahul@okhdfcbank"
                className={`w-full pl-9 pr-3 py-2.5 rounded-xl text-sm border transition-colors outline-none ${
                  isDark
                    ? 'bg-slate-950 border-slate-700 text-white placeholder-slate-500 focus:border-cyan-500'
                    : 'bg-slate-50 border-slate-300 text-slate-900 placeholder-slate-400 focus:border-sky-600'
                }`}
              />
            </div>
          </div>

          {/* Amount */}
          <div>
            <label className="block text-xs font-semibold text-slate-400 mb-1.5">
              Amount (INR)
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400 font-bold text-base">
                ₹
              </div>
              <input
                id="input_amount"
                data-testid="input_amount"
                type="number"
                step="any"
                value={uiState.amountInput}
                onChange={(e) => onAmountChanged(e.target.value)}
                placeholder="5000"
                className={`w-full pl-8 pr-3 py-2.5 rounded-xl text-sm border transition-colors outline-none font-medium ${
                  isDark
                    ? 'bg-slate-950 border-slate-700 text-white placeholder-slate-500 focus:border-cyan-500'
                    : 'bg-slate-50 border-slate-300 text-slate-900 placeholder-slate-400 focus:border-sky-600'
                }`}
              />
            </div>
          </div>
        </div>

        {/* Quick Demo Scenarios */}
        <div>
          <div className="text-xs font-semibold text-slate-400 mb-2">Quick Demo Scenarios:</div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => onPrefill('rahul@okhdfcbank', '5000')}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors ${
                isDark
                  ? 'bg-slate-800/80 border-slate-700 text-slate-200 hover:border-cyan-500/50 hover:bg-slate-800'
                  : 'bg-slate-100 border-slate-200 text-slate-700 hover:bg-slate-200'
              }`}
            >
              ₹5,000 (Velocity Demo)
            </button>
            <button
              type="button"
              onClick={() => onPrefill('merchant@upi', '45000')}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors ${
                isDark
                  ? 'bg-slate-800/80 border-slate-700 text-slate-200 hover:border-cyan-500/50 hover:bg-slate-800'
                  : 'bg-slate-100 border-slate-200 text-slate-700 hover:bg-slate-200'
              }`}
            >
              ₹45,000 (High-Value)
            </button>
            <button
              type="button"
              onClick={() => onPrefill('store@paytm', '2500')}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors ${
                isDark
                  ? 'bg-slate-800/80 border-slate-700 text-slate-200 hover:border-cyan-500/50 hover:bg-slate-800'
                  : 'bg-slate-100 border-slate-200 text-slate-700 hover:bg-slate-200'
              }`}
            >
              ₹2,500 (Low-Risk)
            </button>
          </div>
        </div>

        <div className="h-px bg-slate-800/80" />

        {/* Risk Signal Checkboxes */}
        <div>
          <div className="text-xs font-semibold text-slate-400 mb-2">
            Simulated Risk Signals (Optional):
          </div>
          <div className="space-y-2">
            {/* Untrusted device */}
            <label
              className={`flex items-start gap-3 p-2.5 rounded-xl border cursor-pointer transition-colors ${
                uiState.untrustedDevice
                  ? 'bg-amber-500/10 border-amber-500/40 text-amber-200'
                  : 'bg-slate-950/40 border-slate-800 text-slate-300 hover:border-slate-700'
              }`}
            >
              <input
                id="check_untrusted_device"
                data-testid="check_untrusted_device"
                type="checkbox"
                checked={uiState.untrustedDevice}
                onChange={(e) => onToggleUntrustedDevice(e.target.checked)}
                className="mt-0.5 w-4 h-4 rounded border-slate-600 text-cyan-500 focus:ring-cyan-500 bg-slate-900"
              />
              <div>
                <div className="text-xs font-bold text-slate-200">New / Untrusted Device</div>
                <div className="text-[11px] text-slate-400">
                  Simulates hardware fingerprint anomaly (+25 Risk)
                </div>
              </div>
            </label>

            {/* New Recipient */}
            <label
              className={`flex items-start gap-3 p-2.5 rounded-xl border cursor-pointer transition-colors ${
                uiState.newRecipient
                  ? 'bg-amber-500/10 border-amber-500/40 text-amber-200'
                  : 'bg-slate-950/40 border-slate-800 text-slate-300 hover:border-slate-700'
              }`}
            >
              <input
                id="check_new_recipient"
                data-testid="check_new_recipient"
                type="checkbox"
                checked={uiState.newRecipient}
                onChange={(e) => onToggleNewRecipient(e.target.checked)}
                className="mt-0.5 w-4 h-4 rounded border-slate-600 text-cyan-500 focus:ring-cyan-500 bg-slate-900"
              />
              <div>
                <div className="text-xs font-bold text-slate-200">New Recipient</div>
                <div className="text-[11px] text-slate-400">
                  Simulates first-time unverified beneficiary (+20 Risk)
                </div>
              </div>
            </label>

            {/* Unusual Context */}
            <label
              className={`flex items-start gap-3 p-2.5 rounded-xl border cursor-pointer transition-colors ${
                uiState.unusualContext
                  ? 'bg-amber-500/10 border-amber-500/40 text-amber-200'
                  : 'bg-slate-950/40 border-slate-800 text-slate-300 hover:border-slate-700'
              }`}
            >
              <input
                id="check_unusual_context"
                data-testid="check_unusual_context"
                type="checkbox"
                checked={uiState.unusualContext}
                onChange={(e) => onToggleUnusualContext(e.target.checked)}
                className="mt-0.5 w-4 h-4 rounded border-slate-600 text-cyan-500 focus:ring-cyan-500 bg-slate-900"
              />
              <div>
                <div className="text-xs font-bold text-slate-200">Unusual Time / Location</div>
                <div className="text-[11px] text-slate-400">
                  Simulates geo-jump or midnight execution (+20 Risk)
                </div>
              </div>
            </label>
          </div>
        </div>

        {/* Buttons */}
        <div className="flex flex-col gap-2.5 pt-2">
          {/* 1. Analyse & Pay */}
          <button
            id="btn_analyse_pay"
            data-testid="btn_analyse_pay"
            type="button"
            onClick={onAnalyseAndPay}
            className="w-full py-3 px-4 rounded-xl font-bold text-sm text-white bg-gradient-to-r from-sky-600 to-cyan-600 hover:from-sky-500 hover:to-cyan-500 shadow-lg shadow-cyan-900/30 transition-all flex items-center justify-center gap-2"
          >
            <Shield className="w-4 h-4" />
            Analyse & Pay
          </button>

          {/* 2. Attack Demo: Tamper Amount */}
          <button
            id="btn_tamper_attack"
            data-testid="btn_tamper_attack"
            type="button"
            onClick={onTamperAttack}
            className="w-full py-2.5 px-4 rounded-xl font-bold text-xs text-red-300 bg-red-950/50 border border-red-500/60 hover:bg-red-950/80 transition-all flex items-center justify-center gap-2"
          >
            <AlertTriangle className="w-4 h-4 text-red-400" />
            Attack Demo: Tamper Amount
          </button>

          {/* Attack Demo: Tamper Recipient UPI */}
          <button
            id="btn_tamper_recipient"
            data-testid="btn_tamper_recipient"
            type="button"
            onClick={onTamperRecipientAttack}
            className="w-full py-2 px-4 rounded-xl font-semibold text-xs text-red-400 border border-red-500/30 hover:border-red-500/60 hover:bg-red-950/30 transition-all flex items-center justify-center gap-2"
          >
            <AlertTriangle className="w-3.5 h-3.5 text-red-400" />
            Attack Demo: Tamper Recipient UPI
          </button>

          {/* 3. Reset Demo History */}
          <button
            id="btn_reset_history"
            data-testid="btn_reset_history"
            type="button"
            onClick={onResetHistory}
            className="w-full py-2.5 px-4 rounded-xl font-semibold text-xs text-slate-300 border border-slate-700 hover:border-slate-500 hover:bg-slate-800/60 transition-all flex items-center justify-center gap-2"
          >
            <RotateCcw className="w-4 h-4 text-slate-400" />
            Reset Demo History
          </button>
        </div>
      </div>
    </div>
  );
};
