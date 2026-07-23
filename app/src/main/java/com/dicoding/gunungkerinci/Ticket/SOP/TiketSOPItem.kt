package com.dicoding.gunungkerinci.Ticket.SOP

sealed class TiketSOPItem {
    // Header (Back + Judul + Card Download)
    object Header : TiketSOPItem()

    // Seluruh isi SOP dari API (HTML)
    data class Html(val html: String) : TiketSOPItem()

    // Footer (Checkbox + Tombol Selanjutnya)
    object Footer : TiketSOPItem()
}