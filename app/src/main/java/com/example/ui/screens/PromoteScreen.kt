package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.SocialPlatform
import com.example.data.model.SocialTaskEntity
import com.example.data.model.TaskActionType
import com.example.data.model.TaskCampaignStatus
import com.example.data.model.UserEntity
import com.example.data.repository.RazakRepository
import com.example.i18n.LocalAppStrings
import com.example.ui.components.CampaignStatusPill
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PlatformBadge
import com.example.ui.theme.AmberGoldBright
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.CurrencyFormatter

@Composable
fun PromoteScreen(
    currentUser: UserEntity,
    userCampaigns: List<SocialTaskEntity>,
    isBusy: Boolean,
    onCreateCampaign: (
        title: String,
        description: String,
        platform: SocialPlatform,
        actionType: TaskActionType,
        accountHandle: String,
        targetUrl: String,
        rewardTzs: Long,
        totalSlots: Int,
        onSuccess: () -> Unit
    ) -> Unit,
    onToggleCampaignStatus: (String, TaskCampaignStatus) -> Unit,
    onGoToWalletTopUp: () -> Unit
) {
    val strings = LocalAppStrings.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 860.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1.15f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { PromoteHeroBanner() }
                    item {
                        CampaignCreationCard(
                            walletBalanceTzs = currentUser.walletBalanceTzs,
                            isBusy = isBusy,
                            onCreateCampaign = onCreateCampaign,
                            onGoToWalletTopUp = onGoToWalletTopUp
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(0.95f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = strings.campaignTrackerTitleFmt.format(userCampaigns.size),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    if (userCampaigns.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = strings.noCampaignsTitle,
                                message = strings.noCampaignsMsg,
                                icon = Icons.Filled.Campaign
                            )
                        }
                    } else {
                        items(userCampaigns, key = { it.id }) { campaign ->
                            MyCampaignCard(
                                campaign = campaign,
                                onToggleStatus = onToggleCampaignStatus
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { PromoteHeroBanner() }
                item {
                    CampaignCreationCard(
                        walletBalanceTzs = currentUser.walletBalanceTzs,
                        isBusy = isBusy,
                        onCreateCampaign = onCreateCampaign,
                        onGoToWalletTopUp = onGoToWalletTopUp
                    )
                }
                item {
                    Text(
                        text = strings.campaignTrackerTitleFmt.format(userCampaigns.size),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                if (userCampaigns.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = strings.noCampaignsTitle,
                            message = strings.noCampaignsMsg,
                            icon = Icons.Filled.Campaign
                        )
                    }
                } else {
                    items(userCampaigns, key = { it.id }) { campaign ->
                        MyCampaignCard(
                            campaign = campaign,
                            onToggleStatus = onToggleCampaignStatus
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PromoteHeroBanner() {
    val strings = LocalAppStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(EmeraldDeep, EmeraldPrimary)))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_promote_banner),
            contentDescription = strings.promoteHeroTitle,
            contentScale = ContentScale.Crop,
            alpha = 0.38f,
            modifier = Modifier.matchParentSize()
        )
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = strings.promoteHeroTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.promoteHeroBody,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CampaignCreationCard(
    walletBalanceTzs: Long,
    isBusy: Boolean,
    onCreateCampaign: (String, String, SocialPlatform, TaskActionType, String, String, Long, Int, () -> Unit) -> Unit,
    onGoToWalletTopUp: () -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedPlatform by rememberSaveable { mutableStateOf(SocialPlatform.TIKTOK) }
    var selectedAction by rememberSaveable { mutableStateOf(TaskActionType.FOLLOW) }
    var title by rememberSaveable { mutableStateOf("") }
    var accountHandle by rememberSaveable { mutableStateOf("") }
    var targetUrl by rememberSaveable { mutableStateOf("https://") }
    var description by rememberSaveable { mutableStateOf("") }
    var rewardInput by rememberSaveable { mutableStateOf("400") }
    var slotsInput by rememberSaveable { mutableStateOf("25") }

    val minBudget = RazakRepository.MIN_CAMPAIGN_BUDGET_TZS
    val maxBudget = RazakRepository.MAX_CAMPAIGN_BUDGET_TZS
    val rewardTzs = rewardInput.toLongOrNull() ?: 0L
    val totalSlots = slotsInput.toIntOrNull() ?: 0
    val safeBudgetBig = rewardTzs.toBigInteger() * totalSlots.toBigInteger()
    val totalBudget = safeBudgetBig
        .coerceAtMost(Long.MAX_VALUE.toBigInteger())
        .toLong()
        .coerceAtLeast(0L)
    val (_, workerRewardPerSlot) = RazakRepository.computeCommissionSplit(rewardTzs)
    val (platformCommissionTotal, workerRewardsPoolTotal) = RazakRepository.computeCommissionSplit(totalBudget)

    val budgetError: String? = when {
        rewardTzs < 100L -> strings.errMinSlotBudget
        rewardTzs > 500_000L -> strings.errMaxSlotBudget
        totalSlots < 5 -> strings.errMinSlots
        safeBudgetBig < minBudget.toBigInteger() ->
            strings.errMinCampaignBudgetFmt.format(
                CurrencyFormatter.formatTzs(minBudget),
                CurrencyFormatter.formatTzs(totalBudget)
            )
        safeBudgetBig > maxBudget.toBigInteger() ->
            strings.errMaxCampaignBudgetFmt.format(
                CurrencyFormatter.formatTzs(maxBudget),
                CurrencyFormatter.formatTzs(totalBudget)
            )
        else -> null
    }
    val isBudgetValid = budgetError == null
    val hasSufficientBalance = walletBalanceTzs >= totalBudget && totalBudget > 0L

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(strings.createOrganicAdTitle, style = MaterialTheme.typography.titleLarge)
            Text(
                text = strings.budgetRangeLabelFmt.format(
                    CurrencyFormatter.formatTzs(minBudget),
                    CurrencyFormatter.formatTzs(maxBudget)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(
                        text = strings.organicPolicyNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(strings.choosePlatformStep, style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SocialPlatform.promotePlatforms.forEach { platform ->
                    FilterChip(
                        selected = selectedPlatform == platform,
                        onClick = { selectedPlatform = platform },
                        label = { Text(strings.platformLabel(platform)) },
                        modifier = Modifier.testTag("promote_platform_${platform.name.lowercase()}")
                    )
                }
            }

            Text(strings.engagementActionStep, style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TaskActionType.entries.forEach { action ->
                    FilterChip(
                        selected = selectedAction == action,
                        onClick = {
                            selectedAction = action
                            rewardInput = action.defaultRewardTzs.toString()
                        },
                        label = { Text(strings.actionLabel(action)) },
                        modifier = Modifier.testTag("promote_action_${action.name.lowercase()}")
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(strings.campaignTitleLabel) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("campaign_title_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = accountHandle,
                    onValueChange = { accountHandle = it },
                    label = { Text(strings.accountHandleLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("campaign_handle_input")
                )
                OutlinedTextField(
                    value = targetUrl,
                    onValueChange = { targetUrl = it },
                    label = { Text(strings.profileUrlLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("campaign_url_input")
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(strings.instructionsLabel) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("campaign_desc_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = rewardInput,
                    onValueChange = { rewardInput = it.filter(Char::isDigit).take(7) },
                    label = { Text(strings.budgetPerSlotLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = rewardTzs < 100L || rewardTzs > 500_000L,
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("campaign_reward_input")
                )
                OutlinedTextField(
                    value = slotsInput,
                    onValueChange = { slotsInput = it.filter(Char::isDigit).take(5) },
                    label = { Text(strings.targetSlotsLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = totalSlots < 5,
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("campaign_slots_input")
                )
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.totalBudgetLabel, style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = CurrencyFormatter.formatTzs(totalBudget),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (isBudgetValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.platformCommissionLabel, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "-${CurrencyFormatter.formatTzs(platformCommissionTotal)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AmberGoldBright,
                            modifier = Modifier.testTag("campaign_commission_preview")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.workerRewardsPoolLabel, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${CurrencyFormatter.formatTzs(workerRewardsPoolTotal)} (${CurrencyFormatter.formatTzs(workerRewardPerSlot)}${strings.perTaskShort})",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("campaign_worker_pool_preview")
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.yourWalletBalanceLabel, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = CurrencyFormatter.formatTzs(walletBalanceTzs),
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasSufficientBalance) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = strings.commissionRuleNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (budgetError != null) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("campaign_budget_error")
                ) {
                    Text(
                        text = budgetError,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (!hasSufficientBalance && totalBudget > 0L) {
                OutlinedButton(
                    onClick = onGoToWalletTopUp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("campaign_topup_button")
                ) {
                    Text(strings.insufficientBalanceTopUpBtn)
                }
            }

            Button(
                onClick = {
                    onCreateCampaign(
                        title,
                        description,
                        selectedPlatform,
                        selectedAction,
                        accountHandle,
                        targetUrl,
                        rewardTzs,
                        totalSlots
                    ) {
                        title = ""
                        accountHandle = ""
                        targetUrl = "https://"
                        description = ""
                    }
                },
                enabled = !isBusy && isBudgetValid && hasSufficientBalance,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("launch_campaign_button")
            ) {
                Icon(Icons.Filled.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.fundAndLaunchBtnFmt.format(CurrencyFormatter.formatTzs(totalBudget)))
            }
        }
    }
}

@Composable
private fun MyCampaignCard(
    campaign: SocialTaskEntity,
    onToggleStatus: (String, TaskCampaignStatus) -> Unit
) {
    val strings = LocalAppStrings.current
    val progress = if (campaign.totalSlots > 0) {
        (campaign.completedSlots.toFloat() / campaign.totalSlots.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val pct = (progress * 100).toInt()
    val grossPerSlot = campaign.resolvedGrossPerSlotTzs()
    val totalBudget = grossPerSlot * campaign.totalSlots
    val spentBudget = grossPerSlot * campaign.completedSlots
    val remainingBudget = (totalBudget - spentBudget).coerceAtLeast(0L)
    val status = campaign.resolvedStatus()

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
                PlatformBadge(platform = campaign.resolvedPlatform())
                CampaignStatusPill(status = status)
            }
            Text(campaign.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${strings.actionLabel(campaign.resolvedActionType())} • ${campaign.accountHandle} • ${strings.workerPayoutPerTaskFmt.format(CurrencyFormatter.formatTzs(campaign.rewardTzs))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.verifiedCountFmt.format(campaign.completedSlots, campaign.totalSlots, pct),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = strings.budgetShortFmt.format(CurrencyFormatter.formatTzs(totalBudget)),
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberGoldBright
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.spentShortFmt.format(CurrencyFormatter.formatTzs(spentBudget)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = strings.remainingShortFmt.format(CurrencyFormatter.formatTzs(remainingBudget)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = strings.commissionPaidCampaignFmt.format(CurrencyFormatter.formatTzs(campaign.commissionEarnedTzs)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (status == TaskCampaignStatus.ACTIVE || status == TaskCampaignStatus.PAUSED) {
                OutlinedButton(
                    onClick = {
                        val next = if (status == TaskCampaignStatus.ACTIVE) {
                            TaskCampaignStatus.PAUSED
                        } else {
                            TaskCampaignStatus.ACTIVE
                        }
                        onToggleStatus(campaign.id, next)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("toggle_own_campaign_${campaign.id}")
                ) {
                    Icon(
                        imageVector = if (status == TaskCampaignStatus.ACTIVE) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (status == TaskCampaignStatus.ACTIVE) strings.pauseCampaign else strings.resumeCampaign)
                }
            }
        }
    }
}
