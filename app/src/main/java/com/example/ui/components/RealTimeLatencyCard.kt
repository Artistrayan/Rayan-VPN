package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalContrastTokens
import com.example.vpn.model.RealTimeLatencyReport
import com.example.vpn.model.VpnServer

@Composable
fun RealTimeLatencyMonitorCard(
    latencyReports: List<RealTimeLatencyReport>,
    selectedServerId: String,
    onSelectServer: (VpnServer) -> Unit,
    onSmartConnectFastest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalContrastTokens.current
    val fastestReport = latencyReports.find { it.isFastest }

    CyberCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("real_time_latency_card"),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(tokens.accentGlow.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = "Monitor",
                            tint = tokens.accentGlow,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "مانیتورینگ زنده پینگ و تاخیر شبکه",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "پایش آنلاین سرورهای آمریکا، هلند و آلمان",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Live pulsing indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(tokens.successColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = tokens.successColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Compare Servers Grid: USA, Netherlands, Germany
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                latencyReports.forEach { report ->
                    val isSelected = report.server.id == selectedServerId
                    val pingColor by animateColorAsState(
                        targetValue = when {
                            report.currentPingMs < 45 -> tokens.successColor
                            report.currentPingMs < 80 -> tokens.accentGlow
                            else -> tokens.warningColor
                        },
                        label = "ping_color"
                    )

                    Surface(
                        onClick = { onSelectServer(report.server) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) tokens.accentGlow.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(
                            tokens.cardBorderWidth,
                            if (report.isFastest) tokens.successColor else if (isSelected) tokens.accentGlow else tokens.cardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("latency_node_${report.server.countryCode}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = report.server.flagEmoji,
                                    fontSize = 22.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = report.server.countryNameFa,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        if (report.isFastest) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(tokens.successColor.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "⚡ سریع‌ترین",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = tokens.successColor
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${report.server.cityFa} • نوسان: ±${report.jitterMs}ms",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${report.currentPingMs} ms",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = pingColor
                                    )
                                    Text(
                                        text = report.qualityLabelFa,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(tokens.accentGlow),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.Black,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Smart Connect Button
            Button(
                onClick = onSmartConnectFastest,
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.accentGlow.copy(alpha = if (tokens.isHighContrast) 0.9f else 0.18f)
                ),
                border = BorderStroke(1.dp, tokens.accentGlow),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("smart_connect_fastest_btn")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Smart Connect",
                        tint = if (tokens.isHighContrast) Color.Black else tokens.accentGlow,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "اتصال هوشمند به سریع‌ترین سرور (${fastestReport?.server?.countryNameFa ?: "آلمان"})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (tokens.isHighContrast) Color.Black else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}
