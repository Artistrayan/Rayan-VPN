package com.example.vpn

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the installation identity, WireGuard private key, and API device token
 * in one AES-GCM encrypted record protected by a non-exportable Android Keystore key.
 *
 * Only [EnrollmentMaterial.wireGuardPublicKey] is intended to leave this device.
 */
class DeviceEnrollmentStore(context: Context) {
    data class EnrollmentMaterial(
        val installationId: String,
        val wireGuardPublicKey: String,
        val deviceToken: String?
    )

    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    @Synchronized
    fun getOrCreateMaterial(): EnrollmentMaterial {
        val existing = readRecord()
        if (existing != null) return existing.toMaterial()

        val keyPair = KeyPair()
        val installationBytes = ByteArray(32).also(SecureRandom()::nextBytes)
        val record = JSONObject()
            .put("installationId", Base64.encodeToString(installationBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            .put("wireGuardPrivateKey", keyPair.privateKey.toBase64())
            .put("deviceToken", JSONObject.NULL)
        persist(record)
        installationBytes.fill(0)
        return record.toMaterial()
    }

    @Synchronized
    fun saveDeviceToken(token: String) {
        require(token.matches(Regex("[A-Za-z0-9_-]{40,64}"))) { "Invalid device credential." }
        val record = readRecord() ?: throw IllegalStateException("Device identity has not been generated.")
        record.put("deviceToken", token)
        persist(record)
    }

    @Synchronized
    fun getDeviceToken(): String? = readRecord()?.optString("deviceToken")?.takeIf { it.isNotBlank() && it != "null" }

    private fun JSONObject.toMaterial(): EnrollmentMaterial {
        val privateKey = getString("wireGuardPrivateKey")
        val keyPair = KeyPair(Key.fromBase64(privateKey))
        return EnrollmentMaterial(
            installationId = getString("installationId"),
            wireGuardPublicKey = keyPair.publicKey.toBase64(),
            deviceToken = optString("deviceToken").takeIf { it.isNotBlank() && it != "null" }
        )
    }

    private fun readRecord(): JSONObject? {
        val encoded = preferences.getString(RECORD_KEY, null) ?: return null
        return try {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            require(payload.size > IV_BYTES) { "Encrypted device identity is truncated." }
            val iv = payload.copyOfRange(0, IV_BYTES)
            val ciphertext = payload.copyOfRange(IV_BYTES, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateEncryptionKey(), GCMParameterSpec(TAG_BITS, iv))
            JSONObject(String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8))
        } catch (error: Exception) {
            preferences.edit().remove(RECORD_KEY).commit()
            throw IllegalStateException("Device identity could not be decrypted. Re-enrollment is required.", error)
        }
    }

    private fun persist(record: JSONObject) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateEncryptionKey())
        val ciphertext = cipher.doFinal(record.toString().toByteArray(StandardCharsets.UTF_8))
        val encoded = Base64.encodeToString(cipher.iv + ciphertext, Base64.NO_WRAP)
        check(preferences.edit().putString(RECORD_KEY, encoded).commit()) {
            "Unable to save encrypted device identity."
        }
    }

    private fun getOrCreateEncryptionKey(): SecretKey {
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

    private companion object {
        const val PREFERENCES = "rayan_vpn_device_identity"
        const val RECORD_KEY = "encrypted_identity"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "rayan_vpn_device_identity_aes"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
