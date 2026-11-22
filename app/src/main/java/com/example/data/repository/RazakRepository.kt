package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.local.SecurityUtils
import com.example.data.local.SessionStore
import com.example.data.model.BusinessAccountEntity
import com.example.data.model.BusinessLedgerEntryEntity
import com.example.data.model.PayoutProvider
import com.example.data.model.PlatformCommissionEntity
import com.example.data.model.SocialPlatform
import com.example.data.model.SocialTaskEntity
import com.example.data.model.SubmissionStatus
import com.example.data.model.TaskActionType
import com.example.data.model.TaskCampaignStatus
import com.example.data.model.TaskSubmissionEntity
import com.example.data.model.TransactionType
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.WalletTransactionEntity
import com.example.data.model.WithdrawalRequestEntity
import com.example.data.model.WithdrawalStatus
import com.example.data.remote.BusinessSettlementGateway
import com.example.data.remote.BusinessSettlementProvider
import com.example.data.remote.MobileMoneyDisbursementRequest
import com.example.data.remote.OperationType
import com.example.data.remote.PaymentProviderGateway
import com.example.data.remote.UnconfiguredMobileMoneyGateway
import com.example.data.remote.UnconfiguredTzBusinessGateway
import com.example.data.remote.handleFirestoreError
import com.example.i18n.AppLanguage
import com.example.i18n.LocalizedAppException
import com.example.util.CurrencyFormatter
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

class RazakRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val sessionStore: SessionStore,
    private val paymentProviderGateway: PaymentProviderGateway = UnconfiguredMobileMoneyGateway(),
    val businessSettlementGateway: BusinessSettlementGateway = UnconfiguredTzBusinessGateway()
) {
    private val withdrawalMutex = Mutex()

    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
        sessionStore = SessionStore(context.applicationContext),
        paymentProviderGateway = UnconfiguredMobileMoneyGateway()
    )

    val darkThemeFlow: Flow<Boolean?> = sessionStore.darkThemeFlow
    val languageCodeFlow: Flow<String> = sessionStore.languageCodeFlow
    val onboardingCompletedFlow: Flow<Boolean> = sessionStore.onboardingCompletedFlow

    suspend fun setDarkTheme(enabled: Boolean?) {
        sessionStore.setDarkTheme(enabled)
    }

    suspend fun setLanguageCode(code: String) {
        sessionStore.setLanguageCode(code)
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        sessionStore.setOnboardingCompleted(completed)
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw LocalizedAppException(
                resolver = { it.errSignInRequired },
                fallbackEnglish = "User must be signed in with Google before accessing Firestore."
            )
    }

    private fun isAdminUser(firebaseUser: FirebaseUser?): Boolean {
        return firebaseUser != null &&
            firebaseUser.isEmailVerified &&
            firebaseUser.email.equals(ADMIN_EMAIL, ignoreCase = true)
    }

    suspend fun ensureUserProfile(firebaseUser: FirebaseUser): Result<UserEntity> {
        val uid = firebaseUser.uid
        val docPath = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val existing = snapshot.toObject(UserEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                    ?: throw LocalizedAppException({ it.errProfileRead }, "Unable to read user profile.")
                val savedCode = AppLanguage.fromCode(existing.preferredLanguage).code
                sessionStore.setLanguageCode(savedCode)
                Result.success(existing)
            } else {
                val email = (firebaseUser.email ?: "$uid@razaktask.co.tz").take(120)
                val rawName = firebaseUser.displayName?.trim().takeIf { !it.isNullOrBlank() }
                    ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
                val cleanName = rawName.take(80)
                val baseUsername = email.substringBefore("@")
                    .replace(Regex("[^a-zA-Z0-9_]"), "_")
                    .take(30)
                    .ifBlank { "member_${uid.take(6)}" }
                val role = if (isAdminUser(firebaseUser)) UserRole.ADMIN.name else UserRole.USER.name
                val starterBonus = 2_000L
                val initialLangCode = AppLanguage.fromCode(
                    sessionStore.languageCodeFlow.firstOrNull()
                ).code

                val batch = db.batch()
                val profilePayload = mapOf(
                    "userId" to uid,
                    "fullName" to cleanName,
                    "username" to baseUsername,
                    "email" to email,
                    "phoneNumber" to "",
                    "preferredLanguage" to initialLangCode,
                    "role" to role,
                    "walletBalanceTzs" to starterBonus,
                    "totalEarnedTzs" to 0L,
                    "totalSpentTzs" to 0L,
                    "tasksCompletedCount" to 0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, profilePayload)

                val txRef = docRef.collection("transactions").document()
                batch.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "type" to TransactionType.DEPOSIT_TOPUP.name,
                        "amountTzs" to starterBonus,
                        "description" to "Welcome Starter Bonus",
                        "referenceCode" to SecurityUtils.generateReferenceCode("WLC"),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.commit().await()

                val createdSnap = docRef.get().await()
                val created = createdSnap.toObject(UserEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                    ?: UserEntity(
                        id = uid,
                        userId = uid,
                        fullName = cleanName,
                        username = baseUsername,
                        email = email,
                        role = role,
                        preferredLanguage = initialLangCode,
                        walletBalanceTzs = starterBonus
                    )
                Result.success(created)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePreferredLanguage(
        userId: String,
        language: AppLanguage
    ): Result<Unit> {
        sessionStore.setLanguageCode(language.code)
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            if (uid != userId) {
                throw LocalizedAppException({ it.errSignedInMismatch }, "Signed-in user mismatch.")
            }
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "preferredLanguage" to language.code,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeUser(userId: String): Flow<UserEntity?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .catch { error ->
                logFlowError(error, OperationType.GET, path, userId)
                throw error
            }
    }

    fun observeMarketplaceTasks(userId: String): Flow<List<SocialTaskEntity>> {
        val path = "tasks"
        return db.collection("tasks")
            .whereEqualTo("status", TaskCampaignStatus.ACTIVE.name)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SocialTaskEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                    .filter { it.completedSlots < it.totalSlots }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeUserCampaigns(userId: String): Flow<List<SocialTaskEntity>> {
        val path = "tasks"
        return db.collection("tasks")
            .whereEqualTo("creatorUserId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SocialTaskEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeUserSubmissions(userId: String): Flow<List<TaskSubmissionEntity>> {
        val path = "submissions"
        return db.collection("submissions")
            .whereEqualTo("workerUserId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(TaskSubmissionEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.submittedAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeUserTransactions(userId: String): Flow<List<WalletTransactionEntity>> {
        val path = "users/$userId/transactions"
        return db.collection("users").document(userId).collection("transactions")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(WalletTransactionEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeUserWithdrawals(userId: String): Flow<List<WithdrawalRequestEntity>> {
        val path = "withdrawals"
        return db.collection("withdrawals")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(WithdrawalRequestEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.requestedAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeAllUsers(adminId: String): Flow<List<UserEntity>> {
        val path = "users"
        return db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, adminId)
                throw error
            }
    }

    fun observeAllCampaigns(adminId: String): Flow<List<SocialTaskEntity>> {
        val path = "tasks"
        return db.collection("tasks")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SocialTaskEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, adminId)
                throw error
            }
    }

    fun observeAllSubmissions(adminId: String): Flow<List<TaskSubmissionEntity>> {
        val path = "submissions"
        return db.collection("submissions")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(TaskSubmissionEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.submittedAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, adminId)
                throw error
            }
    }

    fun observeAllWithdrawals(adminId: String): Flow<List<WithdrawalRequestEntity>> {
        val path = "withdrawals"
        return db.collection("withdrawals")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(WithdrawalRequestEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.requestedAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, adminId)
                throw error
            }
    }

    fun observeAllCommissions(adminId: String): Flow<List<PlatformCommissionEntity>> {
        val path = "commissions"
        return db.collection("commissions")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PlatformCommissionEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, adminId)
                throw error
            }
    }

    fun observeBusinessAccount(adminId: String): Flow<BusinessAccountEntity?> {
        val path = "business_account/$BUSINESS_ACCOUNT_ID"
        return db.collection("business_account").document(BUSINESS_ACCOUNT_ID)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(BusinessAccountEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .catch { error ->
                logFlowError(error, OperationType.GET, path, adminId)
                throw error
            }
    }

    fun observeBusinessLedger(adminId: String): Flow<List<BusinessLedgerEntryEntity>> {
        val path = "business_account/$BUSINESS_ACCOUNT_ID/ledger"
        return db.collection("business_account")
            .document(BUSINESS_ACCOUNT_ID)
            .collection("ledger")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(BusinessLedgerEntryEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, adminId)
                throw error
            }
    }

    suspend fun configureBusinessSettlementProvider(
        provider: BusinessSettlementProvider,
        merchantRef: String
    ): Result<Unit> {
        val cleanRef = merchantRef.trim().take(80)
        val path = "business_account/$BUSINESS_ACCOUNT_ID"
        return try {
            requireUserId()
            val bizRef = db.collection("business_account").document(BUSINESS_ACCOUNT_ID)
            db.runTransaction { transaction ->
                val bizSnap = transaction.get(bizRef)
                val existing = if (bizSnap.exists()) {
                    bizSnap.toObject(BusinessAccountEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                } else null

                if (existing == null) {
                    transaction.set(
                        bizRef,
                        mapOf(
                            "accountId" to BUSINESS_ACCOUNT_ID,
                            "accountName" to BUSINESS_ACCOUNT_NAME,
                            "totalCommissionEarnedTzs" to 0L,
                            "totalWorkerRewardsAllocatedTzs" to 0L,
                            "totalGrossVerifiedVolumeTzs" to 0L,
                            "completedTransactionsCount" to 0,
                            "settlementProviderCode" to provider.code,
                            "settlementMerchantRef" to cleanRef,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    transaction.update(
                        bizRef,
                        mapOf(
                            "settlementProviderCode" to provider.code,
                            "settlementMerchantRef" to cleanRef,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        userId: String,
        fullName: String,
        username: String,
        phoneNumber: String
    ): Result<Unit> {
        val cleanName = fullName.trim()
        val cleanUsername = username.trim().removePrefix("@")
        val cleanPhone = phoneNumber.trim()
        if (cleanName.isEmpty() || cleanName.length > 80) {
            return Result.failure(LocalizedAppException({ it.errFullNameLength }, "Full name must be 1 to 80 characters."))
        }
        if (cleanUsername.length < 2 || cleanUsername.length > 40) {
            return Result.failure(LocalizedAppException({ it.errUsernameLength }, "Username must be 2 to 40 characters."))
        }
        if (cleanPhone.length > 30) {
            return Result.failure(LocalizedAppException({ it.errPhoneTooLong }, "Phone number is too long."))
        }

        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            if (uid != userId) {
                throw LocalizedAppException({ it.errSignedInMismatch }, "Signed-in user mismatch.")
            }
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "fullName" to cleanName,
                        "username" to cleanUsername,
                        "phoneNumber" to cleanPhone,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun submitTaskProof(
        workerId: String,
        workerName: String,
        taskId: String,
        socialHandleUsed: String,
        proofNote: String,
        proofImageUri: String?
    ): Result<Unit> {
        val cleanHandle = socialHandleUsed.trim()
        val cleanNote = proofNote.trim()
        if (cleanHandle.length < 2 || cleanHandle.length > 80) {
            return Result.failure(LocalizedAppException({ it.errHandleLength }, "Enter the social handle you used (2–80 characters)."))
        }
        if (cleanNote.length < 4 || cleanNote.length > 500) {
            return Result.failure(LocalizedAppException({ it.errProofNoteLength }, "Provide a verification note (4–500 characters)."))
        }

        val path = "submissions"
        return try {
            val uid = requireUserId()
            if (uid != workerId) {
                throw LocalizedAppException({ it.errSignedInMismatch }, "Signed-in user mismatch.")
            }

            val taskSnap = db.collection("tasks").document(taskId).get().await()
            val task = taskSnap.toObject(SocialTaskEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                ?: throw LocalizedAppException({ it.errTaskNotFound }, "Task no longer exists.")

            if (task.resolvedStatus() != TaskCampaignStatus.ACTIVE) {
                throw LocalizedAppException({ it.errTaskNotActive }, "This task is no longer active.")
            }
            if (task.completedSlots >= task.totalSlots) {
                throw LocalizedAppException({ it.errTaskFull }, "All slots for this task have been filled.")
            }
            if (task.creatorUserId == uid) {
                throw LocalizedAppException({ it.errOwnTaskProof }, "You cannot submit proof for your own campaign.")
            }

            val existingSubs = db.collection("submissions")
                .whereEqualTo("workerUserId", uid)
                .get()
                .await()
                .toObjects(TaskSubmissionEntity::class.java, ServerTimestampBehavior.ESTIMATE)

            val duplicate = existingSubs.firstOrNull {
                it.taskId == taskId && it.resolvedStatus() != SubmissionStatus.REJECTED
            }
            if (duplicate != null) {
                throw LocalizedAppException({ it.errDuplicateSubmission }, "You already have a submission for this task.")
            }

            val subRef = db.collection("submissions").document()
            val payload = mapOf(
                "taskId" to task.id,
                "taskTitle" to task.title.take(120),
                "platform" to task.platform,
                "actionType" to task.actionType,
                "rewardTzs" to task.rewardTzs,
                "workerUserId" to uid,
                "workerName" to workerName.trim().ifBlank { "Member" }.take(80),
                "socialHandleUsed" to cleanHandle,
                "proofNote" to cleanNote,
                "proofImageUri" to proofImageUri?.takeIf { it.isNotBlank() && it.length <= 600 },
                "status" to SubmissionStatus.PENDING.name,
                "submittedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            subRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createPromotionCampaign(
        creatorUserId: String,
        title: String,
        description: String,
        platform: SocialPlatform,
        actionType: TaskActionType,
        accountHandle: String,
        targetUrl: String,
        rewardTzs: Long,
        totalSlots: Int
    ): Result<Unit> {
        val cleanTitle = title.trim()
        val cleanDesc = description.trim()
        val cleanHandle = accountHandle.trim()
        val cleanUrl = targetUrl.trim()

        if (cleanTitle.length < 5 || cleanTitle.length > 120) {
            return Result.failure(LocalizedAppException({ it.errCampaignTitleLength }, "Campaign title must be 5–120 characters."))
        }
        if (cleanDesc.length < 10 || cleanDesc.length > 600) {
            return Result.failure(LocalizedAppException({ it.errCampaignDescLength }, "Instructions must be 10–600 characters."))
        }
        if (cleanHandle.length < 2 || cleanHandle.length > 80) {
            return Result.failure(LocalizedAppException({ it.errCampaignHandleLength }, "Enter your target account handle (2–80 characters)."))
        }
        if ((!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) || cleanUrl.length > 300) {
            return Result.failure(LocalizedAppException({ it.errTargetUrlInvalid }, "Target link must start with https:// or http://."))
        }
        if (rewardTzs < 100L || rewardTzs > 500_000L) {
            return Result.failure(LocalizedAppException({ it.errRewardSlotRange }, "Reward per task must be between TZS 100 and TZS 500,000."))
        }
        if (totalSlots < 5 || totalSlots > 10_000) {
            return Result.failure(LocalizedAppException({ it.errSlotsRange }, "Campaign slots must be between 5 and 10,000."))
        }

        val totalCost = rewardTzs * totalSlots
        if (totalCost < MIN_CAMPAIGN_BUDGET_TZS) {
            return Result.failure(
                LocalizedAppException(
                    resolver = { it.errMinCampaignBudgetFmt.format(CurrencyFormatter.formatTzs(MIN_CAMPAIGN_BUDGET_TZS), CurrencyFormatter.formatTzs(totalCost)) },
                    fallbackEnglish = "Minimum campaign budget is ${CurrencyFormatter.formatTzs(MIN_CAMPAIGN_BUDGET_TZS)}."
                )
            )
        }
        if (totalCost > MAX_CAMPAIGN_BUDGET_TZS) {
            return Result.failure(
                LocalizedAppException(
                    resolver = { it.errMaxCampaignBudgetFmt.format(CurrencyFormatter.formatTzs(MAX_CAMPAIGN_BUDGET_TZS), CurrencyFormatter.formatTzs(totalCost)) },
                    fallbackEnglish = "Maximum campaign budget is ${CurrencyFormatter.formatTzs(MAX_CAMPAIGN_BUDGET_TZS)}."
                )
            )
        }

        val (commissionPerSlot, workerRewardPerSlot) = computeCommissionSplit(rewardTzs)
        val path = "tasks"
        return try {
            val uid = requireUserId()
            if (uid != creatorUserId) {
                throw LocalizedAppException({ it.errSignedInMismatch }, "Signed-in user mismatch.")
            }
            val creatorRef = db.collection("users").document(uid)
            val taskRef = db.collection("tasks").document()
            val txRef = creatorRef.collection("transactions").document()
            val isAdmin = isAdminUser(auth.currentUser)

            db.runTransaction { transaction ->
                val creatorSnap = transaction.get(creatorRef)
                val creator = creatorSnap.toObject(UserEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                    ?: throw LocalizedAppException({ it.errProfileRead }, "User profile not found.")

                if (creator.walletBalanceTzs < totalCost) {
                    throw LocalizedAppException(
                        resolver = { it.errInsufficientBalanceFmt.format(CurrencyFormatter.formatTzs(totalCost), CurrencyFormatter.formatTzs(creator.walletBalanceTzs)) },
                        fallbackEnglish = "Insufficient balance."
                    )
                }

                val initialStatus = if (isAdmin || creator.resolvedRole() == UserRole.ADMIN) {
                    TaskCampaignStatus.ACTIVE.name
                } else {
                    TaskCampaignStatus.PENDING_REVIEW.name
                }

                transaction.update(
                    creatorRef,
                    mapOf(
                        "walletBalanceTzs" to creator.walletBalanceTzs - totalCost,
                        "totalSpentTzs" to creator.totalSpentTzs + totalCost,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                transaction.set(
                    taskRef,
                    mapOf(
                        "creatorUserId" to uid,
                        "creatorName" to creator.fullName.take(80),
                        "title" to cleanTitle,
                        "description" to cleanDesc,
                        "platform" to platform.name,
                        "actionType" to actionType.name,
                        "accountHandle" to cleanHandle,
                        "targetUrl" to cleanUrl,
                        "rewardTzs" to workerRewardPerSlot,
                        "grossPerSlotTzs" to rewardTzs,
                        "commissionPerSlotTzs" to commissionPerSlot,
                        "commissionEarnedTzs" to 0L,
                        "totalSlots" to totalSlots,
                        "completedSlots" to 0,
                        "status" to initialStatus,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                transaction.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "type" to TransactionType.CAMPAIGN_FUND.name,
                        "amountTzs" to totalCost,
                        "description" to "Campaign escrow: $cleanTitle ($totalSlots slots • 15% fee only on approved tasks)".take(200),
                        "referenceCode" to SecurityUtils.generateReferenceCode("CMP"),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun depositToWallet(
        userId: String,
        amountTzs: Long,
        provider: PayoutProvider,
        phoneOrReference: String
    ): Result<Unit> {
        val cleanRef = phoneOrReference.trim()
        if (amountTzs < 1_000L || amountTzs > 50_000_000L) {
            return Result.failure(LocalizedAppException({ it.errTopUpRange }, "Top-up amount must be between TZS 1,000 and TZS 50,000,000."))
        }
        if (cleanRef.length < 6) {
            return Result.failure(LocalizedAppException({ it.errValidMobileOrRef }, "Enter a valid mobile number or transaction reference."))
        }

        val docPath = "users/$userId/transactions"
        return try {
            val uid = requireUserId()
            if (uid != userId) {
                throw LocalizedAppException({ it.errSignedInMismatch }, "Signed-in user mismatch.")
            }
            val userRef = db.collection("users").document(uid)
            val txRef = userRef.collection("transactions").document()

            db.runTransaction { transaction ->
                val userSnap = transaction.get(userRef)
                val user = userSnap.toObject(UserEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                    ?: throw LocalizedAppException({ it.errProfileRead }, "User profile not found.")
                transaction.update(
                    userRef,
                    mapOf(
                        "walletBalanceTzs" to user.walletBalanceTzs + amountTzs,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "type" to TransactionType.DEPOSIT_TOPUP.name,
                        "amountTzs" to amountTzs,
                        "description" to "${provider.displayName} Top-Up ($cleanRef)".take(200),
                        "referenceCode" to SecurityUtils.generateReferenceCode("DEP"),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun requestWithdrawal(
        userId: String,
        amountTzs: Long,
        provider: PayoutProvider,
        accountNumber: String,
        accountName: String
    ): Result<Unit> {
        if (provider !in PayoutProvider.withdrawalMethods) {
            return Result.failure(LocalizedAppException({ it.errSelectWithdrawalMethod }, "Select M-Pesa, Airtel Money, Mixx by Yas, or HaloPesa."))
        }
        if (amountTzs < MIN_WITHDRAWAL_TZS || amountTzs > MAX_WITHDRAWAL_TZS) {
            return Result.failure(
                LocalizedAppException(
                    resolver = { it.errWithdrawalRangeFmt.format(CurrencyFormatter.formatTzs(MIN_WITHDRAWAL_TZS), CurrencyFormatter.formatTzs(MAX_WITHDRAWAL_TZS)) },
                    fallbackEnglish = "Withdrawal amount must be between ${CurrencyFormatter.formatTzs(MIN_WITHDRAWAL_TZS)} and ${CurrencyFormatter.formatTzs(MAX_WITHDRAWAL_TZS)}."
                )
            )
        }
        val cleanNumber = paymentProviderGateway.validateAndNormalizeTzMobileNumber(accountNumber)
            .getOrElse { return Result.failure(it) }
        val cleanName = accountName.trim()
        if (cleanName.length < 2 || cleanName.length > 80) {
            return Result.failure(LocalizedAppException({ it.errRecipientNameLength }, "Enter the registered recipient account name."))
        }

        val path = "withdrawals"
        return withdrawalMutex.withLock {
            try {
                val uid = requireUserId()
                if (uid != userId) {
                    throw LocalizedAppException({ it.errSignedInMismatch }, "Signed-in user mismatch.")
                }

                // Prevent duplicate withdrawal requests while a pending request exists
                val pendingSnapshot = db.collection("withdrawals")
                    .whereEqualTo("userId", uid)
                    .whereEqualTo("status", WithdrawalStatus.PENDING.name)
                    .limit(1)
                    .get()
                    .await()
                if (!pendingSnapshot.isEmpty) {
                    throw LocalizedAppException(
                        resolver = { it.errDuplicateWithdrawal },
                        fallbackEnglish = "Duplicate withdrawal blocked: you already have a pending withdrawal request awaiting admin approval."
                    )
                }

                val userRef = db.collection("users").document(uid)
                val withdrawalRef = db.collection("withdrawals").document()
                val txRef = userRef.collection("transactions").document()

                val dispatchResult = paymentProviderGateway.prepareDisbursement(
                    MobileMoneyDisbursementRequest(
                        withdrawalId = withdrawalRef.id,
                        userId = uid,
                        provider = provider,
                        normalizedMsisdn = cleanNumber,
                        recipientName = cleanName,
                        amountTzs = amountTzs
                    )
                )

                db.runTransaction { transaction ->
                    val userSnap = transaction.get(userRef)
                    val user = userSnap.toObject(UserEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                        ?: throw LocalizedAppException({ it.errProfileRead }, "User profile not found.")
                    if (user.walletBalanceTzs < amountTzs) {
                        throw LocalizedAppException(
                            resolver = { it.errExceedsBalanceFmt.format(CurrencyFormatter.formatTzs(user.walletBalanceTzs)) },
                            fallbackEnglish = "Insufficient balance (${CurrencyFormatter.formatTzs(user.walletBalanceTzs)} available)."
                        )
                    }

                    transaction.update(
                        userRef,
                        mapOf(
                            "walletBalanceTzs" to user.walletBalanceTzs - amountTzs,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val withdrawalPayload = mapOf(
                        "userId" to uid,
                        "userName" to user.fullName.take(80),
                        "amountTzs" to amountTzs,
                        "provider" to provider.name,
                        "accountNumber" to cleanNumber,
                        "accountName" to cleanName,
                        "status" to WithdrawalStatus.PENDING.name,
                        "requestedAt" to FieldValue.serverTimestamp()
                    )
                    transaction.set(withdrawalRef, withdrawalPayload)
                    transaction.set(
                        txRef,
                        mapOf(
                            "userId" to uid,
                            "type" to TransactionType.WITHDRAWAL.name,
                            "amountTzs" to amountTzs,
                            "description" to "Withdrawal via ${provider.displayName} ($cleanNumber)".take(200),
                            "referenceCode" to SecurityUtils.generateReferenceCode("WDR"),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }.await()
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, path)
                Result.failure(e)
            }
        }
    }

    suspend fun reviewSubmission(
        submissionId: String,
        approve: Boolean,
        note: String
    ): Result<Unit> {
        val path = "submissions/$submissionId"
        return try {
            requireUserId()
            val subRef = db.collection("submissions").document(submissionId)
            val commissionRef = db.collection("commissions").document(submissionId)
            val businessAccountRef = db.collection("business_account").document(BUSINESS_ACCOUNT_ID)
            val businessLedgerRef = businessAccountRef.collection("ledger").document(submissionId)

            db.runTransaction { transaction ->
                // All Firestore reads MUST execute before any writes
                val subSnap = transaction.get(subRef)
                val submission = subSnap.toObject(TaskSubmissionEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                    ?: throw LocalizedAppException({ it.errSubmissionNotFound }, "Submission not found.")
                if (submission.resolvedStatus() != SubmissionStatus.PENDING) {
                    throw LocalizedAppException({ it.errSubmissionReviewed }, "Submission already reviewed.")
                }

                if (approve) {
                    val existingCommissionSnap = transaction.get(commissionRef)
                    val existingBusinessLedgerSnap = transaction.get(businessLedgerRef)
                    if (existingCommissionSnap.exists() || existingBusinessLedgerSnap.exists()) {
                        throw LocalizedAppException({ it.errDuplicatePayout }, "Duplicate payout blocked: submission already processed.")
                    }

                    val workerRef = db.collection("users").document(submission.workerUserId)
                    val taskRef = db.collection("tasks").document(submission.taskId)
                    val workerSnap = transaction.get(workerRef)
                    val worker = workerSnap.toObject(UserEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                        ?: throw LocalizedAppException({ it.errWorkerMissing }, "Worker profile not found.")
                    val taskSnap = transaction.get(taskRef)
                    val task = taskSnap.toObject(SocialTaskEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                    val bizSnap = transaction.get(businessAccountRef)
                    val existingBiz = if (bizSnap.exists()) {
                        bizSnap.toObject(BusinessAccountEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                    } else null

                    if (task != null && task.completedSlots >= task.totalSlots) {
                        throw LocalizedAppException({ it.errAllSlotsDone }, "All campaign slots are already completed.")
                    }

                    val grossPerSlot = task?.resolvedGrossPerSlotTzs() ?: submission.rewardTzs
                    val workerReward = if (task != null && task.grossPerSlotTzs > 0L) {
                        submission.rewardTzs
                    } else {
                        computeCommissionSplit(grossPerSlot).second
                    }
                    val commissionAmount = (grossPerSlot - workerReward).coerceAtLeast(0L)

                    transaction.update(
                        subRef,
                        mapOf(
                            "status" to SubmissionStatus.APPROVED.name,
                            "reviewerNote" to note.trim().ifBlank { "Proof verified and approved." }.take(300),
                            "reviewedAt" to FieldValue.serverTimestamp()
                        )
                    )

                    if (task != null) {
                        val updatedSlots = (task.completedSlots + 1).coerceAtMost(task.totalSlots)
                        val updatedStatus = if (updatedSlots >= task.totalSlots) {
                            TaskCampaignStatus.COMPLETED.name
                        } else {
                            task.status
                        }
                        transaction.update(
                            taskRef,
                            mapOf(
                                "completedSlots" to updatedSlots,
                                "commissionEarnedTzs" to task.commissionEarnedTzs + commissionAmount,
                                "status" to updatedStatus,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }

                    transaction.update(
                        workerRef,
                        mapOf(
                            "walletBalanceTzs" to worker.walletBalanceTzs + workerReward,
                            "totalEarnedTzs" to worker.totalEarnedTzs + workerReward,
                            "tasksCompletedCount" to worker.tasksCompletedCount + 1,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )

                    val workerTxRef = workerRef.collection("transactions").document()
                    transaction.set(
                        workerTxRef,
                        mapOf(
                            "userId" to worker.userId,
                            "type" to TransactionType.TASK_REWARD.name,
                            "amountTzs" to workerReward,
                            "description" to "Approved (85% reward): ${submission.taskTitle}".take(200),
                            "referenceCode" to SecurityUtils.generateReferenceCode("TSK"),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )

                    val refCode = SecurityUtils.generateReferenceCode("BIZ")
                    val creatorUserId = task?.creatorUserId ?: worker.userId

                    transaction.set(
                        commissionRef,
                        mapOf(
                            "submissionId" to submissionId,
                            "taskId" to submission.taskId,
                            "taskTitle" to submission.taskTitle.take(120),
                            "creatorUserId" to creatorUserId,
                            "workerUserId" to worker.userId,
                            "grossAmountTzs" to grossPerSlot,
                            "commissionTzs" to commissionAmount,
                            "workerRewardTzs" to workerReward,
                            "referenceCode" to refCode,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )

                    // Record segregated 15% company earnings ONLY after verified approval
                    transaction.set(
                        businessLedgerRef,
                        mapOf(
                            "submissionId" to submissionId,
                            "taskId" to submission.taskId,
                            "taskTitle" to submission.taskTitle.take(120),
                            "creatorUserId" to creatorUserId,
                            "workerUserId" to worker.userId,
                            "grossAmountTzs" to grossPerSlot,
                            "commissionTzs" to commissionAmount,
                            "workerRewardTzs" to workerReward,
                            "referenceCode" to refCode,
                            "verified" to true,
                            "settlementChannel" to "ESCROW_VERIFIED_SPLIT",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )

                    val nextBizCommission = (existingBiz?.totalCommissionEarnedTzs ?: 0L) + commissionAmount
                    val nextBizWorkerAllocated = (existingBiz?.totalWorkerRewardsAllocatedTzs ?: 0L) + workerReward
                    val nextBizGross = (existingBiz?.totalGrossVerifiedVolumeTzs ?: 0L) + grossPerSlot
                    val nextBizCount = (existingBiz?.completedTransactionsCount ?: 0) + 1
                    val bizPayload = mapOf(
                        "accountId" to BUSINESS_ACCOUNT_ID,
                        "accountName" to (existingBiz?.accountName?.ifBlank { BUSINESS_ACCOUNT_NAME } ?: BUSINESS_ACCOUNT_NAME),
                        "totalCommissionEarnedTzs" to nextBizCommission,
                        "totalWorkerRewardsAllocatedTzs" to nextBizWorkerAllocated,
                        "totalGrossVerifiedVolumeTzs" to nextBizGross,
                        "completedTransactionsCount" to nextBizCount,
                        "settlementProviderCode" to (existingBiz?.settlementProviderCode ?: BusinessSettlementProvider.UNCONFIGURED.code),
                        "settlementMerchantRef" to (existingBiz?.settlementMerchantRef ?: ""),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (existingBiz == null) {
                        transaction.set(businessAccountRef, bizPayload)
                    } else {
                        transaction.update(businessAccountRef, bizPayload)
                    }

                    if (commissionAmount >= 15L && task != null && task.creatorUserId.isNotBlank()) {
                        val creatorTxRef = db.collection("users")
                            .document(task.creatorUserId)
                            .collection("transactions")
                            .document()
                        transaction.set(
                            creatorTxRef,
                            mapOf(
                                "userId" to task.creatorUserId,
                                "type" to TransactionType.PLATFORM_COMMISSION.name,
                                "amountTzs" to commissionAmount,
                                "description" to "15% fee (approved task): ${submission.taskTitle}".take(200),
                                "referenceCode" to refCode,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                } else {
                    // Never deduct commission or reward on rejected tasks
                    transaction.update(
                        subRef,
                        mapOf(
                            "status" to SubmissionStatus.REJECTED.name,
                            "reviewerNote" to note.trim().ifBlank { "Proof could not be verified." }.take(300),
                            "reviewedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun reviewWithdrawal(
        withdrawalId: String,
        approve: Boolean,
        note: String
    ): Result<Unit> {
        val path = "withdrawals/$withdrawalId"
        return try {
            requireUserId()
            val withdrawalRef = db.collection("withdrawals").document(withdrawalId)

            db.runTransaction { transaction ->
                val wSnap = transaction.get(withdrawalRef)
                val withdrawal = wSnap.toObject(WithdrawalRequestEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                    ?: throw LocalizedAppException({ it.errWithdrawalNotFound }, "Withdrawal request not found.")
                if (withdrawal.resolvedStatus() != WithdrawalStatus.PENDING) {
                    throw LocalizedAppException({ it.errWithdrawalProcessed }, "Withdrawal already processed.")
                }

                if (approve) {
                    transaction.update(
                        withdrawalRef,
                        mapOf(
                            "status" to WithdrawalStatus.APPROVED.name,
                            "adminNote" to note.trim().ifBlank { "Disbursed via ${withdrawal.resolvedProvider().displayName}" }.take(300),
                            "processedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    val userRef = db.collection("users").document(withdrawal.userId)
                    val userSnap = transaction.get(userRef)
                    val user = userSnap.toObject(UserEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                        ?: throw LocalizedAppException({ it.errProfileRead }, "User not found.")

                    transaction.update(
                        withdrawalRef,
                        mapOf(
                            "status" to WithdrawalStatus.REJECTED.name,
                            "adminNote" to note.trim().ifBlank { "Payout details rejected; TZS refunded." }.take(300),
                            "processedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    transaction.update(
                        userRef,
                        mapOf(
                            "walletBalanceTzs" to user.walletBalanceTzs + withdrawal.amountTzs,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val txRef = userRef.collection("transactions").document()
                    transaction.set(
                        txRef,
                        mapOf(
                            "userId" to user.userId,
                            "type" to TransactionType.WITHDRAWAL_REFUND.name,
                            "amountTzs" to withdrawal.amountTzs,
                            "description" to "Refund: ${withdrawal.resolvedProvider().displayName} payout declined".take(200),
                            "referenceCode" to SecurityUtils.generateReferenceCode("RFD"),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun moderateCampaign(
        taskId: String,
        newStatus: TaskCampaignStatus
    ): Result<Unit> {
        val path = "tasks/$taskId"
        return try {
            requireUserId()
            val taskRef = db.collection("tasks").document(taskId)

            db.runTransaction { transaction ->
                val taskSnap = transaction.get(taskRef)
                val task = taskSnap.toObject(SocialTaskEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                    ?: throw LocalizedAppException({ it.errCampaignNotFound }, "Campaign not found.")
                val prevStatus = task.resolvedStatus()

                transaction.update(
                    taskRef,
                    mapOf(
                        "status" to newStatus.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (newStatus == TaskCampaignStatus.REJECTED &&
                    (prevStatus == TaskCampaignStatus.PENDING_REVIEW || prevStatus == TaskCampaignStatus.ACTIVE || prevStatus == TaskCampaignStatus.PAUSED)
                ) {
                    val remaining = (task.totalSlots - task.completedSlots).coerceAtLeast(0)
                    // Refund 100% of remaining incomplete slots; never deduct 15% commission from incomplete or rejected tasks
                    val refundAmount = remaining * task.resolvedGrossPerSlotTzs()
                    if (refundAmount > 0L) {
                        val creatorRef = db.collection("users").document(task.creatorUserId)
                        val creatorSnap = transaction.get(creatorRef)
                        val creator = creatorSnap.toObject(UserEntity::class.java, ServerTimestampBehavior.ESTIMATE)
                        if (creator != null) {
                            transaction.update(
                                creatorRef,
                                mapOf(
                                    "walletBalanceTzs" to creator.walletBalanceTzs + refundAmount,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                            val txRef = creatorRef.collection("transactions").document()
                            transaction.set(
                                txRef,
                                mapOf(
                                    "userId" to creator.userId,
                                    "type" to TransactionType.WITHDRAWAL_REFUND.name,
                                    "amountTzs" to refundAmount,
                                    "description" to "100% refund for uncompleted slots: ${task.title}".take(200),
                                    "referenceCode" to SecurityUtils.generateReferenceCode("CMF"),
                                    "createdAt" to FieldValue.serverTimestamp()
                                )
                            )
                        }
                    }
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun publishStarterMarketplaceTasks(adminName: String): Result<Unit> {
        val path = "tasks"
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            val samples = listOf(
                Triple("Discover & Engage Dar Style Hub", SocialPlatform.INSTAGRAM, TaskActionType.FOLLOW) to
                    listOf("Visit @dar_style_hub on Instagram, review the latest Tanzanian streetwear lookbook, and leave genuine feedback.", "@dar_style_hub", "https://www.instagram.com", "500", "100"),
                Triple("Watch & Comment Zanzibar Sunset Clip", SocialPlatform.TIKTOK, TaskActionType.COMMENT) to
                    listOf("Watch our full Stone Town cultural reel on TikTok and share an authentic comment about Zanzibar.", "@zanzibar_vibes", "https://www.tiktok.com", "800", "80"),
                Triple("Discover Swahili Tech Daily Channel", SocialPlatform.YOUTUBE, TaskActionType.SUBSCRIBE) to
                    listOf("Watch our latest smartphone review video on Swahili Tech Daily and subscribe if you enjoy tech content.", "Swahili Tech Daily", "https://www.youtube.com", "1400", "60"),
                Triple("Explore Kilimanjaro Roasters Page", SocialPlatform.FACEBOOK, TaskActionType.LIKE) to
                    listOf("Visit Kilimanjaro Roasters TZ on Facebook, read our Arabica harvest story, and react to the latest update.", "Kilimanjaro Roasters TZ", "https://www.facebook.com", "400", "120"),
                Triple("Share TZ Creators Showcase Feature", SocialPlatform.TIKTOK, TaskActionType.SHARE_REPOST) to
                    listOf("Watch the East Africa creators spotlight on TikTok and share it organically with friends who create digital media.", "@tz_creators_hub", "https://www.tiktok.com", "500", "50")
            )

            samples.forEach { (meta, details) ->
                val grossSlot = details[3].toLong()
                val (commissionSlot, workerReward) = computeCommissionSplit(grossSlot)
                val ref = db.collection("tasks").document()
                batch.set(
                    ref,
                    mapOf(
                        "creatorUserId" to uid,
                        "creatorName" to adminName.take(80),
                        "title" to meta.first,
                        "description" to details[0],
                        "platform" to meta.second.name,
                        "actionType" to meta.third.name,
                        "accountHandle" to details[1],
                        "targetUrl" to details[2],
                        "rewardTzs" to workerReward,
                        "grossPerSlotTzs" to grossSlot,
                        "commissionPerSlotTzs" to commissionSlot,
                        "commissionEarnedTzs" to 0L,
                        "totalSlots" to details[4].toInt(),
                        "completedSlots" to 0,
                        "status" to TaskCampaignStatus.ACTIVE.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private fun logFlowError(error: Throwable, operationType: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val toLog = firestoreError ?: error
        if (auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(toLog, operationType, path)
        }
    }

    companion object {
        const val ADMIN_EMAIL = "hamisijohari87@gmail.com"
        const val BUSINESS_ACCOUNT_ID = "razak_main"
        const val BUSINESS_ACCOUNT_NAME = "RAZAK TASK BUSINESS ACCOUNT"
        const val MIN_WITHDRAWAL_TZS = 1_000L
        const val MAX_WITHDRAWAL_TZS = 1_000_000L
        const val MIN_CAMPAIGN_BUDGET_TZS = 1_000L
        const val MAX_CAMPAIGN_BUDGET_TZS = 1_000_000L
        const val PLATFORM_COMMISSION_RATE_PCT = 15L

        fun computeCommissionSplit(grossAmountTzs: Long): Pair<Long, Long> {
            val safeGross = grossAmountTzs.coerceAtLeast(0L)
            val commission = (safeGross * PLATFORM_COMMISSION_RATE_PCT) / 100L
            val workerReward = (safeGross - commission).coerceAtLeast(0L)
            return commission to workerReward
        }

        fun computePendingCommissionTzs(campaigns: List<SocialTaskEntity>): Long {
            return campaigns
                .filter {
                    val st = it.resolvedStatus()
                    st == TaskCampaignStatus.PENDING_REVIEW ||
                        st == TaskCampaignStatus.ACTIVE ||
                        st == TaskCampaignStatus.PAUSED
                }
                .sumOf { campaign ->
                    val remainingSlots = (campaign.totalSlots - campaign.completedSlots).coerceAtLeast(0)
                    val perSlotCommission = computeCommissionSplit(campaign.resolvedGrossPerSlotTzs()).first
                    remainingSlots.toLong() * perSlotCommission
                }
        }

        fun buildFinancialReportText(
            totalBusinessEarningsTzs: Long,
            workerRewardsAllocatedTzs: Long,
            pendingCommissionTzs: Long,
            customerWalletCustodyTzs: Long,
            completedCount: Int,
            settlementProviderLabel: String
        ): String {
            return buildString {
                appendLine("=== RAZAK TASK BUSINESS ACCOUNT REPORT ===")
                appendLine("Verified Company Earnings (15%): ${CurrencyFormatter.formatTzs(totalBusinessEarningsTzs)}")
                appendLine("Approved Worker Rewards (85%): ${CurrencyFormatter.formatTzs(workerRewardsAllocatedTzs)}")
                appendLine("Pending Commissions (Uncompleted Paid Campaigns): ${CurrencyFormatter.formatTzs(pendingCommissionTzs)}")
                appendLine("Segregated Customer Wallet Custody: ${CurrencyFormatter.formatTzs(customerWalletCustodyTzs)}")
                appendLine("Verified Completed Transactions: $completedCount")
                appendLine("Business Settlement Provider: $settlementProviderLabel")
                appendLine("Policy: Only verified task completions are recorded. Customer funds and company earnings are strictly separated.")
            }
        }
    }
}
