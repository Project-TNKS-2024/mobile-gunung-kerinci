package com.dicoding.gunungkerinci.Ticket.SOP

sealed class SOPItem {
    object Header : SOPItem()
    data class Title(val text: String) : SOPItem()
    data class Subtitle(val text: String) : SOPItem()
    data class Content(val text: String) : SOPItem()
    object Footer : SOPItem()
}