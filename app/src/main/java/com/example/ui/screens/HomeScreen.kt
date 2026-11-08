package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CyberCard
import com.example.ui.components.PingBadge
import com.example.ui.components.RewardedAdDialog
import com.example.ui.components.formatByteRate
import com.example.ui.components.formatTotalBytes
import com.example.ui.theme.LocalContrastTokens
import com.example.vpn.model.VpnServer
import com.example.vpn.model.VpnSessionState
import com.example.vpn.model.VpnStatus
import java.util.Locale

@Composable
fun HomeScreen(
    sessionState: VpnSessionState,
    onConnectToggle: () -> Unit,
    onSelectServerClick: () -> Unit,
    onAddRewardMinutes: (String, Int) -> Unit
) {
    val tokens = LocalContrastTokens.current
    var showAdDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val isConnected = sessionState.status == VpnStatus.CONNECTED
    val isConnecting = sessionState.status == VpnStatus.CONNECTING

    val powerColor by animateColorAsState(
        targetValue = when {
            isConnected -> tokens.successColor
            isConnecting -> tokens.warningColor
            else -> tokens.accentGlow
        },
        label = "power_color"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("home_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Selected Server Banner
        ServerSelectionBanner(
            currentServer = sessionState.currentServer,
            onClick = onSelectServerClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Large Central Power Button with pulse rings
        Box(
            modifier = Modifier
                .size(230.dp)
                .testTag("vpn_power_container"),
            contentAlignment = Alignment.Center
        ) {
            // Ambient outer glow ring
            if (isConnected || isConnecting) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(powerColor.copy(alpha = if (tokens.isHighContrast) 0.08f else 0.15f))
                        .border(
                            width = if (tokens.isHighContrast) 2.dp else 1.dp,
                            color = powerColor.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                )
            }

            // Circular progress track
            if (isConnected && !sessionState.isVipActive) {
                val progress = (sessionState.remainingSeconds.toFloat() / sessionState.totalSessionAllowedSeconds).coerceIn(0f, 1f)
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(190.dp),
                    color = powerColor,
                    strokeWidth = 6.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                )
            }

            // Main Interactive Power Button
            Surface(
                onClick = onConnectToggle,
                shape = CircleShape,
                color = tokens.cardBackground,
                border = BorderStroke(
                    width = if (tokens.isHighContrast) 3.dp else 2.dp,
                    color = powerColor
                ),
                modifier = Modifier
                    .size(160.dp)
                    .testTag("vpn_power_button"),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(46.dp),
                            color = powerColor,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Connect Toggle",
                            tint = powerColor,
                            modifier = Modifier.size(52.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when (sessionState.status) {
                            VpnStatus.CONNECTED -> "قطع اتصال"
                            VpnStatus.CONNECTING -> "اتصال..."
                            VpnStatus.DISCONNECTING -> "در حال قطع..."
                            VpnStatus.DISCONNECTED -> "شروع اتصال"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status description text
        Text(
            text = when (sessionState.status) {
                VpnStatus.CONNECTED -> "فیلتر شکن فعال و اینترنت شما کاملاً رمزنگاری شده است"
                VpnStatus.CONNECTING -> sessionState.connectionStepMessage.ifBlank { "در حال برقراری اتصال با سرور..." }
                VpnStatus.DISCONNECTING -> "در حال قطع نشست امن..."
                VpnStatus.DISCONNECTED -> "برای عبور امن از فیلترینگ دکمه بالا را لمس کنید"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (isConnected) tokens.successColor else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Connection Timer Card with +10 Min Button
        SessionTimerCard(
            sessionState = sessionState,
            onAddRewardClick = { showAdDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Live Real-Time Speed & Telemetry Card
        LiveSpeedMetricsCard(sessionState = sessionState)

        Spacer(modifier = Modifier.height(16.dp))

        // Security Info Pill (IP & Protocol)
        SecurityInfoPill(sessionState = sessionState)

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Rewarded Ad Dialog
    if (showAdDialog) {
        RewardedAdDialog(
            onDismiss = { showAdDialog = false },
            onRewardEarned = { nonce ->
                onAddRewardMinutes(nonce, 10)
            }
        )
    }
}

@Composable
private fun ServerSelectionBanner(
    currentServer: VpnServer,
    onClick: () -> Unit
) {
    val tokens = LocalContrastTokens.current

    CyberCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("server_selection_banner"),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentServer.flagEmoji,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentServer.countryNameFa,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (currentServer.isDefault) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(tokens.accentGlow.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "پیش‌فرض",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = tokens.accentGlow
                                )
                            }
                        }
                    }
                    Text(
                        text = "${currentServer.cityFa} (${currentServer.cityEn})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                PingBadge(pingMs = currentServer.pingMs)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Select Server",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SessionTimerCard(
    sessionState: VpnSessionState,
    onAddRewardClick: () -> Unit
) {
    val tokens = LocalContrastTokens.current
    val isVip = sessionState.isVipActive

    CyberCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("session_timer_card"),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Timer",
                        tint = tokens.accentGlow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مدت زمان اتصال:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (isVip) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFD700).copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "نامحدود (اشتراک VIP)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFD700)
                        )
                    }
                } else {
                    val formatted = formatRemainingTime(sessionState.remainingSeconds)
                    Text(
                        text = formatted,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = if (sessionState.remainingSeconds < 300) tokens.warningColor else tokens.accentGlow
                    )
                }
            }

            if (!isVip) {
                Spacer(modifier = Modifier.height(12.dp))

                // +10 Min Button Requirement
                Button(
                    onClick = onAddRewardClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.accentGlow.copy(alpha = if (tokens.isHighContrast) 0.9f else 0.15f)
                    ),
                    border = BorderStroke(1.dp, tokens.accentGlow),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("add_10_minutes_ad_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = "Watch Ad",
                            tint = if (tokens.isHighContrast) Color.Black else tokens.accentGlow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "➕ تماشای تبلیغ (+۱۰ دقیقه زمان اضافه)",
                            color = if (tokens.isHighContrast) Color.Black else MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveSpeedMetricsCard(sessionState: VpnSessionState) {
    val tokens = LocalContrastTokens.current
    val traffic = sessionState.traffic

    CyberCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_speed_card"),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Speed",
                        tint = tokens.accentGlow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "سرعت زنده و ترافیک شبکه",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "پینگ: ${traffic.pingMs} ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Download Speed
                SpeedStatItem(
                    label = "سرعت دانلود",
                    value = if (sessionState.status == VpnStatus.CONNECTED) formatByteRate(traffic.downloadSpeedBps) else "۰ MB/s",
                    icon = Icons.Default.ArrowDownward,
                    color = tokens.successColor,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Upload Speed
                SpeedStatItem(
                    label = "سرعت آپلود",
                    value = if (sessionState.status == VpnStatus.CONNECTED) formatByteRate(traffic.uploadSpeedBps) else "۰ MB/s",
                    icon = Icons.Default.ArrowUpward,
                    color = tokens.accentGlow,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Total Bytes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "حجم رد و بدل شده:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTotalBytes(traffic.totalDownloadBytes + traffic.totalUploadBytes),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun SpeedStatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    val tokens = LocalContrastTokens.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorder.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun SecurityInfoPill(sessionState: VpnSessionState) {
    val tokens = LocalContrastTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.cardBackground)
            .border(tokens.cardBorderWidth, tokens.cardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Protocol",
                tint = tokens.accentGlow,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "پروتکل: ${sessionState.activeProtocol.title}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = "IP",
                tint = tokens.successColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "IP: ${sessionState.assignedIp}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

private fun formatRemainingTime(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
}
