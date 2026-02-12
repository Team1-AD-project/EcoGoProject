package com.ecogo.data

import com.ecogo.api.PaginationDto
import com.google.gson.annotations.SerializedName

/**
 * Aligned with backend OrderController response data:
 * {
 *   "orders": [ { ... } ],
 *   "pagination": { ... }
 * }
 *
 * Also backward-compatible if the data field is named "data: [ ... ]"
 */
data class MobileOrderHistoryData(
    @SerializedName(value = "orders", alternate = ["data"])
    val orders: List<OrderSummaryUi> = emptyList(),
    val pagination: PaginationDto? = null
)

data class OrderSummaryUi(
    val id: String,
    val orderNumber: String? = null,
    val status: String? = null,
    val finalAmount: Double? = null,
    val createdAt: String? = null,
    val itemCount: Int? = null,
    val isRedemption: Boolean? = null,
    val trackingNumber: String? = null,
    val carrier: String? = null
)
