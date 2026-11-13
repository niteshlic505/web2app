package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.data.model.UserPlan
import com.example.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class BillingConnectionState(val label: String) {
    DISCONNECTED("Disconnected"),
    CONNECTING("Connecting to Google Play..."),
    CONNECTED("Google Play Billing Active"),
    UNAVAILABLE("Play Store Billing Unavailable")
}

data class PlayProductListing(
    val plan: UserPlan,
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val billingPeriodLabel: String,
    val isSubscription: Boolean,
    val offerToken: String?,
    val productDetails: ProductDetails
)

sealed class PlayPurchaseFeedback {
    data object Idle : PlayPurchaseFeedback()
    data object Connecting : PlayPurchaseFeedback()
    data class LaunchingPlaySheet(val plan: UserPlan) : PlayPurchaseFeedback()
    data class PendingPayment(val plan: UserPlan, val message: String) : PlayPurchaseFeedback()
    data class PurchaseSuccess(val plan: UserPlan, val orderId: String?) : PlayPurchaseFeedback()
    data class Restored(val plan: UserPlan, val count: Int) : PlayPurchaseFeedback()
    data object NoPurchasesToRestore : PlayPurchaseFeedback()
    data object Cancelled : PlayPurchaseFeedback()
    data class Error(
        val responseCode: Int,
        val codeName: String,
        val message: String,
        val debugMessage: String = ""
    ) : PlayPurchaseFeedback()
}

