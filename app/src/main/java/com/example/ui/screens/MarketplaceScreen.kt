package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.SocialPlatform
import com.example.data.model.SocialTaskEntity
import com.example.data.model.SubmissionStatus
import com.example.data.model.TaskSubmissionEntity
import com.example.data.model.UserEntity
import com.example.i18n.LocalAppStrings
import com.example.ui.components.DualSectionHubCards
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PlatformBadge
import com.example.ui.components.SocialTaskCard
import com.example.ui.components.TzsRewardBadge
import com.example.ui.theme.AmberGoldBright
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    currentUser: UserEntity,
    tasks: List<SocialTaskEntity>,
    userSubmissions: List<TaskSubmissionEntity>,
    userCampaignsCount: Int,
    selectedPlatform: SocialPlatform?,
    searchQuery: String,
    selectedTask: SocialTaskEntity?,
    isBusy: Boolean,
    onSelectPlatform: (SocialPlatform?) -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectTask: (SocialTaskEntity?) -> Unit,
    onSubmitProof: (String, String, String, String?, () -> Unit) -> Unit,
    onNavigateToPromote: () -> Unit,
    onNavigateToWallet: () -> Unit
) {
    val strings = LocalAppStrings.current
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val submittedTaskIds = remember(userSubmissions) {
        userSubmissions
            .filter { it.resolvedStatus() != SubmissionStatus.REJECTED }
            .map { it.taskId }
            .toSet()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTwoPane = maxWidth >= 880.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Main Marketplace Feed / Grid
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 300.dp),
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MarketplaceHeroCard(
                        availableCount = tasks.size,
                        walletBalanceTzs = currentUser.walletBalanceTzs,
                        onPromoteClick = onNavigateToPromote
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    DualSectionHubCards(
                        liveTasksCount = tasks.size,
                        totalEarnedTzs = currentUser.totalEarnedTzs,
                        activeCampaignsCount = userCampaignsCount,
                        onBrowseTasks = {
                            scope.launch { gridState.animateScrollToItem(2) }
                        },
                        onViewEarnings = onNavigateToWallet,
                        onCreateAdCampaign = onNavigateToPromote,
                        onTrackCampaigns = onNavigateToPromote
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = { Text(strings.searchPlaceholder) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = strings.searchDesc) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChange("") }) {
                                        Icon(Icons.Filled.Close, contentDescription = strings.clearSearchDesc)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("marketplace_search_input")
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedPlatform == null,
                                    onClick = { onSelectPlatform(null) },
                                    label = { Text(strings.allPlatforms) },
                                    modifier = Modifier.testTag("filter_all")
                                )
                            }
                            items(SocialPlatform.entries) { platform ->
                                FilterChip(
                                    selected = selectedPlatform == platform,
                                    onClick = {
                                        onSelectPlatform(if (selectedPlatform == platform) null else platform)
                                    },
                                    label = { Text(strings.platformLabel(platform)) },
                                    modifier = Modifier.testTag("filter_${platform.name.lowercase()}")
                                )
                            }
                        }
                    }
                }

                if (tasks.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyStateCard(
                            title = strings.noMatchingTasksTitle,
                            message = strings.noMatchingTasksMsg,
                            icon = Icons.Filled.TaskAlt
                        )
                    }
                } else {
                    items(tasks, key = { it.id }) { task ->
                        SocialTaskCard(
                            task = task,
                            isAlreadySubmitted = submittedTaskIds.contains(task.id),
                            isOwnCampaign = task.creatorUserId == currentUser.userId,
                            isSelected = isTwoPane && selectedTask?.id == task.id,
                            onClick = { onSelectTask(task) }
                        )
                    }
                }
            }

            // Desktop / Tablet List-Detail Side Pane
            if (isTwoPane) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    tonalElevation = 1.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    val activeTask = selectedTask ?: tasks.firstOrNull()
                    if (activeTask != null) {
                        TaskDetailAndProofContent(
                            task = activeTask,
                            currentUser = currentUser,
                            isAlreadySubmitted = submittedTaskIds.contains(activeTask.id),
                            isBusy = isBusy,
                            showCloseButton = false,
                            onClose = { onSelectTask(null) },
                            onSubmitProof = onSubmitProof
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyStateCard(
                                title = strings.selectTaskTitle,
                                message = strings.selectTaskMsg
                            )
                        }
                    }
                }
            }
        }

        // Mobile Bottom Sheet for Task Detail & Proof Submission
        if (!isTwoPane && selectedTask != null) {
            BackHandler { onSelectTask(null) }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { onSelectTask(null) },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                TaskDetailAndProofContent(
                    task = selectedTask,
                    currentUser = currentUser,
                    isAlreadySubmitted = submittedTaskIds.contains(selectedTask.id),
                    isBusy = isBusy,
                    showCloseButton = true,
                    onClose = { onSelectTask(null) },
                    onSubmitProof = onSubmitProof
                )
            }
        }
    }
}

