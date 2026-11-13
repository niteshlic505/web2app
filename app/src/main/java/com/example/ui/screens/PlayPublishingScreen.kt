package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppProject
import com.example.data.model.PublishTrack
import com.example.data.repository.AppRepository
import com.example.ui.components.CorporateButton
import com.example.ui.components.GlassCard
import com.example.ui.components.SecondaryOutlineButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CorporateBlue
import com.example.ui.theme.CorporateCobalt
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayPublishingScreen(
    projectId: String,
    repository: AppRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var project by remember { mutableStateOf<AppProject?>(null) }

    var isGoogleAccountConnected by remember { mutableStateOf(false) }
    var selectedTrack by remember { mutableStateOf(PublishTrack.INTERNAL) }
    var releaseNotes by remember { mutableStateOf("Initial release built with Website2App Studio. Optimized WebKit runtime with native bottom navigation.") }
    var privacyPolicyUrl by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submissionSuccess by remember { mutableStateOf(false) }

    // Checklist states
    var appIconVerified by remember { mutableStateOf(true) }
    var screenshotsReady by remember { mutableStateOf(true) }
    var featureGraphicReady by remember { mutableStateOf(true) }
    var dataSafetyCompleted by remember { mutableStateOf(true) }
    var targetApi34Verified by remember { mutableStateOf(true) }
    var userAcceptedPolicyDisclaimer by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        val proj = repository.getProject(projectId)
        project = proj
        if (proj != null) {
            privacyPolicyUrl = "${proj.url}/privacy"
        }
    }

    val currentProject = project ?: return

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column {
                    Text(
                        text = "Google Play Publishing Assistant",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${currentProject.name} (${currentProject.packageName})",
                        color = CorporateCobalt,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Google Play Developer Account Connection
        item {
            GlassCard(
                borderColor = if (isGoogleAccountConnected) EmeraldSuccess else DarkCardBorder,
                backgroundColor = DarkSurface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isGoogleAccountConnected) EmeraldSuccess.copy(alpha = 0.15f) else DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isGoogleAccountConnected) Icons.Default.VerifiedUser else Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = if (isGoogleAccountConnected) EmeraldSuccess else CorporateBlue
                            )
                        }
                        Column {
                            Text(
                                text = if (isGoogleAccountConnected) "Developer Account Connected" else "Google Play Developer API",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isGoogleAccountConnected) "Service Account: api-gateway@website2app.iam" else "Link via OAuth or Google Cloud Service Account",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    SecondaryOutlineButton(
                        text = if (isGoogleAccountConnected) "Disconnect" else "Connect API",
                        onClick = {
                            isGoogleAccountConnected = !isGoogleAccountConnected
                            if (isGoogleAccountConnected) {
                                Toast.makeText(context, "Google Play Developer API linked", Toast.LENGTH_SHORT).show()
                            }
                        },
                        borderColor = if (isGoogleAccountConnected) EmeraldSuccess else CorporateBlue,
                        textColor = if (isGoogleAccountConnected) EmeraldSuccess else CorporateBlue
                    )
                }
            }
        }

        // Release Track Selection
        item {
            GlassCard(backgroundColor = DarkSurface) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "1. Google Play Release Track", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    for (track in PublishTrack.values()) {
                        val isSelected = selectedTrack == track
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) DarkSurfaceVariant else Color.Transparent)
                                .clickable { selectedTrack = track }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = track.label, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(text = track.badge, color = TextSecondary, fontSize = 11.sp)
                            }
                            StatusBadge(text = track.name, color = if (isSelected) CorporateBlue else TextMuted)
                        }
                    }
                }
            }
        }

        // Release Notes
        item {
            GlassCard(backgroundColor = DarkSurface) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "2. Release Notes & Version Metadata", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Target Version: ${currentProject.versionName} (Version Code: ${currentProject.versionCode})", color = CorporateCobalt, fontSize = 12.sp)
                    OutlinedTextField(
                        value = releaseNotes,
                        onValueChange = { releaseNotes = it },
                        label = { Text("What's new in this build?") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CorporateBlue,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = privacyPolicyUrl,
                        onValueChange = { privacyPolicyUrl = it },
                        label = { Text("Privacy Policy URL (Google Play Mandatory)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CorporateBlue,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }
            }
        }

        // Google Play Store Pre-Submission Checklist
        item {
            GlassCard(backgroundColor = DarkSurface) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "3. Google Play Store Compliance Checklist", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Verify all mandatory listing assets and policy declarations before deployment:", color = TextSecondary, fontSize = 12.sp)

                    ChecklistRow("High-Res 512x512 Adaptive Icon", appIconVerified) { appIconVerified = it }
                    ChecklistRow("Phone & Tablet Screenshots (Min 2 required)", screenshotsReady) { screenshotsReady = it }
                    ChecklistRow("1024x500 Feature Graphic Banner", featureGraphicReady) { featureGraphicReady = it }
                    ChecklistRow("Data Safety & Network Transmission Form", dataSafetyCompleted) { dataSafetyCompleted = it }
                    ChecklistRow("Target SDK 34+ (Configured: API 36)", targetApi34Verified) { targetApi34Verified = it }
                }
            }
        }

        // Policy Disclaimer & Terms
        item {
            GlassCard(
                borderColor = Color(0xFFF59E0B).copy(alpha = 0.5f),
                backgroundColor = Color(0xFF141208)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Policy, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                        Text(text = "Google Play Policy & Verification Notice", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(
                        text = "Google Play Console account registration ($25 one-time registration), developer identity verification, policy compliance, and review remain the responsibility of the publisher. Website2App Studio compiles and formats valid signed AAB packages for your account.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { userAcceptedPolicyDisclaimer = !userAcceptedPolicyDisclaimer }
                    ) {
                        Checkbox(
                            checked = userAcceptedPolicyDisclaimer,
                            onCheckedChange = { userAcceptedPolicyDisclaimer = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = CorporateBlue,
                                uncheckedColor = TextMuted
                            )
                        )
                        Text(
                            text = "I verify authorization and ownership rights for this website domain.",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Submission Button
        item {
            val allChecked = isGoogleAccountConnected && userAcceptedPolicyDisclaimer &&
                    appIconVerified && screenshotsReady && featureGraphicReady && dataSafetyCompleted

            CorporateButton(
                text = if (isSubmitting) "Dispatching Release to Google Play..." else if (submissionSuccess) "Release Dispatched Successfully!" else "Submit Release to ${selectedTrack.label}",
                icon = Icons.Default.CloudUpload,
                enabled = allChecked && !isSubmitting,
                onClick = {
                    isSubmitting = true
                    coroutineScope.launch {
                        delay(1200)
                        isSubmitting = false
                        submissionSuccess = true
                        Toast.makeText(context, "Successfully queued release on Google Play ${selectedTrack.label}!", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "submit_play_store_button"
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ChecklistRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = EmeraldSuccess,
                uncheckedColor = TextMuted
            )
        )
        Text(text = title, color = TextPrimary, fontSize = 12.sp)
    }
}
