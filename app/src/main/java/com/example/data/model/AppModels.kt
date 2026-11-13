package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TargetDeviceProfile(val label: String, val badge: String, val description: String) {
    AUTO_DETECT(
        "Auto-Detect Live Hardware (Smart)",
        "Auto-Adaptive",
        "Dynamically tracks your device form factor (Tablet, Phone, Foldable, DeX) and customizes the build."
    ),
    TABLET_STANDARD(
        "Standard Tablet (10\"-11\" Widescreen)",
        "Tablet 10\"-11\"",
        "Optimized for mainstream 10\"-11\" tablets (Lenovo Tab, Samsung Galaxy Tab, Xiaomi Pad) with Navigation Rail."
    ),
    TABLET_PRO(
        "Pro Tablet / 2-in-1 (12\"-14\" & DeX)",
        "Tablet Pro 12\"+",
        "Desktop-class split-pane layout, keyboard/mouse cursor feedback, and multi-window resizeableActivity."
    ),
    TABLET_COMPACT(
        "Compact Tablet (7\"-8.4\")",
        "Tablet 7\"-8\"",
        "Optimized for 7\"-8.4\" compact tablets, e-readers, and dual-mode handhelds."
    ),
    PHONE_COMPACT(
        "Compact Smartphone (5.0\"-6.1\")",
        "Phone Compact",
        "One-hand ergonomic touch, portrait navigation, and mobile-optimized viewport."
    ),
    PHONE_LARGE(
        "Large Smartphone / Pro Max (6.7\")",
        "Phone Pro Max",
        "High-density phablet display with immersive full-bleed content."
    ),
    FOLDABLE(
        "Foldable Dual-Screen (Book / Flip)",
        "Foldable",
        "Dynamic layout responding seamlessly to cover screen vs unfolded tablet inner display."
    ),
    UNIVERSAL(
        "Universal Responsive Hybrid",
        "Universal All-Device",
        "Cross-platform responsive package that seamlessly auto-adapts to all tablets, phones, and desktops."
    ),
    // Backward compatibility aliases
    TABLET(
        "Tablet & Large Display",
        "Tablet 600dp+",
        "Optimized for 7\"-13\" Android Tablets with widescreen viewport and multi-window."
    ),
    PHONE(
        "Mobile Smartphone",
        "Mobile Phone",
        "Tailored for handheld smartphones with mobile User-Agent."
    )
}

enum class ConversionMode(val label: String, val badge: String, val description: String) {
    SMART_WEBVIEW(
        "Smart WebView Architecture",
        "Enterprise WebKit",
        "Optimized WebKit runtime with native splash, action bar, background caching, and secure JavaScript bridge."
    ),
    PWA(
        "Progressive Web App (PWA)",
        "PWA Native",
        "Preserves service workers, web manifests, offline cache storage, and system application shortcuts."
    ),
    AI_HYBRID(
        "Native Hybrid Architecture",
        "Native Hybrid",
        "Integrates web content into native Jetpack Compose scaffolds with native bottom navigation, share sheets, and dialogs."
    )
}

enum class NavStyle(val label: String) {
    BOTTOM_NAV("Native Bottom Navigation Bar"),
    DRAWER("Enterprise Navigation Drawer"),
    FLOATING_PILL("Floating Quick Navigation"),
    NONE("Clean Fullscreen Viewport")
}

enum class BuildType(val label: String, val extension: String, val description: String) {
    DEBUG_APK("Development Debug APK", ".apk", "Unsigned development package for rapid on-device testing"),
    RELEASE_APK("Production Release APK", ".apk", "Optimized, signed standalone APK ready for distribution"),
    RELEASE_AAB("Android App Bundle (AAB)", ".aab", "Standard format required for Google Play Console submissions"),
    SIGNED_AAB("Signed Enterprise AAB", ".aab", "Signed with verified JKS keystore for Google Play production tracks"),
    PROJECT_ZIP("Android Studio Project Source", ".zip", "Complete compilable Gradle Kotlin project with full source code")
}

enum class BuildStatus(val label: String, val isTerminal: Boolean) {
    QUEUED("Queued in Worker Pool", false),
    ANALYZING("Validating Manifest & Hardware Profile", false),
    COMPILING_RESOURCES("AAPT2 Resource Compilation", false),
    BUILDING_DEX("Kotlinc & D8 DEX Bytecode Processing", false),
    OPTIMIZING("ProGuard/R8 Dead Code Elimination", false),
    SIGNING("V2/V3 APK Scheme Keystore Signing", false),
    SUCCESS("Build Completed", true),
    FAILED("Build Error", true)
}

