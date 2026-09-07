import React from 'react';
import { CheckCircle2, AlertCircle, AlertTriangle, Lock, Sparkles } from 'lucide-react';
import { TrustPayUiState } from '../types';

interface RiskAssessmentCardProps {
  uiState: TrustPayUiState;
}

export const RiskAssessmentCard: React.FC<RiskAssessmentCardProps> = ({ uiState }) => {
  const score = uiState.currentRiskScore ?? 0;
  const decision = uiState.finalDecision;
  const isDark = uiState.isDarkTheme;

  // Determine badge styling based on finalDecision and score
  let statusColor = 'text-emerald-400';
  let badgeBg = 'bg-emerald-500/15 border-emerald-500/40 text-emerald-300';
  let statusTitle = 'APPROVED';
  let barColor = 'bg-emerald-500';

  if (decision === 'TAMPER_DETECTED') {
    statusColor = 'text-red-400';
    badgeBg = 'bg-red-500/15 border-red-500/50 text-red-300';
    statusTitle = 'TAMPERING DETECTED';
    barColor = 'bg-red-500';
  } else if (decision === 'BLOCKED') {
    statusColor = 'text-red-400';
    badgeBg = 'bg-red-500/15 border-red-500/50 text-red-300';
    statusTitle = 'PAYMENT BLOCKED';
    barColor = 'bg-red-500';
  } else if (decision === 'APPROVED') {
    statusColor = 'text-emerald-400';
    badgeBg = 'bg-emerald-500/15 border-emerald-500/40 text-emerald-300';
    statusTitle = 'APPROVED';
    barColor = 'bg-emerald-500';
  } else {
    // NONE (Pending / intermediate)
    if (score >= 70) {
      statusColor = 'text-amber-400';
      badgeBg = 'bg-amber-500/15 border-amber-500/40 text-amber-300';
      statusTitle = 'ELEVATED RISK / ANOMALY';
      barColor = 'bg-amber-500';
    } else {
      statusColor = 'text-emerald-400';
      badgeBg = 'bg-emerald-500/15 border-emerald-500/40 text-emerald-300';
      statusTitle = 'LOW RISK ASSESSMENT';
      barColor = 'bg-emerald-500';
    }
  }

  return (
    <div
      className={`rounded-2xl p-5 md:p-6 border transition-all ${
        isDark
          ? 'bg-slate-900/90 border-slate-800 text-slate-100 shadow-xl'
          : 'bg-white border-slate-200 text-slate-900 shadow-md'
      }`}
    >
      <div className="flex flex-col gap-4">
        {/* Header with Score and Status Title */}
        <div className="flex items-center justify-between">
          <div>
            <div className="text-[10px] font-black uppercase tracking-widest text-slate-400">
              RISK ASSESSMENT
            </div>
            <div className="flex items-baseline gap-1">
              <span className={`text-3xl font-black ${statusColor}`}>{score}</span>
              <span className="text-sm font-bold text-slate-500">/100</span>
            </div>
          </div>

          <div
            className={`px-3 py-1.5 rounded-full border text-xs font-black tracking-wider uppercase ${badgeBg}`}
          >
            {statusTitle}
          </div>
        </div>

        {/* Progress bar */}
        <div className="w-full h-2 rounded-full bg-slate-800 overflow-hidden">
          <div
            className={`h-full ${barColor} transition-all duration-500 ease-out`}
            style={{ width: `${Math.min(100, Math.max(0, score))}%` }}
          />
        </div>

        {/* Decision Message */}
        {uiState.decisionMessage && (
          <div
            className={`p-3 rounded-xl border flex items-start gap-2.5 text-xs font-semibold ${
              decision === 'APPROVED'
                ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
                : decision === 'TAMPER_DETECTED' || decision === 'BLOCKED'
                ? 'bg-red-500/10 border-red-500/30 text-red-300'
                : 'bg-slate-800 border-slate-700 text-slate-200'
            }`}
          >
            {decision === 'APPROVED' ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
            ) : decision === 'TAMPER_DETECTED' || decision === 'BLOCKED' ? (
              <AlertCircle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
            ) : (
              <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
            )}
            <span>{uiState.decisionMessage}</span>
          </div>
        )}

        {/* Detected Security Signals */}
        {uiState.signals.length > 0 && (
          <div>
            <div className="text-xs font-bold text-slate-400 mb-1.5">
              Detected Security Signals:
            </div>
            <div className="space-y-1">
              {uiState.signals.map((signal, idx) => (
                <div key={idx} className="flex items-center gap-2 text-xs text-slate-300">
                  <span className={`w-1.5 h-1.5 rounded-full shrink-0 ${barColor}`} />
                  <span>{signal}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Cryptographic SHA-256 Binding Display */}
        {uiState.currentAnalysis && (
          <div className="p-3.5 rounded-xl bg-slate-950 border border-cyan-500/30">
            <div className="flex items-center gap-2 mb-1">
              <Lock className="w-3.5 h-3.5 text-cyan-400" />
              <div className="text-xs font-bold text-white">SHA-256 Payload Binding Digest</div>
            </div>
            <div className="text-[10px] text-slate-400 font-mono">
              Format: TXN_ID|RECIPIENT|AMOUNT|CURRENCY
            </div>
            <div className="mt-1.5 text-[11px] font-mono text-cyan-300 break-all select-all bg-slate-900/80 p-2 rounded border border-slate-800">
              {uiState.currentAnalysis.boundHash}
            </div>
          </div>
        )}

        {/* Optional AI Risk Insight / SOC Commentary */}
        {uiState.currentAnalysis?.aiInsights && (
          <div className="p-3 rounded-xl bg-cyan-950/30 border border-cyan-500/20 text-xs">
            <div className="flex items-center gap-1.5 text-cyan-300 font-bold mb-1">
              <Sparkles className="w-3.5 h-3.5 text-cyan-400" />
              <span>TrustPay Security Intelligence</span>
            </div>
            <div className="text-slate-300 leading-relaxed">
              {uiState.currentAnalysis.aiInsights}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
