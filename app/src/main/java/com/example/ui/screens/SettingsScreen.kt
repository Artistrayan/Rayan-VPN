package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CyberCard
import com.example.ui.theme.LocalContrastTokens
import com.example.vpn.model.ContrastMode
import com.example.vpn.model.VpnProtocol
import com.example.vpn.model.VpnSessionState

@Composable
fun SettingsScreen(
    sessionState: VpnSessionState,
    onContrastModeChange: (ContrastMode) -> Unit,
    onProtocolChange: (VpnProtocol) -> Unit,
    onKillSwitchToggle: (Boolean) -> Unit,
    onAutoConnectToggle: (Boolean) -> Unit,
    onDnsChange: (String) -> Unit
) {
    val tokens = LocalContrastTokens.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Section: In-App Contrast System (سیستم نمایش تباین درون برنامه)
        SectionHeader(
            title = "سیستم نمایش تباین درون برنامه",
            subtitle = "امکان تغییر پوسته و تنظیم حداکثر تباین رنگی برای خوانایی کامل",
            icon = Icons.Default.BrightnessMedium
        )

        Spacer(modifier = Modifier.height(10.dp))

        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                ContrastMode.entries.forEachIndexed { index, mode ->
                    ContrastOptionRow(
                        mode = mode,
                        isSelected = sessionState.contrastMode == mode,
                        onSelect = { onContrastModeChange(mode) }
                    )
                    if (index < ContrastMode.entries.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section: Next-Gen Protocols (سبک و سیستم جدید)
        SectionHeader(
            title = "پروتکل اتصال (سیستم جدید و سبک)",
            subtitle = "انتخاب پروتکل‌های نسل جدید برای بیشترین سرعت و دور زدن فیلترینگ شدید",
            icon = Icons.Default.Router
        )

        Spacer(modifier = Modifier.height(10.dp))

        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                VpnProtocol.entries.forEachIndexed { index, protocol ->
                    ProtocolOptionRow(
                        protocol = protocol,
                        isSelected = sessionState.activeProtocol == protocol,
                        onSelect = { onProtocolChange(protocol) }
                    )
                    if (index < VpnProtocol.entries.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section: Advanced Security Features
        SectionHeader(
            title = "امنیت و امکانات پیشرفته",
            subtitle = "تنظیمات جلوگیری از نشت داده و حفاظت مضاعف",
            icon = Icons.Default.Security
        )

        Spacer(modifier = Modifier.height(10.dp))

        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Kill Switch
                SwitchOptionRow(
                    title = "کیل سوییچ (Kill Switch)",
                    desc = "در صورت قطع شدن VPN، اینترنت کل گوشی مسدود می‌شود تا آی‌پی لو نرود",
                    checked = sessionState.isKillSwitchEnabled,
                    onCheckedChange = onKillSwitchToggle
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Auto Connect
                SwitchOptionRow(
                    title = "اتصال خودکار هنگام باز شدن برنامه",
                    desc = "به محض اجرای برنامه، بلافاصله به سرور پیش‌فرض وصل شو",
                    checked = sessionState.isAutoConnectEnabled,
                    onCheckedChange = onAutoConnectToggle
                )

                Spacer(modifier = Modifier.height(12.dp))

                // DNS Selector
                DnsSelectorRow(
                    currentDns = sessionState.selectedDns,
                    onDnsSelected = onDnsChange
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section: Anti-Cheat & Security Integrity Status
        SectionHeader(
            title = "سامانه امنیت و ضد هک برنامه",
            subtitle = "پایش پیوسته و جلوگیری از دورزدن و تقلب",
            icon = Icons.Default.Security
        )

        Spacer(modifier = Modifier.height(10.dp))

        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SecurityStatusRow(
                    label = "وضعیت روت و دستکاری سیستم:",
                    status = if (sessionState.isDeviceRooted) "روت شناسایی شد (هشدار)" else "امن و استاندارد ✅",
                    isWarning = sessionState.isDeviceRooted
                )
                SecurityStatusRow(
                    label = "اعتبارسنجی آن‌چین ترون:",
                    status = "فعال (TronScan Mainnet)",
                    isWarning = false
                )
                SecurityStatusRow(
                    label = "امضای ضد تقلب VIP (HMAC):",
                    status = "فعال و ایمن",
                    isWarning = false
                )
                SecurityStatusRow(
                    label = "محافظت ضد تغییر ساعت (Anti-Time-Travel):",
                    status = "فعال",
                    isWarning = false
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // App Info
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(tokens.accentGlow.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = tokens.accentGlow,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Aria VPN • نسخه ۲.۴.۰",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "پروکسی اختصاصی بدون ثبت لاگ (Zero Logs Policy)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val tokens = LocalContrastTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(tokens.accentGlow.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tokens.accentGlow,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ContrastOptionRow(
    mode: ContrastMode,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val tokens = LocalContrastTokens.current
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) tokens.accentGlow.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(
            tokens.cardBorderWidth,
            if (isSelected) tokens.accentGlow else Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("contrast_mode_${mode.name}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mode.titleFa,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = mode.descFa,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = tokens.accentGlow)
            )
        }
    }
}

@Composable
private fun ProtocolOptionRow(
    protocol: VpnProtocol,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val tokens = LocalContrastTokens.current
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) tokens.accentGlow.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(
            tokens.cardBorderWidth,
            if (isSelected) tokens.accentGlow else Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("protocol_${protocol.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = protocol.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (protocol.isRecommended) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF00E676))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "پیشنهادی",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = Color.Black
                            )
                        }
                    }
                }
                Text(
                    text = protocol.descriptionFa,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = tokens.accentGlow)
            )
        }
    }
}

@Composable
private fun SwitchOptionRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val tokens = LocalContrastTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = tokens.accentGlow
            )
        )
    }
}

@Composable
private fun DnsSelectorRow(
    currentDns: String,
    onDnsSelected: (String) -> Unit
) {
    val tokens = LocalContrastTokens.current
    val dnsList = listOf(
        "Cloudflare (1.1.1.1)",
        "Google (8.8.8.8)",
        "Quad9 Security (9.9.9.9)",
        "AdGuard Anti-Ad"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "سرور DNS رمزنگاری‌شده:",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            dnsList.take(2).forEach { dns ->
                Surface(
                    onClick = { onDnsSelected(dns) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (currentDns == dns) tokens.accentGlow.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(
                        tokens.cardBorderWidth,
                        if (currentDns == dns) tokens.accentGlow else Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = dns,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityStatusRow(
    label: String,
    status: String,
    isWarning: Boolean
) {
    val tokens = LocalContrastTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (isWarning) Color(0xFFFF3D71) else tokens.successColor
        )
    }
}

