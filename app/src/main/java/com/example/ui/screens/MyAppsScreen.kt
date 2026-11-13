package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.data.model.ConversionMode
import com.example.data.repository.AppRepository
import com.example.ui.components.CorporateButton
import com.example.ui.components.GlassCard
import com.example.ui.components.ProjectIconBadge
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
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppsScreen(
    repository: AppRepository,
    onCreateAppClick: () -> Unit,
    onOpenPreview: (String) -> Unit,
    onOpenCustomize: (String) -> Unit,
    onOpenBuild: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val projects by repository.allProjects.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterMode by remember { mutableStateOf<ConversionMode?>(null) }

    val filteredProjects = projects.filter { proj ->
        val matchesSearch = proj.name.contains(searchQuery, ignoreCase = true) ||
                proj.url.contains(searchQuery, ignoreCase = true) ||
                proj.packageName.contains(searchQuery, ignoreCase = true)
        val matchesMode = selectedFilterMode == null || proj.conversionMode == selectedFilterMode
        matchesSearch && matchesMode
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
                        text = "Application Portfolio",
                        color = TextPrimary,
                        fontFamily = com.example.ui.theme.SpaceGroteskFontFamily,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${projects.size} CONFIGURED APPS // v10.0",
                        color = com.example.ui.theme.IndianSaffron,
                        fontFamily = com.example.ui.theme.JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                CorporateButton(
                    text = "New App",
                    icon = Icons.Default.Add,
                    onClick = onCreateAppClick,
                    testTag = "my_apps_new_button"
                )
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by name, package name, or domain") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = CorporateBlue)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CorporateBlue,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                singleLine = true
            )
        }

        // Filter Pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedFilterMode == null) CorporateBlue.copy(alpha = 0.15f) else DarkSurfaceVariant)
                        .border(BorderStroke(1.dp, if (selectedFilterMode == null) CorporateBlue else DarkCardBorder), RoundedCornerShape(8.dp))
                        .clickable { selectedFilterMode = null }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "All (${projects.size})",
                        color = if (selectedFilterMode == null) CorporateBlue else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                for (mode in ConversionMode.values()) {
                    val count = projects.count { it.conversionMode == mode }
                    val isSelected = selectedFilterMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CorporateBlue.copy(alpha = 0.15f) else DarkSurfaceVariant)
                            .border(BorderStroke(1.dp, if (isSelected) CorporateBlue else DarkCardBorder), RoundedCornerShape(8.dp))
                            .clickable { selectedFilterMode = mode }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${mode.badge} ($count)",
                            color = if (isSelected) CorporateBlue else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        if (filteredProjects.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = DarkCardBorder,
                    backgroundColor = DarkSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = CorporateBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (projects.isEmpty()) "No Applications Created Yet" else "No Matching Applications Found",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (projects.isEmpty()) "Click 'New App' above to import and convert your first website." else "Try adjusting your search query or filter criteria.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(filteredProjects) { project ->
                var menuExpanded by remember { mutableStateOf(false) }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = DarkCardBorder,
                    backgroundColor = DarkSurface
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ProjectIconBadge(symbolName = project.iconEmoji)
                                Column {
                                    Text(
                                        text = project.name,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${project.packageName} • v${project.versionName} (${project.versionCode})",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(
                                    text = project.conversionMode.badge,
                                    color = when (project.conversionMode) {
                                        ConversionMode.SMART_WEBVIEW -> CorporateBlue
                                        ConversionMode.PWA -> EmeraldSuccess
                                        ConversionMode.AI_HYBRID -> CorporateCobalt
                                    }
                                )
                                Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = TextSecondary)
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        modifier = Modifier.background(DarkSurfaceVariant)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Duplicate Project", color = TextPrimary) },
                                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CorporateBlue) },
                                            onClick = {
                                                menuExpanded = false
                                                coroutineScope.launch {
                                                    repository.duplicateProject(project)
                                                    Toast.makeText(context, "Project duplicated", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Export JSON Config", color = TextPrimary) },
                                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = CorporateCobalt) },
                                            onClick = {
                                                menuExpanded = false
                                                val json = JSONObject().apply {
                                                    put("id", project.id)
                                                    put("name", project.name)
                                                    put("url", project.url)
                                                    put("package", project.packageName)
                                                    put("mode", project.conversionMode.name)
                                                    put("versionName", project.versionName)
                                                    put("versionCode", project.versionCode)
                                                }
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_SUBJECT, "${project.name} Configuration")
                                                    putExtra(Intent.EXTRA_TEXT, json.toString(2))
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Export App Config"))
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete Project", color = Color(0xFFEF4444)) },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                                            onClick = {
                                                menuExpanded = false
                                                coroutineScope.launch {
                                                    repository.deleteProject(project.id)
                                                    Toast.makeText(context, "Project removed", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = project.url,
                            color = CorporateCobalt,
                            fontSize = 12.sp,
                            maxLines = 1
                        )

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SecondaryOutlineButton(
                                text = "Preview",
                                icon = Icons.Default.PlayArrow,
                                onClick = { onOpenPreview(project.id) },
                                modifier = Modifier.weight(1f),
                                borderColor = CorporateBlue.copy(alpha = 0.5f),
                                textColor = CorporateBlue
                            )
                            SecondaryOutlineButton(
                                text = "Customize",
                                icon = Icons.Default.Tune,
                                onClick = { onOpenCustomize(project.id) },
                                modifier = Modifier.weight(1f),
                                borderColor = DarkCardBorder,
                                textColor = TextPrimary
                            )
                            SecondaryOutlineButton(
                                text = "Build",
                                icon = Icons.Default.Build,
                                onClick = { onOpenBuild(project.id) },
                                modifier = Modifier.weight(1f),
                                borderColor = EmeraldSuccess.copy(alpha = 0.5f),
                                textColor = EmeraldSuccess
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
