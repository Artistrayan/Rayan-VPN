package com.example.vpn

import android.content.Context
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Statistics
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Adapter over WireGuard's official Android userspace backend. */
class WireGuardTunnelController(context: Context) {
    private val backend = GoBackend(context.applicationContext)
    private val tunnel = object : Tunnel {
        override fun getName(): String = TUNNEL_NAME
        override fun onStateChange(newState: Tunnel.State) = Unit
    }

    suspend fun connect(configuration: String): Tunnel.State = withContext(Dispatchers.IO) {
        val parsed = Config.parse(ByteArrayInputStream(configuration.toByteArray(StandardCharsets.UTF_8)))
        require(parsed.peers.size == 1) { "A device profile must contain exactly one server peer." }
        val state = backend.setState(tunnel, Tunnel.State.UP, parsed)
        check(state == Tunnel.State.UP && backend.getState(tunnel) == Tunnel.State.UP) {
            "WireGuard backend did not confirm the tunnel state."
        }
        state
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        if (backend.getState(tunnel) == Tunnel.State.UP) backend.setState(tunnel, Tunnel.State.DOWN, null)
    }

    fun disconnectBlocking() {
        if (backend.getState(tunnel) == Tunnel.State.UP) backend.setState(tunnel, Tunnel.State.DOWN, null)
    }

    fun isUp(): Boolean = runCatching { backend.getState(tunnel) == Tunnel.State.UP }.getOrDefault(false)
    fun statistics(): Statistics = backend.getStatistics(tunnel)

    fun hasHandshake(): Boolean {
        val stats = backend.getStatistics(tunnel)
        return stats.peers().any { key -> stats.peer(key)?.latestHandshakeEpochMillis()?.let { it > 0L } == true }
    }

    private companion object {
        const val TUNNEL_NAME = "rayan0"
    }
}
