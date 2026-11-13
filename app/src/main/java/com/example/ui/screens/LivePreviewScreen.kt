package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.StandaloneAppActivity
import com.example.data.model.AppProject
import com.example.data.model.AutomatedAuditReport
import com.example.data.model.NavItem
import com.example.data.repository.AppRepository
import com.example.engine.AutomatedTestingEngine
import com.example.ui.components.CorporateButton
import com.example.ui.components.GlassCard
import com.example.ui.components.ProjectIconBadge
import com.example.ui.components.SecondaryOutlineButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CorporateBlue
import com.example.ui.theme.CorporateCobalt
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray

enum class DevicePreviewModel(val label: String, val screenWidthDp: Int, val isTablet: Boolean) {
    PHONE_PORTRAIT("Phone 6.1\"", 310, false),
    PHONE_LARGE("Phone Max", 345, false),
    TABLET_PORTRAIT("Tablet 8.4\"", 420, true),
    TABLET_LANDSCAPE("Tablet 11\"", 480, true)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LivePreviewScreen(
    projectId: String,
    repository: AppRepository,
    onNavigateBack: () -> Unit,
    onOpenCustomize: (String) -> Unit,
    onOpenBuild: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var project by remember { mutableStateOf<AppProject?>(null) }
    var selectedDevice by remember { mutableStateOf(DevicePreviewModel.PHONE_PORTRAIT) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Live Device, 1: Automated Audit Report

    var isSimulatingOffline by remember { mutableStateOf(false) }
    var showSplashScreenPreview by remember { mutableStateOf(false) }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webLoadingProgress by remember { mutableIntStateOf(0) }
    var currentWebUrl by remember { mutableStateOf("") }
    var auditReport by remember { mutableStateOf<AutomatedAuditReport?>(null) }
    var isRunningAudit by remember { mutableStateOf(false) }

    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(projectId) {
        val proj = repository.getProject(projectId)
        project = proj
        if (proj != null) {
            currentWebUrl = proj.url
            auditReport = AutomatedTestingEngine.runFullAudit(proj)
        }
    }

    val currentProject = project ?: return

    val parsedNavItems = remember(currentProject.navItemsJson) {
        runCatching {
            val json = JSONArray(currentProject.navItemsJson)
            val list = mutableListOf<NavItem>()
            for (i in 0 until json.length()) {
                val item = json.getJSONObject(i)
                list.add(
                    NavItem(
                        title = item.optString("title", "Item"),
                        url = item.optString("url", currentProject.url),
                        iconName = item.optString("iconName", "Home")
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    var selectedNavIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        // Top Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Column {
                    Text(
                        text = currentProject.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentProject.conversionMode.badge,
                        color = CorporateCobalt,
                        fontSize = 11.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SecondaryOutlineButton(
                    text = if (selectedDevice.isTablet) "Run on Tablet" else "Run on Phone",
                    icon = Icons.Default.PlayArrow,
                    onClick = {
                        val intent = StandaloneAppActivity.createIntent(
                            context = context,
                            url = currentProject.url,
                            name = currentProject.name,
                            iconCategory = currentProject.iconEmoji,
                            navJson = currentProject.navItemsJson,
                            splashTitle = currentProject.splashTitle,
                            splashSubtitle = currentProject.splashSubtitle,
                            splashDelayMs = currentProject.splashDelayMs,
                            forceTablet = selectedDevice.isTablet
                        )
                        context.startActivity(intent)
                    },
                    borderColor = CorporateBlue,
                    textColor = CorporateBlue,
                    testTag = "run_standalone_from_preview"
                )
                CorporateButton(
                    text = "Build",
                    icon = Icons.Default.Build,
                    onClick = { onOpenBuild(currentProject.id) },
                    testTag = "preview_to_build"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tabs: Live Device vs Automated Audit
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurface,
            contentColor = CorporateBlue,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CorporateBlue,
                    height = 2.dp
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Live Device Emulator", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Compatibility Audit", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedTab == 0) {
            // Live Device Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device selector pills
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (device in DevicePreviewModel.values()) {
                        val isSelected = selectedDevice == device
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CorporateBlue.copy(alpha = 0.15f) else DarkSurfaceVariant)
                                .border(BorderStroke(1.dp, if (isSelected) CorporateBlue else DarkCardBorder), RoundedCornerShape(6.dp))
                                .clickable { selectedDevice = device }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = device.label,
                                color = if (isSelected) CorporateBlue else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Testing toggles
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSimulatingOffline) Color(0xFFEF4444).copy(alpha = 0.15f) else DarkSurfaceVariant)
                            .clickable { isSimulatingOffline = !isSimulatingOffline }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isSimulatingOffline) "Offline: ON" else "Simulate Offline",
                            color = if (isSimulatingOffline) Color(0xFFEF4444) else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceVariant)
                            .clickable {
                                showSplashScreenPreview = true
                                coroutineScope.launch {
                                    delay(currentProject.splashDelayMs)
                                    showSplashScreenPreview = false
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Test Splash",
                            color = CorporateCobalt,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Phone Bezel Frame with dynamic height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(selectedDevice.screenWidthDp.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF030712))
                        .border(BorderStroke(2.dp, Color(0xFF374151)), RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Status Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .background(Color(0xFF0F172A))
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "09:41", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "5G", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(text = "100%", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Interactive In-Device Address Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .background(Color(0xFF111827))
                                .border(BorderStroke(0.5.dp, DarkCardBorder))
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "HTTPS", tint = EmeraldSuccess, modifier = Modifier.size(13.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = currentWebUrl.ifBlank { currentProject.url },
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { webViewInstance?.reload() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = TextSecondary, modifier = Modifier.size(14.dp))
                            }
                            IconButton(
                                onClick = {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(currentProject.url))
                                        context.startActivity(browserIntent)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in browser", tint = TextSecondary, modifier = Modifier.size(14.dp))
                            }
                        }

                        // Loading progress indicator
                        if (webLoadingProgress in 1..99) {
                            LinearProgressIndicator(
                                progress = { webLoadingProgress / 100f },
                                modifier = Modifier.fillMaxWidth().height(2.dp),
                                color = CorporateBlue,
                                trackColor = Color.Transparent
                            )
                        }

                        // Web View Container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color.White)
                        ) {
                            if (showSplashScreenPreview) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF0F172A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ProjectIconBadge(symbolName = currentProject.iconEmoji, sizeDp = 50.dp)
                                        Text(
                                            text = currentProject.splashTitle.ifBlank { currentProject.name },
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = currentProject.splashSubtitle,
                                            color = CorporateCobalt,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = CorporateBlue,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            } else if (isSimulatingOffline) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF0F172A))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WifiOff,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = "No Connection Available",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Offline resilience active. Reconnect to resume live streaming.",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        SecondaryOutlineButton(
                                            text = "Retry",
                                            onClick = {
                                                isSimulatingOffline = false
                                                webViewInstance?.reload()
                                            },
                                            borderColor = CorporateBlue,
                                            textColor = CorporateBlue
                                        )
                                    }
                                }
                            } else {
                                AndroidView(
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            settings.apply {
                                                javaScriptEnabled = true
                                                domStorageEnabled = true
                                                databaseEnabled = true
                                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                                useWideViewPort = true
                                                loadWithOverviewMode = true
                                                setSupportZoom(true)
                                                builtInZoomControls = true
                                                displayZoomControls = false
                                                cacheMode = WebSettings.LOAD_DEFAULT
                                                allowFileAccess = true
                                                allowContentAccess = true
                                                userAgentString = if (selectedDevice.isTablet) {
                                                    "Mozilla/5.0 (Linux; Android 14; Tablet) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
                                                } else {
                                                    "Mozilla/5.0 (Linux; Android 14; Mobile; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                                                }
                                            }

                                            webViewClient = object : WebViewClient() {
                                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                                    super.onPageStarted(view, url, favicon)
                                                    webLoadingProgress = 25
                                                }

                                                override fun onPageFinished(view: WebView?, url: String?) {
                                                    super.onPageFinished(view, url)
                                                    webLoadingProgress = 100
                                                    url?.let { currentWebUrl = it }
                                                }

                                                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                                    super.onReceivedError(view, request, error)
                                                    webLoadingProgress = 0
                                                }

                                                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                                    handler?.proceed()
                                                }
                                            }

                                            webChromeClient = object : WebChromeClient() {
                                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                    super.onProgressChanged(view, newProgress)
                                                    webLoadingProgress = newProgress
                                                }
                                            }

