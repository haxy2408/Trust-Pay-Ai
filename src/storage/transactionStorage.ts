import {
  AuditLogEntry,
  CoSigner,
  DualAuthAdminStats,
  DualAuthTransaction,
  InAppNotification,
  PaymentTransaction,
  QrAdminStats,
  QrPaymentRequest,
  UserAccount,
} from '../types';
import { generateSalt, hashPasswordWithSalt, computeBindingHash } from '../security/crypto';

const STORAGE_KEYS = {
  USERS: 'trustpay_users',
  ACTIVE_USER: 'trustpay_active_user',
  BALANCE_PREFIX: 'trustpay_balance_',
  BALANCE: 'trustpay_account_balance',
  TRANSACTIONS: 'trustpay_transactions',
  AUDIT_LOGS: 'trustpay_audit_logs',
  THEME_DARK: 'trustpay_is_dark_theme',
  QR_TOKENS: 'trustpay_p2p_qr_tokens',
  QR_ADMIN_STATS: 'trustpay_qr_admin_stats',
  DUAL_AUTH_TRANSACTIONS: 'trustpay_dual_auth_transactions',
  CO_SIGNERS: 'trustpay_co_signers',
  NOTIFICATIONS: 'trustpay_in_app_notifications',
  DUAL_AUTH_ADMIN_STATS: 'trustpay_dual_auth_admin_stats',
};

export const DEFAULT_DEMO_BALANCE = 100000.0;

/**
 * Pre-seed default authorised co-signers.
 */
function initializeDefaultCoSigners(): CoSigner[] {
  return [
    {
      id: 'cosigner_1',
      fullName: 'Priya Patel',
      email: 'priya@example.com',
      role: 'Chief Financial Officer (CFO)',
      active: true,
      addedAt: Date.now() - 7 * 86400000,
    },
    {
      id: 'cosigner_2',
      fullName: 'Vikram Mehta',
      email: 'vikram@example.com',
      role: 'Risk & Compliance Officer',
      active: true,
      addedAt: Date.now() - 5 * 86400000,
    },
    {
      id: 'cosigner_3',
      fullName: 'Ananya Rao',
      email: 'ananya@example.com',
      role: 'Senior Treasury Director',
      active: true,
      addedAt: Date.now() - 2 * 86400000,
    },
  ];
}

/**
 * Pre-seed default demo accounts so evaluators can test P2P between two users easily.
 */
function initializeDefaultUsers(): UserAccount[] {
  const salt1 = generateSalt();
  const passwordHash1 = hashPasswordWithSalt('password123', salt1);
  const user1: UserAccount = {
    fullName: 'Rahul Sharma',
    email: 'rahul@example.com',
    mobileNumber: '9876543210',
    passwordHash: passwordHash1,
    salt: salt1,
    registeredAt: Date.now() - 3600000,
  };

  const salt2 = generateSalt();
  const passwordHash2 = hashPasswordWithSalt('password123', salt2);
  const user2: UserAccount = {
    fullName: 'Priya Patel',
    email: 'priya@example.com',
    mobileNumber: '9876543211',
    passwordHash: passwordHash2,
    salt: salt2,
    registeredAt: Date.now() - 1800000,
  };

  return [user1, user2];
}

