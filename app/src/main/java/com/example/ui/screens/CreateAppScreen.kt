package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppProject
import com.example.data.model.ConversionMode
import com.example.data.model.NavStyle
import com.example.data.model.TargetDeviceProfile
import com.example.data.model.WebsiteAnalysisResult
import com.example.data.repository.AppRepository
import com.example.engine.BuildPipelineEngine
import com.example.engine.WebsiteAnalysisEngine
import com.example.ui.components.CorporateButton
import com.example.ui.components.DeviceCompatibilityBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.MakeInIndiaBadge
import com.example.ui.components.ProjectIconBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.TricolourHeaderBar
import com.example.ui.theme.AshokaBlue
import com.example.ui.theme.CorporateBlue
import com.example.ui.theme.CorporateCobalt
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndiaGreen
import com.example.ui.theme.IndianSaffron
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAppScreen(
    repository: AppRepository,
    initialUrl: String? = null,
    onAppCreated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hardware = BuildPipelineEngine.detectCurrentDevice(context)

    var urlInput by remember { mutableStateOf(initialUrl ?: "https://") }
    var appNameInput by remember { mutableStateOf("") }
    var sitemapInput by remember { mutableStateOf("") }

    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableStateOf(0) }
    var analysisStatusText by remember { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf<WebsiteAnalysisResult?>(null) }
    var selectedMode by remember { mutableStateOf(ConversionMode.SMART_WEBVIEW) }
    var selectedDeviceProfile by remember { mutableStateOf(TargetDeviceProfile.AUTO_DETECT) }
    var selectedCategorySymbol by remember { mutableStateOf("web") }
    var selectedColor by remember { mutableStateOf("#FF9933") }

    val categorySymbols = listOf(
        Pair("web", "Portal"),
        Pair("business", "Enterprise"),
        Pair("store", "E-Commerce"),
        Pair("cloud", "SaaS"),
        Pair("dashboard", "Analytics"),
        Pair("code", "Developer"),
        Pair("article", "Media"),
        Pair("tablet", "Tablet App")
    )

    fun startAnalysis() {
        if (urlInput.isBlank() || urlInput == "https://") return
        isAnalyzing = true
        analysisProgress = 15
        analysisStatusText = "Resolving host domain and TLS certificates..."

        coroutineScope.launch {
            delay(350)
            analysisProgress = 45
            analysisStatusText = "Analyzing HTML5 viewport, meta structure & webmanifest..."
            delay(400)
            analysisProgress = 75
            analysisStatusText = "Inspecting navigation hierarchy, forms & endpoints..."

            val result = WebsiteAnalysisEngine.analyzeWebsite(urlInput)
            delay(300)
            analysisProgress = 100
            analysisStatusText = "Website Inspection Complete"
            delay(150)

            isAnalyzing = false
            analysisResult = result
            if (appNameInput.isBlank()) {
                appNameInput = result.title.ifBlank { "Mobile Web App" }
            }
            selectedMode = result.recommendedMode
        }
    }

    LaunchedEffect(initialUrl) {
        if (initialUrl != null && initialUrl.isNotBlank() && initialUrl != "https://") {
            startAnalysis()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Website Import & Analysis",
                        color = TextPrimary,
                        fontFamily = com.example.ui.theme.SpaceGroteskFontFamily,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cyber-Bharat Engine // v10.0",
                        color = IndianSaffron,
                        fontFamily = com.example.ui.theme.JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                MakeInIndiaBadge()
            }
        }

        // URL Input Card
        item {
            GlassCard(
                borderColor = if (analysisResult != null) IndiaGreen.copy(alpha = 0.5f) else IndianSaffron.copy(alpha = 0.4f),
                backgroundColor = DarkSurface,
                showTricolourTopBar = true
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "1. Website Connection URL",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("website_url_input"),
                        label = { Text("Target Website URL (HTTPS)") },
                        placeholder = { Text("https://mycompany.com") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndianSaffron,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = IndianSaffron
                        )
                    )

                    OutlinedTextField(
                        value = appNameInput,
                        onValueChange = { appNameInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_name_input"),
                        label = { Text("Application Display Name (Optional)") },
                        placeholder = { Text("My Enterprise App") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndianSaffron,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = IndianSaffron
                        )
                    )

                    CorporateButton(
                        text = if (isAnalyzing) "Analyzing Structure..." else "Analyze & Inspect Website",
                        icon = Icons.Default.AutoAwesome,
                        onClick = { startAnalysis() },
                        enabled = !isAnalyzing && urlInput.length > 8,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "analyze_website_button"
                    )

                    if (isAnalyzing) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { analysisProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = IndianSaffron,
                                trackColor = Color(0xFF1E293B)
                            )
                            Text(
                                text = analysisStatusText,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Hardware & Device Compatibility Target Selector
        item {
            Text(
                text = "2. Target Device Architecture",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            GlassCard(backgroundColor = DarkSurface) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detected Hardware: ${hardware.classification}",
                            color = IndianSaffron,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        DeviceCompatibilityBadge(isTablet = hardware.isTablet)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TargetDeviceProfile.values().forEach { profile ->
                            val isSelected = selectedDeviceProfile == profile
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) IndianSaffron.copy(alpha = 0.12f) else DarkSurfaceVariant)
                                    .border(
                                        BorderStroke(1.dp, if (isSelected) IndianSaffron else DarkCardBorder),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedDeviceProfile = profile }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) IndianSaffron else Color.Transparent)
                                        .border(BorderStroke(2.dp, if (isSelected) IndianSaffron else TextMuted), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF1E1100), modifier = Modifier.size(13.dp))
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(text = profile.label, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        StatusBadge(text = profile.badge, color = if (isSelected) IndianSaffron else TextMuted)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = profile.description, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Analysis Results Card
        analysisResult?.let { res ->
            item {
                Text(
                    text = "3. Inspection & Compatibility Report",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                GlassCard(
                    borderColor = IndiaGreen.copy(alpha = 0.4f),
                    backgroundColor = DarkSurface
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = res.title.ifBlank { "Identified Web Resource" },
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = res.url,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            StatusBadge(
                                text = "Score: ${res.readinessScore}/100",
                                color = IndiaGreen
                            )
                        }

                        // Readiness Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusBadge(text = if (res.isHttps) "TLS Encrypted" else "Insecure HTTP", color = if (res.isHttps) IndiaGreen else Color(0xFFEF4444))
                            StatusBadge(text = if (res.isMobileResponsive) "Adaptive Viewport" else "Fixed Width", color = if (res.isMobileResponsive) IndiaGreen else IndianSaffron)
                            StatusBadge(text = if (res.pwaManifestFound) "PWA Manifest" else "Standard HTML5", color = AshokaBlue)
                        }

                        if (res.metaDescription.isNotBlank()) {
                            Text(
                                text = res.metaDescription,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Architecture Mode Selector
            item {
                Text(
                    text = "4. Conversion Architecture Mode",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConversionMode.values().forEach { mode ->
                        val isSelected = selectedMode == mode
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = if (isSelected) IndianSaffron else DarkCardBorder,
                            backgroundColor = if (isSelected) DarkSurfaceVariant else DarkSurface,
                            onClick = { selectedMode = mode }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) IndianSaffron else Color.Transparent)
                                        .border(BorderStroke(2.dp, if (isSelected) IndianSaffron else TextMuted), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF1E1100),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = mode.label,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        if (res.recommendedMode == mode) {
                                            StatusBadge(text = "RECOMMENDED", color = IndiaGreen)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = mode.description,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category Selector
            item {
                Text(
                    text = "5. Application Sector & Classification",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                GlassCard(backgroundColor = DarkSurface) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categorySymbols.take(4).forEach { (symbolKey, label) ->
                                val isSelected = selectedCategorySymbol == symbolKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) IndianSaffron.copy(alpha = 0.15f) else DarkSurfaceVariant)
                                        .border(BorderStroke(1.dp, if (isSelected) IndianSaffron else DarkCardBorder), RoundedCornerShape(8.dp))
                                        .clickable { selectedCategorySymbol = symbolKey }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        ProjectIconBadge(symbolName = symbolKey, sizeDp = 28.dp, tintColor = IndianSaffron)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = label, color = if (isSelected) IndianSaffron else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categorySymbols.drop(4).take(4).forEach { (symbolKey, label) ->
                                val isSelected = selectedCategorySymbol == symbolKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) IndianSaffron.copy(alpha = 0.15f) else DarkSurfaceVariant)
                                        .border(BorderStroke(1.dp, if (isSelected) IndianSaffron else DarkCardBorder), RoundedCornerShape(8.dp))
                                        .clickable { selectedCategorySymbol = symbolKey }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        ProjectIconBadge(symbolName = symbolKey, sizeDp = 28.dp, tintColor = IndianSaffron)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = label, color = if (isSelected) IndianSaffron else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Generate Button
            item {
                CorporateButton(
                    text = "Generate Bharat Enterprise App (v10.0)",
                    icon = Icons.Default.Layers,
                    onClick = {
                        coroutineScope.launch {
                            val domain = WebsiteAnalysisEngine.extractDomain(urlInput)
                            val pkg = WebsiteAnalysisEngine.generatePackageName(domain)
                            val finalName = appNameInput.ifBlank { res.title.ifBlank { domain.substringBefore(".") } }
                            val projectId = "app_${UUID.randomUUID().toString().take(8)}"

                            val navItemsJson = if (res.detectedPages.isNotEmpty()) {
                                "[" + res.detectedPages.joinToString(",") {
                                    """{"title":"${it.title}","url":"${it.url}","iconName":"${it.iconName}"}"""
                                } + "]"
                            } else "[]"

                            val newProject = AppProject(
                                id = projectId,
                                name = finalName,
                                url = res.url,
                                packageName = pkg,
                                versionCode = 10,
                                versionName = "10.0",
                                conversionMode = selectedMode,
                                targetDeviceProfile = selectedDeviceProfile,
                                primaryColorHex = selectedColor,
                                navStyle = NavStyle.BOTTOM_NAV,
                                navItemsJson = navItemsJson,
                                iconEmoji = selectedCategorySymbol,
                                splashTitle = finalName,
                                splashSubtitle = "Powered by Website2App Studio (Bharat Edition)"
                            )
                            repository.saveProject(newProject)
                            onAppCreated(projectId)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "complete_conversion_button"
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
