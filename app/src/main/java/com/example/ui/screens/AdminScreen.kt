package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.BusinessAccountEntity
import com.example.data.model.BusinessLedgerEntryEntity
import com.example.data.model.PlatformCommissionEntity
import com.example.data.model.SocialTaskEntity
import com.example.data.model.SubmissionStatus
import com.example.data.model.TaskCampaignStatus
import com.example.data.model.TaskSubmissionEntity
import com.example.data.model.UserEntity
import com.example.data.model.WithdrawalRequestEntity
import com.example.data.model.WithdrawalStatus
import com.example.data.remote.BusinessSettlementProvider
import com.example.data.repository.RazakRepository
import com.example.i18n.LocalAppStrings
import com.example.ui.components.CampaignStatusPill
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PlatformBadge
import com.example.ui.components.StatKpiCard
import com.example.ui.components.SubmissionStatusPill
import com.example.ui.components.TzsRewardBadge
import com.example.ui.components.WithdrawalStatusPill
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CobaltAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    users: List<UserEntity>,
    submissions: List<TaskSubmissionEntity>,
    withdrawals: List<WithdrawalRequestEntity>,
    campaigns: List<SocialTaskEntity>,
    commissions: List<PlatformCommissionEntity>,
    businessAccount: BusinessAccountEntity?,
    businessLedger: List<BusinessLedgerEntryEntity>,
    isBusy: Boolean,
    onReviewSubmission: (String, Boolean, String) -> Unit,
    onReviewWithdrawal: (String, Boolean, String) -> Unit,
    onModerateCampaign: (String, TaskCampaignStatus) -> Unit,
    onPublishStarterTasks: () -> Unit,
    onConfigureBusinessSettlement: (BusinessSettlementProvider, String) -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedSubTab by rememberSaveable { mutableIntStateOf(0) }

    val pendingSubmissions = submissions.filter { it.resolvedStatus() == SubmissionStatus.PENDING }
    val pendingWithdrawals = withdrawals.filter { it.resolvedStatus() == WithdrawalStatus.PENDING }
    val pendingCampaigns = campaigns.filter { it.resolvedStatus() == TaskCampaignStatus.PENDING_REVIEW }

    val totalPendingPayoutTzs = pendingWithdrawals.sumOf { it.amountTzs }
    val totalEscrowVolumeTzs = campaigns.sumOf { it.resolvedGrossPerSlotTzs() * it.totalSlots }
    val totalPlatformEarningsTzs = maxOf(
        businessAccount?.totalCommissionEarnedTzs ?: 0L,
        commissions.sumOf { it.commissionTzs }
    )
    val totalPendingCommissionTzs = RazakRepository.computePendingCommissionTzs(campaigns)
    val customerWalletCustodyTzs = users.sumOf { it.walletBalanceTzs }

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryScrollableTabRow(
            selectedTabIndex = selectedSubTab,
            edgePadding = 12.dp
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text(strings.adminTabOverview) },
                modifier = Modifier.testTag("admin_tab_overview")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text(strings.adminTabProofsFmt.format(pendingSubmissions.size)) },
                modifier = Modifier.testTag("admin_tab_proofs")
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = { Text(strings.adminTabPayoutsFmt.format(pendingWithdrawals.size)) },
                modifier = Modifier.testTag("admin_tab_payouts")
            )
            Tab(
                selected = selectedSubTab == 3,
                onClick = { selectedSubTab = 3 },
                text = { Text(strings.adminTabCampaignsFmt.format(pendingCampaigns.size)) },
                modifier = Modifier.testTag("admin_tab_campaigns")
            )
            Tab(
                selected = selectedSubTab == 4,
                onClick = { selectedSubTab = 4 },
                text = { Text(strings.adminTabBusiness) },
                modifier = Modifier.testTag("admin_tab_earnings")
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 820.dp

            when (selectedSubTab) {
                0 -> AdminOverviewPane(
                    isWide = isWide,
                    isBusy = isBusy,
                    users = users,
                    pendingProofs = pendingSubmissions.size,
                    pendingPayouts = pendingWithdrawals.size,
                    pendingPayoutTzs = totalPendingPayoutTzs,
                    totalCampaignVolumeTzs = totalEscrowVolumeTzs,
                    totalPlatformEarningsTzs = totalPlatformEarningsTzs,
                    commissionsCount = maxOf(businessLedger.size, commissions.size),
                    activeCampaignsCount = campaigns.count { it.resolvedStatus() == TaskCampaignStatus.ACTIVE },
                    onJumpToProofs = { selectedSubTab = 1 },
                    onJumpToPayouts = { selectedSubTab = 2 },
                    onJumpToCampaigns = { selectedSubTab = 3 },
                    onJumpToEarnings = { selectedSubTab = 4 },
                    onPublishStarterTasks = onPublishStarterTasks
                )
                1 -> AdminSubmissionsPane(
                    submissions = submissions,
                    onReviewSubmission = onReviewSubmission
                )
                2 -> AdminWithdrawalsPane(
                    withdrawals = withdrawals,
                    onReviewWithdrawal = onReviewWithdrawal
                )
                3 -> AdminCampaignsPane(
                    campaigns = campaigns,
                    onModerateCampaign = onModerateCampaign
                )
                4 -> AdminBusinessAccountPane(
                    isWide = isWide,
                    isBusy = isBusy,
                    businessAccount = businessAccount,
                    businessLedger = businessLedger,
                    commissions = commissions,
                    totalPendingCommissionTzs = totalPendingCommissionTzs,
                    customerWalletCustodyTzs = customerWalletCustodyTzs,
                    onConfigureBusinessSettlement = onConfigureBusinessSettlement
                )
            }
        }
    }
}

