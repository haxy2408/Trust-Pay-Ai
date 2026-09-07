export type AuthScreen = 'REGISTER' | 'LOGIN' | 'DASHBOARD';

export interface UserAccount {
  fullName: string;
  email: string;
  mobileNumber: string;
  passwordHash: string;
  salt: string;
  registeredAt: number;
}

export interface LoginFormState {
  identifier: string; // Email or 10-digit mobile
  password: string;
  isPasswordVisible: boolean;
  identifierError: string | null;
  passwordError: string | null;
  errorMessage: string | null;
  successMessage: string | null;
}

export interface RegisterFormState {
  fullName: string;
  email: string;
  mobileNumber: string;
  password: string;
  confirmPassword: string;
  isPasswordVisible: boolean;
  isConfirmPasswordVisible: boolean;
  fullNameError: string | null;
  emailError: string | null;
  mobileNumberError: string | null;
  passwordError: string | null;
  confirmPasswordError: string | null;
  errorMessage: string | null;
}

export interface PaymentTransaction {
  id: string;
  recipient: string;
  amount: number;
  currency: string;
  timestamp: number;
  status: string; // "APPROVED", "BLOCKED", "TAMPER_DETECTED", "PENDING"
  bindingHash: string;
  riskScore: number;
  balanceAfter?: number;
}

export interface RiskAnalysisResult {
  score: number;
  signals: string[];
  requiresBiometric: boolean;
  requiresOtp: boolean;
  isVelocityAnomaly: boolean;
  boundHash: string;
  aiInsights?: string;
}

export interface AuthenticatedTransactionSnapshot {
  transactionId: string;
  recipient: string;
  amount: number;
  currency: string;
  bindingHash: string;
  timestamp: number;
}

export type FinalDecisionStatus = 'NONE' | 'APPROVED' | 'TAMPER_DETECTED' | 'BLOCKED';

export type VerificationStep = 'NONE' | 'BIOMETRIC' | 'CHALLENGE' | 'DEMO_OTP' | 'COMPLETED';

export type ChallengeType = 'AMOUNT' | 'RECIPIENT_DOMAIN';

export interface ChallengeData {
  type: ChallengeType;
  question: string;
  expectedAnswer: string;
}

export interface VerificationState {
  transactionId: string;
  recipient: string;
  amount: number;
  currentStep: VerificationStep;
  isVelocityAnomaly: boolean;
  designatedFinger?: string; // Random finger challenge for high-frequency transactions
  challengeData?: ChallengeData;
  challengeInput: string;
  challengeError: string | null;
  demoOtp?: string;
  otpInput: string;
  otpError: string | null;
}

export type AuditStatus = 'SUCCESS' | 'WARNING' | 'DANGER' | 'INFO';

export interface AuditLogEntry {
  id: string;
  timestamp: number;
  title: string;
  details: string;
  status: AuditStatus;
  hash?: string;
}

export interface TrustPayUiState {
  currentScreen: AuthScreen;
  currentUser: UserAccount | null;
  isDarkTheme: boolean;

  // Account balance
  accountBalance: number;

  // Inputs
  recipientInput: string;
  amountInput: string;

  // Simulated risk toggles
  untrustedDevice: boolean;
  newRecipient: boolean;
  unusualContext: boolean;

  // Analysis & Status
  currentAnalysis: RiskAnalysisResult | null;
  currentRiskScore: number | null;
  finalDecision: FinalDecisionStatus;
  decisionMessage: string;
  signals: string[];

  // Verification & dialogs
  verificationState: VerificationState | null;

  // History & Ledger
  allTransactions: PaymentTransaction[];
  rollingWindowTransactions: PaymentTransaction[];
  rollingWindowTotalAmount: number;
  auditLogs: AuditLogEntry[];

  // Auth Forms
  loginForm: LoginFormState;
  registerForm: RegisterFormState;
}

export type QrTokenStatus = 'ACTIVE' | 'EXPIRED' | 'USED' | 'REFRESHING' | 'CANCELLED';

