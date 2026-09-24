package com.toolbox.admin.data.api

import com.toolbox.admin.BuildConfig
import com.toolbox.admin.data.model.AdminConfig
import com.toolbox.admin.data.model.AdminJson
import com.toolbox.admin.data.model.AdminPayment
import com.toolbox.admin.data.model.AdminPaymentsResponse
import com.toolbox.admin.data.model.AdminStats
import com.toolbox.admin.data.model.AdminUser
import com.toolbox.admin.data.model.AdminUsersResponse
import com.toolbox.admin.data.model.BlockRequest
import com.toolbox.admin.data.model.ConfigPatch
import com.toolbox.admin.data.model.ConfigResponse
import com.toolbox.admin.data.model.ErrorResponse
import com.toolbox.admin.data.model.GrantRequest
import com.toolbox.admin.data.model.UserResponse
import com.toolbox.admin.data.model.UsernameRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

sealed class AdminApiResult<out T> {
    data class Success<T>(val data: T) : AdminApiResult<T>()
    data class Failure(val message: String, val code: Int = 0) : AdminApiResult<Nothing>()
}

@Singleton
class AdminApi @Inject constructor() {
    private val baseUrl: String = BuildConfig.API_BASE_URL.trimEnd('/')
    private var adminToken: String = ""

    fun setToken(token: String) {
        adminToken = token.trim()
    }

    fun hasToken(): Boolean = adminToken.isNotBlank()

