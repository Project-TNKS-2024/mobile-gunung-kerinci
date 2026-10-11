package com.dicoding.gunungkerinci.ui.peringatan_dini

internal data class PeringatanDini(
    val id: Int,
    val type: String?,
    val title: String,
    val description: String,
    val severity: String,
    val createdAt: String
)

internal data class PeringatanDiniUiState(
    val activeWarning: PeringatanDini? = null,
    val popupVisible: Boolean = false,
    val errorMessage: String? = null,
    val isUpdatingLocation: Boolean = false,
    val lastLocationUpdate: String? = null,
    val locationUpdateMessage: String? = null
)
