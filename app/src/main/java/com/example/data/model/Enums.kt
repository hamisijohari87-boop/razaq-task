package com.example.data.model

enum class UserRole(val displayName: String) {
    USER("Member"),
    ADMIN("Administrator")
}

enum class SocialPlatform(val displayName: String, val shortLabel: String) {
    INSTAGRAM("Instagram", "IG"),
    TIKTOK("TikTok", "TT"),
    YOUTUBE("YouTube", "YT"),
    FACEBOOK("Facebook", "FB"),
    X_TWITTER("X (Twitter)", "X");

    companion object {
        val promotePlatforms: List<SocialPlatform> = listOf(
            TIKTOK,
            YOUTUBE,
            INSTAGRAM,
            FACEBOOK,
            X_TWITTER
        )
    }
}

enum class TaskActionType(val displayName: String, val defaultRewardTzs: Long) {
    FOLLOW("Organic Account Discovery", 350L),
    LIKE("Content Appreciation (Like)", 250L),
    COMMENT("Authentic Comment", 500L),
    SUBSCRIBE("Channel Discovery & Subscribe", 1_000L),
    SHARE_REPOST("Organic Content Share", 450L)
}

enum class TaskCampaignStatus(val displayName: String) {
    PENDING_REVIEW("Pending Review"),
    ACTIVE("Active"),
    PAUSED("Paused"),
    COMPLETED("Completed"),
    REJECTED("Rejected")
}

enum class SubmissionStatus(val displayName: String) {
    PENDING("Pending Review"),
    APPROVED("Approved"),
    REJECTED("Rejected")
}

enum class TransactionType(val displayName: String, val isCredit: Boolean) {
    TASK_REWARD("Task Reward (85%)", true),
    DEPOSIT_TOPUP("Wallet Top-Up", true),
    CAMPAIGN_FUND("Campaign Budget", false),
    PLATFORM_COMMISSION("Platform Commission (15%)", false),
    WITHDRAWAL("Withdrawal Request", false),
    WITHDRAWAL_REFUND("Refund", true)
}

enum class PayoutProvider(
    val displayName: String,
    val hint: String,
    val providerCode: String
) {
    MPESA_VODACOM("M-Pesa", "074X / 075X / 076X XXX XXX", "VODACOM_MPESA_TZ"),
    AIRTEL_MONEY("Airtel Money", "078X / 068X / 069X XXX XXX", "AIRTEL_MONEY_TZ"),
    TIGOPESA_MIXX("Mixx by Yas", "071X / 065X / 067X XXX XXX", "YAS_MIXX_TZ"),
    HALOPESA("HaloPesa", "062X XXX XXX", "HALOTEL_HALOPESA_TZ"),
    CRDB_BANK("CRDB Bank", "Account Number", "CRDB_TZ"),
    NMB_BANK("NMB Bank", "Account Number", "NMB_TZ");

    companion object {
        val withdrawalMethods: List<PayoutProvider> = listOf(
            MPESA_VODACOM,
            AIRTEL_MONEY,
            TIGOPESA_MIXX,
            HALOPESA
        )
    }
}

enum class WithdrawalStatus(val displayName: String) {
    PENDING("Pending"),
    APPROVED("Paid"),
    REJECTED("Rejected")
}
