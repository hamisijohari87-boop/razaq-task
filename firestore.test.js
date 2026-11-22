const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const ADMIN_UID = "admin_uid_789";
const ADMIN_EMAIL = "hamisijohari87@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied across all collections", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("users").doc(ALICE_UID).get());
  await assertFails(db.collection("tasks").where("status", "==", "ACTIVE").get());
  await assertFails(db.collection("submissions").get());
  await assertFails(db.collection("withdrawals").get());
});

test("User profile security: owner create/read/update, anti-escalation, anti-ghost-field, cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@tz.co", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@tz.co", email_verified: true }).firestore();
  const ts = pastDate(5000);

  // Valid registration profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Amina Juma",
      username: "amina_tz",
      email: "alice@tz.co",
      phoneNumber: "+255713456789",
      role: "USER",
      walletBalanceTzs: 2000,
      totalEarnedTzs: 0,
      totalSpentTzs: 0,
      tasksCompletedCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob cannot read or modify Alice's profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      fullName: "Hacked",
      updatedAt: pastDate(1000),
    })
  );

  // Self-privilege escalation & balance forgery rejected
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      fullName: "Bob Escalator",
      username: "bob_esc",
      email: "bob@tz.co",
      phoneNumber: "+255700000000",
      role: "ADMIN",
      walletBalanceTzs: 2000,
      totalEarnedTzs: 0,
      totalSpentTzs: 0,
      tasksCompletedCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      fullName: "Bob Rich",
      username: "bob_rich",
      email: "bob@tz.co",
      phoneNumber: "+255700000000",
      role: "USER",
      walletBalanceTzs: 999999,
      totalEarnedTzs: 0,
      totalSpentTzs: 0,
      tasksCompletedCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Shadow update with ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      fullName: "Amina Updated",
      isVerified: true,
      updatedAt: pastDate(1000),
    })
  );

  // Alice cannot self-increment totalEarnedTzs
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      totalEarnedTzs: 50000,
      updatedAt: pastDate(1000),
    })
  );

  // Valid owner top-up update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      walletBalanceTzs: 12000,
      updatedAt: pastDate(1000),
    })
  );

  // Valid language preference update (Kiswahili -> Arabic) succeeds; unsupported code fails
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "ar",
      updatedAt: pastDate(900),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "xx",
      updatedAt: pastDate(800),
    })
  );
});

