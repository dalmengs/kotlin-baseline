package com.dalmeng.template.other

data class BaseResponse<T>(
    val statusCode: Int,
    val success: Boolean,
    val data: T?,
    val error: String?
) {
    companion object {
        fun <T> ok(data: T? = null, statusCode: Int = 200): BaseResponse<T> =
            BaseResponse(
                statusCode = statusCode,
                success = true,
                data = data,
                error = null
            )

        fun failed(statusCode: Int, errorMessage: String?): BaseResponse<Nothing> =
            BaseResponse(
                statusCode = statusCode,
                success = false,
                data = null,
                error = errorMessage ?: "Internal server error"
            )
    }
}