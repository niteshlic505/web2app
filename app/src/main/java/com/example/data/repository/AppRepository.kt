package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.AppProject
import com.example.data.model.BuildRecord
import com.example.data.model.SigningKey
import com.example.data.model.UserPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.util.UUID

data class PlayEntitlementMetadata(
    val plan: UserPlan = UserPlan.FREE,
    val productId: String? = null,
    val orderId: String? = null,
    val purchaseToken: String? = null,
    val purchasedAtMs: Long = 0L
)

class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val billingPrefs = appContext.getSharedPreferences("website2app_billing_prefs", Context.MODE_PRIVATE)
    private val db = AppDatabase.getInstance(appContext)
    private val projectDao = db.projectDao()
    private val buildDao = db.buildDao()

    val allProjects: Flow<List<AppProject>> = projectDao.getAllProjects()
    val allBuilds: Flow<List<BuildRecord>> = buildDao.getAllBuilds()

    private val _currentPlan = MutableStateFlow(loadPersistedPlan())
    val currentPlan: StateFlow<UserPlan> = _currentPlan.asStateFlow()

    private val _entitlementMetadata = MutableStateFlow(loadPersistedEntitlement())
    val entitlementMetadata: StateFlow<PlayEntitlementMetadata> = _entitlementMetadata.asStateFlow()

    private val _signingKeys = MutableStateFlow<List<SigningKey>>(emptyList())
    val signingKeys: StateFlow<List<SigningKey>> = _signingKeys.asStateFlow()

    init {
        initDefaultKeys()
    }

    private fun loadPersistedPlan(): UserPlan {
        val savedName = billingPrefs.getString(KEY_ACTIVE_PLAN, UserPlan.FREE.name)
        return runCatching { UserPlan.valueOf(savedName ?: UserPlan.FREE.name) }.getOrDefault(UserPlan.FREE)
    }

    private fun loadPersistedEntitlement(): PlayEntitlementMetadata {
        return PlayEntitlementMetadata(
            plan = loadPersistedPlan(),
            productId = billingPrefs.getString(KEY_PLAY_PRODUCT_ID, null),
            orderId = billingPrefs.getString(KEY_PLAY_ORDER_ID, null),
            purchaseToken = billingPrefs.getString(KEY_PLAY_PURCHASE_TOKEN, null),
            purchasedAtMs = billingPrefs.getLong(KEY_PLAY_PURCHASED_AT, 0L)
        )
    }

    private fun initDefaultKeys() {
        val defaultKey = SigningKey(
            id = "key_prod_default",
            alias = "production-release-key",
            validityYears = 25,
            organization = "Website2App Enterprise Studio",
            sha1 = "F4:89:C2:5E:11:78:A9:63:2B:80:CC:54:19:D4:33:AA:08:92:E1:9F",
            sha256 = "6B:22:98:81:4A:E3:FA:31:0D:18:7E:6C:5F:AA:C9:83:B1:DF:72:00:81:2F:56:4C:19:5D:87:AE:33:10:9B:EE",
            isDefault = true
        )
        val stagingKey = SigningKey(
            id = "key_staging",
            alias = "internal-alpha-key",
            validityYears = 10,
            organization = "Quality Assurance & Internal Track",
            sha1 = "2A:1B:3C:4D:5E:6F:70:81:92:A3:B4:C5:D6:E7:F8:09:1A:2B:3C:4D",
            sha256 = "3F:A8:19:B2:C3:D4:E5:F6:07:18:29:3A:4B:5C:6D:7E:8F:90:A1:B2:C3:D4:E5:F6:07:18:29:3A:4B:5C:6D:7E",
            isDefault = false
        )
        _signingKeys.value = listOf(defaultKey, stagingKey)
    }

    suspend fun createSigningKey(alias: String, organization: String, validityYears: Int): SigningKey {
        val rawSeed = "$alias-$organization-${System.currentTimeMillis()}"
        val sha1Digest = MessageDigest.getInstance("SHA-1").digest(rawSeed.toByteArray())
        val sha256Digest = MessageDigest.getInstance("SHA-256").digest(rawSeed.toByteArray())

        val sha1 = sha1Digest.joinToString(":") { "%02X".format(it) }
        val sha256 = sha256Digest.joinToString(":") { "%02X".format(it) }

        val newKey = SigningKey(
            id = "key_${UUID.randomUUID()}",
            alias = alias.trim().ifEmpty { "release-key-${System.currentTimeMillis()}" },
            validityYears = validityYears,
            organization = organization.trim().ifEmpty { "Enterprise Studio" },
            sha1 = sha1,
            sha256 = sha256,
            isDefault = false
        )
        _signingKeys.value = _signingKeys.value + newKey
        return newKey
    }

    fun updatePlan(plan: UserPlan) {
        billingPrefs.edit()
            .putString(KEY_ACTIVE_PLAN, plan.name)
            .apply()
        _currentPlan.value = plan
        _entitlementMetadata.value = _entitlementMetadata.value.copy(plan = plan)
        com.example.data.auth.FirebaseAuthManager.syncPlanEntitlement(
            context = appContext,
            planTitle = plan.title,
            playProductId = plan.playProductId,
            playOrderId = _entitlementMetadata.value.orderId
        )
    }

    fun recordPlayPurchase(
        plan: UserPlan,
        productId: String,
        orderId: String?,
        purchaseToken: String,
        purchaseTimeMs: Long
    ) {
        billingPrefs.edit()
            .putString(KEY_ACTIVE_PLAN, plan.name)
            .putString(KEY_PLAY_PRODUCT_ID, productId)
            .putString(KEY_PLAY_ORDER_ID, orderId ?: "")
            .putString(KEY_PLAY_PURCHASE_TOKEN, purchaseToken)
            .putLong(KEY_PLAY_PURCHASED_AT, purchaseTimeMs)
            .apply()

        _currentPlan.value = plan
        _entitlementMetadata.value = PlayEntitlementMetadata(
            plan = plan,
            productId = productId,
            orderId = orderId,
            purchaseToken = purchaseToken,
            purchasedAtMs = purchaseTimeMs
        )
        com.example.data.auth.FirebaseAuthManager.syncPlanEntitlement(
            context = appContext,
            planTitle = plan.title,
            playProductId = productId,
            playOrderId = orderId
        )
    }

    fun revertToFreeIfPlayEntitlementExpired() {
        val hadPlayToken = !billingPrefs.getString(KEY_PLAY_PURCHASE_TOKEN, null).isNullOrBlank()
        if (!hadPlayToken) return

        billingPrefs.edit()
            .putString(KEY_ACTIVE_PLAN, UserPlan.FREE.name)
            .remove(KEY_PLAY_PRODUCT_ID)
            .remove(KEY_PLAY_ORDER_ID)
            .remove(KEY_PLAY_PURCHASE_TOKEN)
            .remove(KEY_PLAY_PURCHASED_AT)
            .apply()

        _currentPlan.value = UserPlan.FREE
        _entitlementMetadata.value = PlayEntitlementMetadata(plan = UserPlan.FREE)
        com.example.data.auth.FirebaseAuthManager.syncPlanEntitlement(
            context = appContext,
            planTitle = UserPlan.FREE.title,
            playProductId = null,
            playOrderId = null
        )
    }

    companion object {
        private const val KEY_ACTIVE_PLAN = "active_plan"
        private const val KEY_PLAY_PRODUCT_ID = "play_product_id"
        private const val KEY_PLAY_ORDER_ID = "play_order_id"
        private const val KEY_PLAY_PURCHASE_TOKEN = "play_purchase_token"
        private const val KEY_PLAY_PURCHASED_AT = "play_purchased_at"
    }

    suspend fun getProject(id: String): AppProject? = projectDao.getProjectById(id)

    suspend fun saveProject(project: AppProject) {
        projectDao.insertProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProject(id: String) {
        projectDao.deleteProjectById(id)
    }

    suspend fun duplicateProject(original: AppProject): AppProject {
        val copy = original.copy(
            id = UUID.randomUUID().toString(),
            name = "${original.name} (Copy)",
            packageName = "${original.packageName}.copy",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        projectDao.insertProject(copy)
        return copy
    }

    suspend fun saveBuild(record: BuildRecord) {
        buildDao.insertBuild(record)
    }

    suspend fun deleteBuild(id: String) {
        buildDao.deleteBuildById(id)
    }
}
