package com.example

import com.example.data.local.SecurityUtils
import com.example.data.model.BusinessAccountEntity
import com.example.data.model.PayoutProvider
import com.example.data.model.SocialPlatform
import com.example.data.model.SocialTaskEntity
import com.example.data.model.SubmissionStatus
import com.example.data.model.TaskActionType
import com.example.data.model.TaskCampaignStatus
import com.example.data.model.TaskSubmissionEntity
import com.example.data.model.TransactionType
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.WithdrawalRequestEntity
import com.example.data.model.WithdrawalStatus
import com.example.data.remote.BusinessSettlementProvider
import com.example.data.remote.UnconfiguredMobileMoneyGateway
import com.example.data.remote.UnconfiguredTzBusinessGateway
import com.example.data.repository.RazakRepository
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.i18n.RazakTranslations
import com.example.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `currency formatter displays TZS and compact values accurately`() {
        assertEquals("TZS 12,500", CurrencyFormatter.formatTzs(12_500L))
        assertEquals("TZS 1.0M", CurrencyFormatter.formatCompactTzs(1_000_000L))
    }

    @Test
    fun `platform commission split allocates 15 percent commission and 85 percent worker rewards`() {
        val (commission100k, worker100k) = RazakRepository.computeCommissionSplit(100_000L)
        assertEquals(15_000L, commission100k)
        assertEquals(85_000L, worker100k)
        assertEquals(100_000L, commission100k + worker100k)

        val (commission500, worker500) = RazakRepository.computeCommissionSplit(500L)
        assertEquals(75L, commission500)
        assertEquals(425L, worker500)
    }

    @Test
    fun `pending commissions only sum uncompleted paid campaigns and exclude rejected or completed`() {
        val campaigns = listOf(
            SocialTaskEntity(
                id = "c_active",
                grossPerSlotTzs = 1_000L,
                commissionPerSlotTzs = 150L,
                totalSlots = 10,
                completedSlots = 4,
                status = TaskCampaignStatus.ACTIVE.name
            ),
            SocialTaskEntity(
                id = "c_rejected",
                grossPerSlotTzs = 1_000L,
                commissionPerSlotTzs = 150L,
                totalSlots = 10,
                completedSlots = 0,
                status = TaskCampaignStatus.REJECTED.name
            ),
            SocialTaskEntity(
                id = "c_completed",
                grossPerSlotTzs = 1_000L,
                commissionPerSlotTzs = 150L,
                totalSlots = 5,
                completedSlots = 5,
                status = TaskCampaignStatus.COMPLETED.name
            )
        )
        // Active campaign has 6 remaining slots * 150 TZS = 900 TZS pending
        assertEquals(900L, RazakRepository.computePendingCommissionTzs(campaigns))
    }

    @Test
    fun `withdrawal methods, limits, and tanzanian msisdn normalization match rules`() {
        assertEquals(
            listOf(
                PayoutProvider.MPESA_VODACOM,
                PayoutProvider.AIRTEL_MONEY,
                PayoutProvider.TIGOPESA_MIXX,
                PayoutProvider.HALOPESA
            ),
            PayoutProvider.withdrawalMethods
        )
        assertEquals(1_000L, RazakRepository.MIN_WITHDRAWAL_TZS)
        assertEquals(1_000_000L, RazakRepository.MAX_WITHDRAWAL_TZS)
        assertEquals(1_000L, RazakRepository.MIN_CAMPAIGN_BUDGET_TZS)
        assertEquals(1_000_000L, RazakRepository.MAX_CAMPAIGN_BUDGET_TZS)

        val gateway = UnconfiguredMobileMoneyGateway()
        assertEquals("0754123456", gateway.validateAndNormalizeTzMobileNumber("+255 754 123 456").getOrThrow())
        assertEquals("0622112233", gateway.validateAndNormalizeTzMobileNumber("255622112233").getOrThrow())
        assertEquals("0788334455", gateway.validateAndNormalizeTzMobileNumber("0788334455").getOrThrow())
        assertTrue(gateway.validateAndNormalizeTzMobileNumber("12345").isFailure)
        assertTrue(gateway.validateAndNormalizeTzMobileNumber("0812345678").isFailure)
    }

    @Test
    fun `tanzanian business settlement gateway never simulates live payments`() {
        val gateway = UnconfiguredTzBusinessGateway()
        val status = gateway.inspectSettlementReadiness(
            BusinessSettlementProvider.SELCOM_BUSINESS_TZ.code,
            "MERCH-TZ-2026"
        )
        assertEquals(BusinessSettlementProvider.SELCOM_BUSINESS_TZ, status.provider)
        assertFalse(status.isLiveGatewayConfigured)
        assertTrue(status.statusMessage.contains("server-side only"))

        val report = RazakRepository.buildFinancialReportText(
            totalBusinessEarningsTzs = 15_000L,
            workerRewardsAllocatedTzs = 85_000L,
            pendingCommissionTzs = 4_500L,
            customerWalletCustodyTzs = 120_000L,
            completedCount = 12,
            settlementProviderLabel = "Selcom Pay Business TZ"
        )
        assertTrue(report.contains("RAZAK TASK BUSINESS ACCOUNT REPORT"))
        assertTrue(report.contains("TZS 15,000"))
        assertTrue(report.contains("TZS 85,000"))
    }

    @Test
    fun `firestore entity enum resolvers safely handle valid and unknown values`() {
        val user = UserEntity(role = "ADMIN")
        assertEquals(UserRole.ADMIN, user.resolvedRole())
        assertEquals(UserRole.USER, UserEntity(role = "UNKNOWN_ROLE").resolvedRole())

        val task = SocialTaskEntity(
            platform = SocialPlatform.TIKTOK.name,
            actionType = TaskActionType.COMMENT.name,
            status = TaskCampaignStatus.ACTIVE.name
        )
        assertEquals(SocialPlatform.TIKTOK, task.resolvedPlatform())
        assertEquals(TaskActionType.COMMENT, task.resolvedActionType())
        assertEquals(TaskCampaignStatus.ACTIVE, task.resolvedStatus())

        val submission = TaskSubmissionEntity(status = "UNKNOWN")
        assertEquals(SubmissionStatus.PENDING, submission.resolvedStatus())

        val withdrawal = WithdrawalRequestEntity(
            provider = PayoutProvider.HALOPESA.name,
            status = WithdrawalStatus.APPROVED.name
        )
        assertEquals(PayoutProvider.HALOPESA, withdrawal.resolvedProvider())
        assertEquals(WithdrawalStatus.APPROVED, withdrawal.resolvedStatus())
        assertEquals(TransactionType.PLATFORM_COMMISSION.isCredit, false)
        assertEquals("razak_main", BusinessAccountEntity().accountId)
    }

    @Test
    fun `security utils reference generation and password hash remain deterministic`() {
        val code = SecurityUtils.generateReferenceCode("WDR")
        assertTrue(code.startsWith("WDR-"))
        assertEquals(10, code.length)

        val hash = SecurityUtils.hashPassword("razak2026")
        assertTrue(SecurityUtils.verifyPassword("razak2026", hash))
        assertFalse(SecurityUtils.verifyPassword("wrong", hash))
    }

    @Test
    fun `multilingual config supports 10 languages with Kiswahili default and Arabic RTL`() {
        assertEquals(10, AppLanguage.entries.size)
        assertEquals(AppLanguage.KISWAHILI, AppLanguage.DEFAULT)
        assertEquals("sw", AppLanguage.DEFAULT.code)
        assertEquals(AppLanguage.KISWAHILI, AppLanguage.fromCode(null))
        assertEquals(AppLanguage.KISWAHILI, AppLanguage.fromCode("unknown"))
        assertTrue(AppLanguage.ARABIC.isRtl)
        AppLanguage.entries.filter { it != AppLanguage.ARABIC }.forEach {
            assertFalse("${it.code} must be LTR", it.isRtl)
        }
    }

    @Test
    fun `all 10 language packs have non-blank strings and valid format specifiers`() {
        val stringFields = AppStrings::class.java.declaredFields.filter { it.type == String::class.java }
        assertTrue(stringFields.size > 150)

        AppLanguage.entries.forEach { lang ->
            val s = RazakTranslations.forLanguage(lang)
            assertEquals("RAZAQ TASK", s.appName)

            stringFields.forEach { field ->
                field.isAccessible = true
                val value = field.get(s) as? String ?: ""
                assertTrue("Blank string '${field.name}' in ${lang.code}", value.isNotBlank())
            }

            // Format every single *Fmt property to catch any IllegalFormatException across all 10 languages
            s.onboardingContinueFmt.format(lang.nativeName)
            s.languageChangedNoticeFmt.format(lang.nativeName)
            s.tasksCountPillFmt.format(5)
            s.approvedEarningsFmt.format("TZS 50,000")
            s.myCampaignsNoBotsFmt.format(2)
            s.liveTasksPillFmt.format(12)
            s.balanceColonFmt.format("TZS 10,000")
            s.slotsRemainingFmt.format(4, 10)
            s.slotsLeftFmt.format(4, 10)
            s.promotedByFmt.format("Asha")
            s.targetAccountFmt.format("@razaktz")
            s.openPlatformAccountFmt.format("TikTok")
            s.yourHandleLabelFmt.format("TikTok")
            s.submitProofAndClaimFmt.format("TZS 425")
            s.campaignTrackerTitleFmt.format(3)
            s.budgetRangeLabelFmt.format("TZS 1,000", "TZS 1,000,000")
            s.fundAndLaunchBtnFmt.format("TZS 10,000")
            s.workerPayoutPerTaskFmt.format("TZS 425")
            s.verifiedCountFmt.format(4, 10, 40)
            s.budgetShortFmt.format("TZS 10,000")
            s.spentShortFmt.format("TZS 4,000")
            s.remainingShortFmt.format("TZS 6,000")
            s.commissionPaidCampaignFmt.format("TZS 600")
            s.greetingFmt.format("Juma")
            s.kpiApprovedTasksSubFmt.format(7)
            s.kpiSpentSubFmt.format("TZS 20,000")
            s.kpiCampaignsRunSubFmt.format(3)
            s.myCampaignPerfTitleFmt.format(3)
            s.verifiedCompletionsFmt.format(4, 10, "TZS 10,000")
            s.mySubmissionsTitleFmt.format(6)
            s.handleUsedFmt.format("@juma", "2026-01-01")
            s.proofColonFmt.format("Proof attached")
            s.reviewerNoteFmt.format("Verified")
            s.lifetimeEarnedAndSpendFmt.format("TZS 50,000", "TZS 20,000")
            s.withdrawalLimitsSubFmt.format("TZS 1,000", "TZS 1,000,000")
            s.availableColonFmt.format("TZS 15,000")
            s.mobileNumberHintLabelFmt.format("M-Pesa", "075X XXX XXX")
            s.withdrawAmountBtnFmt.format("TZS 5,000")
            s.withdrawalHistoryTitleFmt.format(2)
            s.requestedAtFmt.format("2026-01-01")
            s.adminNoteShortFmt.format("Approved")
            s.txLedgerTitleFmt.format(8)
            s.addAmountBtnFmt.format("TZS 5,000")
            s.adminTabProofsFmt.format(3)
            s.adminTabPayoutsFmt.format(2)
            s.adminTabCampaignsFmt.format(5)
            s.kpiApprovedTasksCommissionSubFmt.format(12)
            s.kpiLiveSub.format(9)
            s.platformMembersTitleFmt.format(45)
            s.workerHandleTimeFmt.format("Juma", "@juma", "12:00")
            s.proofNoteColonFmt.format("Done")
            s.memberColonFmt.format("Juma")
            s.channelRequestedFmt.format("M-Pesa", "12:00")
            s.approvePayoutDialogTitleFmt.format("TZS 5,000")
            s.promoterSummaryFmt.format("Asha", "TikTok", 4, 10, "TZS 425")
            s.kpiVerifiedTxCountSubFmt.format(12)
            s.completedVerifiedTxTitleFmt.format(12)
            s.grossColonFmt.format("TZS 1,000")
            s.worker85ColonFmt.format("TZS 850")
            s.bizSelectedNoRefFmt.format("Selcom")
            s.bizLinkedReadyFmt.format("Selcom", "MERCH-1")
            s.msgWalletCreditedFmt.format("TZS 5,000")
            s.msgWithdrawalSubmittedFmt.format("TZS 5,000")
            s.msgCampaignUpdatedFmt.format("ACTIVE")
            s.msgSettlementSavedFmt.format("Selcom")
            s.errMinCampaignBudgetFmt.format("TZS 1,000", "TZS 500")
            s.errMaxCampaignBudgetFmt.format("TZS 1,000,000", "TZS 2,000,000")
            s.errInsufficientBalanceFmt.format("TZS 10,000", "TZS 2,000")
            s.errWithdrawalRangeFmt.format("TZS 1,000", "TZS 1,000,000")
            s.errMinWithdrawalFmt.format("TZS 1,000")
            s.errMaxWithdrawalFmt.format("TZS 1,000,000")
            s.errExceedsBalanceFmt.format("TZS 2,000")

            val report = s.buildFinancialReportText(
                totalBusinessEarningsTzs = 15_000L,
                workerRewardsAllocatedTzs = 85_000L,
                pendingCommissionTzs = 4_500L,
                customerWalletCustodyTzs = 120_000L,
                completedCount = 12,
                settlementProviderLabel = s.settlementProviderLabel(BusinessSettlementProvider.SELCOM_BUSINESS_TZ)
            )
            assertTrue(report.contains("TZS 15,000"))
            assertTrue(report.contains("TZS 85,000"))

            UserRole.entries.forEach { assertTrue(s.roleLabel(it).isNotBlank()) }
            TaskActionType.entries.forEach { assertTrue(s.actionLabel(it).isNotBlank()) }
            TaskCampaignStatus.entries.forEach { assertTrue(s.campaignStatusLabel(it).isNotBlank()) }
            SubmissionStatus.entries.forEach { assertTrue(s.submissionStatusLabel(it).isNotBlank()) }
            TransactionType.entries.forEach { assertTrue(s.transactionTypeLabel(it).isNotBlank()) }
            WithdrawalStatus.entries.forEach { assertTrue(s.withdrawalStatusLabel(it).isNotBlank()) }
        }
    }
}
