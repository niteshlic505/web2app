package com.example.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AppRepository
import com.example.ui.components.AshokaChakraHUD
import com.example.ui.components.BharatFuturisticBackground
import com.example.ui.components.TricolourHeaderBar
import com.example.ui.screens.BuildCenterScreen
import com.example.ui.screens.CreateAppScreen
import com.example.ui.screens.CustomizationStudioScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LivePreviewScreen
import com.example.ui.screens.MyAppsScreen
import com.example.ui.screens.PlayPublishingScreen
import com.example.ui.screens.ProfilePricingScreen
import com.example.ui.theme.AshokaCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.IndianSaffron
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

sealed class Screen {
    object Main : Screen()
    data class LivePreview(val projectId: String) : Screen()
    data class CustomizationStudio(val projectId: String) : Screen()
    data class PlayPublishing(val projectId: String) : Screen()
}

enum class MainTab(val title: String, val activeIcon: ImageVector, val inactiveIcon: ImageVector) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    CREATE("Create", Icons.Filled.AddCircle, Icons.Filled.AddCircle),
    MY_APPS("Apps", Icons.Filled.Layers, Icons.Outlined.Layers),
    BUILD_CENTRE("Build", Icons.Filled.Build, Icons.Outlined.Build),
    PROFILE("Studio", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun MainApp(
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
    var selectedTab by remember { mutableStateOf(MainTab.HOME) }
    var targetInitialUrlForCreate by remember { mutableStateOf<String?>(null) }
    var targetBuildProjectId by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = currentScreen !is Screen.Main) {
        currentScreen = Screen.Main
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            if (currentScreen is Screen.Main && !isTablet) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DarkCardBorder)
                    )
                    NavigationBar(
                        containerColor = DarkSurfaceElevated.copy(alpha = 0.96f),
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        MainTab.values().forEach { tab ->
                            val isSelected = selectedTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (tab == MainTab.CREATE) {
                                        targetInitialUrlForCreate = null
                                    }
                                    selectedTab = tab
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(if (tab == MainTab.CREATE) 26.dp else 22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = IndianSaffron,
                                    selectedTextColor = IndianSaffron,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = IndianSaffron.copy(alpha = 0.18f)
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        BharatFuturisticBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Executive Futuristic Indian Tricolour Laser Ribbon at Top
                TricolourHeaderBar()

                Row(modifier = Modifier.fillMaxSize()) {
                    // If Tablet, render executive Navigation Rail on the left
                    if (currentScreen is Screen.Main && isTablet) {
                        NavigationRail(
                            containerColor = DarkSurfaceElevated.copy(alpha = 0.94f),
                            contentColor = TextPrimary,
                            modifier = Modifier
                                .fillMaxHeight()
                                .border(width = 0.5.dp, color = DarkCardBorder),
                            header = {
                                Spacer(modifier = Modifier.height(10.dp))
                                AshokaChakraHUD(
                                    sizeDp = 32.dp,
                                    primaryRingColor = AshokaCyan,
                                    accentColor = IndianSaffron,
                                    animate = true
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "BHARAT",
                                    color = IndianSaffron,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        ) {
                            Spacer(modifier = Modifier.height(14.dp))
                            MainTab.values().forEach { tab ->
                                val isSelected = selectedTab == tab
                                NavigationRailItem(
                                    selected = isSelected,
                                    onClick = {
                                        if (tab == MainTab.CREATE) {
                                            targetInitialUrlForCreate = null
                                        }
                                        selectedTab = tab
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                            contentDescription = tab.title,
                                            modifier = Modifier.size(23.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = IndianSaffron,
                                        selectedTextColor = IndianSaffron,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                        indicatorColor = IndianSaffron.copy(alpha = 0.18f)
                                    )
                                )
                            }
                        }
                    }

                    // Main Content Screen
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                        when (screen) {
                            is Screen.Main -> {
                                when (selectedTab) {
                                    MainTab.HOME -> {
                                        HomeScreen(
                                            repository = repository,
                                            onCreateAppClick = { templateTargetUrl ->
                                                targetInitialUrlForCreate = templateTargetUrl
                                                selectedTab = MainTab.CREATE
                                            },
                                            onOpenPreview = { projId ->
                                                currentScreen = Screen.LivePreview(projId)
                                            },
                                            onOpenCustomize = { projId ->
                                                currentScreen = Screen.CustomizationStudio(projId)
                                            },
                                            onOpenBuild = { projId ->
                                                targetBuildProjectId = projId
                                                selectedTab = MainTab.BUILD_CENTRE
                                            }
                                        )
                                    }
                                    MainTab.CREATE -> {
                                        CreateAppScreen(
                                            repository = repository,
                                            initialUrl = targetInitialUrlForCreate,
                                            onAppCreated = { newProjId ->
                                                currentScreen = Screen.LivePreview(newProjId)
                                            }
                                        )
                                    }
                                    MainTab.MY_APPS -> {
                                        MyAppsScreen(
                                            repository = repository,
                                            onCreateAppClick = {
                                                targetInitialUrlForCreate = null
                                                selectedTab = MainTab.CREATE
                                            },
                                            onOpenPreview = { projId ->
                                                currentScreen = Screen.LivePreview(projId)
                                            },
                                            onOpenCustomize = { projId ->
                                                currentScreen = Screen.CustomizationStudio(projId)
                                            },
                                            onOpenBuild = { projId ->
                                                targetBuildProjectId = projId
                                                selectedTab = MainTab.BUILD_CENTRE
                                            }
                                        )
                                    }
                                    MainTab.BUILD_CENTRE -> {
                                        BuildCenterScreen(
                                            initialProjectId = targetBuildProjectId,
                                            repository = repository
                                        )
                                    }
                                    MainTab.PROFILE -> {
                                        ProfilePricingScreen(repository = repository)
                                    }
                                }
                            }
                            is Screen.LivePreview -> {
                                LivePreviewScreen(
                                    projectId = screen.projectId,
                                    repository = repository,
                                    onNavigateBack = { currentScreen = Screen.Main },
                                    onOpenCustomize = { projId ->
                                        currentScreen = Screen.CustomizationStudio(projId)
                                    },
                                    onOpenBuild = { projId ->
                                        targetBuildProjectId = projId
                                        selectedTab = MainTab.BUILD_CENTRE
                                        currentScreen = Screen.Main
                                    }
                                )
                            }
                            is Screen.CustomizationStudio -> {
                                CustomizationStudioScreen(
                                    projectId = screen.projectId,
                                    repository = repository,
                                    onNavigateBack = { currentScreen = Screen.Main },
                                    onOpenPreview = { projId ->
                                        currentScreen = Screen.LivePreview(projId)
                                    },
                                    onOpenBuild = { projId ->
                                        targetBuildProjectId = projId
                                        selectedTab = MainTab.BUILD_CENTRE
                                        currentScreen = Screen.Main
                                    }
                                )
                            }
                            is Screen.PlayPublishing -> {
                                PlayPublishingScreen(
                                    projectId = screen.projectId,
                                    repository = repository,
                                    onNavigateBack = { currentScreen = Screen.Main }
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
