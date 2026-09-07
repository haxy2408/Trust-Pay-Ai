import React, { useState, useEffect } from 'react';
import { AuditLogEntry, CoSigner, DualAuthAdminStats, QrAdminStats, UserAccount } from '../types';
import { TransactionStorage } from '../storage/transactionStorage';
import { formatIndianCurrency } from '../security/crypto';
import {
  ShieldAlert,
  Activity,
  QrCode,
  ShieldCheck,
  RefreshCw,
  X,
  Users,
  AlertTriangle,
  Lock,
  ArrowRightLeft,
  Key,
  KeyRound,
  Clock,
} from 'lucide-react';

interface AdminPanelModalProps {
  currentUser: UserAccount;
  isDarkTheme: boolean;
  onClose: () => void;
  onSwitchUser: (user: UserAccount) => void;
}

export const AdminPanelModal: React.FC<AdminPanelModalProps> = ({
  currentUser,
  isDarkTheme,
  onClose,
  onSwitchUser,
}) => {
  const [stats, setStats] = useState<QrAdminStats | null>(null);
  const [dualStats, setDualStats] = useState<DualAuthAdminStats | null>(null);
  const [coSigners, setCoSigners] = useState<CoSigner[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLogEntry[]>([]);
  const [allUsers, setAllUsers] = useState<UserAccount[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const loadData = async () => {
    setIsLoading(true);
    try {
      const res = await fetch('/api/p2p/admin-stats');
      if (res.ok) {
        const data = await res.json();
        setStats(data);
      } else {
        setStats(TransactionStorage.getQrAdminStats());
      }
    } catch {
      setStats(TransactionStorage.getQrAdminStats());
    }

    try {
      const dualRes = await fetch('/api/dual-auth/admin-stats');
      if (dualRes.ok) {
        const dualData = await dualRes.json();
        setDualStats(dualData);
      } else {
        setDualStats(TransactionStorage.getDualAuthAdminStats());
      }
    } catch {
      setDualStats(TransactionStorage.getDualAuthAdminStats());
    }

    setCoSigners(TransactionStorage.getCoSigners());
    setAuditLogs(TransactionStorage.getAuditLogs());
    setAllUsers(TransactionStorage.getUsers());
    setIsLoading(false);
  };

  useEffect(() => {
    loadData();
  }, []);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/75 backdrop-blur-sm animate-in fade-in duration-200">
      <div
        className={`w-full max-w-3xl rounded-2xl border shadow-2xl overflow-hidden flex flex-col max-h-[92vh] ${
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
            <div className="w-8 h-8 rounded-lg bg-indigo-500/10 border border-indigo-500/30 flex items-center justify-center text-indigo-600 dark:text-indigo-400">
              <Activity className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-base font-bold leading-tight">TrustPay Admin & SOC Center</h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Live QR Metrics, Attack Defense & Fraud Statistics
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={loadData}
              disabled={isLoading}
              className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
              title="Refresh data"
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
        <div className="p-5 overflow-y-auto space-y-5">
          {/* Real-time QR Statistics Grid */}
          <div>
            <div className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-2.5 flex items-center gap-1.5">
              <QrCode className="w-4 h-4 text-cyan-500" />
              Real-Time QR Security & Rotation Metrics
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div
                className={`p-3.5 rounded-xl border ${
                  isDarkTheme ? 'bg-slate-800/40 border-slate-700' : 'bg-slate-50 border-slate-200'
                }`}
              >
                <div className="text-[11px] font-semibold text-slate-500">Total QR Generated</div>
                <div className="text-xl font-extrabold text-slate-800 dark:text-slate-100 font-mono mt-1">
                  {stats?.totalGenerated ?? 0}
                </div>
                <div className="text-[10px] text-slate-400 mt-0.5">30s TTL per token</div>
              </div>

              <div
                className={`p-3.5 rounded-xl border ${
                  isDarkTheme ? 'bg-slate-800/40 border-slate-700' : 'bg-slate-50 border-slate-200'
                }`}
              >
                <div className="text-[11px] font-semibold text-emerald-600 dark:text-emerald-400">
                  Active in Rotation
                </div>
                <div className="text-xl font-extrabold text-emerald-600 dark:text-emerald-400 font-mono mt-1">
                  {stats?.activeCount ?? 0}
                </div>
                <div className="text-[10px] text-slate-400 mt-0.5">Live valid windows</div>
              </div>

              <div
                className={`p-3.5 rounded-xl border ${
                  isDarkTheme ? 'bg-slate-800/40 border-slate-700' : 'bg-slate-50 border-slate-200'
                }`}
              >
                <div className="text-[11px] font-semibold text-purple-600 dark:text-purple-400">
                  Used / Redeemed
                </div>
                <div className="text-xl font-extrabold text-purple-600 dark:text-purple-400 font-mono mt-1">
                  {stats?.usedCount ?? 0}
                </div>
                <div className="text-[10px] text-slate-400 mt-0.5">One-time executed</div>
              </div>

              <div
                className={`p-3.5 rounded-xl border ${
                  isDarkTheme ? 'bg-slate-800/40 border-slate-700' : 'bg-slate-50 border-slate-200'
                }`}
              >
                <div className="text-[11px] font-semibold text-rose-600 dark:text-rose-400">
                  Replays Thwarted
                </div>
                <div className="text-xl font-extrabold text-rose-600 dark:text-rose-400 font-mono mt-1">
                  {stats?.replayBlockedCount ?? 0}
                </div>
                <div className="text-[10px] text-slate-400 mt-0.5">Double-spend blocks</div>
              </div>
            </div>
          </div>

          {/* Dual Authorization Governance Metrics */}
          <div>
            <div className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-2.5 flex items-center justify-between">
              <span className="flex items-center gap-1.5">
                <KeyRound className="w-4 h-4 text-violet-500" />
                Dual-Authorization SOC & Governance Ledger
              </span>
              <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-violet-500/10 text-violet-600 dark:text-violet-400 border border-violet-500/20">
                TWO-SIGNER SECURITY
              </span>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div
                className={`p-3.5 rounded-xl border ${
                  isDarkTheme ? 'bg-slate-800/40 border-slate-700' : 'bg-slate-50 border-slate-200'
                }`}
              >
                <div className="text-[11px] font-semibold text-slate-500">Dual-Auth Requests</div>
                <div className="text-xl font-extrabold text-slate-800 dark:text-slate-100 font-mono mt-1">
                  {dualStats?.totalRequests ?? 0}
                </div>
                <div className="text-[10px] text-slate-400 mt-0.5">All initiated workflows</div>
              </div>

              <div
                className={`p-3.5 rounded-xl border ${
                  isDarkTheme ? 'bg-slate-800/40 border-slate-700' : 'bg-slate-50 border-slate-200'
                }`}
              >
                <div className="text-[11px] font-semibold text-amber-600 dark:text-amber-400">
                  Pending Second Signer
                </div>
                <div className="text-xl font-extrabold text-amber-600 dark:text-amber-400 font-mono mt-1">
                  {dualStats?.pendingCount ?? 0}
                </div>
                <div className="text-[10px] text-slate-400 mt-0.5">Active 5-min windows</div>
              </div>

              <div
                className={`p-3.5 rounded-xl border ${
                  isDarkTheme ? 'bg-slate-800/40 border-slate-700' : 'bg-slate-50 border-slate-200'
                }`}
              >
                <div className="text-[11px] font-semibold text-emerald-600 dark:text-emerald-400">
                  Approved (2 Signers)
                </div>
                <div className="text-xl font-extrabold text-emerald-600 dark:text-emerald-400 font-mono mt-1">
                  {dualStats?.approvedCount ?? 0}
                </div>
                <div className="text-[10px] text-slate-400 mt-0.5">Dual-signed & settled</div>
              </div>

              <div
                className={`p-3.5 rounded-xl border ${
                  isDarkTheme ? 'bg-slate-800/40 border-slate-700' : 'bg-slate-50 border-slate-200'
                }`}
              >
                <div className="text-[11px] font-semibold text-violet-600 dark:text-violet-400">
                  Dual-Settled Volume
                </div>
                <div className="text-base font-extrabold text-violet-600 dark:text-violet-400 font-mono mt-1 truncate">
                  {formatIndianCurrency(dualStats?.totalApprovedAmount ?? 0)}
                </div>
                <div className="text-[10px] text-slate-400 mt-0.5">
                  Rejected/Expired: {(dualStats?.rejectedCount ?? 0) + (dualStats?.expiredCount ?? 0)}
                </div>
              </div>
            </div>

            {/* Authorized Co-Signers Badges */}
            <div className="mt-3 flex items-center gap-2 flex-wrap text-xs">
              <span className="text-[11px] font-bold text-slate-500">Corporate Co-Signers:</span>
              {coSigners.map((c) => (
                <span
                  key={c.id}
                  className={`px-2.5 py-1 rounded-lg border text-[10px] font-semibold flex items-center gap-1.5 ${
                    c.active
                      ? isDarkTheme
                        ? 'bg-slate-800 border-slate-700 text-slate-300'
                        : 'bg-white border-slate-200 text-slate-700'
                      : 'bg-slate-200 dark:bg-slate-800 text-slate-400 line-through'
                  }`}
                >
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                  <strong>{c.fullName}</strong> ({c.role.split(' ')[0]})
                </span>
              ))}
            </div>
          </div>

          {/* Attack Defense Highlights */}
          <div
            className={`p-4 rounded-xl border space-y-3 ${
              isDarkTheme ? 'bg-slate-800/30 border-slate-700' : 'bg-slate-50 border-slate-200'
            }`}
          >
            <div className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 flex items-center justify-between">
              <span className="flex items-center gap-1.5">
                <ShieldCheck className="w-4 h-4 text-emerald-500" />
                Cryptographic Invariants Enforced
              </span>
              <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-500 border border-emerald-500/20">
                ACTIVE DEFENSE
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5 text-xs">
              <div className="p-2.5 rounded-lg bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800">
                <div className="font-bold text-slate-800 dark:text-slate-200">30s Auto Rotation</div>
                <div className="text-[11px] text-slate-500 mt-1">
                  Tokens expire automatically after 30 seconds. Outdated or captured screenshots cannot be redeemed.
                </div>
              </div>

              <div className="p-2.5 rounded-lg bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800">
                <div className="font-bold text-slate-800 dark:text-slate-200">Zero APK Secrets</div>
                <div className="text-[11px] text-slate-500 mt-1">
                  HMAC-SHA256 signature is strictly created and validated by Firebase Cloud Functions, never in client code.
                </div>
              </div>

              <div className="p-2.5 rounded-lg bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800">
                <div className="font-bold text-slate-800 dark:text-slate-200">Atomic Double-Spend Check</div>
                <div className="text-[11px] text-slate-500 mt-1">
                  Firestore transaction / atomic backend lock marks token USED concurrently with balance mutation.
                </div>
              </div>
            </div>
          </div>

          {/* Demo User Switcher (For testing 2 users / 2 phones easily) */}
          <div
            className={`p-4 rounded-xl border space-y-3 ${
              isDarkTheme ? 'bg-slate-800/30 border-slate-700' : 'bg-slate-50 border-slate-200'
            }`}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                <Users className="w-4 h-4 text-cyan-500" />
                Simulate Two Separate Users / Phones
              </div>
              <span className="text-[10px] text-slate-400">Active: {currentUser.fullName}</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {allUsers.map((u) => {
                const isSelected = u.email === currentUser.email;
                const bal = TransactionStorage.getUserBalance(u.email);

                return (
                  <div
                    key={u.email}
                    onClick={() => {
                      if (!isSelected) {
                        TransactionStorage.setActiveUser(u);
                        onSwitchUser(u);
                      }
                    }}
                    className={`p-3 rounded-xl border flex items-center justify-between cursor-pointer transition ${
                      isSelected
                        ? 'border-cyan-500 bg-cyan-500/10 text-cyan-700 dark:text-cyan-300'
                        : isDarkTheme
                        ? 'border-slate-700 bg-slate-900 hover:bg-slate-800'
                        : 'border-slate-200 bg-white hover:bg-slate-100'
                    }`}
                  >
                    <div>
                      <div className="text-xs font-bold">{u.fullName}</div>
                      <div className="text-[11px] font-mono text-slate-500">{u.email}</div>
                    </div>
                    <div className="text-right">
                      <div className="text-xs font-mono font-bold">{formatIndianCurrency(bal)}</div>
                      <span className="text-[10px] font-semibold">
                        {isSelected ? 'Current User' : 'Click to Switch'}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Audit Logs Stream */}
          <div>
            <div className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-2 flex items-center justify-between">
              <span>Security Audit Trail ({auditLogs.length} events)</span>
              <span className="text-[10px] text-slate-400">Latest immutable logs</span>
            </div>

            <div className="space-y-1.5 max-h-48 overflow-y-auto pr-1">
              {auditLogs.slice(0, 15).map((log) => (
                <div
                  key={log.id}
                  className={`p-2.5 rounded-lg border text-[11px] flex items-center justify-between ${
                    log.status === 'DANGER'
                      ? 'bg-rose-500/5 border-rose-500/20 text-rose-700 dark:text-rose-400'
                      : log.status === 'WARNING'
                      ? 'bg-amber-500/5 border-amber-500/20 text-amber-700 dark:text-amber-400'
                      : isDarkTheme
                      ? 'bg-slate-900/60 border-slate-800 text-slate-300'
                      : 'bg-slate-50 border-slate-200 text-slate-700'
                  }`}
                >
                  <div>
                    <div className="font-bold flex items-center gap-1.5">
                      <span>{log.title}</span>
                      <span className="text-[9px] font-mono text-slate-400">
                        {new Date(log.timestamp).toLocaleTimeString()}
                      </span>
                    </div>
                    <div className="text-slate-500 dark:text-slate-400 mt-0.5">{log.details}</div>
                  </div>
                  {log.hash && (
                    <span className="text-[9px] font-mono text-cyan-600 truncate max-w-[100px]" title={log.hash}>
                      {log.hash.substring(0, 10)}...
                    </span>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Disclaimer */}
          <div className="text-center pt-2 border-t border-slate-200 dark:border-slate-800">
            <span className="text-[11px] font-semibold text-amber-600 dark:text-amber-400">
              Simulation only - no real money transfer
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
