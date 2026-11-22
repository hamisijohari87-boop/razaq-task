package com.example.data.model

import androidx.annotation.Keep
import com.example.i18n.AppLanguage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val role: String = UserRole.USER.name,
    val preferredLanguage: String = AppLanguage.DEFAULT.code,
    val walletBalanceTzs: Long = 0L,
    val totalEarnedTzs: Long = 0L,
    val totalSpentTzs: Long = 0L,
    val tasksCompletedCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedRole(): UserRole =
        enumValues<UserRole>().firstOrNull { it.name == role } ?: UserRole.USER

    fun resolvedLanguage(): AppLanguage =
        AppLanguage.fromCode(preferredLanguage)
}

@Keep
@IgnoreExtraProperties
data class SocialTaskEntity(
    @DocumentId val id: String = "",
    val creatorUserId: String = "",
    val creatorName: String = "",
    val title: String = "",
    val description: String = "",
    val platform: String = SocialPlatform.INSTAGRAM.name,
    val actionType: String = TaskActionType.FOLLOW.name,
    val accountHandle: String = "",
    val targetUrl: String = "",
    val rewardTzs: Long = 0L,
    val grossPerSlotTzs: Long = 0L,
    val commissionPerSlotTzs: Long = 0L,
    val commissionEarnedTzs: Long = 0L,
    val totalSlots: Int = 0,
    val completedSlots: Int = 0,
    val status: String = TaskCampaignStatus.PENDING_REVIEW.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedGrossPerSlotTzs(): Long =
        if (grossPerSlotTzs > 0L) grossPerSlotTzs else rewardTzs

    fun resolvedCommissionPerSlotTzs(): Long =
        if (grossPerSlotTzs > 0L) commissionPerSlotTzs else (rewardTzs * 15L) / 100L

    fun resolvedPlatform(): SocialPlatform =
        enumValues<SocialPlatform>().firstOrNull { it.name == platform } ?: SocialPlatform.INSTAGRAM

    fun resolvedActionType(): TaskActionType =
        enumValues<TaskActionType>().firstOrNull { it.name == actionType } ?: TaskActionType.FOLLOW

    fun resolvedStatus(): TaskCampaignStatus =
        enumValues<TaskCampaignStatus>().firstOrNull { it.name == status } ?: TaskCampaignStatus.PENDING_REVIEW
}

@Keep
@IgnoreExtraProperties
data class TaskSubmissionEntity(
    @DocumentId val id: String = "",
    val taskId: String = "",
    val taskTitle: String = "",
    val platform: String = SocialPlatform.INSTAGRAM.name,
    val actionType: String = TaskActionType.FOLLOW.name,
    val rewardTzs: Long = 0L,
    val workerUserId: String = "",
    val workerName: String = "",
    val socialHandleUsed: String = "",
    val proofNote: String = "",
    val proofImageUri: String? = null,
    val status: String = SubmissionStatus.PENDING.name,
    val reviewerNote: String? = null,
    @ServerTimestamp val submittedAt: Timestamp? = null,
    @ServerTimestamp val reviewedAt: Timestamp? = null
) {
    fun resolvedPlatform(): SocialPlatform =
        enumValues<SocialPlatform>().firstOrNull { it.name == platform } ?: SocialPlatform.INSTAGRAM

    fun resolvedActionType(): TaskActionType =
        enumValues<TaskActionType>().firstOrNull { it.name == actionType } ?: TaskActionType.FOLLOW

    fun resolvedStatus(): SubmissionStatus =
        enumValues<SubmissionStatus>().firstOrNull { it.name == status } ?: SubmissionStatus.PENDING
}

@Keep
@IgnoreExtraProperties
data class WalletTransactionEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = TransactionType.TASK_REWARD.name,
    val amountTzs: Long = 0L,
    val description: String = "",
    val referenceCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun resolvedType(): TransactionType =
        enumValues<TransactionType>().firstOrNull { it.name == type } ?: TransactionType.TASK_REWARD
}

@Keep
@IgnoreExtraProperties
data class WithdrawalRequestEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val amountTzs: Long = 0L,
    val provider: String = PayoutProvider.MPESA_VODACOM.name,
    val accountNumber: String = "",
    val accountName: String = "",
    val status: String = WithdrawalStatus.PENDING.name,
    val adminNote: String? = null,
    @ServerTimestamp val requestedAt: Timestamp? = null,
    @ServerTimestamp val processedAt: Timestamp? = null
) {
    fun resolvedProvider(): PayoutProvider =
        enumValues<PayoutProvider>().firstOrNull { it.name == provider } ?: PayoutProvider.MPESA_VODACOM

    fun resolvedStatus(): WithdrawalStatus =
        enumValues<WithdrawalStatus>().firstOrNull { it.name == status } ?: WithdrawalStatus.PENDING
}

@Keep
@IgnoreExtraProperties
data class PlatformCommissionEntity(
    @DocumentId val id: String = "",
    val submissionId: String = "",
    val taskId: String = "",
    val taskTitle: String = "",
    val creatorUserId: String = "",
    val workerUserId: String = "",
    val grossAmountTzs: Long = 0L,
    val commissionTzs: Long = 0L,
    val workerRewardTzs: Long = 0L,
    val referenceCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BusinessAccountEntity(
    @DocumentId val id: String = "razak_main",
    val accountId: String = "razak_main",
    val accountName: String = "RAZAK TASK BUSINESS ACCOUNT",
    val totalCommissionEarnedTzs: Long = 0L,
    val totalWorkerRewardsAllocatedTzs: Long = 0L,
    val totalGrossVerifiedVolumeTzs: Long = 0L,
    val completedTransactionsCount: Int = 0,
    val settlementProviderCode: String = "UNCONFIGURED",
    val settlementMerchantRef: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BusinessLedgerEntryEntity(
    @DocumentId val id: String = "",
    val submissionId: String = "",
    val taskId: String = "",
    val taskTitle: String = "",
    val creatorUserId: String = "",
    val workerUserId: String = "",
    val grossAmountTzs: Long = 0L,
    val commissionTzs: Long = 0L,
    val workerRewardTzs: Long = 0L,
    val referenceCode: String = "",
    val verified: Boolean = true,
    val settlementChannel: String = "ESCROW_VERIFIED_SPLIT",
    @ServerTimestamp val createdAt: Timestamp? = null
)
