package com.example.vpn.model

enum class VpnStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}

enum class ContrastMode(val titleFa: String, val titleEn: String, val descFa: String) {
    CYBER_DARK("تاریک سایبر", "Cyber Dark", "پوسته تیره مدرن با نورهای نئونی"),
    HIGH_CONTRAST("تباین بسیار بالا", "Ultra High Contrast", "کنتراست حداکثری AMOLED و لبه‌های سفید برای بالاترین خوانایی"),
    CLEAN_LIGHT("روشن و شفاف", "Clean Light", "پوسته روشن با کنتراست متعادل و شفاف")
}

enum class VpnProtocol(
    val id: String,
    val title: String,
    val descriptionFa: String,
    val isRecommended: Boolean = false
) {
    WIREGUARD(
        "wireguard",
        "WireGuard Ultra",
        "پروتکل نسل جدید، بسیار سبک، کم‌مصرف و فوق‌سریع بر پایه رمزنگاری ChaCha20",
        true
    ),
    V2RAY(
        "v2ray",
        "V2Ray / VLESS + TLS",
        "تکنولوژی هوشمند ضد فیلتر با شبیه‌سازی ترافیک معمولی وب و عبور از فیلترینگ عمیق",
        false
    ),
    SHADOWSOCKS(
        "shadowsocks",
        "Shadowsocks AEAD",
        "پروتکل سبک با رمزنگاری مدرن و تاخیر کم",
        false
    ),
    OPENVPN(
        "openvpn",
        "OpenVPN UDP",
        "استاندارد جهانی امن با پایداری تضمین‌شده",
        false
    )
}

data class VpnServer(
    val id: String,
    val countryNameFa: String,
    val countryNameEn: String,
    val countryCode: String,
    val cityFa: String,
    val cityEn: String,
    val flagEmoji: String,
    val serverAddress: String,
    val pingHost: String,
    val pingMs: Int,
    val loadPercentage: Int,
    val isVip: Boolean,
    val isDefault: Boolean = false,
    val supportedProtocols: List<VpnProtocol> = listOf(
        VpnProtocol.WIREGUARD,
        VpnProtocol.V2RAY,
        VpnProtocol.SHADOWSOCKS
    )
)

data class RealTimeLatencyReport(
    val server: VpnServer,
    val currentPingMs: Int,
    val jitterMs: Int = 2,
    val pingHistory: List<Int> = emptyList(),
    val isFastest: Boolean = false,
    val qualityLabelFa: String = "عالی"
)

data class LiveTrafficStats(
    val downloadSpeedBps: Long = 0L,
    val uploadSpeedBps: Long = 0L,
    val totalDownloadBytes: Long = 0L,
    val totalUploadBytes: Long = 0L,
    val pingMs: Int = 45,
    val jitterMs: Int = 3
)

data class VpnSessionState(
    val status: VpnStatus = VpnStatus.DISCONNECTED,
    val currentServer: VpnServer,
    val remainingSeconds: Long = 3600L, // 1 hour initial
    val totalSessionAllowedSeconds: Long = 3600L,
    val elapsedSeconds: Long = 0L,
    val isVipActive: Boolean = false,
    val vipExpiryDate: String = "",
    val traffic: LiveTrafficStats = LiveTrafficStats(),
    val activeProtocol: VpnProtocol = VpnProtocol.WIREGUARD,
    val assignedIp: String = "185.220.101.45",
    val contrastMode: ContrastMode = ContrastMode.CYBER_DARK,
    val isKillSwitchEnabled: Boolean = false,
    val isAutoConnectEnabled: Boolean = false,
    val isDeviceRooted: Boolean = false,
    val selectedDns: String = "Cloudflare (1.1.1.1)",
    val connectionStepMessage: String = ""
)

data class SubscriptionPlan(
    val id: String,
    val titleFa: String,
    val durationTextFa: String,
    val priceFa: String,
    val priceUsd: String,
    val discountBadgeFa: String? = null,
    val isPopular: Boolean = false,
    val daysCount: Int
)
