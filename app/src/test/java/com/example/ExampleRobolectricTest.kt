package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppProject
import com.example.data.model.ConversionMode
import com.example.engine.AutomatedTestingEngine
import com.example.engine.WebsiteAnalysisEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Website2App Studio", appName)
    }

    @Test
    fun `test website domain extraction and package naming`() {
        val domain = WebsiteAnalysisEngine.extractDomain("https://shop.mybrand.in/products")
        assertEquals("shop.mybrand.in", domain)

        val pkg = WebsiteAnalysisEngine.generatePackageName("mybrand.com")
        assertEquals("com.mybrand.app", pkg)
    }

    @Test
    fun `test automated audit engine evaluation`() = runBlocking {
        val project = AppProject(
            id = "test_proj_1",
            name = "Test Web App",
            url = "https://example.com",
            packageName = "com.example.app",
            conversionMode = ConversionMode.SMART_WEBVIEW
        )

        val audit = AutomatedTestingEngine.runFullAudit(project)
        assertNotNull(audit)
        assertTrue(audit.scorePercentage >= 80)
        assertEquals("A+", audit.overallGrade)
    }

    @Test
    fun `test device hardware detection and tablet compatibility`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val hardware = com.example.engine.BuildPipelineEngine.detectCurrentDevice(context)
        assertNotNull(hardware)
        assertNotNull(hardware.classification)
        assertTrue(hardware.smallestScreenWidthDp > 0)

        val telemetry = com.example.engine.DeviceTelemetryTracker.getDeviceTelemetry(context)
        assertNotNull(telemetry)
        assertNotNull(telemetry.category)
        assertNotNull(telemetry.tabletCompatibilitySummary)
        assertTrue(telemetry.smallestScreenWidthDp >= 0)
    }

    @Test
    fun `test project version code and target profiles`() {
        val project = AppProject(
            id = "test_v10",
            name = "Enterprise Bharat Test",
            url = "https://gov.in",
            packageName = "in.gov.portal",
            versionCode = 10,
            versionName = "10.0",
            targetDeviceProfile = com.example.data.model.TargetDeviceProfile.AUTO_DETECT
        )
        assertEquals(10, project.versionCode)
        assertEquals("10.0", project.versionName)
        assertEquals(com.example.data.model.TargetDeviceProfile.AUTO_DETECT, project.targetDeviceProfile)
    }

    @Test
    fun `test standalone apk builder generates valid signed apk with custom metadata`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val project = AppProject(
            id = "test_bazaar",
            name = "Bharat Bazaar",
            url = "https://bharatbazaar.in",
            packageName = "in.bharatbazaar.app",
            versionCode = 10,
            versionName = "10.0",
            targetDeviceProfile = com.example.data.model.TargetDeviceProfile.TABLET_STANDARD
        )
        val outApk = java.io.File(context.cacheDir, "bharat_bazaar_test.apk")
        if (outApk.exists()) outApk.delete()

        val bytesWritten = com.example.engine.StandaloneApkBuilder.buildStandaloneApk(
            context = context,
            project = project,
            targetProfile = com.example.data.model.TargetDeviceProfile.TABLET_STANDARD,
            outputFile = outApk
        )
        assertTrue(outApk.exists())
        assertTrue(bytesWritten > 10_000L)

        // Verify APK contains injected webapp_config.json and patched AndroidManifest.xml
        val entries = mutableSetOf<String>()
        var configContent = ""
        java.util.zip.ZipInputStream(outApk.inputStream()).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                entries.add(e.name)
                if (e.name == "assets/webapp_config.json") {
                    configContent = zis.readBytes().toString(Charsets.UTF_8)
                }
                zis.closeEntry()
                e = zis.nextEntry
            }
        }
        assertTrue(entries.contains("AndroidManifest.xml"))
        assertTrue(entries.contains("classes.dex"))
        assertTrue(entries.contains("resources.arsc"))
        assertTrue(entries.contains("assets/webapp_config.json"))
        val parsedConfig = org.json.JSONObject(configContent)
        assertEquals("https://bharatbazaar.in", parsedConfig.getString("url"))
        assertEquals("Bharat Bazaar", parsedConfig.getString("appName"))

        // Verify with official Android SDK tools (aapt2, zipalign, apksigner)
        val sdkRoot = System.getenv("ANDROID_SDK_ROOT")
        if (!sdkRoot.isNullOrBlank()) {
            val btDir = java.io.File(sdkRoot, "build-tools").listFiles()?.maxByOrNull { it.name }
            if (btDir != null) {
                val aapt2 = java.io.File(btDir, "aapt2").absolutePath
                val badgingProc = ProcessBuilder(aapt2, "dump", "badging", outApk.absolutePath)
                    .redirectErrorStream(true).start()
                val badgingOut = badgingProc.inputStream.bufferedReader().readText()
                assertEquals("aapt2 dump badging failed: $badgingOut", 0, badgingProc.waitFor())
                assertTrue("Expected custom package name in badging: $badgingOut", badgingOut.contains("name='in.bharatbazaar.app'"))
                assertTrue("Expected versionCode 10 in badging: $badgingOut", badgingOut.contains("versionCode='10'"))
                assertTrue("Expected versionName 10.0 in badging: $badgingOut", badgingOut.contains("versionName='10.0'"))
                assertTrue("Expected custom label in badging: $badgingOut", badgingOut.contains("application-label:'Bharat Bazaar'"))

                val zipalign = java.io.File(btDir, "zipalign").absolutePath
                val alignProc = ProcessBuilder(zipalign, "-c", "-p", "4", outApk.absolutePath)
                    .redirectErrorStream(true).start()
                val alignOut = alignProc.inputStream.bufferedReader().readText()
                assertEquals("zipalign check failed: $alignOut", 0, alignProc.waitFor())

                val apksigner = java.io.File(btDir, "apksigner").absolutePath
                val sigProc = ProcessBuilder(apksigner, "verify", "--verbose", outApk.absolutePath)
                    .redirectErrorStream(true).start()
                val sigOut = sigProc.inputStream.bufferedReader().readText()
                assertEquals("apksigner verify failed: $sigOut", 0, sigProc.waitFor())
                assertTrue("Expected v2 verification in apksigner output: $sigOut", sigOut.contains("Verified using v2 scheme (APK Signature Scheme v2): true"))
            }
        }
    }

    @Test
    fun `test google play billing product ids and plan persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("website2app_billing_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()

        assertEquals(null, com.example.data.model.UserPlan.FREE.playProductId)
        assertEquals("website2app_pro_monthly", com.example.data.model.UserPlan.PRO.playProductId)
        assertEquals(
            com.example.data.model.UserPlan.BUSINESS,
            com.example.data.model.UserPlan.fromPlayProductId("website2app_business_monthly")
        )
        assertEquals(
            com.example.data.model.UserPlan.AGENCY,
            com.example.data.model.UserPlan.fromPlayProductId("website2app_agency_monthly")
        )

        assertEquals("Monthly", com.example.billing.PlayBillingManager.formatBillingPeriod("P1M"))
        assertEquals("Annual", com.example.billing.PlayBillingManager.formatBillingPeriod("P1Y"))
        assertEquals("OK", com.example.billing.PlayBillingManager.responseCodeName(0))

        val repo = com.example.data.repository.AppRepository(context)
        assertEquals(com.example.data.model.UserPlan.FREE, repo.currentPlan.value)

        repo.recordPlayPurchase(
            plan = com.example.data.model.UserPlan.PRO,
            productId = "website2app_pro_monthly",
            orderId = "GPA.3312-4455-6677-88990",
            purchaseToken = "tok_test_pro_123",
            purchaseTimeMs = 1700000000000L
        )
        assertEquals(com.example.data.model.UserPlan.PRO, repo.currentPlan.value)
        assertEquals("GPA.3312-4455-6677-88990", repo.entitlementMetadata.value.orderId)

        // Verify persistence across repository re-instantiation
        val repo2 = com.example.data.repository.AppRepository(context)
        assertEquals(com.example.data.model.UserPlan.PRO, repo2.currentPlan.value)
        assertEquals("website2app_pro_monthly", repo2.entitlementMetadata.value.productId)

        repo2.revertToFreeIfPlayEntitlementExpired()
        assertEquals(com.example.data.model.UserPlan.FREE, repo2.currentPlan.value)
    }
}
