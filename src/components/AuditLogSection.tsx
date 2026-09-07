import React from 'react';
import { ShieldAlert, Trash2 } from 'lucide-react';
import { AuditLogEntry } from '../types';

interface AuditLogSectionProps {
  logs: AuditLogEntry[];
  onClearLogs: () => void;
}

export const AuditLogSection: React.FC<AuditLogSectionProps> = ({ logs, onClearLogs }) => {
  return (
    <div className="rounded-2xl p-5 md:p-6 border border-slate-800 bg-slate-900/90 text-slate-100 shadow-xl">
      <div className="flex flex-col gap-4">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <ShieldAlert className="w-5 h-5 text-sky-500" />
            <h3 className="text-base font-bold text-slate-100">Timestamped Audit Log</h3>
          </div>

          {logs.length > 0 && (
            <button
              onClick={onClearLogs}
              className="text-xs font-semibold text-slate-400 hover:text-red-400 flex items-center gap-1 transition-colors"
            >
              <Trash2 className="w-3.5 h-3.5" />
              Clear
            </button>
          )}
        </div>

        {/* Logs */}
        {logs.length === 0 ? (
          <div className="text-xs italic text-slate-500 py-3 text-center">
            No audit events recorded yet.
          </div>
        ) : (
          <div className="space-y-2 max-h-96 overflow-y-auto pr-1">
            {logs.slice(0, 20).map((entry) => {
              const timeStr = new Date(entry.timestamp).toLocaleTimeString([], {
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
              });

              let dotColor = 'bg-cyan-400';
              let badgeBg = 'bg-cyan-500/10 border-cyan-500/30 text-cyan-300';
              if (entry.status === 'SUCCESS') {
                dotColor = 'bg-emerald-400';
                badgeBg = 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300';
              } else if (entry.status === 'WARNING') {
                dotColor = 'bg-amber-400';
                badgeBg = 'bg-amber-500/10 border-amber-500/30 text-amber-300';
              } else if (entry.status === 'DANGER') {
                dotColor = 'bg-red-400';
                badgeBg = 'bg-red-500/10 border-red-500/30 text-red-300';
              }

              return (
                <div
                  key={entry.id}
                  className="p-3 rounded-xl bg-slate-950/60 border border-slate-800/80 flex flex-col gap-1 text-xs"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2 font-bold text-slate-200">
                      <span className={`w-2 h-2 rounded-full ${dotColor}`} />
                      <span>{entry.title}</span>
                    </div>
                    <span className="text-[11px] text-slate-400 font-mono">{timeStr}</span>
                  </div>

                  <div className="text-slate-300 text-[11.5px] leading-relaxed pl-4">
                    {entry.details}
                  </div>

                  {entry.hash && (
                    <div className="pl-4 text-[10px] font-mono text-cyan-400 truncate">
                      SHA-256: {entry.hash.substring(0, 24)}...
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
