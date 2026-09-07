import React, { useState, useEffect } from 'react';
import { QrPaymentRequest } from '../types';
import { TransactionStorage } from '../storage/transactionStorage';
import { formatIndianCurrency } from '../security/crypto';
import { QrCode, Clock, ShieldCheck, CheckCircle2, AlertCircle, X, RefreshCw } from 'lucide-react';

interface QrHistoryModalProps {
  isDarkTheme: boolean;
  onClose: () => void;
}

export const QrHistoryModal: React.FC<QrHistoryModalProps> = ({ isDarkTheme, onClose }) => {
  const [tokens, setTokens] = useState<QrPaymentRequest[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const fetchHistory = async () => {
    setIsLoading(true);
    try {
      const res = await fetch('/api/p2p/tokens');
      if (res.ok) {
        const data = await res.json();
        setTokens(data.tokens || []);
      } else {
        setTokens(TransactionStorage.getQrTokens());
      }
    } catch {
      setTokens(TransactionStorage.getQrTokens());
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchHistory();
  }, []);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/75 backdrop-blur-sm animate-in fade-in duration-200">
      <div
        className={`w-full max-w-2xl rounded-2xl border shadow-2xl overflow-hidden flex flex-col max-h-[90vh] ${
          isDarkTheme ? 'bg-slate-900 border-slate-700 text-slate-100' : 'bg-white border-slate-200 text-slate-800'
        }`}
      >
        {/* Header */}
        <div
          className={`px-5 py-4 flex items-center justify-between border-b ${
            isDarkTheme ? 'border-slate-800 bg-slate-900/60' : 'border-slate-100 bg-slate-50/80'
          }`}
        >
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-cyan-500/10 border border-cyan-500/30 flex items-center justify-center text-cyan-600 dark:text-cyan-400">
              <QrCode className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-base font-bold leading-tight">P2P QR Request History</h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Rotated Tokens, Audit Trails & Transaction Outcomes
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={fetchHistory}
              disabled={isLoading}
              className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
              title="Refresh history"
            >
              <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
            </button>
            <button
              onClick={onClose}
              className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Content Body */}
        <div className="p-5 overflow-y-auto space-y-3">
          {tokens.length === 0 ? (
            <div className="p-8 text-center text-slate-400 text-xs border border-dashed rounded-xl">
              No QR tokens recorded yet. Generate or scan a QR code to view historical audit logs.
            </div>
          ) : (
            tokens.map((token) => {
              const isUsed = token.status === 'USED';
              const isExpired = Date.now() > token.expiresAt || token.status === 'EXPIRED';

              return (
                <div
                  key={token.tokenId}
                  className={`p-3.5 rounded-xl border text-xs space-y-2 transition ${
                    isDarkTheme ? 'bg-slate-800/40 border-slate-700/80' : 'bg-slate-50/80 border-slate-200'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="font-mono font-bold text-slate-800 dark:text-slate-200">
                        {token.tokenId}
                      </span>
                      <span
                        className={`text-[10px] font-bold px-2 py-0.5 rounded-full uppercase ${
                          isUsed
                            ? 'bg-purple-500/10 text-purple-600 dark:text-purple-400 border border-purple-500/20'
                            : isExpired
                            ? 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/20'
                            : 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20'
                        }`}
                      >
                        {isUsed ? 'USED' : isExpired ? 'EXPIRED' : 'ACTIVE'}
                      </span>
                    </div>

                    <div className="font-mono font-bold text-cyan-600 dark:text-cyan-400">
                      {token.amount ? formatIndianCurrency(token.amount) : 'Open Amount'}
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-2 text-[11px] text-slate-500 dark:text-slate-400">
                    <div>
                      <span className="font-semibold">Receiver: </span>
                      <span className="text-slate-700 dark:text-slate-300 font-medium">
                        {token.receiverName} ({token.receiverId})
                      </span>
                    </div>
                    <div>
                      <span className="font-semibold">Created: </span>
                      <span>{new Date(token.createdAt).toLocaleTimeString()}</span>
                    </div>
                    <div>
                      <span className="font-semibold">Expiry TTL: </span>
                      <span>{new Date(token.expiresAt).toLocaleTimeString()} (30s window)</span>
                    </div>
                    <div>
                      <span className="font-semibold">Note: </span>
                      <span>{token.note || 'None'}</span>
                    </div>
                  </div>

                  {/* Outcome Result */}
                  <div
                    className={`p-2 rounded-lg text-[11px] flex items-center justify-between ${
                      isUsed
                        ? 'bg-purple-500/5 text-purple-700 dark:text-purple-300 border border-purple-500/20'
                        : isExpired
                        ? 'bg-amber-500/5 text-amber-700 dark:text-amber-300 border border-amber-500/20'
                        : 'bg-emerald-500/5 text-emerald-700 dark:text-emerald-300 border border-emerald-500/20'
                    }`}
                  >
                    <div className="flex items-center gap-1.5 font-medium">
                      {isUsed ? (
                        <>
                          <CheckCircle2 className="w-3.5 h-3.5 text-purple-500" />
                          <span>
                            Settled by {token.usedBySenderName || token.usedBySenderId || 'Sender'} at{' '}
                            {token.usedAt ? new Date(token.usedAt).toLocaleTimeString() : 'N/A'} (Tx:{' '}
                            {token.transactionId?.substring(0, 14)}...)
                          </span>
                        </>
                      ) : isExpired ? (
                        <>
                          <Clock className="w-3.5 h-3.5 text-amber-500" />
                          <span>Automatically rotated & expired after 30s. Replay prohibited.</span>
                        </>
                      ) : (
                        <>
                          <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
                          <span>Active on server. Waiting for scanning user.</span>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              );
            })
          )}

          <div className="text-center pt-2">
            <span className="text-[11px] font-semibold text-amber-600 dark:text-amber-400">
              Simulation only - no real money transfer
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
