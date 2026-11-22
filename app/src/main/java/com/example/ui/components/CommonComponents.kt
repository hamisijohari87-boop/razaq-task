package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SocialPlatform
import com.example.data.model.SocialTaskEntity
import com.example.data.model.SubmissionStatus
import com.example.data.model.TaskCampaignStatus
import com.example.data.model.WithdrawalStatus
import com.example.i18n.LocalAppStrings
import com.example.ui.theme.AmberGoldBright
import com.example.ui.theme.AmberGoldContainer
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.InstagramPink
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OnAmberGoldContainer
import com.example.ui.theme.StatusApprovedBg
import com.example.ui.theme.StatusApprovedFg
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingFg
import com.example.ui.theme.StatusRejectedBg
import com.example.ui.theme.StatusRejectedFg
import com.example.ui.theme.TikTokTeal
import com.example.ui.theme.XBlack
import com.example.ui.theme.YouTubeRed
import com.example.util.CurrencyFormatter

fun SocialPlatform.brandColor(): Color = when (this) {
    SocialPlatform.INSTAGRAM -> InstagramPink
    SocialPlatform.TIKTOK -> TikTokTeal
    SocialPlatform.YOUTUBE -> YouTubeRed
    SocialPlatform.FACEBOOK -> FacebookBlue
    SocialPlatform.X_TWITTER -> XBlack
}

fun SocialPlatform.brandIcon(): ImageVector = when (this) {
    SocialPlatform.INSTAGRAM -> Icons.Filled.CameraAlt
    SocialPlatform.TIKTOK -> Icons.Filled.MusicNote
    SocialPlatform.YOUTUBE -> Icons.Filled.PlayArrow
    SocialPlatform.FACEBOOK -> Icons.Filled.ThumbUp
    SocialPlatform.X_TWITTER -> Icons.Filled.Share
}

