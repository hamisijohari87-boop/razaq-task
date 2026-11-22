import {
  getAuth,
  GoogleAuthProvider,
  onAuthStateChanged,
  signInWithPopup,
  signOut
} from "https://www.gstatic.com/firebasejs/11.4.0/firebase-auth.js";
import {
  collection,
  doc,
  getDoc,
  getDocs,
  getFirestore,
  limit,
  onSnapshot,
  query,
  runTransaction,
  serverTimestamp,
  updateDoc,
  where,
  writeBatch
} from "https://www.gstatic.com/firebasejs/11.4.0/firebase-firestore.js";
import { ADMIN_EMAIL, FIREBASE_CONFIG, FIRESTORE_DATABASE_ID } from "./firebase-config.js";
import { DEFAULT_LANGUAGE, LANGUAGES, resolveLanguage, t } from "./i18n.js";
 LANGUAGES, resolveLanguage, t } from "./i18n.js";

const MIN_LIMIT_TZS = 1_000;
const MAX_LIMIT_TZS = 1_000_000;
const COMMISSION_PCT = 15;
const BUSINESS_ACCOUNT_ID = "razak_main";
const STORAGE_LANG_KEY = "razak_lang";
const STORAGE_ONBOARDING_KEY = "razak_onboarding_done";
const STORAGE_THEME_KEY = "razak_theme";

const PLATFORMS = [
  { id: "TIKTOK", label: "TikTok" },
  { id: "YOUTUBE", label: "YouTube" },
  { id: "INSTAGRAM", label: "Instagram" },
  { id: "FACEBOOK", label: "Facebook" },
  { id: "X_TWITTER", label: "X (Twitter)" }
];

const ACTIONS = [
  { id: "FOLLOW", labelKey: "actionFollow", defaultTzs: 350 },
  { id: "LIKE", labelKey: "actionLike", defaultTzs: 250 },
  { id: "COMMENT", labelKey: "actionComment", defaultTzs: 500 },
  { id: "SUBSCRIBE", labelKey: "actionSubscribe", defaultTzs: 1000 },
  { id: "SHARE_REPOST", labelKey: "actionShare", defaultTzs: 450 }
ge, t } from "./i18n.js";


const WITHDRAWAL_METHODS = [
  { id: "MPESA_VODACOM", label: "M-Pesa", hint: "074X / 075X / 076X XXX XXX" },
  { id: "AIRTEL_MONEY", label: "Airtel Money", hint: "078X / 068X / 069X XXX XXX" },
  { id: "TIGOPESA_MIXX", label: "Mixx by Yas", hint: "071X / 065X / 067X XXX XXX" },
  { id: "HALOPESA", label: "HaloPesa", hint: "062X XXX XXX" }
];

const BUSINESS_PROVIDERS = [
  { id: "SELCOM_BUSINESS_TZ", label: "Selcom Pay Business TZ" },
  { id: "AZAMPAY_BUSINESS_TZ", label: "AzamPay Business TZ" },
  { id: "CRDB_CORPORATE_TZ", label: "CRDB Corporate / SimBanking" },
  { id: "NMB_BUSINESS_TZ", label: "NMB Business Lipa" }
];

const app = initializeApp(FIREBASE_CONFIG);
const auth = getAuth(app);
const db = getFirestore(app, FIRESTORE_DATABASE_ID);

const savedLang = localStorage.getItem(STORAGE_LANG_KEY);
const initialLang = savedLang ? resolveLanguage(savedLang).code : DEFAULT_LANGUAGE;
if (!savedLang) {
  localStorage.setItem(STORAGE_LANG_KEY, initialLang);
}

const state = {
  lang: initialLang,
  onboardingDone: localStorage.getItem(STORAGE_ONBOARDING_KEY) === "1",
  authUser: null,
  profile: null,
  tab: "marketplace",
  adminSubTab: "overview",
  platformFilter: "ALL",
  searchQuery: "",
  tasks: [],
  myCampaigns: [],
  mySubmissions: [],
  transactions: [],
  withdrawals: [],
  adminUsers: [],
  adminSubmissions: [],
  adminWithdrawals: [],
  adminCampaigns: [],
  adminCommissions: [],
  businessAccount: null,
  businessLedger: [],
  unsubscribers: [],
  selectedPlatform: "TIKTOK",
  selectedAction: "FOLLOW",
  selectedProvider: "MPESA_VODACOM",
  selectedBizProvider: "SELCOM_BUSINESS_TZ"
};

function tr(key, ...args) {
  return t(state.lang, key, ...args);
}

function applyDocumentLocale() {
  const langMeta = resolveLanguage(state.lang);
  document.documentElement.lang = langMeta.code;
  document.documentElement.dir = langMeta.rtl ? "rtl" : "ltr";
  document.title = tr("docTitle");
}

async function changeLanguage(langCode, persistToFirestore = true, showNotice = true) {
  const resolved = resolveLanguage(langCode);
  state.lang = resolved.code;
  localStorage.setItem(STORAGE_LANG_KEY, resolved.code);
  applyDocumentLocale();
  renderApp();

  if (persistToFirestore && state.authUser) {
    try {
      await updateDoc(doc(db, "users", state.authUser.uid), {
        preferredLanguage: resolved.code,
        updatedAt: serverTimestamp()
      });
    } catch (_) {
      // Offline / transient network errors shouldn't block immediate UI switching
    }
  }
  if (showNotice) {
    showToast(tr("langChangedToast", resolved.nativeName));
  }
}

