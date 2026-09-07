import React from 'react';
import { Gauge, Clock } from 'lucide-react';
import { PaymentTransaction, TrustPayUiState } from '../types';
import { formatIndianCurrency } from '../security/crypto';

interface VelocityMonitorCardProps {
  uiState: TrustPayUiState;
  onResetHistory: () => void;
}

export const VELOCITY_AMOUNT_THRESHOLD = 15000.0;
export const VELOCITY_COUNT_THRESHOLD = 3;

export const VelocityMonitorCard: React.FC<VelocityMonitorCardProps> = ({ uiState }) => {
  const isDark = uiState.isDarkTheme;
  const count = uiState.rollingWindowTransactions.length;
  const totalAmount = uiState.rollingWindowTotalAmount;
  const progress = Math.min(1, Math.max(0, totalAmount / VELOCITY_AMOUNT_THRESHOLD));

  return (
    <div
      className={`rounded-2xl p-5 md:p-6 border transition-all ${
        isDark
          ? 'bg-slate-900/90 border-slate-800 text-slate-100 shadow-xl'
          : 'bg-white border-slate-200 text-slate-900 shadow-md'
      }`}
    >
      <div className="flex flex-col gap-4">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-cyan-500/20 text-cyan-400">
              <Gauge className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-100">Velocity Anomaly Monitor</h3>
              <p className="text-xs text-slate-400">Rolling 5-minute sliding window</p>
            </div>
          </div>

          <div
            className={`px-3 py-1 rounded-full text-xs font-bold ${
              count >= 2
                ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40'
                : 'bg-slate-800 text-slate-300'
            }`}
          >
            {count} txns in window
          </div>
        </div>

        {/* Stats Row */}
        <div className="grid grid-cols-2 gap-4 pt-1">
          <div>
            <div className="text-xs text-slate-400">Window Cumulative Total</div>
            <div
              className={`text-xl font-extrabold ${
                totalAmount >= VELOCITY_AMOUNT_THRESHOLD ? 'text-red-400' : 'text-cyan-400'
              }`}
            >
              {formatIndianCurrency(totalAmount)}
            </div>
          </div>

          <div className="text-right">
            <div className="text-xs text-slate-400">Anomaly Trigger Threshold</div>
            <div className="text-sm font-bold text-slate-200">≥ 3 txns & ≥ ₹15,000</div>
          </div>
        </div>

        {/* Progress Bar */}
        <div className="w-full h-2 rounded-full bg-slate-800 overflow-hidden">
          <div
            className={`h-full transition-all duration-300 ${
              progress >= 1 ? 'bg-amber-500' : 'bg-cyan-500'
            }`}
            style={{ width: `${progress * 100}%` }}
          />
        </div>

        {/* Transactions in window */}
        <div className="pt-2 border-t border-slate-800/80">
          <div className="text-xs font-bold text-slate-400 mb-2">
            Active Transactions in Rolling Window:
          </div>

          {uiState.rollingWindowTransactions.length > 0 ? (
            <div className="space-y-2">
              {uiState.rollingWindowTransactions.map((tx) => {
                const timeStr = new Date(tx.timestamp).toLocaleTimeString([], {
                  hour: '2-digit',
                  minute: '2-digit',
                  second: '2-digit',
                });
                return (
                  <div
                    key={tx.id}
                    className="flex items-center justify-between p-2.5 rounded-xl bg-slate-950/60 border border-slate-800 text-xs"
                  >
                    <div>
                      <div className="font-bold text-slate-200">{tx.recipient}</div>
                      <div className="text-[11px] text-slate-500 font-mono flex items-center gap-1 mt-0.5">
                        <Clock className="w-3 h-3 text-slate-500" />
                        <span>{tx.id} • {timeStr}</span>
                      </div>
                    </div>
                    <div className="font-bold text-emerald-400 text-sm">
                      {formatIndianCurrency(tx.amount)}
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <p className="text-xs italic text-slate-500 py-1">
              No prior transactions within the last 5 minutes. Ready for first attempt.
            </p>
          )}
        </div>
      </div>
    </div>
  );
};
