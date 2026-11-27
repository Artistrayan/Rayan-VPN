package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.vpn.model.LiveTrafficStats
import com.example.vpn.model.VpnStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AriaVpnService : Service() {
    companion object {
        const val ACTION_START = "com.example.vpn.ACTION_START"
        const val ACTION_STOP = "com.example.vpn.ACTION_STOP"
        private const val CHANNEL_ID = "rayan_vpn_channel"
        private const val NOTIFICATION_ID = 1001

        fun startVpn(context: Context) {
            val intent = Intent(context, AriaVpnService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stopVpn(context: Context) {
            context.startService(Intent(context, AriaVpnService::class.java).apply { action = ACTION_STOP })
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectionJob: Job? = null
    private var tunnelController: WireGuardTunnelController? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startAsForeground("در حال آماده‌سازی اتصال امن…")
                startWireGuardTunnel()
            }
            ACTION_STOP -> stopWireGuardTunnel()
        }
        return START_NOT_STICKY
    }

    private fun startWireGuardTunnel() {
        connectionJob?.cancel()
        VpnStateManager.updateStatus(VpnStatus.CONNECTING, "در حال بررسی پیکربندی معتبر سرور…")
        connectionJob = serviceScope.launch {
            try {
                val selectedServer = VpnStateManager.sessionState.value.currentServer
                val configuration = WireGuardConfigStore(applicationContext).load(selectedServer.id)
                    ?: throw IllegalStateException(
                        "پیکربندی امن این سرور هنوز از شبکه مرکزی دریافت نشده است. هیچ سرور آزمایشی فعال نیست."
                    )
                val controller = WireGuardTunnelController(applicationContext)
                tunnelController = controller
                VpnStateManager.updateStatus(VpnStatus.CONNECTING, "در حال راه‌اندازی موتور واقعی WireGuard…")
                controller.connect(configuration)
                if (!controller.isUp()) {
                    throw IllegalStateException("موتور WireGuard وضعیت فعال را تأیید نکرد.")
                }
                startAsForeground("تونل راه‌اندازی شد؛ در انتظار تأیید handshake سرور…")
                monitorTunnel(controller)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                runCatching { tunnelController?.disconnect() }
                tunnelController = null
                VpnStateManager.updateStatus(VpnStatus.DISCONNECTED, error.message ?: "اتصال WireGuard برقرار نشد.")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private suspend fun monitorTunnel(controller: WireGuardTunnelController) {
        val startedAt = SystemClock.elapsedRealtime()
        val handshakeDeadline = startedAt + 30_000L
        var handshakeConfirmed = false
        var previousRx = 0L
        var previousTx = 0L
        var previousSampleAt = startedAt

        while (serviceScope.isActive && controller.isUp()) {
            val stats = controller.statistics()
            val receivedBytes = stats.totalRx().coerceAtLeast(0L)
            val transmittedBytes = stats.totalTx().coerceAtLeast(0L)
            val now = SystemClock.elapsedRealtime()
            val elapsedMs = (now - previousSampleAt).coerceAtLeast(1L)

            if (!handshakeConfirmed && controller.hasHandshake()) {
                handshakeConfirmed = true
                VpnStateManager.updateStatus(VpnStatus.CONNECTED, "تونل WireGuard و handshake سرور تأیید شدند.")
                updateNotification("اتصال WireGuard فعال است")
            }
            if (!handshakeConfirmed && now >= handshakeDeadline) {
                throw IllegalStateException("تونل ایجاد شد، اما از سرور handshake دریافت نشد. پیکربندی یا دسترسی سرور را بررسی کنید.")
            }

            val downloadRate = ((receivedBytes - previousRx).coerceAtLeast(0L) * 1000L) / elapsedMs
            val uploadRate = ((transmittedBytes - previousTx).coerceAtLeast(0L) * 1000L) / elapsedMs
            VpnStateManager.updateTunnelStats(
                LiveTrafficStats(
                    downloadSpeedBps = downloadRate,
                    uploadSpeedBps = uploadRate,
                    totalDownloadBytes = receivedBytes,
                    totalUploadBytes = transmittedBytes,
                    pingMs = 0,
                    jitterMs = 0
                ),
                (now - startedAt) / 1000L
            )
            previousRx = receivedBytes
            previousTx = transmittedBytes
            previousSampleAt = now
            delay(1000L)
        }
        if (!controller.isUp()) throw IllegalStateException("تونل WireGuard متوقف شده است.")
    }

    private fun stopWireGuardTunnel() {
        connectionJob?.cancel()
        serviceScope.launch {
            runCatching { tunnelController?.disconnect() }
            tunnelController = null
            VpnStateManager.updateStatus(VpnStatus.DISCONNECTED, "اتصال قطع شد.")
            VpnStateManager.resetSessionTimer()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startAsForeground(message: String) {
        val notification = buildNotification(message)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(message: String) {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, buildNotification(message))
    }

    private fun buildNotification(message: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stopIntent = Intent(this, AriaVpnService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Rayan VPN")
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "قطع اتصال", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, getString(R.string.vpn_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = getString(R.string.vpn_channel_desc)
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        connectionJob?.cancel()
        runCatching { tunnelController?.disconnectBlocking() }
        tunnelController = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