function esc(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function formatTzs(amount) {
  const num = Number(amount || 0);
  return `TZS ${num.toLocaleString("en-US")}`;
}

function refCode(prefix) {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let code = "";
  for (let i = 0; i < 6; i++) {
    code += chars[Math.floor(Math.random() * chars.length)];
  }
  return `${prefix}-${code}`;
}

function computeSplit(gross) {
  const safe = Math.max(0, Math.floor(Number(gross) || 0));
  const commission = Math.floor((safe * COMMISSION_PCT) / 100);
  const worker = Math.max(0, safe - commission);
  return { commission, worker };
}

function statusLabel(status) {
  switch (String(status || "").toUpperCase()) {
    case "PENDING": return tr("statusPending");
    case "PENDING_REVIEW": return tr("statusPendingReview");
    case "ACTIVE": return tr("statusActive");
    case "PAUSED": return tr("statusPaused");
    case "COMPLETED": return tr("statusCompleted");
    case "APPROVED": return tr("statusApproved");
    case "REJECTED": return tr("statusRejected");
    default: return String(status || "");
  }
}

function txTypeLabel(type) {
  switch (String(type || "").toUpperCase()) {
    case "TASK_REWARD": return tr("txTaskReward");
    case "DEPOSIT_TOPUP": return tr("txDepositTopUp");
    case "CAMPAIGN_FUND": return tr("txCampaignFund");
    case "PLATFORM_COMMISSION": return tr("txPlatformCommission");
    case "WITHDRAWAL": return tr("txWithdrawal");
    case "WITHDRAWAL_REFUND": return tr("txWithdrawalRefund");
    default: return String(type || "");
  }
}

function actionLabel(actionId) {
  const found = ACTIONS.find((a) => a.id === actionId);
  return found ? tr(found.labelKey) : String(actionId || "");
}

function normalizeTzMobile(raw) {
  const stripped = String(raw || "").trim().replace(/[\s\-()]/g, "");
  let normalized = stripped;
  if (stripped.startsWith("+255") && stripped.length === 13) {
    normalized = "0" + stripped.slice(4);
  } else if (stripped.startsWith("255") && stripped.length === 12) {
    normalized = "0" + stripped.slice(3);
  }
  if (!/^0[67][0-9]{8}$/.test(normalized)) {
    throw new Error(tr("errValidTzMobile"));
  }
  return normalized;
}

function isAdminUser() {
  if (!state.authUser) return false;
  const verifiedAdminEmail =
    state.authUser.emailVerified &&
    String(state.authUser.email || "").toLowerCase() === ADMIN_EMAIL.toLowerCase();
  return verifiedAdminEmail || state.profile?.role === "ADMIN";
}

function showToast(message) {
  const container = document.getElementById("toast-container");
  const el = document.createElement("div");
  el.className = "toast";
  el.textContent = message;
  container.appendChild(el);
  setTimeout(() => el.remove(), 4000);
}

function toggleTheme() {
  const root = document.documentElement;
  const next = root.getAttribute("data-theme") === "dark" ? "light" : "dark";
  root.setAttribute("data-theme", next);
  localStorage.setItem(STORAGE_THEME_KEY, next);
}

(function initSavedThemeAndLocale() {
  const savedTheme = localStorage.getItem(STORAGE_THEME_KEY);
  if (savedTheme === "dark" || savedTheme === "light") {
    document.documentElement.setAttribute("data-theme", savedTheme);
  }
  applyDocumentLocale();
})();

async function ensureUserProfile(firebaseUser) {
  const userRef = doc(db, "users", firebaseUser.uid);
  const snap = await getDoc(userRef);
  if (snap.exists()) return;

  const email = (firebaseUser.email || `${firebaseUser.uid}@razaktask.co.tz`).slice(0, 120);
  const fullName = (firebaseUser.displayName || email.split("@")[0]).trim().slice(0, 80);
  const username = email.split("@")[0].replace(/[^a-zA-Z0-9_]/g, "_").slice(0, 30) || `member_${firebaseUser.uid.slice(0, 6)}`;
  const isBootstrappedAdmin =
    firebaseUser.emailVerified && email.toLowerCase() === ADMIN_EMAIL.toLowerCase();

  const batch = writeBatch(db);
  batch.set(userRef, {
    userId: firebaseUser.uid,
    fullName,
    username,
    email,
    phoneNumber: "",
    role: isBootstrappedAdmin ? "ADMIN" : "USER",
    preferredLanguage: resolveLanguage(state.lang).code,
    walletBalanceTzs: 2000,
    totalEarnedTzs: 0,
    totalSpentTzs: 0,
    tasksCompletedCount: 0,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp()
  });
  const txRef = doc(collection(db, "users", firebaseUser.uid, "transactions"));
  batch.set(txRef, {
    userId: firebaseUser.uid,
    type: "DEPOSIT_TOPUP",
    amountTzs: 2000,
    description: "Welcome Starter Bonus",
    referenceCode: refCode("WLC"),
    createdAt: serverTimestamp()
  });
  await batch.commit();
}

function clearSubscriptions() {
  state.unsubscribers.forEach((fn) => fn());
  state.unsubscribers = [];
}

function subscribeUserData(uid) {
  clearSubscriptions();

  state.unsubscribers.push(
    onSnapshot(doc(db, "users", uid), (snap) => {
      state.profile = snap.exists() ? { id: snap.id, ...snap.data() } : null;
      const remoteLang = state.profile?.preferredLanguage;
      if (remoteLang && LANGUAGES.some((l) => l.code === remoteLang) && remoteLang !== state.lang) {
        state.lang = remoteLang;
        localStorage.setItem(STORAGE_LANG_KEY, remoteLang);
        applyDocumentLocale();
      }
      if (isAdminUser() && !state.adminListening) {
        subscribeAdminData();
      }
      renderApp();
    })
  );

  state.unsubscribers.push(
    onSnapshot(query(collection(db, "tasks"), where("status", "==", "ACTIVE")), (snap) => {
      state.tasks = snap.docs
        .map((d) => ({ id: d.id, ...d.data() }))
        .filter((t) => (t.completedSlots || 0) < (t.totalSlots || 0));
      renderApp();
    })
  );

  state.unsubscribers.push(
    onSnapshot(query(collection(db, "tasks"), where("creatorUserId", "==", uid)), (snap) => {
      state.myCampaigns = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      renderApp();
    })
  );

  state.unsubscribers.push(
    onSnapshot(query(collection(db, "submissions"), where("workerUserId", "==", uid)), (snap) => {
      state.mySubmissions = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      renderApp();
    })
  );

  state.unsubscribers.push(
    onSnapshot(collection(db, "users", uid, "transactions"), (snap) => {
      state.transactions = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      renderApp();
    })
  );

  state.unsubscribers.push(
    onSnapshot(query(collection(db, "withdrawals"), where("userId", "==", uid)), (snap) => {
      state.withdrawals = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      renderApp();
    })
  );

  state.adminListening = false;
  if (isAdminUser()) {
    subscribeAdminData();
  }
}

function subscribeAdminData() {
  if (state.adminListening) return;
  state.adminListening = true;

  state.unsubscribers.push(
    onSnapshot(collection(db, "users"), (snap) => {
      state.adminUsers = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      renderApp();
    }),
    onSnapshot(collection(db, "submissions"), (snap) => {
      state.adminSubmissions = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      renderApp();
    }),
    onSnapshot(collection(db, "withdrawals"), (snap) => {
      state.adminWithdrawals = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      renderApp();
    }),
    onSnapshot(collection(db, "tasks"), (snap) => {
      state.adminCampaigns = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      renderApp();
    }),
    onSnapshot(collection(db, "commissions"), (snap) => {
      state.adminCommissions = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      renderApp();
    }),
    onSnapshot(doc(db, "business_account", BUSINESS_ACCOUNT_ID), (snap) => {
      state.businessAccount = snap.exists() ? { id: snap.id, ...snap.data() } : null;
      if (state.businessAccount?.settlementProviderCode) {
        state.selectedBizProvider = state.businessAccount.settlementProviderCode;
      }
      renderApp();
    }),
    onSnapshot(collection(db, "business_account", BUSINESS_ACCOUNT_ID, "ledger"), (snap) => {
      state.businessLedger = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      renderApp();
    })
  );
}

// Financial & Marketplace Actions (Atomic Firebase Transactions)
async function submitProofAction(task, socialHandle, proofNote) {
  const uid = state.authUser.uid;
  if (!socialHandle || socialHandle.trim().length < 2) {
    throw new Error(tr("errHandleRequired"));
  }
  if (!proofNote || proofNote.trim().length < 4) {
    throw new Error(tr("errNoteLength"));
  }
  const existing = state.mySubmissions.find((s) => s.taskId === task.id && s.status !== "REJECTED");
  if (existing) {
    throw new Error(tr("errExistingSubmission"));
  }

  const subRef = doc(collection(db, "submissions"));
  await writeBatch(db).set(subRef, {
    taskId: task.id,
    taskTitle: task.title.slice(0, 120),
    platform: task.platform,
    actionType: task.actionType,
    rewardTzs: task.rewardTzs,
    workerUserId: uid,
    workerName: state.profile?.fullName || "Member",
    socialHandleUsed: socialHandle.trim().slice(0, 80),
    proofNote: proofNote.trim().slice(0, 500),
    status: "PENDING",
    submittedAt: serverTimestamp()
  }).commit();
}

async function createCampaignAction(form) {
  const uid = state.authUser.uid;
  const grossSlot = Number(form.grossSlot || 0);
  const slots = Number(form.slots || 0);
  const totalBudget = grossSlot * slots;
  if (!form.title || form.title.trim().length < 5) throw new Error(tr("errTitleLength"));
  if (!form.handle || form.handle.trim().length < 2) throw new Error(tr("errTargetHandle"));
  if (!form.url || (!form.url.startsWith("http://") && !form.url.startsWith("https://"))) {
    throw new Error(tr("errTargetUrl"));
  }
  if (!form.desc || form.desc.trim().length < 10) throw new Error(tr("errInstructionsLength"));
  if (grossSlot < 100 || grossSlot > 500_000) throw new Error(tr("errSlotBudgetRange"));
  if (slots < 5 || slots > 10_000) throw new Error(tr("errSlotsRange"));
  if (totalBudget < MIN_LIMIT_TZS || totalBudget > MAX_LIMIT_TZS) {
    throw new Error(tr("errCampaignRange", formatTzs(MIN_LIMIT_TZS), formatTzs(MAX_LIMIT_TZS)));
  }

  const { commission: commissionSlot, worker: workerSlot } = computeSplit(grossSlot);
  const userRef = doc(db, "users", uid);
  const taskRef = doc(collection(db, "tasks"));
  const txRef = doc(collection(db, "users", uid, "transactions"));

  await runTransaction(db, async (tx) => {
    const userSnap = await tx.get(userRef);
    const user = userSnap.data();
    if (!user) throw new Error(tr("errFirebaseSync"));
    if ((user.walletBalanceTzs || 0) < totalBudget) {
      throw new Error(tr("errInsufficientBalance", formatTzs(user.walletBalanceTzs || 0)));
    }

    tx.update(userRef, {
      walletBalanceTzs: user.walletBalanceTzs - totalBudget,
      totalSpentTzs: (user.totalSpentTzs || 0) + totalBudget,
      updatedAt: serverTimestamp()
    });
    tx.set(taskRef, {
      creatorUserId: uid,
      creatorName: user.fullName || "Member",
      title: form.title.trim().slice(0, 120),
      description: form.desc.trim().slice(0, 600),
      platform: state.selectedPlatform,
      actionType: state.selectedAction,
      accountHandle: form.handle.trim().slice(0, 80),
      targetUrl: form.url.trim().slice(0, 300),
      rewardTzs: workerSlot,
      grossPerSlotTzs: grossSlot,
      commissionPerSlotTzs: commissionSlot,
      commissionEarnedTzs: 0,
      totalSlots: slots,
      completedSlots: 0,
      status: isAdminUser() ? "ACTIVE" : "PENDING_REVIEW",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    });
    tx.set(txRef, {
      userId: uid,
      type: "CAMPAIGN_FUND",
      amountTzs: totalBudget,
      description: `Campaign escrow: ${form.title.trim()} (${slots} slots • 15% fee on approved only)`.slice(0, 200),
      referenceCode: refCode("CMP"),
      createdAt: serverTimestamp()
    });
  });
}

async function depositTopUpAction(amount, reference) {
  const uid = state.authUser.uid;
  const amt = Math.floor(Number(amount) || 0);
  if (amt < 1000 || amt > 50_000_000) throw new Error(tr("errTopUpRange"));
  if (!reference || reference.trim().length < 6) throw new Error(tr("errTopUpRef"));

  const userRef = doc(db, "users", uid);
  const txRef = doc(collection(db, "users", uid, "transactions"));
  await runTransaction(db, async (tx) => {
    const userSnap = await tx.get(userRef);
    const user = userSnap.data();
    tx.update(userRef, {
      walletBalanceTzs: (user.walletBalanceTzs || 0) + amt,
      updatedAt: serverTimestamp()
    });
    tx.set(txRef, {
      userId: uid,
      type: "DEPOSIT_TOPUP",
      amountTzs: amt,
      description: `${WITHDRAWAL_METHODS.find((m) => m.id === state.selectedProvider)?.label || "Mobile Money"} Top-Up (${reference.trim()})`.slice(0, 200),
      referenceCode: refCode("DEP"),
      createdAt: serverTimestamp()
    });
  });
}

async function requestWithdrawalAction(amount, phone, accountName) {
  const uid = state.authUser.uid;
  const amt = Math.floor(Number(amount) || 0);
  if (amt < MIN_LIMIT_TZS || amt > MAX_LIMIT_TZS) {
    throw new Error(tr("errWithdrawRange", formatTzs(MIN_LIMIT_TZS), formatTzs(MAX_LIMIT_TZS)));
  }
  const normalizedPhone = normalizeTzMobile(phone);
  if (!accountName || accountName.trim().length < 2) {
    throw new Error(tr("errRecipientName"));
  }
  const pendingSnap = await getDocs(
    query(collection(db, "withdrawals"), where("userId", "==", uid), where("status", "==", "PENDING"), limit(1))
  );
  if (!pendingSnap.empty) {
    throw new Error(tr("errDuplicateWithdraw"));
  }

  const userRef = doc(db, "users", uid);
  const wRef = doc(collection(db, "withdrawals"));
  const txRef = doc(collection(db, "users", uid, "transactions"));

  await runTransaction(db, async (tx) => {
    const userSnap = await tx.get(userRef);
    const user = userSnap.data();
    if ((user.walletBalanceTzs || 0) < amt) {
      throw new Error(tr("errInsufficientBalance", formatTzs(user.walletBalanceTzs || 0)));
    }
    tx.update(userRef, {
      walletBalanceTzs: user.walletBalanceTzs - amt,
      updatedAt: serverTimestamp()
    });
    tx.set(wRef, {
      userId: uid,
      userName: user.fullName || "Member",
      amountTzs: amt,
      provider: state.selectedProvider,
      accountNumber: normalizedPhone,
      accountName: accountName.trim().slice(0, 80),
      status: "PENDING",
      requestedAt: serverTimestamp()
    });
    tx.set(txRef, {
      userId: uid,
      type: "WITHDRAWAL",
      amountTzs: amt,
      description: `Withdrawal via ${WITHDRAWAL_METHODS.find((m) => m.id === state.selectedProvider)?.label} (${normalizedPhone})`.slice(0, 200),
      referenceCode: refCode("WDR"),
      createdAt: serverTimestamp()
    });
  });
}

async function adminReviewSubmission(submissionId, approve) {
  const subRef = doc(db, "submissions", submissionId);
  const commRef = doc(db, "commissions", submissionId);
  const bizRef = doc(db, "business_account", BUSINESS_ACCOUNT_ID);
  const bizLedgerRef = doc(db, "business_account", BUSINESS_ACCOUNT_ID, "ledger", submissionId);

  await runTransaction(db, async (tx) => {
    const subSnap = await tx.get(subRef);
    if (!subSnap.exists()) throw new Error("Submission not found.");
    const sub = subSnap.data();
    if (sub.status !== "PENDING") throw new Error("Already reviewed.");

    if (!approve) {
      tx.update(subRef, {
        status: "REJECTED",
        reviewerNote: "Rejected by admin. No commission deducted.",
        reviewedAt: serverTimestamp()
      });
      return;
    }

    const [commSnap, ledgerSnap, workerSnap, taskSnap, bizSnap] = await Promise.all([
      tx.get(commRef),
      tx.get(bizLedgerRef),
      tx.get(doc(db, "users", sub.workerUserId)),
      tx.get(doc(db, "tasks", sub.taskId)),
      tx.get(bizRef)
    ]);

    if (commSnap.exists() || ledgerSnap.exists()) {
      throw new Error("Duplicate payout blocked: already processed.");
    }
    const worker = workerSnap.data();
    if (!worker) throw new Error("Worker profile missing.");
    const task = taskSnap.exists() ? taskSnap.data() : null;
    if (task && (task.completedSlots || 0) >= (task.totalSlots || 0)) {
      throw new Error("All campaign slots completed.");
    }

    const gross = task?.grossPerSlotTzs || sub.rewardTzs;
    const workerReward = task?.grossPerSlotTzs ? sub.rewardTzs : computeSplit(gross).worker;
    const commission = Math.max(0, gross - workerReward);
    const reference = refCode("BIZ");

    tx.update(subRef, {
      status: "APPROVED",
      reviewerNote: "Verified by admin.",
      reviewedAt: serverTimestamp()
    });

    if (task) {
      const nextSlots = Math.min(task.totalSlots, (task.completedSlots || 0) + 1);
      tx.update(doc(db, "tasks", sub.taskId), {
        completedSlots: nextSlots,
        commissionEarnedTzs: (task.commissionEarnedTzs || 0) + commission,
        status: nextSlots >= task.totalSlots ? "COMPLETED" : task.status,
        updatedAt: serverTimestamp()
      });
    }

    tx.update(doc(db, "users", sub.workerUserId), {
      walletBalanceTzs: (worker.walletBalanceTzs || 0) + workerReward,
      totalEarnedTzs: (worker.totalEarnedTzs || 0) + workerReward,
      tasksCompletedCount: (worker.tasksCompletedCount || 0) + 1,
      updatedAt: serverTimestamp()
    });

    tx.set(doc(collection(db, "users", sub.workerUserId, "transactions")), {
      userId: sub.workerUserId,
      type: "TASK_REWARD",
      amountTzs: workerReward,
      description: `Approved (85% reward): ${sub.taskTitle}`.slice(0, 200),
      referenceCode: refCode("TSK"),
      createdAt: serverTimestamp()
    });

    const creatorId = task?.creatorUserId || sub.workerUserId;
    const ledgerDoc = {
      submissionId,
      taskId: sub.taskId,
      taskTitle: sub.taskTitle.slice(0, 120),
      creatorUserId: creatorId,
      workerUserId: sub.workerUserId,
      grossAmountTzs: gross,
      commissionTzs: commission,
      workerRewardTzs: workerReward,
      referenceCode: reference,
      createdAt: serverTimestamp()
    };
    tx.set(commRef, ledgerDoc);
    tx.set(bizLedgerRef, {
      ...ledgerDoc,
      verified: true,
      settlementChannel: "ESCROW_VERIFIED_SPLIT"
    });

    const existingBiz = bizSnap.exists() ? bizSnap.data() : {};
    const bizPayload = {
      accountId: BUSINESS_ACCOUNT_ID,
      accountName: existingBiz.accountName || "RAZAQ TASK BUSINESS ACCOUNT",
      totalCommissionEarnedTzs: (existingBiz.totalCommissionEarnedTzs || 0) + commission,
      totalWorkerRewardsAllocatedTzs: (existingBiz.totalWorkerRewardsAllocatedTzs || 0) + workerReward,
      totalGrossVerifiedVolumeTzs: (existingBiz.totalGrossVerifiedVolumeTzs || 0) + gross,
      completedTransactionsCount: (existingBiz.completedTransactionsCount || 0) + 1,
      settlementProviderCode: existingBiz.settlementProviderCode || "UNCONFIGURED",
      settlementMerchantRef: existingBiz.settlementMerchantRef || "",
      updatedAt: serverTimestamp()
    };
    if (bizSnap.exists()) tx.update(bizRef, bizPayload);
    else tx.set(bizRef, bizPayload);

    if (commission >= 15 && task?.creatorUserId) {
      tx.set(doc(collection(db, "users", task.creatorUserId, "transactions")), {
        userId: task.creatorUserId,
        type: "PLATFORM_COMMISSION",
        amountTzs: commission,
        description: `15% fee (approved task): ${sub.taskTitle}`.slice(0, 200),
        referenceCode: reference,
        createdAt: serverTimestamp()
      });
    }
  });
}

async function adminReviewWithdrawal(withdrawalId, approve) {
  const wRef = doc(db, "withdrawals", withdrawalId);
  await runTransaction(db, async (tx) => {
    const wSnap = await tx.get(wRef);
    const w = wSnap.data();
    if (!w || w.status !== "PENDING") throw new Error("Already processed.");

    if (approve) {
      tx.update(wRef, {
        status: "APPROVED",
        adminNote: `Approved via ${w.provider}`,
        processedAt: serverTimestamp()
      });
    } else {
      const userRef = doc(db, "users", w.userId);
      const userSnap = await tx.get(userRef);
      const user = userSnap.data();
      tx.update(wRef, {
        status: "REJECTED",
        adminNote: "Declined by admin; TZS refunded.",
        processedAt: serverTimestamp()
      });
      tx.update(userRef, {
        walletBalanceTzs: (user.walletBalanceTzs || 0) + w.amountTzs,
        updatedAt: serverTimestamp()
      });
      tx.set(doc(collection(db, "users", w.userId, "transactions")), {
        userId: w.userId,
        type: "WITHDRAWAL_REFUND",
        amountTzs: w.amountTzs,
        description: `Refund: ${w.provider} payout declined`.slice(0, 200),
        referenceCode: refCode("RFD"),
        createdAt: serverTimestamp()
      });
    }
  });
}

async function adminModerateCampaign(taskId, newStatus) {
  const taskRef = doc(db, "tasks", taskId);
  await runTransaction(db, async (tx) => {
    const tSnap = await tx.get(taskRef);
    const task = tSnap.data();
    if (!task) throw new Error("Campaign not found.");

    tx.update(taskRef, { status: newStatus, updatedAt: serverTimestamp() });
    if (newStatus === "REJECTED" && ["PENDING_REVIEW", "ACTIVE", "PAUSED"].includes(task.status)) {
      const remaining = Math.max(0, (task.totalSlots || 0) - (task.completedSlots || 0));
      const grossSlot = task.grossPerSlotTzs || task.rewardTzs;
      const refund = remaining * grossSlot;
      if (refund > 0) {
        const userRef = doc(db, "users", task.creatorUserId);
        const uSnap = await tx.get(userRef);
        if (uSnap.exists()) {
          tx.update(userRef, {
            walletBalanceTzs: (uSnap.data().walletBalanceTzs || 0) + refund,
            updatedAt: serverTimestamp()
          });
          tx.set(doc(collection(db, "users", task.creatorUserId, "transactions")), {
            userId: task.creatorUserId,
            type: "WITHDRAWAL_REFUND",
            amountTzs: refund,
            description: `100% refund for uncompleted slots: ${task.title}`.slice(0, 200),
            referenceCode: refCode("CMF"),
            createdAt: serverTimestamp()
          });
        }
      }
    }
  });
}

async function saveBusinessSettlementRail(providerCode, merchantRef) {
  const bizRef = doc(db, "business_account", BUSINESS_ACCOUNT_ID);
  await runTransaction(db, async (tx) => {
    const snap = await tx.get(bizRef);
    if (!snap.exists()) {
      tx.set(bizRef, {
        accountId: BUSINESS_ACCOUNT_ID,
        accountName: "RAZAQ TASK BUSINESS ACCOUNT",
        totalCommissionEarnedTzs: 0,
        totalWorkerRewardsAllocatedTzs: 0,
        totalGrossVerifiedVolumeTzs: 0,
        completedTransactionsCount: 0,
        settlementProviderCode: providerCode,
        settlementMerchantRef: merchantRef.trim().slice(0, 80),
        updatedAt: serverTimestamp()
      });
    } else {
      tx.update(bizRef, {
        settlementProviderCode: providerCode,
        settlementMerchantRef: merchantRef.trim().slice(0, 80),
        updatedAt: serverTimestamp()
      });
    }
  });
}

// UI Rendering
function renderOnboarding() {
  document.getElementById("onb-badge").textContent = tr("onboardingBadge");
  document.getElementById("onb-title").textContent = tr("onboardingTitle");
  document.getElementById("onb-subtitle").textContent = tr("onboardingSubtitle");
  document.getElementById("onboarding-continue-btn").textContent = tr("continueBtn");

  const grid = document.getElementById("onboarding-lang-grid");
  grid.innerHTML = LANGUAGES.map((l) => {
    const isActive = l.code === state.lang;
    return `
      <button type="button" class="lang-card ${isActive ? "active" : ""}" data-lang="${l.code}">
        <div class="lang-card-top">
          <span>${esc(l.nativeName)}</span>
          <div class="lang-pill-group">
            ${l.code === DEFAULT_LANGUAGE ? `<span class="badge badge-green">${esc(tr("defaultBadge"))}</span>` : ""}
            ${l.rtl ? `<span class="badge badge-amber">${esc(tr("rtlBadge"))}</span>` : ""}
          </div>
        </div>
        <div class="muted-label">${esc(l.englishName)} (${esc(l.code.toUpperCase())})</div>
      </button>
    `;
  }).join("");

  grid.querySelectorAll("[data-lang]").forEach((btn) => {
    btn.onclick = () => changeLanguage(btn.dataset.lang, false, false);
  });
}

function renderAuthStrings() {
  const currentLangObj = resolveLanguage(state.lang);
  document.getElementById("auth-brand-title").textContent = tr("appName");
  document.getElementById("auth-brand-subtitle").textContent = tr("brandSubtitle");
  document.getElementById("auth-lang-btn").textContent = currentLangObj.nativeName;
  document.getElementById("auth-lead").textContent = tr("authLead");
  document.getElementById("auth-chip-1").textContent = tr("authChip1");
  document.getElementById("auth-chip-2").textContent = tr("authChip2");
  document.getElementById("auth-chip-3").textContent = tr("authChip3");
  document.getElementById("google-signin-btn").textContent = tr("authGoogleBtn");
  document.getElementById("auth-meta-text").textContent = tr("authMeta");
}

function renderNav() {
  const currentLangObj = resolveLanguage(state.lang);
  document.getElementById("sidebar-brand-title").textContent = tr("appName");
  document.getElementById("sidebar-brand-subtitle").textContent = tr("crossPlatformSync");
  document.getElementById("sidebar-bal-label").textContent = tr("availableBalance");
  document.getElementById("sidebar-profile-btn").textContent = tr("profileBtn");
  document.getElementById("sidebar-lang-btn").textContent = tr("languageBtn");
  document.getElementById("theme-toggle-btn").textContent = tr("themeBtn");
  document.getElementById("topbar-lang-btn").textContent = currentLangObj.code.toUpperCase();
  document.getElementById("topbar-theme-btn").textContent = tr("themeBtn");
  document.getElementById("topbar-profile-btn").textContent = tr("accountBtn");

  const navItems = [
    { id: "marketplace", label: tr("navEarn") },
    { id: "promote", label: tr("navGrow") },
    { id: "dashboard", label: tr("navDashboard") },
    { id: "wallet", label: tr("navWallet") }
  ];
  if (isAdminUser()) {
    const pendingCount =
      state.adminSubmissions.filter((s) => s.status === "PENDING").length +
      state.adminWithdrawals.filter((w) => w.status === "PENDING").length;
    navItems.push({ id: "admin", label: tr("navAdmin"), badge: pendingCount });
  }

  const desktopNav = document.getElementById("desktop-nav");
  const mobileNav = document.getElementById("mobile-nav");
  desktopNav.innerHTML = "";
  mobileNav.innerHTML = "";

  navItems.forEach((item) => {
    const badgeHtml = item.badge > 0 ? `<span class="nav-badge">${item.badge}</span>` : "";
    const dBtn = document.createElement("button");
    dBtn.className = `nav-item ${state.tab === item.id ? "active" : ""}`;
    dBtn.innerHTML = `<span>${esc(item.label)}</span>${badgeHtml}`;
    dBtn.onclick = () => { state.tab = item.id; renderApp(); };
    desktopNav.appendChild(dBtn);

    const mBtn = document.createElement("button");
    mBtn.className = `nav-item ${state.tab === item.id ? "active" : ""}`;
    mBtn.innerHTML = `<span>${esc(item.label)}</span>`;
    mBtn.onclick = () => { state.tab = item.id; renderApp(); };
    mobileNav.appendChild(mBtn);
  });

  const titles = {
    marketplace: tr("titleMarket"),
    promote: tr("titlePromote"),
    dashboard: tr("titleDashboard"),
    wallet: tr("titleWallet"),
    admin: tr("titleAdmin")
  };
  document.getElementById("topbar-title").textContent = titles[state.tab] || tr("appName");
}

function renderApp() {
  applyDocumentLocale();
  const onboardingScreen = document.getElementById("onboarding-screen");
  const authScreen = document.getElementById("auth-screen");
  const appShell = document.getElementById("app-shell");

  if (!state.onboardingDone) {
    onboardingScreen.classList.remove("hidden");
    authScreen.classList.add("hidden");
    appShell.classList.add("hidden");
    renderOnboarding();
    return;
  }

  onboardingScreen.classList.add("hidden");

  if (!state.authUser) {
    authScreen.classList.remove("hidden");
    appShell.classList.add("hidden");
    renderAuthStrings();
    return;
  }

  authScreen.classList.add("hidden");
  appShell.classList.remove("hidden");

  const bal = formatTzs(state.profile?.walletBalanceTzs || 0);
  document.getElementById("sidebar-balance").textContent = bal;
  document.getElementById("topbar-wallet-btn").textContent = bal;

  renderNav();
  const main = document.getElementById("main-content");

  if (state.tab === "marketplace") renderMarketplace(main);
  else if (state.tab === "promote") renderPromote(main);
  else if (state.tab === "dashboard") renderDashboard(main);
  else if (state.tab === "wallet") renderWallet(main);
  else if (state.tab === "admin" && isAdminUser()) renderAdmin(main);
  else {
    state.tab = "marketplace";
    renderMarketplace(main);
  }
}

function renderDualHub(container) {
  const hub = document.createElement("div");
  hub.className = "hub-grid";
  hub.innerHTML = `
    <div class="hub-card hub-earn" id="hub-earn-card">
      <span class="badge badge-green">${esc(tr("sec1Badge"))}</span>
      <h3 style="margin:0;">${esc(tr("sec1Title"))}</h3>
      <p class="muted-label" style="margin:0;">${esc(tr("sec1Desc"))}</p>
    </div>
    <div class="hub-card hub-grow" id="hub-grow-card">
      <span class="badge badge-amber">${esc(tr("sec2Badge"))}</span>
      <h3 style="margin:0;">${esc(tr("sec2Title"))}</h3>
      <p class="muted-label" style="margin:0;">${esc(tr("sec2Desc"))}</p>
    </div>
  `;
  container.appendChild(hub);
  hub.querySelector("#hub-earn-card").onclick = () => { state.tab = "marketplace"; renderApp(); };
  hub.querySelector("#hub-grow-card").onclick = () => { state.tab = "promote"; renderApp(); };
}

function renderMarketplace(main) {
  main.innerHTML = "";
  renderDualHub(main);

  const filterBar = document.createElement("div");
  filterBar.className = "card form-grid";
  filterBar.innerHTML = `
    <div class="pill-row">
      <button class="pill-btn ${state.platformFilter === "ALL" ? "active" : ""}" data-p="ALL">${esc(tr("allPlatforms"))}</button>
      ${PLATFORMS.map((p) => `<button class="pill-btn ${state.platformFilter === p.id ? "active" : ""}" data-p="${p.id}">${esc(p.label)}</button>`).join("")}
    </div>
    <input id="market-search" type="search" placeholder="${esc(tr("searchPlaceholder"))}" value="${esc(state.searchQuery)}" />
  `;
  main.appendChild(filterBar);

  filterBar.querySelectorAll("[data-p]").forEach((btn) => {
    btn.onclick = () => { state.platformFilter = btn.dataset.p; renderApp(); };
  });
  const searchInput = filterBar.querySelector("#market-search");
  searchInput.oninput = (e) => {
    state.searchQuery = e.target.value;
    renderTaskCards(grid);
  };

  const grid = document.createElement("div");
  grid.className = "task-grid";
  main.appendChild(grid);
  renderTaskCards(grid);
}

function renderTaskCards(grid) {
  const q = state.searchQuery.trim().toLowerCase();
  const filtered = state.tasks.filter((tItem) => {
    const pOk = state.platformFilter === "ALL" || tItem.platform === state.platformFilter;
    const qOk = !q || (tItem.title || "").toLowerCase().includes(q) || (tItem.accountHandle || "").toLowerCase().includes(q);
    return pOk && qOk;
  });

  if (filtered.length === 0) {
    grid.innerHTML = `<div class="card"><p class="muted-label">${esc(tr("noMatchingTasks"))}</p></div>`;
    return;
  }

  grid.innerHTML = "";
  filtered.forEach((task) => {
    const card = document.createElement("div");
    card.className = "card form-grid";
    const pct = task.totalSlots > 0 ? Math.round(((task.completedSlots || 0) / task.totalSlots) * 100) : 0;
    const isMine = task.creatorUserId === state.authUser.uid;
    card.innerHTML = `
      <div class="kv-row">
        <span class="badge badge-green">${esc(task.platform)}</span>
        <span class="mono badge badge-amber">+${formatTzs(task.rewardTzs)}</span>
      </div>
      <h3 style="margin:0; font-size:16px;">${esc(task.title)}</h3>
      <p class="muted-label" style="margin:0;">${esc(task.description)}</p>
      <div class="muted-label">${esc(task.accountHandle)} • ${esc(tr("slotsText", task.completedSlots || 0, task.totalSlots, pct))}</div>
      <div class="progress-track"><div class="progress-fill" style="width:${pct}%"></div></div>
      <button class="btn btn-primary" ${isMine ? "disabled" : ""}>${esc(isMine ? tr("yourCampaignBtn") : tr("openSubmitProofBtn"))}</button>
    `;
    if (!isMine) {
      card.querySelector("button").onclick = () => openSubmitModal(task);
    }
    grid.appendChild(card);
  });
}

function openSubmitModal(task) {
  const backdrop = document.getElementById("modal-backdrop");
  const box = document.getElementById("modal-box");
  backdrop.classList.remove("hidden");
  box.innerHTML = `
    <div class="form-grid">
      <div class="kv-row">
        <h3 style="margin:0;">${esc(tr("submitProofTitle"))}</h3>
        <button class="btn btn-outline btn-sm" id="close-modal">${esc(tr("closeBtn"))}</button>
      </div>
      <p class="muted-label">${esc(task.title)} • ${esc(tr("rewardLabel"))}: <strong class="mono">${formatTzs(task.rewardTzs)}</strong></p>
      <a class="btn btn-ghost" href="${esc(task.targetUrl)}" target="_blank" rel="noopener noreferrer">${esc(tr("openSocialLink", task.accountHandle))}</a>
      <label>${esc(tr("yourHandleLabel"))}
        <input id="proof-handle" type="text" placeholder="${esc(tr("yourHandlePlaceholder"))}" />
      </label>
      <label>${esc(tr("verificationNoteLabel"))}
        <textarea id="proof-note" rows="3" placeholder="${esc(tr("verificationNotePlaceholder"))}"></textarea>
      </label>
      <p id="modal-err" class="error-banner hidden"></p>
      <button id="submit-proof-btn" class="btn btn-primary">${esc(tr("submitProofCta"))}</button>
    </div>
  `;
  box.querySelector("#close-modal").onclick = () => backdrop.classList.add("hidden");
  box.querySelector("#submit-proof-btn").onclick = async () => {
    try {
      await submitProofAction(task, box.querySelector("#proof-handle").value, box.querySelector("#proof-note").value);
      backdrop.classList.add("hidden");
      showToast(tr("toastProofSubmitted"));
    } catch (err) {
      const errEl = box.querySelector("#modal-err");
      errEl.textContent = err.message;
      errEl.classList.remove("hidden");
    }
  };
}

function renderPromote(main) {
  main.innerHTML = `
    <div class="two-col-grid">
      <div class="card form-grid">
        <h3 style="margin:0;">${esc(tr("promoteTitle"))}</h3>
        <p class="muted-label" style="margin:0;">${esc(tr("promoteDesc"))}</p>
        <div>
          <div class="muted-label" style="margin-bottom:6px;">${esc(tr("socialPlatformLabel"))}</div>
          <div class="pill-row" id="promote-platforms">
            ${PLATFORMS.map((p) => `<button type="button" class="pill-btn ${state.selectedPlatform === p.id ? "active" : ""}" data-id="${p.id}">${esc(p.label)}</button>`).join("")}
          </div>
        </div>
        <div>
          <div class="muted-label" style="margin-bottom:6px;">${esc(tr("organicActionLabel"))}</div>
          <div class="pill-row" id="promote-actions">
            ${ACTIONS.map((a) => `<button type="button" class="pill-btn ${state.selectedAction === a.id ? "active" : ""}" data-id="${a.id}" data-def="${a.defaultTzs}">${esc(tr(a.labelKey))}</button>`).join("")}
          </div>
        </div>
        <label>${esc(tr("campaignTitleLabel"))}<input id="cmp-title" type="text" placeholder="${esc(tr("campaignTitlePlaceholder"))}" /></label>
        <div class="form-row">
          <label>${esc(tr("accountHandleLabel"))}<input id="cmp-handle" type="text" placeholder="${esc(tr("accountHandlePlaceholder"))}" /></label>
          <label>${esc(tr("profileUrlLabel"))}<input id="cmp-url" type="url" value="https://" /></label>
        </div>
        <label>${esc(tr("instructionsLabel"))}<textarea id="cmp-desc" rows="2" placeholder="${esc(tr("instructionsPlaceholder"))}"></textarea></label>
        <div class="form-row">
          <label>${esc(tr("budgetPerSlotLabel"))}<input id="cmp-gross" type="number" value="400" min="100" max="500000" /></label>
          <label>${esc(tr("targetSlotsLabel"))}<input id="cmp-slots" type="number" value="25" min="5" max="10000" /></label>
        </div>
        <div class="summary-box" id="cmp-preview"></div>
        <p id="cmp-error" class="error-banner hidden"></p>
        <button id="cmp-submit" class="btn btn-primary btn-lg" type="button">${esc(tr("launchCampaignBtn"))}</button>
      </div>
      <div class="card form-grid">
        <h3 style="margin:0;">${esc(tr("trackerTitle", state.myCampaigns.length))}</h3>
        <div class="list-stack" id="my-campaigns-list"></div>
      </div>
    </div>
  `;

  const grossEl = main.querySelector("#cmp-gross");
  const slotsEl = main.querySelector("#cmp-slots");
  const previewEl = main.querySelector("#cmp-preview");
  const errorEl = main.querySelector("#cmp-error");
  const submitBtn = main.querySelector("#cmp-submit");

  const updatePreview = () => {
    const grossSlot = Math.floor(Number(grossEl.value) || 0);
    const slots = Math.floor(Number(slotsEl.value) || 0);
    const totalBudget = grossSlot * slots;
    const { commission, worker } = computeSplit(totalBudget);
    const { worker: workerSlot } = computeSplit(grossSlot);
    const bal = state.profile?.walletBalanceTzs || 0;
    previewEl.innerHTML = `
      <div class="kv-row"><strong>${esc(tr("totalBudgetLabel"))}</strong><strong class="mono">${formatTzs(totalBudget)}</strong></div>
      <div class="kv-row"><span>${esc(tr("commission15Label"))}</span><span class="mono" style="color:var(--amber)">-${formatTzs(commission)}</span></div>
      <div class="kv-row"><span>${esc(tr("workerPool85Label"))}</span><span class="mono" style="color:var(--primary)">${formatTzs(worker)} (${formatTzs(workerSlot)}${esc(tr("perTaskUnit"))})</span></div>
      <div class="kv-row"><span>${esc(tr("yourWalletLabel"))}</span><span class="mono">${formatTzs(bal)}</span></div>
      <div class="muted-label">${esc(tr("commissionNote"))}</div>
    `;
    let errMsg = "";
    if (grossSlot < 100 || grossSlot > 500_000) errMsg = tr("errSlotBudgetRange");
    else if (slots < 5) errMsg = tr("errSlotsMin");
    else if (totalBudget < MIN_LIMIT_TZS) errMsg = tr("errMinCampaignBudget", formatTzs(MIN_LIMIT_TZS));
    else if (totalBudget > MAX_LIMIT_TZS) errMsg = tr("errMaxCampaignBudget", formatTzs(MAX_LIMIT_TZS));
    else if (bal < totalBudget) errMsg = tr("errInsufficientBalance", formatTzs(bal));

    if (errMsg) {
      errorEl.textContent = errMsg;
      errorEl.classList.remove("hidden");
      submitBtn.disabled = true;
    } else {
      errorEl.classList.add("hidden");
      submitBtn.disabled = false;
    }
  };

  main.querySelectorAll("#promote-platforms button").forEach((b) => {
    b.onclick = () => { state.selectedPlatform = b.dataset.id; renderApp(); };
  });
  main.querySelectorAll("#promote-actions button").forEach((b) => {
    b.onclick = () => { state.selectedAction = b.dataset.id; grossEl.value = b.dataset.def; updatePreview(); renderApp(); };
  });
  grossEl.oninput = updatePreview;
  slotsEl.oninput = updatePreview;
  updatePreview();

  submitBtn.onclick = async () => {
    try {
      await createCampaignAction({
        title: main.querySelector("#cmp-title").value,
        handle: main.querySelector("#cmp-handle").value,
        url: main.querySelector("#cmp-url").value,
        desc: main.querySelector("#cmp-desc").value,
        grossSlot: grossEl.value,
        slots: slotsEl.value
      });
      showToast(tr("toastCampaignCreated"));
      renderApp();
    } catch (err) {
      errorEl.textContent = err.message;
      errorEl.classList.remove("hidden");
    }
  };

  const listEl = main.querySelector("#my-campaigns-list");
  if (state.myCampaigns.length === 0) {
    listEl.innerHTML = `<p class="muted-label">${esc(tr("noCampaignsYet"))}</p>`;
  } else {
    state.myCampaigns.forEach((c) => {
      const grossSlot = c.grossPerSlotTzs || c.rewardTzs;
      const totalB = grossSlot * (c.totalSlots || 0);
      const row = document.createElement("div");
      row.className = "summary-box";
      row.innerHTML = `
        <div class="kv-row"><strong>${esc(c.title)}</strong><span class="badge">${esc(statusLabel(c.status))}</span></div>
        <div class="muted-label">${esc(c.platform)} • ${esc(tr("verifiedText", c.completedSlots || 0, c.totalSlots, formatTzs(c.rewardTzs)))}</div>
        <div class="kv-row"><span>${esc(tr("budgetCommissionRow", formatTzs(totalB), formatTzs(c.commissionEarnedTzs || 0)))}</span></div>
      `;
      listEl.appendChild(row);
    });
  }
}

function renderDashboard(main) {
  main.innerHTML = "";
  renderDualHub(main);

  const p = state.profile || {};
  const kpis = document.createElement("div");
  kpis.className = "kpi-grid";
  kpis.innerHTML = `
    <div class="kpi-card"><div class="muted-label">${esc(tr("kpiWalletTitle"))}</div><div class="kpi-value mono">${formatTzs(p.walletBalanceTzs)}</div><div class="muted-label">${esc(tr("kpiWalletSub"))}</div></div>
    <div class="kpi-card"><div class="muted-label">${esc(tr("kpiEarnedTitle"))}</div><div class="kpi-value mono">${formatTzs(p.totalEarnedTzs)}</div><div class="muted-label">${esc(tr("kpiEarnedSub", p.tasksCompletedCount || 0))}</div></div>
    <div class="kpi-card"><div class="muted-label">${esc(tr("kpiFundedTitle"))}</div><div class="kpi-value mono">${state.myCampaigns.length}</div><div class="muted-label">${esc(tr("kpiFundedSub", formatTzs(p.totalSpentTzs)))}</div></div>
    <div class="kpi-card"><div class="muted-label">${esc(tr("kpiLiveTasksTitle"))}</div><div class="kpi-value mono">${state.tasks.length}</div><div class="muted-label">${esc(tr("kpiLiveTasksSub"))}</div></div>
  `;
  main.appendChild(kpis);

  const subsCard = document.createElement("div");
  subsCard.className = "card form-grid";
  subsCard.innerHTML = `<h3 style="margin:0;">${esc(tr("mySubmissionsTitle", state.mySubmissions.length))}</h3>`;
  if (state.mySubmissions.length === 0) {
    subsCard.innerHTML += `<p class="muted-label">${esc(tr("noSubmissionsYet"))}</p>`;
  } else {
    state.mySubmissions.forEach((s) => {
      subsCard.innerHTML += `
        <div class="summary-box">
          <div class="kv-row"><strong>${esc(s.taskTitle)}</strong><span class="badge">${esc(statusLabel(s.status))}</span></div>
          <div class="muted-label">${esc(s.platform)} • ${formatTzs(s.rewardTzs)} • ${esc(tr("handleMeta", s.socialHandleUsed))}</div>
        </div>
      `;
    });
  }
  main.appendChild(subsCard);
}

function renderWallet(main) {
  const hasPending = state.withdrawals.some((w) => w.status === "PENDING");
  const selectedMethodObj = WITHDRAWAL_METHODS.find((m) => m.id === state.selectedProvider) || WITHDRAWAL_METHODS[0];
  main.innerHTML = `
    <div class="two-col-grid">
      <div class="card form-grid">
        <div class="kv-row">
          <h3 style="margin:0;">${esc(tr("walletWithdrawTitle"))}</h3>
          <span class="badge badge-green">TZS 1,000 – 1,000,000</span>
        </div>
        <div class="muted-label">${esc(tr("walletBalanceLabel"))}: <strong class="mono">${formatTzs(state.profile?.walletBalanceTzs || 0)}</strong></div>
        <div class="pill-row" id="wallet-methods">
          ${WITHDRAWAL_METHODS.map((m) => `<button type="button" class="pill-btn ${state.selectedProvider === m.id ? "active" : ""}" data-id="${m.id}">${esc(m.label)}</button>`).join("")}
        </div>
        <label>${esc(tr("withdrawalAmountLabel"))}<input id="wdr-amt" type="number" placeholder="1000 - 1000000" min="1000" max="1000000" /></label>
        <label>${esc(tr("mobileNumberLabel", selectedMethodObj.label))}<input id="wdr-phone" type="tel" placeholder="${esc(selectedMethodObj.hint)}" /></label>
        <label>${esc(tr("accountNameLabel"))}<input id="wdr-name" type="text" value="${esc(state.profile?.fullName || "")}" /></label>
        ${hasPending ? `<p class="error-banner">${esc(tr("duplicatePendingBanner"))}</p>` : ""}
        <p id="wdr-err" class="error-banner hidden"></p>
        <button id="wdr-btn" class="btn btn-primary" ${hasPending ? "disabled" : ""}>${esc(tr("requestWithdrawBtn"))}</button>

        <hr style="border:none;border-top:1px solid var(--border);margin:8px 0;" />
        <h3 style="margin:0;">${esc(tr("topUpTitle"))}</h3>
        <div class="form-row">
          <label>${esc(tr("topUpAmountLabel"))}<input id="dep-amt" type="number" placeholder="5000" /></label>
          <label>${esc(tr("mobileRefLabel"))}<input id="dep-ref" type="text" placeholder="0754123456" /></label>
        </div>
        <button id="dep-btn" class="btn btn-outline">${esc(tr("addFundsBtn"))}</button>
      </div>

      <div class="form-grid">
        <div class="card form-grid">
          <h3 style="margin:0;">${esc(tr("withdrawalHistoryTitle", state.withdrawals.length))}</h3>
          <div class="list-stack">
            ${state.withdrawals.length === 0 ? `<p class="muted-label">${esc(tr("noWithdrawalsYet"))}</p>` : state.withdrawals.map((w) => `
              <div class="summary-box">
                <div class="kv-row"><strong class="mono">${formatTzs(w.amountTzs)}</strong><span class="badge">${esc(statusLabel(w.status))}</span></div>
                <div class="muted-label">${esc(WITHDRAWAL_METHODS.find((m) => m.id === w.provider)?.label || w.provider)} • ${esc(w.accountNumber)} (${esc(w.accountName)})</div>
              </div>
            `).join("")}
          </div>
        </div>
        <div class="card form-grid">
          <h3 style="margin:0;">${esc(tr("walletLedgerTitle", state.transactions.length))}</h3>
          <div class="list-stack">
            ${state.transactions.slice(0, 20).map((tItem) => `
              <div class="summary-box">
                <div class="kv-row"><strong>${esc(tItem.description)}</strong><span class="mono">${formatTzs(tItem.amountTzs)}</span></div>
                <div class="muted-label">${esc(txTypeLabel(tItem.type))} • ${esc(tItem.referenceCode)}</div>
              </div>
            `).join("")}
          </div>
        </div>
      </div>
    </div>
  `;

  main.querySelectorAll("#wallet-methods button").forEach((b) => {
    b.onclick = () => { state.selectedProvider = b.dataset.id; renderApp(); };
  });

  main.querySelector("#wdr-btn").onclick = async () => {
    try {
      await requestWithdrawalAction(
        main.querySelector("#wdr-amt").value,
        main.querySelector("#wdr-phone").value,
        main.querySelector("#wdr-name").value
      );
      showToast(tr("toastWithdrawSubmitted"));
    } catch (err) {
      const eEl = main.querySelector("#wdr-err");
      eEl.textContent = err.message;
      eEl.classList.remove("hidden");
    }
  };

  main.querySelector("#dep-btn").onclick = async () => {
    try {
      await depositTopUpAction(main.querySelector("#dep-amt").value, main.querySelector("#dep-ref").value);
      showToast(tr("toastWalletCredited"));
    } catch (err) {
      showToast(err.message);
    }
  };
}

function renderAdmin(main) {
  const pendingSubs = state.adminSubmissions.filter((s) => s.status === "PENDING");
  const pendingWdrs = state.adminWithdrawals.filter((w) => w.status === "PENDING");
  const bizCommission = Math.max(
    state.businessAccount?.totalCommissionEarnedTzs || 0,
    state.adminCommissions.reduce((acc, c) => acc + (c.commissionTzs || 0), 0)
  );
  const workerAllocated = Math.max(
    state.businessAccount?.totalWorkerRewardsAllocatedTzs || 0,
    state.adminCommissions.reduce((acc, c) => acc + (c.workerRewardTzs || 0), 0)
  );
  const pendingCommissions = state.adminCampaigns
    .filter((c) => ["PENDING_REVIEW", "ACTIVE", "PAUSED"].includes(c.status))
    .reduce((acc, c) => acc + Math.max(0, (c.totalSlots || 0) - (c.completedSlots || 0)) * computeSplit(c.grossPerSlotTzs || c.rewardTzs).commission, 0);
  const customerCustody = state.adminUsers.reduce((acc, u) => acc + (u.walletBalanceTzs || 0), 0);

  main.innerHTML = `
    <div class="tabs-bar">
      <button class="pill-btn ${state.adminSubTab === "overview" ? "active" : ""}" data-atab="overview">${esc(tr("adminOverview"))}</button>
      <button class="pill-btn ${state.adminSubTab === "proofs" ? "active" : ""}" data-atab="proofs">${esc(tr("adminProofs", pendingSubs.length))}</button>
      <button class="pill-btn ${state.adminSubTab === "payouts" ? "active" : ""}" data-atab="payouts">${esc(tr("adminPayouts", pendingWdrs.length))}</button>
      <button class="pill-btn ${state.adminSubTab === "campaigns" ? "active" : ""}" data-atab="campaigns">${esc(tr("adminCampaigns", state.adminCampaigns.length))}</button>
      <button class="pill-btn ${state.adminSubTab === "business" ? "active" : ""}" data-atab="business">${esc(tr("adminBusinessTab"))}</button>
    </div>
    <div id="admin-pane" class="form-grid"></div>
  `;

  main.querySelectorAll("[data-atab]").forEach((b) => {
    b.onclick = () => { state.adminSubTab = b.dataset.atab; renderApp(); };
  });

  const pane = main.querySelector("#admin-pane");
  if (state.adminSubTab === "overview") {
    pane.innerHTML = `
      <div class="kpi-grid">
        <div class="kpi-card"><div class="muted-label">${esc(tr("bizEarnings15"))}</div><div class="kpi-value mono">${formatTzs(bizCommission)}</div><div class="muted-label">${esc(tr("verifiedOnlySub"))}</div></div>
        <div class="kpi-card"><div class="muted-label">${esc(tr("pendingProofsKpi"))}</div><div class="kpi-value mono">${pendingSubs.length}</div></div>
        <div class="kpi-card"><div class="muted-label">${esc(tr("pendingPayoutsKpi"))}</div><div class="kpi-value mono">${pendingWdrs.length}</div></div>
        <div class="kpi-card"><div class="muted-label">${esc(tr("registeredMembersKpi"))}</div><div class="kpi-value mono">${state.adminUsers.length}</div></div>
      </div>
    `;
  } else if (state.adminSubTab === "proofs") {
    state.adminSubmissions.forEach((s) => {
      const card = document.createElement("div");
      card.className = "card form-grid";
      card.innerHTML = `
        <div class="kv-row"><strong>${esc(s.taskTitle)}</strong><span class="badge">${esc(statusLabel(s.status))}</span></div>
        <div class="muted-label">${esc(tr("workerMeta", s.workerName, s.socialHandleUsed, formatTzs(s.rewardTzs)))}</div>
        <p style="margin:0;">${esc(s.proofNote)}</p>
        ${s.status === "PENDING" ? `<div class="form-row"><button class="btn btn-primary" data-act="yes">${esc(tr("approvePayBtn"))}</button><button class="btn btn-danger" data-act="no">${esc(tr("rejectBtn"))}</button></div>` : ""}
      `;
      if (s.status === "PENDING") {
        card.querySelector("[data-act=yes]").onclick = () => adminReviewSubmission(s.id, true).then(() => showToast(tr("toastApprovedRecorded"))).catch((e) => showToast(e.message));
        card.querySelector("[data-act=no]").onclick = () => adminReviewSubmission(s.id, false).then(() => showToast(tr("toastRejectedNoFee"))).catch((e) => showToast(e.message));
      }
      pane.appendChild(card);
    });
  } else if (state.adminSubTab === "payouts") {
    state.adminWithdrawals.forEach((w) => {
      const card = document.createElement("div");
      card.className = "card form-grid";
      card.innerHTML = `
        <div class="kv-row"><strong class="mono">${formatTzs(w.amountTzs)}</strong><span class="badge">${esc(statusLabel(w.status))}</span></div>
        <div class="muted-label">${esc(w.userName)} • ${esc(WITHDRAWAL_METHODS.find((m) => m.id === w.provider)?.label || w.provider)} • ${esc(w.accountNumber)}</div>
        ${w.status === "PENDING" ? `<div class="form-row"><button class="btn btn-primary" data-act="yes">${esc(tr("approveMarkPaidBtn"))}</button><button class="btn btn-danger" data-act="no">${esc(tr("declineRefundBtn"))}</button></div>` : ""}
      `;
      if (w.status === "PENDING") {
        card.querySelector("[data-act=yes]").onclick = () => adminReviewWithdrawal(w.id, true).then(() => showToast(tr("toastWithdrawPaid"))).catch((e) => showToast(e.message));
        card.querySelector("[data-act=no]").onclick = () => adminReviewWithdrawal(w.id, false).then(() => showToast(tr("toastWithdrawDeclined"))).catch((e) => showToast(e.message));
      }
      pane.appendChild(card);
    });
  } else if (state.adminSubTab === "campaigns") {
    state.adminCampaigns.forEach((c) => {
      const card = document.createElement("div");
      card.className = "card form-grid";
      card.innerHTML = `
        <div class="kv-row"><strong>${esc(c.title)}</strong><span class="badge">${esc(statusLabel(c.status))}</span></div>
        <div class="muted-label">${esc(c.creatorName)} • ${esc(actionLabel(c.actionType))} • ${esc(tr("slotsLabel", c.completedSlots || 0, c.totalSlots))}</div>
        <div class="pill-row">
          ${["PENDING_REVIEW", "PAUSED"].includes(c.status) ? `<button class="btn btn-primary btn-sm" data-st="ACTIVE">${esc(tr("approveLiveBtn"))}</button>` : ""}
          ${c.status === "ACTIVE" ? `<button class="btn btn-outline btn-sm" data-st="PAUSED">${esc(tr("pauseBtn"))}</button>` : ""}
          ${["PENDING_REVIEW", "ACTIVE", "PAUSED"].includes(c.status) ? `<button class="btn btn-danger btn-sm" data-st="REJECTED">${esc(tr("rejectRefundBtn"))}</button>` : ""}
        </div>
      `;
      card.querySelectorAll("[data-st]").forEach((btn) => {
        btn.onclick = () => adminModerateCampaign(c.id, btn.dataset.st).then(() => showToast(tr("toastCampaignUpdated"))).catch((e) => showToast(e.message));
      });
      pane.appendChild(card);
    });
  } else if (state.adminSubTab === "business") {
    const ledger = state.businessLedger.length > 0 ? state.businessLedger : state.adminCommissions;
    pane.innerHTML = `
      <div class="card form-grid">
        <div class="kv-row">
          <div>
            <h3 style="margin:0;">${esc(tr("bizAccountHeading"))}</h3>
            <p class="muted-label" style="margin:0;">${esc(tr("bizAccountSub"))}</p>
          </div>
          <button id="download-biz-csv" class="btn btn-outline btn-sm">${esc(tr("downloadCsvBtn"))}</button>
        </div>
        <div class="kpi-grid">
          <div class="kpi-card"><div class="muted-label">${esc(tr("totalBizEarnings15"))}</div><div class="kpi-value mono" style="color:var(--primary)">${formatTzs(bizCommission)}</div><div class="muted-label">${esc(tr("verifiedTxCount", ledger.length))}</div></div>
          <div class="kpi-card"><div class="muted-label">${esc(tr("pendingCommissions15"))}</div><div class="kpi-value mono" style="color:var(--amber)">${formatTzs(pendingCommissions)}</div><div class="muted-label">${esc(tr("uncompletedSlotsSub"))}</div></div>
          <div class="kpi-card"><div class="muted-label">${esc(tr("workerRewards85"))}</div><div class="kpi-value mono" style="color:var(--cobalt)">${formatTzs(workerAllocated)}</div><div class="muted-label">${esc(tr("allocatedWorkersSub"))}</div></div>
          <div class="kpi-card"><div class="muted-label">${esc(tr("customerCustodyTitle"))}</div><div class="kpi-value mono">${formatTzs(customerCustody)}</div><div class="muted-label">${esc(tr("customerCustodySub"))}</div></div>
        </div>
      </div>

      <div class="card form-grid">
        <h3 style="margin:0;">${esc(tr("officialProviderTitle"))}</h3>
        <p class="muted-label" style="margin:0;">${esc(tr("officialProviderSub"))}</p>
        <div class="pill-row" id="biz-providers">
          ${BUSINESS_PROVIDERS.map((bp) => `<button type="button" class="pill-btn ${state.selectedBizProvider === bp.id ? "active" : ""}" data-bp="${bp.id}">${esc(bp.label)}</button>`).join("")}
        </div>
        <label>${esc(tr("merchantRefLabel"))}
          <input id="biz-merchant-ref" type="text" value="${esc(state.businessAccount?.settlementMerchantRef || "")}" placeholder="e.g. TILL-TZ-901" />
        </label>
        <button id="save-biz-rail" class="btn btn-primary" type="button">${esc(tr("saveSettlementBtn"))}</button>
      </div>

      <div class="card form-grid">
        <h3 style="margin:0;">${esc(tr("completedVerifiedTxTitle", ledger.length))}</h3>
        <div class="list-stack">
          ${ledger.length === 0 ? `<p class="muted-label">${esc(tr("noVerifiedTxYet"))}</p>` : ledger.map((e) => `
            <div class="summary-box">
              <div class="kv-row"><strong>${esc(e.taskTitle)}</strong><strong class="mono" style="color:var(--primary)">+${formatTzs(e.commissionTzs)} (15%)</strong></div>
              <div class="kv-row muted-label"><span>${esc(tr("grossLabel", formatTzs(e.grossAmountTzs)))}</span><span>${esc(tr("workerShareLabel", formatTzs(e.workerRewardTzs)))}</span><span>${esc(tr("refLabel", e.referenceCode))}</span></div>
            </div>
          `).join("")}
        </div>
      </div>
    `;

    pane.querySelectorAll("#biz-providers button").forEach((b) => {
      b.onclick = () => { state.selectedBizProvider = b.dataset.bp; renderApp(); };
    });
    pane.querySelector("#save-biz-rail").onclick = () => {
      const refVal = pane.querySelector("#biz-merchant-ref").value;
      saveBusinessSettlementRail(state.selectedBizProvider, refVal)
        .then(() => showToast(tr("toastSettlementSaved")))
        .catch((e) => showToast(e.message));
    };
    pane.querySelector("#download-biz-csv").onclick = () => {
      const rows = [
        ["Reference", "Task", "Gross TZS", "Company 15% TZS", "Worker 85% TZS", "Verified"],
        ...ledger.map((l) => [l.referenceCode, `"${String(l.taskTitle).replaceAll('"', '""')}"`, l.grossAmountTzs, l.commissionTzs, l.workerRewardTzs, "true"])
      ];
      const csv = rows.map((r) => r.join(",")).join("\n");
      const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
      const a = document.createElement("a");
      a.href = URL.createObjectURL(blob);
      a.download = "razaq_task_business_report.csv";
      a.click();
    };
  }
}

function buildLanguagePillsHtml() {
  return `
    <div class="onboarding-lang-grid" id="modal-lang-grid">
      ${LANGUAGES.map((l) => {
        const active = l.code === state.lang;
        return `
          <button type="button" class="lang-card ${active ? "active" : ""}" data-mlang="${l.code}">
            <div class="lang-card-top">
              <span>${esc(l.nativeName)}</span>
              ${l.rtl ? `<span class="badge badge-amber">${esc(tr("rtlBadge"))}</span>` : ""}
            </div>
            <div class="muted-label">${esc(l.englishName)}</div>
          </button>
        `;
      }).join("")}
    </div>
  `;
}

function openLanguageModal() {
  const backdrop = document.getElementById("modal-backdrop");
  const box = document.getElementById("modal-box");
  backdrop.classList.remove("hidden");
  box.innerHTML = `
    <div class="form-grid">
      <div class="kv-row">
        <h3 style="margin:0;">${esc(tr("settingsLangTitle"))}</h3>
        <button id="close-lang-modal" class="btn btn-outline btn-sm">${esc(tr("closeBtn"))}</button>
      </div>
      <p class="muted-label" style="margin:0;">${esc(tr("settingsLangDesc"))}</p>
      ${buildLanguagePillsHtml()}
    </div>
  `;
  box.querySelector("#close-lang-modal").onclick = () => backdrop.classList.add("hidden");
  box.querySelectorAll("[data-mlang]").forEach((btn) => {
    btn.onclick = async () => {
      await changeLanguage(btn.dataset.mlang, true, true);
      backdrop.classList.add("hidden");
    };
  });
}

function openProfileModal() {
  const backdrop = document.getElementById("modal-backdrop");
  const box = document.getElementById("modal-box");
  backdrop.classList.remove("hidden");
  box.innerHTML = `
    <div class="form-grid">
      <div class="kv-row">
        <h3 style="margin:0;">${esc(tr("profileModalTitle"))}</h3>
        <button id="close-prof" class="btn btn-outline btn-sm">${esc(tr("closeBtn"))}</button>
      </div>
      <div class="muted-label">${esc(state.profile?.email || state.authUser?.email)} • ${esc(tr("roleLabel", state.profile?.role || "USER"))}</div>
      <div class="summary-box">
        <div class="muted-label">${esc(tr("profileSyncNote"))}</div>
      </div>
      <div class="summary-box">
        <strong style="font-size:14px;">${esc(tr("settingsLangTitle"))}</strong>
        <div class="muted-label" style="margin-bottom:8px;">${esc(tr("settingsLangDesc"))}</div>
        ${buildLanguagePillsHtml()}
      </div>
      <button id="signout-btn" class="btn btn-danger">${esc(tr("signOutBtn"))}</button>
    </div>
  `;
  box.querySelector("#close-prof").onclick = () => backdrop.classList.add("hidden");
  box.querySelectorAll("[data-mlang]").forEach((btn) => {
    btn.onclick = async () => {
      await changeLanguage(btn.dataset.mlang, true, true);
      openProfileModal();
    };
  });
  box.querySelector("#signout-btn").onclick = async () => {
    backdrop.classList.add("hidden");
    clearSubscriptions();
    await signOut(auth);
  };
}

document.getElementById("onboarding-continue-btn").onclick = () => {
  state.onboardingDone = true;
  localStorage.setItem(STORAGE_ONBOARDING_KEY, "1");
  localStorage.setItem(STORAGE_LANG_KEY, state.lang);
  renderApp();
};

document.getElementById("google-signin-btn").onclick = async () => {
  const errEl = document.getElementById("auth-error");
  errEl.classList.add("hidden");
  try {
    const provider = new GoogleAuthProvider();
    await signInWithPopup(auth, provider);
  } catch (err) {
    errEl.textContent = err.message || tr("errGoogleFailed");
    errEl.classList.remove("hidden");
  }
};

document.getElementById("auth-lang-btn").onclick = openLanguageModal;
document.getElementById("sidebar-lang-btn").onclick = openLanguageModal;
document.getElementById("topbar-lang-btn").onclick = openLanguageModal;
document.getElementById("theme-toggle-btn").onclick = toggleTheme;
document.getElementById("topbar-theme-btn").onclick = toggleTheme;
document.getElementById("sidebar-profile-btn").onclick = openProfileModal;
document.getElementById("topbar-profile-btn").onclick = openProfileModal;
document.getElementById("sidebar-wallet-card").onclick = () => { state.tab = "wallet"; renderApp(); };
document.getElementById("topbar-wallet-btn").onclick = () => { state.tab = "wallet"; renderApp(); };

onAuthStateChanged(auth, async (user) => {
  state.authUser = user;
  if (!user) {
    clearSubscriptions();
    state.profile = null;
    renderApp();
    return;
  }
  try {
    await ensureUserProfile(user);
    subscribeUserData(user.uid);
  } catch (err) {
    showToast(err.message || tr("errFirebaseSync"));
  }
  renderApp();
});
