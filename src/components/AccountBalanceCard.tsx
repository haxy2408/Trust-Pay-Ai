import React from 'react';
import { Landmark, RotateCcw, ShieldCheck } from 'lucide-react';
import { formatIndianCurrency } from '../security/crypto';

interface AccountBalanceCardProps {
  balance: number;
  isDarkTheme: boolean;
  onResetDemoBalance: () => void;
}

export const AccountBalanceCard: React.FC<AccountBalanceCardProps> = ({
  balance,
  isDarkTheme,
  onResetDemoBalance,
}) => {
  return (
    <div
      id="card_demo_account_balance"
      data-testid="card_demo_account_balance"
      className={`rounded-2xl p-5 border transition-all ${
        isDarkTheme
          ? 'bg-slate-900/90 border-cyan-500/40 text-slate-100 shadow-lg'
          : 'bg-white border-slate-200 text-slate-900 shadow-md'
      }`}
    >
      <div className="flex flex-col gap-4">
        {/* Top Header Row */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div
              className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                isDarkTheme ? 'bg-cyan-500/20 text-cyan-400' : 'bg-slate-900 text-cyan-400'
              }`}
            >
              <Landmark className="w-5 h-5" />
            </div>
            <div>
              <div className="text-[11px] font-black uppercase tracking-wider text-cyan-500">
                DEMO ACCOUNT BALANCE
              </div>
              <div className="text-xs text-slate-400">
                Simulated Sandbox (No real bank connection)
              </div>
            </div>
          </div>

          <button
            id="btn_demo_balance_reset"
            data-testid="btn_demo_balance_reset"
            onClick={onResetDemoBalance}
            className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold border transition-colors ${
              isDarkTheme
                ? 'border-cyan-500/40 text-cyan-300 hover:bg-cyan-500/10'
                : 'border-slate-300 text-slate-700 hover:bg-slate-100'
            }`}
          >
            <RotateCcw className="w-3.5 h-3.5" />
            Demo Balance Reset
          </button>
        </div>

        {/* Balance Display Box */}
        <div
          className={`p-4 rounded-xl border ${
            isDarkTheme
              ? 'bg-black/40 border-cyan-500/25'
              : 'bg-slate-50 border-slate-200'
          }`}
        >
          <div
            id="text_available_balance"
            data-testid="text_available_balance"
            className={`text-2xl md:text-3xl font-extrabold tracking-tight ${
              isDarkTheme ? 'text-white' : 'text-slate-900'
            }`}
          >
            Available Balance: {formatIndianCurrency(balance)}
          </div>
          <div className="flex items-center gap-2 mt-2 text-xs text-slate-400">
            <ShieldCheck className="w-4 h-4 text-emerald-500 shrink-0" />
            <span>Default: ₹1,00,000.00 • Local storage • Protected by TrustPay Shield</span>
          </div>
        </div>
      </div>
    </div>
  );
};
