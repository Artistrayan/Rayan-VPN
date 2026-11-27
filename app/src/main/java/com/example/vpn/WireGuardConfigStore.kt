package com.example.vpn

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.wireguard.config.Config
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts device-specific WireGuard profiles at rest with a non-exportable Android Keystore key. */
class WireGuardConfigStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun save(serverId: String, configuration: String) {
        validateServerId(serverId)
        validateConfiguration(configuration)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(configuration.toByteArray(StandardCharsets.UTF_8))
        val encoded = Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
        check(preferences.edit().putString(storageKey(serverId), encoded).commit()) {
            "Unable to persist encrypted WireGuard configuration."
        }
    }

    fun load(serverId: String): String? {
        validateServerId(serverId)
        val encoded = preferences.getString(storageKey(serverId), null) ?: return null
        return try {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            require(payload.size > IV_LENGTH) { "Encrypted WireGuard configuration is truncated." }
            val iv = payload.copyOfRange(0, IV_LENGTH)
            val ciphertext = payload.copyOfRange(IV_LENGTH, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8).also(::validateConfiguration)
        } catch (error: Exception) {
            preferences.edit().remove(storageKey(serverId)).commit()
            throw IllegalStateException("Stored WireGuard configuration could not be decrypted or validated.", error)
        }
    }

    fun remove(serverId: String) {
        validateServerId(serverId)
        preferences.edit().remove(storageKey(serverId)).apply()
    }

    private fun validateConfiguration(configuration: String) {
        require(configuration.isNotBlank()) { "WireGuard configuration is empty." }
        val parsed = Config.parse(ByteArrayInputStream(configuration.toByteArray(StandardCharsets.UTF_8)))
        require(parsed.peers.isNotEmpty()) { "WireGuard configuration has no peer." }
        require(parsed.peers.size == 1) { "A device configuration must contain exactly one server peer." }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun validateServerId(serverId: String) {
        require(SERVER_ID_PATTERN.matches(serverId)) { "Invalid server identifier." }
    }

    private fun storageKey(serverId: String) = "wireguard_config_$serverId"

    private companion object {
        const val PREFERENCES_NAME = "rayan_vpn_encrypted_tunnel_configs"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "rayan_vpn_wireguard_config_aes"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_LENGTH_BITS = 128
        val SERVER_ID_PATTERN = Regex("[A-Za-z0-9_-]{1,64}")
    }
}
