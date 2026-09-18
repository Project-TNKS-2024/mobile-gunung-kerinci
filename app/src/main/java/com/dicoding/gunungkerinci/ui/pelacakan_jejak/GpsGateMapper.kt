package com.dicoding.gunungkerinci.ui.pelacakan_jejak

import com.dicoding.gunungkerinci.model.GpsNearestPost

/**
 * Memetakan hasil deteksi pos terdekat dari server (`POST /tracking/checkpoint/gps`)
 * menjadi status gerbang check-in.
 *
 * Catatan: client TIDAK menghitung radius. Keputusan `within_radius` datang dari
 * server; fungsi ini hanya menerjemahkannya ke [GpsGateState].
 *
 * Dipisah dari ViewModel supaya bisa diuji sebagai fungsi murni (tanpa coroutine).
 */
internal object GpsGateMapper {

    fun toGateState(nearest: GpsNearestPost): GpsGateState =
        if (nearest.withinRadius) {
            GpsGateState.DalamRadius(
                postName = nearest.nama,
                distanceMeters = nearest.distanceMeters,
                radiusMeter = nearest.radiusMeter
            )
        } else {
            GpsGateState.LuarRadius(
                postName = nearest.nama,
                distanceMeters = nearest.distanceMeters,
                radiusMeter = nearest.radiusMeter
            )
        }
}