test("Wallet transactions: user-scoped immutable ledger", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(3000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      type: "DEPOSIT_TOPUP",
      amountTzs: 10000,
      description: "M-Pesa Vodacom Top-Up",
      referenceCode: "DEP-123456",
      createdAt: ts,
    })
  );

  // User cannot forge TASK_REWARD transaction directly
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_forge").set({
      userId: ALICE_UID,
      type: "TASK_REWARD",
      amountTzs: 50000,
      description: "Forged Reward",
      referenceCode: "TSK-999999",
      createdAt: ts,
    })
  );

  // Transactions are immutable
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").update({
      amountTzs: 99999,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").delete()
  );

  // Own query succeeds; Bob cannot list Alice's transactions
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Tasks, submissions, withdrawals, email spoofing check, and admin approvals", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@tz.co", email_verified: true }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@tz.co", email_verified: true }).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const ts = pastDate(10000);

  // Seed Alice profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Amina Juma",
      username: "amina_tz",
      email: "alice@tz.co",
      phoneNumber: "+255713456789",
      role: "USER",
      walletBalanceTzs: 2000,
      totalEarnedTzs: 0,
      totalSpentTzs: 0,
      tasksCompletedCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Regular user cannot create ACTIVE campaign directly
  await assertFails(
    bobDb.collection("tasks").doc("task_bob_1").set({
      creatorUserId: BOB_UID,
      creatorName: "Baraka Musa",
      title: "Follow Baraka Media IG",
      description: "Follow our Instagram page and like the pinned reel.",
      platform: "INSTAGRAM",
      actionType: "FOLLOW",
      accountHandle: "@baraka_media",
      targetUrl: "https://instagram.com/baraka_media",
      rewardTzs: 425,
      grossPerSlotTzs: 500,
      commissionPerSlotTzs: 75,
      commissionEarnedTzs: 0,
      totalSlots: 20,
      completedSlots: 0,
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Regular user creates PENDING_REVIEW campaign (TZS 500/slot * 20 = TZS 10,000)
  await assertSucceeds(
    bobDb.collection("tasks").doc("task_bob_1").set({
      creatorUserId: BOB_UID,
      creatorName: "Baraka Musa",
      title: "Follow Baraka Media IG",
      description: "Follow our Instagram page and like the pinned reel.",
      platform: "INSTAGRAM",
      actionType: "FOLLOW",
      accountHandle: "@baraka_media",
      targetUrl: "https://instagram.com/baraka_media",
      rewardTzs: 425,
      grossPerSlotTzs: 500,
      commissionPerSlotTzs: 75,
      commissionEarnedTzs: 0,
      totalSlots: 20,
      completedSlots: 0,
      status: "PENDING_REVIEW",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Spoofed unverified admin email MUST fail to activate task
  await assertFails(
    spoofedAdminDb.collection("tasks").doc("task_bob_1").update({
      status: "ACTIVE",
      updatedAt: pastDate(5000),
    })
  );

  // Verified bootstrapped admin activates campaign
  await assertSucceeds(
    adminDb.collection("tasks").doc("task_bob_1").update({
      status: "ACTIVE",
      updatedAt: pastDate(5000),
    })
  );

  // Unfiltered tasks list fails for Alice; filtered ACTIVE query succeeds
  await assertFails(aliceDb.collection("tasks").get());
  await assertSucceeds(aliceDb.collection("tasks").where("status", "==", "ACTIVE").get());
  await assertSucceeds(bobDb.collection("tasks").where("creatorUserId", "==", BOB_UID).get());

  // Submission with forged rewardTzs fails; valid submission succeeds (85% = 425 TZS)
  await assertFails(
    aliceDb.collection("submissions").doc("sub_1").set({
      taskId: "task_bob_1",
      taskTitle: "Follow Baraka Media IG",
      platform: "INSTAGRAM",
      actionType: "FOLLOW",
      rewardTzs: 99999,
      workerUserId: ALICE_UID,
      workerName: "Amina Juma",
      socialHandleUsed: "@amina_ig",
      proofNote: "Followed and liked reel.",
      status: "PENDING",
      submittedAt: pastDate(2000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("submissions").doc("sub_1").set({
      taskId: "task_bob_1",
      taskTitle: "Follow Baraka Media IG",
      platform: "INSTAGRAM",
      actionType: "FOLLOW",
      rewardTzs: 425,
      workerUserId: ALICE_UID,
      workerName: "Amina Juma",
      socialHandleUsed: "@amina_ig",
      proofNote: "Followed and liked reel.",
      status: "PENDING",
      submittedAt: pastDate(2000),
    })
  );

  // Worker cannot self-approve submission
  await assertFails(
    aliceDb.collection("submissions").doc("sub_1").update({
      status: "APPROVED",
      reviewedAt: pastDate(500),
    })
  );

  // Admin approves submission: credits Alice 85% (425 TZS), updates campaign completedSlots & commissionEarnedTzs (75 TZS), writes commission ledger
  await assertSucceeds(
    adminDb.collection("submissions").doc("sub_1").update({
      status: "APPROVED",
      reviewerNote: "Verified",
      reviewedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    adminDb.collection("tasks").doc("task_bob_1").update({
      completedSlots: 1,
      commissionEarnedTzs: 75,
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      walletBalanceTzs: 2425,
      totalEarnedTzs: 425,
      tasksCompletedCount: 1,
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_reward_1").set({
      userId: ALICE_UID,
      type: "TASK_REWARD",
      amountTzs: 425,
      description: "Approved (85% reward): Follow Baraka Media IG",
      referenceCode: "TSK-100200",
      createdAt: pastDate(500),
    })
  );
  await assertSucceeds(
    adminDb.collection("users").doc(BOB_UID).collection("transactions").doc("tx_comm_1").set({
      userId: BOB_UID,
      type: "PLATFORM_COMMISSION",
      amountTzs: 75,
      description: "15% fee (approved task): Follow Baraka Media IG",
      referenceCode: "FEE-100200",
      createdAt: pastDate(500),
    })
  );

  // Non-admin cannot create commission records; invalid 15%/85% math is rejected
  await assertFails(
    aliceDb.collection("commissions").doc("sub_1").set({
      submissionId: "sub_1",
      taskId: "task_bob_1",
      taskTitle: "Follow Baraka Media IG",
      creatorUserId: BOB_UID,
      workerUserId: ALICE_UID,
      grossAmountTzs: 500,
      commissionTzs: 75,
      workerRewardTzs: 425,
      referenceCode: "FEE-100200",
      createdAt: pastDate(500),
    })
  );
  await assertFails(
    adminDb.collection("commissions").doc("sub_bad_math").set({
      submissionId: "sub_bad_math",
      taskId: "task_bob_1",
      taskTitle: "Follow Baraka Media IG",
      creatorUserId: BOB_UID,
      workerUserId: ALICE_UID,
      grossAmountTzs: 4000,
      commissionTzs: 1000,
      workerRewardTzs: 3000,
      referenceCode: "FEE-BAD001",
      createdAt: pastDate(500),
    })
  );

  // Verified admin creates exact 15%/85% commission ledger entry once
  await assertSucceeds(
    adminDb.collection("commissions").doc("sub_1").set({
      submissionId: "sub_1",
      taskId: "task_bob_1",
      taskTitle: "Follow Baraka Media IG",
      creatorUserId: BOB_UID,
      workerUserId: ALICE_UID,
      grossAmountTzs: 500,
      commissionTzs: 75,
      workerRewardTzs: 425,
      referenceCode: "FEE-100200",
      createdAt: pastDate(500),
    })
  );

  // Exact example from spec: TZS 100,000 gross -> TZS 15,000 commission + TZS 85,000 worker reward
  await assertSucceeds(
    adminDb.collection("commissions").doc("sub_spec_100k").set({
      submissionId: "sub_spec_100k",
      taskId: "task_bob_1",
      taskTitle: "Follow Baraka Media IG",
      creatorUserId: BOB_UID,
      workerUserId: ALICE_UID,
      grossAmountTzs: 100000,
      commissionTzs: 15000,
      workerRewardTzs: 85000,
      referenceCode: "FEE-100000",
      createdAt: pastDate(500),
    })
  );

  // Duplicate overwrite / update / delete of commission record MUST fail (immutable)
  await assertFails(
    adminDb.collection("commissions").doc("sub_1").update({
      commissionTzs: 100,
    })
  );
  await assertFails(
    adminDb.collection("commissions").doc("sub_1").delete()
  );

  // Creator Bob can read his own commission record; Alice cannot read Bob's commission record
  await assertSucceeds(bobDb.collection("commissions").where("creatorUserId", "==", BOB_UID).get());
  await assertFails(aliceDb.collection("commissions").doc("sub_1").get());
  await assertSucceeds(adminDb.collection("commissions").get());

  // Campaign budget below TZS 1,000 or above TZS 1,000,000 MUST fail
  await assertFails(
    bobDb.collection("tasks").doc("task_too_small").set({
      creatorUserId: BOB_UID,
      creatorName: "Baraka Musa",
      title: "Too Small Budget Task",
      description: "Follow our legitimate Instagram creator page today.",
      platform: "INSTAGRAM",
      actionType: "FOLLOW",
      accountHandle: "@baraka_media",
      targetUrl: "https://instagram.com/baraka_media",
      rewardTzs: 100,
      totalSlots: 5,
      completedSlots: 0,
      status: "PENDING_REVIEW",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    bobDb.collection("tasks").doc("task_too_large").set({
      creatorUserId: BOB_UID,
      creatorName: "Baraka Musa",
      title: "Too Large Budget Task",
      description: "Promote our YouTube channel with organic views.",
      platform: "YOUTUBE",
      actionType: "SUBSCRIBE",
      accountHandle: "Baraka Tech",
      targetUrl: "https://youtube.com/@barakatech",
      rewardTzs: 200000,
      totalSlots: 10,
      completedSlots: 0,
      status: "PENDING_REVIEW",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Withdrawal outside limits (< 1000 or > 1000000) or unsupported provider MUST fail
  await assertFails(
    aliceDb.collection("withdrawals").doc("wdr_too_small").set({
      userId: ALICE_UID,
      userName: "Amina Juma",
      amountTzs: 500,
      provider: "MPESA_VODACOM",
      accountNumber: "0713456789",
      accountName: "Amina Juma",
      status: "PENDING",
      requestedAt: pastDate(300),
    })
  );
  await assertFails(
    aliceDb.collection("withdrawals").doc("wdr_too_large").set({
      userId: ALICE_UID,
      userName: "Amina Juma",
      amountTzs: 1500000,
      provider: "AIRTEL_MONEY",
      accountNumber: "0788456789",
      accountName: "Amina Juma",
      status: "PENDING",
      requestedAt: pastDate(300),
    })
  );
  await assertFails(
    aliceDb.collection("withdrawals").doc("wdr_unsupported_bank").set({
      userId: ALICE_UID,
      userName: "Amina Juma",
      amountTzs: 5000,
      provider: "CRDB_BANK",
      accountNumber: "015012345678",
      accountName: "Amina Juma",
      status: "PENDING",
      requestedAt: pastDate(300),
    })
  );

  // Alice requests 1000 TZS withdrawal (exact minimum) via Mixx by Yas
  await assertSucceeds(
    aliceDb.collection("withdrawals").doc("wdr_1").set({
      userId: ALICE_UID,
      userName: "Amina Juma",
      amountTzs: 1000,
      provider: "TIGOPESA_MIXX",
      accountNumber: "0713456789",
      accountName: "Amina Juma",
      status: "PENDING",
      requestedAt: pastDate(300),
    })
  );

  // Filtered withdrawal query succeeds; unfiltered fails for Alice
  await assertFails(aliceDb.collection("withdrawals").get());
  await assertSucceeds(aliceDb.collection("withdrawals").where("userId", "==", ALICE_UID).get());

  // Admin marks withdrawal paid
  await assertSucceeds(
    adminDb.collection("withdrawals").doc("wdr_1").update({
      status: "APPROVED",
      adminNote: "Disbursed via Mixx by Yas",
      processedAt: pastDate(100),
    })
  );
});

test("RAZAK TASK BUSINESS ACCOUNT: segregated company earnings & verified-only ledger", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@tz.co", email_verified: true }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const ts = pastDate(1000);

  // Non-admin cannot read or write segregated business_account or ledger
  await assertFails(aliceDb.collection("business_account").doc("razak_main").get());
  await assertFails(
    aliceDb.collection("business_account").doc("razak_main").set({
      accountId: "razak_main",
      accountName: "RAZAK TASK BUSINESS ACCOUNT",
      totalCommissionEarnedTzs: 15000,
      totalWorkerRewardsAllocatedTzs: 85000,
      totalGrossVerifiedVolumeTzs: 100000,
      completedTransactionsCount: 1,
      settlementProviderCode: "SELCOM_BUSINESS_TZ",
      settlementMerchantRef: "TILL-001",
      updatedAt: ts,
    })
  );
  await assertFails(
    aliceDb.collection("business_account").doc("razak_main").collection("ledger").doc("sub_1").get()
  );

  // Admin creates business_account summary document with exact 15% + 85% = 100% totals
  await assertSucceeds(
    adminDb.collection("business_account").doc("razak_main").set({
      accountId: "razak_main",
      accountName: "RAZAK TASK BUSINESS ACCOUNT",
      totalCommissionEarnedTzs: 15000,
      totalWorkerRewardsAllocatedTzs: 85000,
      totalGrossVerifiedVolumeTzs: 100000,
      completedTransactionsCount: 1,
      settlementProviderCode: "SELCOM_BUSINESS_TZ",
      settlementMerchantRef: "MERCH-TZ-901",
      updatedAt: ts,
    })
  );

  // Totals mismatch fails
  await assertFails(
    adminDb.collection("business_account").doc("razak_main").update({
      totalCommissionEarnedTzs: 20000,
      totalGrossVerifiedVolumeTzs: 100000,
      updatedAt: pastDate(500),
    })
  );

  // Ledger requires verified: true and exact 15%/85% math
  await assertFails(
    adminDb.collection("business_account").doc("razak_main").collection("ledger").doc("sub_unverified").set({
      submissionId: "sub_unverified",
      taskId: "task_1",
      taskTitle: "Campaign A",
      creatorUserId: BOB_UID,
      workerUserId: ALICE_UID,
      grossAmountTzs: 100000,
      commissionTzs: 15000,
      workerRewardTzs: 85000,
      referenceCode: "BIZ-001",
      verified: false,
      settlementChannel: "ESCROW_VERIFIED_SPLIT",
      createdAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("business_account").doc("razak_main").collection("ledger").doc("sub_verified_1").set({
      submissionId: "sub_verified_1",
      taskId: "task_1",
      taskTitle: "Campaign A",
      creatorUserId: BOB_UID,
      workerUserId: ALICE_UID,
      grossAmountTzs: 100000,
      commissionTzs: 15000,
      workerRewardTzs: 85000,
      referenceCode: "BIZ-001",
      verified: true,
      settlementChannel: "ESCROW_VERIFIED_SPLIT",
      createdAt: ts,
    })
  );

  // Immutable ledger entries cannot be modified or deleted
  await assertFails(
    adminDb.collection("business_account").doc("razak_main").collection("ledger").doc("sub_verified_1").update({
      commissionTzs: 99999,
    })
  );
  await assertFails(
    adminDb.collection("business_account").doc("razak_main").collection("ledger").doc("sub_verified_1").delete()
  );
});
