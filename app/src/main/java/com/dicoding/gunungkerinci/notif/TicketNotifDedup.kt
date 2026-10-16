package com.dicoding.gunungkerinci.notif

import android.content.Context

/**
 * Penanda kejadian notifikasi tiket yang sudah ditampilkan, disimpan di
 * SharedPreferences kecil (bukan tabel database). Event key sudah mencakup
 * identitas akun, sehingga dedup terpisah per akun.
 */
internal object TicketNotifDedup {

    private const val PREF = "ticket_notif_dedup"
    private const val KEY_SHOWN = "shown_events"
    private const val KEY_ACTIVE_OVERDUE = "active_overdue"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun allShown(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_SHOWN, emptySet()).orEmpty()

    fun isShown(context: Context, eventKey: String): Boolean =
        allShown(context).contains(eventKey)

    fun markShown(context: Context, eventKey: String) {
        val p = prefs(context)
        val set = p.getStringSet(KEY_SHOWN, emptySet()).orEmpty().toMutableSet()
        if (set.add(eventKey)) p.edit().putStringSet(KEY_SHOWN, set).apply()
    }

    fun clearEvent(context: Context, eventKey: String) {
        val p = prefs(context)
        val set = p.getStringSet(KEY_SHOWN, emptySet()).orEmpty().toMutableSet()
        if (set.remove(eventKey)) p.edit().putStringSet(KEY_SHOWN, set).apply()
    }

    /** Booking id yang notifikasi overdue-nya masih aktif (untuk cleanup). */
    fun activeOverdueBookingIds(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_ACTIVE_OVERDUE, emptySet()).orEmpty()

    fun setActiveOverdue(context: Context, bookingIds: Set<String>) {
        prefs(context).edit().putStringSet(KEY_ACTIVE_OVERDUE, bookingIds).apply()
    }

    fun clearActiveOverdue(context: Context) {
        prefs(context).edit().remove(KEY_ACTIVE_OVERDUE).apply()
    }
}