export interface QrPaymentRequest {
  tokenId: string;
  receiverId: string;
  receiverName: string;
  amount: number | null;
  note: string | null;
  nonce: string;
  createdAt: number;
  expiresAt: number;
  status: QrTokenStatus;
  signature: string;
  payloadUri: string;
  usedBySenderId?: string;
  usedBySenderName?: string;
  usedAt?: number;
  transactionId?: string;
}

export interface QrValidationResult {
  valid: boolean;
  error?: string;
  errorCode?: 'EXPIRED' | 'ALREADY_USED' | 'TAMPERED' | 'INVALID_FORMAT' | 'SELF_PAYMENT' | 'RECEIVER_NOT_FOUND';
  request?: QrPaymentRequest;
  remainingSeconds?: number;
}

export interface QrAdminStats {
  totalGenerated: number;
  activeCount: number;
  expiredCount: number;
  usedCount: number;
  replayBlockedCount: number;
  tamperBlockedCount: number;
  totalTransferredAmount: number;
}

export type DualAuthStatus =
  | 'PENDING SECOND AUTHORIZATION'
  | 'APPROVED BY TWO SIGNERS'
  | 'REJECTED BY SECOND SIGNER'
  | 'EXPIRED'
  | 'TAMPER_DETECTED'
  | 'BLOCKED';

export interface CoSigner {
  id: string;
  fullName: string;
  email: string;
  role: string;
  active: boolean;
  addedAt: number;
}

export interface DualAuthTransaction {
  id: string; // e.g. "TP_DUAL_17257..."
  initiatorId: string; // User 1 email
  initiatorName: string;
  coSignerId: string; // Selected User 2 email
  coSignerName: string;
  recipient: string;
  amount: number;
  currency: string;
  note: string;
  status: DualAuthStatus;
  createdAt: number;
  expiresAt: number; // 5 minutes (300 seconds)
  riskScore: number;
  signals: string[];
  nonce: string;
  firstApprovalSignature: string;
  secondApprovalSignature?: string;
  secondApprovedAt?: number;
  approvedByUserId?: string;
  approvedByUserName?: string;
  rejectionReason?: string;
  balanceDeducted: boolean;
  label: 'Dual-Authorization Transaction';
}

export interface DualAuthAdminStats {
  totalRequests: number;
  pendingCount: number;
  approvedCount: number;
  rejectedCount: number;
  expiredCount: number;
  tamperBlockedCount: number;
  totalApprovedAmount: number;
}

export interface InAppNotification {
  id: string;
  targetUserEmail: string;
  title: string;
  message: string;
  transactionId: string;
  timestamp: number;
  read: boolean;
  type: 'DUAL_AUTH_REQUEST' | 'DUAL_AUTH_APPROVED' | 'DUAL_AUTH_REJECTED';
}

// ==========================================
// LOCATION & DISTANCE CLASSIFICATION TYPES
// ==========================================

export type DistanceCategory =
  | 'Usual distance'
  | 'Medium distance'
  | 'Far distance'
  | 'High distance';

export type GeolocationStatus =
  | 'IDLE'
  | 'PROMPTING'
  | 'DETECTING'
  | 'LOCATED'
  | 'DENIED'
  | 'UNAVAILABLE'
  | 'TIMEOUT'
  | 'UNSUPPORTED';

export interface GeoCoordinates {
  latitude: number;
  longitude: number;
  accuracy?: number;
  altitude?: number | null;
  timestamp?: number;
}

export interface TargetLocation {
  id: string;
  name: string;
  category?: string;
  address?: string;
  latitude: number;
  longitude: number;
  icon?: string;
  description?: string;
}

export interface ClassifiedLocation extends TargetLocation {
  distanceKm: number;
  formattedDistance: string; // e.g. "5 km", "14.2 km"
  displayLabel: string; // e.g. "5 km — Usual distance"
  categoryClassification: DistanceCategory;
  colorIndicator: 'GREEN' | 'YELLOW' | 'ORANGE' | 'RED';
  hexColor: string;
  bgClass: string;
  textClass: string;
  borderClass: string;
}

