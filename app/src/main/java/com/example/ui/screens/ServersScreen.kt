package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CyberCard
import com.example.ui.components.PingBadge
import com.example.ui.components.RealTimeLatencyMonitorCard
import com.example.ui.theme.LocalContrastTokens
import com.example.vpn.model.RealTimeLatencyReport
import com.example.vpn.model.VpnServer

@Composable
fun ServersScreen(
    servers: List<VpnServer>,
    selectedServer: VpnServer,
    latencyReports: List<RealTimeLatencyReport>,
    isTestingPings: Boolean,
    isVipActive: Boolean,
    onSelectServer: (VpnServer) -> Unit,
    onSetDefaultServer: (String) -> Unit,
    onRefreshPings: () -> Unit,
    onSmartConnectFastest: () -> Unit,
    onUpgradeVipClick: () -> Unit
) {
    val tokens = LocalContrastTokens.current
    var selectedCategory by remember { mutableStateOf("all") }

    val filteredServers = remember(servers, selectedCategory) {
        when (selectedCategory) {
            "free" -> servers.filter { !it.isVip }
            "vip" -> servers.filter { it.isVip }
            "fastest" -> servers.sortedBy { it.pingMs }
            else -> servers
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "refresh_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("servers_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))

                // Title and subtitle
                Text(
                    text = "انتخاب سرور پرسرعت",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "سرورهای رایگان آمریکا، هلند و آلمان به همراه نودهای اختصاصی VIP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Real-Time Latency Monitor Card
                if (latencyReports.isNotEmpty()) {
                    RealTimeLatencyMonitorCard(
                        latencyReports = latencyReports,
                        selectedServerId = selectedServer.id,
                        onSelectServer = onSelectServer,
                        onSmartConnectFastest = onSmartConnectFastest
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Filter Categories Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryChip(
                        label = "همه",
                        selected = selectedCategory == "all",
                        onClick = { selectedCategory = "all" }
                    )
                    CategoryChip(
                        label = "رایگان",
                        selected = selectedCategory == "free",
                        onClick = { selectedCategory = "free" }
                    )
                    CategoryChip(
                        label = "ویژه VIP",
                        selected = selectedCategory == "vip",
                        onClick = { selectedCategory = "vip" }
                    )
                    CategoryChip(
                        label = "سریع‌ترین",
                        selected = selectedCategory == "fastest",
                        onClick = { selectedCategory = "fastest" }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Server Items
            items(filteredServers, key = { it.id }) { server ->
                ServerListItem(
                    server = server,
                    isSelected = server.id == selectedServer.id,
                    isVipUser = isVipActive,
                    onSelect = {
                        if (server.isVip && !isVipActive) {
                            onUpgradeVipClick()
                        } else {
                            onSelectServer(server)
                        }
                    },
                    onToggleDefault = {
                        onSetDefaultServer(server.id)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // Floating Ping Refresh Button
        FloatingActionButton(
            onClick = onRefreshPings,
            containerColor = tokens.accentGlow,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("refresh_pings_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh Pings",
                modifier = Modifier
                    .size(24.dp)
                    .then(if (isTestingPings) Modifier.rotate(rotationAngle) else Modifier)
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tokens = LocalContrastTokens.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.labelSmall
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = tokens.accentGlow.copy(alpha = if (tokens.isHighContrast) 0.9f else 0.2f),
            selectedLabelColor = if (tokens.isHighContrast) Color.Black else tokens.accentGlow,
            containerColor = tokens.cardBackground,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = BorderStroke(
            tokens.cardBorderWidth,
            if (selected) tokens.accentGlow else tokens.cardBorder
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun ServerListItem(
    server: VpnServer,
    isSelected: Boolean,
    isVipUser: Boolean,
    onSelect: () -> Unit,
    onToggleDefault: () -> Unit
) {
    val tokens = LocalContrastTokens.current
    val isLockedVip = server.isVip && !isVipUser

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) tokens.accentGlow.copy(alpha = 0.12f) else tokens.cardBackground,
        border = BorderStroke(
            tokens.cardBorderWidth,
            if (isSelected) tokens.accentGlow else tokens.cardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("server_item_${server.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Flag and Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = server.flagEmoji,
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = server.countryNameFa,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (server.isVip) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFFD700).copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "VIP",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFFFFD700)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${server.cityFa} • ${server.serverAddress}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Default Toggle & Selection Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PingBadge(pingMs = server.pingMs)

                    Spacer(modifier = Modifier.width(6.dp))

                    // Star Button to set as Default
                    IconButton(
                        onClick = onToggleDefault,
                        modifier = Modifier.size(36.dp).testTag("default_toggle_${server.id}")
                    ) {
                        Icon(
                            imageVector = if (server.isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Set Default",
                            tint = if (server.isDefault) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (isLockedVip) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked VIP",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(tokens.accentGlow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Server Load Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "میزان ترافیک سرور: ${server.loadPercentage}٪",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { server.loadPercentage / 100f },
                    modifier = Modifier
                        .width(90.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = when {
                        server.loadPercentage < 50 -> tokens.successColor
                        server.loadPercentage < 80 -> tokens.warningColor
                        else -> Color(0xFFFF3D71)
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}
