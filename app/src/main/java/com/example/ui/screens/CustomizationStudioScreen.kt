package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppProject
import com.example.data.model.NavItem
import com.example.data.model.NavStyle
import com.example.data.repository.AppRepository
import com.example.ui.components.CorporateButton
import com.example.ui.components.GlassCard
import com.example.ui.components.ProjectIconBadge
import com.example.ui.components.SecondaryOutlineButton
import com.example.ui.theme.CorporateBlue
import com.example.ui.theme.CorporateCobalt
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationStudioScreen(
    projectId: String,
    repository: AppRepository,
    onNavigateBack: () -> Unit,
    onOpenPreview: ((String) -> Unit)? = null,
    onOpenBuild: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var project by remember { mutableStateOf<AppProject?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Branding, 1: Navigation, 2: Features, 3: Splash

    var name by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var versionName by remember { mutableStateOf("10.0") }
    var versionCode by remember { mutableIntStateOf(10) }
    var selectedCategorySymbol by remember { mutableStateOf("web") }
    var primaryColor by remember { mutableStateOf("#FF9933") }
    var accentColor by remember { mutableStateOf("#1A56DB") }
    var navStyle by remember { mutableStateOf(NavStyle.BOTTOM_NAV) }

    // Splash
    var splashTitle by remember { mutableStateOf("") }
    var splashSubtitle by remember { mutableStateOf("") }
    var splashDelaySeconds by remember { mutableStateOf(2f) }

    // Features toggles
    var pullToRefresh by remember { mutableStateOf(true) }
    var offlineScreen by remember { mutableStateOf(true) }
    var fileUploads by remember { mutableStateOf(true) }
    var fileDownloads by remember { mutableStateOf(true) }
    var cameraAccess by remember { mutableStateOf(false) }
    var pushNotifications by remember { mutableStateOf(true) }
    var deepLinks by remember { mutableStateOf(true) }

    val navItems = remember { mutableStateListOf<NavItem>() }

    LaunchedEffect(projectId) {
        val proj = repository.getProject(projectId)
        project = proj
        if (proj != null) {
            name = proj.name
            packageName = proj.packageName
            versionName = proj.versionName
            versionCode = proj.versionCode
            selectedCategorySymbol = proj.iconEmoji
            primaryColor = proj.primaryColorHex
            accentColor = proj.accentColorHex
            navStyle = proj.navStyle
            splashTitle = proj.splashTitle.ifBlank { proj.name }
            splashSubtitle = proj.splashSubtitle
            splashDelaySeconds = (proj.splashDelayMs / 1000f).coerceIn(1f, 5f)

            navItems.clear()
            runCatching {
                val array = JSONArray(proj.navItemsJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    navItems.add(
                        NavItem(
                            title = obj.optString("title", "Page"),
                            url = obj.optString("url", proj.url),
                            iconName = obj.optString("iconName", "Home")
                        )
                    )
                }
            }
            if (navItems.isEmpty()) {
                navItems.add(NavItem("Home", proj.url, "Home"))
                navItems.add(NavItem("Explore", "${proj.url}#explore", "Explore"))
                navItems.add(NavItem("Contact", "${proj.url}#contact", "Email"))
            }
        }
    }

    val currentProject = project ?: return

    fun saveAllChanges() {
        coroutineScope.launch {
            val jsonArray = JSONArray()
            for (item in navItems) {
                val obj = JSONObject().apply {
                    put("title", item.title)
                    put("url", item.url)
                    put("iconName", item.iconName)
                }
                jsonArray.put(obj)
            }

            val updated = currentProject.copy(
                name = name.trim().ifBlank { currentProject.name },
                packageName = packageName.trim().ifBlank { currentProject.packageName },
                versionName = versionName.trim().ifBlank { "1.0.0" },
                versionCode = versionCode,
                iconEmoji = selectedCategorySymbol,
                primaryColorHex = primaryColor,
                accentColorHex = accentColor,
                navStyle = navStyle,
                navItemsJson = jsonArray.toString(),
                splashTitle = splashTitle,
                splashSubtitle = splashSubtitle,
                splashDelayMs = (splashDelaySeconds * 1000).toLong()
            )
            repository.saveProject(updated)
            Toast.makeText(context, "Configuration updated successfully", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Column {
                        Text(
                            text = "Customization Studio",
                            color = TextPrimary,
                            fontFamily = com.example.ui.theme.SpaceGroteskFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${currentProject.name} // CONFIG",
                            color = com.example.ui.theme.IndianSaffron,
                            fontFamily = com.example.ui.theme.JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                CorporateButton(
                    text = "Save",
                    icon = Icons.Default.Save,
                    onClick = { saveAllChanges() },
                    testTag = "save_customization_button"
                )
            }
        }

        // Studio Tabs
        item {
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
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Branding") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Navigation") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Features") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Splash") })
            }
        }

        when (selectedTab) {
            0 -> {
                // Tab 0: Branding & General
                item {
                    GlassCard(backgroundColor = DarkSurface) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = "Application Metadata & Identity", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("App Name (Launcher Label)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CorporateBlue,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            OutlinedTextField(
                                value = packageName,
                                onValueChange = { packageName = it },
                                label = { Text("Package Name (Application ID)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CorporateBlue,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = versionName,
                                    onValueChange = { versionName = it },
                                    label = { Text("Version Name") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CorporateBlue,
                                        unfocusedBorderColor = DarkCardBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                OutlinedTextField(
                                    value = versionCode.toString(),
                                    onValueChange = { versionCode = it.toIntOrNull() ?: 1 },
                                    label = { Text("Version Code") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CorporateBlue,
                                        unfocusedBorderColor = DarkCardBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                item {
                    // App Sector Symbol Picker
                    GlassCard(backgroundColor = DarkSurface) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(text = "Application Icon Classification", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            val symbols = listOf(
                                Pair("web", "Web Portal"),
                                Pair("business", "Enterprise"),
                                Pair("store", "E-Commerce"),
                                Pair("cloud", "SaaS Cloud"),
                                Pair("dashboard", "Analytics"),
                                Pair("code", "Developer"),
                                Pair("article", "Media"),
                                Pair("mobile", "Mobile")
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for ((key, label) in symbols) {
                                    val isSelected = selectedCategorySymbol == key
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) CorporateBlue.copy(alpha = 0.15f) else DarkSurfaceVariant)
                                            .border(BorderStroke(1.dp, if (isSelected) CorporateBlue else DarkCardBorder), RoundedCornerShape(8.dp))
                                            .clickable { selectedCategorySymbol = key }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            ProjectIconBadge(symbolName = key, sizeDp = 32.dp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = label, color = if (isSelected) CorporateBlue else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // Corporate Palette
                    GlassCard(backgroundColor = DarkSurface) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(text = "Brand Accent Color Theme", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            val presets = listOf(
                                Pair("#FF9933", "#38BDF8"), // Kesari Saffron + Ashoka Cyan
                                Pair("#1A56DB", "#38BDF8"), // Royal Ashoka Blue
                                Pair("#138808", "#10B981"), // Tiranga Emerald
                                Pair("#F59E0B", "#FF9933"), // Imperial Gold
                                Pair("#6366F1", "#38BDF8")  // Cyber Indigo
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for ((p, a) in presets) {
                                    val isSelected = primaryColor == p
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(p)))
                                            .border(BorderStroke(2.dp, if (isSelected) Color.White else Color.Transparent), CircleShape)
                                            .clickable {
                                                primaryColor = p
                                                accentColor = a
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Tab 1: Navigation
                item {
                    GlassCard(backgroundColor = DarkSurface) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = "Navigation Architecture", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            for (style in NavStyle.values()) {
                                val isSelected = navStyle == style
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) DarkSurfaceVariant else Color.Transparent)
                                        .clickable { navStyle = style }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) CorporateBlue else DarkSurfaceVariant)
                                            .border(BorderStroke(1.dp, if (isSelected) CorporateBlue else TextMuted), CircleShape)
                                    )
                                    Text(text = style.label, color = if (isSelected) TextPrimary else TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                item {
                    // Navigation Items List
                    GlassCard(backgroundColor = DarkSurface) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Bottom Navigation Tabs (${navItems.size})", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                SecondaryOutlineButton(
                                    text = "Add Tab",
                                    icon = Icons.Default.Add,
                                    onClick = {
                                        navItems.add(NavItem("Page ${navItems.size + 1}", currentProject.url, "Home"))
                                    }
                                )
                            }

                            navItems.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurfaceVariant)
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = item.title,
                                        onValueChange = { newTitle ->
                                            navItems[index] = item.copy(title = newTitle)
                                        },
                                        label = { Text("Title") },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CorporateBlue,
                                            unfocusedBorderColor = DarkCardBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = item.url,
                                        onValueChange = { newUrl ->
                                            navItems[index] = item.copy(url = newUrl)
                                        },
                                        label = { Text("URL") },
                                        modifier = Modifier.weight(1.5f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CorporateBlue,
                                            unfocusedBorderColor = DarkCardBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        singleLine = true
                                    )
                                    IconButton(
                                        onClick = {
                                            if (navItems.size > 1) navItems.removeAt(index)
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Tab 2: Functionality Modules & Permissions
                item {
                    GlassCard(backgroundColor = DarkSurface) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = "Android Native Modules & Capabilities", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                            FeatureSwitchRow("Pull to Refresh", "Swipe down to reload web view contents", pullToRefresh) { pullToRefresh = it }
                            FeatureSwitchRow("Offline Screen Resilience", "Display custom error page with retry on disconnect", offlineScreen) { offlineScreen = it }
                            FeatureSwitchRow("HTML5 File Uploads", "Android Photo Picker and file selection integration", fileUploads) { fileUploads = it }
                            FeatureSwitchRow("File Downloads", "Download manager integration for PDF and binary assets", fileDownloads) { fileDownloads = it }
                            FeatureSwitchRow("Camera & Media Permissions", "Request camera runtime permission when required", cameraAccess) { cameraAccess = it }
                            FeatureSwitchRow("Push Notifications", "Firebase Cloud Messaging and web notification support", pushNotifications) { pushNotifications = it }
                            FeatureSwitchRow("Deep Links", "Handle custom URL schemes and intent filters", deepLinks) { deepLinks = it }
                        }
                    }
                }
            }

            3 -> {
                // Tab 3: Splash Screen
                item {
                    GlassCard(backgroundColor = DarkSurface) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = "Native Android Splash Screen", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            OutlinedTextField(
                                value = splashTitle,
                                onValueChange = { splashTitle = it },
                                label = { Text("Splash Title") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CorporateBlue,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            OutlinedTextField(
                                value = splashSubtitle,
                                onValueChange = { splashSubtitle = it },
                                label = { Text("Splash Subtitle") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CorporateBlue,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Text(
                                text = "Display Duration: ${String.format("%.1f", splashDelaySeconds)} seconds",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Slider(
                                value = splashDelaySeconds,
                                onValueChange = { splashDelaySeconds = it },
                                valueRange = 1f..5f,
                                colors = SliderDefaults.colors(
                                    thumbColor = CorporateBlue,
                                    activeTrackColor = CorporateBlue,
                                    inactiveTrackColor = DarkCardBorder
                                )
                            )
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun FeatureSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CorporateBlue,
                checkedTrackColor = CorporateBlue.copy(alpha = 0.3f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkSurfaceVariant
            )
        )
    }
}
