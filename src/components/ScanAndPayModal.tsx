import React, { useState, useEffect, useRef } from 'react';
import {
  AuditLogEntry,
  PaymentTransaction,
  QrPaymentRequest,
  QrValidationResult,
  UserAccount,
  VerificationState,
} from '../types';
import { TransactionStorage } from '../storage/transactionStorage';
import {
  computeBindingHash,
  formatIndianCurrency,
  parseQrPayloadUri,
  sha256Sync,
} from '../security/crypto';
import jsQR from 'jsqr';
import {
  Camera,
  ScanLine,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Clock,
  ShieldCheck,
  ArrowRight,
  RefreshCw,
  X,
  Fingerprint,
  Lock,
  KeyRound,
  ShieldAlert,
  Sparkles,
} from 'lucide-react';

interface ScanAndPayModalProps {
  currentUser: UserAccount;
  currentBalance: number;
  isDarkTheme: boolean;
  onClose: () => void;
  onPaymentCompleted: (tx: PaymentTransaction, newBalance: number) => void;
  onRequestSecurityVerification?: (state: VerificationState, onVerified: () => void) => void;
}

export const ScanAndPayModal: React.FC<ScanAndPayModalProps> = ({
  currentUser,
  currentBalance,
  isDarkTheme,
  onClose,
  onPaymentCompleted,
}) => {
  const [step, setStep] = useState<'SCAN' | 'CONFIRM' | 'VERIFY' | 'SUCCESS'>('SCAN');
  const [activeTab, setActiveTab] = useState<'CAMERA' | 'DEMO_TOKENS' | 'PASTE'>('DEMO_TOKENS');

  // Scanning State
  const [scannedUri, setScannedUri] = useState('');
  const [isValidating, setIsValidating] = useState(false);
  const [validationResult, setValidationResult] = useState<QrValidationResult | null>(null);
  const [validationError, setValidationError] = useState<string | null>(null);

  // Transfer State
  const [validatedToken, setValidatedToken] = useState<QrPaymentRequest | null>(null);
  const [transferAmount, setTransferAmount] = useState<string>('');
  const [transferNote, setTransferNote] = useState<string>('');
  const [countdownRemaining, setCountdownRemaining] = useState<number>(30);
  const [isProcessing, setIsProcessing] = useState(false);

  // Security Verification Step
  const [requiresBiometric, setRequiresBiometric] = useState(false);
  const [requiresOtp, setRequiresOtp] = useState(false);
  const [isHighValue, setIsHighValue] = useState(false);
  const [biometricPassed, setBiometricPassed] = useState(false);
  const [otpInput, setOtpInput] = useState('');
  const [generatedOtp, setGeneratedOtp] = useState('');
  const [otpError, setOtpError] = useState<string | null>(null);
  const [completedTx, setCompletedTx] = useState<PaymentTransaction | null>(null);

  // Camera video ref
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const animationFrameRef = useRef<number | null>(null);

  // List of active tokens in the system for instant testing
  const [availableTokens, setAvailableTokens] = useState<QrPaymentRequest[]>([]);

  // Load available tokens
  useEffect(() => {
    fetchTokens();
  }, []);

  const fetchTokens = async () => {
    try {
      const res = await fetch('/api/p2p/tokens');
      if (res.ok) {
        const data = await res.json();
        setAvailableTokens(data.tokens || []);
      }
    } catch {
      setAvailableTokens(TransactionStorage.getQrTokens());
    }
  };

  // Camera scanner loop
  useEffect(() => {
    if (activeTab !== 'CAMERA' || step !== 'SCAN') {
      stopCamera();
      return;
    }

    startCamera();

    return () => {
      stopCamera();
    };
  }, [activeTab, step]);

  const startCamera = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment', width: { ideal: 640 }, height: { ideal: 480 } },
      });
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        videoRef.current.setAttribute('playsinline', 'true');
        videoRef.current.play();
        requestAnimationFrame(tick);
      }
    } catch (err) {
      console.warn('Camera access not granted or unavailable in preview container:', err);
    }
  };

  const stopCamera = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }
    if (animationFrameRef.current) {
      cancelAnimationFrame(animationFrameRef.current);
      animationFrameRef.current = null;
    }
  };

  const tick = () => {
    if (videoRef.current && videoRef.current.readyState === videoRef.current.HAVE_ENOUGH_DATA) {
      const video = videoRef.current;
      const canvas = document.createElement('canvas');
      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;
      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        const code = jsQR(imageData.data, imageData.width, imageData.height);
        if (code && code.data) {
          handleValidatePayload(code.data);
          return;
        }
      }
    }
    animationFrameRef.current = requestAnimationFrame(tick);
  };

  // Countdown timer for validated token
  useEffect(() => {
    if (step !== 'CONFIRM' && step !== 'VERIFY') return;
    if (!validatedToken) return;

    const timer = setInterval(() => {
      const diffMs = validatedToken.expiresAt - Date.now();
      const secs = Math.max(0, Math.ceil(diffMs / 1000));
      setCountdownRemaining(secs);

      if (secs <= 0) {
        clearInterval(timer);
        setValidationError('QR expired. Ask the receiver to generate a new QR.');
        setStep('SCAN');
      }
    }, 500);

    return () => clearInterval(timer);
  }, [step, validatedToken]);

  // Main QR validation logic (called on scan or click)
  const handleValidatePayload = async (rawUri: string) => {
    setIsValidating(true);
    setValidationError(null);
    setValidationResult(null);

    // 1. Strict Protocol Check
    if (!rawUri.startsWith('trustpay://p2p/request')) {
      setValidationError('Invalid TrustPay QR code.');
      setIsValidating(false);
      return;
    }

    const parsed = parseQrPayloadUri(rawUri);
    if (!parsed || !parsed.tokenId) {
      setValidationError('Invalid TrustPay QR code.');
      setIsValidating(false);
      return;
    }

    try {
      const res = await fetch('/api/p2p/validate-qr-token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tokenId: parsed.tokenId,
          senderId: currentUser.email,
        }),
      });

      const data = await res.json();

      if (!data.valid) {
        // Record security audit
        if (data.errorCode === 'ALREADY_USED') {
          TransactionStorage.recordQrSecurityEvent('replay_blocked');
          TransactionStorage.appendAuditLog({
            id: `LOG_REPLAY_${Date.now()}`,
            timestamp: Date.now(),
            title: 'QR Replay Attack Blocked',
            details: `Attempted replay of used QR token ${parsed.tokenId.substring(0, 14)}...`,
            status: 'DANGER',
          });
        } else if (data.errorCode === 'TAMPERED') {
          TransactionStorage.recordQrSecurityEvent('tamper_blocked');
          TransactionStorage.appendAuditLog({
            id: `LOG_TAMPER_${Date.now()}`,
            timestamp: Date.now(),
            title: 'QR Tampering Blocked',
            details: `HMAC signature mismatch detected for token ${parsed.tokenId.substring(0, 14)}...`,
            status: 'DANGER',
          });
        } else if (data.errorCode === 'EXPIRED') {
          TransactionStorage.recordQrSecurityEvent('expired');
        }

        setValidationError(data.error || 'Invalid TrustPay QR code.');
        setValidationResult(data);
        return;
      }

      // Valid token!
      const token: QrPaymentRequest = data.request;
      setValidatedToken(token);
      setCountdownRemaining(data.remainingSeconds || 30);
      setTransferAmount(token.amount ? token.amount.toString() : '');
      setTransferNote(token.note || '');
      setStep('CONFIRM');
    } catch (err: any) {
      console.error('Validation fetch error:', err);
      setValidationError('Network error validating QR. Please retry.');
    } finally {
      setIsValidating(false);
    }
  };

  // Move to Security Verification Step
  const handleProceedToVerification = () => {
    const amt = parseFloat(transferAmount);
    if (isNaN(amt) || amt <= 0) {
      setValidationError('Please enter a valid transfer amount.');
      return;
    }

    if (amt > currentBalance) {
      setValidationError(`Insufficient balance. Current balance is ${formatIndianCurrency(currentBalance)}.`);
      return;
    }

    // Evaluate Risk
    const isOver40k = amt > 40000;
    setIsHighValue(isOver40k);
    setRequiresBiometric(isOver40k);
    setRequiresOtp(isOver40k);

    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    setGeneratedOtp(otp);

    setStep('VERIFY');
  };

  // Complete Atomic P2P Transfer
  const handleExecutePayment = async () => {
    if (!validatedToken) return;
    const amt = parseFloat(transferAmount);

    if (requiresOtp && otpInput.trim() !== generatedOtp) {
      setOtpError('Invalid Demo OTP. Please enter the designated code.');
      return;
    }

    if (requiresBiometric && !biometricPassed) {
      setValidationError('Biometric authorization required to proceed.');
      return;
    }

    setIsProcessing(true);
    setValidationError(null);

    try {
      const bindingHash = sha256Sync(
        `${validatedToken.tokenId}:${currentUser.email}:${validatedToken.receiverId}:${amt}:${Date.now()}`
      );

      const res = await fetch('/api/p2p/complete-transfer', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tokenId: validatedToken.tokenId,
          senderId: currentUser.email,
          senderName: currentUser.fullName,
          amount: amt,
          bindingHash,
          riskScore: isHighValue ? 65 : 15,
        }),
      });

      const data = await res.json();

      if (!res.ok || !data.success) {
        setValidationError(data.error || 'Payment execution failed.');
        setIsProcessing(false);
        return;
      }

      // Successful Transfer Record
      const newTx: PaymentTransaction = {
        id: data.transactionId,
        recipient: `${validatedToken.receiverName} (${validatedToken.receiverId})`,
        amount: amt,
        currency: 'INR',
        timestamp: Date.now(),
        status: 'APPROVED',
        bindingHash,
        riskScore: isHighValue ? 65 : 15,
        balanceAfter: data.senderBalance,
      };

      // Save locally
      TransactionStorage.appendTransaction(newTx);
      TransactionStorage.saveUserBalance(data.senderBalance, currentUser.email);
      TransactionStorage.saveUserBalance(data.receiverBalance, validatedToken.receiverId);
      TransactionStorage.updateQrToken(validatedToken.tokenId, {
        status: 'USED',
        usedBySenderId: currentUser.email,
        usedBySenderName: currentUser.fullName,
        usedAt: Date.now(),
        transactionId: data.transactionId,
      });

      TransactionStorage.appendAuditLog({
        id: `LOG_P2P_${Date.now()}`,
        timestamp: Date.now(),
        title: 'P2P Transfer Executed Successfully',
        details: `₹${amt.toFixed(2)} transferred to ${validatedToken.receiverName}. QR token ${validatedToken.tokenId.substring(0, 14)}... marked USED.`,
        status: 'SUCCESS',
        hash: bindingHash,
      });

      setCompletedTx(newTx);
      setStep('SUCCESS');
      onPaymentCompleted(newTx, data.senderBalance);
    } catch (err: any) {
      console.error('Error executing transfer:', err);
      setValidationError(err.message || 'Payment failed. Please retry.');
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/75 backdrop-blur-sm animate-in fade-in duration-200">
      <div
        className={`w-full max-w-lg rounded-2xl border shadow-2xl overflow-hidden flex flex-col max-h-[92vh] ${
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
              <ScanLine className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-base font-bold leading-tight">Scan & Pay</h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                P2P QR Scanner & Cryptographic Verification
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-5 overflow-y-auto space-y-4">
          {/* STEP 1: SCANNING */}
          {step === 'SCAN' && (
            <div className="space-y-4">
              {/* Tab Selector */}
              <div className="flex rounded-xl bg-slate-100 dark:bg-slate-800/60 p-1 border border-slate-200 dark:border-slate-700/60 text-xs font-semibold">
                <button
                  onClick={() => setActiveTab('DEMO_TOKENS')}
                  className={`flex-1 py-1.5 rounded-lg transition ${
                    activeTab === 'DEMO_TOKENS'
                      ? 'bg-white dark:bg-slate-700 text-cyan-600 dark:text-cyan-400 shadow-sm'
                      : 'text-slate-500 hover:text-slate-700 dark:hover:text-slate-300'
                  }`}
                >
                  Active System QRs
                </button>
                <button
                  onClick={() => setActiveTab('CAMERA')}
                  className={`flex-1 py-1.5 rounded-lg transition ${
                    activeTab === 'CAMERA'
                      ? 'bg-white dark:bg-slate-700 text-cyan-600 dark:text-cyan-400 shadow-sm'
                      : 'text-slate-500 hover:text-slate-700 dark:hover:text-slate-300'
                  }`}
                >
                  Camera Scanner
                </button>
                <button
                  onClick={() => setActiveTab('PASTE')}
                  className={`flex-1 py-1.5 rounded-lg transition ${
                    activeTab === 'PASTE'
                      ? 'bg-white dark:bg-slate-700 text-cyan-600 dark:text-cyan-400 shadow-sm'
                      : 'text-slate-500 hover:text-slate-700 dark:hover:text-slate-300'
                  }`}
                >
                  Paste QR URI
                </button>
              </div>

              {/* Error Message Box */}
              {validationError && (
                <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-600 dark:text-rose-400 text-xs flex items-start gap-2 animate-shake">
                  <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                  <div className="leading-relaxed">
                    <span className="font-bold">Validation Blocked: </span>
                    {validationError}
                  </div>
                </div>
              )}

              {/* TAB 1: ACTIVE SYSTEM QRs */}
              {activeTab === 'DEMO_TOKENS' && (
                <div className="space-y-3">
                  <div className="text-xs text-slate-500 dark:text-slate-400 leading-relaxed">
                    Click any generated QR payment request below to simulate instant scanner input. Tokens from
                    another user (or generated in "Receive Money") can be scanned here:
                  </div>

                  <div className="space-y-2 max-h-52 overflow-y-auto pr-1">
                    {availableTokens.length === 0 ? (
                      <div className="p-4 rounded-xl border border-dashed text-center text-xs text-slate-400">
                        No active QR tokens yet. Click "Receive Money - Generate QR" on the dashboard to create
                        one, or use the Attack Simulator below!
                      </div>
                    ) : (
                      availableTokens.map((tok) => {
                        const isOwn = tok.receiverId === currentUser.email;
                        const isExpired = Date.now() > tok.expiresAt;
                        const isUsed = tok.status === 'USED';

                        return (
                          <div
                            key={tok.tokenId}
                            onClick={() => handleValidatePayload(tok.payloadUri)}
                            className={`p-3 rounded-xl border transition-all cursor-pointer flex items-center justify-between ${
                              isDarkTheme
                                ? 'bg-slate-800/40 hover:bg-slate-800 border-slate-700/80'
                                : 'bg-slate-50 hover:bg-slate-100 border-slate-200'
                            }`}
                          >
                            <div className="flex items-center gap-2.5">
                              <div className="w-8 h-8 rounded-lg bg-cyan-500/10 text-cyan-600 dark:text-cyan-400 flex items-center justify-center font-bold text-xs">
                                ₹
                              </div>
                              <div>
                                <div className="text-xs font-bold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                                  <span>{tok.receiverName}</span>
                                  {isOwn && (
                                    <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-200 dark:bg-slate-700 text-slate-500">
                                      Your QR
                                    </span>
                                  )}
                                </div>
                                <div className="text-[11px] text-slate-500 font-mono">
                                  {tok.amount ? formatIndianCurrency(tok.amount) : 'Open Amount'} •{' '}
                                  {tok.note || 'No note'}
                                </div>
                              </div>
                            </div>

                            <div className="text-right">
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
                          </div>
                        );
                      })
                    )}
                  </div>

                  {/* Security Edge-Case Attack Simulator */}
                  <div className="pt-2 border-t border-slate-200 dark:border-slate-800">
                    <div className="text-[11px] font-bold uppercase tracking-wider text-slate-400 mb-2 flex items-center gap-1">
                      <ShieldAlert className="w-3.5 h-3.5 text-amber-500" />
                      Simulate Security Attack Cases
                    </div>
                    <div className="grid grid-cols-3 gap-2">
                      <button
                        onClick={() =>
                          handleValidatePayload(
                            'trustpay://p2p/request?tokenId=tp_qr_fake_expired&expiresAt=1600000000000&version=1'
                          )
                        }
                        className="py-1.5 px-2 rounded-lg text-[10px] font-bold border border-amber-500/30 bg-amber-500/10 text-amber-600 dark:text-amber-400 hover:bg-amber-500/20 transition"
                      >
                        Expired QR Test
                      </button>
                      <button
                        onClick={() =>
                          handleValidatePayload('trustpay://p2p/request?tokenId=tp_qr_tampered_attack&expiresAt=9999999999999&version=1')
                        }
                        className="py-1.5 px-2 rounded-lg text-[10px] font-bold border border-rose-500/30 bg-rose-500/10 text-rose-600 dark:text-rose-400 hover:bg-rose-500/20 transition"
                      >
                        Tampered QR Test
                      </button>
                      <button
                        onClick={() => handleValidatePayload('https://malicious-scam.com/qr?pay=99999')}
                        className="py-1.5 px-2 rounded-lg text-[10px] font-bold border border-purple-500/30 bg-purple-500/10 text-purple-600 dark:text-purple-400 hover:bg-purple-500/20 transition"
                      >
                        Invalid Protocol
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {/* TAB 2: LIVE CAMERA SCANNER */}
              {activeTab === 'CAMERA' && (
                <div className="space-y-3 flex flex-col items-center">
                  <div className="w-full max-w-[280px] h-[240px] bg-slate-950 rounded-2xl overflow-hidden relative flex items-center justify-center border-2 border-cyan-500/40 shadow-inner">
                    <video ref={videoRef} className="w-full h-full object-cover" />
                    {/* Scanner Reticle Overlay */}
                    <div className="absolute inset-8 border-2 border-dashed border-cyan-400 rounded-xl pointer-events-none animate-pulse" />
                    <div className="absolute bottom-2 px-2.5 py-1 rounded bg-black/60 text-[10px] text-white backdrop-blur-sm">
                      Align TrustPay QR code in frame
                    </div>
                  </div>
                  <p className="text-xs text-slate-500 text-center">
                    Uses CameraX / ML Kit compatible barcode reader. If camera is blocked in your iframe, use
                    "Active System QRs" tab above!
                  </p>
                </div>
              )}

              {/* TAB 3: PASTE URI */}
              {activeTab === 'PASTE' && (
                <div className="space-y-3">
                  <label className="block text-xs font-semibold text-slate-500 dark:text-slate-400">
                    Paste Scanned QR Payload URI:
                  </label>
                  <textarea
                    value={scannedUri}
                    onChange={(e) => setScannedUri(e.target.value)}
                    placeholder="trustpay://p2p/request?tokenId=tp_qr_...&expiresAt=...&version=1"
                    rows={3}
                    className={`w-full p-2.5 text-xs rounded-xl border outline-none font-mono ${
                      isDarkTheme
                        ? 'bg-slate-800 border-slate-700 text-white focus:border-cyan-500'
                        : 'bg-slate-50 border-slate-300 text-slate-900 focus:border-cyan-600'
                    }`}
                  />
                  <button
                    onClick={() => handleValidatePayload(scannedUri.trim())}
                    disabled={!scannedUri.trim() || isValidating}
                    className="w-full py-2.5 text-xs font-bold rounded-xl bg-cyan-600 hover:bg-cyan-700 text-white flex items-center justify-center gap-1.5 transition disabled:opacity-50"
                  >
                    <ShieldCheck className="w-4 h-4" />
                    Validate Scanned Token
                  </button>
                </div>
              )}
            </div>
          )}

          {/* STEP 2: CONFIRM & ENTER AMOUNT */}
          {step === 'CONFIRM' && validatedToken && (
            <div className="space-y-4">
              {/* Receiver Card */}
              <div
                className={`p-4 rounded-xl border flex items-center justify-between ${
                  isDarkTheme ? 'bg-slate-800/60 border-slate-700' : 'bg-slate-50 border-slate-200'
                }`}
              >
                <div>
                  <div className="text-[11px] font-bold text-cyan-600 dark:text-cyan-400 uppercase tracking-wider">
                    Verified Payee
                  </div>
                  <div className="text-base font-extrabold text-slate-800 dark:text-slate-100">
                    {validatedToken.receiverName}
                  </div>
                  <div className="text-xs text-slate-500 font-mono">{validatedToken.receiverId}</div>
                </div>

                {/* Remaining validity countdown */}
                <div className="text-right">
                  <div className="text-[10px] font-semibold text-slate-500 uppercase">QR TTL</div>
                  <div className="text-sm font-bold font-mono text-amber-500 flex items-center gap-1">
                    <Clock className="w-3.5 h-3.5 animate-pulse" />
                    {countdownRemaining}s left
                  </div>
                </div>
              </div>

              {/* Note Display if provided */}
              {validatedToken.note && (
                <div className="p-2.5 rounded-lg bg-cyan-500/10 border border-cyan-500/20 text-xs text-cyan-700 dark:text-cyan-300">
                  <span className="font-semibold">Note:</span> “{validatedToken.note}”
                </div>
              )}

              {/* Transfer Amount Input */}
              <div className="space-y-1.5">
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
                  Transfer Amount (INR)
                </label>
                <div className="relative">
                  <span className="absolute left-3 top-3 text-lg font-bold text-slate-400">₹</span>
                  <input
                    type="number"
                    value={transferAmount}
                    onChange={(e) => setTransferAmount(e.target.value)}
                    disabled={validatedToken.amount !== null}
                    placeholder="Enter amount (e.g. 5000)"
                    className={`w-full pl-8 pr-4 py-2.5 text-base font-extrabold rounded-xl border outline-none font-mono ${
                      isDarkTheme
                        ? 'bg-slate-800 border-slate-700 text-white focus:border-cyan-500'
                        : 'bg-white border-slate-300 text-slate-900 focus:border-cyan-600'
                    } ${validatedToken.amount !== null ? 'bg-slate-100 dark:bg-slate-800/80 cursor-not-allowed' : ''}`}
                  />
                </div>
                <div className="flex justify-between text-[11px] text-slate-500 pt-0.5">
                  <span>Available Balance: {formatIndianCurrency(currentBalance)}</span>
                  {parseFloat(transferAmount) > 40000 && (
                    <span className="text-amber-500 font-bold flex items-center gap-1">
                      <ShieldAlert className="w-3 h-3" /> High-Value Protocol Applies
                    </span>
                  )}
                </div>
              </div>

              {/* Error Box */}
              {validationError && (
                <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-600 dark:text-rose-400 text-xs">
                  {validationError}
                </div>
              )}

              {/* Action Buttons */}
              <div className="flex gap-2 pt-2">
                <button
                  onClick={() => setStep('SCAN')}
                  className="flex-1 py-2.5 text-xs font-bold rounded-xl border border-slate-300 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
                >
                  Cancel
                </button>
                <button
                  onClick={handleProceedToVerification}
                  className="flex-2 py-2.5 text-xs font-bold rounded-xl bg-cyan-600 hover:bg-cyan-700 text-white flex items-center justify-center gap-1.5 transition shadow-sm"
                >
                  Authorize Payment <ArrowRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}

          {/* STEP 3: HIGH-VALUE VERIFICATION (BIOMETRIC + OTP) */}
          {step === 'VERIFY' && (
            <div className="space-y-4">
              <div className="text-center space-y-1">
                <div className="w-12 h-12 rounded-full bg-cyan-500/10 text-cyan-600 dark:text-cyan-400 flex items-center justify-center mx-auto mb-2 border border-cyan-500/30">
                  <Lock className="w-6 h-6" />
                </div>
                <h3 className="text-base font-extrabold text-slate-800 dark:text-slate-100">
                  Transaction Security Verification
                </h3>
                <p className="text-xs text-slate-500">
                  Cryptographically authenticating ₹{parseFloat(transferAmount).toLocaleString('en-IN')} transfer to{' '}
                  {validatedToken?.receiverName}.
                </p>
              </div>

              {/* Biometric Challenge */}
              <div
                className={`p-3.5 rounded-xl border space-y-2 ${
                  isDarkTheme ? 'bg-slate-800/40 border-slate-700' : 'bg-slate-50 border-slate-200'
                }`}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Fingerprint className="w-4 h-4 text-cyan-500" />
                    <span className="text-xs font-bold text-slate-700 dark:text-slate-300">
                      Step 1: Biometric Authentication
                    </span>
                  </div>
                  {biometricPassed ? (
                    <span className="text-[10px] font-bold text-emerald-500 flex items-center gap-1">
                      <CheckCircle2 className="w-3.5 h-3.5" /> Passed
                    </span>
                  ) : (
                    <button
                      onClick={() => setBiometricPassed(true)}
                      className="px-2.5 py-1 text-[11px] font-bold rounded-lg bg-cyan-600 text-white hover:bg-cyan-700 transition"
                    >
                      Simulate Pass
                    </button>
                  )}
                </div>
                <p className="text-[11px] text-slate-500">
                  Simulates Android BiometricPrompt / fingerprint sensor matching.
                </p>
              </div>

              {/* OTP Challenge */}
              <div
                className={`p-3.5 rounded-xl border space-y-2 ${
                  isDarkTheme ? 'bg-slate-800/40 border-slate-700' : 'bg-slate-50 border-slate-200'
                }`}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <KeyRound className="w-4 h-4 text-cyan-500" />
                    <span className="text-xs font-bold text-slate-700 dark:text-slate-300">
                      Step 2: Designated Security OTP
                    </span>
                  </div>
                  <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded bg-cyan-500/10 text-cyan-600 dark:text-cyan-400">
                    Code: {generatedOtp}
                  </span>
                </div>

                <input
                  type="text"
                  maxLength={6}
                  value={otpInput}
                  onChange={(e) => {
                    setOtpInput(e.target.value);
                    setOtpError(null);
                  }}
                  placeholder="Enter 6-digit OTP above"
                  className={`w-full px-3 py-2 text-sm text-center tracking-widest font-mono font-bold rounded-lg border outline-none ${
                    isDarkTheme
                      ? 'bg-slate-900 border-slate-700 text-white focus:border-cyan-500'
                      : 'bg-white border-slate-300 text-slate-900 focus:border-cyan-600'
                  }`}
                />
                {otpError && <div className="text-[11px] text-rose-500 font-medium">{otpError}</div>}
              </div>

              {/* Execution Error */}
              {validationError && (
                <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-600 text-xs">
                  {validationError}
                </div>
              )}

              {/* Confirm Execution Button */}
              <div className="flex gap-2 pt-2">
                <button
                  onClick={() => setStep('CONFIRM')}
                  className="flex-1 py-2.5 text-xs font-bold rounded-xl border border-slate-300 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
                >
                  Back
                </button>
                <button
                  onClick={handleExecutePayment}
                  disabled={isProcessing || (requiresBiometric && !biometricPassed)}
                  className="flex-2 py-2.5 text-xs font-bold rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white flex items-center justify-center gap-1.5 transition disabled:opacity-50 shadow-md"
                >
                  {isProcessing ? (
                    <>
                      <RefreshCw className="w-4 h-4 animate-spin" />
                      Executing Atomic Transfer...
                    </>
                  ) : (
                    <>
                      <ShieldCheck className="w-4 h-4" />
                      Confirm & Transfer Now
                    </>
                  )}
                </button>
              </div>
            </div>
          )}

          {/* STEP 4: SUCCESS RECEIPT */}
          {step === 'SUCCESS' && completedTx && (
            <div className="space-y-4 text-center py-2">
              <div className="w-16 h-16 rounded-full bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 flex items-center justify-center mx-auto border-2 border-emerald-500/30">
                <CheckCircle2 className="w-8 h-8 animate-in zoom-in" />
              </div>

              <div>
                <h3 className="text-lg font-black text-slate-800 dark:text-slate-100">
                  Payment Completed!
                </h3>
                <p className="text-xs text-slate-500">Atomic P2P Transfer & Cryptographic Seal Cleared</p>
              </div>

              <div className="text-2xl font-black text-cyan-600 dark:text-cyan-400 font-mono">
                {formatIndianCurrency(completedTx.amount)}
              </div>

              {/* Receipt Details Box */}
              <div
                className={`p-3.5 rounded-xl border text-xs text-left space-y-2 font-mono ${
                  isDarkTheme ? 'bg-slate-800/50 border-slate-700 text-slate-300' : 'bg-slate-50 border-slate-200 text-slate-700'
                }`}
              >
                <div className="flex justify-between">
                  <span className="text-slate-500">Recipient:</span>
                  <span className="font-bold">{completedTx.recipient}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Tx Reference:</span>
                  <span className="font-bold text-cyan-600 dark:text-cyan-400">{completedTx.id}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">QR Token Status:</span>
                  <span className="text-purple-500 font-bold">USED (One-Time Redeemed)</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">New Sender Balance:</span>
                  <span className="font-bold text-emerald-500">
                    {formatIndianCurrency(completedTx.balanceAfter || 0)}
                  </span>
                </div>
                <div className="pt-1 border-t border-slate-200 dark:border-slate-700/60">
                  <span className="text-[10px] text-slate-500 block">SHA-256 Payload Binding Digest:</span>
                  <span className="text-[10px] text-slate-400 truncate block">
                    {completedTx.bindingHash}
                  </span>
                </div>
              </div>

              <button
                onClick={onClose}
                className="w-full py-2.5 text-xs font-bold rounded-xl bg-cyan-600 hover:bg-cyan-700 text-white transition shadow-sm"
              >
                Done
              </button>
            </div>
          )}

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
