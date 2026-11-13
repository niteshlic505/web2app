package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AshokaBlue
import com.example.ui.theme.AshokaCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardBorderGlow
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.IndiaGreen
import com.example.ui.theme.IndiaGreenLight
import com.example.ui.theme.IndianSaffron
import com.example.ui.theme.IndianSaffronGlow
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSaffron
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BharatFuturisticBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Subtle ambient saffron glow top-right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(IndianSaffron.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(w * 0.9f, h * 0.08f),
                    radius = w * 0.7f
                ),
                center = Offset(w * 0.9f, h * 0.08f),
                radius = w * 0.7f
            )

            // Subtle Ashoka cyan-blue glow bottom-left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AshokaCyan.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.1f, h * 0.85f),
                    radius = w * 0.75f
                ),
                center = Offset(w * 0.1f, h * 0.85f),
                radius = w * 0.75f
            )

            // Subtle emerald glow mid-right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(IndiaGreenLight.copy(alpha = 0.04f), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.55f),
                    radius = w * 0.55f
                ),
                center = Offset(w * 0.85f, h * 0.55f),
                radius = w * 0.55f
            )

            // Architectural micro-grid lines
            val stepPx = 42.dp.toPx()
            val gridColor = Color(0xFF1A3366).copy(alpha = 0.14f)
            var x = 0f
            while (x < w) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 0.7f)
                x += stepPx
            }
            var y = 0f
            while (y < h) {
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 0.7f)
                y += stepPx
            }
        }
        content()
    }
}

@Composable
fun AshokaChakraHUD(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 64.dp,
    primaryRingColor: Color = AshokaCyan,
    accentColor: Color = IndianSaffron,
    animate: Boolean = true
) {
    val rotationDeg = if (animate) {
        val infiniteTransition = rememberInfiniteTransition(label = "chakra_spin")
        val angle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 28000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "chakra_angle"
        )
        angle
    } else 0f

    Canvas(modifier = modifier.size(sizeDp)) {
        val r = size.minDimension / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val center = Offset(cx, cy)

        // Outer holographic orbital arc
        drawCircle(
            color = accentColor.copy(alpha = 0.25f),
            radius = r * 0.96f,
            center = center,
            style = Stroke(width = 1.2.dp.toPx())
        )

        // Main Chakra rim
        drawCircle(
            color = primaryRingColor.copy(alpha = 0.85f),
            radius = r * 0.82f,
            center = center,
            style = Stroke(width = 1.8.dp.toPx())
        )

        // Inner core hub
        drawCircle(
            color = accentColor,
            radius = r * 0.16f,
            center = center
        )

        // 24 spokes rotating smoothly
        rotate(degrees = rotationDeg, pivot = center) {
            val spokeCount = 24
            for (i in 0 until spokeCount) {
                val angleRad = Math.toRadians((i * (360.0 / spokeCount)))
                val startX = cx + (r * 0.20f * cos(angleRad)).toFloat()
                val startY = cy + (r * 0.20f * sin(angleRad)).toFloat()
                val endX = cx + (r * 0.79f * cos(angleRad)).toFloat()
                val endY = cy + (r * 0.79f * sin(angleRad)).toFloat()

                drawLine(
                    color = if (i % 6 == 0) accentColor.copy(alpha = 0.9f) else primaryRingColor.copy(alpha = 0.55f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (i % 6 == 0) 1.5.dp.toPx() else 0.9.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun TricolourHeaderBar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        IndianSaffron,
                        IndianSaffronGlow,
                        Color(0xFFF8FAFC),
                        AshokaCyan,
                        IndiaGreenLight,
                        IndiaGreen
                    )
                )
            )
    )
}

@Composable
fun SectionHeader(
    title: String,
    tag: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(IndianSaffron, AshokaCyan, IndiaGreenLight)
                        )
                    )
            )
            Text(
                text = title,
                color = TextPrimary,
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            )
        }
        if (tag != null) {
            Text(
                text = tag,
                color = AshokaCyan,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = DarkCardBorder,
    backgroundColor: Color = DarkSurface,
    cornerRadius: Dp = 14.dp,
    showTricolourTopBar: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val glassGradient = Brush.verticalGradient(
        colors = listOf(
            DarkSurfaceElevated.copy(alpha = 0.92f),
            backgroundColor.copy(alpha = 0.96f)
        )
    )
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            borderColor.copy(alpha = 0.85f),
            DarkCardBorderGlow.copy(alpha = 0.35f),
            borderColor.copy(alpha = 0.7f)
        )
    )

    val cardModifier = if (onClick != null) {
        modifier
            .clip(shape)
            .background(glassGradient)
            .border(BorderStroke(1.dp, borderBrush), shape)
            .clickable { onClick() }
    } else {
        modifier
            .clip(shape)
            .background(glassGradient)
            .border(BorderStroke(1.dp, borderBrush), shape)
    }

    Column(modifier = cardModifier) {
        if (showTricolourTopBar) {
            TricolourHeaderBar()
        }
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun CorporateButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    backgroundColor: Color = IndianSaffron,
    contentColor: Color = if (backgroundColor == IndianSaffron) Color(0xFF1A0E00) else Color.White,
    testTag: String = "corporate_button"
) {
    val shape = RoundedCornerShape(10.dp)
    val isSaffron = backgroundColor == IndianSaffron

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSaffron) Color.Transparent else backgroundColor,
            disabledContainerColor = Color(0xFF14213D)
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(
                if (enabled && isSaffron) {
                    Modifier
                        .clip(shape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(IndianSaffron, IndianSaffronGlow)
                            )
                        )
                } else Modifier
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) contentColor else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = if (enabled) contentColor else TextMuted,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
            )
        }
    }
}

