package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.StandaloneAppActivity
import com.example.data.model.AppProject
import com.example.data.model.BuildRecord
import com.example.data.model.BuildStatus
import com.example.data.model.BuildType
import com.example.data.model.SigningKey
import com.example.data.model.TargetDeviceProfile
import com.example.data.repository.AppRepository
import com.example.engine.BuildPipelineEngine
import com.example.engine.DeviceTelemetryTracker
import com.example.ui.components.CorporateButton
import com.example.ui.components.DeviceCompatibilityBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.MakeInIndiaBadge
import com.example.ui.components.SecondaryOutlineButton
import com.example.ui.components.StatusBadge
import com.example.ui.components.TerminalLogViewer
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildCenterScreen(
    initialProjectId: String?,
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val telemetry = DeviceTelemetryTracker.getDeviceTelemetry(context)

    val projects by repository.allProjects.collectAsState(initial = emptyList())
    val builds by repository.allBuilds.collectAsState(initial = emptyList())
    val signingKeys by repository.signingKeys.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Build Pipeline, 1: Keystores
    var selectedProject by remember { mutableStateOf<AppProject?>(null) }
    var selectedBuildType by remember { mutableStateOf(BuildType.RELEASE_APK) }
    var selectedTargetProfile by remember { mutableStateOf(TargetDeviceProfile.AUTO_DETECT) }
    var selectedKeystoreAlias by remember { mutableStateOf("production-release-key") }

    var isBuilding by remember { mutableStateOf(false) }
    var activeBuildRecord by remember { mutableStateOf<BuildRecord?>(null) }
    var projectDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(projects, initialProjectId) {
        if (selectedProject == null && projects.isNotEmpty()) {
            selectedProject = if (initialProjectId != null) {
                projects.find { it.id == initialProjectId } ?: projects.first()
            } else {
                projects.first()
            }
        }
    }

    fun startCompilation() {
        val proj = selectedProject ?: return
        isBuilding = true

        coroutineScope.launch {
            BuildPipelineEngine.runBuild(
                context = context,
                project = proj,
                buildType = selectedBuildType,
                keystoreAlias = selectedKeystoreAlias,
                targetProfile = selectedTargetProfile
            ).collectLatest { record ->
                activeBuildRecord = record
                if (record.status.isTerminal) {
                    isBuilding = false
                    repository.saveBuild(record)
                }
            }
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
                        text = "Build & Artifact Compiler",
                        color = TextPrimary,
                        fontFamily = com.example.ui.theme.SpaceGroteskFontFamily,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "CYBER-BHARAT COMPILER // v10.0",
                        color = IndianSaffron,
                        fontFamily = com.example.ui.theme.JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                MakeInIndiaBadge()
            }
        }

        // Live Hardware Diagnostics & Tablet Compatibility Banner
        item {
            GlassCard(
                borderColor = if (telemetry.isTablet) IndianSaffron.copy(alpha = 0.5f) else AshokaBlue.copy(alpha = 0.5f),
                backgroundColor = DarkSurface,
                showTricolourTopBar = true
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
                                imageVector = if (telemetry.isTablet) Icons.Default.Tablet else Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = if (telemetry.isTablet) IndianSaffron else AshokaBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Current Device: ${telemetry.deviceName}",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        DeviceCompatibilityBadge(isTablet = telemetry.isTablet)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Display: ${telemetry.screenWidthDp}x${telemetry.screenHeightDp} dp (sw${telemetry.smallestScreenWidthDp}dp)",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(text = "•", color = TextMuted)
                        Text(
                            text = telemetry.androidVersion,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(IndiaGreen.copy(alpha = 0.12f))
                            .border(BorderStroke(0.5.dp, IndiaGreen.copy(alpha = 0.4f)), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = IndiaGreen, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Tablet Ready: xlargeScreens declared, non-telephony WiFi compliant, and dual-mode responsive.",
                                color = IndiaGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Installation Diagnostic Guide Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0C1A38))
                    .border(BorderStroke(0.5.dp, DarkCardBorder), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = IndianSaffron, modifier = Modifier.size(15.dp))
                        Text(
                            text = "Tablet & Phone Installation Troubleshooting Guide",
                            color = IndianSaffron,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "1. Signature Conflict: If updating on your tablet, uninstall previous test builds first to avoid signature conflicts.\n2. One-Tap Launcher: Tap 'Pin to Home Screen' below to install an instant desktop shortcut without package conflicts.\n3. Universal Source: Download 'Android Studio Project (.zip)' for a full standalone Gradle package.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Tabs
        item {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkSurface,
                contentColor = IndianSaffron,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = IndianSaffron,
                        height = 2.dp
                    )
                }
            ) {
                Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("Build Pipeline") })
                Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Keystore Credentials") })
            }
        }

        if (selectedTabIndex == 0) {
            // Build Pipeline Tab
            item {
                GlassCard(backgroundColor = DarkSurface) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "1. Target Application Project", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        if (projects.isEmpty()) {
                            Text(
                                text = "No application projects available. Please convert a website first from the Create App section.",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        } else {
                            Box {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurfaceVariant)
                                        .border(BorderStroke(1.dp, DarkCardBorder), RoundedCornerShape(8.dp))
                                        .clickable { projectDropdownExpanded = true }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedProject?.name ?: "Select an application project",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(text = "▼", color = IndianSaffron, fontSize = 12.sp)
                                }

                                DropdownMenu(
                                    expanded = projectDropdownExpanded,
                                    onDismissRequest = { projectDropdownExpanded = false },
                                    modifier = Modifier.background(DarkSurfaceVariant)
                                ) {
                                    projects.forEach { proj ->
                                        DropdownMenuItem(
                                            text = { Text("${proj.name} (${proj.packageName})", color = TextPrimary) },
                                            onClick = {
                                                selectedProject = proj
                                                projectDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "2. Target Device Compatibility Profile", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                TargetDeviceProfile.AUTO_DETECT,
                                TargetDeviceProfile.TABLET_STANDARD,
                                TargetDeviceProfile.TABLET_PRO,
                                TargetDeviceProfile.TABLET_COMPACT,
                                TargetDeviceProfile.PHONE_COMPACT,
                                TargetDeviceProfile.PHONE_LARGE,
                                TargetDeviceProfile.FOLDABLE,
                                TargetDeviceProfile.UNIVERSAL
                            ).forEach { profile ->
                                val isSelected = selectedTargetProfile == profile
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) IndianSaffron.copy(alpha = 0.12f) else DarkSurfaceVariant)
                                        .border(BorderStroke(1.dp, if (isSelected) IndianSaffron else DarkCardBorder), RoundedCornerShape(8.dp))
                                        .clickable { selectedTargetProfile = profile }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) IndianSaffron else Color.Transparent)
                                                .border(BorderStroke(1.5.dp, if (isSelected) IndianSaffron else TextMuted), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF1E1100), modifier = Modifier.size(10.dp))
                                            }
                                        }
                                        Column {
                                            Text(text = profile.label, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(text = profile.description, color = TextSecondary, fontSize = 11.sp)
                                        }
                                    }
                                    StatusBadge(text = profile.badge, color = if (isSelected) IndianSaffron else TextMuted)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "3. Build Artifact Format", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (type in BuildType.values()) {
                                val isSelected = selectedBuildType == type
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) DarkSurfaceVariant else Color.Transparent)
                                        .border(BorderStroke(1.dp, if (isSelected) IndianSaffron else DarkCardBorder), RoundedCornerShape(8.dp))
                                        .clickable { selectedBuildType = type }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = type.label, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(text = type.description, color = TextSecondary, fontSize = 11.sp)
                                    }
                                    StatusBadge(text = type.extension, color = if (isSelected) IndianSaffron else TextMuted)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "4. Release Signing Keystore", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = IndiaGreen, modifier = Modifier.size(18.dp))
                                Text(text = selectedKeystoreAlias, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            StatusBadge(text = "RSA 2048 / v1+v2", color = IndiaGreen)
                        }

                        CorporateButton(
                            text = if (isBuilding) "Compiling Standalone APK (v10.0)..." else "Execute Cloud Build (v10.0)",
                            icon = Icons.Default.Build,
                            onClick = { startCompilation() },
                            enabled = !isBuilding && selectedProject != null,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "start_cloud_build_button"
                        )
                    }
                }
            }

            // Active or latest build monitor
            activeBuildRecord?.let { currentBuild ->
                item {
                    GlassCard(
                        borderColor = if (currentBuild.status == BuildStatus.SUCCESS) IndiaGreen else IndianSaffron,
                        backgroundColor = DarkSurface,
                        showTricolourTopBar = currentBuild.status == BuildStatus.SUCCESS
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${currentBuild.projectName} • ${currentBuild.buildType.label}",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = currentBuild.status.label,
                                            color = if (currentBuild.status == BuildStatus.SUCCESS) IndiaGreen else IndianSaffron,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(text = "•", color = TextMuted)
                                        Text(
                                            text = currentBuild.targetDeviceLabel,
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Text(
                                    text = "${currentBuild.progress}%",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            LinearProgressIndicator(
                                progress = { currentBuild.progress / 100f },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = if (currentBuild.status == BuildStatus.SUCCESS) IndiaGreen else IndianSaffron,
                                trackColor = Color(0xFF1E293B)
                            )

                            // Terminal Logs
                            TerminalLogViewer(
                                logs = currentBuild.logs,
                                title = "Build Stream • ${currentBuild.status.name}"
                            )

                            // Action buttons when build succeeds
                            if (currentBuild.status == BuildStatus.SUCCESS) {
                                val file = File(currentBuild.downloadPath)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Install APK Directly button
                                        if (currentBuild.buildType == BuildType.DEBUG_APK || currentBuild.buildType == BuildType.RELEASE_APK) {
                                            CorporateButton(
                                                text = "Install APK",
                                                icon = Icons.Default.Launch,
                                                onClick = {
                                                    if (file.exists()) {
                                                        BuildPipelineEngine.launchApkInstaller(context, file)
                                                    } else {
                                                        Toast.makeText(context, "Package file ready", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                testTag = "install_apk_button"
                                            )
                                        }

                                        // Pin to Home Screen (Instant Native Launcher)
                                        selectedProject?.let { proj ->
                                            SecondaryOutlineButton(
                                                text = "Pin App",
                                                icon = Icons.Default.PushPin,
                                                onClick = {
                                                    BuildPipelineEngine.pinToHomeScreen(context, proj)
                                                },
                                                modifier = Modifier.weight(1f),
                                                borderColor = IndiaGreen,
                                                textColor = IndiaGreen,
                                                testTag = "pin_app_shortcut_button"
                                            )

                                            // Run in Standalone Mode
                                            SecondaryOutlineButton(
                                                text = "Run App",
                                                icon = Icons.Default.PlayArrow,
                                                onClick = {
                                                    val intent = StandaloneAppActivity.createIntent(
                                                        context = context,
                                                        url = proj.url,
                                                        name = proj.name,
                                                        iconCategory = proj.iconEmoji,
                                                        navJson = proj.navItemsJson,
                                                        splashTitle = proj.splashTitle,
                                                        splashSubtitle = proj.splashSubtitle,
                                                        splashDelayMs = proj.splashDelayMs,
                                                        forceTablet = (selectedTargetProfile == TargetDeviceProfile.TABLET_STANDARD || selectedTargetProfile == TargetDeviceProfile.TABLET_PRO)
                                                    )
                                                    context.startActivity(intent)
                                                },
                                                modifier = Modifier.weight(1f),
                                                borderColor = IndianSaffron,
                                                textColor = IndianSaffron,
                                                testTag = "run_standalone_app_button"
                                            )
                                        }

                                        // Share/Export File button
                                        SecondaryOutlineButton(
                                            text = "Export",
                                            icon = Icons.Default.CloudDownload,
                                            onClick = {
                                                if (file.exists()) {
                                                    val mimeType = when {
                                                        currentBuild.fileName.endsWith(".zip") -> "application/zip"
                                                        currentBuild.fileName.endsWith(".aab") -> "application/octet-stream"
                                                        else -> "application/vnd.android.package-archive"
                                                    }
                                                    val uri = FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.provider",
                                                        file
                                                    )
                                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                                        type = mimeType
                                                        putExtra(Intent.EXTRA_STREAM, uri)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(Intent.createChooser(intent, "Share ${currentBuild.fileName}"))
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            borderColor = AshokaBlue,
                                            textColor = AshokaBlue,
                                            testTag = "export_artifact_button"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Build History
            item {
                Text(
                    text = "Build History (${builds.size})",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (builds.isEmpty()) {
                item {
                    GlassCard(backgroundColor = DarkSurface) {
                        Text(text = "No builds recorded yet. Configure a project and click 'Execute Cloud Build' above.", color = TextMuted, fontSize = 12.sp)
                    }
                }
            } else {
                items(builds) { build ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = DarkCardBorder,
                        backgroundColor = DarkSurface
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "${build.projectName} • ${build.buildType.label}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = if (build.fileName.isNotBlank()) "${build.fileName} (${BuildPipelineEngine.formatBytes(build.fileSizeBytes)})" else "Build ID: ${build.id}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Target: ${build.targetDeviceLabel}",
                                    color = IndianSaffron,
                                    fontSize = 10.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusBadge(
                                    text = build.status.name,
                                    color = if (build.status == BuildStatus.SUCCESS) IndiaGreen else Color(0xFFEF4444)
                                )
                                IconButton(
                                    onClick = {
                                        activeBuildRecord = build
                                    }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "View Logs", tint = IndianSaffron, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Signing Keystores Tab
            item {
                GlassCard(backgroundColor = DarkSurface) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "Verified JKS Keystores", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = "Manage cryptographic signatures for Google Play Store production and internal deployment.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        signingKeys.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(BorderStroke(1.dp, if (selectedKeystoreAlias == key.alias) IndianSaffron else DarkCardBorder), RoundedCornerShape(8.dp))
                                    .clickable { selectedKeystoreAlias = key.alias }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(text = key.alias, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            if (selectedKeystoreAlias == key.alias) {
                                                StatusBadge(text = "ACTIVE", color = IndianSaffron)
                                            }
                                        }
                                        Text(text = "Org: ${key.organization} • Validity: ${key.validityYears} Years", color = TextSecondary, fontSize = 11.sp)
                                        Text(text = "SHA-256: ${key.sha256.take(24)}...", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = IndiaGreen, modifier = Modifier.size(20.dp))
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
