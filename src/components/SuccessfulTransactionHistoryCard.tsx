import React from 'react';
import { History, CheckCircle2, Landmark } from 'lucide-react';
import { PaymentTransaction } from '../types';
import { formatIndianCurrency } from '../security/crypto';

interface SuccessfulTransactionHistoryCardProps {
  transactions: PaymentTransaction[];
  isDarkTheme: boolean;
}

export const SuccessfulTransactionHistoryCard: React.FC<SuccessfulTransactionHistoryCardProps> = ({
  transactions,
  isDarkTheme,
}) => {
  const successfulTxns = transactions.filter(
    (tx) => tx.status === 'APPROVED' || tx.status.toLowerCase() === 'successful'
  );

  return (
    <div
      id="card_successful_transaction_history"
      data-testid="card_successful_transaction_history"
      className={`rounded-2xl p-5 md:p-6 border transition-all ${
        isDarkTheme
          ? 'bg-slate-900/90 border-slate-800 text-slate-100 shadow-xl'
          : 'bg-white border-slate-200 text-slate-900 shadow-md'
      }`}
    >
      <div className="flex flex-col gap-4">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <History className="w-5 h-5 text-sky-500" />
            <h3 className="text-base font-bold text-slate-100">Successful Transactions</h3>
          </div>

          <div className="px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-500/15 border border-emerald-500/30 text-emerald-400">
            {successfulTxns.length} approved
          </div>
        </div>

        {/* List or Empty State */}
        {successfulTxns.length === 0 ? (
          <div className="p-6 rounded-xl bg-slate-950/40 border border-slate-800 text-center flex flex-col items-center gap-2">
            <CheckCircle2 className="w-7 h-7 text-emerald-500/50" />
            <div className="text-sm font-semibold text-slate-300">No successful payments yet</div>
            <p className="text-xs text-slate-500 max-w-sm">
              Approved payments will appear here with recipient, amount, and post-payment remaining balance.
            </p>
          </div>
        ) : (
          <div className="space-y-2.5">
            {successfulTxns.map((tx) => {
              const dateStr = new Date(tx.timestamp).toLocaleString('en-IN', {
                day: '2-digit',
                month: 'short',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
                hour12: true,
              });

              return (
                <div
                  key={tx.id}
                  id={`tx_item_${tx.id}`}
                  data-testid={`tx_item_${tx.id}`}
                  className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800 hover:border-slate-700 transition-all flex flex-col gap-2"
                >
                  {/* Row 1: Recipient and Amount */}
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-bold text-slate-100 truncate pr-2">
                      {tx.recipient}
                    </span>
                    <span className="text-sm font-black text-cyan-400 shrink-0">
                      - {formatIndianCurrency(tx.amount)}
                    </span>
                  </div>

                  {/* Row 2: Date/Time and Status */}
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-slate-400 text-[11px]">{dateStr}</span>
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-emerald-500/15 text-emerald-300 text-[10px] font-bold">
                      <CheckCircle2 className="w-3 h-3 text-emerald-400" />
                      Status: Successful
                    </span>
                  </div>

                  {/* Row 3: Balance remaining after payment */}
                  <div className="flex items-center gap-1.5 text-[11px] font-semibold text-cyan-400/90 pt-1 border-t border-slate-800/60">
                    <Landmark className="w-3.5 h-3.5 text-cyan-400 shrink-0" />
                    <span>
                      Balance remaining after payment:{' '}
                      {tx.balanceAfter !== undefined
                        ? formatIndianCurrency(tx.balanceAfter)
                        : 'Recorded'}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
