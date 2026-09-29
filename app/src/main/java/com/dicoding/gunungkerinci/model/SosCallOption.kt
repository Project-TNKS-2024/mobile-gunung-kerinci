package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

/** Respons GET /api/sos/call-options. */
data class SosCallOptionsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<SosCallOption>?,
    @SerializedName("errors") val errors: Any?
)

data class SosCallOption(
    @SerializedName("name") val name: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("whatsapp_link") val whatsappLink: String?,
    @SerializedName("tel_link") val telLink: String?
)
