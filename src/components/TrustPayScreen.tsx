import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  AuditLogEntry,
  ChallengeData,
  PaymentTransaction,
  RiskAnalysisResult,
  TrustPayUiState,
  UserAccount,
  VerificationState,
  FinalDecisionStatus,
} from '../types';
import {
  computeBindingHash,
  createSnapshot,
  generateDemoOtp,
  generateTransactionId,
  verifyTransactionIntegrity,
  formatIndianCurrency,
} from '../security/crypto';
import { TransactionStorage, DEFAULT_DEMO_BALANCE } from '../storage/transactionStorage';
import { HeaderBanner } from './HeaderBanner';
import { AccountBalanceCard } from './AccountBalanceCard';
import { PaymentInputCard } from './PaymentInputCard';
import { RiskAssessmentCard } from './RiskAssessmentCard';
import { VelocityMonitorCard, VELOCITY_AMOUNT_THRESHOLD, VELOCITY_COUNT_THRESHOLD } from './VelocityMonitorCard';
import { SuccessfulTransactionHistoryCard } from './SuccessfulTransactionHistoryCard';
import { AuditLogSection } from './AuditLogSection';
import { VerificationFlowModal } from './VerificationFlowModal';
import { BiometricSimulationModal } from './BiometricSimulationModal';
import { DemoScenarioGuideModal } from './DemoScenarioGuideModal';
import { ResetBalanceModal } from './ResetBalanceModal';
import { ReceiveMoneyQrModal } from './ReceiveMoneyQrModal';
import { ScanAndPayModal } from './ScanAndPayModal';
import { QrHistoryModal } from './QrHistoryModal';
import { AdminPanelModal } from './AdminPanelModal';
import { DualAuthPanelModal } from './DualAuthPanelModal';
import { LocationDistanceClassifierView } from './distance/LocationDistanceClassifierView';
import {
  QrCode,
  ScanLine,
  History,
  ShieldCheck,
  ArrowDownLeft,
  ShieldAlert,
  KeyRound,
  Compass,
  MapPin,
} from 'lucide-react';

interface TrustPayScreenProps {
  currentUser: UserAccount | null;
  isDarkTheme: boolean;
  onToggleTheme: () => void;
  onLogout: () => void;
}

const DESIGNATED_FINGERS = [
  'Right Index Finger',
  'Left Thumb',
  'Right Thumb',
  'Left Index Finger',
  'Right Middle Finger',
];

