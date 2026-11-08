package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

object SecurityGuard {

    private const val PREFS_SECURITY = "aria_vpn_security_prefs"
    private const val KEY_USED_TXIDS = "used_blockchain_txids"
    private const val KEY_VIP_SIGNATURE = "tamper_proof_vip_signature"
    private const val KEY_VIP_PLAN_ID = "tamper_proof_plan_id"
    private const val KEY_VIP_EXPIRY_TS = "tamper_proof_expiry_ts"
    private const val KEY_LAST_KNOWN_REALTIME = "last_known_realtime"
    private const val KEY_LAST_KNOWN_WALL_CLOCK = "last_known_wall_clock"

    // Internal secret seed combined with device identity for tamper-proof HMAC
    private val HMAC_SALT = "AriaVpn_AntiCheat_SecToken_2026_@#9812_TBMvBiVB".toByteArray(StandardCharsets.UTF_8)

    // Anti-replay ad reward nonce
    private var currentAdChallengeNonce: String? = null
    private var adChallengeStartTime: Long = 0L

    /**
     * Checks if the device appears rooted, has su binaries, or running under dangerous environments.
     */
    fun isDeviceRooted(): Boolean {
        // 1. Check build tags
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        // 2. Check common root & superuser binary paths
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/magisk/.core/bin/su"
        )
        for (path in paths) {
            try {
                if (File(path).exists()) return true
            } catch (_: Exception) { }
        }

        return false
    }

    /**
     * Computes a cryptographic HMAC-SHA256 signature binding the device ID, plan, and expiry time.
     */
    fun createVipSignature(context: Context, planId: String, expiryTimestamp: Long): String {
        val deviceId = getDeviceFingerprint(context)
        val payload = "$deviceId:$planId:$expiryTimestamp:VALID_VIP_TOKEN"
        return hmacSha256(payload, HMAC_SALT)
    }

    /**
     * Verifies that the local VIP status has not been tampered with or modified via preferences editor.
     */
    fun verifyVipIntegrity(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
        val savedSig = prefs.getString(KEY_VIP_SIGNATURE, null) ?: return false
        val planId = prefs.getString(KEY_VIP_PLAN_ID, "") ?: return false
        val expiryTs = prefs.getLong(KEY_VIP_EXPIRY_TS, 0L)

        // Check if expired
        val now = System.currentTimeMillis()
        if (now > expiryTs) {
            revokeVip(context)
            return false
        }

        // Recompute signature and verify
        val expectedSig = createVipSignature(context, planId, expiryTs)
        val isValid = constantTimeEquals(savedSig, expectedSig)
        if (!isValid) {
            revokeVip(context)
        }
        return isValid
    }

    /**
     * Securely commits a verified VIP activation to storage with cryptographic signature.
     */
    fun commitVerifiedVip(context: Context, planId: String, durationDays: Int) {
        val prefs = context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
        val expiryTs = System.currentTimeMillis() + (durationDays.toLong() * 24 * 60 * 60 * 1000)
        val signature = createVipSignature(context, planId, expiryTs)

        prefs.edit()
            .putString(KEY_VIP_SIGNATURE, signature)
            .putString(KEY_VIP_PLAN_ID, planId)
            .putLong(KEY_VIP_EXPIRY_TS, expiryTs)
            .apply()
    }

    fun revokeVip(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_VIP_SIGNATURE)
            .remove(KEY_VIP_PLAN_ID)
            .remove(KEY_VIP_EXPIRY_TS)
            .apply()
    }

    /**
     * Anti-Replay: Verifies that a transaction hash has never been used before.
     */
    fun isTxidUsed(context: Context, txid: String): Boolean {
        val cleanTxid = txid.trim().lowercase()
        val prefs = context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
        val usedSet = prefs.getStringSet(KEY_USED_TXIDS, emptySet()) ?: emptySet()
        val hash = sha256(cleanTxid)
        return usedSet.contains(hash)
    }

    /**
     * Marks a blockchain TXID as consumed to prevent replay attack.
     */
    fun recordUsedTxid(context: Context, txid: String) {
        val cleanTxid = txid.trim().lowercase()
        val prefs = context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
        val usedSet = prefs.getStringSet(KEY_USED_TXIDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        usedSet.add(sha256(cleanTxid))
        prefs.edit().putStringSet(KEY_USED_TXIDS, usedSet).apply()
    }

    /**
     * Anti-Time-Travel: Validates that the system clock has not been manually rolled back to spoof timers.
     */
    fun checkClockIntegrity(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
        val nowWallClock = System.currentTimeMillis()
        val nowRealtime = SystemClock.elapsedRealtime()

        val lastWallClock = prefs.getLong(KEY_LAST_KNOWN_WALL_CLOCK, 0L)
        val lastRealtime = prefs.getLong(KEY_LAST_KNOWN_REALTIME, 0L)

        if (lastWallClock != 0L && lastRealtime != 0L) {
            val realtimeDelta = nowRealtime - lastRealtime
            val wallDelta = nowWallClock - lastWallClock

            // If wall clock rolled backwards by more than 2 minutes while realtime continued forward
            if (wallDelta < -120_000L && realtimeDelta > 0) {
                return false // Clock tampering detected!
            }
        }

        prefs.edit()
            .putLong(KEY_LAST_KNOWN_WALL_CLOCK, nowWallClock)
            .putLong(KEY_LAST_KNOWN_REALTIME, nowRealtime)
            .apply()

        return true
    }

    /**
     * Generates a one-time challenge nonce before presenting an ad.
     */
    fun generateAdChallenge(): String {
        adChallengeStartTime = SystemClock.elapsedRealtime()
        val nonce = "nonce_" + Random.nextLong(1000000000L, 9999999999L)
        currentAdChallengeNonce = nonce
        return nonce
    }

    /**
     * Verifies that the ad challenge is valid and that sufficient real elapsed time (minimum 4.5 seconds)
     * passed before granting reward, preventing instant bypass injection.
     */
    fun verifyAdReward(challengeNonce: String): Boolean {
        if (currentAdChallengeNonce == null || currentAdChallengeNonce != challengeNonce) {
            return false
        }
        val elapsed = SystemClock.elapsedRealtime() - adChallengeStartTime
        currentAdChallengeNonce = null // Consume nonce
        return elapsed >= 4500L
    }

    private fun getDeviceFingerprint(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "device_default"
        val model = Build.MODEL ?: "unknown"
        val hardware = Build.HARDWARE ?: "unknown"
        return sha256("$androidId:$model:$hardware")
    }

    private fun hmacSha256(data: String, key: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        val result = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return bytesToHex(result)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return bytesToHex(hash)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