                                            loadUrl(currentProject.url)
                                            webViewInstance = this
                                        }
                                    },
                                    update = { view ->
                                        if (view.url != currentWebUrl && currentWebUrl.isNotBlank()) {
                                            view.loadUrl(currentWebUrl)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Simulated Bottom Bar
                        if (parsedNavItems.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(0.5.dp, DarkCardBorder)),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                parsedNavItems.forEachIndexed { index, navItem ->
                                    val isSelected = selectedNavIndex == index
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable {
                                                selectedNavIndex = index
                                                currentWebUrl = navItem.url
                                                webViewInstance?.loadUrl(navItem.url)
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (navItem.iconName) {
                                                "Explore" -> Icons.Default.Explore
                                                "ShoppingCart", "ShoppingBag" -> Icons.Default.ShoppingCart
                                                "Person", "Account" -> Icons.Default.Person
                                                else -> Icons.Default.Home
                                            },
                                            contentDescription = navItem.title,
                                            tint = if (isSelected) CorporateBlue else TextMuted,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = navItem.title,
                                            color = if (isSelected) CorporateBlue else TextMuted,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Gesture Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(TextMuted)
                            )
                        }
                    }
                }
            }
        } else {
            // Automated Audit Report View
            auditReport?.let { report ->
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassCard(
                        borderColor = DarkCardBorder,
                        backgroundColor = DarkSurface
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Pre-Flight Compatibility Audit",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Automated policy and SDK 36 verification",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess.copy(alpha = 0.15f))
                                    .border(BorderStroke(2.dp, EmeraldSuccess), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = report.overallGrade,
                                    color = EmeraldSuccess,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Itemized Diagnostics (${report.checks.size})",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        SecondaryOutlineButton(
                            text = if (isRunningAudit) "Auditing..." else "Re-Run Audit",
                            onClick = {
                                isRunningAudit = true
                                coroutineScope.launch {
                                    delay(400)
                                    auditReport = AutomatedTestingEngine.runFullAudit(currentProject)
                                    isRunningAudit = false
                                }
                            },
                            icon = Icons.Default.Refresh
                        )
                    }

                    for (check in report.checks) {
                        GlassCard(
                            backgroundColor = DarkSurface,
                            borderColor = if (check.passed) DarkCardBorder else Color(0xFFEF4444).copy(alpha = 0.5f),
                            cornerRadius = 10.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = if (check.passed) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (check.passed) EmeraldSuccess else Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = check.title,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        StatusBadge(
                                            text = check.category,
                                            color = if (check.passed) CorporateBlue else Color(0xFFEF4444)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = check.detail,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
