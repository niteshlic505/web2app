package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.NavItem
import com.example.ui.components.CorporateButton
import com.example.ui.components.DeviceCompatibilityBadge
import com.example.ui.components.ProjectIconBadge
import com.example.ui.components.TricolourHeaderBar
import com.example.ui.theme.AshokaBlue
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.IndianSaffron
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import org.json.JSONArray

class StandaloneAppActivity : ComponentActivity() {

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_ICON = "extra_icon"
        const val EXTRA_NAV_JSON = "extra_nav_json"
        const val EXTRA_SPLASH_TITLE = "extra_splash_title"
        const val EXTRA_SPLASH_SUBTITLE = "extra_splash_subtitle"
        const val EXTRA_SPLASH_DELAY_MS = "extra_splash_delay_ms"
        const val EXTRA_FORCE_TABLET = "extra_force_tablet"

        fun createIntent(
            context: Context,
            url: String,
            name: String,
            iconCategory: String = "web",
            navJson: String = "[]",
            splashTitle: String = "",
            splashSubtitle: String = "Bharat Enterprise Web Application",
            splashDelayMs: Long = 1500L,
            forceTablet: Boolean = false
        ): Intent {
            return Intent(context, StandaloneAppActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_NAME, name)
                putExtra(EXTRA_ICON, iconCategory)
                putExtra(EXTRA_NAV_JSON, navJson)
                putExtra(EXTRA_SPLASH_TITLE, splashTitle.ifBlank { name })
                putExtra(EXTRA_SPLASH_SUBTITLE, splashSubtitle)
                putExtra(EXTRA_SPLASH_DELAY_MS, splashDelayMs)
                putExtra(EXTRA_FORCE_TABLET, forceTablet)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val targetUrl = intent.getStringExtra(EXTRA_URL) ?: "https://example.com"
        val appName = intent.getStringExtra(EXTRA_NAME) ?: "Web Application"
        val iconCategory = intent.getStringExtra(EXTRA_ICON) ?: "web"
        val navJson = intent.getStringExtra(EXTRA_NAV_JSON) ?: "[]"
        val splashTitle = intent.getStringExtra(EXTRA_SPLASH_TITLE) ?: appName
        val splashSubtitle = intent.getStringExtra(EXTRA_SPLASH_SUBTITLE) ?: "Bharat Enterprise Application"
        val splashDelayMs = intent.getLongExtra(EXTRA_SPLASH_DELAY_MS, 1500L)
        val forceTablet = intent.getBooleanExtra(EXTRA_FORCE_TABLET, false)

        val navItems = parseNavItems(navJson, targetUrl)

        setContent {
            MyApplicationTheme {
                StandaloneAppScreen(
                    targetUrl = targetUrl,
                    appName = appName,
                    iconCategory = iconCategory,
                    navItems = navItems,
                    splashTitle = splashTitle,
                    splashSubtitle = splashSubtitle,
                    splashDelayMs = splashDelayMs,
                    forceTabletMode = forceTablet,
                    onCloseApp = { finish() }
                )
            }
        }
    }

