import React, { useState, useEffect, useMemo } from 'react';
import {
  AuditLogEntry,
  CoSigner,
  DualAuthStatus,
  DualAuthTransaction,
  InAppNotification,
  UserAccount,
} from '../types';
import { TransactionStorage } from '../storage/transactionStorage';
import {
  computeBindingHash,
  computeDualAuthSecondSignature,
  formatIndianCurrency,
} from '../security/crypto';
import {
  ShieldAlert,
  ShieldCheck,
  Fingerprint,
  Users,
  Clock,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Search,
  Copy,
  Check,
  Lock,
  ArrowRight,
  FileText,
  RefreshCw,
  UserPlus,
  X,
  Bell,
  KeyRound,
  Sparkles,
  ChevronRight,
  Send,
  SlidersHorizontal,
} from 'lucide-react';

interface DualAuthPanelModalProps {
  currentUser: UserAccount;
  isDarkTheme: boolean;
  onClose: () => void;
  onSwitchUser: (user: UserAccount) => void;
  onBalanceUpdated: () => void;
}

type TabType = 'CREATE' | 'PENDING' | 'HISTORY' | 'MANAGE_COSIGNERS';

export const DualAuthPanelModal: React.FC<DualAuthPanelModalProps> = ({
  currentUser,
  isDarkTheme,
  onClose,
  onSwitchUser,
  onBalanceUpdated,
}) => {
  const [activeTab, setActiveTab] = useState<TabType>('CREATE');

  // Co-signers & Transactions state
  const [coSigners, setCoSigners] = useState<CoSigner[]>([]);
  const [dualTransactions, setDualTransactions] = useState<DualAuthTransaction[]>([]);
  const [notifications, setNotifications] = useState<InAppNotification[]>([]);
  const [now, setNow] = useState<number>(Date.now());

  // Form State: Create Dual-Approval Payment
  const [recipient, setRecipient] = useState('');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [selectedCoSignerEmail, setSelectedCoSignerEmail] = useState('');

  // Simulated Risk Toggles for User 1 creation
  const [untrustedDevice, setUntrustedDevice] = useState(false);
  const [newRecipient, setNewRecipient] = useState(false);
  const [unusualContext, setUnusualContext] = useState(false);

  // User 1 Biometric Verification step
  const [isVerifyingUser1, setIsVerifyingUser1] = useState(false);
  const [user1BioStep, setUser1BioStep] = useState<'IDLE' | 'BIOMETRIC_SCAN' | 'DONE'>('IDLE');
  const [user1FormError, setUser1FormError] = useState<string | null>(null);
  const [recentlyCreatedTx, setRecentlyCreatedTx] = useState<DualAuthTransaction | null>(null);

  // User 2 Approval & Rejection Modal State
  const [selectedTxForApproval, setSelectedTxForApproval] = useState<DualAuthTransaction | null>(null);
  const [isApproving, setIsApproving] = useState(false);
  const [user2BioStatus, setUser2BioStatus] = useState<'IDLE' | 'SCANNING' | 'VERIFIED'>('IDLE');
  const [approvalConfirmedCheck, setApprovalConfirmedCheck] = useState(false);
  const [approvalError, setApprovalError] = useState<string | null>(null);

  // Rejection modal state
  const [selectedTxForRejection, setSelectedTxForRejection] = useState<DualAuthTransaction | null>(null);
  const [rejectionReason, setRejectionReason] = useState('');

  // Manage Co-Signers Form
  const [newSignerName, setNewSignerName] = useState('');
  const [newSignerEmail, setNewSignerEmail] = useState('');
  const [newSignerRole, setNewSignerRole] = useState('');
  const [signerFormMsg, setSignerFormMsg] = useState<{ text: string; isError: boolean } | null>(null);

  // History filter and search
  const [historySearch, setHistorySearch] = useState('');
  const [historyFilter, setHistoryFilter] = useState<string>('ALL');
  const [copiedHash, setCopiedHash] = useState<string | null>(null);

  // Live timer tick for 5-minute expiry updates
  useEffect(() => {
    const timer = setInterval(() => {
      setNow(Date.now());
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  // Reload data
  const loadData = () => {
    const signers = TransactionStorage.getCoSigners();
    setCoSigners(signers);

    // Auto select first available co-signer that is NOT current user
    if (!selectedCoSignerEmail) {
      const firstOther = signers.find((s) => s.email.toLowerCase() !== currentUser.email.toLowerCase() && s.active);
      if (firstOther) {
        setSelectedCoSignerEmail(firstOther.email);
      }
    }

    const txs = TransactionStorage.getDualAuthTransactions();
    setDualTransactions(txs);

    const notifs = TransactionStorage.getInAppNotifications(currentUser.email);
    setNotifications(notifs);
  };

  useEffect(() => {
    loadData();
  }, [currentUser.email]);

  // Keep selected co-signer valid when switching users
  useEffect(() => {
    const activeOthers = coSigners.filter(
      (s) => s.email.toLowerCase() !== currentUser.email.toLowerCase() && s.active
    );
    if (activeOthers.length > 0) {
      if (!selectedCoSignerEmail || selectedCoSignerEmail.toLowerCase() === currentUser.email.toLowerCase()) {
        setSelectedCoSignerEmail(activeOthers[0].email);
      }
    }
  }, [currentUser, coSigners]);

  // Pending count badge
  const pendingCount = useMemo(() => {
    return dualTransactions.filter((t) => t.status === 'PENDING SECOND AUTHORIZATION' && now <= t.expiresAt).length;
  }, [dualTransactions, now]);

  // Current user balance
  const userBalance = TransactionStorage.getUserBalance(currentUser.email);

  // Copy helper
  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedHash(text);
    setTimeout(() => setCopiedHash(null), 2000);
  };

  // ----------------------------------------------------
  // USER 1: CREATE DUAL-APPROVAL PAYMENT
  // ----------------------------------------------------
  const handleStartUser1Initiation = () => {
    setUser1FormError(null);
    const parsedAmt = parseFloat(amount);

    if (!recipient.trim()) {
      setUser1FormError('Please enter a valid recipient UPI ID or account.');
      return;
    }

    if (isNaN(parsedAmt) || parsedAmt <= 0) {
      setUser1FormError('Please enter a valid positive transfer amount.');
      return;
    }

    if (userBalance < parsedAmt) {
      setUser1FormError(
        `Insufficient account balance. Available: ${formatIndianCurrency(userBalance)}, Required: ${formatIndianCurrency(
          parsedAmt
        )}`
      );
      return;
    }

    if (!selectedCoSignerEmail) {
      setUser1FormError('Please select an authorized second signer.');
      return;
    }

    if (selectedCoSignerEmail.toLowerCase() === currentUser.email.toLowerCase()) {
      setUser1FormError('Security Violation: User 1 cannot select themselves as the second signer.');
      return;
    }

    // Begin User 1 Biometric Verification step
    setIsVerifyingUser1(true);
    setUser1BioStep('BIOMETRIC_SCAN');
  };

  const handleCompleteUser1Verification = () => {
    setUser1BioStep('DONE');
    const parsedAmt = parseFloat(amount);
    const targetCoSigner = coSigners.find((c) => c.email.toLowerCase() === selectedCoSignerEmail.toLowerCase());

    if (!targetCoSigner) {
      setUser1FormError('Selected co-signer not found.');
      setIsVerifyingUser1(false);
      return;
    }

    // Heuristic risk score
    let calculatedRisk = 15;
    const signals: string[] = [];
    if (parsedAmt > 25000) {
      calculatedRisk += 30;
      signals.push('High-Value Dual Transaction Threshold');
    }
    if (untrustedDevice) {
      calculatedRisk += 35;
      signals.push('Untrusted Corporate Terminal');
    }
    if (newRecipient) {
      calculatedRisk += 25;
      signals.push('Unverified Commercial Vendor');
    }
    if (unusualContext) {
      calculatedRisk += 20;
      signals.push('Off-hours Settlement Context');
    }

    // Cryptographic binding hash for User 1
    const bindingHash = computeBindingHash(
      `TEMP_DUAL_${Date.now()}`,
      recipient.trim(),
      parsedAmt,
      'INR'
    );

    // Create dual-authorization request (Status: PENDING SECOND AUTHORIZATION, NO balance deduction!)
    const created = TransactionStorage.createDualAuthPayment({
      initiator: currentUser,
      coSigner: targetCoSigner,
      recipient: recipient.trim(),
      amount: parsedAmt,
      note: note.trim() || 'Dual-Authorization Enterprise Transfer',
      riskScore: calculatedRisk,
      signals,
      bindingHash,
    });

    // Also mirror to Express server
    fetch('/api/dual-auth/create', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        initiatorId: currentUser.email,
        initiatorName: currentUser.fullName,
        coSignerId: targetCoSigner.email,
        coSignerName: targetCoSigner.fullName,
        recipient: recipient.trim(),
        amount: parsedAmt,
        note: note.trim(),
        riskScore: calculatedRisk,
        signals,
        bindingHash,
      }),
    }).catch(() => {});

    setRecentlyCreatedTx(created);
    setIsVerifyingUser1(false);
    setUser1BioStep('IDLE');
    setRecipient('');
    setAmount('');
    setNote('');

    loadData();
  };

  // ----------------------------------------------------
  // USER 2: APPROVE AND SIGN FLOW
  // ----------------------------------------------------
  const handleOpenApprovalModal = (tx: DualAuthTransaction) => {
    setSelectedTxForApproval(tx);
    setUser2BioStatus('IDLE');
    setApprovalConfirmedCheck(false);
    setApprovalError(null);
  };

  const handleConfirmUser2Approval = () => {
    if (!selectedTxForApproval) return;
    setApprovalError(null);

    // Rule 1: Ensure User 2 !== User 1
    if (currentUser.email.toLowerCase() === selectedTxForApproval.initiatorId.toLowerCase()) {
      setApprovalError('Security Violation: User 1 must never approve their own transaction as User 2.');
      return;
    }

    // Rule 2: Biometric verification required
    if (user2BioStatus !== 'VERIFIED') {
      setApprovalError('Biometric authentication is required to sign this transaction.');
      return;
    }

    // Rule 3: Final confirmation check
    if (!approvalConfirmedCheck) {
      setApprovalError('Please check the confirmation box: "I approve this exact transaction".');
      return;
    }

    // Compute second approval signature linking:
    // transaction ID, amount, recipient, User 1 ID, User 2 ID, timestamp, and nonce
    const timestamp = Date.now();
    const secondSig = computeDualAuthSecondSignature(
      selectedTxForApproval.id,
      selectedTxForApproval.amount,
      selectedTxForApproval.recipient,
      selectedTxForApproval.initiatorId,
      currentUser.email,
      timestamp,
      selectedTxForApproval.nonce
    );

    // Execute atomic balance deduction & status update in storage
    const result = TransactionStorage.approveDualAuthPayment({
      transactionId: selectedTxForApproval.id,
      coSignerUser: currentUser,
      secondApprovalSignature: secondSig,
    });

    if (!result.success) {
      setApprovalError(result.error || 'Approval failed.');
      return;
    }

    // Mirror to Express backend
    fetch('/api/dual-auth/approve', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        transactionId: selectedTxForApproval.id,
        coSignerId: currentUser.email,
        coSignerName: currentUser.fullName,
        secondApprovalSignature: secondSig,
      }),
    }).catch(() => {});

    setSelectedTxForApproval(null);
    onBalanceUpdated();
    loadData();
  };

  // ----------------------------------------------------
  // USER 2: REJECT FLOW
  // ----------------------------------------------------
  const handleOpenRejectionModal = (tx: DualAuthTransaction) => {
    setSelectedTxForRejection(tx);
    setRejectionReason('');
  };

  const handleConfirmRejection = () => {
    if (!selectedTxForRejection) return;
    const reason = rejectionReason.trim() || 'Declined by authorized co-signer.';

    TransactionStorage.rejectDualAuthPayment({
      transactionId: selectedTxForRejection.id,
      coSignerUser: currentUser,
      reason,
    });

    fetch('/api/dual-auth/reject', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        transactionId: selectedTxForRejection.id,
        coSignerId: currentUser.email,
        reason,
      }),
    }).catch(() => {});

    setSelectedTxForRejection(null);
    loadData();
  };

  // ----------------------------------------------------
  // MANAGE CO-SIGNERS
  // ----------------------------------------------------
  const handleAddCoSigner = (e: React.FormEvent) => {
    e.preventDefault();
    setSignerFormMsg(null);

    if (!newSignerName.trim() || !newSignerEmail.trim() || !newSignerRole.trim()) {
      setSignerFormMsg({ text: 'Please fill out all fields.', isError: true });
      return;
    }

    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailPattern.test(newSignerEmail.trim())) {
      setSignerFormMsg({ text: 'Please enter a valid email address.', isError: true });
      return;
    }

    const newSigner: CoSigner = {
      id: `cosigner_${Date.now()}`,
      fullName: newSignerName.trim(),
      email: newSignerEmail.trim().toLowerCase(),
      role: newSignerRole.trim(),
      active: true,
      addedAt: Date.now(),
    };

    TransactionStorage.addCoSigner(newSigner);

    // Mirror to server
    fetch('/api/dual-auth/co-signers', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newSigner),
    }).catch(() => {});

    setSignerFormMsg({ text: `Added ${newSigner.fullName} as authorized co-signer.`, isError: false });
    setNewSignerName('');
    setNewSignerEmail('');
    setNewSignerRole('');
    loadData();
  };

  const handleToggleSigner = (id: string) => {
    TransactionStorage.toggleCoSignerActive(id);
    loadData();
  };

  // Quick switch user helper for testing 2 signers
  const allDemoUsers = TransactionStorage.getUsers();

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-slate-950/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div
        className={`w-full max-w-4xl rounded-2xl border shadow-2xl overflow-hidden flex flex-col max-h-[94vh] ${
          isDarkTheme ? 'bg-slate-900 border-slate-700 text-slate-100' : 'bg-white border-slate-200 text-slate-800'
        }`}
      >
        {/* Modal Top Header */}
        <div
          className={`px-5 py-4 border-b flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
            isDarkTheme ? 'border-slate-800 bg-slate-900/80' : 'border-slate-100 bg-slate-50'
          }`}
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-violet-500/10 border border-violet-500/30 flex items-center justify-center text-violet-600 dark:text-violet-400">
              <KeyRound className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base sm:text-lg font-extrabold tracking-tight">Dual Authorization Panel</h2>
                <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-violet-500/10 text-violet-600 dark:text-violet-400 border border-violet-500/20">
                  Two-Person Key
                </span>
              </div>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Independent multi-party approval engine • 5-minute rotating TTL • Cryptographic dual signatures
              </p>
            </div>
          </div>

          {/* User badge & Close */}
          <div className="flex items-center gap-2">
            <div
              className={`px-3 py-1.5 rounded-lg border text-xs flex items-center gap-2 ${
                isDarkTheme ? 'bg-slate-800/80 border-slate-700' : 'bg-white border-slate-200'
              }`}
            >
              <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
              <div className="text-left">
                <span className="text-[10px] text-slate-400 block leading-none">Signed in as</span>
                <span className="font-bold truncate max-w-[130px] block leading-tight">{currentUser.fullName}</span>
              </div>
            </div>

            <button
              onClick={onClose}
              className="p-2 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
              title="Close Panel"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Demo User Switcher Bar (Crucial for simulating User 1 and User 2 independently!) */}
        <div
          className={`px-5 py-2.5 border-b text-xs flex flex-wrap items-center justify-between gap-2 ${
            isDarkTheme ? 'bg-violet-950/20 border-slate-800' : 'bg-violet-50/60 border-violet-100'
          }`}
        >
          <div className="flex items-center gap-1.5 font-bold text-violet-700 dark:text-violet-300">
            <Users className="w-3.5 h-3.5" />
            <span>Simulate Two Signers:</span>
            <span className="font-normal text-slate-500 dark:text-slate-400 text-[11px]">
              (Switch to test User 1 initiator vs User 2 co-signer approval)
            </span>
          </div>

          <div className="flex items-center gap-1.5">
            {allDemoUsers.map((u) => {
              const isCurrent = u.email === currentUser.email;
              return (
                <button
                  key={u.email}
                  onClick={() => {
                    if (!isCurrent) {
                      TransactionStorage.setActiveUser(u);
                      onSwitchUser(u);
                    }
                  }}
                  className={`px-2.5 py-1 rounded-md text-[11px] font-bold transition flex items-center gap-1 ${
                    isCurrent
                      ? 'bg-violet-600 text-white shadow-sm'
                      : isDarkTheme
                      ? 'bg-slate-800 hover:bg-slate-700 text-slate-300'
                      : 'bg-white hover:bg-slate-100 text-slate-700 border border-slate-200'
                  }`}
                >
                  <span>{u.fullName}</span>
                  {isCurrent && <span className="text-[9px] opacity-80">(Active)</span>}
                </button>
              );
            })}
          </div>
        </div>

        {/* Navigation Tabs */}
        <div
          className={`px-5 py-2 border-b flex items-center gap-2 overflow-x-auto ${
            isDarkTheme ? 'border-slate-800 bg-slate-900/40' : 'border-slate-100 bg-slate-50/50'
          }`}
        >
          <button
            onClick={() => setActiveTab('CREATE')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition flex items-center gap-1.5 whitespace-nowrap ${
              activeTab === 'CREATE'
                ? 'bg-violet-600 text-white shadow-sm'
                : 'text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
            }`}
          >
            <Send className="w-3.5 h-3.5" />
            Create Dual-Approval Payment
          </button>

          <button
            onClick={() => setActiveTab('PENDING')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition flex items-center gap-1.5 whitespace-nowrap ${
              activeTab === 'PENDING'
                ? 'bg-violet-600 text-white shadow-sm'
                : 'text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
            }`}
          >
            <Clock className="w-3.5 h-3.5" />
            Pending Approvals
            {pendingCount > 0 && (
              <span className="px-1.5 py-0.2 rounded-full bg-amber-500 text-slate-950 font-extrabold text-[10px]">
                {pendingCount}
              </span>
            )}
          </button>

          <button
            onClick={() => setActiveTab('HISTORY')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition flex items-center gap-1.5 whitespace-nowrap ${
              activeTab === 'HISTORY'
                ? 'bg-violet-600 text-white shadow-sm'
                : 'text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
            }`}
          >
            <FileText className="w-3.5 h-3.5" />
            Dual-Approval History
          </button>

          <button
            onClick={() => setActiveTab('MANAGE_COSIGNERS')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition flex items-center gap-1.5 whitespace-nowrap ${
              activeTab === 'MANAGE_COSIGNERS'
                ? 'bg-violet-600 text-white shadow-sm'
                : 'text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
            }`}
          >
            <Users className="w-3.5 h-3.5" />
            Manage Co-Signers
          </button>

          <div className="ml-auto">
            <button
              onClick={onClose}
              className="px-3 py-1.5 rounded-lg text-xs font-bold text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-800 transition flex items-center gap-1"
            >
              <ArrowRight className="w-3.5 h-3.5" />
              Back to Main Dashboard
            </button>
          </div>
        </div>

        {/* Modal Scrollable Content Body */}
        <div className="p-5 overflow-y-auto flex-1 space-y-5">
          {/* ============================================================ */}
          {/* TAB 1: CREATE DUAL-APPROVAL PAYMENT                          */}
          {/* ============================================================ */}
          {activeTab === 'CREATE' && (
            <div className="space-y-5 max-w-2xl mx-auto">
              <div
                className={`p-4 rounded-xl border flex items-start justify-between gap-3 ${
                  isDarkTheme ? 'bg-slate-800/40 border-slate-700' : 'bg-violet-50/50 border-violet-100'
                }`}
              >
                <div className="space-y-1">
                  <div className="text-xs font-bold uppercase tracking-wider text-violet-600 dark:text-violet-400">
                    Dual Authorization Safeguard Policy
                  </div>
                  <p className="text-xs text-slate-600 dark:text-slate-300">
                    Payments initiated here will <strong>NOT</strong> deduct balance immediately. Status will enter{' '}
                    <code className="text-[11px] font-mono font-bold bg-amber-500/10 text-amber-700 dark:text-amber-300 px-1 py-0.5 rounded">
                      PENDING SECOND AUTHORIZATION
                    </code>{' '}
                    with a 5-minute cryptographic countdown. The transaction settles only when the designated co-signer approves.
                  </p>
                </div>
                <div className="text-right shrink-0">
                  <span className="text-[10px] text-slate-400 block">Your Available Balance</span>
                  <span className="text-sm font-bold font-mono text-emerald-600 dark:text-emerald-400">
                    {formatIndianCurrency(userBalance)}
                  </span>
                </div>
              </div>

              {/* Form Input Fields */}
              <div
                className={`p-5 rounded-2xl border space-y-4 shadow-sm ${
                  isDarkTheme ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-200'
                }`}
              >
                {user1FormError && (
                  <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-700 dark:text-rose-400 text-xs font-semibold flex items-center gap-2">
                    <AlertTriangle className="w-4 h-4 shrink-0" />
                    <span>{user1FormError}</span>
                  </div>
                )}

                {/* Recipient Input */}
                <div>
                  <label className="text-xs font-bold text-slate-600 dark:text-slate-300 mb-1 block">
                    Recipient (UPI ID / Merchant / Bank Account)
                  </label>
                  <input
                    type="text"
                    value={recipient}
                    onChange={(e) => setRecipient(e.target.value)}
                    placeholder="e.g. vendor-cloud@hdfcbank or payroll@icicibank"
                    className={`w-full px-3.5 py-2.5 rounded-xl border text-sm transition focus:outline-none focus:ring-2 focus:ring-violet-500 ${
                      isDarkTheme ? 'bg-slate-800/80 border-slate-700 text-slate-100' : 'bg-slate-50 border-slate-200'
                    }`}
                  />
                  <div className="flex items-center gap-1.5 mt-1.5 flex-wrap text-[10px] text-slate-400">
                    <span>Quick presets:</span>
                    <button
                      type="button"
                      onClick={() => setRecipient('infra-cloud@hdfcbank')}
                      className="text-violet-600 hover:underline"
                    >
                      infra-cloud@hdfcbank
                    </button>
                    <span>•</span>
                    <button
                      type="button"
                      onClick={() => setRecipient('vendor-security@icicibank')}
                      className="text-violet-600 hover:underline"
                    >
                      vendor-security@icicibank
                    </button>
                    <span>•</span>
                    <button
                      type="button"
                      onClick={() => setRecipient('office-lease@axis')}
                      className="text-violet-600 hover:underline"
                    >
                      office-lease@axis
                    </button>
                  </div>
                </div>

                {/* Amount Input */}
                <div>
                  <label className="text-xs font-bold text-slate-600 dark:text-slate-300 mb-1 block">
                    Transfer Amount (₹)
                  </label>
                  <div className="relative">
                    <span className="absolute left-3.5 top-2.5 text-slate-400 font-bold text-sm">₹</span>
                    <input
                      type="number"
                      step="any"
                      value={amount}
                      onChange={(e) => setAmount(e.target.value)}
                      placeholder="e.g. 50000.00"
                      className={`w-full pl-8 pr-3.5 py-2.5 rounded-xl border text-sm font-mono font-bold transition focus:outline-none focus:ring-2 focus:ring-violet-500 ${
                        isDarkTheme ? 'bg-slate-800/80 border-slate-700 text-slate-100' : 'bg-slate-50 border-slate-200'
                      }`}
                    />
                  </div>
                  <div className="flex items-center gap-2 mt-1.5 flex-wrap text-[10px]">
                    <button
                      type="button"
                      onClick={() => setAmount('15000')}
                      className="px-2 py-0.5 rounded bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 text-slate-600 dark:text-slate-300 font-mono"
                    >
                      ₹15,000
                    </button>
                    <button
                      type="button"
                      onClick={() => setAmount('50000')}
                      className="px-2 py-0.5 rounded bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 text-slate-600 dark:text-slate-300 font-mono"
                    >
                      ₹50,000
                    </button>
                    <button
                      type="button"
                      onClick={() => setAmount('100000')}
                      className="px-2 py-0.5 rounded bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 text-slate-600 dark:text-slate-300 font-mono"
                    >
                      ₹1,00,000
                    </button>
                  </div>
                </div>

                {/* Payment Note */}
                <div>
                  <label className="text-xs font-bold text-slate-600 dark:text-slate-300 mb-1 block">
                    Payment Note / Business Purpose
                  </label>
                  <input
                    type="text"
                    value={note}
                    onChange={(e) => setNote(e.target.value)}
                    placeholder="e.g. Annual Cloud Infrastructure Renewal"
                    className={`w-full px-3.5 py-2.5 rounded-xl border text-sm transition focus:outline-none focus:ring-2 focus:ring-violet-500 ${
                      isDarkTheme ? 'bg-slate-800/80 border-slate-700 text-slate-100' : 'bg-slate-50 border-slate-200'
                    }`}
                  />
                </div>

                {/* Select Authorised Co-Signer (User 2) */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-bold text-slate-600 dark:text-slate-300">
                      Designated Authorised Co-Signer (User 2)
                    </label>
                    <span className="text-[10px] text-slate-400">Excludes User 1 automatically</span>
                  </div>

                  <select
                    value={selectedCoSignerEmail}
                    onChange={(e) => setSelectedCoSignerEmail(e.target.value)}
                    className={`w-full px-3.5 py-2.5 rounded-xl border text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-violet-500 ${
                      isDarkTheme ? 'bg-slate-800 border-slate-700 text-slate-100' : 'bg-slate-50 border-slate-200'
                    }`}
                  >
                    {coSigners
                      .filter((c) => c.email.toLowerCase() !== currentUser.email.toLowerCase() && c.active)
                      .map((c) => (
                        <option key={c.email} value={c.email}>
                          {c.fullName} ({c.email}) — {c.role}
                        </option>
                      ))}
                  </select>
                </div>

                {/* Simulated Risk Context Toggles */}
                <div
                  className={`p-3 rounded-xl border space-y-2 text-xs ${
                    isDarkTheme ? 'bg-slate-800/30 border-slate-800' : 'bg-slate-50 border-slate-200'
                  }`}
                >
                  <div className="flex items-center gap-1 font-bold text-slate-600 dark:text-slate-300 text-[11px] uppercase tracking-wider">
                    <SlidersHorizontal className="w-3.5 h-3.5 text-violet-500" />
                    <span>Simulate Security Risk Signals (Optional)</span>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                    <label className="flex items-center gap-2 cursor-pointer text-[11px]">
                      <input
                        type="checkbox"
                        checked={untrustedDevice}
                        onChange={(e) => setUntrustedDevice(e.target.checked)}
                        className="rounded text-violet-600"
                      />
                      <span>Untrusted Device</span>
                    </label>

                    <label className="flex items-center gap-2 cursor-pointer text-[11px]">
                      <input
                        type="checkbox"
                        checked={newRecipient}
                        onChange={(e) => setNewRecipient(e.target.checked)}
                        className="rounded text-violet-600"
                      />
                      <span>New Vendor</span>
                    </label>

                    <label className="flex items-center gap-2 cursor-pointer text-[11px]">
                      <input
                        type="checkbox"
                        checked={unusualContext}
                        onChange={(e) => setUnusualContext(e.target.checked)}
                        className="rounded text-violet-600"
                      />
                      <span>Off-Hours Context</span>
                    </label>
                  </div>
                </div>

                {/* Submit Action */}
                <button
                  type="button"
                  onClick={handleStartUser1Initiation}
                  className="w-full py-3 px-4 rounded-xl font-bold text-sm bg-violet-600 hover:bg-violet-700 text-white flex items-center justify-center gap-2 transition shadow-sm"
                >
                  <Fingerprint className="w-4 h-4" />
                  Authenticate &amp; Initiate Dual-Approval Payment
                </button>
              </div>

              {/* Success Notification Card if recently created */}
              {recentlyCreatedTx && (
                <div
                  className={`p-5 rounded-2xl border animate-in fade-in zoom-in-95 duration-200 space-y-3 ${
                    isDarkTheme ? 'bg-amber-950/20 border-amber-500/30 text-amber-300' : 'bg-amber-50 border-amber-200 text-amber-900'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Clock className="w-5 h-5 text-amber-600 dark:text-amber-400 animate-pulse" />
                      <span className="font-extrabold text-sm">Dual-Approval Request Initiated!</span>
                    </div>
                    <span className="text-[10px] font-bold font-mono px-2 py-0.5 rounded bg-amber-500/20">
                      {recentlyCreatedTx.id}
                    </span>
                  </div>

                  <p className="text-xs leading-relaxed">
                    Status set to <strong>PENDING SECOND AUTHORIZATION</strong>. An in-app notification has been dispatched
                    to co-signer <strong>{recentlyCreatedTx.coSignerName}</strong> ({recentlyCreatedTx.coSignerId}).
                    Account balance has <strong>NOT</strong> been deducted.
                  </p>

                  <div className="flex items-center justify-between pt-2 border-t border-amber-500/20 text-xs">
                    <div>
                      <span>Expires in: </span>
                      <strong className="font-mono">5 minutes (300s)</strong>
                    </div>
                    <button
                      onClick={() => setActiveTab('PENDING')}
                      className="px-3 py-1 rounded-lg bg-amber-600 hover:bg-amber-700 text-white font-bold text-xs flex items-center gap-1 transition"
                    >
                      View in Pending Approvals
                      <ArrowRight className="w-3 h-3" />
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ============================================================ */}
          {/* TAB 2: PENDING APPROVALS                                     */}
          {/* ============================================================ */}
          {activeTab === 'PENDING' && (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold text-slate-800 dark:text-slate-200">
                    Awaiting Second Signer Authorization ({pendingCount})
                  </h3>
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Requires independent authentication, biometric verification, and confirmation by the authorised co-signer.
                  </p>
                </div>
                <button
                  onClick={loadData}
                  className="px-2.5 py-1 rounded-lg text-xs font-semibold bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 flex items-center gap-1"
                >
                  <RefreshCw className="w-3 h-3" />
                  Refresh
                </button>
              </div>

              {pendingCount === 0 ? (
                <div
                  className={`p-10 rounded-2xl border text-center space-y-2 ${
                    isDarkTheme ? 'bg-slate-900 border-slate-800' : 'bg-slate-50 border-slate-200'
                  }`}
                >
                  <CheckCircle2 className="w-8 h-8 mx-auto text-emerald-500 opacity-80" />
                  <div className="text-sm font-bold">No Pending Approvals</div>
                  <p className="text-xs text-slate-500 dark:text-slate-400 max-w-sm mx-auto">
                    There are no payments awaiting second approval. New requests will appear here with an active 5-minute countdown.
                  </p>
                </div>
              ) : (
                <div className="grid grid-cols-1 gap-4">
                  {dualTransactions
                    .filter((t) => t.status === 'PENDING SECOND AUTHORIZATION' && now <= t.expiresAt)
                    .map((tx) => {
                      const remainingMs = Math.max(0, tx.expiresAt - now);
                      const remainingSeconds = Math.ceil(remainingMs / 1000);
                      const minutes = Math.floor(remainingSeconds / 60);
                      const seconds = remainingSeconds % 60;
                      const formattedTime = `${minutes}:${seconds < 10 ? '0' : ''}${seconds}`;
                      const isExpired = remainingSeconds <= 0;

                      // Is current user the initiator?
                      const isInitiator = currentUser.email.toLowerCase() === tx.initiatorId.toLowerCase();
                      // Is current user the designated co-signer?
                      const isDesignatedCoSigner = currentUser.email.toLowerCase() === tx.coSignerId.toLowerCase();

                      return (
                        <div
                          key={tx.id}
                          className={`p-5 rounded-2xl border shadow-sm space-y-4 ${
                            isDarkTheme ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-200'
                          }`}
                        >
                          {/* Top row: Label and TTL counter */}
                          <div className="flex flex-wrap items-center justify-between gap-2">
                            <div className="flex items-center gap-2">
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-wider bg-violet-500/10 text-violet-700 dark:text-violet-300 border border-violet-500/20">
                                Dual-Authorization Transaction
                              </span>
                              <span className="font-mono text-xs font-bold text-slate-500 dark:text-slate-400">
                                {tx.id}
                              </span>
                            </div>

                            {/* 5-minute Countdown Timer */}
                            <div
                              className={`px-3 py-1 rounded-lg border flex items-center gap-1.5 font-mono text-xs font-bold ${
                                remainingSeconds < 60
                                  ? 'bg-rose-500/10 border-rose-500/30 text-rose-600 dark:text-rose-400 animate-pulse'
                                  : 'bg-amber-500/10 border-amber-500/30 text-amber-600 dark:text-amber-400'
                              }`}
                            >
                              <Clock className="w-3.5 h-3.5" />
                              <span>Window: {isExpired ? 'EXPIRED' : `${formattedTime} remaining`}</span>
                            </div>
                          </div>

                          {/* Transaction Details Grid */}
                          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3 text-xs">
                            <div
                              className={`p-3 rounded-xl border ${
                                isDarkTheme ? 'bg-slate-800/40 border-slate-800' : 'bg-slate-50 border-slate-200'
                              }`}
                            >
                              <span className="text-[10px] text-slate-400 block font-semibold">Initiator (User 1)</span>
                              <span className="font-bold text-slate-800 dark:text-slate-200 block truncate">
                                {tx.initiatorName}
                              </span>
                              <span className="text-[10px] font-mono text-slate-500">{tx.initiatorId}</span>
                            </div>

                            <div
                              className={`p-3 rounded-xl border ${
                                isDarkTheme ? 'bg-slate-800/40 border-slate-800' : 'bg-slate-50 border-slate-200'
                              }`}
                            >
                              <span className="text-[10px] text-slate-400 block font-semibold">Recipient</span>
                              <span className="font-bold text-slate-800 dark:text-slate-200 block truncate">
                                {tx.recipient}
                              </span>
                              <span className="text-[10px] text-slate-500">{tx.note || 'No note'}</span>
                            </div>

                            <div
                              className={`p-3 rounded-xl border ${
                                isDarkTheme ? 'bg-slate-800/40 border-slate-800' : 'bg-slate-50 border-slate-200'
                              }`}
                            >
                              <span className="text-[10px] text-slate-400 block font-semibold">Amount</span>
                              <span className="font-bold font-mono text-base text-violet-600 dark:text-violet-400 block">
                                {formatIndianCurrency(tx.amount)}
                              </span>
                              <span className="text-[10px] text-slate-500">
                                {new Date(tx.createdAt).toLocaleTimeString()}
                              </span>
                            </div>

                            <div
                              className={`p-3 rounded-xl border ${
                                isDarkTheme ? 'bg-slate-800/40 border-slate-800' : 'bg-slate-50 border-slate-200'
                              }`}
                            >
                              <span className="text-[10px] text-slate-400 block font-semibold">Co-Signer Target</span>
                              <span className="font-bold text-slate-800 dark:text-slate-200 block truncate">
                                {tx.coSignerName}
                              </span>
                              <span className="text-[10px] font-mono text-violet-600 dark:text-violet-400">
                                {tx.coSignerId}
                              </span>
                            </div>
                          </div>

                          {/* Risk Assessment Score Bar */}
                          <div className="flex items-center justify-between text-xs pt-1">
                            <div className="flex items-center gap-2">
                              <span className="text-[11px] text-slate-500">Risk Assessment:</span>
                              <span
                                className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                                  tx.riskScore > 50
                                    ? 'bg-rose-500/10 text-rose-600 border border-rose-500/20'
                                    : tx.riskScore > 30
                                    ? 'bg-amber-500/10 text-amber-600 border border-amber-500/20'
                                    : 'bg-emerald-500/10 text-emerald-600 border border-emerald-500/20'
                                }`}
                              >
                                {tx.riskScore}/100 {tx.riskScore > 50 ? 'HIGH RISK' : 'CLEARED'}
                              </span>
                              {tx.signals && tx.signals.length > 0 && (
                                <span className="text-[10px] text-slate-400">
                                  Signals: {tx.signals.join(', ')}
                                </span>
                              )}
                            </div>

                            <span className="text-[10px] font-mono text-slate-400 truncate max-w-[200px]" title={tx.firstApprovalSignature}>
                              Sig1: {tx.firstApprovalSignature.substring(0, 14)}...
                            </span>
                          </div>

                          {/* Action Buttons / Enforcement Warnings */}
                          <div className="pt-3 border-t border-slate-200 dark:border-slate-800 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
                            {isInitiator ? (
                              <div className="flex items-center gap-2 text-xs text-amber-600 dark:text-amber-400 bg-amber-500/10 border border-amber-500/20 px-3 py-2 rounded-xl w-full justify-between">
                                <div className="flex items-center gap-1.5">
                                  <Lock className="w-3.5 h-3.5 shrink-0" />
                                  <span>
                                    <strong>Enforcement:</strong> You initiated this payment. User 1 cannot approve their own transaction as User 2.
                                  </span>
                                </div>
                                <button
                                  type="button"
                                  onClick={() => {
                                    const coUser = allDemoUsers.find((u) => u.email.toLowerCase() === tx.coSignerId.toLowerCase());
                                    if (coUser) {
                                      TransactionStorage.setActiveUser(coUser);
                                      onSwitchUser(coUser);
                                    }
                                  }}
                                  className="px-2.5 py-1 rounded bg-amber-600 text-white font-bold text-[11px] whitespace-nowrap hover:bg-amber-700 transition"
                                >
                                  Switch to {tx.coSignerName}
                                </button>
                              </div>
                            ) : (
                              <>
                                <div className="text-xs text-slate-500 dark:text-slate-400 flex items-center gap-1">
                                  <ShieldCheck className="w-4 h-4 text-emerald-500" />
                                  <span>
                                    {isDesignatedCoSigner
                                      ? 'You are the authorized co-signer for this transfer.'
                                      : 'You have corporate co-signer privileges.'}
                                  </span>
                                </div>

                                <div className="flex items-center gap-2">
                                  <button
                                    onClick={() => handleOpenRejectionModal(tx)}
                                    className="px-3.5 py-2 rounded-xl text-xs font-bold bg-rose-500/10 hover:bg-rose-500/20 text-rose-700 dark:text-rose-300 border border-rose-500/20 transition"
                                  >
                                    Reject Payment
                                  </button>

                                  <button
                                    onClick={() => handleOpenApprovalModal(tx)}
                                    className="px-4 py-2 rounded-xl text-xs font-bold bg-emerald-600 hover:bg-emerald-700 text-white flex items-center gap-1.5 transition shadow-sm"
                                  >
                                    <Fingerprint className="w-3.5 h-3.5" />
                                    Approve and Sign
                                  </button>
                                </div>
                              </>
                            )}
                          </div>
                        </div>
                      );
                    })}
                </div>
              )}
            </div>
          )}

          {/* ============================================================ */}
          {/* TAB 3: DUAL-APPROVAL HISTORY                                 */}
          {/* ============================================================ */}
          {activeTab === 'HISTORY' && (
            <div className="space-y-4">
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
                <div>
                  <h3 className="text-sm font-bold text-slate-800 dark:text-slate-200">
                    Dual-Authorization Ledger &amp; Signatures ({dualTransactions.length})
                  </h3>
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Complete immutable audit history with dual cryptographic signatures and rejection logs.
                  </p>
                </div>

                {/* Filters */}
                <div className="flex items-center gap-2 flex-wrap">
                  <div className="relative">
                    <Search className="w-3.5 h-3.5 absolute left-2.5 top-2.5 text-slate-400" />
                    <input
                      type="text"
                      value={historySearch}
                      onChange={(e) => setHistorySearch(e.target.value)}
                      placeholder="Search transactions..."
                      className={`pl-8 pr-3 py-1.5 rounded-lg border text-xs focus:outline-none ${
                        isDarkTheme ? 'bg-slate-800 border-slate-700 text-slate-100' : 'bg-slate-50 border-slate-200'
                      }`}
                    />
                  </div>

                  <select
                    value={historyFilter}
                    onChange={(e) => setHistoryFilter(e.target.value)}
                    className={`px-2.5 py-1.5 rounded-lg border text-xs font-semibold ${
                      isDarkTheme ? 'bg-slate-800 border-slate-700 text-slate-100' : 'bg-slate-50 border-slate-200'
                    }`}
                  >
                    <option value="ALL">All Statuses</option>
                    <option value="APPROVED BY TWO SIGNERS">Approved (2 Signers)</option>
                    <option value="PENDING SECOND AUTHORIZATION">Pending</option>
                    <option value="REJECTED BY SECOND SIGNER">Rejected</option>
                    <option value="EXPIRED">Expired</option>
                  </select>
                </div>
              </div>

              {dualTransactions.length === 0 ? (
                <div
                  className={`p-10 rounded-2xl border text-center space-y-2 ${
                    isDarkTheme ? 'bg-slate-900 border-slate-800' : 'bg-slate-50 border-slate-200'
                  }`}
                >
                  <FileText className="w-8 h-8 mx-auto text-slate-400 opacity-60" />
                  <div className="text-sm font-bold">No Dual-Authorization Records</div>
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Dual-authorization transactions are kept strictly separate from standard payments.
                  </p>
                </div>
              ) : (
                <div className="space-y-3">
                  {dualTransactions
                    .filter((tx) => {
                      if (historyFilter !== 'ALL' && tx.status !== historyFilter) return false;
                      if (!historySearch.trim()) return true;
                      const q = historySearch.toLowerCase();
                      return (
                        tx.id.toLowerCase().includes(q) ||
                        tx.recipient.toLowerCase().includes(q) ||
                        tx.initiatorName.toLowerCase().includes(q) ||
                        tx.coSignerName.toLowerCase().includes(q) ||
                        tx.note.toLowerCase().includes(q)
                      );
                    })
                    .map((tx) => {
                      return (
                        <div
                          key={tx.id}
                          className={`p-4 rounded-xl border text-xs space-y-3 ${
                            isDarkTheme ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-200'
                          }`}
                        >
                          <div className="flex flex-wrap items-center justify-between gap-2">
                            <div className="flex items-center gap-2">
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-wider bg-violet-500/10 text-violet-700 dark:text-violet-300 border border-violet-500/20">
                                Dual-Authorization Transaction
                              </span>
                              <span className="font-mono font-bold">{tx.id}</span>
                            </div>

                            {/* Status Badge */}
                            <span
                              className={`px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-wider ${
                                tx.status === 'APPROVED BY TWO SIGNERS'
                                  ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20'
                                  : tx.status === 'PENDING SECOND AUTHORIZATION'
                                  ? 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20'
                                  : tx.status === 'REJECTED BY SECOND SIGNER'
                                  ? 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/20'
                                  : 'bg-slate-500/10 text-slate-500 border border-slate-500/20'
                              }`}
                            >
                              {tx.status}
                            </span>
                          </div>

                          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-[11px]">
                            <div>
                              <span className="text-slate-400 block text-[10px]">Initiator (User 1)</span>
                              <span className="font-bold">{tx.initiatorName}</span>
                            </div>
                            <div>
                              <span className="text-slate-400 block text-[10px]">Recipient</span>
                              <span className="font-bold">{tx.recipient}</span>
                            </div>
                            <div>
                              <span className="text-slate-400 block text-[10px]">Amount</span>
                              <span className="font-bold font-mono text-sm text-violet-600 dark:text-violet-400">
                                {formatIndianCurrency(tx.amount)}
                              </span>
                            </div>
                            <div>
                              <span className="text-slate-400 block text-[10px]">Co-Signer</span>
                              <span className="font-bold">{tx.approvedByUserName || tx.coSignerName}</span>
                            </div>
                          </div>

                          {/* Rejection reason if any */}
                          {tx.rejectionReason && (
                            <div className="p-2 rounded-lg bg-rose-500/5 border border-rose-500/20 text-rose-600 dark:text-rose-400 text-[11px]">
                              <strong>Rejection Reason:</strong> {tx.rejectionReason}
                            </div>
                          )}

                          {/* Cryptographic Signature Breakdown */}
                          <div
                            className={`p-2.5 rounded-lg border text-[10px] space-y-1 font-mono ${
                              isDarkTheme ? 'bg-slate-950/60 border-slate-800' : 'bg-slate-50 border-slate-200'
                            }`}
                          >
                            <div className="flex items-center justify-between">
                              <span className="text-slate-400">Signature 1 (User 1):</span>
                              <div className="flex items-center gap-1">
                                <span className="text-violet-500 truncate max-w-[200px]" title={tx.firstApprovalSignature}>
                                  {tx.firstApprovalSignature}
                                </span>
                                <button
                                  onClick={() => handleCopy(tx.firstApprovalSignature)}
                                  className="text-slate-400 hover:text-slate-600"
                                >
                                  {copiedHash === tx.firstApprovalSignature ? <Check className="w-3 h-3 text-emerald-500" /> : <Copy className="w-3 h-3" />}
                                </button>
                              </div>
                            </div>

                            {tx.secondApprovalSignature && (
                              <div className="flex items-center justify-between">
                                <span className="text-slate-400">Signature 2 (User 2):</span>
                                <div className="flex items-center gap-1">
                                  <span className="text-emerald-500 truncate max-w-[200px]" title={tx.secondApprovalSignature}>
                                    {tx.secondApprovalSignature}
                                  </span>
                                  <button
                                    onClick={() => handleCopy(tx.secondApprovalSignature!)}
                                    className="text-slate-400 hover:text-slate-600"
                                  >
                                    {copiedHash === tx.secondApprovalSignature ? <Check className="w-3 h-3 text-emerald-500" /> : <Copy className="w-3 h-3" />}
                                  </button>
                                </div>
                              </div>
                            )}

                            <div className="flex items-center justify-between text-[9px] text-slate-500 pt-0.5">
                              <span>Nonce: {tx.nonce}</span>
                              <span>Timestamp: {new Date(tx.createdAt).toLocaleString()}</span>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                </div>
              )}
            </div>
          )}

          {/* ============================================================ */}
          {/* TAB 4: MANAGE CO-SIGNERS                                     */}
          {/* ============================================================ */}
          {activeTab === 'MANAGE_COSIGNERS' && (
            <div className="space-y-5 max-w-2xl mx-auto">
              <div>
                <h3 className="text-sm font-bold text-slate-800 dark:text-slate-200">
                  Authorised Corporate Co-Signers ({coSigners.length})
                </h3>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  Only active authorised co-signers can execute secondary authorizations for enterprise transfers.
                </p>
              </div>

              {/* Add Co-Signer Form */}
              <form
                onSubmit={handleAddCoSigner}
                className={`p-4 rounded-xl border space-y-3 ${
                  isDarkTheme ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-200'
                }`}
              >
                <div className="text-xs font-bold uppercase tracking-wider text-violet-600 dark:text-violet-400 flex items-center gap-1.5">
                  <UserPlus className="w-3.5 h-3.5" />
                  Add Authorised Co-Signer
                </div>

                {signerFormMsg && (
                  <div
                    className={`p-2.5 rounded-lg text-xs font-semibold ${
                      signerFormMsg.isError
                        ? 'bg-rose-500/10 text-rose-600 border border-rose-500/20'
                        : 'bg-emerald-500/10 text-emerald-600 border border-emerald-500/20'
                    }`}
                  >
                    {signerFormMsg.text}
                  </div>
                )}

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                  <input
                    type="text"
                    value={newSignerName}
                    onChange={(e) => setNewSignerName(e.target.value)}
                    placeholder="Full Name"
                    className={`px-3 py-2 rounded-lg border text-xs focus:outline-none ${
                      isDarkTheme ? 'bg-slate-800 border-slate-700 text-slate-100' : 'bg-slate-50 border-slate-200'
                    }`}
                  />

                  <input
                    type="email"
                    value={newSignerEmail}
                    onChange={(e) => setNewSignerEmail(e.target.value)}
                    placeholder="Email Address"
                    className={`px-3 py-2 rounded-lg border text-xs focus:outline-none ${
                      isDarkTheme ? 'bg-slate-800 border-slate-700 text-slate-100' : 'bg-slate-50 border-slate-200'
                    }`}
                  />

                  <input
                    type="text"
                    value={newSignerRole}
                    onChange={(e) => setNewSignerRole(e.target.value)}
                    placeholder="Corporate Role"
                    className={`px-3 py-2 rounded-lg border text-xs focus:outline-none ${
                      isDarkTheme ? 'bg-slate-800 border-slate-700 text-slate-100' : 'bg-slate-50 border-slate-200'
                    }`}
                  />
                </div>

                <button
                  type="submit"
                  className="w-full py-2 rounded-lg font-bold text-xs bg-violet-600 hover:bg-violet-700 text-white transition flex items-center justify-center gap-1.5"
                >
                  <UserPlus className="w-3.5 h-3.5" />
                  Save New Authorised Co-Signer
                </button>
              </form>

              {/* Co-Signers List */}
              <div className="space-y-2">
                {coSigners.map((signer) => (
                  <div
                    key={signer.id}
                    className={`p-3.5 rounded-xl border flex items-center justify-between text-xs transition ${
                      isDarkTheme ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-200'
                    }`}
                  >
                    <div className="space-y-0.5">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-slate-800 dark:text-slate-200">{signer.fullName}</span>
                        <span className="text-[10px] px-2 py-0.5 rounded bg-violet-500/10 text-violet-600 dark:text-violet-400 font-semibold border border-violet-500/20">
                          {signer.role}
                        </span>
                      </div>
                      <div className="text-[11px] font-mono text-slate-500">{signer.email}</div>
                    </div>

                    <div className="flex items-center gap-3">
                      <button
                        onClick={() => handleToggleSigner(signer.id)}
                        className={`px-3 py-1 rounded-lg text-xs font-bold transition ${
                          signer.active
                            ? 'bg-emerald-500/10 text-emerald-600 border border-emerald-500/20 hover:bg-emerald-500/20'
                            : 'bg-slate-500/10 text-slate-500 border border-slate-500/20 hover:bg-slate-500/20'
                        }`}
                      >
                        {signer.active ? 'Active' : 'Disabled'}
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div
          className={`px-5 py-3 border-t text-center text-xs ${
            isDarkTheme ? 'border-slate-800 bg-slate-900/60' : 'border-slate-100 bg-slate-50/80'
          }`}
        >
          <span className="text-amber-600 dark:text-amber-400 font-semibold">
            Simulation only - no real money transfer. Dual-Authorization payments require two independent cryptographic keys.
          </span>
        </div>
      </div>

      {/* ============================================================ */}
      {/* USER 1 BIOMETRIC VERIFICATION SHEET                           */}
      {/* ============================================================ */}
      {isVerifyingUser1 && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-in fade-in">
          <div
            className={`w-full max-w-md rounded-2xl border p-6 shadow-2xl space-y-4 ${
              isDarkTheme ? 'bg-slate-900 border-slate-700 text-slate-100' : 'bg-white border-slate-200 text-slate-800'
            }`}
          >
            <div className="text-center space-y-1">
              <div className="w-12 h-12 rounded-2xl bg-violet-500/10 border border-violet-500/30 flex items-center justify-center text-violet-600 dark:text-violet-400 mx-auto">
                <Fingerprint className="w-6 h-6 animate-pulse" />
              </div>
              <h3 className="text-base font-bold">User 1 Biometric Verification</h3>
              <p className="text-xs text-slate-500">
                Authorize the creation of this dual-approval payment of <strong>₹{parseFloat(amount).toLocaleString('en-IN')}</strong>.
              </p>
            </div>

            <div
              className={`p-4 rounded-xl border text-center space-y-2 cursor-pointer transition hover:border-violet-500 ${
                user1BioStep === 'DONE'
                  ? 'border-emerald-500 bg-emerald-500/10'
                  : isDarkTheme
                  ? 'bg-slate-800/60 border-slate-700'
                  : 'bg-slate-50 border-slate-200'
              }`}
              onClick={handleCompleteUser1Verification}
            >
              <Fingerprint className="w-10 h-10 mx-auto text-violet-500 animate-bounce" />
              <div className="text-xs font-bold text-violet-600 dark:text-violet-400">
                Click sensor to simulate Android Biometric scan
              </div>
              <p className="text-[11px] text-slate-400">
                Binds transaction payload with SHA-256 integrity hash.
              </p>
            </div>

            <div className="flex items-center gap-2 pt-2">
              <button
                type="button"
                onClick={() => setIsVerifyingUser1(false)}
                className="w-1/2 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 text-xs font-semibold text-slate-600 dark:text-slate-300"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleCompleteUser1Verification}
                className="w-1/2 py-2.5 rounded-xl bg-violet-600 text-white text-xs font-bold hover:bg-violet-700"
              >
                Confirm Biometric
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ============================================================ */}
      {/* USER 2 APPROVAL MODAL (Biometric + Final Exact Confirmation)  */}
      {/* ============================================================ */}
      {selectedTxForApproval && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-in fade-in">
          <div
            className={`w-full max-w-lg rounded-2xl border p-6 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto ${
              isDarkTheme ? 'bg-slate-900 border-slate-700 text-slate-100' : 'bg-white border-slate-200 text-slate-800'
            }`}
          >
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-600 dark:text-emerald-400">
                  <ShieldCheck className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold">Second Signer Authorization</h3>
                  <p className="text-xs text-slate-500">
                    Signing as <strong>{currentUser.fullName}</strong> ({currentUser.email})
                  </p>
                </div>
              </div>
              <button
                onClick={() => setSelectedTxForApproval(null)}
                className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {approvalError && (
              <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-700 dark:text-rose-400 text-xs font-semibold flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 shrink-0" />
                <span>{approvalError}</span>
              </div>
            )}

            {/* Exact Transaction Audit Card */}
            <div
              className={`p-4 rounded-xl border text-xs space-y-2 ${
                isDarkTheme ? 'bg-slate-800/40 border-slate-800' : 'bg-slate-50 border-slate-200'
              }`}
            >
              <div className="flex justify-between">
                <span className="text-slate-400">Transaction ID:</span>
                <span className="font-mono font-bold">{selectedTxForApproval.id}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Initiator (User 1):</span>
                <span className="font-bold">{selectedTxForApproval.initiatorName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Recipient:</span>
                <span className="font-bold">{selectedTxForApproval.recipient}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Amount:</span>
                <span className="font-bold font-mono text-sm text-emerald-600 dark:text-emerald-400">
                  {formatIndianCurrency(selectedTxForApproval.amount)}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Purpose:</span>
                <span>{selectedTxForApproval.note}</span>
              </div>
              <div className="flex justify-between text-[11px] pt-1 border-t border-slate-200 dark:border-slate-800">
                <span className="text-slate-400">Risk Assessment:</span>
                <span className="font-bold text-amber-600">{selectedTxForApproval.riskScore}/100</span>
              </div>
            </div>

            {/* Android Biometric Scan Trigger */}
            <div
              onClick={() => setUser2BioStatus('VERIFIED')}
              className={`p-4 rounded-xl border text-center space-y-2 cursor-pointer transition ${
                user2BioStatus === 'VERIFIED'
                  ? 'bg-emerald-500/10 border-emerald-500 text-emerald-600'
                  : 'bg-violet-500/5 border-violet-500/30 hover:border-violet-500'
              }`}
            >
              <Fingerprint
                className={`w-10 h-10 mx-auto ${
                  user2BioStatus === 'VERIFIED' ? 'text-emerald-500' : 'text-violet-500 animate-pulse'
                }`}
              />
              <div className="text-xs font-bold">
                {user2BioStatus === 'VERIFIED'
                  ? '✓ Android Biometric Authenticated'
                  : 'Touch Sensor: Authenticate Second Signer Biometrics'}
              </div>
              <div className="text-[10px] text-slate-400">
                Requires hardware-backed credential authentication to bind approval signature.
              </div>
            </div>

            {/* Explicit Confirmation Checkbox */}
            <label
              className={`p-3 rounded-xl border flex items-start gap-2.5 cursor-pointer text-xs ${
                isDarkTheme ? 'bg-slate-800/30 border-slate-700' : 'bg-slate-50 border-slate-200'
              }`}
            >
              <input
                type="checkbox"
                checked={approvalConfirmedCheck}
                onChange={(e) => setApprovalConfirmedCheck(e.target.checked)}
                className="rounded text-emerald-600 mt-0.5"
              />
              <span className="text-slate-700 dark:text-slate-200 font-semibold leading-relaxed">
                I approve this exact transaction of{' '}
                <strong className="text-emerald-600 dark:text-emerald-400">
                  {formatIndianCurrency(selectedTxForApproval.amount)}
                </strong>{' '}
                to <strong>{selectedTxForApproval.recipient}</strong>. I authorize the immediate release of funds from User 1's account.
              </span>
            </label>

            {/* Actions */}
            <div className="flex items-center gap-2 pt-2">
              <button
                type="button"
                onClick={() => setSelectedTxForApproval(null)}
                className="w-1/3 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 text-xs font-semibold text-slate-600 dark:text-slate-300"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={user2BioStatus !== 'VERIFIED' || !approvalConfirmedCheck}
                onClick={handleConfirmUser2Approval}
                className={`w-2/3 py-2.5 rounded-xl font-bold text-xs flex items-center justify-center gap-2 transition ${
                  user2BioStatus === 'VERIFIED' && approvalConfirmedCheck
                    ? 'bg-emerald-600 hover:bg-emerald-700 text-white shadow-sm'
                    : 'bg-slate-200 dark:bg-slate-800 text-slate-400 cursor-not-allowed'
                }`}
              >
                <ShieldCheck className="w-4 h-4" />
                Sign &amp; Release Funds
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ============================================================ */}
      {/* USER 2 REJECTION MODAL                                       */}
      {/* ============================================================ */}
      {selectedTxForRejection && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-in fade-in">
          <div
            className={`w-full max-w-md rounded-2xl border p-6 shadow-2xl space-y-4 ${
              isDarkTheme ? 'bg-slate-900 border-slate-700 text-slate-100' : 'bg-white border-slate-200 text-slate-800'
            }`}
          >
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-10 h-10 rounded-xl bg-rose-500/10 border border-rose-500/30 flex items-center justify-center text-rose-600 dark:text-rose-400">
                  <XCircle className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold">Reject Dual-Approval Payment</h3>
                  <p className="text-xs text-slate-500">Transaction ID: {selectedTxForRejection.id}</p>
                </div>
              </div>
              <button
                onClick={() => setSelectedTxForRejection(null)}
                className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs text-slate-600 dark:text-slate-300">
              Rejecting will mark the transaction as <strong>REJECTED BY SECOND SIGNER</strong>. No balance will be deducted
              from User 1.
            </p>

            <div>
              <label className="text-xs font-bold text-slate-600 dark:text-slate-300 mb-1 block">
                Reason for Rejection
              </label>
              <textarea
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                rows={3}
                placeholder="e.g. Unrecognized vendor invoice or budget ceiling exceeded"
                className={`w-full p-2.5 rounded-xl border text-xs focus:outline-none ${
                  isDarkTheme ? 'bg-slate-800 border-slate-700 text-slate-100' : 'bg-slate-50 border-slate-200'
                }`}
              />
            </div>

            <div className="flex items-center gap-2 pt-2">
              <button
                type="button"
                onClick={() => setSelectedTxForRejection(null)}
                className="w-1/2 py-2 rounded-xl border text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmRejection}
                className="w-1/2 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold"
              >
                Confirm Rejection
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
