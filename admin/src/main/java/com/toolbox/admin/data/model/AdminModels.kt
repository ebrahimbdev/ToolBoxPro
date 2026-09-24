package com.toolbox.admin.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AdminStats(
    @SerialName("total_users") val totalUsers: Int = 0,
    @SerialName("active_users_7d") val activeUsers7d: Int = 0,
    @SerialName("premium_users") val premiumUsers: Int = 0,
    @SerialName("pending_payments") val pendingPayments: Int = 0,
    @SerialName("total_ad_views") val totalAdViews: Int = 0,
    @SerialName("total_tool_opens") val totalToolOpens: Int = 0,
    @SerialName("confirmed_revenue") val confirmedRevenue: Long = 0
)

@Serializable
data class AdminUser(
    val id: String = "",
    @SerialName("device_id") val deviceId: String = "",
    val username: String? = null,
    val brand: String? = null,
    val model: String? = null,
    @SerialName("os_version") val osVersion: String? = null,
    @SerialName("app_version") val appVersion: String? = null,
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("last_seen") val lastSeen: Long = 0,
    @SerialName("ad_views") val adViews: Int = 0,
    @SerialName("tool_opens") val toolOpens: Int = 0,
    @SerialName("is_blocked") val isBlocked: Int = 0
)

@Serializable
data class AdminUsersResponse(
    val users: List<AdminUser> = emptyList(),
    val total: Int = 0
)

@Serializable
data class AdminPayment(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val amount: Int = 0,
    val method: String = "",
    val status: String = "",
    val plan: String = "",
    @SerialName("gateway_ref") val gatewayRef: String? = null,
    @SerialName("tx_hash") val txHash: String? = null,
    val note: String? = null,
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("confirmed_at") val confirmedAt: Long? = null,
    val username: String? = null,
    @SerialName("device_id") val deviceId: String? = null,
    val model: String? = null
)

@Serializable
data class AdminPaymentsResponse(
    val payments: List<AdminPayment> = emptyList()
)

@Serializable
data class AdminConfig(
    val key: String = "app",
    @SerialName("ad_interval_seconds") val adIntervalSeconds: Int = 45,
    @SerialName("banner_enabled") val bannerEnabled: Int = 1,
    @SerialName("interstitial_enabled") val interstitialEnabled: Int = 1,
    @SerialName("free_ad_multiplier") val freeAdMultiplier: Int = 2,
    @SerialName("subscription_price_toman") val subscriptionPriceToman: Int = 150000,
    @SerialName("subscription_duration_days") val subscriptionDurationDays: Int = 30,
    @SerialName("card_number") val cardNumber: String = "",
    @SerialName("card_holder") val cardHolder: String = "",
    @SerialName("crypto_wallet") val cryptoWallet: String = "",
    @SerialName("crypto_network") val cryptoNetwork: String = "TRC20",
    @SerialName("gateway_enabled") val gatewayEnabled: Int = 0,
    @SerialName("gateway_status_note") val gatewayStatusNote: String = "coming soon",
    @SerialName("updated_at") val updatedAt: Long = 0
)

@Serializable
data class ConfigResponse(val config: AdminConfig? = null)

@Serializable
data class OkResponse(val ok: Boolean = false)

@Serializable
data class GrantRequest(
    @SerialName("duration_days") val durationDays: Int = 30,
    val price: Int = 0,
    val plan: String = "monthly"
)

@Serializable
data class BlockRequest(val blocked: Int = 1)

@Serializable
data class UsernameRequest(val username: String)

@Serializable
data class ConfigPatch(
    @SerialName("ad_interval_seconds") val adIntervalSeconds: Int? = null,
    @SerialName("banner_enabled") val bannerEnabled: Int? = null,
    @SerialName("interstitial_enabled") val interstitialEnabled: Int? = null,
    @SerialName("free_ad_multiplier") val freeAdMultiplier: Int? = null,
    @SerialName("subscription_price_toman") val subscriptionPriceToman: Int? = null,
    @SerialName("subscription_duration_days") val subscriptionDurationDays: Int? = null,
    @SerialName("card_number") val cardNumber: String? = null,
    @SerialName("card_holder") val cardHolder: String? = null,
    @SerialName("crypto_wallet") val cryptoWallet: String? = null,
    @SerialName("crypto_network") val cryptoNetwork: String? = null,
    @SerialName("gateway_enabled") val gatewayEnabled: Int? = null,
    @SerialName("gateway_status_note") val gatewayStatusNote: String? = null
)

@Serializable
data class UserResponse(val user: AdminUser? = null)

@Serializable
data class PaymentResponse(val payment: AdminPayment? = null)

@Serializable
data class ErrorResponse(val error: String = "")

object AdminJson {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
}
