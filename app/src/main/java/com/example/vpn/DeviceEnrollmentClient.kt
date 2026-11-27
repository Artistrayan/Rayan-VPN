package com.example.vpn

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Anonymous device enrollment client.
 * The request contains only an installation identifier and WireGuard public key.
 */
class DeviceEnrollmentClient(
    private val store: DeviceEnrollmentStore,
    controlPlaneBaseUrl: String = BuildConfig.CONTROL_PLANE_BASE_URL,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) {
    data class DeviceCredential(val deviceId: String, val token: String)

    private val enrollmentUrl: String = normalizeBaseUrl(controlPlaneBaseUrl) + "/api/v1/devices/enroll"

    suspend fun enrollIfNeeded(): DeviceCredential = withContext(Dispatchers.IO) {
        val existingToken = store.getDeviceToken()
        if (existingToken != null) {
            return@withContext DeviceCredential(deviceId = "already-enrolled", token = existingToken)
        }

        val material = store.getOrCreateMaterial()
        val payload = JSONObject()
            .put("installationId", material.installationId)
            .put("platform", "android")
            .put("wireGuardPublicKey", material.wireGuardPublicKey)

        val request = Request.Builder()
            .url(enrollmentUrl)
            .header("Accept", "application/json")
            .header("Cache-Control", "no-store")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val errorCode = runCatching { JSONObject(responseText).optString("error") }.getOrDefault("unknown")
                throw IOException("Device enrollment failed (HTTP ${response.code}, ${errorCode.ifBlank { "unknown" }}).")
            }
            val responseJson = JSONObject(responseText)
            val deviceId = responseJson.getString("deviceId")
            val token = responseJson.getString("deviceToken")
            require(deviceId.isNotBlank()) { "Control plane returned an empty device identifier." }
            store.saveDeviceToken(token)
            DeviceCredential(deviceId, token)
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun normalizeBaseUrl(value: String): String {
            val trimmed = value.trim().trimEnd('/')
            require(trimmed.isNotBlank()) { "Rayan VPN control-plane URL is not configured." }
            val parsed = java.net.URI(trimmed)
            require(parsed.scheme == "https" || parsed.host in setOf("localhost", "10.0.2.2")) {
                "The control plane must use HTTPS."
            }
            require(parsed.userInfo == null && parsed.query == null && parsed.fragment == null) {
                "The control-plane URL contains unsupported components."
            }
            return trimmed
        }
    }
}
