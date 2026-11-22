package com.example.data.remote

import com.example.data.model.PayoutProvider
import com.example.i18n.LocalizedAppException

data class MobileMoneyDisbursementRequest(
    val withdrawalId: String,
    val userId: String,
    val provider: PayoutProvider,
    val normalizedMsisdn: String,
    val recipientName: String,
    val amountTzs: Long,
    val currency: String = "TZS"
)

sealed interface DisbursementDispatchResult {
    data class AwaitingManualAdminReview(
        val providerCode: String,
        val normalizedMsisdn: String,
        val message: String
    ) : DisbursementDispatchResult

    data class DispatchedToProvider(
        val providerReference: String
    ) : DisbursementDispatchResult
}

/**
 * Abstraction for mobile money payout provider integration (M-Pesa, Airtel Money, Mixx by Yas, HaloPesa).
 * Real B2C payouts must run via a verified server webhook/Cloud Function once merchant API credentials are configured.
 */
interface PaymentProviderGateway {
    fun validateAndNormalizeTzMobileNumber(rawNumber: String): Result<String>
    suspend fun prepareDisbursement(request: MobileMoneyDisbursementRequest): DisbursementDispatchResult
}

class UnconfiguredMobileMoneyGateway : PaymentProviderGateway {

    override fun validateAndNormalizeTzMobileNumber(rawNumber: String): Result<String> {
        val stripped = rawNumber.trim().replace(Regex("[\\s\\-()]"), "")
        val normalized = when {
            stripped.startsWith("+255") && stripped.length == 13 -> "0" + stripped.substring(4)
            stripped.startsWith("255") && stripped.length == 12 -> "0" + stripped.substring(3)
            else -> stripped
        }
        val validPattern = Regex("^0[67][0-9]{8}$")
        return if (validPattern.matches(normalized)) {
            Result.success(normalized)
        } else {
            Result.failure(
                LocalizedAppException(
                    resolver = { it.errInvalidTzPhone },
                    fallbackEnglish = "Enter a valid Tanzanian mobile money number (e.g., 0754123456 or +255754123456)."
                )
            )
        }
    }

    override suspend fun prepareDisbursement(
        request: MobileMoneyDisbursementRequest
    ): DisbursementDispatchResult {
        // Real payment provider disbursement is intentionally NOT simulated on-device.
        // Requests remain PENDING in Firestore until approved by an administrator or fulfilled by a backend payout webhook.
        return DisbursementDispatchResult.AwaitingManualAdminReview(
            providerCode = request.provider.providerCode,
            normalizedMsisdn = request.normalizedMsisdn,
            message = "Queued for admin verification & ${request.provider.displayName} payout."
        )
    }
}

enum class BusinessSettlementProvider(
    val code: String,
    val displayName: String,
    val description: String
) {
    UNCONFIGURED(
        code = "UNCONFIGURED",
        displayName = "Not Connected Yet",
        description = "Select an official Tanzanian business settlement partner when merchant API credentials are provisioned."
    ),
    SELCOM_BUSINESS_TZ(
        code = "SELCOM_BUSINESS_TZ",
        displayName = "Selcom Pay Business TZ",
        description = "Official Tanzanian merchant aggregation & corporate settlement gateway."
    ),
    AZAMPAY_BUSINESS_TZ(
        code = "AZAMPAY_BUSINESS_TZ",
        displayName = "AzamPay Business TZ",
        description = "Regulated Tanzanian corporate collection & settlement rail."
    ),
    CRDB_CORPORATE_TZ(
        code = "CRDB_CORPORATE_TZ",
        displayName = "CRDB Corporate / SimBanking",
        description = "CRDB Bank Tanzania corporate settlement account integration."
    ),
    NMB_BUSINESS_TZ(
        code = "NMB_BUSINESS_TZ",
        displayName = "NMB Business Lipa",
        description = "NMB Bank Tanzania business treasury settlement account integration."
    )
}

data class BusinessSettlementStatus(
    val provider: BusinessSettlementProvider,
    val isLiveGatewayConfigured: Boolean,
    val statusMessage: String
)

/**
 * Contract for future official Tanzanian corporate payment settlement integration.
 * Real bank/aggregator transfers are intentionally NOT simulated in the app.
 */
interface BusinessSettlementGateway {
    fun inspectSettlementReadiness(providerCode: String, merchantRef: String): BusinessSettlementStatus
}

class UnconfiguredTzBusinessGateway : BusinessSettlementGateway {
    override fun inspectSettlementReadiness(
        providerCode: String,
        merchantRef: String
    ): BusinessSettlementStatus {
        val provider = BusinessSettlementProvider.entries.firstOrNull { it.code == providerCode }
            ?: BusinessSettlementProvider.UNCONFIGURED
        val message = when {
            provider == BusinessSettlementProvider.UNCONFIGURED ->
                "Ready for Tanzanian business gateway integration. Choose a provider below to store merchant routing metadata."
            merchantRef.isBlank() ->
                "${provider.displayName} selected. Add your verified merchant/till reference when live webhook keys are configured."
            else ->
                "${provider.displayName} ($merchantRef) linked as target settlement rail. Live transfers execute server-side only."
        }
        return BusinessSettlementStatus(
            provider = provider,
            isLiveGatewayConfigured = false,
            statusMessage = message
        )
    }
}
