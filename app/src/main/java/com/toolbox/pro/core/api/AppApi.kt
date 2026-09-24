package com.toolbox.pro.core.api

import com.toolbox.pro.BuildConfig
import com.toolbox.pro.core.identity.DeviceIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val message: String, val code: Int = 0) : ApiResult<Nothing>()
}

@Singleton
class AppApi @Inject constructor() {
    private val baseUrl: String = BuildConfig.API_BASE_URL.trimEnd('/')
    private val appKey: String = BuildConfig.APP_KEY
    private val hmacSecret: String = BuildConfig.APP_HMAC_SECRET

    private fun sha256Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun hmacHex(message: String): String {
        if (hmacSecret.isBlank()) return "dev"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(hmacSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(message.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun signedHeaders(deviceId: String, method: String, path: String, body: String): Map<String, String> {
        val timestamp = System.currentTimeMillis().toString()
        val bodyHash = sha256Hex(body)
        val message = "$method\n$path\n$timestamp\n$deviceId\n$bodyHash"
        val signature = hmacHex(message)
        return mapOf(
            "X-Device-Id" to deviceId,
            "X-App-Key" to appKey,
            "X-Timestamp" to timestamp,
            "X-Signature" to signature
        )
    }

    private suspend fun request(
        method: String,
        path: String,
        identity: DeviceIdentity,
        body: String = ""
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank() || !baseUrl.startsWith("http")) {
            return@withContext ApiResult.Failure("API base URL not configured", 0)
        }
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$baseUrl$path")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 12_000
                readTimeout = 15_000
                doInput = true
                if (body.isNotEmpty()) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setFixedLengthStreamingMode(body.toByteArray(Charsets.UTF_8).size)
                }
                setRequestProperty("Accept", "application/json")
                signedHeaders(identity.deviceId, method, path, body).forEach { (k, v) ->
                    setRequestProperty(k, v)
                }
            }
            if (body.isNotEmpty()) {
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code in 200..299) {
                ApiResult.Success(text)
            } else {
                val msg = runCatching {
                    AppJson.json.decodeFromString(ApiErrorResponse.serializer(), text).error
                }.getOrNull() ?: "HTTP $code"
                ApiResult.Failure(msg, code)
            }
        } catch (e: IOException) {
            ApiResult.Failure(e.message ?: "network error", 0)
        } catch (e: Exception) {
            ApiResult.Failure(e.message ?: "error", 0)
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun register(identity: DeviceIdentity): ApiResult<AppUserDto> {
        val body = AppJson.json.encodeToString(
            stringMapSerializer(),
            buildMap<String, String> {
                put("username", identity.username)
                put("brand", identity.brand)
                put("model", identity.model)
                put("os_version", identity.osVersion)
                put("app_version", identity.appVersion)
            }
        )
        return when (val r = request("POST", "/api/v1/register", identity, body)) {
            is ApiResult.Success -> parseUser(r.data)
            is ApiResult.Failure -> r
        }
    }

    suspend fun heartbeat(
        identity: DeviceIdentity,
        adViews: Int = 0,
        toolOpens: Int = 0
    ): ApiResult<AppUserDto> {
        val body = AppJson.json.encodeToString(
            stringMapSerializer(),
            buildMap<String, String> {
                put("username", identity.username)
                put("app_version", identity.appVersion)
                put("ad_views", adViews.toString())
                put("tool_opens", toolOpens.toString())
            }
        )
        return when (val r = request("POST", "/api/v1/heartbeat", identity, body)) {
            is ApiResult.Success -> parseUser(r.data)
            is ApiResult.Failure -> r
        }
    }

    suspend fun getConfig(identity: DeviceIdentity): ApiResult<RemoteConfigDto> {
        return when (val r = request("GET", "/api/v1/config", identity)) {
            is ApiResult.Success -> runCatching {
                ApiResult.Success(AppJson.json.decodeFromString(RemoteConfigDto.serializer(), r.data))
            }.getOrElse { ApiResult.Failure(it.message ?: "parse error") }
            is ApiResult.Failure -> r
        }
    }

    suspend fun createPayment(
        identity: DeviceIdentity,
        method: String,
        amount: Int,
        plan: String = "monthly",
        txHash: String = "",
        note: String = ""
    ): ApiResult<PaymentCreateResponse> {
        val body = AppJson.json.encodeToString(
            PaymentCreateRequest.serializer(),
            PaymentCreateRequest(method = method, amount = amount, plan = plan, txHash = txHash, note = note)
        )
        return when (val r = request("POST", "/api/v1/payments/create", identity, body)) {
            is ApiResult.Success -> runCatching {
                ApiResult.Success(AppJson.json.decodeFromString(PaymentCreateResponse.serializer(), r.data))
            }.getOrElse { ApiResult.Failure(it.message ?: "parse error") }
            is ApiResult.Failure -> r
        }
    }

    suspend fun getSubscription(identity: DeviceIdentity): ApiResult<SubscriptionResponse> {
        return when (val r = request("GET", "/api/v1/subscription", identity)) {
            is ApiResult.Success -> runCatching {
                ApiResult.Success(AppJson.json.decodeFromString(SubscriptionResponse.serializer(), r.data))
            }.getOrElse { ApiResult.Failure(it.message ?: "parse error") }
            is ApiResult.Failure -> r
        }
    }

    private fun parseUser(raw: String): ApiResult<AppUserDto> {
        return runCatching {
            ApiResult.Success(AppJson.json.decodeFromString(AppUserDto.serializer(), raw))
        }.getOrElse { ApiResult.Failure(it.message ?: "parse error") }
    }

    private fun stringMapSerializer(): kotlinx.serialization.KSerializer<Map<String, String>> {
        return kotlinx.serialization.builtins.MapSerializer(
            kotlinx.serialization.serializer<String>(),
            kotlinx.serialization.serializer<String>()
        )
    }
}