    private fun parseNavItems(jsonStr: String, fallbackUrl: String): List<NavItem> {
        return runCatching {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<NavItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    NavItem(
                        title = obj.optString("title", "Item"),
                        url = obj.optString("url", fallbackUrl),
                        iconName = obj.optString("iconName", "Home")
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StandaloneAppScreen(
    targetUrl: String,
    appName: String,
    iconCategory: String,
    navItems: List<NavItem>,
    splashTitle: String,
    splashSubtitle: String,
    splashDelayMs: Long,
    forceTabletMode: Boolean = false,
    onCloseApp: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val hardwareIsTablet = configuration.smallestScreenWidthDp >= 600 || forceTabletMode
    var isDesktopMode by remember { mutableStateOf(hardwareIsTablet) }

    var isSplashVisible by remember { mutableStateOf(true) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var isOffline by remember { mutableStateOf(false) }
    var selectedNavIndex by remember { mutableIntStateOf(0) }
    var currentUrl by remember { mutableStateOf(targetUrl) }

    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onCloseApp()
        }
    }

    LaunchedEffect(Unit) {
        delay(splashDelayMs)
        isSplashVisible = false
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            // Only show bottom navigation on mobile phones; on tablets we use a side rail or top controls
            if (!isSplashVisible && navItems.isNotEmpty() && !hardwareIsTablet) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .background(Color(0xFF0A1838))
                        .border(BorderStroke(0.5.dp, DarkCardBorder))
                        .height(52.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navItems.forEachIndexed { index, item ->
                        val isSelected = selectedNavIndex == index
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    selectedNavIndex = index
                                    currentUrl = item.url
                                    webViewInstance?.loadUrl(item.url)
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = when (item.iconName) {
                                    "Explore" -> Icons.Default.Explore
                                    "ShoppingCart", "ShoppingBag" -> Icons.Default.ShoppingCart
                                    "Person", "Account" -> Icons.Default.Person
                                    else -> Icons.Default.Home
                                },
                                contentDescription = item.title,
                                tint = if (isSelected) IndianSaffron else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = item.title,
                                color = if (isSelected) IndianSaffron else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Tricolour subtle national corporate ribbon
                TricolourHeaderBar()

                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(DarkSurface)
                        .border(BorderStroke(0.5.dp, DarkCardBorder))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProjectIconBadge(symbolName = iconCategory, sizeDp = 28.dp, tintColor = IndianSaffron)
                        Column {
                            Text(
                                text = appName,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                            Text(
                                text = if (hardwareIsTablet) "Tablet Mode (sw${configuration.smallestScreenWidthDp}dp)" else "Mobile Phone",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Desktop/Tablet Viewport toggle button
                        IconButton(
                            onClick = {
                                isDesktopMode = !isDesktopMode
                                webViewInstance?.settings?.userAgentString = if (isDesktopMode) {
                                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
                                } else {
                                    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                                }
                                webViewInstance?.reload()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.Smartphone,
                                contentDescription = "Toggle Viewport",
                                tint = if (isDesktopMode) IndianSaffron else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { webViewInstance?.reload() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = onCloseApp,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                if (loadingProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { loadingProgress / 100f },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = IndianSaffron,
                        trackColor = Color.Transparent
                    )
                }

                // Tablet Navigation Rail + WebView Container
                Row(modifier = Modifier.fillMaxSize()) {
                    if (hardwareIsTablet && navItems.isNotEmpty()) {
                        // Executive Tablet Navigation Rail
                        Column(
                            modifier = Modifier
                                .width(76.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF0A1838))
                                .border(BorderStroke(0.5.dp, DarkCardBorder))
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            navItems.forEachIndexed { index, item ->
                                val isSelected = selectedNavIndex == index
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) IndianSaffron.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable {
                                            selectedNavIndex = index
                                            currentUrl = item.url
                                            webViewInstance?.loadUrl(item.url)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = when (item.iconName) {
                                            "Explore" -> Icons.Default.Explore
                                            "ShoppingCart", "ShoppingBag" -> Icons.Default.ShoppingCart
                                            "Person", "Account" -> Icons.Default.Person
                                            else -> Icons.Default.Home
                                        },
                                        contentDescription = item.title,
                                        tint = if (isSelected) IndianSaffron else TextMuted,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = item.title,
                                        color = if (isSelected) IndianSaffron else TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Main Content View
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (isOffline) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(DarkSurface)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(48.dp))
                                    Text(text = "Network Offline", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(text = "Unable to connect to the target website. Check your connectivity.", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                                    CorporateButton(
                                        text = "Retry Connection",
                                        icon = Icons.Default.Refresh,
                                        onClick = {
                                            isOffline = false
                                            webViewInstance?.reload()
                                        }
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

                                            userAgentString = if (hardwareIsTablet) {
                                                "Mozilla/5.0 (Linux; Android 14; Tablet) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
                                            } else {
                                                "Mozilla/5.0 (Linux; Android 14; Mobile; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                                            }
                                        }

                                        webViewClient = object : WebViewClient() {
                                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                                super.onPageStarted(view, url, favicon)
                                                loadingProgress = 20
                                                isOffline = false
                                            }

                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                super.onPageFinished(view, url)
                                                loadingProgress = 100
                                                url?.let { currentUrl = it }
                                            }

                                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                                super.onReceivedError(view, request, error)
                                                if (request?.isForMainFrame == true) {
                                                    isOffline = true
                                                }
                                            }

                                            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                                handler?.proceed()
                                            }
                                        }

                                        webChromeClient = object : WebChromeClient() {
                                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                super.onProgressChanged(view, newProgress)
                                                loadingProgress = newProgress
                                            }
                                        }

                                        setDownloadListener { url, _, _, _, _ ->
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                ctx.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(ctx, "Download URL: $url", Toast.LENGTH_SHORT).show()
                                            }
                                        }

                                        loadUrl(targetUrl)
                                        webViewInstance = this
                                    }
                                },
                                update = { view ->
                                    if (view.url != currentUrl && currentUrl.isNotBlank()) {
                                        view.loadUrl(currentUrl)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Splash Screen Overlay with Indian Corporate Identity
            AnimatedVisibility(
                visible = isSplashVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF050D1F)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        TricolourHeaderBar(modifier = Modifier.width(160.dp).clip(RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.height(6.dp))
                        ProjectIconBadge(symbolName = iconCategory, sizeDp = 76.dp, tintColor = IndianSaffron)
                        Text(text = splashTitle, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(text = splashSubtitle, color = IndianSaffron, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            color = IndianSaffron,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}
