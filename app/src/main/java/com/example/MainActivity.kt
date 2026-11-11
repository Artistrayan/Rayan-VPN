package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppHeader
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ServersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SubscriptionScreen
import com.example.ui.theme.AriaVpnTheme
import com.example.ui.theme.LocalContrastTokens
import com.example.vpn.AriaVpnService
import com.example.vpn.VpnStateManager
import com.example.vpn.model.VpnStatus
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        VpnStateManager.initialize(applicationContext)

        setContent {
            val sessionState by VpnStateManager.sessionState.collectAsStateWithLifecycle()
            val servers by VpnStateManager.serversFlow.collectAsStateWithLifecycle()
            val isTestingPings by VpnStateManager.isTestingPings.collectAsStateWithLifecycle()
            val latencyReports by VpnStateManager.latencyReportsFlow.collectAsStateWithLifecycle()

            AriaVpnTheme(contrastMode = sessionState.contrastMode) {
                // Persian is right-to-left
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MainAppScaffold(
                        sessionState = sessionState,
                        servers = servers,
                        latencyReports = latencyReports,
                        isTestingPings = isTestingPings
                    )
                }
            }
        }
    }
}

@Composable
fun MainAppScaffold(
    sessionState: com.example.vpn.model.VpnSessionState,
    servers: List<com.example.vpn.model.VpnServer>,
    latencyReports: List<com.example.vpn.model.RealTimeLatencyReport>,
    isTestingPings: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val tokens = LocalContrastTokens.current

    // Navigation tab index: 0 = Home, 1 = Servers, 2 = VIP Subscription, 3 = Settings
    var selectedTab by remember { mutableIntStateOf(0) }

    // Android VpnService System Confirmation Launcher
    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            AriaVpnService.startVpn(context)
            scope.launch {
                snackbarHostState.showSnackbar("اتصال فیلتر شکن برقرار شد")
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("اجازه سیستم برای اتصال VPN رد شد")
            }
        }
    }

    fun handleConnectToggle() {
        if (sessionState.status == VpnStatus.CONNECTED || sessionState.status == VpnStatus.CONNECTING) {
            AriaVpnService.stopVpn(context)
            scope.launch {
                snackbarHostState.showSnackbar("اتصال فیلتر شکن قطع شد")
            }
        } else {
            // Check real system permission via VpnService.prepare()
            val prepareIntent: Intent? = VpnService.prepare(context)
            if (prepareIntent != null) {
                // Request OS permission
                vpnPrepareLauncher.launch(prepareIntent)
            } else {
                // Permission already granted, start immediately
                AriaVpnService.startVpn(context)
            }
        }
    }

    // Auto connect on launch: measures latency of servers (USA, Netherlands, Germany)
    // and automatically connects to the server with the lowest latency ping measured during initialization.
    LaunchedEffect(Unit) {
        if (sessionState.isAutoConnectEnabled && sessionState.status == VpnStatus.DISCONNECTED) {
            val fastest = VpnStateManager.measureAndSelectFastestOnLaunch()
            snackbarHostState.showSnackbar(
                "⚡ اتصال خودکار: سرور ${fastest.cityFa} با کمترین پینگ (${fastest.pingMs}ms) انتخاب شد"
            )
            val prepareIntent: Intent? = VpnService.prepare(context)
            if (prepareIntent == null) {
                AriaVpnService.startVpn(context)
            } else {
                vpnPrepareLauncher.launch(prepareIntent)
            }
        }
    }

    // Handle Back Press to return to Home Tab
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppHeader(
                currentContrast = sessionState.contrastMode,
                isVip = sessionState.isVipActive,
                onContrastSelected = { mode ->
                    VpnStateManager.setContrastMode(mode)
                    scope.launch {
                        snackbarHostState.showSnackbar("پوسته تباین تغییر یافت: ${mode.titleFa}")
                    }
                },
                onVipClick = {
                    selectedTab = 2 // Switch to VIP Tab
                }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .border(
                        width = tokens.cardBorderWidth,
                        color = tokens.cardBorder
                    )
                    .testTag("bottom_nav_bar"),
                containerColor = tokens.cardBackground,
                tonalElevation = 6.dp
            ) {
                // Tab 0: Home / Connection
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.PowerSettingsNew else Icons.Outlined.PowerSettingsNew,
                            contentDescription = "اتصال"
                        )
                    },
                    label = {
                        Text(
                            text = "اتصال",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = tokens.accentGlow,
                        indicatorColor = tokens.accentGlow,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_item_home")
                )

                // Tab 1: Servers
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.Dns else Icons.Outlined.Dns,
                            contentDescription = "سرورها"
                        )
                    },
                    label = {
                        Text(
                            text = "سرورها",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = tokens.accentGlow,
                        indicatorColor = tokens.accentGlow,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_item_servers")
                )

                // Tab 2: VIP Subscription
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.WorkspacePremium else Icons.Outlined.WorkspacePremium,
                            contentDescription = "اشتراک VIP"
                        )
                    },
                    label = {
                        Text(
                            text = "اشتراک VIP",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = Color(0xFFFFD700),
                        indicatorColor = Color(0xFFFFD700),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_item_vip")
                )

                // Tab 3: Settings
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 3) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "تنظیمات"
                        )
                    },
                    label = {
                        Text(
                            text = "تنظیمات",
                            fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = tokens.accentGlow,
                        indicatorColor = tokens.accentGlow,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_content"
            ) { targetTab ->
                when (targetTab) {
                    0 -> HomeScreen(
                        sessionState = sessionState,
                        onConnectToggle = { handleConnectToggle() },
                        onSelectServerClick = { selectedTab = 1 },
                        onAddRewardMinutes = { nonce, mins ->
                            val success = VpnStateManager.addRewardMinutes(challengeNonce = nonce, minutes = mins, context = context)
                            scope.launch {
                                if (success) {
                                    snackbarHostState.showSnackbar("تبریک! +$mins دقیقه به زمان اتصال شما افزوده شد")
                                } else {
                                    snackbarHostState.showSnackbar("خطای ضد تقلب: چالش زمان تبلیغ نامعتبر است!")
                                }
                            }
                        }
                    )

                    1 -> ServersScreen(
                        servers = servers,
                        selectedServer = sessionState.currentServer,
                        latencyReports = latencyReports,
                        isTestingPings = isTestingPings,
                        isVipActive = sessionState.isVipActive,
                        onSelectServer = { server ->
                            VpnStateManager.selectServer(server)
                            selectedTab = 0
                            scope.launch {
                                snackbarHostState.showSnackbar("سرور انتخاب شد: ${server.cityFa} (${server.countryNameFa})")
                            }
                        },
                        onSetDefaultServer = { serverId ->
                            VpnStateManager.setDefaultServer(serverId)
                            scope.launch {
                                snackbarHostState.showSnackbar("این سرور به عنوان پیش‌فرض ذخیره شد")
                            }
                        },
                        onRefreshPings = {
                            VpnStateManager.refreshAllPings()
                            scope.launch {
                                snackbarHostState.showSnackbar("در حال محاسبه مجدد پینگ سرورها...")
                            }
                        },
                        onSmartConnectFastest = {
                            val fastest = VpnStateManager.selectFastestServer()
                            selectedTab = 0
                            if (sessionState.status != VpnStatus.CONNECTED && sessionState.status != VpnStatus.CONNECTING) {
                                handleConnectToggle()
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar("⚡ اتصال هوشمند به سریع‌ترین سرور: ${fastest.cityFa} (${fastest.countryNameFa})")
                            }
                        },
                        onUpgradeVipClick = {
                            selectedTab = 2
                        }
                    )

                    2 -> SubscriptionScreen(
                        isVipActive = sessionState.isVipActive,
                        vipExpiryDate = sessionState.vipExpiryDate,
                        onVipActivated = { plan ->
                            scope.launch {
                                snackbarHostState.showSnackbar("🎉 اشتراک طلایی با موفقیت فعال شد! دسترسی نامحدود باز است")
                            }
                        }
                    )

                    3 -> SettingsScreen(
                        sessionState = sessionState,
                        onContrastModeChange = { mode ->
                            VpnStateManager.setContrastMode(mode)
                        },
                        onProtocolChange = { protocol ->
                            VpnStateManager.setProtocol(protocol)
                            scope.launch {
                                snackbarHostState.showSnackbar("پروتکل اتصال تغییر یافت: ${protocol.title}")
                            }
                        },
                        onKillSwitchToggle = { enabled ->
                            VpnStateManager.setKillSwitch(enabled)
                            scope.launch {
                                snackbarHostState.showSnackbar(if (enabled) "کیل سوییچ فعال شد" else "کیل سوییچ غیرفعال شد")
                            }
                        },
                        onAutoConnectToggle = { enabled ->
                            VpnStateManager.setAutoConnect(enabled)
                        },
                        onDnsChange = { dns ->
                            VpnStateManager.setDns(dns)
                            scope.launch {
                                snackbarHostState.showSnackbar("DNS به $dns تغییر یافت")
                            }
                        }
                    )
                }
            }
        }
    }
}
