package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.PayoutProvider
import com.example.data.model.UserEntity
import com.example.data.model.WalletTransactionEntity
import com.example.data.model.WithdrawalRequestEntity
import com.example.data.model.WithdrawalStatus
import com.example.data.repository.RazakRepository
import com.example.i18n.LocalAppStrings
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.WithdrawalStatusPill
import com.example.ui.theme.AmberGoldBright
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusApprovedFg
import com.example.util.CurrencyFormatter

@Composable
fun WalletScreen(
    currentUser: UserEntity,
    transactions: List<WalletTransactionEntity>,
    withdrawals: List<WithdrawalRequestEntity>,
    isBusy: Boolean,
    onDepositTzs: (Long, PayoutProvider, String, () -> Unit) -> Unit,
    onRequestWithdrawal: (Long, PayoutProvider, String, String, () -> Unit) -> Unit
) {
    val strings = LocalAppStrings.current
    var showDepositDialog by rememberSaveable { mutableStateOf(false) }

    val hasPendingWithdrawal = withdrawals.any { it.resolvedStatus() == WithdrawalStatus.PENDING }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 880.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        WalletHeroCard(
                            user = currentUser,
                            onOpenDeposit = { showDepositDialog = true }
                        )
                    }
                    item {
                        WithdrawalRequestFormCard(
                            currentUser = currentUser,
                            hasPendingWithdrawal = hasPendingWithdrawal,
                            isBusy = isBusy,
                            onRequestWithdrawal = onRequestWithdrawal
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (withdrawals.isNotEmpty()) {
                        item {
                            Text(strings.withdrawalHistoryTitleFmt.format(withdrawals.size), style = MaterialTheme.typography.titleLarge)
                        }
                        items(withdrawals, key = { "w_${it.id}" }) { req ->
                            WithdrawalHistoryCard(req)
                        }
                    }
                    item {
                        Text(strings.txLedgerTitleFmt.format(transactions.size), style = MaterialTheme.typography.titleLarge)
                    }
                    if (transactions.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = strings.noTxTitle,
                                message = strings.noTxMsg,
                                icon = Icons.AutoMirrored.Filled.ReceiptLong
                            )
                        }
                    } else {
                        items(transactions, key = { "t_${it.id}" }) { tx ->
                            TransactionItemCard(tx)
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
                item {
                    WalletHeroCard(
                        user = currentUser,
                        onOpenDeposit = { showDepositDialog = true }
                    )
                }
                item {
                    WithdrawalRequestFormCard(
                        currentUser = currentUser,
                        hasPendingWithdrawal = hasPendingWithdrawal,
                        isBusy = isBusy,
                        onRequestWithdrawal = onRequestWithdrawal
                    )
                }
                if (withdrawals.isNotEmpty()) {
                    item {
                        Text(strings.withdrawalHistoryTitleFmt.format(withdrawals.size), style = MaterialTheme.typography.titleLarge)
                    }
                    items(withdrawals, key = { "w_${it.id}" }) { req ->
                        WithdrawalHistoryCard(req)
                    }
                }
                item {
                    Text(strings.txLedgerTitleFmt.format(transactions.size), style = MaterialTheme.typography.titleLarge)
                }
                if (transactions.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = strings.noTxTitle,
                            message = strings.noTxMsg,
                            icon = Icons.AutoMirrored.Filled.ReceiptLong
                        )
                    }
                } else {
                    items(transactions, key = { "t_${it.id}" }) { tx ->
                        TransactionItemCard(tx)
                    }
                }
            }
        }
    }

    if (showDepositDialog) {
        DepositTzsDialog(
            defaultPhone = currentUser.phoneNumber,
            isBusy = isBusy,
            onDismiss = { showDepositDialog = false },
            onConfirmDeposit = { amount, provider, ref ->
                onDepositTzs(amount, provider, ref) {
                    showDepositDialog = false
                }
            }
        )
    }
}

