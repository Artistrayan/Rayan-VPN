package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalContrastTokens
import com.example.vpn.VpnStateManager
import com.example.vpn.model.ContrastMode

@Composable
fun AppHeader(
    currentContrast: ContrastMode,
    isVip: Boolean,
    onContrastSelected: (ContrastMode) -> Unit,
    onVipClick: () -> Unit
) {
    var showContrastMenu by remember { mutableStateOf(false) }
    val tokens = LocalContrastTokens.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("app_header"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title & Logo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(tokens.cardBorderWidth, tokens.accentGlow, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "Logo",
                    tint = tokens.accentGlow,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "ARIA VPN",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "فیلتر شکن نسل جدید",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Actions: Contrast Switcher & VIP Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Contrast Mode Switcher Button
            Box {
                Surface(
                    onClick = { showContrastMenu = true },
                    shape = RoundedCornerShape(20.dp),
                    color = tokens.cardBackground,
                    border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorder),
                    modifier = Modifier.testTag("contrast_switcher_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrightnessMedium,
                            contentDescription = "کنتراست",
                            tint = tokens.accentGlow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (currentContrast) {
                                ContrastMode.CYBER_DARK -> "تاریک"
                                ContrastMode.HIGH_CONTRAST -> "تباین بالا"
                                ContrastMode.CLEAN_LIGHT -> "روشن"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                DropdownMenu(
                    expanded = showContrastMenu,
                    onDismissRequest = { showContrastMenu = false },
                    modifier = Modifier
                        .background(tokens.cardBackground)
                        .border(tokens.cardBorderWidth, tokens.cardBorder)
                ) {
                    ContrastMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(
                                            text = mode.titleFa,
                                            fontWeight = if (mode == currentContrast) FontWeight.Bold else FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = mode.titleEn,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (mode == currentContrast) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = tokens.accentGlow,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                showContrastMenu = false
                                onContrastSelected(mode)
                            }
                        )
                    }
                }
            }

            // VIP / Free Status Pill
            Surface(
                onClick = onVipClick,
                shape = RoundedCornerShape(20.dp),
                color = if (isVip) Color(0xFFFFD700).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    tokens.cardBorderWidth,
                    if (isVip) Color(0xFFFFD700) else tokens.cardBorder
                ),
                modifier = Modifier.testTag("vip_pill_header")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isVip) Icons.Default.Star else Icons.Default.WorkspacePremium,
                        contentDescription = "VIP",
                        tint = if (isVip) Color(0xFFFFD700) else tokens.accentGlow,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isVip) "اشتراک VIP" else "خرید اشتراک",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isVip) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    content: @Composable () -> Unit
) {
    val tokens = LocalContrastTokens.current
    Surface(
        modifier = modifier,
        shape = shape,
        color = tokens.cardBackground,
        border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorder),
        content = content
    )
}

@Composable
fun PingBadge(pingMs: Int, modifier: Modifier = Modifier) {
    val dotColor by animateColorAsState(
        targetValue = when {
            pingMs < 50 -> Color(0xFF00E676)
            pingMs < 100 -> Color(0xFFFFB300)
            else -> Color(0xFFFF3D71)
        },
        label = "ping_color"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(dotColor.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "$pingMs ms",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

fun formatByteRate(bytesPerSec: Long): String {
    val kb = bytesPerSec / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format("%.1f MB/s", mb)
    } else {
        String.format("%.0f KB/s", kb)
    }
}

fun formatTotalBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    val gb = mb / 1024.0
    return if (gb >= 1.0) {
        String.format("%.2f GB", gb)
    } else {
        String.format("%.1f MB", mb)
    }
}