export const TrustPayScreen: React.FC<TrustPayScreenProps> = ({
  currentUser,
  isDarkTheme,
  onToggleTheme,
  onLogout,
}) => {
  // Top-level Navigation Tab
  const [activeTab, setActiveTab] = useState<'DASHBOARD' | 'LOCATION_CLASSIFIER'>('DASHBOARD');

  // Primary State
  const [balance, setBalance] = useState<number>(() => TransactionStorage.getBalance());
  const [transactions, setTransactions] = useState<PaymentTransaction[]>(() =>
    TransactionStorage.getTransactions()
  );
  const [auditLogs, setAuditLogs] = useState<AuditLogEntry[]>(() =>
    TransactionStorage.getAuditLogs()
  );

  // Form Inputs
  const [recipientInput, setRecipientInput] = useState<string>('rahul@okhdfcbank');
  const [amountInput, setAmountInput] = useState<string>('5000');

  // Risk Signal Toggles
  const [untrustedDevice, setUntrustedDevice] = useState<boolean>(false);
  const [newRecipient, setNewRecipient] = useState<boolean>(false);
  const [unusualContext, setUnusualContext] = useState<boolean>(false);

  // Analysis and Decision
  const [currentRiskScore, setCurrentRiskScore] = useState<number | null>(null);
  const [finalDecision, setFinalDecision] = useState<FinalDecisionStatus>('NONE');
  const [decisionMessage, setDecisionMessage] = useState<string>('');
  const [signals, setSignals] = useState<string[]>([]);
  const [currentAnalysis, setCurrentAnalysis] = useState<RiskAnalysisResult | null>(null);

  // Modals & Flows
  const [verificationState, setVerificationState] = useState<VerificationState | null>(null);
  const [showBiometricModal, setShowBiometricModal] = useState<boolean>(false);
  const [pendingSnapshot, setPendingSnapshot] = useState<any | null>(null);
  const [showGuideModal, setShowGuideModal] = useState<boolean>(false);
  const [showResetBalanceModal, setShowResetBalanceModal] = useState<boolean>(false);
  const [showReceiveQrModal, setShowReceiveQrModal] = useState<boolean>(false);
  const [showScanPayModal, setShowScanPayModal] = useState<boolean>(false);
  const [showQrHistoryModal, setShowQrHistoryModal] = useState<boolean>(false);
  const [showAdminModal, setShowAdminModal] = useState<boolean>(false);
  const [showDualAuthModal, setShowDualAuthModal] = useState<boolean>(false);
  const [activeUserLocal, setActiveUserLocal] = useState<UserAccount | null>(currentUser);

  // Dynamic count of pending dual authorizations
  const pendingDualAuthCount = useMemo(() => {
    try {
      const txs = TransactionStorage.getDualAuthTransactions();
      const now = Date.now();
      return txs.filter((t) => t.status === 'PENDING SECOND AUTHORIZATION' && now <= t.expiresAt).length;
    } catch {
      return 0;
    }
  }, [showDualAuthModal, balance]);

  // Sync balance when active user changes
  const handleSwitchUser = (newUser: UserAccount) => {
    setActiveUserLocal(newUser);
    const newBal = TransactionStorage.getUserBalance(newUser.email);
    setBalance(newBal);
    addAuditLog(
      'Switched Active User',
      `Switched session to ${newUser.fullName} (${newUser.email}).`,
      'INFO'
    );
  };

  // Calculate 5-minute rolling window transactions
  const rollingWindowTransactions = useMemo(() => {
    const fiveMinutesAgo = Date.now() - 5 * 60 * 1000;
    return transactions.filter(
      (tx) =>
        (tx.status === 'APPROVED' || tx.status.toLowerCase() === 'successful') &&
        tx.timestamp >= fiveMinutesAgo
    );
  }, [transactions]);

  const rollingWindowTotalAmount = useMemo(() => {
    return rollingWindowTransactions.reduce((acc, tx) => acc + tx.amount, 0);
  }, [rollingWindowTransactions]);

  // Append Audit Log Helper
  const addAuditLog = useCallback(
    (title: string, details: string, status: 'SUCCESS' | 'WARNING' | 'DANGER' | 'INFO', hash?: string) => {
      const entry: AuditLogEntry = {
        id: `LOG_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
        timestamp: Date.now(),
        title,
        details,
        status,
        hash,
      };
      TransactionStorage.appendAuditLog(entry);
      setAuditLogs((prev) => [entry, ...prev]);
    },
    []
  );

  // Request server-side security risk insights via Express API route if available
  const fetchSecurityRiskInsights = async (
    txId: string,
    recipient: string,
    amount: number,
    score: number,
    activeSignals: string[],
    isAnomaly: boolean
  ) => {
    try {
      const res = await fetch('/api/risk-insights', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          transactionId: txId,
          recipient,
          amount,
          score,
          signals: activeSignals,
          isAnomaly,
        }),
      });
      if (res.ok) {
        const data = await res.json();
        if (data.insights) {
          setCurrentAnalysis((prev) => (prev ? { ...prev, aiInsights: data.insights } : null));
        }
      }
    } catch {
      // Graceful fallback
    }
  };

  // Complete Payment helper
  const completePayment = (
    txId: string,
    recipient: string,
    amount: number,
    boundHash: string,
    riskScore: number
  ) => {
    const newBalance = balance - amount;
    TransactionStorage.saveBalance(newBalance);
    setBalance(newBalance);

    const tx: PaymentTransaction = {
      id: txId,
      recipient,
      amount,
      currency: 'INR',
      timestamp: Date.now(),
      status: 'APPROVED',
      bindingHash: boundHash,
      riskScore,
      balanceAfter: newBalance,
    };

    TransactionStorage.appendTransaction(tx);
    setTransactions((prev) => [tx, ...prev]);

    setFinalDecision('APPROVED');
    setDecisionMessage(
      `Transaction approved. Payment of ${formatIndianCurrency(amount)} released. Payload sealed with SHA-256.`
    );

    addAuditLog(
      'Transaction Approved',
      `Payment of ${formatIndianCurrency(amount)} to ${recipient} approved. Balance remaining: ${formatIndianCurrency(newBalance)}.`,
      'SUCCESS',
      boundHash
    );
  };

  // Primary: Analyse and Pay Action
  const handleAnalyseAndPay = () => {
    const recipient = recipientInput.trim();
    const amount = parseFloat(amountInput);

    if (!recipient) {
      setFinalDecision('BLOCKED');
      setDecisionMessage('Invalid payment request: Recipient UPI ID cannot be empty.');
      return;
    }

    if (isNaN(amount) || amount <= 0) {
      setFinalDecision('BLOCKED');
      setDecisionMessage('Invalid payment request: Amount must be greater than zero.');
      return;
    }

    // Check account balance
    if (amount > balance) {
      setFinalDecision('BLOCKED');
      setDecisionMessage(
        `Insufficient funds: Requested ${formatIndianCurrency(amount)} exceeds available balance ${formatIndianCurrency(balance)}.`
      );
      addAuditLog(
        'Transaction Rejected',
        `Insufficient funds for payment of ${formatIndianCurrency(amount)} to ${recipient}. Available: ${formatIndianCurrency(balance)}.`,
        'WARNING'
      );
      return;
    }

    const txId = generateTransactionId();
    const bindingHash = computeBindingHash(txId, recipient, amount, 'INR');
    const snapshot = createSnapshot(txId, recipient, amount, 'INR');

    // Risk Scoring Engine
    let score = 10;
    const detectedSignals: string[] = [];

    if (amount > 25000) {
      score += 30;
      detectedSignals.push(`High transaction value (> ₹25,000) [+30]`);
    } else if (amount > 10000) {
      score += 15;
      detectedSignals.push(`Elevated transaction value (> ₹10,000) [+15]`);
    }

    if (untrustedDevice) {
      score += 25;
      detectedSignals.push(`New / Untrusted hardware fingerprint [+25]`);
    }
    if (newRecipient) {
      score += 20;
      detectedSignals.push(`First-time unverified beneficiary [+20]`);
    }
    if (unusualContext) {
      score += 20;
      detectedSignals.push(`Unusual geo-temporal transaction context [+20]`);
    }

    // Check Velocity Anomaly
    const projectedCount = rollingWindowTransactions.length + 1;
    const projectedAmount = rollingWindowTotalAmount + amount;
    const isVelocityAnomaly =
      projectedCount >= VELOCITY_COUNT_THRESHOLD &&
      projectedAmount >= VELOCITY_AMOUNT_THRESHOLD;

    if (isVelocityAnomaly) {
      score += 40;
      detectedSignals.push(
        `Velocity Anomaly: ≥3 transactions & ≥₹15,000 in rolling 5-min window [+40]`
      );
    }

    score = Math.min(99, score);
    const requiresBiometric = score >= 70 || amount > 25000;
    const requiresOtp = isVelocityAnomaly;

    const localSecurityInsights = `TrustPay Security Engine [${score >= 70 || isVelocityAnomaly ? 'CRITICAL' : score >= 40 ? 'MODERATE' : 'LOW'} RISK · ${score}/100]: ${detectedSignals.join('; ') || 'Standard behavioral baseline'}. ${isVelocityAnomaly ? 'Rolling 5-min velocity limit reached. Multi-factor authentication (Biometric + Intent Challenge + OTP) enforced.' : requiresBiometric ? 'Biometric fingerprint authorization mandated prior to funds settlement.' : 'Sealed with SHA-256 integrity binding. Instant approval enabled.'}`;

    const analysis: RiskAnalysisResult = {
      score,
      signals: detectedSignals,
      requiresBiometric,
      requiresOtp,
      isVelocityAnomaly,
      boundHash: bindingHash,
      aiInsights: localSecurityInsights,
    };

    setCurrentAnalysis(analysis);
    setCurrentRiskScore(score);
    setSignals(detectedSignals);
    setPendingSnapshot(snapshot);

    // Call server for background simulated security insights
    fetchSecurityRiskInsights(txId, recipient, amount, score, detectedSignals, isVelocityAnomaly);

    // Flow routing
    if (isVelocityAnomaly) {
      // 3-Step Verification Protocol
      const randomFinger =
        DESIGNATED_FINGERS[Math.floor(Math.random() * DESIGNATED_FINGERS.length)];
      const testOtp = generateDemoOtp();

      // Create Detail Challenge: either confirm exact amount or domain
      const challenge: ChallengeData = {
        type: 'AMOUNT',
        question: `Cognitive Intent Challenge: Enter the EXACT amount in INR being authorized for this transfer:`,
        expectedAnswer: amount.toString(),
      };

      setVerificationState({
        transactionId: txId,
        recipient,
        amount,
        currentStep: 'BIOMETRIC',
        isVelocityAnomaly: true,
        designatedFinger: randomFinger,
        challengeData: challenge,
        challengeInput: '',
        challengeError: null,
        demoOtp: testOtp,
        otpInput: '',
        otpError: null,
      });

      setFinalDecision('NONE');
      setDecisionMessage('Velocity anomaly detected! 3-step authorization required.');
      addAuditLog(
        'Velocity Anomaly Detected',
        `Payment of ${formatIndianCurrency(amount)} triggered 5-min velocity threshold (${projectedCount} txns, ${formatIndianCurrency(projectedAmount)}). Initiating 3-step challenge.`,
        'WARNING',
        bindingHash
      );
    } else if (requiresBiometric) {
      setShowBiometricModal(true);
      setFinalDecision('NONE');
      setDecisionMessage('High-risk transaction detected. Biometric authentication required.');
      addAuditLog(
        'Biometric Challenge Triggered',
        `Risk score ${score}/100 requires biometric authorization for ${recipient}.`,
        'INFO',
        bindingHash
      );
    } else {
      // Immediate approval
      const isValid = verifyTransactionIntegrity(snapshot, recipient, amount, 'INR', txId);
      if (isValid) {
        completePayment(txId, recipient, amount, bindingHash, score);
      } else {
        setFinalDecision('TAMPER_DETECTED');
        setDecisionMessage('Cryptographic integrity check failed before release.');
        addAuditLog(
          'Cryptographic Seal Mismatch',
          `Transaction ${txId} payload mismatch during instant release.`,
          'DANGER',
          bindingHash
        );
      }
    }
  };

  // Attack Demo: Tamper Amount
  const handleTamperAmountAttack = () => {
    const recipient = recipientInput.trim() || 'rahul@okhdfcbank';
    const originalAmount = parseFloat(amountInput) || 5000;
    const txId = generateTransactionId();

    // Legitimate user authorized $originalAmount
    const snapshot = createSnapshot(txId, recipient, originalAmount, 'INR');

    // Adversary attempts to tamper amount to ₹99,999.00
    const tamperedAmount = 99999.0;
    const tamperedHash = computeBindingHash(txId, recipient, tamperedAmount, 'INR');

    setCurrentRiskScore(100);
    setSignals([
      'CRITICAL: Payload integrity seal violated',
      `Original authorized amount: ${formatIndianCurrency(originalAmount)}`,
      `Malicious payload attempted: ${formatIndianCurrency(tamperedAmount)}`,
    ]);

    // Perform TrustPay's cryptographic verification
    const isValid = verifyTransactionIntegrity(snapshot, recipient, tamperedAmount, 'INR', txId);

    if (!isValid) {
      setFinalDecision('TAMPER_DETECTED');
      setDecisionMessage(
        'Transaction tampering detected! Amount was altered after authorization. Payment aborted and funds secured.'
      );
      setCurrentAnalysis({
        score: 100,
        signals: [
          'CRITICAL: Amount tampering intercepted',
          'Cryptographic SHA-256 mismatch detected',
        ],
        requiresBiometric: false,
        requiresOtp: false,
        isVelocityAnomaly: false,
        boundHash: tamperedHash,
      });

      addAuditLog(
        'Attack Intercepted: Amount Tampered',
        `Adversary attempted to alter authorized amount ${formatIndianCurrency(originalAmount)} to ${formatIndianCurrency(tamperedAmount)}. Cryptographic mismatch detected.`,
        'DANGER',
        snapshot.bindingHash
      );
    }
  };

  // Attack Demo: Tamper Recipient
  const handleTamperRecipientAttack = () => {
    const originalRecipient = recipientInput.trim() || 'rahul@okhdfcbank';
    const amount = parseFloat(amountInput) || 5000;
    const txId = generateTransactionId();

    // User authorizes original recipient
    const snapshot = createSnapshot(txId, originalRecipient, amount, 'INR');

    // Adversary tampers recipient to attacker UPI
    const tamperedRecipient = 'attacker@fraud';
    const tamperedHash = computeBindingHash(txId, tamperedRecipient, amount, 'INR');

    setCurrentRiskScore(100);
    setSignals([
      'CRITICAL: Recipient payee identity altered',
      `Original recipient: ${originalRecipient}`,
      `Tampered recipient: ${tamperedRecipient}`,
    ]);

    const isValid = verifyTransactionIntegrity(snapshot, tamperedRecipient, amount, 'INR', txId);

    if (!isValid) {
      setFinalDecision('TAMPER_DETECTED');
      setDecisionMessage(
        'Payee tampering detected! Recipient was altered after authorization. Payment aborted and blocked.'
      );
      setCurrentAnalysis({
        score: 100,
        signals: [
          'CRITICAL: Payee substitution attack intercepted',
          'Cryptographic SHA-256 mismatch detected',
        ],
        requiresBiometric: false,
        requiresOtp: false,
        isVelocityAnomaly: false,
        boundHash: tamperedHash,
      });

      addAuditLog(
        'Attack Intercepted: Payee Tampered',
        `Adversary attempted to substitute authorized payee ${originalRecipient} with ${tamperedRecipient}. Cryptographic mismatch detected.`,
        'DANGER',
        snapshot.bindingHash
      );
    }
  };

  // Reset Demo Balance
  const handleConfirmResetBalance = () => {
    const newBal = TransactionStorage.resetDemoBalance();
    setBalance(newBal);
    setShowResetBalanceModal(false);
    addAuditLog(
      'Demo Balance Reset',
      `Demo Sandbox balance restored to default allocation of ${formatIndianCurrency(DEFAULT_DEMO_BALANCE)}.`,
      'INFO'
    );
  };

  // Reset Demo History
  const handleResetHistory = () => {
    TransactionStorage.clearTransactions();
    setTransactions([]);
    setFinalDecision('NONE');
    setDecisionMessage('');
    setCurrentRiskScore(null);
    setSignals([]);
    setCurrentAnalysis(null);
    addAuditLog('History Cleared', 'Demo transactions and rolling window reset to zero.', 'INFO');
  };

  // Standard Biometric Callback
  const handleBiometricModalSuccess = () => {
    setShowBiometricModal(false);
    if (!pendingSnapshot) return;

    const isValid = verifyTransactionIntegrity(
      pendingSnapshot,
      pendingSnapshot.recipient,
      pendingSnapshot.amount,
      pendingSnapshot.currency,
      pendingSnapshot.transactionId
    );

    if (isValid) {
      completePayment(
        pendingSnapshot.transactionId,
        pendingSnapshot.recipient,
        pendingSnapshot.amount,
        pendingSnapshot.bindingHash,
        currentRiskScore ?? 75
      );
    } else {
      setFinalDecision('TAMPER_DETECTED');
      setDecisionMessage('Cryptographic verification failed post-biometric.');
    }
  };

  const handleBiometricModalFailure = () => {
    setShowBiometricModal(false);
    setFinalDecision('BLOCKED');
    setDecisionMessage('Biometric authentication failed. Payment was blocked.');
    addAuditLog('Biometric Verification Failed', 'Biometric mismatch or sensor error. Payment blocked.', 'DANGER');
  };

  const handleBiometricModalCancel = () => {
    setShowBiometricModal(false);
    setFinalDecision('BLOCKED');
    setDecisionMessage('Payment cancelled by user.');
    addAuditLog('Payment Cancelled', 'Biometric challenge cancelled by user.', 'INFO');
  };

  // 3-Step Verification Stepper Handlers
  const handleVerificationBiometric = (success: boolean) => {
    if (!verificationState) return;
    if (success) {
      setVerificationState((prev) => (prev ? { ...prev, currentStep: 'CHALLENGE' } : null));
      addAuditLog(
        'Step 1 Passed',
        `Biometric challenge using designated finger (${verificationState.designatedFinger}) successfully verified.`,
        'SUCCESS'
      );
    } else {
      setVerificationState(null);
      setFinalDecision('BLOCKED');
      setDecisionMessage('Biometric verification failed. Designated finger mismatch. Payment blocked.');
      addAuditLog(
        'Step 1 Failed',
        `Biometric mismatch with designated finger (${verificationState.designatedFinger}). Payment blocked.`,
        'DANGER'
      );
    }
  };

  const handleVerificationChallenge = (answer: string) => {
    if (!verificationState || !verificationState.challengeData) return;
    const cleanAnswer = answer.trim();

    if (cleanAnswer === verificationState.challengeData.expectedAnswer) {
      setVerificationState((prev) => (prev ? { ...prev, currentStep: 'DEMO_OTP', challengeError: null } : null));
      addAuditLog('Step 2 Passed', 'Cognitive detail challenge verified.', 'SUCCESS');
    } else {
      setVerificationState((prev) =>
        prev
          ? {
              ...prev,
              challengeError: `Incorrect answer. Expected: "${verificationState.challengeData?.expectedAnswer}"`,
            }
          : null
      );
    }
  };

  const handleVerificationOtp = (otp: string) => {
    if (!verificationState) return;
    if (otp === verificationState.demoOtp) {
      const txId = verificationState.transactionId;
      const recipient = verificationState.recipient;
      const amount = verificationState.amount;
      const boundHash = pendingSnapshot?.bindingHash || computeBindingHash(txId, recipient, amount, 'INR');

      setVerificationState(null);

      // Verify integrity against snapshot
      if (pendingSnapshot) {
        const isValid = verifyTransactionIntegrity(pendingSnapshot, recipient, amount, 'INR', txId);
        if (!isValid) {
          setFinalDecision('TAMPER_DETECTED');
          setDecisionMessage('Transaction payload was tampered before final OTP release.');
          return;
        }
      }

      completePayment(txId, recipient, amount, boundHash, currentRiskScore ?? 85);
      addAuditLog(
        'Step 3 Passed',
        'Demo Bank OTP verified. 3-step authorization protocol completed successfully.',
        'SUCCESS'
      );
    } else {
      setVerificationState((prev) =>
        prev ? { ...prev, otpError: 'Invalid OTP. Please enter the simulated demo OTP shown above.' } : null
      );
    }
  };

  const handleCancelVerification = () => {
    setVerificationState(null);
    setFinalDecision('BLOCKED');
    setDecisionMessage('3-step verification protocol cancelled by user. Payment blocked.');
    addAuditLog('Verification Aborted', 'Multi-factor verification cancelled. Payment blocked.', 'WARNING');
  };

  // Compile composite UI state for child components
  const compositeUiState: TrustPayUiState = {
    currentScreen: 'DASHBOARD',
    currentUser,
    isDarkTheme,
    accountBalance: balance,
    recipientInput,
    amountInput,
    untrustedDevice,
    newRecipient,
    unusualContext,
    currentAnalysis,
    currentRiskScore,
    finalDecision,
    decisionMessage,
    signals,
    verificationState,
    allTransactions: transactions,
    rollingWindowTransactions,
    rollingWindowTotalAmount,
    auditLogs,
    loginForm: {
      identifier: '',
      password: '',
      isPasswordVisible: false,
      identifierError: null,
      passwordError: null,
      errorMessage: null,
      successMessage: null,
    },
    registerForm: {
      fullName: '',
      email: '',
      mobileNumber: '',
      password: '',
      confirmPassword: '',
      isPasswordVisible: false,
      isConfirmPasswordVisible: false,
      fullNameError: null,
      emailError: null,
      mobileNumberError: null,
      passwordError: null,
      confirmPasswordError: null,
      errorMessage: null,
    },
  };

  return (
    <div
      className={`min-h-screen transition-colors duration-200 ${
        isDarkTheme ? 'bg-slate-950 text-slate-100' : 'bg-slate-100 text-slate-900'
      }`}
    >
      <div className="max-w-7xl mx-auto px-4 py-6 md:py-8 flex flex-col gap-6">
        {/* 1. Header Banner */}
        <HeaderBanner
          isDarkTheme={isDarkTheme}
          currentUser={currentUser}
          onToggleTheme={onToggleTheme}
          onOpenGuide={() => setShowGuideModal(true)}
          onLogout={onLogout}
        />

        {/* Primary View Navigation Switcher */}
        <div className="flex items-center gap-2 p-1.5 rounded-2xl bg-slate-200/80 dark:bg-slate-900/80 border border-slate-300 dark:border-slate-800 shadow-sm">
          <button
            onClick={() => setActiveTab('DASHBOARD')}
            className={`flex-1 py-2.5 px-3 sm:px-4 rounded-xl text-xs sm:text-sm font-extrabold flex items-center justify-center gap-2 transition ${
              activeTab === 'DASHBOARD'
                ? 'bg-white dark:bg-slate-800 text-slate-900 dark:text-white shadow-sm ring-1 ring-slate-200 dark:ring-slate-700'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <ShieldCheck className="w-4 h-4 text-cyan-500 shrink-0" />
            <span>TrustPay Security &amp; Transfers</span>
          </button>

          <button
            onClick={() => setActiveTab('LOCATION_CLASSIFIER')}
            className={`flex-1 py-2.5 px-3 sm:px-4 rounded-xl text-xs sm:text-sm font-extrabold flex items-center justify-center gap-2 transition ${
              activeTab === 'LOCATION_CLASSIFIER'
                ? 'bg-cyan-600 text-white shadow-md'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <Compass className="w-4 h-4 text-cyan-400 shrink-0" />
            <span className="truncate">Location Distance Classifier</span>
            <span className="hidden sm:inline text-[11px] font-mono opacity-80">(0–10, 10–20, 20–30, &gt;30 km)</span>
          </button>
        </div>

        {activeTab === 'LOCATION_CLASSIFIER' ? (
          <LocationDistanceClassifierView
            isDarkTheme={isDarkTheme}
            onBackToDashboard={() => setActiveTab('DASHBOARD')}
          />
        ) : (
          <>
            {/* 2. Prominent Demo Account Balance Card */}
            <AccountBalanceCard
              balance={balance}
              isDarkTheme={isDarkTheme}
              onResetDemoBalance={() => setShowResetBalanceModal(true)}
            />

            {/* 2.5 P2P QR Operations Banner & Action Cards */}
            <div className="flex flex-col gap-3">
              {/* Simulation Disclaimer Banner */}
              <div
                className={`px-4 py-2 rounded-xl border flex items-center justify-between text-xs font-semibold ${
                  isDarkTheme
                    ? 'bg-amber-500/10 border-amber-500/30 text-amber-400'
                    : 'bg-amber-50 border-amber-300 text-amber-800'
                }`}
              >
                <div className="flex items-center gap-2">
                  <ShieldAlert className="w-4 h-4 shrink-0 text-amber-500" />
                  <span>Simulation only - no real money transfer. P2P transfers are cryptographically authenticated in memory and Cloud Firestore.</span>
                </div>
                <div className="flex items-center gap-2 flex-wrap">
                  <button
                    onClick={() => setActiveTab('LOCATION_CLASSIFIER')}
                    className="px-2.5 py-1 rounded-lg bg-cyan-500/15 hover:bg-cyan-500/25 text-cyan-700 dark:text-cyan-300 font-bold transition flex items-center gap-1"
                  >
                    <Compass className="w-3.5 h-3.5" />
                    Distance Classifier
                  </button>
                  <button
                    onClick={() => setShowDualAuthModal(true)}
                    className="px-2.5 py-1 rounded-lg bg-violet-500/15 hover:bg-violet-500/25 text-violet-700 dark:text-violet-300 font-bold transition flex items-center gap-1"
                  >
                    <KeyRound className="w-3.5 h-3.5" />
                    Dual Auth Panel
                    {pendingDualAuthCount > 0 && (
                      <span className="ml-1 px-1.5 py-0.2 rounded-full bg-amber-500 text-slate-950 font-extrabold text-[10px]">
                        {pendingDualAuthCount}
                      </span>
                    )}
                  </button>
                  <button
                    onClick={() => setShowQrHistoryModal(true)}
                    className="px-2.5 py-1 rounded-lg bg-amber-500/15 hover:bg-amber-500/25 text-amber-700 dark:text-amber-300 font-bold transition flex items-center gap-1"
                  >
                    <History className="w-3.5 h-3.5" />
                    QR History
                  </button>
                  <button
                    onClick={() => setShowAdminModal(true)}
                    className="px-2.5 py-1 rounded-lg bg-indigo-500/15 hover:bg-indigo-500/25 text-indigo-700 dark:text-indigo-300 font-bold transition flex items-center gap-1"
                  >
                    <ShieldCheck className="w-3.5 h-3.5" />
                    Admin Panel
                  </button>
                </div>
              </div>

              {/* Dual Authorization, Location Classifier & P2P Cards Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                {/* Card 1: Receive Money - Generate QR */}
                <div
                  className={`p-5 rounded-2xl border shadow-sm transition-all hover:shadow-md flex flex-col justify-between ${
                    isDarkTheme
                      ? 'bg-slate-900/80 border-slate-800 text-slate-100'
                      : 'bg-white border-slate-200 text-slate-800'
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 rounded-xl bg-cyan-500/10 border border-cyan-500/30 flex items-center justify-center text-cyan-600 dark:text-cyan-400">
                        <ArrowDownLeft className="w-6 h-6" />
                      </div>
                      <div>
                        <h3 className="text-base font-extrabold">Receive Money</h3>
                        <p className="text-xs text-slate-500 dark:text-slate-400">
                          Auto-rotates 30s • Anti-Swap
                        </p>
                      </div>
                    </div>
                    <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-cyan-500/10 text-cyan-600 dark:text-cyan-400 border border-cyan-500/20">
                      Secure
                    </span>
                  </div>

                  <p className="text-xs text-slate-600 dark:text-slate-300 mt-3 leading-relaxed">
                    Generate safe, time-bounded QR payment requests with unique backend HMAC signatures. Refreshes every 30 seconds.
                  </p>

                  <button
                    onClick={() => setShowReceiveQrModal(true)}
                    className="mt-4 w-full py-2.5 px-4 rounded-xl font-bold text-xs bg-cyan-600 hover:bg-cyan-700 text-white flex items-center justify-center gap-2 transition shadow-sm"
                  >
                    <QrCode className="w-4 h-4" />
                    Receive Money - QR
                  </button>
                </div>

                {/* Card 2: Scan & Pay */}
                <div
                  className={`p-5 rounded-2xl border shadow-sm transition-all hover:shadow-md flex flex-col justify-between ${
                    isDarkTheme
                      ? 'bg-slate-900/80 border-slate-800 text-slate-100'
                      : 'bg-white border-slate-200 text-slate-800'
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 rounded-xl bg-indigo-500/10 border border-indigo-500/30 flex items-center justify-center text-indigo-600 dark:text-indigo-400">
                        <ScanLine className="w-6 h-6" />
                      </div>
                      <div>
                        <h3 className="text-base font-extrabold">Scan &amp; Pay</h3>
                        <p className="text-xs text-slate-500 dark:text-slate-400">
                          CameraX Scanner • Anti-Replay
                        </p>
                      </div>
                    </div>
                    <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20">
                      One-Time
                    </span>
                  </div>

                  <p className="text-xs text-slate-600 dark:text-slate-300 mt-3 leading-relaxed">
                    Scan TrustPay payment QR codes via camera, system tokens, or URI. Validates freshness, nonces, and signatures.
                  </p>

                  <button
                    onClick={() => setShowScanPayModal(true)}
                    className="mt-4 w-full py-2.5 px-4 rounded-xl font-bold text-xs bg-indigo-600 hover:bg-indigo-700 text-white flex items-center justify-center gap-2 transition shadow-sm"
                  >
                    <ScanLine className="w-4 h-4" />
                    Scan &amp; Pay
                  </button>
                </div>

                {/* Card 3: Dual Authorization Panel */}
                <div
                  className={`p-5 rounded-2xl border shadow-sm transition-all hover:shadow-md flex flex-col justify-between ${
                    isDarkTheme
                      ? 'bg-slate-900/80 border-slate-800 text-slate-100'
                      : 'bg-white border-slate-200 text-slate-800'
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 rounded-xl bg-violet-500/10 border border-violet-500/30 flex items-center justify-center text-violet-600 dark:text-violet-400">
                        <KeyRound className="w-6 h-6" />
                      </div>
                      <div>
                        <h3 className="text-base font-extrabold">Dual Authorization</h3>
                        <p className="text-xs text-slate-500 dark:text-slate-400">
                          Two-Person Rule • 5-min TTL
                        </p>
                      </div>
                    </div>
                    {pendingDualAuthCount > 0 ? (
                      <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-600 dark:text-amber-400 border border-amber-500/30 flex items-center gap-1">
                        <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-ping" />
                        {pendingDualAuthCount} Pending
                      </span>
                    ) : (
                      <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-violet-500/10 text-violet-600 dark:text-violet-400 border border-violet-500/20">
                        Two Signers
                      </span>
                    )}
                  </div>

                  <p className="text-xs text-slate-600 dark:text-slate-300 mt-3 leading-relaxed">
                    Require two separate authorised people to approve corporate transfers. Separate from standard single-user flow.
                  </p>

                  <button
                    onClick={() => setShowDualAuthModal(true)}
                    className="mt-4 w-full py-2.5 px-4 rounded-xl font-bold text-xs bg-violet-600 hover:bg-violet-700 text-white flex items-center justify-center gap-2 transition shadow-sm"
                  >
                    <KeyRound className="w-4 h-4" />
                    Dual Auth Panel
                  </button>
                </div>

                {/* Card 4: Location Distance Classifier */}
                <div
                  className={`p-5 rounded-2xl border shadow-sm transition-all hover:shadow-md flex flex-col justify-between ${
                    isDarkTheme
                      ? 'bg-slate-900/80 border-slate-800 text-slate-100'
                      : 'bg-white border-slate-200 text-slate-800'
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-600 dark:text-emerald-400">
                        <Compass className="w-6 h-6" />
                      </div>
                      <div>
                        <h3 className="text-base font-extrabold">Distance Classifier</h3>
                        <p className="text-xs text-slate-500 dark:text-slate-400">
                          Haversine • 4 Distance Tiers
                        </p>
                      </div>
                    </div>
                    <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20">
                      GPS Active
                    </span>
                  </div>

                  <p className="text-xs text-slate-600 dark:text-slate-300 mt-3 leading-relaxed">
                    Calculate distances in km from current GPS location. Categorizes into Usual (0–10km), Medium (10–20km), Far (20–30km), and High (&gt;30km).
                  </p>

                  <button
                    onClick={() => setActiveTab('LOCATION_CLASSIFIER')}
                    className="mt-4 w-full py-2.5 px-4 rounded-xl font-bold text-xs bg-emerald-600 hover:bg-emerald-700 text-white flex items-center justify-center gap-2 transition shadow-sm"
                  >
                    <Compass className="w-4 h-4" />
                    Distance Classifier
                  </button>
                </div>
              </div>
            </div>

            {/* 3. Main Operational Dashboard Grid */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
              {/* Left Column: Payment Form + Velocity Anomaly Monitor */}
              <div className="lg:col-span-6 flex flex-col gap-6">
                <PaymentInputCard
                  uiState={compositeUiState}
                  onRecipientChanged={setRecipientInput}
                  onAmountChanged={setAmountInput}
                  onToggleUntrustedDevice={setUntrustedDevice}
                  onToggleNewRecipient={setNewRecipient}
                  onToggleUnusualContext={setUnusualContext}
                  onAnalyseAndPay={handleAnalyseAndPay}
                  onTamperAttack={handleTamperAmountAttack}
                  onTamperRecipientAttack={handleTamperRecipientAttack}
                  onResetHistory={handleResetHistory}
                  onPrefill={(rec, amt) => {
                    setRecipientInput(rec);
                    setAmountInput(amt);
                  }}
                />

                <VelocityMonitorCard
                  uiState={compositeUiState}
                  onResetHistory={handleResetHistory}
                />
              </div>

              {/* Right Column: Risk Assessment + Successful Transactions + Audit Log */}
              <div className="lg:col-span-6 flex flex-col gap-6">
                <RiskAssessmentCard uiState={compositeUiState} />

                <SuccessfulTransactionHistoryCard
                  transactions={transactions}
                  isDarkTheme={isDarkTheme}
                />

                <AuditLogSection
                  logs={auditLogs}
                  onClearLogs={() => {
                    TransactionStorage.clearAuditLogs();
                    setAuditLogs([]);
                  }}
                />
              </div>
            </div>
          </>
        )}
      </div>

      {/* MODALS */}
      {/* 1. 3-Step Verification Modal */}
      {verificationState && (
        <VerificationFlowModal
          state={verificationState}
          onBiometricResult={handleVerificationBiometric}
          onChallengeSubmit={handleVerificationChallenge}
          onOtpSubmit={handleVerificationOtp}
          onCancel={handleCancelVerification}
        />
      )}

      {/* 2. Standard Biometric Modal */}
      {showBiometricModal && pendingSnapshot && (
        <BiometricSimulationModal
          recipient={pendingSnapshot.recipient}
          amount={pendingSnapshot.amount}
          onSuccess={handleBiometricModalSuccess}
          onFailure={handleBiometricModalFailure}
          onCancel={handleBiometricModalCancel}
        />
      )}

      {/* 3. Demo Scenario Walkthrough Modal */}
      {showGuideModal && (
        <DemoScenarioGuideModal onClose={() => setShowGuideModal(false)} />
      )}

      {/* 4. Reset Balance Confirmation Modal */}
      {showResetBalanceModal && (
        <ResetBalanceModal
          currentBalance={balance}
          onConfirm={handleConfirmResetBalance}
          onCancel={() => setShowResetBalanceModal(false)}
        />
      )}

      {/* 5. Receive Money - Generate QR Modal */}
      {showReceiveQrModal && activeUserLocal && (
        <ReceiveMoneyQrModal
          currentUser={activeUserLocal}
          isDarkTheme={isDarkTheme}
          onClose={() => setShowReceiveQrModal(false)}
        />
      )}

      {/* 6. Scan & Pay Modal */}
      {showScanPayModal && activeUserLocal && (
        <ScanAndPayModal
          currentUser={activeUserLocal}
          currentBalance={balance}
          isDarkTheme={isDarkTheme}
          onClose={() => setShowScanPayModal(false)}
          onPaymentCompleted={(tx, newBal) => {
            setBalance(newBal);
            setTransactions((prev) => [tx, ...prev]);
            addAuditLog(
              'P2P Payment Completed',
              `Paid ₹${tx.amount.toFixed(2)} to ${tx.recipient} via secure QR.`,
              'SUCCESS',
              tx.bindingHash
            );
          }}
        />
      )}

      {/* 7. QR History Modal */}
      {showQrHistoryModal && (
        <QrHistoryModal
          isDarkTheme={isDarkTheme}
          onClose={() => setShowQrHistoryModal(false)}
        />
      )}

      {/* 8. Admin & SOC Panel Modal */}
      {showAdminModal && activeUserLocal && (
        <AdminPanelModal
          currentUser={activeUserLocal}
          isDarkTheme={isDarkTheme}
          onClose={() => setShowAdminModal(false)}
          onSwitchUser={handleSwitchUser}
        />
      )}

      {/* 9. Dual Authorization Panel Modal */}
      {showDualAuthModal && activeUserLocal && (
        <DualAuthPanelModal
          currentUser={activeUserLocal}
          isDarkTheme={isDarkTheme}
          onClose={() => setShowDualAuthModal(false)}
          onSwitchUser={handleSwitchUser}
          onBalanceUpdated={() => {
            const newBal = TransactionStorage.getUserBalance(activeUserLocal.email);
            setBalance(newBal);
            addAuditLog(
              'Dual-Authorization Release',
              `Balance synchronized following dual-authorization settlement.`,
              'SUCCESS'
            );
          }}
        />
      )}
    </div>
  );
};