@Composable
private fun MarketplaceHeroCard(
    availableCount: Int,
    walletBalanceTzs: Long,
    onPromoteClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(EmeraldDeep, EmeraldPrimary)))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_hero_marketplace),
            contentDescription = strings.marketHeroTitle,
            contentScale = ContentScale.Crop,
            alpha = 0.35f,
            modifier = Modifier.matchParentSize()
        )
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = AmberGoldBright
                ) {
                    Text(
                        text = strings.liveTasksPillFmt.format(availableCount),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF451A03)
                    )
                }
                Text(
                    text = strings.balanceColonFmt.format(CurrencyFormatter.formatTzs(walletBalanceTzs)),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = strings.marketHeroTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = strings.marketHeroBody,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.86f)
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = onPromoteClick,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
                modifier = Modifier.testTag("hero_promote_cta")
            ) {
                Text(strings.marketHeroCta, color = Color.White)
            }
        }
    }
}

@Composable
fun TaskDetailAndProofContent(
    task: SocialTaskEntity,
    currentUser: UserEntity,
    isAlreadySubmitted: Boolean,
    isBusy: Boolean,
    showCloseButton: Boolean,
    onClose: () -> Unit,
    onSubmitProof: (String, String, String, String?, () -> Unit) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var socialHandleUsed by rememberSaveable(task.id) { mutableStateOf("@${currentUser.username}") }
    var proofNote by rememberSaveable(task.id) { mutableStateOf("") }
    var selectedImageUri by rememberSaveable(task.id) { mutableStateOf<String?>(null) }

    // Play-compliant zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri.toString()
        }
    }

    val isOwnCampaign = task.creatorUserId == currentUser.userId
    val remaining = (task.totalSlots - task.completedSlots).coerceAtLeast(0)
    val platform = task.resolvedPlatform()
    val platformName = strings.platformLabel(platform)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlatformBadge(platform = platform)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TzsRewardBadge(amountTzs = task.rewardTzs)
                if (showCloseButton) {
                    IconButton(onClick = onClose, modifier = Modifier.testTag("close_task_detail")) {
                        Icon(Icons.Filled.Close, contentDescription = strings.close)
                    }
                }
            }
        }

        Text(text = task.title, style = MaterialTheme.typography.headlineSmall)

        Text(
            text = "${strings.promotedByFmt.format(task.creatorName)} • ${strings.slotsRemainingFmt.format(remaining, task.totalSlots)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(strings.taskInstructions, style = MaterialTheme.typography.labelLarge)
                Text(task.description, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = strings.targetAccountFmt.format(task.accountHandle),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        OutlinedButton(
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(task.targetUrl))
                    context.startActivity(intent)
                } catch (_: Exception) {
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("open_social_link_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.openPlatformAccountFmt.format(platformName))
        }

        HorizontalDivider()

        when {
            isOwnCampaign -> {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = strings.ownCampaignNotice,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            isAlreadySubmitted -> {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = strings.alreadySubmittedNotice,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(strings.submitProofTitle, style = MaterialTheme.typography.titleMedium)
                }

                OutlinedTextField(
                    value = socialHandleUsed,
                    onValueChange = { socialHandleUsed = it },
                    label = { Text(strings.yourHandleLabelFmt.format(platformName)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("proof_handle_input")
                )

                OutlinedTextField(
                    value = proofNote,
                    onValueChange = { proofNote = it },
                    label = { Text(strings.proofNoteLabel) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("proof_note_input")
                )

                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pick_proof_screenshot_button")
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedImageUri == null) strings.attachScreenshot else strings.changeScreenshot)
                }

                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = strings.attachScreenshot,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                }

                Button(
                    onClick = {
                        onSubmitProof(task.id, socialHandleUsed, proofNote, selectedImageUri) {
                            proofNote = ""
                            selectedImageUri = null
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_proof_button")
                ) {
                    Text(strings.submitProofAndClaimFmt.format(CurrencyFormatter.formatTzs(task.rewardTzs)))
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