enum class PublishTrack(val label: String, val badge: String) {
    INTERNAL("Internal Testing Track", "Fastest Approval"),
    CLOSED("Closed Alpha / Beta Track", "Authorized Testers"),
    OPEN("Open Public Testing Track", "Store Index Active"),
    PRODUCTION("Production Release Track", "Public Deployment")
}

enum class UserPlan(
    val title: String,
    val priceInr: Int,
    val priceUsd: Int,
    val maxProjects: Int,
    val aabExport: Boolean,
    val playProductId: String?
) {
    FREE("Bharat Community", 0, 0, 5, true, null),
    PRO("Professional Studio", 999, 12, 25, true, "website2app_pro_monthly"),
    BUSINESS("Enterprise Business", 2499, 29, 100, true, "website2app_business_monthly"),
    AGENCY("Bharat Enterprise Unlimited", 6999, 85, 9999, true, "website2app_agency_monthly");

    companion object {
        fun fromPlayProductId(productId: String): UserPlan? =
            entries.firstOrNull { it.playProductId == productId }
    }
}

@Entity(tableName = "app_projects")
data class AppProject(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val packageName: String,
    val versionCode: Int = 10,
    val versionName: String = "10.0",
    val conversionMode: ConversionMode = ConversionMode.SMART_WEBVIEW,
    val targetDeviceProfile: TargetDeviceProfile = TargetDeviceProfile.AUTO_DETECT,
    val enableTabletDualPane: Boolean = true,
    val enableDesktopModeOnTablet: Boolean = false,
    val primaryColorHex: String = "#FF9933",
    val accentColorHex: String = "#1A56DB",
    val backgroundColorHex: String = "#040B18",
    val navStyle: NavStyle = NavStyle.BOTTOM_NAV,
    val navItemsJson: String = "[]",
    val featuresJson: String = "{}",
    val websiteAnalysisJson: String = "{}",
    val iconEmoji: String = "web",
    val splashTitle: String = "",
    val splashSubtitle: String = "Powered by Website2App Studio",
    val splashDelayMs: Long = 1800L,
    val targetSdk: Int = 36,
    val minSdk: Int = 24,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "build_records")
data class BuildRecord(
    @PrimaryKey val id: String,
    val projectId: String,
    val projectName: String,
    val buildType: BuildType,
    val status: BuildStatus,
    val progress: Int = 0,
    val fileSizeBytes: Long = 0L,
    val fileName: String = "",
    val downloadPath: String = "",
    val targetDeviceLabel: String = "Adaptive (Tablet & Phone)",
    val logs: String = "",
    val keystoreAlias: String = "production-release-key",
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

data class NavItem(
    val title: String,
    val url: String,
    val iconName: String
)

data class AppFeaturesConfig(
    val pullToRefresh: Boolean = true,
    val offlineCacheScreen: Boolean = true,
    val fileUploads: Boolean = true,
    val fileDownloads: Boolean = true,
    val cameraAccess: Boolean = false,
    val locationAccess: Boolean = false,
    val pushNotifications: Boolean = true,
    val deepLinks: Boolean = true,
    val externalBrowserFallback: Boolean = true,
    val zoomControls: Boolean = true
)

data class WebsiteAnalysisResult(
    val url: String,
    val title: String = "",
    val metaDescription: String = "",
    val faviconUrl: String? = null,
    val isHttps: Boolean = true,
    val isMobileResponsive: Boolean = true,
    val pwaManifestFound: Boolean = false,
    val serviceWorkerDetected: Boolean = false,
    val authDetected: Boolean = false,
    val ecommerceDetected: Boolean = false,
    val formsCount: Int = 0,
    val detectedPages: List<NavItem> = emptyList(),
    val techStack: List<String> = emptyList(),
    val readinessScore: Int = 94,
    val recommendedMode: ConversionMode = ConversionMode.SMART_WEBVIEW
)

data class AuditCheckItem(
    val title: String,
    val category: String,
    val passed: Boolean,
    val detail: String,
    val severity: String = "INFO"
)

data class AutomatedAuditReport(
    val projectId: String,
    val overallGrade: String,
    val scorePercentage: Int,
    val checks: List<AuditCheckItem>,
    val timestamp: Long = System.currentTimeMillis()
)

data class SigningKey(
    val id: String,
    val alias: String,
    val validityYears: Int,
    val organization: String,
    val sha1: String,
    val sha256: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class PublishListing(
    val projectId: String,
    val appTitle: String,
    val shortDescription: String,
    val fullDescription: String,
    val privacyPolicyUrl: String,
    val contactEmail: String,
    val category: String,
    val targetTrack: PublishTrack = PublishTrack.INTERNAL,
    val isReleaseNotesReady: Boolean = true,
    val isScreenshotsUploaded: Boolean = true,
    val isFeatureGraphicUploaded: Boolean = true,
    val isDataSafetyCompleted: Boolean = true,
    val targetAabRecordId: String = ""
)
