package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.SessionStore
import com.example.data.model.SubmissionStatus
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.WithdrawalStatus
import com.example.data.remote.GoogleAuthHelper
import com.example.data.repository.RazakRepository
import com.example.i18n.AppLanguage
import com.example.i18n.LocalAppStrings
import com.example.i18n.RazakTranslations
import com.example.ui.components.LanguagePickerDialog
import com.example.ui.components.LanguageSettingsSection
import com.example.ui.components.OnboardingLanguageScreen
import com.example.ui.components.RazakBrandLogo
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MarketplaceScreen
import com.example.ui.screens.PromoteScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.state.MainTab
import com.example.ui.state.RazakViewModel
import com.example.ui.state.UiState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RazakTaskTheme
import com.example.util.CurrencyFormatter
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val firestore = FirebaseFirestore.getInstance(
            applicationContext.getString(R.string.firestore_database_id)
        )
        val sessionStore = SessionStore(applicationContext)
        val repository = RazakRepository(
            db = firestore,
            auth = Firebase.auth,
            sessionStore = sessionStore
        )

        setContent {
            val darkOverride by repository.darkThemeFlow.collectAsStateWithLifecycle(initialValue = null)
            val languageCode by repository.languageCodeFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.DEFAULT.code)
            val onboardingCompleted by remember(repository) {
                repository.onboardingCompletedFlow.map<Boolean, Boolean?> { it }
            }.collectAsStateWithLifecycle(initialValue = null)

            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = darkOverride ?: systemDark
            val scope = rememberCoroutineScope()

            val appLanguage = AppLanguage.fromCode(languageCode)
            val strings = remember(appLanguage) { RazakTranslations.forLanguage(appLanguage) }
            val layoutDirection = if (appLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(
                LocalAppStrings provides strings,
                LocalLayoutDirection provides layoutDirection
            ) {
                RazakTaskTheme(darkTheme = useDarkTheme) {
                    when (onboardingCompleted) {
                        null -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        false -> {
                            OnboardingLanguageScreen(
                                selectedLanguage = appLanguage,
                                onSelectLanguage = { lang ->
                                    scope.launch { repository.setLanguageCode(lang.code) }
                                },
                                onContinue = {
                                    scope.launch { repository.setOnboardingCompleted(true) }
                                }
                            )
                        }
                        true -> {
                            RazakAppRoot(
                                repository = repository,
                                currentLanguage = appLanguage,
                                isDarkTheme = useDarkTheme,
                                onToggleTheme = {
                                    scope.launch { repository.setDarkTheme(!useDarkTheme) }
                                },
                                onSelectLanguage = { lang ->
                                    scope.launch { repository.setLanguageCode(lang.code) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private data class NavDestination(
    val tab: MainTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
private fun RazakAppRoot(
    repository: RazakRepository,
    currentLanguage: AppLanguage,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit
) {
    val strings = LocalAppStrings.current
    val authFlow = remember { Firebase.auth.authStateFlow() }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()

    val signedInUser = currentUser
    if (signedInUser == null) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                AuthScreen(
                    currentLanguage = currentLanguage,
                    onSelectLanguage = onSelectLanguage,
                    onAuthSuccess = { /* Reactive authStateFlow automatically transitions UI */ }
                )
            }
        }
    } else {
        key(signedInUser.uid) {
            val viewModel: RazakViewModel = viewModel(
                key = signedInUser.uid,
                factory = RazakViewModel.Factory(repository, signedInUser.uid)
            )
            val snackbarHostState = remember { SnackbarHostState() }
            val userState by viewModel.userState.collectAsStateWithLifecycle()
            val vmLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.events.collect { msg ->
                    snackbarHostState.showSnackbar(msg)
                }
            }

            when (val state = userState) {
                UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = strings.syncingProfile,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.widthIn(max = 440.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(strings.couldNotLoadProfile, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = viewModel::retryProfileInitialization,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(strings.retry)
                                    }
                                    OutlinedButton(
                                        onClick = { GoogleAuthHelper.signOut(credentialManager, scope) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(strings.signOut)
                                    }
                                }
                            }
                        }
                    }
                }
                is UiState.Success -> {
                    AuthenticatedShell(
                        user = state.data,
                        firebaseUser = signedInUser,
                        viewModel = viewModel,
                        currentLanguage = vmLanguage,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = onToggleTheme,
                        onSelectLanguage = viewModel::changeLanguage,
                        onSignOut = { GoogleAuthHelper.signOut(credentialManager, scope) },
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedShell(
    user: UserEntity,
    firebaseUser: FirebaseUser,
    viewModel: RazakViewModel,
    currentLanguage: AppLanguage,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSignOut: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val strings = LocalAppStrings.current
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val marketplaceTasks by viewModel.marketplaceTasks.collectAsStateWithLifecycle()
    val userCampaigns by viewModel.userCampaigns.collectAsStateWithLifecycle()
    val userSubmissions by viewModel.userSubmissions.collectAsStateWithLifecycle()
    val userTransactions by viewModel.userTransactions.collectAsStateWithLifecycle()
    val userWithdrawals by viewModel.userWithdrawals.collectAsStateWithLifecycle()
    val selectedPlatform by viewModel.selectedPlatform.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTask by viewModel.selectedTask.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val adminSubmissions by viewModel.adminSubmissions.collectAsStateWithLifecycle()
    val adminWithdrawals by viewModel.adminWithdrawals.collectAsStateWithLifecycle()
    val adminCampaigns by viewModel.adminCampaigns.collectAsStateWithLifecycle()
    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val adminCommissions by viewModel.adminCommissions.collectAsStateWithLifecycle()
    val businessAccount by viewModel.businessAccount.collectAsStateWithLifecycle()
    val businessLedger by viewModel.businessLedger.collectAsStateWithLifecycle()

    var showProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }

    val isAdmin = remember(user.role, firebaseUser.email, firebaseUser.isEmailVerified) {
        user.resolvedRole() == UserRole.ADMIN ||
            (firebaseUser.isEmailVerified && firebaseUser.email.equals(RazakRepository.ADMIN_EMAIL, ignoreCase = true))
    }

    val pendingAdminCount = remember(adminSubmissions, adminWithdrawals) {
        adminSubmissions.count { it.resolvedStatus() == SubmissionStatus.PENDING } +
            adminWithdrawals.count { it.resolvedStatus() == WithdrawalStatus.PENDING }
    }

    val destinations = remember(isAdmin, pendingAdminCount, strings) {
        buildList {
            add(NavDestination(MainTab.MARKETPLACE, strings.navMarket, Icons.Filled.Storefront, Icons.Outlined.Storefront))
            add(NavDestination(MainTab.PROMOTE, strings.navPromote, Icons.Filled.Campaign, Icons.Outlined.Campaign))
            add(NavDestination(MainTab.DASHBOARD, strings.navDashboard, Icons.Filled.Dashboard, Icons.Outlined.Dashboard))
            add(NavDestination(MainTab.WALLET, strings.navWallet, Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet))
            if (isAdmin) {
                add(NavDestination(MainTab.ADMIN, strings.navAdmin, Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings, pendingAdminCount))
            }
        }
    }

    if (selectedTab != MainTab.MARKETPLACE) {
        BackHandler {
            viewModel.selectTab(MainTab.MARKETPLACE)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isDesktopDrawer = maxWidth >= 1000.dp
        val isTabletRail = maxWidth in 600.dp..999.dp

        val mainContent: @Composable (Modifier) -> Unit = { contentModifier ->
            Column(modifier = contentModifier.fillMaxSize()) {
                when (selectedTab) {
                    MainTab.MARKETPLACE -> MarketplaceScreen(
                        currentUser = user,
                        tasks = marketplaceTasks,
                        userSubmissions = userSubmissions,
                        userCampaignsCount = userCampaigns.size,
                        selectedPlatform = selectedPlatform,
                        searchQuery = searchQuery,
                        selectedTask = selectedTask,
                        isBusy = isBusy,
                        onSelectPlatform = viewModel::selectPlatformFilter,
                        onSearchChange = viewModel::updateSearchQuery,
                        onSelectTask = viewModel::selectTaskForDetail,
                        onSubmitProof = viewModel::submitTaskProof,
                        onNavigateToPromote = { viewModel.selectTab(MainTab.PROMOTE) },
                        onNavigateToWallet = { viewModel.selectTab(MainTab.WALLET) }
                    )
                    MainTab.PROMOTE -> PromoteScreen(
                        currentUser = user,
                        userCampaigns = userCampaigns,
                        isBusy = isBusy,
                        onCreateCampaign = viewModel::createPromotionCampaign,
                        onToggleCampaignStatus = viewModel::moderateCampaign,
                        onGoToWalletTopUp = { viewModel.selectTab(MainTab.WALLET) }
                    )
                    MainTab.DASHBOARD -> DashboardScreen(
                        currentUser = user,
                        userSubmissions = userSubmissions,
                        userCampaigns = userCampaigns,
                        liveTasksCount = marketplaceTasks.size,
                        onGoToMarketplace = { viewModel.selectTab(MainTab.MARKETPLACE) },
                        onGoToPromote = { viewModel.selectTab(MainTab.PROMOTE) },
                        onGoToWallet = { viewModel.selectTab(MainTab.WALLET) }
                    )
                    MainTab.WALLET -> WalletScreen(
                        currentUser = user,
                        transactions = userTransactions,
                        withdrawals = userWithdrawals,
                        isBusy = isBusy,
                        onDepositTzs = viewModel::depositToWallet,
                        onRequestWithdrawal = viewModel::requestWithdrawal
                    )
                    MainTab.ADMIN -> {
                        if (isAdmin) {
                            AdminScreen(
                                users = adminUsers,
                                submissions = adminSubmissions,
                                withdrawals = adminWithdrawals,
                                campaigns = adminCampaigns,
                                commissions = adminCommissions,
                                businessAccount = businessAccount,
                                businessLedger = businessLedger,
                                isBusy = isBusy,
                                onReviewSubmission = viewModel::reviewSubmission,
                                onReviewWithdrawal = viewModel::reviewWithdrawal,
                                onModerateCampaign = viewModel::moderateCampaign,
                                onPublishStarterTasks = viewModel::publishStarterTasks,
                                onConfigureBusinessSettlement = viewModel::configureBusinessSettlementProvider
                            )
                        } else {
                            viewModel.selectTab(MainTab.MARKETPLACE)
                        }
                    }
                }
            }
        }

        if (isDesktopDrawer) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(modifier = Modifier.width(268.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .statusBarsPadding()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                RazakBrandLogo(modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp))
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                destinations.forEach { dest ->
                                    val selected = selectedTab == dest.tab
                                    NavigationDrawerItem(
                                        selected = selected,
                                        onClick = { viewModel.selectTab(dest.tab) },
                                        icon = {
                                            NavIconWithBadge(dest, selected)
                                        },
                                        label = { Text(dest.label) },
                                        modifier = Modifier.testTag("nav_${dest.tab.name.lowercase()}")
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(
                                    onClick = { viewModel.selectTab(MainTab.WALLET) },
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                        Text(strings.walletBalance, style = MaterialTheme.typography.labelSmall)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = CurrencyFormatter.formatTzs(user.walletBalanceTzs),
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { showProfileDialog = true },
                                        modifier = Modifier.weight(1f).testTag("profile_button")
                                    ) {
                                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(user.fullName, maxLines = 1)
                                    }
                                    IconButton(
                                        onClick = { showLanguageDialog = true },
                                        modifier = Modifier.testTag("drawer_language_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Language,
                                            contentDescription = strings.languageButtonLabel
                                        )
                                    }
                                    IconButton(onClick = onToggleTheme) {
                                        Icon(
                                            imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                            contentDescription = strings.toggleThemeDesc
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        RazakTopBar(
                            user = user,
                            isDarkTheme = isDarkTheme,
                            showBrandLogo = false,
                            screenTitle = destinations.firstOrNull { it.tab == selectedTab }?.label ?: strings.navMarket,
                            onToggleTheme = onToggleTheme,
                            onOpenLanguage = { showLanguageDialog = true },
                            onOpenWallet = { viewModel.selectTab(MainTab.WALLET) },
                            onOpenProfile = { showProfileDialog = true }
                        )
                    }
                ) { innerPadding ->
                    mainContent(Modifier.padding(innerPadding))
                }
            }
        } else {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    RazakTopBar(
                        user = user,
                        isDarkTheme = isDarkTheme,
                        showBrandLogo = true,
                        screenTitle = null,
                        onToggleTheme = onToggleTheme,
                        onOpenLanguage = { showLanguageDialog = true },
                        onOpenWallet = { viewModel.selectTab(MainTab.WALLET) },
                        onOpenProfile = { showProfileDialog = true }
                    )
                },
                bottomBar = {
                    if (!isTabletRail) {
                        NavigationBar {
                            destinations.forEach { dest ->
                                val selected = selectedTab == dest.tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(dest.tab) },
                                    icon = { NavIconWithBadge(dest, selected) },
                                    label = { Text(dest.label, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_${dest.tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isTabletRail) {
                        NavigationRail(
                            modifier = Modifier
                                .fillMaxHeight()
                                .navigationBarsPadding()
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            destinations.forEach { dest ->
                                val selected = selectedTab == dest.tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(dest.tab) },
                                    icon = { NavIconWithBadge(dest, selected) },
                                    label = { Text(dest.label, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_${dest.tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                    mainContent(Modifier.weight(1f))
                }
            }
        }
    }

    if (showProfileDialog) {
        ProfileAccountDialog(
            user = user,
            currentLanguage = currentLanguage,
            isBusy = isBusy,
            onSelectLanguage = onSelectLanguage,
            onSaveProfile = { name, username, phone ->
                viewModel.updateProfile(name, username, phone) {
                    showProfileDialog = false
                }
            },
            onSignOut = {
                showProfileDialog = false
                onSignOut()
            },
            onDismiss = { showProfileDialog = false }
        )
    }

    if (showLanguageDialog) {
        LanguagePickerDialog(
            currentLanguage = currentLanguage,
            onSelectLanguage = onSelectLanguage,
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
private fun ProfileAccountDialog(
    user: UserEntity,
    currentLanguage: AppLanguage,
    isBusy: Boolean,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSaveProfile: (String, String, String) -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var fullName by rememberSaveable(user.userId) { mutableStateOf(user.fullName) }
    var username by rememberSaveable(user.userId) { mutableStateOf(user.username) }
    var phoneNumber by rememberSaveable(user.userId) { mutableStateOf(user.phoneNumber) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.accountAndProfileTitle) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${user.email} • ${strings.roleLabel(user.resolvedRole())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = strings.crossPlatformSyncNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${strings.walletBalance}: ${CurrencyFormatter.formatTzs(user.walletBalanceTzs)}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider()
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it.take(80) },
                    label = { Text(strings.fullNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_fullname_input")
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.take(40) },
                    label = { Text(strings.usernameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_username_input")
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it.take(30) },
                    label = { Text(strings.mobileMoneyNumberLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_phone_input")
                )
                HorizontalDivider()
                LanguageSettingsSection(
                    currentLanguage = currentLanguage,
                    onSelectLanguage = onSelectLanguage
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveProfile(fullName, username, phoneNumber) },
                enabled = !isBusy,
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text(strings.saveProfile)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onSignOut,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.signOut)
                }
                TextButton(onClick = onDismiss) {
                    Text(strings.close)
                }
            }
        }
    )
}

@Composable
private fun NavIconWithBadge(dest: NavDestination, selected: Boolean) {
    val icon = if (selected) dest.selectedIcon else dest.unselectedIcon
    if (dest.badgeCount > 0) {
        BadgedBox(badge = { Badge { Text("${dest.badgeCount}") } }) {
            Icon(icon, contentDescription = dest.label)
        }
    } else {
        Icon(icon, contentDescription = dest.label)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RazakTopBar(
    user: UserEntity,
    isDarkTheme: Boolean,
    showBrandLogo: Boolean,
    screenTitle: String?,
    onToggleTheme: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val strings = LocalAppStrings.current
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        title = {
            if (showBrandLogo) {
                RazakBrandLogo(compact = true)
            } else {
                Text(screenTitle ?: strings.appName, style = MaterialTheme.typography.titleLarge)
            }
        },
        actions = {
            Surface(
                onClick = onOpenWallet,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(end = 4.dp).testTag("topbar_wallet_chip")
            ) {
                Text(
                    text = CurrencyFormatter.formatTzs(user.walletBalanceTzs),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            IconButton(onClick = onOpenLanguage, modifier = Modifier.testTag("topbar_language_button")) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = strings.languageButtonLabel
                )
            }
            IconButton(onClick = onToggleTheme, modifier = Modifier.testTag("theme_toggle_button")) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = strings.toggleThemeDesc
                )
            }
            IconButton(onClick = onOpenProfile, modifier = Modifier.testTag("topbar_profile_button")) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = strings.accountProfileDesc
                )
            }
        }
    )
}
