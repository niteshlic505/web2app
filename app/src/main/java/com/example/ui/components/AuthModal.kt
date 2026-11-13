package com.example.ui.components

import android.app.Activity
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.auth.FirebaseAuthManager
import com.example.ui.theme.AshokaBlue
import com.example.ui.theme.CorporateBlue
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndiaGreen
import com.example.ui.theme.IndianSaffron
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSaffron
import com.example.ui.theme.TextSecondary

@Composable
fun AuthModal(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isLoading by FirebaseAuthManager.isLoading.collectAsState()
    val authError by FirebaseAuthManager.authError.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Google Sign-In, 1: Mobile + OTP
    var phoneNumberInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var verificationIdHolder by remember { mutableStateOf<String?>(null) }
    var isOtpSent by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
                .border(BorderStroke(1.dp, IndianSaffron.copy(alpha = 0.5f)), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // National Corporate Ribbon
                TricolourHeaderBar()

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(IndianSaffron),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF1E1100),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Bharat Enterprise Auth",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Firebase Cloud & Identity Verification",
                                color = IndianSaffron,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                // Auth Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurfaceVariant,
                    contentColor = IndianSaffron,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = IndianSaffron,
                            height = 2.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = "Google Account", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = "Mobile + OTP", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (authError != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                .border(BorderStroke(0.5.dp, Color(0xFFEF4444)), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = authError ?: "",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (selectedTab == 0) {
                        // Google Sign-In Tab
                        Text(
                            text = "Authenticate securely using your Google corporate or personal workspace account. Backed by Firebase Authentication & Jetpack Credential Manager.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Interactive Google Sign-In Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .clickable(enabled = !isLoading && activity != null) {
                                    activity?.let { act ->
                                        FirebaseAuthManager.signInWithGoogle(
                                            activity = act,
                                            onSuccess = { user ->
                                                Toast.makeText(context, "Welcome, ${user.displayName ?: "Developer"}!", Toast.LENGTH_SHORT).show()
                                                onDismissRequest()
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp)
                                .testTag("google_sign_in_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color(0xFF1E3A8A),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Connecting to Google...",
                                        color = Color(0xFF1E293B),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                } else {
                                    Text(text = "G", color = Color(0xFF4285F4), fontWeight = FontWeight.Black, fontSize = 20.sp, fontFamily = FontFamily.SansSerif)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Sign in with Google",
                                        color = Color(0xFF1E293B),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // Mobile + OTP Tab
                        Text(
                            text = "Log in instantly with Indian Mobile Number (+91) via OTP SMS verification.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        if (!isOtpSent) {
                            OutlinedTextField(
                                value = phoneNumberInput,
                                onValueChange = {
                                    if (it.length <= 10) phoneNumberInput = it
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("phone_number_input"),
                                label = { Text("10-Digit Mobile Number") },
                                placeholder = { Text("9876543210") },
                                prefix = { Text("+91 ", color = IndianSaffron, fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = IndianSaffron, modifier = Modifier.size(18.dp))
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IndianSaffron,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            CorporateButton(
                                text = if (isLoading) "Dispatching OTP..." else "Send 6-Digit OTP",
                                enabled = !isLoading && phoneNumberInput.length == 10 && activity != null,
                                onClick = {
                                    activity?.let { act ->
                                        FirebaseAuthManager.sendPhoneOtp(
                                            activity = act,
                                            rawPhoneNumber = phoneNumberInput,
                                            onCodeSent = { vId ->
                                                verificationIdHolder = vId
                                                isOtpSent = true
                                                Toast.makeText(context, "OTP dispatched to +91 $phoneNumberInput", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "send_otp_button"
                            )
                        } else {
                            // OTP input mode
                            OutlinedTextField(
                                value = otpInput,
                                onValueChange = {
                                    if (it.length <= 6) otpInput = it
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("otp_code_input"),
                                label = { Text("Enter 6-Digit OTP Code") },
                                placeholder = { Text("123456") },
                                leadingIcon = {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = IndiaGreen, modifier = Modifier.size(18.dp))
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IndiaGreen,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CorporateButton(
                                    text = if (isLoading) "Verifying..." else "Verify & Sign In",
                                    enabled = !isLoading && otpInput.length == 6 && verificationIdHolder != null,
                                    onClick = {
                                        verificationIdHolder?.let { vId ->
                                            FirebaseAuthManager.verifyOtpWithCode(
                                                context = context,
                                                verificationId = vId,
                                                code = otpInput,
                                                onSuccess = { user ->
                                                    Toast.makeText(context, "Verified successfully!", Toast.LENGTH_SHORT).show()
                                                    onDismissRequest()
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    backgroundColor = IndiaGreen,
                                    contentColor = Color.White,
                                    testTag = "verify_otp_button"
                                )
                                SecondaryOutlineButton(
                                    text = "Resend",
                                    onClick = { isOtpSent = false },
                                    modifier = Modifier.weight(0.6f)
                                )
                            }
                        }
                    }

                    // Security compliance badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = IndiaGreen, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "256-bit TLS Encrypted • Digital India Compliant",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