export const TransactionStorage = {
  getUsers(): UserAccount[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.USERS);
      if (!data) {
        const defaultUsers = initializeDefaultUsers();
        localStorage.setItem(STORAGE_KEYS.USERS, JSON.stringify(defaultUsers));
        return defaultUsers;
      }
      return JSON.parse(data);
    } catch {
      return [];
    }
  },

  saveUser(user: UserAccount): void {
    const users = this.getUsers().filter((u) => u.email !== user.email && u.mobileNumber !== user.mobileNumber);
    users.push(user);
    localStorage.setItem(STORAGE_KEYS.USERS, JSON.stringify(users));
  },

  getActiveUser(): UserAccount | null {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.ACTIVE_USER);
      if (data) return JSON.parse(data);
      const users = this.getUsers();
      if (users.length > 0) {
        this.setActiveUser(users[0]);
        return users[0];
      }
      return null;
    } catch {
      return null;
    }
  },

  setActiveUser(user: UserAccount | null): void {
    if (user) {
      localStorage.setItem(STORAGE_KEYS.ACTIVE_USER, JSON.stringify(user));
    } else {
      localStorage.removeItem(STORAGE_KEYS.ACTIVE_USER);
    }
  },

  getUserBalance(email?: string): number {
    const active = email || this.getActiveUser()?.email || 'default';
    try {
      const data = localStorage.getItem(`${STORAGE_KEYS.BALANCE_PREFIX}${active}`);
      if (data !== null) {
        const val = parseFloat(data);
        if (!isNaN(val)) return val;
      }
      // Fallback to legacy balance key if Rahul
      if (active === 'rahul@example.com') {
        const legacy = localStorage.getItem(STORAGE_KEYS.BALANCE);
        if (legacy) {
          const val = parseFloat(legacy);
          if (!isNaN(val)) return val;
        }
      }
      const initial = active === 'priya@example.com' ? 65000.0 : DEFAULT_DEMO_BALANCE;
      this.saveUserBalance(initial, active);
      return initial;
    } catch {
      return DEFAULT_DEMO_BALANCE;
    }
  },

  saveUserBalance(amount: number, email?: string): void {
    const active = email || this.getActiveUser()?.email || 'default';
    localStorage.setItem(`${STORAGE_KEYS.BALANCE_PREFIX}${active}`, amount.toFixed(2));
    if (active === 'rahul@example.com' || !this.getActiveUser()) {
      localStorage.setItem(STORAGE_KEYS.BALANCE, amount.toFixed(2));
    }
  },

  getBalance(): number {
    return this.getUserBalance();
  },

  saveBalance(amount: number): void {
    this.saveUserBalance(amount);
  },

  resetDemoBalance(): number {
    const active = this.getActiveUser()?.email;
    const initial = active === 'priya@example.com' ? 65000.0 : DEFAULT_DEMO_BALANCE;
    this.saveUserBalance(initial, active);
    return initial;
  },

  getTransactions(): PaymentTransaction[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.TRANSACTIONS);
      if (!data) return [];
      return JSON.parse(data);
    } catch {
      return [];
    }
  },

  saveTransactions(transactions: PaymentTransaction[]): void {
    localStorage.setItem(STORAGE_KEYS.TRANSACTIONS, JSON.stringify(transactions));
  },

  appendTransaction(tx: PaymentTransaction): void {
    const list = this.getTransactions();
    list.unshift(tx);
    this.saveTransactions(list);
  },

  clearTransactions(): void {
    localStorage.removeItem(STORAGE_KEYS.TRANSACTIONS);
  },

  getAuditLogs(): AuditLogEntry[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.AUDIT_LOGS);
      if (!data) return [];
      return JSON.parse(data);
    } catch {
      return [];
    }
  },

  saveAuditLogs(logs: AuditLogEntry[]): void {
    localStorage.setItem(STORAGE_KEYS.AUDIT_LOGS, JSON.stringify(logs.slice(0, 100)));
  },

  appendAuditLog(entry: AuditLogEntry): void {
    const logs = this.getAuditLogs();
    logs.unshift(entry);
    this.saveAuditLogs(logs);
  },

  clearAuditLogs(): void {
    localStorage.removeItem(STORAGE_KEYS.AUDIT_LOGS);
  },

  // QR Tokens
  getQrTokens(): QrPaymentRequest[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.QR_TOKENS);
      if (!data) return [];
      return JSON.parse(data);
    } catch {
      return [];
    }
  },

  saveQrTokens(tokens: QrPaymentRequest[]): void {
    localStorage.setItem(STORAGE_KEYS.QR_TOKENS, JSON.stringify(tokens.slice(0, 100)));
  },

  saveQrToken(token: QrPaymentRequest): void {
    const tokens = this.getQrTokens().filter((t) => t.tokenId !== token.tokenId);
    tokens.unshift(token);
    this.saveQrTokens(tokens);
    this.recordQrSecurityEvent('generated');
  },

  updateQrToken(tokenId: string, updates: Partial<QrPaymentRequest>): void {
    const tokens = this.getQrTokens();
    const index = tokens.findIndex((t) => t.tokenId === tokenId);
    if (index !== -1) {
      tokens[index] = { ...tokens[index], ...updates };
      this.saveQrTokens(tokens);
    }
  },

  getQrTokenById(tokenId: string): QrPaymentRequest | null {
    const tokens = this.getQrTokens();
    return tokens.find((t) => t.tokenId === tokenId) || null;
  },

  // Admin stats
  getQrAdminStats(): QrAdminStats {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.QR_ADMIN_STATS);
      if (data) return JSON.parse(data);
    } catch {}

    const tokens = this.getQrTokens();
    const now = Date.now();
    let activeCount = 0;
    let expiredCount = 0;
    let usedCount = 0;

    for (const t of tokens) {
      if (t.status === 'USED') usedCount++;
      else if (t.status === 'EXPIRED' || (t.status === 'ACTIVE' && now > t.expiresAt)) expiredCount++;
      else if (t.status === 'ACTIVE') activeCount++;
    }

    return {
      totalGenerated: tokens.length,
      activeCount,
      expiredCount,
      usedCount,
      replayBlockedCount: 0,
      tamperBlockedCount: 0,
      totalTransferredAmount: 0,
    };
  },

  recordQrSecurityEvent(event: 'replay_blocked' | 'tamper_blocked' | 'generated' | 'used' | 'expired', amount?: number): void {
    const stats = this.getQrAdminStats();
    if (event === 'replay_blocked') stats.replayBlockedCount++;
    if (event === 'tamper_blocked') stats.tamperBlockedCount++;
    if (event === 'generated') stats.totalGenerated++;
    if (event === 'used') {
      stats.usedCount++;
      if (amount) stats.totalTransferredAmount += amount;
    }
    if (event === 'expired') stats.expiredCount++;
    localStorage.setItem(STORAGE_KEYS.QR_ADMIN_STATS, JSON.stringify(stats));
  },

  isDarkTheme(): boolean {
    try {
      const val = localStorage.getItem(STORAGE_KEYS.THEME_DARK);
      if (val !== null) return val === 'true';
      return false; // Polished light theme default as requested
    } catch {
      return false;
    }
  },

  saveDarkTheme(isDark: boolean): void {
    localStorage.setItem(STORAGE_KEYS.THEME_DARK, isDark ? 'true' : 'false');
  },

  // ==========================================
  // DUAL AUTHORIZATION STORAGE & SECURITY ENGINE
  // ==========================================

  getCoSigners(): CoSigner[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.CO_SIGNERS);
      if (!data) {
        const defaultList = initializeDefaultCoSigners();
        localStorage.setItem(STORAGE_KEYS.CO_SIGNERS, JSON.stringify(defaultList));
        return defaultList;
      }
      return JSON.parse(data);
    } catch {
      return initializeDefaultCoSigners();
    }
  },

  saveCoSigners(cosigners: CoSigner[]): void {
    localStorage.setItem(STORAGE_KEYS.CO_SIGNERS, JSON.stringify(cosigners));
  },

  addCoSigner(cosigner: CoSigner): void {
    const list = this.getCoSigners().filter((c) => c.email.toLowerCase() !== cosigner.email.toLowerCase());
    list.push(cosigner);
    this.saveCoSigners(list);
  },

  toggleCoSignerActive(id: string): void {
    const list = this.getCoSigners();
    const target = list.find((c) => c.id === id);
    if (target) {
      target.active = !target.active;
      this.saveCoSigners(list);
    }
  },

  getDualAuthTransactions(): DualAuthTransaction[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.DUAL_AUTH_TRANSACTIONS);
      if (!data) return [];
      const txs: DualAuthTransaction[] = JSON.parse(data);
      const now = Date.now();
      let updated = false;

      // Auto-expire transactions past 5 minutes TTL
      for (const tx of txs) {
        if (tx.status === 'PENDING SECOND AUTHORIZATION' && now > tx.expiresAt) {
          tx.status = 'EXPIRED';
          tx.rejectionReason = 'Pending second approval window (5 minutes) expired.';
          updated = true;
        }
      }

      if (updated) {
        this.saveDualAuthTransactions(txs);
      }

      return txs;
    } catch {
      return [];
    }
  },

  saveDualAuthTransactions(txs: DualAuthTransaction[]): void {
    localStorage.setItem(STORAGE_KEYS.DUAL_AUTH_TRANSACTIONS, JSON.stringify(txs.slice(0, 100)));
  },

  createDualAuthPayment(params: {
    initiator: UserAccount;
    coSigner: CoSigner;
    recipient: string;
    amount: number;
    note: string;
    riskScore: number;
    signals: string[];
    bindingHash: string;
  }): DualAuthTransaction {
    const now = Date.now();
    const expiresAt = now + 5 * 60 * 1000; // Exactly 5 minutes TTL
    const nonce = `tp_nonce_${Math.random().toString(36).substring(2, 10)}_${Date.now()}`;
    const txId = `TP_DUAL_${now}_${Math.floor(1000 + Math.random() * 9000)}`;

    const newTx: DualAuthTransaction = {
      id: txId,
      initiatorId: params.initiator.email,
      initiatorName: params.initiator.fullName,
      coSignerId: params.coSigner.email,
      coSignerName: params.coSigner.fullName,
      recipient: params.recipient,
      amount: params.amount,
      currency: 'INR',
      note: params.note || 'Dual-Authorization Payment',
      status: 'PENDING SECOND AUTHORIZATION',
      createdAt: now,
      expiresAt,
      riskScore: params.riskScore,
      signals: params.signals,
      nonce,
      firstApprovalSignature: params.bindingHash,
      balanceDeducted: false,
      label: 'Dual-Authorization Transaction',
    };

    const txs = this.getDualAuthTransactions();
    txs.unshift(newTx);
    this.saveDualAuthTransactions(txs);

    // Send in-app notification to the selected co-signer
    this.addInAppNotification({
      id: `notif_${now}_${Math.random().toString(36).substring(2, 6)}`,
      targetUserEmail: params.coSigner.email,
      title: 'Action Required: Dual-Approval Request',
      message: `${params.initiator.fullName} requested your co-approval to transfer ₹${params.amount.toLocaleString(
        'en-IN'
      )} to ${params.recipient}. Expires in 5 minutes.`,
      transactionId: txId,
      timestamp: now,
      read: false,
      type: 'DUAL_AUTH_REQUEST',
    });

    // Record audit log
    this.appendAuditLog({
      id: `AUDIT_${now}_${Math.random().toString(36).substring(2, 6)}`,
      timestamp: now,
      title: 'Dual-Auth Initiated (1/2 Signatures)',
      details: `${params.initiator.fullName} approved ₹${params.amount} to ${params.recipient}. Awaiting co-signer: ${params.coSigner.fullName}. Balance NOT deducted yet.`,
      status: 'INFO',
      hash: params.bindingHash,
    });

    return newTx;
  },

  approveDualAuthPayment(params: {
    transactionId: string;
    coSignerUser: UserAccount;
    secondApprovalSignature: string;
  }): { success: boolean; error?: string; transaction?: DualAuthTransaction } {
    const txs = this.getDualAuthTransactions();
    const tx = txs.find((t) => t.id === params.transactionId);

    if (!tx) {
      return { success: false, error: 'Transaction not found.' };
    }

    const now = Date.now();

    // Security Rule 1: User 1 must never approve their own transaction as User 2
    if (params.coSignerUser.email.toLowerCase() === tx.initiatorId.toLowerCase()) {
      const err = 'Security Violation: User 1 cannot approve their own transaction as User 2.';
      this.appendAuditLog({
        id: `AUDIT_${now}_ERR`,
        timestamp: now,
        title: 'Dual-Auth Self-Approval Blocked',
        details: err,
        status: 'DANGER',
      });
      return { success: false, error: err };
    }

    // Security Rule 2: Only authorised co-signers can approve
    const coSigners = this.getCoSigners();
    const isAuthorized = coSigners.some(
      (c) => c.email.toLowerCase() === params.coSignerUser.email.toLowerCase() && c.active
    );
    if (!isAuthorized) {
      const err = 'Authorization Denied: User is not an active authorized co-signer.';
      this.appendAuditLog({
        id: `AUDIT_${now}_ERR`,
        timestamp: now,
        title: 'Unauthorized Co-Signer Blocked',
        details: err,
        status: 'DANGER',
      });
      return { success: false, error: err };
    }

    // Security Rule 3: 5-minute TTL check
    if (now > tx.expiresAt || tx.status === 'EXPIRED') {
      tx.status = 'EXPIRED';
      tx.rejectionReason = 'Pending approval expired after 5 minutes.';
      this.saveDualAuthTransactions(txs);
      return { success: false, error: 'Transaction expired. 5-minute approval window has elapsed.' };
    }

    // Security Rule 4: Must be currently pending
    if (tx.status !== 'PENDING SECOND AUTHORIZATION') {
      return { success: false, error: `Transaction is already ${tx.status}.` };
    }

    // Balance check on Initiator (User 1)
    const initiatorBalance = this.getUserBalance(tx.initiatorId);
    if (initiatorBalance < tx.amount) {
      tx.status = 'BLOCKED';
      tx.rejectionReason = 'Initiator account has insufficient balance at final settlement.';
      this.saveDualAuthTransactions(txs);
      return { success: false, error: 'Initiator has insufficient account balance.' };
    }

    // Deduct balance ONCE from User 1 (Initiator)
    const newInitiatorBalance = initiatorBalance - tx.amount;
    this.saveUserBalance(newInitiatorBalance, tx.initiatorId);

    // Credit recipient if recipient is an existing local demo user
    const recipientUser = this.getUsers().find((u) => u.email.toLowerCase() === tx.recipient.toLowerCase());
    if (recipientUser) {
      const currentRecBal = this.getUserBalance(recipientUser.email);
      this.saveUserBalance(currentRecBal + tx.amount, recipientUser.email);
    }

    // Update Transaction
    tx.status = 'APPROVED BY TWO SIGNERS';
    tx.secondApprovalSignature = params.secondApprovalSignature;
    tx.secondApprovedAt = now;
    tx.approvedByUserId = params.coSignerUser.email;
    tx.approvedByUserName = params.coSignerUser.fullName;
    tx.balanceDeducted = true;

    this.saveDualAuthTransactions(txs);

    // Notify Initiator (User 1) that transfer was completed
    this.addInAppNotification({
      id: `notif_${now}_${Math.random().toString(36).substring(2, 6)}`,
      targetUserEmail: tx.initiatorId,
      title: 'Dual-Approval Payment Completed!',
      message: `${params.coSignerUser.fullName} approved the transfer of ₹${tx.amount.toLocaleString(
        'en-IN'
      )} to ${tx.recipient}. Funds deducted.`,
      transactionId: tx.id,
      timestamp: now,
      read: false,
      type: 'DUAL_AUTH_APPROVED',
    });

    // Record Immutable Audit Log
    this.appendAuditLog({
      id: `AUDIT_${now}_${Math.random().toString(36).substring(2, 6)}`,
      timestamp: now,
      title: 'Dual-Authorization Approved (2 Signers)',
      details: `₹${tx.amount} to ${tx.recipient} approved by Co-Signer ${params.coSignerUser.fullName}. Balance deducted. Second signature verified.`,
      status: 'SUCCESS',
      hash: params.secondApprovalSignature,
    });

    return { success: true, transaction: tx };
  },

  rejectDualAuthPayment(params: {
    transactionId: string;
    coSignerUser: UserAccount;
    reason: string;
  }): { success: boolean; error?: string; transaction?: DualAuthTransaction } {
    const txs = this.getDualAuthTransactions();
    const tx = txs.find((t) => t.id === params.transactionId);

    if (!tx) {
      return { success: false, error: 'Transaction not found.' };
    }

    const now = Date.now();
    tx.status = 'REJECTED BY SECOND SIGNER';
    tx.rejectionReason = params.reason || 'Rejected by authorized co-signer.';
    this.saveDualAuthTransactions(txs);

    // Notify Initiator
    this.addInAppNotification({
      id: `notif_${now}_${Math.random().toString(36).substring(2, 6)}`,
      targetUserEmail: tx.initiatorId,
      title: 'Dual-Approval Payment Rejected',
      message: `${params.coSignerUser.fullName} rejected the transfer of ₹${tx.amount.toLocaleString(
        'en-IN'
      )}. Reason: ${params.reason}. No balance deducted.`,
      transactionId: tx.id,
      timestamp: now,
      read: false,
      type: 'DUAL_AUTH_REJECTED',
    });

    // Record Audit Log
    this.appendAuditLog({
      id: `AUDIT_${now}_REJ`,
      timestamp: now,
      title: 'Dual-Authorization Payment Rejected',
      details: `Transfer of ₹${tx.amount} to ${tx.recipient} rejected by ${params.coSignerUser.fullName}. Reason: ${params.reason}. Balance untouched.`,
      status: 'WARNING',
    });

    return { success: true, transaction: tx };
  },

  getDualAuthAdminStats(): DualAuthAdminStats {
    const txs = this.getDualAuthTransactions();
    let pendingCount = 0;
    let approvedCount = 0;
    let rejectedCount = 0;
    let expiredCount = 0;
    let tamperBlockedCount = 0;
    let totalApprovedAmount = 0;

    for (const tx of txs) {
      if (tx.status === 'PENDING SECOND AUTHORIZATION') pendingCount++;
      else if (tx.status === 'APPROVED BY TWO SIGNERS') {
        approvedCount++;
        totalApprovedAmount += tx.amount;
      } else if (tx.status === 'REJECTED BY SECOND SIGNER') rejectedCount++;
      else if (tx.status === 'EXPIRED') expiredCount++;
      else if (tx.status === 'TAMPER_DETECTED') tamperBlockedCount++;
    }

    return {
      totalRequests: txs.length,
      pendingCount,
      approvedCount,
      rejectedCount,
      expiredCount,
      tamperBlockedCount,
      totalApprovedAmount,
    };
  },

  // In-App Notifications
  getInAppNotifications(userEmail?: string): InAppNotification[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.NOTIFICATIONS);
      if (!data) return [];
      const list: InAppNotification[] = JSON.parse(data);
      if (userEmail) {
        return list.filter((n) => n.targetUserEmail.toLowerCase() === userEmail.toLowerCase());
      }
      return list;
    } catch {
      return [];
    }
  },

  addInAppNotification(notification: InAppNotification): void {
    const list = this.getInAppNotifications();
    list.unshift(notification);
    localStorage.setItem(STORAGE_KEYS.NOTIFICATIONS, JSON.stringify(list.slice(0, 50)));
  },

  markNotificationRead(id: string): void {
    const list = this.getInAppNotifications();
    const item = list.find((n) => n.id === id);
    if (item) {
      item.read = true;
      localStorage.setItem(STORAGE_KEYS.NOTIFICATIONS, JSON.stringify(list));
    }
  },
};