    private suspend fun request(
        method: String,
        path: String,
        body: String = ""
    ): AdminApiResult<String> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank() || !baseUrl.startsWith("http")) {
            return@withContext AdminApiResult.Failure("API base URL not configured")
        }
        if (adminToken.isBlank()) {
            return@withContext AdminApiResult.Failure("Not logged in", 401)
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
                setRequestProperty("Authorization", "Bearer $adminToken")
            }
            if (body.isNotEmpty()) {
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code in 200..299) {
                AdminApiResult.Success(text)
            } else {
                val msg = runCatching {
                    AdminJson.json.decodeFromString(ErrorResponse.serializer(), text).error
                }.getOrNull() ?: "HTTP $code"
                AdminApiResult.Failure(msg, code)
            }
        } catch (e: IOException) {
            AdminApiResult.Failure(e.message ?: "network error")
        } catch (e: Exception) {
            AdminApiResult.Failure(e.message ?: "error")
        } finally {
            conn?.disconnect()
        }
    }

    private inline fun <reified T> parse(text: String): AdminApiResult<T> {
        return runCatching {
            AdminApiResult.Success(AdminJson.json.decodeFromString<T>(text))
        }.getOrElse { AdminApiResult.Failure(it.message ?: "parse error") }
    }

    suspend fun login(token: String): AdminApiResult<Boolean> {
        setToken(token)
        return when (val r = request("POST", "/admin/v1/login")) {
            is AdminApiResult.Success -> AdminApiResult.Success(true)
            is AdminApiResult.Failure -> {
                if (r.code == 401 || r.code == 403) AdminApiResult.Failure("Invalid token", r.code) else r
            }
        }
    }

    suspend fun stats(): AdminApiResult<AdminStats> =
        when (val r = request("GET", "/admin/v1/stats")) {
            is AdminApiResult.Success -> parse(r.data)
            is AdminApiResult.Failure -> r
        }

    suspend fun users(q: String = "", limit: Int = 50): AdminApiResult<AdminUsersResponse> {
        val path = buildString {
            append("/admin/v1/users?limit=$limit")
            if (q.isNotBlank()) append("&q=${java.net.URLEncoder.encode(q, "UTF-8")}")
        }
        return when (val r = request("GET", path)) {
            is AdminApiResult.Success -> parse(r.data)
            is AdminApiResult.Failure -> r
        }
    }

    suspend fun setUsername(deviceId: String, username: String): AdminApiResult<AdminUser?> {
        val body = AdminJson.json.encodeToString(UsernameRequest.serializer(), UsernameRequest(username))
        val path = "/admin/v1/users/${java.net.URLEncoder.encode(deviceId, "UTF-8")}/username"
        return when (val r = request("PATCH", path, body)) {
            is AdminApiResult.Success -> parse<com.toolbox.admin.data.model.UserResponse>(r.data).let {
                when (it) {
                    is AdminApiResult.Success -> AdminApiResult.Success(it.data.user)
                    is AdminApiResult.Failure -> it
                }
            }
            is AdminApiResult.Failure -> r
        }
    }

    suspend fun setBlocked(deviceId: String, blocked: Boolean): AdminApiResult<Boolean> {
        val body = AdminJson.json.encodeToString(BlockRequest.serializer(), BlockRequest(if (blocked) 1 else 0))
        val path = "/admin/v1/users/${java.net.URLEncoder.encode(deviceId, "UTF-8")}/block"
        return when (request("PATCH", path, body)) {
            is AdminApiResult.Success -> AdminApiResult.Success(true)
            is AdminApiResult.Failure -> AdminApiResult.Failure("block failed")
        }
    }

    suspend fun grantSubscription(deviceId: String, days: Int, price: Int = 0): AdminApiResult<Boolean> {
        val body = AdminJson.json.encodeToString(
            GrantRequest.serializer(),
            GrantRequest(durationDays = days, price = price)
        )
        val path = "/admin/v1/users/${java.net.URLEncoder.encode(deviceId, "UTF-8")}/grant"
        return when (request("POST", path, body)) {
            is AdminApiResult.Success -> AdminApiResult.Success(true)
            is AdminApiResult.Failure -> AdminApiResult.Failure("grant failed")
        }
    }

    suspend fun payments(status: String? = null, limit: Int = 50): AdminApiResult<AdminPaymentsResponse> {
        val path = buildString {
            append("/admin/v1/payments?limit=$limit")
            if (!status.isNullOrBlank()) append("&status=$status")
        }
        return when (val r = request("GET", path)) {
            is AdminApiResult.Success -> parse(r.data)
            is AdminApiResult.Failure -> r
        }
    }

    suspend fun confirmPayment(id: String): AdminApiResult<Boolean> {
        val path = "/admin/v1/payments/${java.net.URLEncoder.encode(id, "UTF-8")}/confirm"
        return when (request("PATCH", path)) {
            is AdminApiResult.Success -> AdminApiResult.Success(true)
            is AdminApiResult.Failure -> AdminApiResult.Failure("confirm failed")
        }
    }

    suspend fun rejectPayment(id: String): AdminApiResult<Boolean> {
        val path = "/admin/v1/payments/${java.net.URLEncoder.encode(id, "UTF-8")}/reject"
        return when (request("PATCH", path)) {
            is AdminApiResult.Success -> AdminApiResult.Success(true)
            is AdminApiResult.Failure -> AdminApiResult.Failure("reject failed")
        }
    }

    suspend fun getConfig(): AdminApiResult<AdminConfig> {
        return when (val r = request("GET", "/admin/v1/config")) {
            is AdminApiResult.Success -> parse<ConfigResponse>(r.data).let {
                when (it) {
                    is AdminApiResult.Success -> AdminApiResult.Success(it.data.config ?: AdminConfig())
                    is AdminApiResult.Failure -> it
                }
            }
            is AdminApiResult.Failure -> r
        }
    }

    suspend fun patchConfig(patch: ConfigPatch): AdminApiResult<AdminConfig> {
        val body = AdminJson.json.encodeToString(ConfigPatch.serializer(), patch)
        return when (val r = request("PATCH", "/admin/v1/config", body)) {
            is AdminApiResult.Success -> parse<ConfigResponse>(r.data).let {
                when (it) {
                    is AdminApiResult.Success -> AdminApiResult.Success(it.data.config ?: AdminConfig())
                    is AdminApiResult.Failure -> it
                }
            }
            is AdminApiResult.Failure -> r
        }
    }
}