class PlayBillingManager private constructor(
    context: Context,
    private val repository: AppRepository
) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _connectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _catalog = MutableStateFlow<Map<UserPlan, PlayProductListing>>(emptyMap())
    val catalog: StateFlow<Map<UserPlan, PlayProductListing>> = _catalog.asStateFlow()

    private val _feedback = MutableStateFlow<PlayPurchaseFeedback>(PlayPurchaseFeedback.Idle)
    val feedback: StateFlow<PlayPurchaseFeedback> = _feedback.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private var activeSubsToken: String? = null
    private var activeSubsProductId: String? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    fun startConnection(onConnected: (() -> Unit)? = null) {
        if (billingClient.isReady) {
            _connectionState.value = BillingConnectionState.CONNECTED
            onConnected?.invoke()
            return
        }
        _connectionState.value = BillingConnectionState.CONNECTING
        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    scope.launch {
                        if (billingResult.responseCode == BillingResponseCode.OK) {
                            Log.i(TAG, "Google Play Billing connected.")
                            _connectionState.value = BillingConnectionState.CONNECTED
                            queryAllProducts()
                            syncExistingPurchases(userInitiated = false)
                            onConnected?.invoke()
                        } else {
                            val codeName = responseCodeName(billingResult.responseCode)
                            Log.w(TAG, "Play Billing setup failed: $codeName (${billingResult.debugMessage})")
                            _isBusy.value = false
                            _connectionState.value = BillingConnectionState.UNAVAILABLE
                            _feedback.value = PlayPurchaseFeedback.Error(
                                responseCode = billingResult.responseCode,
                                codeName = codeName,
                                message = humanReadableError(billingResult.responseCode, billingResult.debugMessage),
                                debugMessage = billingResult.debugMessage
                            )
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.w(TAG, "Google Play Billing service disconnected.")
                    scope.launch {
                        _isBusy.value = false
                        _connectionState.value = BillingConnectionState.DISCONNECTED
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Play Billing connection", e)
            _isBusy.value = false
            _connectionState.value = BillingConnectionState.UNAVAILABLE
            _feedback.value = PlayPurchaseFeedback.Error(
                responseCode = BillingResponseCode.BILLING_UNAVAILABLE,
                codeName = "BILLING_UNAVAILABLE",
                message = "Google Play Services is not available on this device.",
                debugMessage = e.localizedMessage ?: ""
            )
        }
    }

    fun queryAllProducts() {
        if (!billingClient.isReady) {
            startConnection()
            return
        }
        val paidPlans = UserPlan.entries.filter { it.playProductId != null }
        val subsProducts = paidPlans.mapNotNull { plan ->
            plan.playProductId?.let { id ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(ProductType.SUBS)
                    .build()
            }
        }
        val inAppProducts = paidPlans.mapNotNull { plan ->
            plan.playProductId?.let { id ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(ProductType.INAPP)
                    .build()
            }
        }

        if (subsProducts.isNotEmpty()) {
            val subsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(subsProducts)
                .build()
            billingClient.queryProductDetailsAsync(subsParams) { billingResult, queryProductDetailsResult ->
                scope.launch {
                    if (billingResult.responseCode == BillingResponseCode.OK) {
                        val list = queryProductDetailsResult.productDetailsList
                        mergeProductDetails(list, isSubscription = true)
                    } else {
                        Log.w(TAG, "SUBS catalog query warning: ${responseCodeName(billingResult.responseCode)}")
                    }
                    // Also check INAPP for plans not configured as subscriptions
                    if (inAppProducts.isNotEmpty()) {
                        val inAppParams = QueryProductDetailsParams.newBuilder()
                            .setProductList(inAppProducts)
                            .build()
                        billingClient.queryProductDetailsAsync(inAppParams) { inAppResult, inAppDetailsResult ->
                            scope.launch {
                                if (inAppResult.responseCode == BillingResponseCode.OK) {
                                    mergeProductDetails(inAppDetailsResult.productDetailsList, isSubscription = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun mergeProductDetails(detailsList: List<ProductDetails>, isSubscription: Boolean) {
        if (detailsList.isEmpty()) return
        val updated = _catalog.value.toMutableMap()
        for (details in detailsList) {
            val plan = UserPlan.fromPlayProductId(details.productId) ?: continue
            if (isSubscription) {
                val offer = details.subscriptionOfferDetails?.firstOrNull() ?: continue
                val firstPhase = offer.pricingPhases.pricingPhaseList.firstOrNull()
                updated[plan] = PlayProductListing(
                    plan = plan,
                    productId = details.productId,
                    title = details.name.ifBlank { plan.title },
                    description = details.description,
                    formattedPrice = firstPhase?.formattedPrice ?: "₹${plan.priceInr}",
                    billingPeriodLabel = formatBillingPeriod(firstPhase?.billingPeriod),
                    isSubscription = true,
                    offerToken = offer.offerToken,
                    productDetails = details
                )
            } else if (!updated.containsKey(plan)) {
                val oneTimeOffer = details.oneTimePurchaseOfferDetails
                updated[plan] = PlayProductListing(
                    plan = plan,
                    productId = details.productId,
                    title = details.name.ifBlank { plan.title },
                    description = details.description,
                    formattedPrice = oneTimeOffer?.formattedPrice ?: "₹${plan.priceInr}",
                    billingPeriodLabel = "One-time Pass",
                    isSubscription = false,
                    offerToken = oneTimeOffer?.offerToken,
                    productDetails = details
                )
            }
        }
        _catalog.value = updated
    }

    fun launchPurchaseFlow(activity: Activity, targetPlan: UserPlan) {
        val productId = targetPlan.playProductId
        if (productId == null) {
            repository.updatePlan(UserPlan.FREE)
            return
        }

        if (!billingClient.isReady) {
            _isBusy.value = true
            _feedback.value = PlayPurchaseFeedback.Connecting
            startConnection {
                if (_catalog.value.containsKey(targetPlan)) {
                    _isBusy.value = false
                    launchPurchaseFlowInternal(activity, targetPlan)
                } else {
                    queryProductAndLaunch(activity, targetPlan, productId)
                }
            }
            return
        }

        val listing = _catalog.value[targetPlan]
        if (listing == null) {
            _isBusy.value = true
            queryProductAndLaunch(activity, targetPlan, productId)
        } else {
            launchPurchaseFlowInternal(activity, targetPlan)
        }
    }

    private fun queryProductAndLaunch(activity: Activity, targetPlan: UserPlan, productId: String) {
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(subsParams) { subsResult, subsDetailsResult ->
            scope.launch {
                if (subsResult.responseCode == BillingResponseCode.OK && subsDetailsResult.productDetailsList.isNotEmpty()) {
                    mergeProductDetails(subsDetailsResult.productDetailsList, isSubscription = true)
                    _isBusy.value = false
                    launchPurchaseFlowInternal(activity, targetPlan)
                } else {
                    val inAppParams = QueryProductDetailsParams.newBuilder()
                        .setProductList(
                            listOf(
                                QueryProductDetailsParams.Product.newBuilder()
                                    .setProductId(productId)
                                    .setProductType(ProductType.INAPP)
                                    .build()
                            )
                        )
                        .build()
                    billingClient.queryProductDetailsAsync(inAppParams) { inAppResult, inAppDetailsResult ->
                        scope.launch {
                            _isBusy.value = false
                            if (inAppResult.responseCode == BillingResponseCode.OK && inAppDetailsResult.productDetailsList.isNotEmpty()) {
                                mergeProductDetails(inAppDetailsResult.productDetailsList, isSubscription = false)
                                launchPurchaseFlowInternal(activity, targetPlan)
                            } else {
                                val effectiveResult = if (subsResult.responseCode != BillingResponseCode.OK) subsResult else inAppResult
                                if (effectiveResult.responseCode != BillingResponseCode.OK) {
                                    _feedback.value = PlayPurchaseFeedback.Error(
                                        responseCode = effectiveResult.responseCode,
                                        codeName = responseCodeName(effectiveResult.responseCode),
                                        message = humanReadableError(effectiveResult.responseCode, effectiveResult.debugMessage),
                                        debugMessage = effectiveResult.debugMessage
                                    )
                                } else {
                                    reportMissingCatalogItem(targetPlan, productId)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun launchPurchaseFlowInternal(activity: Activity, targetPlan: UserPlan) {
        val listing = _catalog.value[targetPlan] ?: run {
            reportMissingCatalogItem(targetPlan, targetPlan.playProductId ?: "")
            return
        }

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(listing.productDetails)

        if (!listing.offerToken.isNullOrBlank()) {
            productParamsBuilder.setOfferToken(listing.offerToken)
        }

        val flowParamsBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))

        val existingSubsToken = activeSubsToken
        val existingProductId = activeSubsProductId
        if (listing.isSubscription &&
            !existingSubsToken.isNullOrBlank() &&
            existingProductId != null &&
            existingProductId != listing.productId
        ) {
            val updateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                .setOldPurchaseToken(existingSubsToken)
                .setSubscriptionReplacementMode(
                    BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION
                )
                .build()
            flowParamsBuilder.setSubscriptionUpdateParams(updateParams)
        }

        _feedback.value = PlayPurchaseFeedback.LaunchingPlaySheet(targetPlan)
        val launchResult = billingClient.launchBillingFlow(activity, flowParamsBuilder.build())
        if (launchResult.responseCode != BillingResponseCode.OK) {
            _feedback.value = PlayPurchaseFeedback.Error(
                responseCode = launchResult.responseCode,
                codeName = responseCodeName(launchResult.responseCode),
                message = humanReadableError(launchResult.responseCode, launchResult.debugMessage),
                debugMessage = launchResult.debugMessage
            )
        }
    }

    private fun reportMissingCatalogItem(plan: UserPlan, productId: String) {
        _feedback.value = PlayPurchaseFeedback.Error(
            responseCode = BillingResponseCode.ITEM_UNAVAILABLE,
            codeName = "ITEM_UNAVAILABLE",
            message = "Product '$productId' (${plan.title}) is not published yet on Google Play Console for ${appContext.packageName}. Upload an AAB to Play Console Internal/Closed Testing and activate SKU '$productId' to complete live Play checkout.",
            debugMessage = "Missing Play Console SKU: $productId"
        )
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        scope.launch {
            _isBusy.value = false
            when (billingResult.responseCode) {
                BillingResponseCode.OK -> {
                    if (!purchases.isNullOrEmpty()) {
                        handlePurchasesList(purchases, isRestoreFlow = false)
                    }
                }
                BillingResponseCode.USER_CANCELED -> {
                    _feedback.value = PlayPurchaseFeedback.Cancelled
                }
                BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    syncExistingPurchases(userInitiated = true)
                }
                else -> {
                    _feedback.value = PlayPurchaseFeedback.Error(
                        responseCode = billingResult.responseCode,
                        codeName = responseCodeName(billingResult.responseCode),
                        message = humanReadableError(billingResult.responseCode, billingResult.debugMessage),
                        debugMessage = billingResult.debugMessage
                    )
                }
            }
        }
    }

    fun restorePurchases() {
        if (!billingClient.isReady) {
            _isBusy.value = true
            _feedback.value = PlayPurchaseFeedback.Connecting
            startConnection {
                syncExistingPurchases(userInitiated = true)
            }
            return
        }
        syncExistingPurchases(userInitiated = true)
    }

    private fun syncExistingPurchases(userInitiated: Boolean) {
        if (!billingClient.isReady) return
        if (userInitiated) _isBusy.value = true

        val collected = mutableListOf<Purchase>()
        val subsQuery = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(subsQuery) { subsResult, subsPurchases ->
            if (subsResult.responseCode == BillingResponseCode.OK) {
                collected.addAll(subsPurchases)
            }
            val inAppQuery = QueryPurchasesParams.newBuilder()
                .setProductType(ProductType.INAPP)
                .build()
            billingClient.queryPurchasesAsync(inAppQuery) { inAppResult, inAppPurchases ->
                scope.launch {
                    if (inAppResult.responseCode == BillingResponseCode.OK) {
                        collected.addAll(inAppPurchases)
                    }
                    _isBusy.value = false
                    if (collected.isEmpty()) {
                        if (subsResult.responseCode == BillingResponseCode.OK &&
                            inAppResult.responseCode == BillingResponseCode.OK
                        ) {
                            repository.revertToFreeIfPlayEntitlementExpired()
                            if (userInitiated) {
                                _feedback.value = PlayPurchaseFeedback.NoPurchasesToRestore
                            }
                        } else if (userInitiated) {
                            _feedback.value = PlayPurchaseFeedback.Error(
                                responseCode = subsResult.responseCode,
                                codeName = responseCodeName(subsResult.responseCode),
                                message = humanReadableError(subsResult.responseCode, subsResult.debugMessage),
                                debugMessage = subsResult.debugMessage
                            )
                        }
                    } else {
                        handlePurchasesList(collected, isRestoreFlow = userInitiated)
                    }
                }
            }
        }
    }

    private fun handlePurchasesList(purchases: List<Purchase>, isRestoreFlow: Boolean) {
        var highestPlan: UserPlan? = null
        var highestPurchase: Purchase? = null
        var matchedProductId = ""

        for (purchase in purchases) {
            val matchedPlans = purchase.products.mapNotNull { pid ->
                UserPlan.fromPlayProductId(pid)?.let { plan -> pid to plan }
            }
            val bestPair = matchedPlans.maxByOrNull { it.second.ordinal } ?: continue
            val (productId, plan) = bestPair

            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    if (!purchase.isAcknowledged) {
                        val ackParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient.acknowledgePurchase(ackParams) { ackResult ->
                            Log.i(TAG, "Acknowledge purchase (${productId}): ${responseCodeName(ackResult.responseCode)}")
                        }
                    }
                    activeSubsToken = purchase.purchaseToken
                    activeSubsProductId = productId

                    if (highestPlan == null || plan.ordinal > highestPlan.ordinal) {
                        highestPlan = plan
                        highestPurchase = purchase
                        matchedProductId = productId
                    }
                }
                Purchase.PurchaseState.PENDING -> {
                    _feedback.value = PlayPurchaseFeedback.PendingPayment(
                        plan = plan,
                        message = "Payment via Google Play is pending confirmation (e.g., UPI/Cash). ${plan.title} activates automatically once Google Play confirms payment."
                    )
                }
                else -> Unit
            }
        }

        if (highestPlan != null && highestPurchase != null) {
            repository.recordPlayPurchase(
                plan = highestPlan,
                productId = matchedProductId,
                orderId = highestPurchase.orderId,
                purchaseToken = highestPurchase.purchaseToken,
                purchaseTimeMs = highestPurchase.purchaseTime
            )
            _feedback.value = if (isRestoreFlow) {
                PlayPurchaseFeedback.Restored(highestPlan, purchases.size)
            } else {
                PlayPurchaseFeedback.PurchaseSuccess(highestPlan, highestPurchase.orderId)
            }
        }
    }

    fun clearFeedback() {
        _feedback.value = PlayPurchaseFeedback.Idle
    }

    companion object {
        private const val TAG = "PlayBillingManager"

        @Volatile
        private var instance: PlayBillingManager? = null

        fun getInstance(context: Context, repository: AppRepository): PlayBillingManager {
            return instance ?: synchronized(this) {
                instance ?: PlayBillingManager(context, repository).also { instance = it }
            }
        }

        fun formatBillingPeriod(isoPeriod: String?): String {
            return when (isoPeriod) {
                "P1W" -> "Weekly"
                "P1M" -> "Monthly"
                "P3M" -> "Quarterly"
                "P6M" -> "6 Months"
                "P1Y" -> "Annual"
                null -> "Monthly"
                else -> isoPeriod
            }
        }

        fun responseCodeName(code: Int): String = when (code) {
            BillingResponseCode.OK -> "OK"
            BillingResponseCode.USER_CANCELED -> "USER_CANCELED"
            BillingResponseCode.SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE"
            BillingResponseCode.BILLING_UNAVAILABLE -> "BILLING_UNAVAILABLE"
            BillingResponseCode.ITEM_UNAVAILABLE -> "ITEM_UNAVAILABLE"
            BillingResponseCode.DEVELOPER_ERROR -> "DEVELOPER_ERROR"
            BillingResponseCode.ERROR -> "ERROR"
            BillingResponseCode.ITEM_ALREADY_OWNED -> "ITEM_ALREADY_OWNED"
            BillingResponseCode.ITEM_NOT_OWNED -> "ITEM_NOT_OWNED"
            BillingResponseCode.SERVICE_DISCONNECTED -> "SERVICE_DISCONNECTED"
            BillingResponseCode.FEATURE_NOT_SUPPORTED -> "FEATURE_NOT_SUPPORTED"
            BillingResponseCode.NETWORK_ERROR -> "NETWORK_ERROR"
            else -> "CODE_$code"
        }

        fun humanReadableError(code: Int, debugMessage: String): String = when (code) {
            BillingResponseCode.BILLING_UNAVAILABLE ->
                "Google Play Billing is unavailable on this device or Play Store account. Ensure the Play Store app is updated and signed in."
            BillingResponseCode.SERVICE_UNAVAILABLE, BillingResponseCode.NETWORK_ERROR ->
                "Network or Google Play Store service temporarily unavailable. Please check connectivity and try again."
            BillingResponseCode.ITEM_UNAVAILABLE ->
                "Subscription product is not yet active on Google Play Console for this package/track."
            BillingResponseCode.FEATURE_NOT_SUPPORTED ->
                "This Google Play Store version does not support subscriptions. Please update Google Play Store."
            BillingResponseCode.DEVELOPER_ERROR ->
                "Play Billing configuration mismatch (${debugMessage.ifBlank { "check Play Console package & signing" }})."
            else ->
                debugMessage.ifBlank { "Google Play Billing returned response code $code." }
        }
    }
}
