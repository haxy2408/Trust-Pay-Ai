import React from 'react';
import { BookOpen, X, Shield, AlertTriangle, Fingerprint, Gauge, RotateCcw } from 'lucide-react';

interface DemoScenarioGuideModalProps {
  onClose: () => void;
}

export const DemoScenarioGuideModal: React.FC<DemoScenarioGuideModalProps> = ({ onClose }) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
      <div className="w-full max-w-2xl max-h-[85vh] overflow-y-auto rounded-2xl bg-slate-900 border border-cyan-500/50 text-white shadow-2xl p-6 flex flex-col gap-5">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center gap-2.5">
            <BookOpen className="w-5 h-5 text-cyan-400" />
            <h3 className="text-base font-bold text-white">TrustPay Demo Scenario Walkthrough</h3>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Introduction */}
        <p className="text-xs text-slate-300 leading-relaxed">
          TrustPay is an adaptive payment security shield that enforces cryptographic SHA-256 payload binding,
          sliding-window velocity anomaly detection, and dynamic multi-factor challenge protocols.
        </p>

        {/* Scenarios Grid */}
        <div className="space-y-3 text-xs">
          {/* Scenario 1 */}
          <div className="p-3 rounded-xl bg-slate-950 border border-slate-800">
            <div className="flex items-center gap-2 font-bold text-emerald-400 mb-1">
              <Shield className="w-4 h-4" />
              <span>Scenario 1: Low-Risk Standard Payment</span>
            </div>
            <p className="text-slate-300 mb-1">
              Click the <b>₹2,500 (Low-Risk)</b> quick chip and click <b>Analyse & Pay</b>.
            </p>
            <p className="text-slate-400 text-[11px]">
              Outcome: Score &lt; 70. Instantly approved, SHA-256 cryptographic binding generated, and balance deducted.
            </p>
          </div>

          {/* Scenario 2 */}
          <div className="p-3 rounded-xl bg-slate-950 border border-slate-800">
            <div className="flex items-center gap-2 font-bold text-amber-400 mb-1">
              <Fingerprint className="w-4 h-4" />
              <span>Scenario 2: High-Value Payment</span>
            </div>
            <p className="text-slate-300 mb-1">
              Click the <b>₹45,000 (High-Value)</b> quick chip and click <b>Analyse & Pay</b>.
            </p>
            <p className="text-slate-400 text-[11px]">
              Outcome: High transaction value mandates Biometric confirmation before releasing payment.
            </p>
          </div>

          {/* Scenario 3 */}
          <div className="p-3 rounded-xl bg-slate-950 border border-slate-800">
            <div className="flex items-center gap-2 font-bold text-red-400 mb-1">
              <AlertTriangle className="w-4 h-4" />
              <span>Scenario 3: Post-Authorization Amount Tampering Attack</span>
            </div>
            <p className="text-slate-300 mb-1">
              Click the red <b>Attack Demo: Tamper Amount</b> button.
            </p>
            <p className="text-slate-400 text-[11px]">
              Outcome: Simulates malware attempting to inject ₹99,999 after user authorization. TrustPay’s cryptographic integrity re-check catches the mismatch and blocks the transaction!
            </p>
          </div>

          {/* Scenario 4 */}
          <div className="p-3 rounded-xl bg-slate-950 border border-slate-800">
            <div className="flex items-center gap-2 font-bold text-cyan-400 mb-1">
              <Gauge className="w-4 h-4" />
              <span>Scenario 4: Rolling 5-Minute Velocity Burst Anomaly</span>
            </div>
            <p className="text-slate-300 mb-1">
              Select <b>₹5,000 (Velocity Demo)</b> and execute 3 payments within 5 minutes.
            </p>
            <p className="text-slate-400 text-[11px]">
              Outcome: On the 3rd payment, the cumulative total reaches ₹15,000 with 3 transactions in 5 minutes. TrustPay flags a Velocity Anomaly and triggers the <b>3-Step Authorization Protocol</b>:
              <br />1. Dynamic Designated Finger challenge (defeats replay bots)
              <br />2. Transaction-Detail Cognitive challenge
              <br />3. Demo OTP Verification
            </p>
          </div>

          {/* Scenario 5 */}
          <div className="p-3 rounded-xl bg-slate-950 border border-slate-800">
            <div className="flex items-center gap-2 font-bold text-slate-300 mb-1">
              <RotateCcw className="w-4 h-4 text-cyan-400" />
              <span>Scenario 5: Resetting Demo Sandbox Balance</span>
            </div>
            <p className="text-slate-300">
              Click <b>Demo Balance Reset</b> on the top balance card to restore available funds to ₹1,00,000.00.
            </p>
          </div>
        </div>

        {/* Footer */}
        <div className="border-t border-slate-800 pt-3 flex justify-end">
          <button
            onClick={onClose}
            className="py-2 px-4 rounded-xl font-bold text-xs bg-cyan-600 hover:bg-cyan-500 text-white transition-colors"
          >
            Got it, Let&apos;s Test
          </button>
        </div>
      </div>
    </div>
  );
};
