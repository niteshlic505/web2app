package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.billing.BillingConnectionState
import com.example.billing.PlayBillingManager
import com.example.billing.PlayPurchaseFeedback
import com.example.data.auth.FirebaseAuthManager
import com.example.data.model.UserPlan
import com.example.data.repository.AppRepository
import com.example.engine.BuildPipelineEngine
import com.example.ui.components.AshokaChakraHUD
import com.example.ui.components.CorporateButton
import com.example.ui.components.DeviceCompatibilityBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.MakeInIndiaBadge
import com.example.ui.components.SecondaryOutlineButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AshokaBlue
import com.example.ui.theme.AshokaCyan
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.IndiaGreen
import com.example.ui.theme.IndiaGreenLight
import com.example.ui.theme.IndianSaffron
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProfilePricingScreen(
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val billingManager = remember(context, repository) { PlayBillingManager.getInstance(context, repository) }

    val hardware = BuildPipelineEngine.detectCurrentDevice(context)
    val currentPlan by repository.currentPlan.collectAsState()
    val entitlement by repository.entitlementMetadata.collectAsState()
    val sessionProfile by FirebaseAuthManager.sessionProfile.collectAsState()

    val billingState by billingManager.connectionState.collectAsState()
    val catalog by billingManager.catalog.collectAsState()
    val billingFeedback by billingManager.feedback.collectAsState()
    val isBillingBusy by billingManager.isBusy.collectAsState()

    var selectedPlanToUpgrade by remember { mutableStateOf<UserPlan?>(null) }

    LaunchedEffect(Unit) {
        billingManager.startConnection()
    }

    LaunchedEffect(billingFeedback) {
        when (val fb = billingFeedback) {
            is PlayPurchaseFeedback.PurchaseSuccess -> {
                selectedPlanToUpgrade = null
                Toast.makeText(
                    context,
                    "Google Play confirmed ${fb.plan.title}!",
                    Toast.LENGTH_LONG
                ).show()
            }
            is PlayPurchaseFeedback.Restored -> {
                Toast.makeText(
                    context,
                    "Restored ${fb.plan.title} from Google Play",
                    Toast.LENGTH_SHORT
                ).show()
            }
            is PlayPurchaseFeedback.NoPurchasesToRestore -> {
                Toast.makeText(
                    context,
                    "No active Google Play purchases found on this account",
                    Toast.LENGTH_SHORT
                ).show()
            }
            is PlayPurchaseFeedback.Cancelled -> {
                Toast.makeText(context, "Google Play checkout cancelled", Toast.LENGTH_SHORT).show()
            }
            else -> Unit
        }
    }

    val planFeatures = mapOf(
        UserPlan.FREE to listOf(
            "5 Active Projects",
            "Smart WebView & PWA Architecture",
            "Debug & Release APK Generation",
            "Tablet & Smartphone Multi-Screen Support",
            "Standard Build Worker Pool"
        ),
        UserPlan.PRO to listOf(
            "25 Active Projects",
            "All 3 Architectures (Smart, PWA, Native Hybrid)",
            "Production Signed APKs (v10.0)",
            "Custom Indian Branding & Vector Icons",
            "Complete Build Logs & Audit Diagnostics"
        ),
        UserPlan.BUSINESS to listOf(
            "100 Active Projects",
            "Signed Release AAB Package Generation",
            "Google Play Publishing API Integration",
            "Priority Cloud Worker Dispatch",
            "Multi-Keystore Release Profiles"
        ),
        UserPlan.AGENCY to listOf(
            "Unlimited Projects",
            "White-Label Studio Branding",
            "Multi-Seat Engineering Collaboration",
            "Dedicated Build Workers",
            "24/7 SLA & GST Invoicing Support"
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Studio Profile Header with Tricolour Top
            GlassCard(
                borderColor = IndianSaffron.copy(alpha = 0.45f),
                backgroundColor = DarkSurface,
                showTricolourTopBar = true
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant)
                                .border(BorderStroke(1.dp, IndianSaffron.copy(alpha = 0.6f)), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AshokaChakraHUD(
                                sizeDp = 32.dp,
                                primaryRingColor = AshokaCyan,
                                accentColor = IndianSaffron,
                                animate = true
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = sessionProfile?.displayName ?: "Bharat Enterprise Studio",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Icon(Icons.Default.Verified, contentDescription = null, tint = IndiaGreen, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = sessionProfile?.email
                                    ?: sessionProfile?.phoneNumber
                                    ?: "admin@website2app.in • Play Billing Ready",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    StatusBadge(text = currentPlan.title, color = IndianSaffron)
                }
            }
        }

        // Google Play Billing Connection & Subscription Control Card
        item {
            val statusColor = when (billingState) {
                BillingConnectionState.CONNECTED -> IndiaGreenLight
                BillingConnectionState.CONNECTING -> AshokaCyan
                BillingConnectionState.DISCONNECTED -> IndianSaffron
                BillingConnectionState.UNAVAILABLE -> CrimsonError
            }
            GlassCard(
                borderColor = statusColor.copy(alpha = 0.45f),
                backgroundColor = DarkSurface
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Shop, contentDescription = null, tint = IndiaGreenLight, modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.play_billing_title),
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "UPI AutoPay • RuPay • Cards • Play Balance",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        StatusBadge(
                            text = when (billingState) {
                                BillingConnectionState.CONNECTED -> "PLAY READY"
                                BillingConnectionState.CONNECTING -> "CONNECTING"
                                BillingConnectionState.DISCONNECTED -> "OFFLINE"
                                BillingConnectionState.UNAVAILABLE -> "UNAVAILABLE"
                            },
                            color = statusColor
                        )
                    }

                    if (!entitlement.orderId.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = IndiaGreenLight, modifier = Modifier.size(16.dp))
                            Column {
                                Text(
                                    text = "Verified Play Order: ${entitlement.orderId}",
                                    color = TextPrimary,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp
                                )
                                if (!entitlement.productId.isNullOrBlank()) {
                                    Text(
                                        text = "SKU: ${entitlement.productId}",
                                        color = TextMuted,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    // Show diagnostic / live feedback from Google Play Billing
                    when (val fb = billingFeedback) {
                        is PlayPurchaseFeedback.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CrimsonError.copy(alpha = 0.12f))
                                    .border(BorderStroke(1.dp, CrimsonError.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonError, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Play Billing Status: ${fb.codeName}",
                                        color = CrimsonError,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = fb.message,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        is PlayPurchaseFeedback.PendingPayment -> {
                            Text(
                                text = fb.message,
                                color = IndianSaffron,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        else -> Unit
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SecondaryOutlineButton(
                            text = if (isBillingBusy) "Syncing..." else stringResource(R.string.play_billing_restore_cta),
                            icon = Icons.Default.Refresh,
                            enabled = !isBillingBusy,
                            onClick = { billingManager.restorePurchases() },
                            borderColor = AshokaCyan,
                            textColor = AshokaCyan,
                            testTag = "restore_play_purchases_button",
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryOutlineButton(
                            text = stringResource(R.string.play_billing_manage_cta),
                            icon = Icons.Default.OpenInNew,
                            onClick = {
                                val pkg = context.packageName
                                val subSku = currentPlan.playProductId
                                val uri = if (subSku != null) {
                                    Uri.parse("https://play.google.com/store/account/subscriptions?sku=$subSku&package=$pkg")
                                } else {
                                    Uri.parse("https://play.google.com/store/account/subscriptions")
                                }
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                }.onFailure {
                                    Toast.makeText(context, "Unable to open Google Play Store", Toast.LENGTH_SHORT).show()
                                }
                            },
                            borderColor = DarkCardBorder,
                            textColor = TextSecondary,
                            testTag = "manage_play_subs_button",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Google Play Checkout Confirmation Panel
        val upgradeTarget = selectedPlanToUpgrade
        if (upgradeTarget != null && upgradeTarget != UserPlan.FREE) {
            val playListing = catalog[upgradeTarget]
            item {
                GlassCard(
                    borderColor = IndianSaffron,
                    backgroundColor = DarkSurface,
                    showTricolourTopBar = true
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = IndiaGreenLight)
                                Text(
                                    text = "Google Play Secure Checkout",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            StatusBadge(
                                text = if (playListing != null) "PLAY CATALOG LIVE" else "GOOGLE PLAY",
                                color = if (playListing != null) IndiaGreenLight else IndianSaffron
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Plan Tier:", color = TextSecondary, fontSize = 12.sp)
                                Text(text = upgradeTarget.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Play Product ID:", color = TextSecondary, fontSize = 12.sp)
                                Text(
                                    text = upgradeTarget.playProductId ?: "-",
                                    color = AshokaCyan,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Billing Cycle:", color = TextSecondary, fontSize = 12.sp)
                                Text(
                                    text = playListing?.billingPeriodLabel ?: "Monthly Subscription",
                                    color = TextPrimary,
                                    fontSize = 12.sp
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (playListing != null) "Google Play Price:" else "Reference Price (INR):",
                                    color = IndianSaffron,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = playListing?.formattedPrice ?: "₹${upgradeTarget.priceInr}/month",
                                    color = IndianSaffron,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        Text(
                            text = "Payments are processed directly by Google Play Store using your linked UPI ID, Debit/Credit Card, NetBanking, or Play Gift Balance. Cancel anytime from Google Play Subscriptions.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CorporateButton(
                                text = if (isBillingBusy) {
                                    "Opening Play..."
                                } else {
                                    "${stringResource(R.string.play_billing_purchase_cta)} (${playListing?.formattedPrice ?: "₹${upgradeTarget.priceInr}"})"
                                },
                                icon = Icons.Default.Shop,
                                enabled = !isBillingBusy && activity != null,
                                onClick = {
                                    if (activity != null) {
                                        billingManager.launchPurchaseFlow(activity, upgradeTarget)
                                    } else {
                                        Toast.makeText(context, "Activity context required for Play Billing", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                testTag = "launch_google_play_billing_button",
                                modifier = Modifier.weight(1f)
                            )
                            SecondaryOutlineButton(
                                text = "Cancel",
                                onClick = {
                                    selectedPlanToUpgrade = null
                                    billingManager.clearFeedback()
                                },
                                modifier = Modifier.weight(0.6f)
                            )
                        }
                    }
                }
            }
        }

        // Hardware Diagnostics Information
        item {
            GlassCard(
                borderColor = DarkCardBorder,
                backgroundColor = DarkSurface
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (hardware.isTablet) Icons.Default.Tablet else Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = if (hardware.isTablet) IndianSaffron else AshokaBlue
                            )
                            Text(
                                text = "Hardware: ${hardware.classification}",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        DeviceCompatibilityBadge(isTablet = hardware.isTablet)
                    }
                    Text(
                        text = "Device Model: ${hardware.manufacturer} ${hardware.model} (${hardware.androidVersion})",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "ABIs: ${hardware.supportedAbis.joinToString(", ")} • Screen: ${hardware.screenWidthDp}x${hardware.screenHeightDp} dp",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Pricing Plans List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Studio Plans via Google Play",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                MakeInIndiaBadge()
            }
        }

        for (plan in UserPlan.entries) {
            val isCurrent = currentPlan == plan
            val playListing = catalog[plan]
            val priceLabel = when {
                plan.priceInr == 0 -> "Free Forever (Make in India)"
                playListing != null -> "${playListing.formattedPrice} / ${playListing.billingPeriodLabel}"
                else -> "₹${plan.priceInr}/month via Google Play"
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (isCurrent) IndianSaffron else DarkCardBorder,
                    backgroundColor = if (isCurrent) DarkSurfaceVariant else DarkSurface
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = plan.title,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = priceLabel,
                                    color = IndianSaffron,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                if (plan.playProductId != null) {
                                    Text(
                                        text = "Play SKU: ${plan.playProductId}",
                                        color = TextMuted,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            if (isCurrent) {
                                StatusBadge(text = "CURRENT PLAN", color = IndiaGreen)
                            } else if (plan != UserPlan.FREE) {
                                SecondaryOutlineButton(
                                    text = "Upgrade",
                                    onClick = {
                                        selectedPlanToUpgrade = plan
                                        billingManager.clearFeedback()
                                    },
                                    borderColor = IndianSaffron,
                                    textColor = IndianSaffron,
                                    testTag = "upgrade_plan_${plan.name.lowercase()}_button"
                                )
                            } else {
                                StatusBadge(text = "INCLUDED", color = TextMuted)
                            }
                        }

                        // Features List
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            planFeatures[plan]?.forEach { feat ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = IndiaGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(text = feat, color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
