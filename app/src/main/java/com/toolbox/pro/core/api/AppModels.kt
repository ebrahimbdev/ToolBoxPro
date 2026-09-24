package com.toolbox.pro.core.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AppUserDto(
    @SerialName("device_id") val deviceId: String = "",
    val username: String = "",
    val brand: String = "",
    val model: String = "",
    @SerialName("os_version") val osVersion: String = "",
    @SerialName("app_version") val appVersion: String = "",
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("last_seen") val lastSeen: Long = 0,
    @SerialName("ad_views") val adViews: Int = 0,
    @SerialName("tool_opens") val toolOpens: Int = 0,
    val premium: Boolean = false,
    val subscription: AppSubscriptionDto? = null,
    val config: AppConfigDto? = null
)

@Serializable
data class AppSubscriptionDto(
    val plan: String = "free",
    val price: Int = 0,
    @SerialName("duration_days") val durationDays: Int = 0,
    @SerialName("starts_at") val startsAt: Long = 0,
    @SerialName("expires_at") val expiresAt: Long = 0,
    val status: String = ""
)

@Serializable
data class AppConfigDto(
    @SerialName("ad_interval_seconds") val adIntervalSeconds: Int = 45,
    @SerialName("banner_enabled") val bannerEnabled: Boolean = true,
    @SerialName("interstitial_enabled") val interstitialEnabled: Boolean = true,
    @SerialName("free_ad_multiplier") val freeAdMultiplier: Int = 2
)

@Serializable
data class RemoteConfigDto(
    @SerialName("ad_interval_seconds") val adIntervalSeconds: Int = 45,
    @SerialName("banner_enabled") val bannerEnabled: Boolean = true,
    @SerialName("interstitial_enabled") val interstitialEnabled: Boolean = true,
    @SerialName("free_ad_multiplier") val freeAdMultiplier: Int = 2,
    @SerialName("subscription_price_toman") val subscriptionPriceToman: Int = 150000,
    @SerialName("subscription_duration_days") val subscriptionDurationDays: Int = 30,
    @SerialName("card_number") val cardNumber: String = "",
    @SerialName("card_holder") val cardHolder: String = "",
    @SerialName("crypto_wallet") val cryptoWallet: String = "",
    @SerialName("crypto_network") val cryptoNetwork: String = "TRC20",
    @SerialName("gateway_enabled") val gatewayEnabled: Boolean = false,
    @SerialName("gateway_status_note") val gatewayStatusNote: String = "coming soon"
)

@Serializable
data class PaymentCreateRequest(
    val method: String,
    val amount: Int,
    val plan: String = "monthly",
    @SerialName("tx_hash") val txHash: String = "",
    val note: String = ""
)

@Serializable
data class PaymentCreateResponse(
    val payment: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    val status: String = ""
)

@Serializable
data class PaymentsMineResponse(
    val payments: List<Map<String, kotlinx.serialization.json.JsonElement>> = emptyList()
)

@Serializable
data class SubscriptionResponse(
    val subscription: AppSubscriptionDto? = null,
    val premium: Boolean = false
)

@Serializable
data class ApiErrorResponse(
    val error: String = ""
)

object AppJson {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
}
