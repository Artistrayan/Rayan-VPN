package com.example.vpn

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import com.example.security.SecurityGuard
import com.example.vpn.model.ContrastMode
import com.example.vpn.model.LiveTrafficStats
import com.example.vpn.model.RealTimeLatencyReport
import com.example.vpn.model.SubscriptionPlan
import com.example.vpn.model.VpnProtocol
import com.example.vpn.model.VpnServer
import com.example.vpn.model.VpnSessionState
import com.example.vpn.model.VpnStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object VpnStateManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var prefs: SharedPreferences

    // Planned regions only. No endpoint is advertised until a real node is provisioned and health-checked.
    val availableServers = listOf(
        VpnServer("de_fra_free", "آلمان", "Germany", "DE", "فرانکفورت", "Frankfurt", "🇩🇪", "", "", 0, 0, false, isDefault = true),
        VpnServer("nl_ams_free", "هلند", "Netherlands", "NL", "آمستردام", "Amsterdam", "🇳🇱", "", "", 0, 0, false),
        VpnServer("us_ny_free", "ایالات متحده آمریکا", "United States", "US", "نیویورک", "New York", "🇺🇸", "", "", 0, 0, false)
    )

    val subscriptionPlans = listOf(
        SubscriptionPlan(
            id = "plan_1month",
            titleFa = "اشتراک ۱ ماهه طلایی",
            durationTextFa = "۳۰ روز دسترسی بدون محدودیت",
            priceFa = "۱۸۰,۰۰۰ تومان",
            priceUsd = "$3.99",
            daysCount = 30
        ),
        SubscriptionPlan(
            id = "plan_3months",
            titleFa = "اشتراک ۳ ماهه پرو",
            durationTextFa = "۹۰ روز اتصال با سرعت ۱۰ گیگابیت",
            priceFa = "۴۵۰,۰۰۰ تومان",
            priceUsd = "$9.99",
            discountBadgeFa = "۲۵٪ تخفیف",
            isPopular = true,
            daysCount = 90
        ),
        SubscriptionPlan(
            id = "plan_1year",
            titleFa = "اشتراک ۱ ساله نامحدود",
            durationTextFa = "۳۶۵ روز دسترسی فول VIP با آی‌پی ثابت",
            priceFa = "۱,۲۰۰,۰۰۰ تومان",
            priceUsd = "$29.99",
            discountBadgeFa = "۵۰٪ تخفیف ویژه",
            daysCount = 365
        )
    )

    private val _serversFlow = MutableStateFlow(availableServers)
    val serversFlow: StateFlow<List<VpnServer>> = _serversFlow.asStateFlow()

    private val _sessionState = MutableStateFlow(
        VpnSessionState(
            currentServer = availableServers.first { it.isDefault }
        )
    )
    val sessionState: StateFlow<VpnSessionState> = _sessionState.asStateFlow()

    private val _isTestingPings = MutableStateFlow(false)
    val isTestingPings: StateFlow<Boolean> = _isTestingPings.asStateFlow()

    // Real-Time Latency Monitor flows for USA, Netherlands, and Germany
    private val pingHistoryMap = mutableMapOf<String, MutableList<Int>>()
    private val _latencyReportsFlow = MutableStateFlow<List<RealTimeLatencyReport>>(emptyList())
    val latencyReportsFlow: StateFlow<List<RealTimeLatencyReport>> = _latencyReportsFlow.asStateFlow()

    private val _fastestServerFlow = MutableStateFlow<VpnServer>(availableServers.first())
    val fastestServerFlow: StateFlow<VpnServer> = _fastestServerFlow.asStateFlow()

    private var latencyMonitorJob: Job? = null

    fun initialize(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences("aria_vpn_prefs", Context.MODE_PRIVATE)

        // Anti-Tamper: Verify VIP cryptographic integrity before trusting storage
        val hasValidVipSignature = SecurityGuard.verifyVipIntegrity(context)
        val isVip = hasValidVipSignature && prefs.getBoolean("is_vip_active", false)
        if (!hasValidVipSignature && prefs.getBoolean("is_vip_active", false)) {
            // Tampering detected! Reset unauthorized VIP state
            prefs.edit().putBoolean("is_vip_active", false).apply()
        }

        // Anti-Time Travel: Check clock integrity
        val isClockValid = SecurityGuard.checkClockIntegrity(context)

        // Security: Root / Hook detection
        val isRooted = SecurityGuard.isDeviceRooted()

        val defaultServerId = prefs.getString("default_server_id", "us_ny_free") ?: "us_ny_free"
        val vipExpiry = if (isVip) (prefs.getString("vip_expiry_date", "") ?: "") else ""
        val savedContrastName = prefs.getString("contrast_mode", ContrastMode.CYBER_DARK.name)
        val contrastMode = try {
            ContrastMode.valueOf(savedContrastName ?: ContrastMode.CYBER_DARK.name)
        } catch (_: Exception) {
            ContrastMode.CYBER_DARK
        }

        val savedProtocolName = prefs.getString("protocol_id", VpnProtocol.WIREGUARD.name)
        val protocol = try {
            VpnProtocol.valueOf(savedProtocolName ?: VpnProtocol.WIREGUARD.name)
        } catch (_: Exception) {
            VpnProtocol.WIREGUARD
        }

        val isKillSwitch = prefs.getBoolean("kill_switch", false)
        val isAutoConnect = prefs.getBoolean("auto_connect", false)
        val dns = prefs.getString("dns", "Cloudflare (1.1.1.1)") ?: "Cloudflare (1.1.1.1)"

        // Update default flag in servers list
        val updatedServers = availableServers.map {
            it.copy(isDefault = it.id == defaultServerId)
        }
        _serversFlow.value = updatedServers

        val initialServer = updatedServers.find { it.id == defaultServerId } ?: updatedServers.first()

        _sessionState.update {
            it.copy(
                currentServer = initialServer,
                isVipActive = isVip,
                vipExpiryDate = vipExpiry,
                contrastMode = contrastMode,
                activeProtocol = protocol,
                isKillSwitchEnabled = isKillSwitch,
                isAutoConnectEnabled = isAutoConnect,
                isDeviceRooted = isRooted,
                selectedDns = dns
            )
        }

        // Test pings on startup and start continuous real-time monitor
        refreshAllPings()
        startRealTimeLatencyMonitor()
    }

    fun selectServer(server: VpnServer) {
        _sessionState.update { it.copy(currentServer = server) }
    }

    fun setDefaultServer(serverId: String) {
        prefs.edit().putString("default_server_id", serverId).apply()
        _serversFlow.update { list ->
            list.map { it.copy(isDefault = it.id == serverId) }
        }
        val defaultServer = _serversFlow.value.find { it.id == serverId }
        if (defaultServer != null && _sessionState.value.status == VpnStatus.DISCONNECTED) {
            _sessionState.update { it.copy(currentServer = defaultServer) }
        }
    }

    fun setContrastMode(mode: ContrastMode) {
        prefs.edit().putString("contrast_mode", mode.name).apply()
        _sessionState.update { it.copy(contrastMode = mode) }
    }

    fun setProtocol(protocol: VpnProtocol) {
        prefs.edit().putString("protocol_id", protocol.name).apply()
        _sessionState.update { it.copy(activeProtocol = protocol) }
    }

    fun setKillSwitch(enabled: Boolean) {
        prefs.edit().putBoolean("kill_switch", enabled).apply()
        _sessionState.update { it.copy(isKillSwitchEnabled = enabled) }
    }

    fun setAutoConnect(enabled: Boolean) {
        prefs.edit().putBoolean("auto_connect", enabled).apply()
        _sessionState.update { it.copy(isAutoConnectEnabled = enabled) }
    }

    fun setDns(dns: String) {
        prefs.edit().putString("dns", dns).apply()
        _sessionState.update { it.copy(selectedDns = dns) }
    }

    fun activateVip(context: Context, planId: String, days: Int, planTitle: String) {
        val expiryText = "تا $days روز آینده (فعال)"
        // Commit HMAC cryptographic signature
        SecurityGuard.commitVerifiedVip(context, planId, days)

        prefs.edit()
            .putBoolean("is_vip_active", true)
            .putString("vip_expiry_date", expiryText)
            .apply()

        _sessionState.update {
            it.copy(
                isVipActive = true,
                vipExpiryDate = expiryText
            )
        }
    }

    fun updateStatus(status: VpnStatus, stepMessage: String = "") {
        _sessionState.update {
            it.copy(
                status = status,
                connectionStepMessage = stepMessage
            )
        }
    }

    fun updateSessionTick(
        remainingSec: Long,
        elapsedSec: Long,
        traffic: LiveTrafficStats,
        assignedIp: String
    ) {
        _sessionState.update {
            it.copy(
                remainingSeconds = remainingSec,
                elapsedSeconds = elapsedSec,
                traffic = traffic,
                assignedIp = assignedIp
            )
        }
    }

    fun updateTunnelStats(traffic: LiveTrafficStats, elapsedSec: Long) {
        _sessionState.update {
            it.copy(traffic = traffic, elapsedSeconds = elapsedSec, assignedIp = "")
        }
    }

    /**
     * Requirement: Add 10 minutes when rewarded ad is watched
     * Anti-Cheat: Requires challenge nonce and minimum watch duration
     */
    @Suppress("UNUSED_PARAMETER")
    fun addRewardMinutes(challengeNonce: String? = null, minutes: Int = 10, context: Context? = null): Boolean {
        // Reward grants remain disabled until a real ad SDK callback is verified server-side.
        return false
    }

    fun resetSessionTimer() {
        _sessionState.update {
            it.copy(
                remainingSeconds = 3600L,
                totalSessionAllowedSeconds = 3600L,
                elapsedSeconds = 0L,
                traffic = LiveTrafficStats()
            )
        }
    }

    fun refreshAllPings() {
        scope.launch {
            _isTestingPings.value = true
            val currentList = _serversFlow.value
            val updated = currentList.map { server ->
                val realPing = measureRealSocketPing(server.pingHost, fallbackMs = server.pingMs)
                server.copy(pingMs = realPing)
            }
            _serversFlow.value = updated
            _isTestingPings.value = false
        }
    }

    /**
     * Performs a real socket connection test to measure network ping/latency in milliseconds.
     */
    private suspend fun measureRealSocketPing(
        host: String,
        @Suppress("UNUSED_PARAMETER") fallbackMs: Int
    ): Int = withContext(Dispatchers.IO) {
        if (host.isBlank()) return@withContext 0
        val startedAt = SystemClock.elapsedRealtime()
        try {
            Socket().use { socket -> socket.connect(InetSocketAddress(host, 443), 1200) }
            (SystemClock.elapsedRealtime() - startedAt).toInt().takeIf { it in 1..5000 } ?: 0
        } catch (_: Exception) {
            0
        }
    }

    fun startRealTimeLatencyMonitor() {
        latencyMonitorJob?.cancel()
        latencyMonitorJob = scope.launch {
            while (isActive) {
                val targets = _serversFlow.value.filter { it.isOperational && it.pingHost.isNotBlank() }
                if (targets.isEmpty()) {
                    _latencyReportsFlow.value = emptyList()
                    delay(3500)
                    continue
                }
                val reports = targets.map { server ->
                    val livePing = measureRealSocketPing(server.pingHost, fallbackMs = 0)
                    val history = pingHistoryMap.getOrPut(server.id) { mutableListOf() }
                    if (livePing > 0) {
                        history.add(livePing)
                        if (history.size > 5) history.removeAt(0)
                    }
                    val jitter = if (history.size > 1) {
                        ((history.maxOrNull() ?: livePing) - (history.minOrNull() ?: livePing)) / 2
                    } else 0
                    val quality = when {
                        livePing == 0 -> "در دسترس نیست"
                        livePing < 45 -> "بسیار عالی"
                        livePing < 75 -> "عالی"
                        livePing < 110 -> "خوب"
                        else -> "متوسط"
                    }
                    RealTimeLatencyReport(
                        server = server.copy(pingMs = livePing),
                        currentPingMs = livePing,
                        jitterMs = jitter,
                        pingHistory = history.toList(),
                        qualityLabelFa = quality
                    )
                }
                val fastest = reports.filter { it.currentPingMs > 0 }.minByOrNull { it.currentPingMs }
                _latencyReportsFlow.value = reports.map {
                    it.copy(isFastest = it.server.id == fastest?.server?.id)
                }
                if (fastest != null) _fastestServerFlow.value = fastest.server
                val pingMap = reports.associate { it.server.id to it.currentPingMs }
                _serversFlow.update { list ->
                    list.map { server -> pingMap[server.id]?.let { server.copy(pingMs = it) } ?: server }
                }
                delay(3500)
            }
        }
    }

    fun selectFastestServer(): VpnServer {
        val fastest = _serversFlow.value
            .filter { it.isOperational && it.pingMs > 0 }
            .minByOrNull { it.pingMs }
            ?: _sessionState.value.currentServer
        selectServer(fastest)
        return fastest
    }

    /**
     * Measures initial network latency of primary servers (USA, Netherlands, Germany)
     * and automatically selects the server with the lowest ping for Auto-Connect on launch.
     */
    suspend fun measureAndSelectFastestOnLaunch(): VpnServer = withContext(Dispatchers.IO) {
        val targets = _serversFlow.value.filter { it.isOperational && it.pingHost.isNotBlank() }
        if (targets.isEmpty()) return@withContext _sessionState.value.currentServer
        val measured = targets.map { server ->
            server.copy(pingMs = measureRealSocketPing(server.pingHost, fallbackMs = 0))
        }
        val fastest = measured.filter { it.pingMs > 0 }.minByOrNull { it.pingMs }
            ?: return@withContext _sessionState.value.currentServer
        selectServer(fastest)
        _fastestServerFlow.value = fastest
        fastest
    }
}
