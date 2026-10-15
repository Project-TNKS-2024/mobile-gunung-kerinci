package com.dicoding.gunungkerinci.data.local.sync

enum class SyncStatus {
    PENDING,
    SYNCING,
    SENT,
    FAILED,

    /** Server menolak permanen, tidak akan dicoba lagi. */
    REJECTED
}