// Backwards compatibility alias
@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    testTag: String = "neon_button"
) {
    CorporateButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        testTag = testTag
    )
}

@Composable
fun SecondaryOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    borderColor: Color = DarkCardBorder,
    textColor: Color = TextPrimary,
    testTag: String = "secondary_outline_button"
) {
    val activeTextColor = if (enabled) textColor else TextMuted
    val activeBorderColor = if (enabled) borderColor.copy(alpha = 0.75f) else DarkCardBorder.copy(alpha = 0.4f)
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, activeBorderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = DarkSurfaceVariant.copy(alpha = 0.45f),
            contentColor = activeTextColor,
            disabledContentColor = TextMuted
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = activeTextColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = activeTextColor,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    subtext: String? = null
) {
    GlassCard(
        modifier = modifier,
        borderColor = accentColor.copy(alpha = 0.45f),
        backgroundColor = DarkSurface,
        cornerRadius = 12.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .border(BorderStroke(0.5.dp, accentColor.copy(alpha = 0.4f)), RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            if (subtext != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtext,
                    color = TextMuted,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .border(BorderStroke(0.8.dp, color.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = text,
                color = color,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MakeInIndiaBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        DarkSurfaceVariant,
                        Color(0xFF162B5A)
                    )
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        colors = listOf(IndianSaffron.copy(alpha = 0.7f), AshokaCyan.copy(alpha = 0.5f), IndiaGreenLight.copy(alpha = 0.7f))
                    )
                ),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AshokaChakraHUD(sizeDp = 15.dp, primaryRingColor = AshokaCyan, accentColor = IndianSaffron, animate = false)
            Text(
                text = "BHARAT ENTERPRISE",
                color = TextSaffron,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
        }
    }
}

@Composable
fun DeviceCompatibilityBadge(
    isTablet: Boolean,
    modifier: Modifier = Modifier
) {
    val badgeColor = if (isTablet) IndianSaffron else AshokaCyan
    val icon = if (isTablet) Icons.Default.Tablet else Icons.Default.Smartphone
    val label = if (isTablet) "TABLET READY (sw600dp+)" else "MOBILE READY"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeColor.copy(alpha = 0.14f))
            .border(BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.55f)), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(13.dp))
            Text(
                text = label,
                color = badgeColor,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProjectIconBadge(
    symbolName: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 44.dp,
    tintColor: Color = IndianSaffron
) {
    val vectorIcon = when (symbolName.lowercase()) {
        "store", "ecommerce", "retail", "shopping" -> Icons.Default.Storefront
        "business", "corporate", "enterprise" -> Icons.Default.Business
        "cloud", "saas", "platform" -> Icons.Default.Cloud
        "dashboard", "analytics", "stats" -> Icons.Default.Assessment
        "article", "news", "blog", "portal" -> Icons.Default.Article
        "code", "dev", "tech" -> Icons.Default.Code
        "tablet", "tab", "ipad" -> Icons.Default.Tablet
        "mobile", "app", "phone" -> Icons.Default.Smartphone
        else -> Icons.Default.Language
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(11.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(DarkSurfaceVariant, DarkSurface)
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(tintColor.copy(alpha = 0.6f), AshokaCyan.copy(alpha = 0.35f))
                    )
                ),
                RoundedCornerShape(11.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = vectorIcon,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(sizeDp * 0.52f)
        )
    }
}

@Composable
fun TerminalLogViewer(
    logs: String,
    modifier: Modifier = Modifier,
    title: String = "Gradle Build Execution Stream"
) {
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF020612))
            .border(BorderStroke(1.dp, DarkCardBorder), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceElevated)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(IndianSaffron))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(IndiaGreenLight))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(
                onClick = { clipboardManager.setText(AnnotatedString(logs)) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy logs",
                    tint = AshokaCyan,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(12.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = if (logs.isBlank()) "Bharat Build Worker initialized. Awaiting task execution...\n" else logs,
                color = AshokaCyan,
                fontSize = 11.sp,
                fontFamily = JetBrainsMonoFontFamily,
                lineHeight = 16.sp
            )
        }
    }
}
