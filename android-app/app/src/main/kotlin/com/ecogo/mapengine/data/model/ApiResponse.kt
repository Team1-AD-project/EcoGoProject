package com.ecogo.mapengine.data.model

import com.google.gson.annotations.SerializedName

/**
 * 通用 API 响应包装类
 * 对应后端统一返回格式
 */
data class ApiResponse<T>(
    @SerializedName("code")
    val code: Int,

    @SerializedName("msg", alternate = ["message"])
    val msg: String = "",

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("success")
    val success: Boolean = true
) {
    val isSuccess: Boolean
        get() = code == 200 || success

    // 兼容 message 字段
    val message: String
        get() = msg
}
