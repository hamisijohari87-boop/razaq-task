package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.SocialTaskEntity
import com.example.data.model.SubmissionStatus
import com.example.data.model.TaskSubmissionEntity
import com.example.data.model.UserEntity
import com.example.i18n.LocalAppStrings
import com.example.ui.components.CampaignStatusPill
import com.example.ui.components.DualSectionHubCards
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PlatformBadge
import com.example.ui.components.StatKpiCard
import com.example.ui.components.SubmissionStatusPill
import com.example.ui.components.TzsRewardBadge
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CobaltAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.util.CurrencyFormatter

@Composable
fun DashboardScreen(
    currentUser: UserEntity,
    userSubmissions: List<TaskSubmissionEntity>,
    userCampaigns: List<SocialTaskEntity>,
    liveTasksCount: Int,
    onGoToMarketplace: () -> Unit,
    onGoToPromote: () -> Unit,
    onGoToWallet: () -> Unit
) {
    val strings = LocalAppStrings.current
    val pendingCount = userSubmissions.count { it.resolvedStatus() == SubmissionStatus.PENDING }
    val approvedCount = userSubmissions.count { it.resolvedStatus() == SubmissionStatus.APPROVED }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 820.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = strings.greetingFmt.format(currentUser.fullName),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val phonePart = if (currentUser.phoneNumber.isNotBlank()) " • ${currentUser.phoneNumber}" else ""
                        Text(
                            text = "@${currentUser.username} • ${strings.roleLabel(currentUser.resolvedRole())}$phonePart",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onGoToMarketplace,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("dashboard_earn_button")
                            ) {
                                Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.earnTasksBtn)
                            }
                            OutlinedButton(
                                onClick = onGoToPromote,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("dashboard_promote_button")
                            ) {
                                Icon(Icons.Filled.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.promoteBtn)
                            }
                            OutlinedButton(
                                onClick = onGoToWallet,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("dashboard_wallet_button")
                            ) {
                                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.withdrawBtn)
                            }
                        }
                    }
                }
            }

            item {
                DualSectionHubCards(
                    liveTasksCount = liveTasksCount,
                    totalEarnedTzs = currentUser.totalEarnedTzs,
                    activeCampaignsCount = userCampaigns.size,
                    onBrowseTasks = onGoToMarketplace,
                    onViewEarnings = onGoToWallet,
                    onCreateAdCampaign = onGoToPromote,
                    onTrackCampaigns = onGoToPromote
                )
            }

            item {
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatKpiCard(
                            title = strings.kpiWalletTitle,
                            value = CurrencyFormatter.formatTzs(currentUser.walletBalanceTzs),
                            subtitle = strings.kpiWalletSubAvail,
                            icon = Icons.Filled.AccountBalanceWallet,
                            accentColor = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        StatKpiCard(
                            title = strings.kpiTotalEarnedTitle,
                            value = CurrencyFormatter.formatTzs(currentUser.totalEarnedTzs),
                            subtitle = strings.kpiApprovedTasksSubFmt.format(approvedCount),
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            accentColor = AmberGold,
                            modifier = Modifier.weight(1f)
                        )
                        StatKpiCard(
                            title = strings.kpiPendingReviewTitle,
                            value = "$pendingCount",
                            subtitle = strings.kpiAwaitingVerifySub,
                            icon = Icons.Filled.HourglassTop,
                            accentColor = CobaltAccent,
                            modifier = Modifier.weight(1f)
                        )
                        StatKpiCard(
                            title = strings.kpiMyCampaignsTitle,
                            value = "${userCampaigns.size}",
                            subtitle = strings.kpiSpentSubFmt.format(CurrencyFormatter.formatCompactTzs(currentUser.totalSpentTzs)),
                            icon = Icons.Filled.Campaign,
                            accentColor = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatKpiCard(
                                title = strings.kpiWalletTitle,
                                value = CurrencyFormatter.formatTzs(currentUser.walletBalanceTzs),
                                subtitle = strings.kpiWalletSubAvail,
                                icon = Icons.Filled.AccountBalanceWallet,
                                accentColor = EmeraldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            StatKpiCard(
                                title = strings.kpiTotalEarnedTitle,
                                value = CurrencyFormatter.formatTzs(currentUser.totalEarnedTzs),
                                subtitle = strings.kpiApprovedTasksSubFmt.format(approvedCount),
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                accentColor = AmberGold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatKpiCard(
                                title = strings.kpiPendingProofsTitle,
                                value = "$pendingCount",
                                subtitle = strings.kpiUnderVerifySub,
                                icon = Icons.Filled.HourglassTop,
                                accentColor = CobaltAccent,
                                modifier = Modifier.weight(1f)
                            )
                            StatKpiCard(
                                title = strings.kpiTasksCompletedTitle,
                                value = "${currentUser.tasksCompletedCount}",
                                subtitle = strings.kpiCampaignsRunSubFmt.format(userCampaigns.size),
                                icon = Icons.Filled.CheckCircle,
                                accentColor = EmeraldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (userCampaigns.isNotEmpty()) {
                item {
                    Text(
                        text = strings.myCampaignPerfTitleFmt.format(userCampaigns.size),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                items(userCampaigns, key = { "dash_cmp_${it.id}" }) { campaign ->
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
                                PlatformBadge(platform = campaign.resolvedPlatform())
                                CampaignStatusPill(status = campaign.resolvedStatus())
                            }
                            Text(campaign.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = strings.verifiedCompletionsFmt.format(
                                    campaign.completedSlots,
                                    campaign.totalSlots,
                                    CurrencyFormatter.formatTzs(campaign.resolvedGrossPerSlotTzs() * campaign.totalSlots)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = strings.mySubmissionsTitleFmt.format(userSubmissions.size),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (userSubmissions.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = strings.noSubmissionsTitle,
                        message = strings.noSubmissionsMsg,
                        icon = Icons.Filled.TaskAlt
                    )
                }
            } else {
                items(userSubmissions, key = { it.id }) { submission ->
                    UserSubmissionCard(submission)
                }
            }
        }
    }
}

@Composable
private fun UserSubmissionCard(submission: TaskSubmissionEntity) {
    val strings = LocalAppStrings.current
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
                PlatformBadge(platform = submission.resolvedPlatform())
                SubmissionStatusPill(status = submission.resolvedStatus())
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = submission.taskTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TzsRewardBadge(amountTzs = submission.rewardTzs)
            }

            Text(
                text = "${strings.handleUsedFmt.format(submission.socialHandleUsed)} • ${CurrencyFormatter.formatRelativeTime(submission.submittedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = strings.proofColonFmt.format(submission.proofNote),
                style = MaterialTheme.typography.bodyMedium
            )

            if (!submission.proofImageUri.isNullOrBlank()) {
                AsyncImage(
                    model = submission.proofImageUri,
                    contentDescription = strings.attachScreenshot,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(MaterialTheme.shapes.small)
                )
            }

            if (!submission.reviewerNote.isNullOrBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = strings.reviewerNoteFmt.format(submission.reviewerNote),
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