@Composable
private fun AdminOverviewPane(
    isWide: Boolean,
    isBusy: Boolean,
    users: List<UserEntity>,
    pendingProofs: Int,
    pendingPayouts: Int,
    pendingPayoutTzs: Long,
    totalCampaignVolumeTzs: Long,
    totalPlatformEarningsTzs: Long,
    commissionsCount: Int,
    activeCampaignsCount: Int,
    onJumpToProofs: () -> Unit,
    onJumpToPayouts: () -> Unit,
    onJumpToCampaigns: () -> Unit,
    onJumpToEarnings: () -> Unit,
    onPublishStarterTasks: () -> Unit
) {
    val strings = LocalAppStrings.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(strings.adminConsoleTitle, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = strings.adminConsoleSub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            if (isWide) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatKpiCard(
                            title = strings.kpiPlatformEarnings15,
                            value = CurrencyFormatter.formatTzs(totalPlatformEarningsTzs),
                            subtitle = strings.kpiApprovedTasksSubFmt.format(commissionsCount),
                            icon = Icons.Filled.AccountBalance,
                            accentColor = EmeraldPrimary,
                            modifier = Modifier.weight(1f).testTag("admin_platform_earnings_kpi")
                        )
                        StatKpiCard(strings.kpiRegisteredUsers, "${users.size}", strings.kpiActiveAccountsSub, Icons.Filled.Group, CobaltAccent, Modifier.weight(1f))
                        StatKpiCard(strings.kpiPendingProofsTitle, "$pendingProofs", strings.kpiAwaitingVerifySub, Icons.Filled.VerifiedUser, CobaltAccent, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatKpiCard(strings.adminTabPayoutsFmt.format(pendingPayouts), CurrencyFormatter.formatTzs(pendingPayoutTzs), "$pendingPayouts", Icons.Filled.Payments, AmberGold, Modifier.weight(1f))
                        StatKpiCard(strings.kpiCampaignVolume, CurrencyFormatter.formatCompactTzs(totalCampaignVolumeTzs), strings.kpiLiveSub.format(activeCampaignsCount), Icons.Filled.Campaign, EmeraldPrimary, Modifier.weight(1f))
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatKpiCard(
                        title = strings.kpiPlatformEarnings15,
                        value = CurrencyFormatter.formatTzs(totalPlatformEarningsTzs),
                        subtitle = strings.kpiApprovedTasksCommissionSubFmt.format(commissionsCount),
                        icon = Icons.Filled.AccountBalance,
                        accentColor = EmeraldPrimary,
                        modifier = Modifier.fillMaxWidth().testTag("admin_platform_earnings_kpi")
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatKpiCard(strings.kpiRegisteredUsers, "${users.size}", strings.kpiActiveAccountsSub, Icons.Filled.Group, EmeraldPrimary, Modifier.weight(1f))
                        StatKpiCard(strings.kpiPendingProofsTitle, "$pendingProofs", strings.kpiUnderVerifySub, Icons.Filled.VerifiedUser, CobaltAccent, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatKpiCard(strings.adminTabPayoutsFmt.format(pendingPayouts), CurrencyFormatter.formatTzs(pendingPayoutTzs), "$pendingPayouts", Icons.Filled.Payments, AmberGold, Modifier.weight(1f))
                        StatKpiCard(strings.kpiCampaignVolume, "$activeCampaignsCount", CurrencyFormatter.formatCompactTzs(totalCampaignVolumeTzs), Icons.Filled.Campaign, EmeraldPrimary, Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.queuesAndRevenueTitle, style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onJumpToProofs, modifier = Modifier.weight(1f).height(48.dp)) {
                            Text(strings.adminTabProofsFmt.format(pendingProofs))
                        }
                        OutlinedButton(onClick = onJumpToPayouts, modifier = Modifier.weight(1f).height(48.dp)) {
                            Text(strings.adminTabPayoutsFmt.format(pendingPayouts))
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onJumpToCampaigns, modifier = Modifier.weight(1f).height(48.dp)) {
                            Text(strings.kpiMyCampaignsTitle)
                        }
                        OutlinedButton(onClick = onJumpToEarnings, modifier = Modifier.weight(1f).height(48.dp)) {
                            Text(strings.adminTabBusiness)
                        }
                    }
                    OutlinedButton(
                        onClick = onPublishStarterTasks,
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("publish_starter_tasks_button")
                    ) {
                        Icon(Icons.Filled.AddTask, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.publishStarterTasksBtn)
                    }
                }
            }
        }

        item {
            Text(strings.platformMembersTitleFmt.format(users.size), style = MaterialTheme.typography.titleLarge)
        }

        items(users, key = { it.id }) { member ->
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${member.fullName} (${strings.roleLabel(member.resolvedRole())})", style = MaterialTheme.typography.titleSmall)
                        val phonePart = if (member.phoneNumber.isNotBlank()) " • ${member.phoneNumber}" else ""
                        Text(
                            text = "@${member.username} • ${member.email}$phonePart",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = CurrencyFormatter.formatTzs(member.walletBalanceTzs),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminSubmissionsPane(
    submissions: List<TaskSubmissionEntity>,
    onReviewSubmission: (String, Boolean, String) -> Unit
) {
    val strings = LocalAppStrings.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (submissions.isEmpty()) {
            item {
                EmptyStateCard(
                    title = strings.noSubmissionsTitle,
                    message = strings.noSubmissionsAdminMsg,
                    icon = Icons.Filled.VerifiedUser
                )
            }
        } else {
            items(submissions, key = { it.id }) { sub ->
                val status = sub.resolvedStatus()
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlatformBadge(platform = sub.resolvedPlatform())
                            SubmissionStatusPill(status = status)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sub.taskTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            TzsRewardBadge(amountTzs = sub.rewardTzs)
                        }

                        Text(
                            text = strings.workerHandleTimeFmt.format(
                                sub.workerName,
                                sub.socialHandleUsed,
                                CurrencyFormatter.formatRelativeTime(sub.submittedAt)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(strings.proofNoteColonFmt.format(sub.proofNote), style = MaterialTheme.typography.bodyMedium)

                        if (!sub.proofImageUri.isNullOrBlank()) {
                            AsyncImage(
                                model = sub.proofImageUri,
                                contentDescription = strings.attachScreenshot,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(MaterialTheme.shapes.small)
                            )
                        }

                        if (status == SubmissionStatus.PENDING) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onReviewSubmission(sub.id, true, strings.msgSubmissionApproved) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("approve_submission_${sub.id}")
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.approveAndPayBtn)
                                }
                                OutlinedButton(
                                    onClick = { onReviewSubmission(sub.id, false, strings.msgSubmissionRejected) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("reject_submission_${sub.id}")
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.rejectBtn)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminWithdrawalsPane(
    withdrawals: List<WithdrawalRequestEntity>,
    onReviewWithdrawal: (String, Boolean, String) -> Unit
) {
    val strings = LocalAppStrings.current
    var pendingActionRequest by remember { mutableStateOf<Pair<WithdrawalRequestEntity, Boolean>?>(null) }
    var actionNoteInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (withdrawals.isEmpty()) {
            item {
                EmptyStateCard(
                    title = strings.noWithdrawalsAdminTitle,
                    message = strings.noWithdrawalsAdminMsg,
                    icon = Icons.Filled.Payments
                )
            }
        } else {
            items(withdrawals, key = { it.id }) { req ->
                val status = req.resolvedStatus()
                val provider = req.resolvedProvider()
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = CurrencyFormatter.formatTzs(req.amountTzs),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            WithdrawalStatusPill(status = status)
                        }
                        Text(strings.memberColonFmt.format(req.userName), style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${provider.displayName} • ${req.accountNumber} • ${req.accountName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = strings.channelRequestedFmt.format(
                                provider.providerCode,
                                CurrencyFormatter.formatRelativeTime(req.requestedAt)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!req.adminNote.isNullOrBlank()) {
                            Text(
                                text = strings.adminNoteShortFmt.format(req.adminNote),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (status == WithdrawalStatus.PENDING) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        actionNoteInput = provider.displayName
                                        pendingActionRequest = req to true
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("approve_withdrawal_${req.id}")
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.approveAndPayWithdrawBtn)
                                }
                                OutlinedButton(
                                    onClick = {
                                        actionNoteInput = strings.msgWithdrawalRejected
                                        pendingActionRequest = req to false
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("reject_withdrawal_${req.id}")
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.declineAndRefundBtn)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingActionRequest?.let { (req, approve) ->
        AlertDialog(
            onDismissRequest = { pendingActionRequest = null },
            title = {
                Text(
                    if (approve) strings.approvePayoutDialogTitleFmt.format(req.resolvedProvider().displayName)
                    else strings.declineRefundDialogTitle
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${req.userName} • ${CurrencyFormatter.formatTzs(req.amountTzs)} • ${req.accountNumber} (${req.resolvedProvider().displayName})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = actionNoteInput,
                        onValueChange = { actionNoteInput = it.take(300) },
                        label = {
                            Text(if (approve) strings.providerRefLabel else strings.rejectionReasonLabel)
                        },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReviewWithdrawal(req.id, approve, actionNoteInput)
                        pendingActionRequest = null
                    }
                ) {
                    Text(if (approve) strings.confirmPaidBtn else strings.confirmRefundBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingActionRequest = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun AdminCampaignsPane(
    campaigns: List<SocialTaskEntity>,
    onModerateCampaign: (String, TaskCampaignStatus) -> Unit
) {
    val strings = LocalAppStrings.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (campaigns.isEmpty()) {
            item {
                EmptyStateCard(
                    title = strings.noCampaignsTitle,
                    message = strings.noCampaignsAdminMsg,
                    icon = Icons.Filled.Campaign
                )
            }
        } else {
            items(campaigns, key = { it.id }) { task ->
                val status = task.resolvedStatus()
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlatformBadge(platform = task.resolvedPlatform())
                            CampaignStatusPill(status = status)
                        }
                        Text(task.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = strings.promoterSummaryFmt.format(
                                task.creatorName,
                                task.accountHandle,
                                task.completedSlots,
                                task.totalSlots,
                                CurrencyFormatter.formatTzs(task.rewardTzs)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (status == TaskCampaignStatus.PENDING_REVIEW || status == TaskCampaignStatus.PAUSED) {
                                Button(
                                    onClick = { onModerateCampaign(task.id, TaskCampaignStatus.ACTIVE) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("activate_campaign_${task.id}")
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.approveLiveBtn)
                                }
                            }
                            if (status == TaskCampaignStatus.ACTIVE) {
                                OutlinedButton(
                                    onClick = { onModerateCampaign(task.id, TaskCampaignStatus.PAUSED) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("pause_campaign_${task.id}")
                                ) {
                                    Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.pauseBtn)
                                }
                            }
                            if (status == TaskCampaignStatus.PENDING_REVIEW || status == TaskCampaignStatus.ACTIVE || status == TaskCampaignStatus.PAUSED) {
                                OutlinedButton(
                                    onClick = { onModerateCampaign(task.id, TaskCampaignStatus.REJECTED) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("reject_campaign_${task.id}")
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.rejectAndRefundBtn)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminBusinessAccountPane(
    isWide: Boolean,
    isBusy: Boolean,
    businessAccount: BusinessAccountEntity?,
    businessLedger: List<BusinessLedgerEntryEntity>,
    commissions: List<PlatformCommissionEntity>,
    totalPendingCommissionTzs: Long,
    customerWalletCustodyTzs: Long,
    onConfigureBusinessSettlement: (BusinessSettlementProvider, String) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    val verifiedCommissionTzs = maxOf(
        businessAccount?.totalCommissionEarnedTzs ?: 0L,
        businessLedger.sumOf { it.commissionTzs },
        commissions.sumOf { it.commissionTzs }
    )
    val workerRewardsAllocatedTzs = maxOf(
        businessAccount?.totalWorkerRewardsAllocatedTzs ?: 0L,
        businessLedger.sumOf { it.workerRewardTzs },
        commissions.sumOf { it.workerRewardTzs }
    )
    val grossVerifiedVolumeTzs = maxOf(
        businessAccount?.totalGrossVerifiedVolumeTzs ?: 0L,
        verifiedCommissionTzs + workerRewardsAllocatedTzs
    )
    val completedCount = maxOf(
        businessAccount?.completedTransactionsCount ?: 0,
        businessLedger.size,
        commissions.size
    )

    val savedProviderCode = businessAccount?.settlementProviderCode ?: BusinessSettlementProvider.UNCONFIGURED.code
    var selectedProvider by remember(savedProviderCode) {
        mutableStateOf(
            BusinessSettlementProvider.entries.firstOrNull { it.code == savedProviderCode }
                ?: BusinessSettlementProvider.UNCONFIGURED
        )
    }
    var merchantRefInput by remember(businessAccount?.settlementMerchantRef) {
        mutableStateOf(businessAccount?.settlementMerchantRef.orEmpty())
    }
    val localizedSettlementStatus = strings.settlementStatusMessage(selectedProvider, merchantRefInput)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.businessAccountTitle, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = strings.businessAccountSub,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                val report = strings.buildFinancialReportText(
                                    totalBusinessEarningsTzs = verifiedCommissionTzs,
                                    workerRewardsAllocatedTzs = workerRewardsAllocatedTzs,
                                    pendingCommissionTzs = totalPendingCommissionTzs,
                                    customerWalletCustodyTzs = customerWalletCustodyTzs,
                                    completedCount = completedCount,
                                    settlementProviderLabel = "${strings.settlementProviderLabel(selectedProvider)} ${merchantRefInput}".trim()
                                )
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, strings.reportHeader)
                                    putExtra(Intent.EXTRA_TEXT, report)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, strings.exportReportChooser))
                            },
                            modifier = Modifier.testTag("export_business_report_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.reportBtn)
                        }
                    }

                    if (isWide) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatKpiCard(
                                title = strings.kpiTotalBizEarnings15,
                                value = CurrencyFormatter.formatTzs(verifiedCommissionTzs),
                                subtitle = strings.kpiVerifiedTxCountSubFmt.format(completedCount),
                                icon = Icons.Filled.AccountBalance,
                                accentColor = EmeraldPrimary,
                                modifier = Modifier.weight(1f).testTag("business_total_earnings_kpi")
                            )
                            StatKpiCard(
                                title = strings.kpiPendingCommissions15,
                                value = CurrencyFormatter.formatTzs(totalPendingCommissionTzs),
                                subtitle = strings.kpiEscrowedSlotsSub,
                                icon = Icons.Filled.Schedule,
                                accentColor = AmberGold,
                                modifier = Modifier.weight(1f).testTag("business_pending_commissions_kpi")
                            )
                            StatKpiCard(
                                title = strings.kpiWorkerRewards85,
                                value = CurrencyFormatter.formatTzs(workerRewardsAllocatedTzs),
                                subtitle = strings.kpiAllocatedWorkersSub,
                                icon = Icons.Filled.Payments,
                                accentColor = CobaltAccent,
                                modifier = Modifier.weight(1f).testTag("business_worker_rewards_kpi")
                            )
                        }
                    } else {
                        StatKpiCard(
                            title = strings.kpiTotalBizEarnings15,
                            value = CurrencyFormatter.formatTzs(verifiedCommissionTzs),
                            subtitle = strings.kpiVerifiedTxCountSubFmt.format(completedCount),
                            icon = Icons.Filled.AccountBalance,
                            accentColor = EmeraldPrimary,
                            modifier = Modifier.fillMaxWidth().testTag("business_total_earnings_kpi")
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatKpiCard(
                                title = strings.kpiPendingCommissions15,
                                value = CurrencyFormatter.formatTzs(totalPendingCommissionTzs),
                                subtitle = strings.kpiEscrowedSlotsSub,
                                icon = Icons.Filled.Schedule,
                                accentColor = AmberGold,
                                modifier = Modifier.weight(1f).testTag("business_pending_commissions_kpi")
                            )
                            StatKpiCard(
                                title = strings.kpiWorkerRewards85,
                                value = CurrencyFormatter.formatTzs(workerRewardsAllocatedTzs),
                                subtitle = strings.kpiAllocatedWorkersSub,
                                icon = Icons.Filled.Payments,
                                accentColor = CobaltAccent,
                                modifier = Modifier.weight(1f).testTag("business_worker_rewards_kpi")
                            )
                        }
                    }

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Text(strings.segregationTitle, style = MaterialTheme.typography.titleSmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(strings.customerWalletsCustodyLabel, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = CurrencyFormatter.formatTzs(customerWalletCustodyTzs),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(strings.companyAccountLabel, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = CurrencyFormatter.formatTzs(verifiedCommissionTzs),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldPrimary
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(strings.totalVerifiedGrossLabel, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = CurrencyFormatter.formatTzs(grossVerifiedVolumeTzs),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.settlementIntegrationTitle, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = localizedSettlementStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BusinessSettlementProvider.entries
                            .filter { it != BusinessSettlementProvider.UNCONFIGURED }
                            .forEach { provider ->
                                FilterChip(
                                    selected = selectedProvider == provider,
                                    onClick = { selectedProvider = provider },
                                    label = { Text(provider.displayName) },
                                    modifier = Modifier.testTag("biz_provider_${provider.code.lowercase()}")
                                )
                            }
                    }
                    OutlinedTextField(
                        value = merchantRefInput,
                        onValueChange = { merchantRefInput = it.take(80) },
                        label = { Text(strings.merchantRefLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("biz_merchant_ref_input")
                    )
                    Button(
                        onClick = { onConfigureBusinessSettlement(selectedProvider, merchantRefInput) },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_biz_provider_button")
                    ) {
                        Text(strings.saveSettlementRailBtn)
                    }
                }
            }
        }

        item {
            Text(
                text = strings.completedVerifiedTxTitleFmt.format(completedCount),
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (businessLedger.isEmpty() && commissions.isEmpty()) {
            item {
                EmptyStateCard(
                    title = strings.noVerifiedBizTxTitle,
                    message = strings.noVerifiedBizTxMsg,
                    icon = Icons.Filled.AccountBalance
                )
            }
        } else if (businessLedger.isNotEmpty()) {
            items(businessLedger, key = { it.id }) { entry ->
                BusinessVerifiedEntryCard(
                    taskTitle = entry.taskTitle,
                    commissionTzs = entry.commissionTzs,
                    grossAmountTzs = entry.grossAmountTzs,
                    workerRewardTzs = entry.workerRewardTzs,
                    referenceCode = entry.referenceCode,
                    channelLabel = if (entry.verified) strings.verifiedEscrowSplit else entry.settlementChannel,
                    createdAt = entry.createdAt
                )
            }
        } else {
            items(commissions, key = { it.id }) { item ->
                BusinessVerifiedEntryCard(
                    taskTitle = item.taskTitle,
                    commissionTzs = item.commissionTzs,
                    grossAmountTzs = item.grossAmountTzs,
                    workerRewardTzs = item.workerRewardTzs,
                    referenceCode = item.referenceCode,
                    channelLabel = strings.verifiedCommissionLabel,
                    createdAt = item.createdAt
                )
            }
        }
    }
}

@Composable
private fun BusinessVerifiedEntryCard(
    taskTitle: String,
    commissionTzs: Long,
    grossAmountTzs: Long,
    workerRewardTzs: Long,
    referenceCode: String,
    channelLabel: String,
    createdAt: com.google.firebase.Timestamp?
) {
    val strings = LocalAppStrings.current
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = taskTitle,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "+${CurrencyFormatter.formatTzs(commissionTzs)} (15%)",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    color = EmeraldPrimary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.grossColonFmt.format(CurrencyFormatter.formatTzs(grossAmountTzs)),
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = strings.worker85ColonFmt.format(CurrencyFormatter.formatTzs(workerRewardTzs)),
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider()
            Text(
                text = "$channelLabel • $referenceCode • ${CurrencyFormatter.formatRelativeTime(createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
