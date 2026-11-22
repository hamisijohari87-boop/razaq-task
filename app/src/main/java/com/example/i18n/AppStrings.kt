package com.example.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.model.SocialPlatform
import com.example.data.model.SubmissionStatus
import com.example.data.model.TaskActionType
import com.example.data.model.TaskCampaignStatus
import com.example.data.model.TransactionType
import com.example.data.model.UserRole
import com.example.data.model.WithdrawalStatus
import com.example.data.remote.BusinessSettlementProvider
import com.example.util.CurrencyFormatter
import com.google.firebase.firestore.FirebaseFirestoreException

class AppStrings(
    val language: AppLanguage
) {
    // Brand & Onboarding & Language Settings
    var appName: String = ""
    var brandTagline: String = ""
    var onboardingBadge: String = ""
    var onboardingTitle: String = ""
    var onboardingSubtitle: String = ""
    var onboardingDefaultTag: String = ""
    var onboardingContinueFmt: String = ""
    var settingsLanguageTitle: String = ""
    var settingsLanguageSubtitle: String = ""
    var languageButtonLabel: String = ""
    var languageChangedNoticeFmt: String = ""
    var rtlBadgeLabel: String = ""
    // Auth Screen
    var authPillTanzania: String = ""
    var authHeroHeadline: String = ""
    var authHeroBody: String = ""
    var authChipSyncTitle: String = ""
    var authChipSyncSub: String = ""
    var authChipSecurityTitle: String = ""
    var authChipSecuritySub: String = ""
    var authCompactBannerDesc: String = ""
    var authCardTitle: String = ""
    var authCardBody: String = ""
    var authSigningIn: String = ""
    var authSignInGoogle: String = ""
    var authFeat1: String = ""
    var authFeat2: String = ""
    var authFeat3: String = ""
    // Navigation & Profile & Topbar
    var navMarket: String = ""
    var navPromote: String = ""
    var navDashboard: String = ""
    var navWallet: String = ""
    var navAdmin: String = ""
    var syncingProfile: String = ""
    var couldNotLoadProfile: String = ""
    var retry: String = ""
    var signOut: String = ""
    var walletBalance: String = ""
    var toggleThemeDesc: String = ""
    var accountProfileDesc: String = ""
    var accountAndProfileTitle: String = ""
    var crossPlatformSyncNote: String = ""
    var fullNameLabel: String = ""
    var usernameLabel: String = ""
    var mobileMoneyNumberLabel: String = ""
    var saveProfile: String = ""
    var close: String = ""
    // Dual Section Hubs
    var section1Label: String = ""
    var earnMoneyTitle: String = ""
    var tasksCountPillFmt: String = ""
    var earnMoneyDesc: String = ""
    var approvedEarningsFmt: String = ""
    var browseTasksBtn: String = ""
    var earningsBtn: String = ""
    var section2Label: String = ""
    var growSocialTitle: String = ""
    var organicAdsPill: String = ""
    var growSocialDesc: String = ""
    var myCampaignsNoBotsFmt: String = ""
    var createAdBtn: String = ""
    var performanceBtn: String = ""
    // Marketplace
    var liveTasksPillFmt: String = ""
    var balanceColonFmt: String = ""
    var marketHeroTitle: String = ""
    var marketHeroBody: String = ""
    var marketHeroCta: String = ""
    var searchPlaceholder: String = ""
    var searchDesc: String = ""
    var clearSearchDesc: String = ""
    var allPlatforms: String = ""
    var noMatchingTasksTitle: String = ""
    var noMatchingTasksMsg: String = ""
    var selectTaskTitle: String = ""
    var selectTaskMsg: String = ""
    var slotsRemainingFmt: String = ""
    var slotsLeftFmt: String = ""
    var promotedByFmt: String = ""
    var taskInstructions: String = ""
    var targetAccountFmt: String = ""
    var openPlatformAccountFmt: String = ""
    var ownCampaignNotice: String = ""
    var alreadySubmittedNotice: String = ""
    var submitProofTitle: String = ""
    var yourHandleLabelFmt: String = ""
    var proofNoteLabel: String = ""
    var attachScreenshot: String = ""
    var changeScreenshot: String = ""
    var submitProofAndClaimFmt: String = ""
    var yourCampaignPill: String = ""
    var submittedStatus: String = ""
    var startTask: String = ""
    // Promote Screen
    var promoteHeroTitle: String = ""
    var promoteHeroBody: String = ""
    var campaignTrackerTitleFmt: String = ""
    var noCampaignsTitle: String = ""
    var noCampaignsMsg: String = ""
    var createOrganicAdTitle: String = ""
    var budgetRangeLabelFmt: String = ""
    var organicPolicyNotice: String = ""
    var choosePlatformStep: String = ""
    var engagementActionStep: String = ""
    var campaignTitleLabel: String = ""
    var accountHandleLabel: String = ""
    var profileUrlLabel: String = ""
    var instructionsLabel: String = ""
    var budgetPerSlotLabel: String = ""
    var targetSlotsLabel: String = ""
    var totalBudgetLabel: String = ""
    var platformCommissionLabel: String = ""
    var workerRewardsPoolLabel: String = ""
    var perTaskShort: String = ""
    var yourWalletBalanceLabel: String = ""
    var commissionRuleNote: String = ""
    var insufficientBalanceTopUpBtn: String = ""
    var fundAndLaunchBtnFmt: String = ""
    var workerPayoutPerTaskFmt: String = ""
    var verifiedCountFmt: String = ""
    var budgetShortFmt: String = ""
    var spentShortFmt: String = ""
    var remainingShortFmt: String = ""
    var commissionPaidCampaignFmt: String = ""
    var pauseCampaign: String = ""
    var resumeCampaign: String = ""
    // Dashboard Screen
    var greetingFmt: String = ""
    var earnTasksBtn: String = ""
    var promoteBtn: String = ""
    var withdrawBtn: String = ""
    var kpiWalletTitle: String = ""
    var kpiWalletSubAvail: String = ""
    var kpiTotalEarnedTitle: String = ""
    var kpiApprovedTasksSubFmt: String = ""
    var kpiPendingReviewTitle: String = ""
    var kpiAwaitingVerifySub: String = ""
    var kpiMyCampaignsTitle: String = ""
    var kpiSpentSubFmt: String = ""
    var kpiPendingProofsTitle: String = ""
    var kpiUnderVerifySub: String = ""
    var kpiTasksCompletedTitle: String = ""
    var kpiCampaignsRunSubFmt: String = ""
    var myCampaignPerfTitleFmt: String = ""
    var verifiedCompletionsFmt: String = ""
    var mySubmissionsTitleFmt: String = ""
    var noSubmissionsTitle: String = ""
    var noSubmissionsMsg: String = ""
    var handleUsedFmt: String = ""
    var proofColonFmt: String = ""
    var reviewerNoteFmt: String = ""
    // Wallet Screen
    var razakWalletHeader: String = ""
    var tanzanianShillingsLabel: String = ""
    var lifetimeEarnedAndSpendFmt: String = ""
    var topUpWalletBtn: String = ""
    var requestWithdrawalTitle: String = ""
    var withdrawalLimitsSubFmt: String = ""
    var pendingWithdrawalBanner: String = ""
    var selectMobileMoneyMethod: String = ""
    var withdrawalAmountLabel: String = ""
    var availableColonFmt: String = ""
    var mobileNumberHintLabelFmt: String = ""
    var registeredRecipientNameLabel: String = ""
    var pendingRequestInReviewBtn: String = ""
    var withdrawAmountBtnFmt: String = ""
    var withdrawalHistoryTitleFmt: String = ""
    var requestedAtFmt: String = ""
    var adminNoteShortFmt: String = ""
    var txLedgerTitleFmt: String = ""
    var noTxTitle: String = ""
    var noTxMsg: String = ""
    var topUpDialogTitle: String = ""
    var topUpDialogBody: String = ""
    var depositAmountLabel: String = ""
    var mobileRefLabel: String = ""
    var addAmountBtnFmt: String = ""
    var cancel: String = ""
    // Admin & Business Account
    var adminTabOverview: String = ""
    var adminTabProofsFmt: String = ""
    var adminTabPayoutsFmt: String = ""
    var adminTabCampaignsFmt: String = ""
    var adminTabBusiness: String = ""
    var adminConsoleTitle: String = ""
    var adminConsoleSub: String = ""
    var kpiPlatformEarnings15: String = ""
    var kpiApprovedTasksCommissionSubFmt: String = ""
    var kpiRegisteredUsers: String = ""
    var kpiActiveAccountsSub: String = ""
    var kpiCampaignVolume: String = ""
    var kpiLiveSub: String = ""
    var queuesAndRevenueTitle: String = ""
    var publishStarterTasksBtn: String = ""
    var platformMembersTitleFmt: String = ""
    var noSubmissionsAdminMsg: String = ""
    var workerHandleTimeFmt: String = ""
    var proofNoteColonFmt: String = ""
    var approveAndPayBtn: String = ""
    var rejectBtn: String = ""
    var noWithdrawalsAdminTitle: String = ""
    var noWithdrawalsAdminMsg: String = ""
    var memberColonFmt: String = ""
    var channelRequestedFmt: String = ""
    var approveAndPayWithdrawBtn: String = ""
    var declineAndRefundBtn: String = ""
    var approvePayoutDialogTitleFmt: String = ""
    var declineRefundDialogTitle: String = ""
    var providerRefLabel: String = ""
    var rejectionReasonLabel: String = ""
    var confirmPaidBtn: String = ""
    var confirmRefundBtn: String = ""
    var noCampaignsAdminMsg: String = ""
    var promoterSummaryFmt: String = ""
    var approveLiveBtn: String = ""
    var pauseBtn: String = ""
    var rejectAndRefundBtn: String = ""
    var businessAccountTitle: String = ""
    var businessAccountSub: String = ""
    var reportBtn: String = ""
    var exportReportChooser: String = ""
    var kpiTotalBizEarnings15: String = ""
    var kpiVerifiedTxCountSubFmt: String = ""
    var kpiPendingCommissions15: String = ""
    var kpiEscrowedSlotsSub: String = ""
    var kpiWorkerRewards85: String = ""
    var kpiAllocatedWorkersSub: String = ""
    var segregationTitle: String = ""
    var customerWalletsCustodyLabel: String = ""
    var companyAccountLabel: String = ""
    var totalVerifiedGrossLabel: String = ""
    var settlementIntegrationTitle: String = ""
    var merchantRefLabel: String = ""
    var saveSettlementRailBtn: String = ""
    var completedVerifiedTxTitleFmt: String = ""
    var noVerifiedBizTxTitle: String = ""
    var noVerifiedBizTxMsg: String = ""
    var verifiedEscrowSplit: String = ""
    var verifiedCommissionLabel: String = ""
    var grossColonFmt: String = ""
    var worker85ColonFmt: String = ""
    var bizNotConnectedMsg: String = ""
    var bizSelectedNoRefFmt: String = ""
    var bizLinkedReadyFmt: String = ""
    var reportHeader: String = ""
    var reportVerifiedEarningsFmt: String = ""
    var reportWorkerRewardsFmt: String = ""
    var reportPendingCommissionsFmt: String = ""
    var reportCustomerCustodyFmt: String = ""
    var reportCompletedCountFmt: String = ""
    var reportSettlementProviderFmt: String = ""
    var reportPolicy: String = ""
    // Enum labels
    var roleUser: String = ""
    var roleAdmin: String = ""
    var actionFollow: String = ""
    var actionLike: String = ""
    var actionComment: String = ""
    var actionSubscribe: String = ""
    var actionShare: String = ""
    var campaignStatusPending: String = ""
    var campaignStatusActive: String = ""
    var campaignStatusPaused: String = ""
    var campaignStatusCompleted: String = ""
    var campaignStatusRejected: String = ""
    var submissionStatusPending: String = ""
    var submissionStatusApproved: String = ""
    var submissionStatusRejected: String = ""
    var txTaskReward: String = ""
    var txDepositTopUp: String = ""
    var txCampaignFund: String = ""
    var txPlatformCommission: String = ""
    var txWithdrawal: String = ""
    var txWithdrawalRefund: String = ""
    var withdrawalStatusPending: String = ""
    var withdrawalStatusApproved: String = ""
    var withdrawalStatusRejected: String = ""
    var providerNotConnected: String = ""
    // Notifications & Messages
    var msgProfileUpdated: String = ""
    var msgProofSubmitted: String = ""
    var msgCampaignPublishedLive: String = ""
    var msgCampaignSubmittedReview: String = ""
    var msgWalletCreditedFmt: String = ""
    var msgWithdrawalSubmittedFmt: String = ""
    var msgSubmissionApproved: String = ""
    var msgSubmissionRejected: String = ""
    var msgWithdrawalApproved: String = ""
    var msgWithdrawalRejected: String = ""
    var msgCampaignUpdatedFmt: String = ""
    var msgStarterTasksPublished: String = ""
    var msgSettlementSavedFmt: String = ""
    // Validation & Error Messages
    var errSignInRequired: String = ""
    var errProfileRead: String = ""
    var errSignedInMismatch: String = ""
    var errFullNameLength: String = ""
    var errUsernameLength: String = ""
    var errPhoneTooLong: String = ""
    var errHandleLength: String = ""
    var errProofNoteLength: String = ""
    var errTaskNotFound: String = ""
    var errTaskNotActive: String = ""
    var errTaskFull: String = ""
    var errOwnTaskProof: String = ""
    var errDuplicateSubmission: String = ""
    var errCampaignTitleLength: String = ""
    var errCampaignDescLength: String = ""
    var errCampaignHandleLength: String = ""
    var errTargetUrlInvalid: String = ""
    var errRewardSlotRange: String = ""
    var errSlotsRange: String = ""
    var errMinCampaignBudgetFmt: String = ""
    var errMaxCampaignBudgetFmt: String = ""
    var errMinSlotBudget: String = ""
    var errMaxSlotBudget: String = ""
    var errMinSlots: String = ""
    var errInsufficientBalanceFmt: String = ""
    var errTopUpRange: String = ""
    var errValidMobileOrRef: String = ""
    var errSelectWithdrawalMethod: String = ""
    var errWithdrawalRangeFmt: String = ""
    var errInvalidTzPhone: String = ""
    var errRecipientNameLength: String = ""
    var errDuplicateWithdrawal: String = ""
    var errDuplicatePayout: String = ""
    var errSubmissionNotFound: String = ""
    var errSubmissionReviewed: String = ""
    var errAllSlotsDone: String = ""
    var errWorkerMissing: String = ""
    var errWithdrawalNotFound: String = ""
    var errWithdrawalProcessed: String = ""
    var errCampaignNotFound: String = ""
    var errMinWithdrawalFmt: String = ""
    var errMaxWithdrawalFmt: String = ""
    var errExceedsBalanceFmt: String = ""
    var errPermissionDenied: String = ""
    var errNetworkUnavailable: String = ""
    var errGenericFailure: String = ""

    fun roleLabel(role: UserRole): String = when (role) {
        UserRole.USER -> roleUser
        UserRole.ADMIN -> roleAdmin
    }

    fun actionLabel(action: TaskActionType): String = when (action) {
        TaskActionType.FOLLOW -> actionFollow
        TaskActionType.LIKE -> actionLike
        TaskActionType.COMMENT -> actionComment
        TaskActionType.SUBSCRIBE -> actionSubscribe
        TaskActionType.SHARE_REPOST -> actionShare
    }

    fun campaignStatusLabel(status: TaskCampaignStatus): String = when (status) {
        TaskCampaignStatus.PENDING_REVIEW -> campaignStatusPending
        TaskCampaignStatus.ACTIVE -> campaignStatusActive
        TaskCampaignStatus.PAUSED -> campaignStatusPaused
        TaskCampaignStatus.COMPLETED -> campaignStatusCompleted
        TaskCampaignStatus.REJECTED -> campaignStatusRejected
    }

    fun submissionStatusLabel(status: SubmissionStatus): String = when (status) {
        SubmissionStatus.PENDING -> submissionStatusPending
        SubmissionStatus.APPROVED -> submissionStatusApproved
        SubmissionStatus.REJECTED -> submissionStatusRejected
    }

    fun transactionTypeLabel(type: TransactionType): String = when (type) {
        TransactionType.TASK_REWARD -> txTaskReward
        TransactionType.DEPOSIT_TOPUP -> txDepositTopUp
        TransactionType.CAMPAIGN_FUND -> txCampaignFund
        TransactionType.PLATFORM_COMMISSION -> txPlatformCommission
        TransactionType.WITHDRAWAL -> txWithdrawal
        TransactionType.WITHDRAWAL_REFUND -> txWithdrawalRefund
    }

    fun withdrawalStatusLabel(status: WithdrawalStatus): String = when (status) {
        WithdrawalStatus.PENDING -> withdrawalStatusPending
        WithdrawalStatus.APPROVED -> withdrawalStatusApproved
        WithdrawalStatus.REJECTED -> withdrawalStatusRejected
    }

    fun settlementProviderLabel(provider: BusinessSettlementProvider): String =
        if (provider == BusinessSettlementProvider.UNCONFIGURED) providerNotConnected else provider.displayName

    fun settlementStatusMessage(provider: BusinessSettlementProvider, merchantRef: String): String = when {
        provider == BusinessSettlementProvider.UNCONFIGURED -> bizNotConnectedMsg
        merchantRef.isBlank() -> bizSelectedNoRefFmt.format(provider.displayName)
        else -> bizLinkedReadyFmt.format(provider.displayName, merchantRef.trim())
    }

    fun platformLabel(platform: SocialPlatform): String = platform.displayName

    fun buildFinancialReportText(
        totalBusinessEarningsTzs: Long,
        workerRewardsAllocatedTzs: Long,
        pendingCommissionTzs: Long,
        customerWalletCustodyTzs: Long,
        completedCount: Int,
        settlementProviderLabel: String
    ): String = buildString {
        appendLine(reportHeader)
        appendLine(reportVerifiedEarningsFmt.format(CurrencyFormatter.formatTzs(totalBusinessEarningsTzs)))
        appendLine(reportWorkerRewardsFmt.format(CurrencyFormatter.formatTzs(workerRewardsAllocatedTzs)))
        appendLine(reportPendingCommissionsFmt.format(CurrencyFormatter.formatTzs(pendingCommissionTzs)))
        appendLine(reportCustomerCustodyFmt.format(CurrencyFormatter.formatTzs(customerWalletCustodyTzs)))
        appendLine(reportCompletedCountFmt.format(completedCount))
        appendLine(reportSettlementProviderFmt.format(settlementProviderLabel))
        appendLine(reportPolicy)
    }
}

class LocalizedAppException(
    val resolver: (AppStrings) -> String,
    fallbackEnglish: String
) : IllegalStateException(fallbackEnglish)

fun Throwable?.resolveLocalizedMessage(strings: AppStrings): String {
    if (this == null) return strings.errGenericFailure
    if (this is LocalizedAppException) return resolver(strings)
    val firestoreErr = generateSequence(this) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreErr != null) {
        return when (firestoreErr.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> strings.errPermissionDenied
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> strings.errNetworkUnavailable
            else -> firestoreErr.localizedMessage ?: strings.errGenericFailure
        }
    }
    return this.message?.takeIf { it.isNotBlank() } ?: strings.errGenericFailure
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> {
    error("LocalAppStrings not provided")
}

fun appStrings(language: AppLanguage, block: AppStrings.() -> Unit): AppStrings =
    AppStrings(language).apply(block)