@Composable
fun RazakBrandLogo(modifier: Modifier = Modifier, compact: Boolean = false) {
    val strings = LocalAppStrings.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 34.dp else 42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(EmeraldPrimary, EmeraldDeep)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = strings.appName,
                tint = AmberGoldBright,
                modifier = Modifier.size(if (compact) 20.dp else 24.dp)
            )
        }
        Column {
            Text(
                text = strings.appName,
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            if (!compact) {
                Text(
                    text = strings.brandTagline,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PlatformBadge(platform: SocialPlatform, modifier: Modifier = Modifier) {
    val bg = platform.brandColor()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, bg.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = platform.brandIcon(),
                contentDescription = platform.displayName,
                tint = bg,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = platform.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = bg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatusPill(label: String, bgColor: Color, fgColor: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = bgColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fgColor
        )
    }
}

@Composable
fun SubmissionStatusPill(status: SubmissionStatus) {
    val strings = LocalAppStrings.current
    val label = strings.submissionStatusLabel(status)
    when (status) {
        SubmissionStatus.PENDING -> StatusPill(label, StatusPendingBg, StatusPendingFg)
        SubmissionStatus.APPROVED -> StatusPill(label, StatusApprovedBg, StatusApprovedFg)
        SubmissionStatus.REJECTED -> StatusPill(label, StatusRejectedBg, StatusRejectedFg)
    }
}

@Composable
fun CampaignStatusPill(status: TaskCampaignStatus) {
    val strings = LocalAppStrings.current
    val label = strings.campaignStatusLabel(status)
    when (status) {
        TaskCampaignStatus.PENDING_REVIEW -> StatusPill(label, StatusPendingBg, StatusPendingFg)
        TaskCampaignStatus.ACTIVE -> StatusPill(label, StatusApprovedBg, StatusApprovedFg)
        TaskCampaignStatus.PAUSED -> StatusPill(label, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        TaskCampaignStatus.COMPLETED -> StatusPill(label, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        TaskCampaignStatus.REJECTED -> StatusPill(label, StatusRejectedBg, StatusRejectedFg)
    }
}

@Composable
fun WithdrawalStatusPill(status: WithdrawalStatus) {
    val strings = LocalAppStrings.current
    val label = strings.withdrawalStatusLabel(status)
    when (status) {
        WithdrawalStatus.PENDING -> StatusPill(label, StatusPendingBg, StatusPendingFg)
        WithdrawalStatus.APPROVED -> StatusPill(label, StatusApprovedBg, StatusApprovedFg)
        WithdrawalStatus.REJECTED -> StatusPill(label, StatusRejectedBg, StatusRejectedFg)
    }
}

@Composable
fun TzsRewardBadge(amountTzs: Long, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = AmberGoldContainer
    ) {
        Text(
            text = "+${CurrencyFormatter.formatTzs(amountTzs)}",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = OnAmberGoldContainer
        )
    }
}

@Composable
fun StatKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SocialTaskCard(
    task: SocialTaskEntity,
    isAlreadySubmitted: Boolean,
    isOwnCampaign: Boolean,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val progress = if (task.totalSlots > 0) {
        (task.completedSlots.toFloat() / task.totalSlots.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val remaining = (task.totalSlots - task.completedSlots).coerceAtLeast(0)

    val platform = task.resolvedPlatform()
    val actionType = task.resolvedActionType()

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlatformBadge(platform = platform)
                TzsRewardBadge(amountTzs = task.rewardTzs)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${strings.actionLabel(actionType)} • ${task.accountHandle}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.slotsLeftFmt.format(remaining, task.totalSlots),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when {
                    isOwnCampaign -> StatusPill(strings.yourCampaignPill, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                    isAlreadySubmitted -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = strings.submittedStatus,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.submittedStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    else -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = strings.startTask,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.startTask,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    message: String,
    icon: ImageVector = Icons.Filled.Public,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DualSectionHubCards(
    liveTasksCount: Int,
    totalEarnedTzs: Long,
    activeCampaignsCount: Int,
    onBrowseTasks: () -> Unit,
    onViewEarnings: () -> Unit,
    onCreateAdCampaign: () -> Unit,
    onTrackCampaigns: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isWide = maxWidth >= 680.dp
        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EarnMoneyHubCard(
                    liveTasksCount = liveTasksCount,
                    totalEarnedTzs = totalEarnedTzs,
                    onBrowseTasks = onBrowseTasks,
                    onViewEarnings = onViewEarnings,
                    modifier = Modifier.weight(1f)
                )
                GrowSocialHubCard(
                    activeCampaignsCount = activeCampaignsCount,
                    onCreateAdCampaign = onCreateAdCampaign,
                    onTrackCampaigns = onTrackCampaigns,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EarnMoneyHubCard(
                    liveTasksCount = liveTasksCount,
                    totalEarnedTzs = totalEarnedTzs,
                    onBrowseTasks = onBrowseTasks,
                    onViewEarnings = onViewEarnings
                )
                GrowSocialHubCard(
                    activeCampaignsCount = activeCampaignsCount,
                    onCreateAdCampaign = onCreateAdCampaign,
                    onTrackCampaigns = onTrackCampaigns
                )
            }
        }
    }
}

@Composable
private fun EarnMoneyHubCard(
    liveTasksCount: Int,
    totalEarnedTzs: Long,
    onBrowseTasks: () -> Unit,
    onViewEarnings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_card_earn_money"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Payments,
                            contentDescription = strings.earnMoneyTitle,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = strings.section1Label,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.earnMoneyTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusPill(
                    label = strings.tasksCountPillFmt.format(liveTasksCount),
                    bgColor = StatusApprovedBg,
                    fgColor = StatusApprovedFg
                )
            }

            Text(
                text = strings.earnMoneyDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = strings.approvedEarningsFmt.format(CurrencyFormatter.formatTzs(totalEarnedTzs)),
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                color = EmeraldPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onBrowseTasks,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("earn_money_browse_button")
                ) {
                    Text(strings.browseTasksBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = onViewEarnings,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("earn_money_history_button")
                ) {
                    Text(strings.earningsBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun GrowSocialHubCard(
    activeCampaignsCount: Int,
    onCreateAdCampaign: () -> Unit,
    onTrackCampaigns: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val accent = MaterialTheme.colorScheme.secondary
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_card_grow_social"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RocketLaunch,
                            contentDescription = strings.growSocialTitle,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = strings.section2Label,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.growSocialTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusPill(
                    label = strings.organicAdsPill,
                    bgColor = AmberGoldContainer,
                    fgColor = OnAmberGoldContainer
                )
            }

            Text(
                text = strings.growSocialDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = strings.myCampaignsNoBotsFmt.format(activeCampaignsCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCreateAdCampaign,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("grow_social_create_button")
                ) {
                    Text(strings.createAdBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = onTrackCampaigns,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("grow_social_track_button")
                ) {
                    Text(strings.performanceBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

