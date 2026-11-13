package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.engine.DeviceTelemetryTracker
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

data class UserSessionProfile(
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val phoneNumber: String? = null,
    val photoUrl: String? = null,
    val plan: String = "Bharat Community",
    val currentDevice: String = "",
    val deviceCategory: String = "",
    val isVerified: Boolean = true
)

object FirebaseAuthManager {

    private const val TAG = "FirebaseAuthManager"

    private val auth: FirebaseAuth get() = Firebase.auth

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _sessionProfile = MutableStateFlow<UserSessionProfile?>(null)
    val sessionProfile: StateFlow<UserSessionProfile?> = _sessionProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    fun initialize(context: Context) {
        val user = auth.currentUser
        _currentUser.value = user
        if (user != null) {
            syncUserProfileToFirestore(context, user)
        }
    }

    /**
     * Interactive Google Sign-In via Jetpack CredentialManager using GetSignInWithGoogleOption.
     */
    fun signInWithGoogle(
        activity: Activity,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val clientId = runCatching {
            activity.getString(R.string.default_web_client_id)
        }.getOrNull()

        if (clientId.isNullOrBlank()) {
            val msg = "Google Client ID configuration pending in google-services.json"
            _authError.value = msg
            onError(msg)
            return
        }

        _isLoading.value = true
        _authError.value = null

        val credentialManager = CredentialManager.create(activity)
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = authResult.user

                    if (user != null) {
                        _currentUser.value = user
                        syncUserProfileToFirestore(activity, user)
                        launch(Dispatchers.Main) {
                            _isLoading.value = false
                            onSuccess(user)
                        }
                    } else {
                        launch(Dispatchers.Main) {
                            _isLoading.value = false
                            onError("Failed to obtain authenticated Firebase user")
                        }
                    }
                } else {
                    launch(Dispatchers.Main) {
                        _isLoading.value = false
                        onError("Unexpected credential format received")
                    }
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "User dismissed Google Sign-In prompt: ${e.message}")
                launch(Dispatchers.Main) {
                    _isLoading.value = false
                    // Cancellation is normal user flow, don't flag as failure
                }
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed: ${e.message}", e)
                launch(Dispatchers.Main) {
                    _isLoading.value = false
                    val errorMsg = e.localizedMessage ?: "Google Sign-In authentication error"
                    _authError.value = errorMsg
                    onError(errorMsg)
                }
            }
        }
    }

    /**
     * Initiates Indian Mobile (+91) phone verification and dispatches OTP SMS.
     */
    fun sendPhoneOtp(
        activity: Activity,
        rawPhoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanNumber = formatPhoneNumber(rawPhoneNumber)
        if (cleanNumber.length < 10) {
            onError("Please enter a valid 10-digit mobile number")
            return
        }

        _isLoading.value = true
        _authError.value = null

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Instant auto-verification
                scope.launch {
                    try {
                        val result = auth.signInWithCredential(credential).await()
                        result.user?.let { user ->
                            _currentUser.value = user
                            syncUserProfileToFirestore(activity, user)
                        }
                        launch(Dispatchers.Main) {
                            _isLoading.value = false
                        }
                    } catch (e: Exception) {
                        launch(Dispatchers.Main) {
                            _isLoading.value = false
                            onError(e.message ?: "Verification failed")
                        }
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "Phone verification failed: ${e.message}", e)
                _isLoading.value = false
                val msg = e.localizedMessage ?: "Failed to dispatch OTP. Please verify phone number."
                _authError.value = msg
                onError(msg)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.i(TAG, "OTP successfully dispatched to $cleanNumber")
                _isLoading.value = false
                onCodeSent(verificationId)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(cleanNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /**
     * Verifies the 6-digit SMS OTP code entered by the user.
     */
    fun verifyOtpWithCode(
        context: Context,
        verificationId: String,
        code: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        if (code.length < 6) {
            onError("Please enter the complete 6-digit OTP code")
            return
        }

        _isLoading.value = true
        _authError.value = null

        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        scope.launch {
            try {
                val result = auth.signInWithCredential(credential).await()
                val user = result.user
                if (user != null) {
                    _currentUser.value = user
                    syncUserProfileToFirestore(context, user)
                    launch(Dispatchers.Main) {
                        _isLoading.value = false
                        onSuccess(user)
                    }
                } else {
                    launch(Dispatchers.Main) {
                        _isLoading.value = false
                        onError("Invalid OTP verification response")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "OTP code verification error: ${e.message}", e)
                launch(Dispatchers.Main) {
                    _isLoading.value = false
                    val errorMsg = e.localizedMessage ?: "Invalid or expired OTP code"
                    _authError.value = errorMsg
                    onError(errorMsg)
                }
            }
        }
    }

    /**
     * Syncs user details & hardware telemetry directly to Firestore.
     */
    private fun syncUserProfileToFirestore(context: Context, user: FirebaseUser) {
        val telemetry = DeviceTelemetryTracker.getDeviceTelemetry(context)
        val savedPlanName = context.getSharedPreferences("website2app_billing_prefs", Context.MODE_PRIVATE)
            .getString("active_plan", com.example.data.model.UserPlan.FREE.name)
        val activePlan = runCatching {
            com.example.data.model.UserPlan.valueOf(savedPlanName ?: com.example.data.model.UserPlan.FREE.name)
        }.getOrDefault(com.example.data.model.UserPlan.FREE)

        val session = UserSessionProfile(
            uid = user.uid,
            email = user.email,
            displayName = user.displayName ?: if (!user.phoneNumber.isNullOrBlank()) "User ${user.phoneNumber?.takeLast(4)}" else "Bharat Developer",
            phoneNumber = user.phoneNumber,
            photoUrl = user.photoUrl?.toString(),
            plan = activePlan.title,
            currentDevice = telemetry.deviceName,
            deviceCategory = telemetry.category.title,
            isVerified = true
        )
        _sessionProfile.value = session

        val dbId = runCatching { context.getString(R.string.firestore_database_id) }.getOrNull()
        if (dbId.isNullOrBlank()) return

        scope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance(dbId)
                val userDoc = mapOf(
                    "userId" to user.uid,
                    "email" to (user.email ?: ""),
                    "displayName" to (session.displayName ?: ""),
                    "phoneNumber" to (user.phoneNumber ?: ""),
                    "currentDevice" to telemetry.deviceName,
                    "deviceType" to telemetry.category.title,
                    "plan" to activePlan.title,
                    "createdAt" to System.currentTimeMillis(),
                    "lastLoginAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(user.uid)
                    .set(userDoc, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Profile sync warning: ${e.message}")
            }
        }
    }

    fun syncPlanEntitlement(
        context: Context,
        planTitle: String,
        playProductId: String?,
        playOrderId: String?
    ) {
        val currentSession = _sessionProfile.value
        if (currentSession != null) {
            _sessionProfile.value = currentSession.copy(plan = planTitle)
        }
        val user = runCatching { auth.currentUser }.getOrNull() ?: return
        val dbId = runCatching { context.getString(R.string.firestore_database_id) }.getOrNull()
        if (dbId.isNullOrBlank()) return

        scope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance(dbId)
                val updates = mutableMapOf<String, Any>(
                    "plan" to planTitle,
                    "planUpdatedAt" to System.currentTimeMillis()
                )
                if (!playProductId.isNullOrBlank()) updates["playProductId"] = playProductId
                if (!playOrderId.isNullOrBlank()) updates["playOrderId"] = playOrderId
                firestore.collection("users").document(user.uid)
                    .set(updates, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Plan entitlement Firestore sync warning: ${e.message}")
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        _sessionProfile.value = null
        _authError.value = null
    }

    private fun formatPhoneNumber(input: String): String {
        val digitsOnly = input.filter { it.isDigit() }
        return when {
            input.startsWith("+") -> input.trim()
            digitsOnly.length == 10 -> "+91$digitsOnly"
            digitsOnly.length == 12 && digitsOnly.startsWith("91") -> "+$digitsOnly"
            else -> "+$digitsOnly"
        }
    }
}
