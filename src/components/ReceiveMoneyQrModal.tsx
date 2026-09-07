import React, { useState, useEffect, useRef } from 'react';
import { QrPaymentRequest, UserAccount } from '../types';
import { TransactionStorage } from '../storage/transactionStorage';
import { formatIndianCurrency } from '../security/crypto';
import QRCode from 'qrcode';
import {
  QrCode,
  RefreshCw,
  Clock,
  ShieldCheck,
  AlertTriangle,
  Copy,
  Check,
  X,
  Sparkles,
  ArrowDownLeft,
} from 'lucide-react';

interface ReceiveMoneyQrModalProps {
  currentUser: UserAccount;
  isDarkTheme: boolean;
  onClose: () => void;
  onQrGenerated?: (token: QrPaymentRequest) => void;
}

export const ReceiveMoneyQrModal: React.FC<ReceiveMoneyQrModalProps> = ({
  currentUser,
  isDarkTheme,
  onClose,
  onQrGenerated,
}) => {
  const [amountInput, setAmountInput] = useState('');
  const [noteInput, setNoteInput] = useState('');
  const [activeToken, setActiveToken] = useState<QrPaymentRequest | null>(null);
  const [status, setStatus] = useState<'ACTIVE' | 'EXPIRED' | 'USED' | 'REFRESHING'>('ACTIVE');
  const [secondsRemaining, setSecondsRemaining] = useState(30);
  const [isLoading, setIsLoading] = useState(false);
  const [copied, setCopied] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const rotationTimerRef = useRef<any>(null);

  // Generate a new secure QR token from server
  const generateNewToken = async (customAmount?: number | null, customNote?: string | null) => {
    setIsLoading(true);
    setStatus('REFRESHING');
    setErrorMessage(null);

    const amt = customAmount !== undefined ? customAmount : (amountInput.trim() ? parseFloat(amountInput) : null);
    const note = customNote !== undefined ? customNote : (noteInput.trim() || null);

    try {
      const res = await fetch('/api/p2p/create-qr-token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          receiverId: currentUser.email,
          receiverName: currentUser.fullName,
          amount: amt,
          note: note,
        }),
      });

      const data = await res.json();
      if (!res.ok || !data.success) {
        throw new Error(data.error || 'Failed to create QR token');
      }

      const token: QrPaymentRequest = data.token;
      setActiveToken(token);
      setStatus('ACTIVE');
      setSecondsRemaining(30);

      // Also persist in local storage for instant offline / cross-tab sync
      TransactionStorage.saveQrToken(token);
      TransactionStorage.appendAuditLog({
        id: `LOG_QR_GEN_${Date.now()}`,
        timestamp: Date.now(),
        title: 'Secure QR Token Generated',
        details: `Token ${token.tokenId.substring(0, 14)}... created with 30s TTL. HMAC signed by backend.`,
        status: 'SUCCESS',
      });

      if (onQrGenerated) {
        onQrGenerated(token);
      }
    } catch (err: any) {
      console.error('Failed to create QR token:', err);
      setErrorMessage(err.message || 'Error generating secure token. Please retry.');
      setStatus('EXPIRED');
    } finally {
      setIsLoading(false);
    }
  };

  // Initial load: generate first QR
  useEffect(() => {
    generateNewToken(null, null);
    return () => {
      if (rotationTimerRef.current) clearInterval(rotationTimerRef.current);
    };
  }, []);

  // Render QR to canvas whenever activeToken changes
  useEffect(() => {
    if (!activeToken || !canvasRef.current) return;

    QRCode.toCanvas(
      canvasRef.current,
      activeToken.payloadUri,
      {
        width: 220,
        margin: 2,
        color: {
          dark: '#0f172a',
          light: '#ffffff',
        },
        errorCorrectionLevel: 'H',
      },
      (err) => {
        if (err) console.error('QR code render error:', err);
      }
    );
  }, [activeToken]);

  // 30-Second Countdown & Automatic Rotation Engine
  useEffect(() => {
    if (status !== 'ACTIVE' || !activeToken) return;

    rotationTimerRef.current = setInterval(() => {
      const now = Date.now();
      const diffMs = activeToken.expiresAt - now;
      const secs = Math.max(0, Math.ceil(diffMs / 1000));

      setSecondsRemaining(secs);

      if (secs <= 0) {
        clearInterval(rotationTimerRef.current);
        setStatus('EXPIRED');
        TransactionStorage.updateQrToken(activeToken.tokenId, { status: 'EXPIRED' });
        TransactionStorage.recordQrSecurityEvent('expired');
        TransactionStorage.appendAuditLog({
          id: `LOG_QR_ROT_${Date.now()}`,
          timestamp: Date.now(),
          title: 'Secure QR Rotation Triggered',
          details: `Token ${activeToken.tokenId.substring(0, 14)}... expired after 30s. Automatically rotating new one-time token.`,
          status: 'INFO',
        });

        // Automatically rotate to new QR code immediately as required!
        generateNewToken();
      }
    }, 500);

    return () => {
      if (rotationTimerRef.current) clearInterval(rotationTimerRef.current);
    };
  }, [status, activeToken]);

  const handleCopyUri = () => {
    if (!activeToken) return;
    navigator.clipboard.writeText(activeToken.payloadUri);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const formattedTimer = `00:${secondsRemaining < 10 ? `0${secondsRemaining}` : secondsRemaining}`;
  const timerPercentage = (secondsRemaining / 30) * 100;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/75 backdrop-blur-sm animate-in fade-in duration-200">
      <div
        className={`w-full max-w-md rounded-2xl border shadow-2xl overflow-hidden flex flex-col max-h-[92vh] ${
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
              <ArrowDownLeft className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-base font-bold leading-tight">Receive Money - Generate QR</h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">Secure P2P Payment Request</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body */}
        <div className="p-5 overflow-y-auto space-y-4">
          {/* Optional Amount & Note Inputs */}
          <div
            className={`p-3.5 rounded-xl border space-y-2.5 ${
              isDarkTheme ? 'bg-slate-800/50 border-slate-700/60' : 'bg-slate-50 border-slate-200'
            }`}
          >
            <div className="flex gap-2">
              <div className="flex-1">
                <label className="block text-[11px] font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1">
                  Requested Amount (Optional)
                </label>
                <div className="relative">
                  <span className="absolute left-2.5 top-2 text-sm text-slate-400 font-bold">₹</span>
                  <input
                    type="number"
                    value={amountInput}
                    onChange={(e) => setAmountInput(e.target.value)}
                    placeholder="Leave empty for open amount"
                    className={`w-full pl-7 pr-3 py-1.5 text-xs rounded-lg border outline-none font-mono ${
                      isDarkTheme
                        ? 'bg-slate-900 border-slate-700 text-white focus:border-cyan-500'
                        : 'bg-white border-slate-300 text-slate-900 focus:border-cyan-600'
                    }`}
                  />
                </div>
              </div>
              <div className="flex-1">
                <label className="block text-[11px] font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1">
                  Note (Optional)
                </label>
                <input
                  type="text"
                  value={noteInput}
                  onChange={(e) => setNoteInput(e.target.value)}
                  placeholder="e.g. Lunch split, Project fee"
                  className={`w-full px-3 py-1.5 text-xs rounded-lg border outline-none ${
                    isDarkTheme
                      ? 'bg-slate-900 border-slate-700 text-white focus:border-cyan-500'
                      : 'bg-white border-slate-300 text-slate-900 focus:border-cyan-600'
                  }`}
                />
              </div>
            </div>

            <button
              onClick={() => generateNewToken()}
              disabled={isLoading}
              className="w-full py-2 text-xs font-semibold rounded-lg bg-cyan-600 hover:bg-cyan-700 text-white flex items-center justify-center gap-1.5 transition shadow-sm disabled:opacity-50"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
              Update QR Parameters
            </button>
          </div>

          {/* QR Code Presentation Box */}
          <div className="flex flex-col items-center justify-center p-5 rounded-2xl bg-slate-950/5 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-700/80 relative">
            {/* Status Pill */}
            <div className="flex items-center gap-2 mb-3">
              <span
                className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider ${
                  status === 'ACTIVE'
                    ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30'
                    : status === 'REFRESHING'
                    ? 'bg-cyan-500/10 text-cyan-600 dark:text-cyan-400 border border-cyan-500/30'
                    : 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/30'
                }`}
              >
                <span
                  className={`w-2 h-2 rounded-full ${
                    status === 'ACTIVE'
                      ? 'bg-emerald-500 animate-pulse'
                      : status === 'REFRESHING'
                      ? 'bg-cyan-500 animate-spin'
                      : 'bg-amber-500'
                  }`}
                />
                {status === 'ACTIVE' ? 'Secure QR active' : status}
              </span>
            </div>

            {/* Canvas Container */}
            <div className="p-3 bg-white rounded-xl shadow-lg border border-slate-200 relative">
              <canvas ref={canvasRef} className="block rounded-lg" />
              {isLoading && (
                <div className="absolute inset-0 bg-white/80 rounded-xl flex flex-col items-center justify-center gap-2">
                  <RefreshCw className="w-8 h-8 text-cyan-600 animate-spin" />
                  <span className="text-xs font-bold text-slate-700">Rotating QR...</span>
                </div>
              )}
            </div>

            {/* Dynamic Details */}
            <div className="mt-4 text-center space-y-1">
              <div className="text-sm font-bold text-slate-800 dark:text-white">
                Payee: {currentUser.fullName}
              </div>
              {activeToken?.amount && (
                <div className="text-lg font-black text-cyan-600 dark:text-cyan-400 font-mono">
                  {formatIndianCurrency(activeToken.amount)}
                </div>
              )}
              {activeToken?.note && (
                <div className="text-xs text-slate-500 dark:text-slate-400 italic">
                  “{activeToken.note}”
                </div>
              )}
            </div>

            {/* Countdown Timer with Rotation Progress */}
            <div className="w-full mt-4 p-3 rounded-xl bg-slate-100 dark:bg-slate-900/80 border border-slate-200 dark:border-slate-800 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Clock className="w-4 h-4 text-amber-500 animate-pulse" />
                <div className="text-left">
                  <div className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                    Secure QR Rotation
                  </div>
                  <div className="text-sm font-mono font-bold text-slate-800 dark:text-slate-200">
                    Expires in <span className="text-amber-500">{formattedTimer}</span>
                  </div>
                </div>
              </div>

              {/* Manual Refresh Button */}
              <button
                onClick={() => generateNewToken()}
                disabled={isLoading}
                className="px-3 py-1.5 text-xs font-bold rounded-lg bg-slate-200 dark:bg-slate-800 hover:bg-slate-300 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 flex items-center gap-1 transition"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
                Refresh QR Now
              </button>
            </div>

            {/* Progress Bar */}
            <div className="w-full bg-slate-200 dark:bg-slate-800 h-1.5 rounded-full overflow-hidden mt-2">
              <div
                className="h-full bg-amber-500 transition-all duration-500"
                style={{ width: `${timerPercentage}%` }}
              />
            </div>

            <div className="mt-2 text-[11px] text-slate-500 dark:text-slate-400 text-center">
              This QR refreshes automatically every 30 seconds
            </div>
          </div>

          {/* Token Security Metadata */}
          {activeToken && (
            <div
              className={`p-3 rounded-xl border text-xs space-y-1.5 font-mono ${
                isDarkTheme ? 'bg-slate-900/50 border-slate-800 text-slate-400' : 'bg-slate-50 border-slate-200 text-slate-600'
              }`}
            >
              <div className="flex justify-between items-center">
                <span className="font-semibold text-slate-500">Token ID:</span>
                <span className="truncate max-w-[200px] text-cyan-600 dark:text-cyan-400">
                  {activeToken.tokenId}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="font-semibold text-slate-500">HMAC Integrity:</span>
                <span className="text-emerald-500 font-bold flex items-center gap-1">
                  <ShieldCheck className="w-3.5 h-3.5" /> Backend Signed
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="font-semibold text-slate-500">One-Time Nonce:</span>
                <span className="truncate max-w-[150px]">{activeToken.nonce.substring(0, 16)}...</span>
              </div>
            </div>
          )}

          {/* Action Row */}
          <div className="flex gap-2">
            <button
              onClick={handleCopyUri}
              className={`flex-1 py-2 text-xs font-semibold rounded-xl border flex items-center justify-center gap-1.5 transition ${
                copied
                  ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-600 dark:text-emerald-400'
                  : isDarkTheme
                  ? 'border-slate-700 bg-slate-800 hover:bg-slate-700 text-white'
                  : 'border-slate-300 bg-slate-100 hover:bg-slate-200 text-slate-800'
              }`}
            >
              {copied ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
              {copied ? 'Payload Copied!' : 'Copy QR Payload URI'}
            </button>
          </div>

          {/* Simulation Disclaimer */}
          <div className="text-center pt-1 border-t border-slate-200 dark:border-slate-800">
            <span className="text-[11px] font-semibold text-amber-600 dark:text-amber-400">
              Simulation only - no real money transfer
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
