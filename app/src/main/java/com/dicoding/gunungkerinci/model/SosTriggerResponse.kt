package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

/** Respons POST /api/sos/trigger. */
data class SosTriggerResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: SosTriggerData?,
    @SerializedName("errors") val errors: Any?
)

data class SosTriggerData(
    @SerializedName("sos_id") val sosId: Int,
    @SerializedName("status") val status: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("rescue_contact") val rescueContact: RescueContact?
)

data class RescueContact(
    @SerializedName("phone") val phone: String?,
    @SerializedName("whatsapp_link") val whatsappLink: String?
)
