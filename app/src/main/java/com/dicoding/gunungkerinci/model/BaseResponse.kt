package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

data class BaseResponse<T> (
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("errors")
    val errors: Any? = null
)