@Composable
private fun WalletHeroCard(
    user: UserEntity,
    onOpenDeposit: () -> Unit
) {
    val strings = LocalAppStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(EmeraldDeep, EmeraldPrimary)))
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.razakWalletHeader,
                    style = MaterialTheme.typography.labelMedium,
                    color = AmberGoldBright,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = strings.tanzanianShillingsLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = CurrencyFormatter.formatTzs(user.walletBalanceTzs),
                style = MaterialTheme.typography.displaySmall,
                fontFamily = JetBrainsMonoFontFamily,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = strings.lifetimeEarnedAndSpendFmt.format(
                    CurrencyFormatter.formatTzs(user.totalEarnedTzs),
                    CurrencyFormatter.formatTzs(user.totalSpentTzs)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onOpenDeposit,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                modifier = Modifier.testTag("open_deposit_dialog_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.topUpWalletBtn, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WithdrawalRequestFormCard(
    currentUser: UserEntity,
    hasPendingWithdrawal: Boolean,
    isBusy: Boolean,
    onRequestWithdrawal: (Long, PayoutProvider, String, String, () -> Unit) -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedProvider by rememberSaveable { mutableStateOf(PayoutProvider.MPESA_VODACOM) }
    var amountInput by rememberSaveable { mutableStateOf("1000") }
    var accountNumber by rememberSaveable { mutableStateOf(currentUser.phoneNumber) }
    var accountName by rememberSaveable { mutableStateOf(currentUser.fullName) }

    val amountTzs = amountInput.toLongOrNull() ?: 0L
    val minTzs = RazakRepository.MIN_WITHDRAWAL_TZS
    val maxTzs = RazakRepository.MAX_WITHDRAWAL_TZS

    val strippedPhone = accountNumber.trim().replace(Regex("[\\s\\-()]"), "")
    val normalizedPhone = when {
        strippedPhone.startsWith("+255") && strippedPhone.length == 13 -> "0" + strippedPhone.substring(4)
        strippedPhone.startsWith("255") && strippedPhone.length == 12 -> "0" + strippedPhone.substring(3)
        else -> strippedPhone
    }
    val isPhoneValid = Regex("^0[67][0-9]{8}$").matches(normalizedPhone)
    val isNameValid = accountName.trim().length in 2..80

    val amountError: String? = when {
        amountInput.isBlank() || amountTzs < minTzs ->
            strings.errMinWithdrawalFmt.format(CurrencyFormatter.formatTzs(minTzs))
        amountTzs > maxTzs ->
            strings.errMaxWithdrawalFmt.format(CurrencyFormatter.formatTzs(maxTzs))
        amountTzs > currentUser.walletBalanceTzs ->
            strings.errExceedsBalanceFmt.format(CurrencyFormatter.formatTzs(currentUser.walletBalanceTzs))
        else -> null
    }

    val phoneError: String? = if (accountNumber.isNotBlank() && !isPhoneValid) {
        strings.errInvalidTzPhone
    } else null

    val canSubmit = !isBusy && !hasPendingWithdrawal && amountError == null && isPhoneValid && isNameValid

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(strings.requestWithdrawalTitle, style = MaterialTheme.typography.titleLarge)
            }

            Text(
                text = strings.withdrawalLimitsSubFmt.format(
                    CurrencyFormatter.formatTzs(minTzs),
                    CurrencyFormatter.formatTzs(maxTzs)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (hasPendingWithdrawal) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = strings.pendingWithdrawalBanner,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Text(strings.selectMobileMoneyMethod, style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PayoutProvider.withdrawalMethods.forEach { provider ->
                    FilterChip(
                        selected = selectedProvider == provider,
                        onClick = { selectedProvider = provider },
                        label = { Text(provider.displayName) },
                        modifier = Modifier.testTag("payout_provider_${provider.name.lowercase()}")
                    )
                }
            }

            OutlinedTextField(
                value = amountInput,
                onValueChange = { next ->
                    val digits = next.filter(Char::isDigit).take(7)
                    amountInput = digits
                },
                label = { Text(strings.withdrawalAmountLabel) },
                isError = amountError != null && amountInput.isNotEmpty(),
                supportingText = if (amountError != null) {
                    { Text(amountError) }
                } else {
                    { Text(strings.availableColonFmt.format(CurrencyFormatter.formatTzs(currentUser.walletBalanceTzs))) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("withdraw_amount_input")
            )

            OutlinedTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it.take(16) },
                label = { Text(strings.mobileNumberHintLabelFmt.format(selectedProvider.displayName, selectedProvider.hint)) },
                isError = phoneError != null,
                supportingText = phoneError?.let { msg -> { Text(msg) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("withdraw_account_input")
            )

            OutlinedTextField(
                value = accountName,
                onValueChange = { accountName = it.take(80) },
                label = { Text(strings.registeredRecipientNameLabel) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("withdraw_name_input")
            )

            Button(
                onClick = {
                    onRequestWithdrawal(amountTzs, selectedProvider, accountNumber, accountName) {
                        amountInput = "1000"
                    }
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_withdrawal_button")
            ) {
                Text(
                    if (hasPendingWithdrawal) strings.pendingRequestInReviewBtn
                    else strings.withdrawAmountBtnFmt.format(CurrencyFormatter.formatTzs(amountTzs))
                )
            }
        }
    }
}

@Composable
private fun WithdrawalHistoryCard(request: WithdrawalRequestEntity) {
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
                    text = CurrencyFormatter.formatTzs(request.amountTzs),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                WithdrawalStatusPill(status = request.resolvedStatus())
            }
            Text(
                text = "${request.resolvedProvider().displayName} • ${request.accountNumber} (${request.accountName})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = strings.requestedAtFmt.format(CurrencyFormatter.formatRelativeTime(request.requestedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!request.adminNote.isNullOrBlank()) {
                Text(
                    text = strings.adminNoteShortFmt.format(request.adminNote),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TransactionItemCard(tx: WalletTransactionEntity) {
    val strings = LocalAppStrings.current
    val txType = tx.resolvedType()
    val isCredit = txType.isCredit
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCredit) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCredit) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                    contentDescription = strings.transactionTypeLabel(txType),
                    tint = if (isCredit) StatusApprovedFg else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(tx.description, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${strings.transactionTypeLabel(txType)} • ${tx.referenceCode} • ${CurrencyFormatter.formatRelativeTime(tx.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = (if (isCredit) "+" else "-") + CurrencyFormatter.formatTzs(tx.amountTzs),
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                color = if (isCredit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DepositTzsDialog(
    defaultPhone: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirmDeposit: (Long, PayoutProvider, String) -> Unit
) {
    val strings = LocalAppStrings.current
    var amountInput by rememberSaveable { mutableStateOf("10000") }
    var selectedProvider by rememberSaveable { mutableStateOf(PayoutProvider.MPESA_VODACOM) }
    var phoneOrRef by rememberSaveable { mutableStateOf(defaultPhone) }

    val amountTzs = amountInput.toLongOrNull() ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.topUpDialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.topUpDialogBody,
                    style = MaterialTheme.typography.bodySmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        PayoutProvider.MPESA_VODACOM,
                        PayoutProvider.TIGOPESA_MIXX,
                        PayoutProvider.AIRTEL_MONEY,
                        PayoutProvider.HALOPESA
                    ).forEach { provider ->
                        FilterChip(
                            selected = selectedProvider == provider,
                            onClick = { selectedProvider = provider },
                            label = { Text(provider.displayName) }
                        )
                    }
                }
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter(Char::isDigit) },
                    label = { Text(strings.depositAmountLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deposit_amount_input")
                )
                OutlinedTextField(
                    value = phoneOrRef,
                    onValueChange = { phoneOrRef = it },
                    label = { Text(strings.mobileRefLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deposit_phone_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmDeposit(amountTzs, selectedProvider, phoneOrRef) },
                enabled = !isBusy && amountTzs >= 1_000L,
                modifier = Modifier.testTag("confirm_deposit_button")
            ) {
                Text(strings.addAmountBtnFmt.format(CurrencyFormatter.formatTzs(amountTzs)))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
