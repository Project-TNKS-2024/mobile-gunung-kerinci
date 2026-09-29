package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

/** Respons POST /api/sos/chat/{sos_id}/send. */
data class SosSendMessageResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: SosSentMessage?,
    @SerializedName("errors") val errors: Any?
)

data class SosSentMessage(
    @SerializedName("id") val id: Int,
    @SerializedName("sender_type") val senderType: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("content") val content: String?,
    @SerializedName("created_at") val createdAt: String?
)

/** Respons GET /api/sos/chat/{sos_id}/messages. */
data class SosMessagesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: SosMessagesData?,
    @SerializedName("errors") val errors: Any?
)

data class SosMessagesData(
    @SerializedName("messages") val messages: List<SosChatMessage>?,
    @SerializedName("pagination") val pagination: SosPagination?
)

data class SosChatMessage(
    @SerializedName("id") val id: Int,
    @SerializedName("sender_type") val senderType: String?,
    @SerializedName("sender_name") val senderName: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("content") val content: String?,
    @SerializedName("is_read") val isRead: Boolean?,
    @SerializedName("created_at") val createdAt: String?
)

data class SosPagination(
    @SerializedName("current_page") val currentPage: Int?,
    @SerializedName("last_page") val lastPage: Int?,
    @SerializedName("total") val total: Int?
)
