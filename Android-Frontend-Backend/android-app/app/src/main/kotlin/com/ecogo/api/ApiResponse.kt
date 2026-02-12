package com.ecogo.api

/**
 * Unified API response format
 * Corresponds to the backend's ResponseMessage<T>
 */
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
) {
    val success: Boolean
        get() = code == 200 || code == 0
}

/**
 * Empty response (no data body)
 */
data class EmptyResponse(
    val message: String = "Success"
)
