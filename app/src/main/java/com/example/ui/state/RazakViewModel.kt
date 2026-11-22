package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.BusinessAccountEntity
import com.example.data.model.BusinessLedgerEntryEntity
import com.example.data.remote.BusinessSettlementProvider
import com.example.data.model.PayoutProvider
import com.example.data.model.PlatformCommissionEntity
import com.example.data.model.SocialPlatform
import com.example.data.model.SocialTaskEntity
import com.example.data.model.TaskActionType
import com.example.data.model.TaskCampaignStatus
import com.example.data.model.TaskSubmissionEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.WalletTransactionEntity
import com.example.data.model.WithdrawalRequestEntity
import com.example.data.repository.RazakRepository
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.i18n.RazakTranslations
import com.example.i18n.resolveLocalizedMessage
import com.example.util.CurrencyFormatter
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
    MARKETPLACE,
    PROMOTE,
    DASHBOARD,
    WALLET,
    ADMIN
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class RazakViewModel(
    private val repository: RazakRepository,
    private val currentUserId: String
) : ViewModel() {

    val darkThemeOverride: StateFlow<Boolean?> = repository.darkThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val appLanguage: StateFlow<AppLanguage> = repository.languageCodeFlow
        .map { AppLanguage.fromCode(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.DEFAULT)

    private fun currentStrings(): AppStrings = RazakTranslations.forLanguage(appLanguage.value)

    private val _selectedTab = MutableStateFlow(MainTab.MARKETPLACE)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _selectedPlatform = MutableStateFlow<SocialPlatform?>(null)
    val selectedPlatform: StateFlow<SocialPlatform?> = _selectedPlatform.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTask = MutableStateFlow<SocialTaskEntity?>(null)
    val selectedTask: StateFlow<SocialTaskEntity?> = _selectedTask.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    init {
        val firebaseUser = Firebase.auth.currentUser
        if (firebaseUser != null && firebaseUser.uid == currentUserId) {
            viewModelScope.launch {
                repository.ensureUserProfile(firebaseUser).onFailure { err ->
                    _events.tryEmit(err.resolveLocalizedMessage(currentStrings()))
                }
            }
        }
    }

    val userState: StateFlow<UiState<UserEntity>> = repository.observeUser(currentUserId)
        .map<UserEntity?, UiState<UserEntity>> { user ->
            if (user == null) UiState.Loading else UiState.Success(user)
        }
        .catchFirestoreError("Failed to load profile")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val marketplaceTasks: StateFlow<List<SocialTaskEntity>> = combine(
        repository.observeMarketplaceTasks(currentUserId)
            .catch { error ->
                unwrapAndLog(error, "marketplace tasks")
                emit(emptyList())
            },
        _selectedPlatform,
        _searchQuery
    ) { tasks, platform, query ->
        tasks.filter { task ->
            val matchesPlatform = platform == null || task.resolvedPlatform() == platform
            val q = query.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                task.title.lowercase().contains(q) ||
                task.accountHandle.lowercase().contains(q) ||
                task.description.lowercase().contains(q)
            matchesPlatform && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userCampaigns: StateFlow<List<SocialTaskEntity>> = repository.observeUserCampaigns(currentUserId)
        .catch { error -> unwrapAndLog(error, "user campaigns"); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSubmissions: StateFlow<List<TaskSubmissionEntity>> = repository.observeUserSubmissions(currentUserId)
        .catch { error -> unwrapAndLog(error, "user submissions"); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userTransactions: StateFlow<List<WalletTransactionEntity>> = repository.observeUserTransactions(currentUserId)
        .catch { error -> unwrapAndLog(error, "wallet transactions"); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userWithdrawals: StateFlow<List<WithdrawalRequestEntity>> = repository.observeUserWithdrawals(currentUserId)
        .catch { error -> unwrapAndLog(error, "user withdrawals"); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun isCurrentAdmin(user: UserEntity?): Boolean {
        val authUser = Firebase.auth.currentUser
        val bootstrapped = authUser != null &&
            authUser.isEmailVerified &&
            authUser.email.equals(RazakRepository.ADMIN_EMAIL, ignoreCase = true)
        return bootstrapped || user?.resolvedRole() == UserRole.ADMIN
    }

    val adminSubmissions: StateFlow<List<TaskSubmissionEntity>> = userState
        .flatMapLatest { state ->
            val user = (state as? UiState.Success)?.data
            if (isCurrentAdmin(user)) {
                repository.observeAllSubmissions(currentUserId)
                    .catch { error -> unwrapAndLog(error, "admin submissions"); emit(emptyList()) }
            } else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminWithdrawals: StateFlow<List<WithdrawalRequestEntity>> = userState
        .flatMapLatest { state ->
            val user = (state as? UiState.Success)?.data
            if (isCurrentAdmin(user)) {
                repository.observeAllWithdrawals(currentUserId)
                    .catch { error -> unwrapAndLog(error, "admin withdrawals"); emit(emptyList()) }
            } else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminCampaigns: StateFlow<List<SocialTaskEntity>> = userState
        .flatMapLatest { state ->
            val user = (state as? UiState.Success)?.data
            if (isCurrentAdmin(user)) {
                repository.observeAllCampaigns(currentUserId)
                    .catch { error -> unwrapAndLog(error, "admin campaigns"); emit(emptyList()) }
            } else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminUsers: StateFlow<List<UserEntity>> = userState
        .flatMapLatest { state ->
            val user = (state as? UiState.Success)?.data
            if (isCurrentAdmin(user)) {
                repository.observeAllUsers(currentUserId)
                    .catch { error -> unwrapAndLog(error, "admin users"); emit(emptyList()) }
            } else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminCommissions: StateFlow<List<PlatformCommissionEntity>> = userState
        .flatMapLatest { state ->
            val user = (state as? UiState.Success)?.data
            if (isCurrentAdmin(user)) {
                repository.observeAllCommissions(currentUserId)
                    .catch { error -> unwrapAndLog(error, "admin commissions"); emit(emptyList()) }
            } else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val businessAccount: StateFlow<BusinessAccountEntity?> = userState
        .flatMapLatest { state ->
            val user = (state as? UiState.Success)?.data
            if (isCurrentAdmin(user)) {
                repository.observeBusinessAccount(currentUserId)
                    .catch { error -> unwrapAndLog(error, "business account"); emit(null) }
            } else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val businessLedger: StateFlow<List<BusinessLedgerEntryEntity>> = userState
        .flatMapLatest { state ->
            val user = (state as? UiState.Success)?.data
            if (isCurrentAdmin(user)) {
                repository.observeBusinessLedger(currentUserId)
                    .catch { error -> unwrapAndLog(error, "business ledger"); emit(emptyList()) }
            } else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun selectPlatformFilter(platform: SocialPlatform?) {
        _selectedPlatform.value = platform
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectTaskForDetail(task: SocialTaskEntity?) {
        _selectedTask.value = task
    }

    fun toggleDarkTheme(currentDark: Boolean) {
        viewModelScope.launch {
            repository.setDarkTheme(!currentDark)
        }
    }

    fun changeLanguage(language: AppLanguage) {
        viewModelScope.launch {
            val targetStrings = RazakTranslations.forLanguage(language)
            repository.updatePreferredLanguage(currentUserId, language)
                .onSuccess {
                    _events.tryEmit(targetStrings.languageChangedNoticeFmt.format(language.nativeName))
                }
                .onFailure { err ->
                    _events.tryEmit(err.resolveLocalizedMessage(targetStrings))
                }
        }
    }

    fun retryProfileInitialization() {
        val firebaseUser = Firebase.auth.currentUser ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.ensureUserProfile(firebaseUser)
            _isBusy.value = false
            res.onFailure { err ->
                _events.tryEmit(err.resolveLocalizedMessage(currentStrings()))
            }
        }
    }

    fun updateProfile(fullName: String, username: String, phoneNumber: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateProfile(currentUserId, fullName, username, phoneNumber)
            _isBusy.value = false
            val s = currentStrings()
            res.onSuccess {
                _events.tryEmit(s.msgProfileUpdated)
                onSuccess()
            }.onFailure { err ->
                _events.tryEmit(err.resolveLocalizedMessage(s))
            }
        }
    }

    fun submitTaskProof(
        taskId: String,
        socialHandleUsed: String,
        proofNote: String,
        proofImageUri: String?,
        onSuccess: () -> Unit
    ) {
        val currentUser = (userState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.submitTaskProof(
                workerId = currentUserId,
                workerName = currentUser.fullName,
                taskId = taskId,
                socialHandleUsed = socialHandleUsed,
                proofNote = proofNote,
                proofImageUri = proofImageUri
            )
            _isBusy.value = false
            val s = currentStrings()
            res.onSuccess {
                _selectedTask.value = null
                _events.tryEmit(s.msgProofSubmitted)
                onSuccess()
            }.onFailure { err ->
                _events.tryEmit(err.resolveLocalizedMessage(s))
            }
        }
    }

    fun createPromotionCampaign(
        title: String,
        description: String,
        platform: SocialPlatform,
        actionType: TaskActionType,
        accountHandle: String,
        targetUrl: String,
        rewardTzs: Long,
        totalSlots: Int,
        onSuccess: () -> Unit
    ) {
        val currentUser = (userState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createPromotionCampaign(
                creatorUserId = currentUserId,
                title = title,
                description = description,
                platform = platform,
                actionType = actionType,
                accountHandle = accountHandle,
                targetUrl = targetUrl,
                rewardTzs = rewardTzs,
                totalSlots = totalSlots
            )
            _isBusy.value = false
            val s = currentStrings()
            res.onSuccess {
                val msg = if (isCurrentAdmin(currentUser)) {
                    s.msgCampaignPublishedLive
                } else {
                    s.msgCampaignSubmittedReview
                }
                _events.tryEmit(msg)
                onSuccess()
            }.onFailure { err ->
                _events.tryEmit(err.resolveLocalizedMessage(s))
            }
        }
    }

    fun depositToWallet(
        amountTzs: Long,
        provider: PayoutProvider,
        phoneOrReference: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.depositToWallet(currentUserId, amountTzs, provider, phoneOrReference)
            _isBusy.value = false
            val s = currentStrings()
            res.onSuccess {
                _events.tryEmit(s.msgWalletCreditedFmt.format(CurrencyFormatter.formatTzs(amountTzs)))
                onSuccess()
            }.onFailure { err ->
                _events.tryEmit(err.resolveLocalizedMessage(s))
            }
        }
    }

    fun requestWithdrawal(
        amountTzs: Long,
        provider: PayoutProvider,
        accountNumber: String,
        accountName: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.requestWithdrawal(currentUserId, amountTzs, provider, accountNumber, accountName)
            _isBusy.value = false
            val s = currentStrings()
            res.onSuccess {
                _events.tryEmit(s.msgWithdrawalSubmittedFmt.format(CurrencyFormatter.formatTzs(amountTzs)))
                onSuccess()
            }.onFailure { err ->
                _events.tryEmit(err.resolveLocalizedMessage(s))
            }
        }
    }

    fun reviewSubmission(submissionId: String, approve: Boolean, note: String = "") {
        viewModelScope.launch {
            val res = repository.reviewSubmission(submissionId, approve, note)
            val s = currentStrings()
            res.onSuccess {
                _events.tryEmit(if (approve) s.msgSubmissionApproved else s.msgSubmissionRejected)
            }.onFailure { err ->
                _events.tryEmit(err.resolveLocalizedMessage(s))
            }
        }
    }

    fun reviewWithdrawal(withdrawalId: String, approve: Boolean, note: String = "") {
        viewModelScope.launch {
            val res = repository.reviewWithdrawal(withdrawalId, approve, note)
            val s = currentStrings()
            res.onSuccess {
                _events.tryEmit(if (approve) s.msgWithdrawalApproved else s.msgWithdrawalRejected)
            }.onFailure { err ->
                _events.tryEmit(err.resolveLocalizedMessage(s))
            }
        }
    }

    fun moderateCampaign(taskId: String, newStatus: TaskCampaignStatus) {
        viewModelScope.launch {
            val res = repository.moderateCampaign(taskId, newStatus)
            val s = currentStrings()
            res.onSuccess {
                _events.tryEmit(s.msgCampaignUpdatedFmt.format(s.campaignStatusLabel(newStatus)))
            }.onFailure { err ->
                _events.tryEmit(err.resolveLocalizedMessage(s))
            }
        }
    }

    fun publishStarterTasks() {
        val currentUser = (userState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.publishStarterMarketplaceTasks(currentUser.fullName)
            _isBusy.value = false
            val s = currentStrings()
            res.onSuccess {
                _events.tryEmit(s.msgStarterTasksPublished)
            }.onFailure { err ->
                _events.tryEmit(err.resolveLocalizedMessage(s))
            }
        }
    }

    fun configureBusinessSettlementProvider(provider: BusinessSettlementProvider, merchantRef: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.configureBusinessSettlementProvider(provider, merchantRef)
            _isBusy.value = false
            val s = currentStrings()
            res.onSuccess {
                _events.tryEmit(s.msgSettlementSavedFmt.format(s.settlementProviderLabel(provider)))
            }.onFailure { err ->
                _events.tryEmit(err.resolveLocalizedMessage(s))
            }
        }
    }

    private fun <T> Flow<UiState<T>>.catchFirestoreError(fallbackMessage: String): Flow<UiState<T>> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, fallbackMessage, rootError)
            emit(UiState.Error(rootError.resolveLocalizedMessage(currentStrings())))
        }

    private fun unwrapAndLog(error: Throwable, label: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $label", rootError)
    }

    class Factory(
        private val repository: RazakRepository,
        private val currentUserId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RazakViewModel(repository, currentUserId) as T
        }
    }

    private companion object {
        const val TAG = "RazakVM"
    }
}
