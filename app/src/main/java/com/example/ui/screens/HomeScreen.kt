package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.auth.FirebaseAuthManager
import com.example.data.model.BuildStatus
import com.example.data.repository.AppRepository
import com.example.engine.BuildPipelineEngine
import com.example.engine.DeviceTelemetryTracker
import com.example.ui.components.AshokaChakraHUD
import com.example.ui.components.AuthModal
import com.example.ui.components.CorporateButton
import com.example.ui.components.DeviceCompatibilityBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.MakeInIndiaBadge
import com.example.ui.components.MetricCard
import com.example.ui.components.ProjectIconBadge
import com.example.ui.components.SecondaryOutlineButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.TricolourHeaderBar
import com.example.ui.theme.AshokaBlue
import com.example.ui.theme.AshokaCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.IndiaGreen
import com.example.ui.theme.IndiaGreenLight
import com.example.ui.theme.IndianSaffron
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSaffron
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    repository: AppRepository,
    onCreateAppClick: (String?) -> Unit,
    onOpenPreview: (String) -> Unit,
    onOpenCustomize: (String) -> Unit,
    onOpenBuild: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val telemetry = DeviceTelemetryTracker.getDeviceTelemetry(context)
    val sessionProfile by FirebaseAuthManager.sessionProfile.collectAsState()

    val projects by repository.allProjects.collectAsState(initial = emptyList())
    val builds by repository.allBuilds.collectAsState(initial = emptyList())
    val currentPlan by repository.currentPlan.collectAsState()

    var showAuthDialog by remember { mutableStateOf(false) }

    val totalProjects = projects.size
    val successfulBuilds = builds.count { it.status == BuildStatus.SUCCESS }
    val readyToPublishCount = projects.count { it.versionCode >= 1 }

    // Live pulsing radar alpha for futuristic HUD
    val pulseTransition = rememberInfiniteTransition(label = "hud_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    if (showAuthDialog) {
        AuthModal(onDismissRequest = { showAuthDialog = false })
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. User Authentication Biometric Strip
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(DarkSurfaceElevated, DarkSurface)
                        )
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (sessionProfile != null) IndiaGreenLight.copy(alpha = 0.5f) else IndianSaffron.copy(alpha = 0.4f)
                        ),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (sessionProfile != null) IndiaGreenLight.copy(alpha = 0.18f) else IndianSaffron.copy(alpha = 0.18f))
                            .border(
                                BorderStroke(1.dp, if (sessionProfile != null) IndiaGreenLight else IndianSaffron),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (sessionProfile != null) Icons.Default.Verified else Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = if (sessionProfile != null) IndiaGreenLight else IndianSaffron,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column {
                        Text(
                            text = sessionProfile?.displayName ?: "Bharat Studio Guest",
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (sessionProfile != null) {
                                sessionProfile?.email ?: sessionProfile?.phoneNumber ?: "Firebase Cloud Synced"
                            } else {
                                "Sign in with Google or +91 Mobile OTP"
                            },
                            color = TextSecondary,
                            fontSize = 10.5.sp
                        )
                    }
                }

                if (sessionProfile != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusBadge(text = "VERIFIED", color = IndiaGreenLight)
                        IconButton(
                            onClick = { FirebaseAuthManager.signOut() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Sign Out",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.horizontalGradient(listOf(IndianSaffron, Color(0xFFFFB347)))
                            )
                            .clickable { showAuthDialog = true }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("home_sign_in_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign In",
                            color = Color(0xFF1A0E00),
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Futuristic Cyber-Bharat Hero Cockpit Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    IndianSaffron.copy(alpha = 0.7f),
                                    AshokaCyan.copy(alpha = 0.55f),
                                    IndiaGreenLight.copy(alpha = 0.65f)
                                )
                            )
                        ),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                // Hero Artwork Background
                Image(
                    painter = painterResource(id = R.drawable.img_bharat_futuristic_hero),
                    contentDescription = "Futuristic Bharat Digital Mandala Hero Banner",
                    modifier = Modifier
                        .matchParentSize(),
                    contentScale = ContentScale.Crop
                )

                // Deep Obsidian Readability Scrim
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    DarkBackground.copy(alpha = 0.94f),
                                    DarkSurface.copy(alpha = 0.80f),
                                    DarkBackground.copy(alpha = 0.45f)
                                )
                            )
                        )
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    TricolourHeaderBar()
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                MakeInIndiaBadge()
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Website2App Studio",
                                    color = TextPrimary,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    letterSpacing = (-0.4).sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "CYBER-BHARAT IDE",
                                        color = AshokaCyan,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(text = "//", color = TextMuted, fontSize = 11.sp)
                                    Text(
                                        text = "v10.0",
                                        color = IndianSaffron,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            AshokaChakraHUD(
                                sizeDp = 54.dp,
                                primaryRingColor = AshokaCyan,
                                accentColor = IndianSaffron,
                                animate = true
                            )
                        }

                        Text(
                            text = "Transform any web portal or PWA into a hardware-adaptive Android APK/AAB tailored dynamically for tablets, foldables, and smartphones.",
                            color = TextSecondary,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )

                        CorporateButton(
                            text = "Convert Website to App (v10.0)",
                            icon = Icons.Default.Add,
                            onClick = { onCreateAppClick(null) },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "home_create_app_button"
                        )
                    }
                }
            }
        }

        // 3. Live Device Hardware Radar & Telemetry HUD
        item {
            SectionHeader(title = "Live Hardware Radar", tag = "REAL-TIME TELEMETRY")
            Spacer(modifier = Modifier.height(8.dp))
            GlassCard(
                borderColor = if (telemetry.isTablet) IndianSaffron.copy(alpha = 0.6f) else AshokaCyan.copy(alpha = 0.55f),
                backgroundColor = DarkSurface,
                showTricolourTopBar = false
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = if (telemetry.isTablet) {
                                                listOf(IndianSaffron, Color(0xFFFFB347))
                                            } else {
                                                listOf(AshokaBlue, AshokaCyan)
                                            }
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (telemetry.isTablet) Icons.Default.Tablet else Icons.Default.Smartphone,
                                    contentDescription = null,
                                    tint = if (telemetry.isTablet) Color(0xFF1A0E00) else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .alpha(pulseAlpha)
                                            .clip(CircleShape)
                                            .background(IndiaGreenLight)
                                    )
                                    Text(
                                        text = telemetry.deviceName,
                                        color = TextPrimary,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp
                                    )
                                }
                                Text(
                                    text = "${telemetry.category.title} • ${telemetry.androidVersion}",
                                    color = if (telemetry.isTablet) TextSaffron else AshokaCyan,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        DeviceCompatibilityBadge(isTablet = telemetry.isTablet)
                    }

                    // Architectural Spec Grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkBackground.copy(alpha = 0.65f))
                            .border(BorderStroke(0.5.dp, DarkCardBorder), RoundedCornerShape(10.dp))
                            .padding(11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "DISPLAY", color = TextMuted, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp)
                            Text(
                                text = "${telemetry.screenWidthDp}x${telemetry.screenHeightDp}dp",
                                color = TextPrimary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "sw${telemetry.smallestScreenWidthDp}dp",
                                color = AshokaCyan,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "CPU / ABI", color = TextMuted, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp)
                            Text(
                                text = telemetry.supportedAbis.firstOrNull() ?: "Universal",
                                color = TextPrimary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "API ${telemetry.apiLevel}",
                                color = TextSaffron,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "LINK / PWR", color = TextMuted, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp)
                            Text(
                                text = telemetry.networkType.substringBefore(" "),
                                color = IndiaGreenLight,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${telemetry.batteryPercentage}% Charge",
                                color = TextSecondary,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Text(
                        text = telemetry.tabletCompatibilitySummary,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 4. Studio Analytics
        item {
            SectionHeader(title = "Studio Telemetry", tag = "CLOUD ENGINE")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Projects",
                    value = totalProjects.toString(),
                    icon = Icons.Default.Layers,
                    accentColor = IndianSaffron,
                    modifier = Modifier.weight(1f),
                    subtext = "${maxOf(0, currentPlan.maxProjects - totalProjects)} slots"
                )
                MetricCard(
                    title = "Compiled",
                    value = successfulBuilds.toString(),
                    icon = Icons.Default.Android,
                    accentColor = IndiaGreenLight,
                    modifier = Modifier.weight(1f),
                    subtext = "All devices"
                )
                MetricCard(
                    title = "Verified",
                    value = readyToPublishCount.toString(),
                    icon = Icons.Default.CloudUpload,
                    accentColor = AshokaCyan,
                    modifier = Modifier.weight(1f),
                    subtext = "v9.0 ready"
                )
            }
        }

        // 5. Active Projects Portfolio
        item {
            SectionHeader(
                title = "Application Projects (${projects.size})",
                tag = if (projects.isEmpty()) "EMPTY" else "ACTIVE WORKSPACE"
            )
        }

        if (projects.isEmpty()) {
            item {
                GlassCard(
                    borderColor = DarkCardBorder,
                    backgroundColor = DarkSurface,
                    cornerRadius = 14.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AshokaChakraHUD(
                            sizeDp = 52.dp,
                            primaryRingColor = AshokaCyan,
                            accentColor = IndianSaffron,
                            animate = true
                        )
                        Text(
                            text = "No Applications Configured",
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enter any website URL to inspect its architecture and compile a futuristic, device-adaptive Android application.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CorporateButton(
                            text = "Start Conversion",
                            icon = Icons.Default.Add,
                            onClick = { onCreateAppClick(null) },
                            testTag = "home_empty_start_conversion"
                        )
                    }
                }
            }
        } else {
            items(projects) { project ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = DarkCardBorder,
                    backgroundColor = DarkSurface,
                    cornerRadius = 14.dp,
                    onClick = { onOpenPreview(project.id) }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                ProjectIconBadge(
                                    symbolName = project.iconEmoji,
                                    sizeDp = 42.dp,
                                    tintColor = IndianSaffron
                                )
                                Column {
                                    Text(
                                        text = project.name,
                                        color = TextPrimary,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = project.packageName,
                                        color = TextMuted,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }
                            StatusBadge(
                                text = "v${project.versionName} (${project.versionCode})",
                                color = IndianSaffron
                            )
                        }

                        Text(
                            text = project.url,
                            color = AshokaCyan,
                            fontSize = 12.sp,
                            maxLines = 1
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatusBadge(text = project.conversionMode.badge, color = AshokaCyan)
                            StatusBadge(text = project.targetDeviceProfile.badge, color = IndiaGreenLight)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SecondaryOutlineButton(
                                text = "Pin App",
                                icon = Icons.Default.PushPin,
                                onClick = {
                                    BuildPipelineEngine.pinToHomeScreen(context, project)
                                },
                                modifier = Modifier.weight(1f),
                                borderColor = IndiaGreenLight.copy(alpha = 0.6f),
                                textColor = IndiaGreenLight,
                                testTag = "home_pin_app_${project.id}"
                            )
                            SecondaryOutlineButton(
                                text = "Preview",
                                icon = Icons.Default.PlayArrow,
                                onClick = { onOpenPreview(project.id) },
                                modifier = Modifier.weight(1f),
                                borderColor = AshokaCyan.copy(alpha = 0.6f),
                                textColor = AshokaCyan,
                                testTag = "home_preview_button_${project.id}"
                            )
                            CorporateButton(
                                text = "Build",
                                icon = Icons.Default.Build,
                                onClick = { onOpenBuild(project.id) },
                                modifier = Modifier.weight(1f),
                                testTag = "home_build_button_${project.id}"
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
