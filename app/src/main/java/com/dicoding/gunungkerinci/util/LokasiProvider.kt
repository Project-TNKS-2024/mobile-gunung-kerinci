package com.dicoding.gunungkerinci.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.concurrent.atomic.AtomicBoolean

// Satu-satunya tempat mengambil lokasi. Dulu ada 3 salinan getLastKnownLocation()
// yang cuma membaca catatan sistem, jadi gagal ("Lokasi tidak tersedia") waktu
// catatannya kosong. cariSekarang() meminta lokasi baru, dengan batas waktu.
internal object LokasiProvider {

    // Lokasi lebih tua dari ini dianggap basi, tidak dikirim ke server.
    const val UMUR_MAKS_MS = 5 * 60 * 1000L

    // Batas waktu menunggu lokasi baru.
    const val TIMEOUT_MS = 15_000L

    // GPS sedang aktif?
    fun gpsAktif(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
    }

    // Catatan lokasi terakhir dari sistem. Bisa null kalau belum ada yang mengukur.
    @SuppressLint("MissingPermission")
    fun lastKnown(context: Context): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    // Lokasi masih layak dipakai? Terima waktu (Long), bukan Location,
    // supaya murni fungsi waktu dan gampang diuji.
    fun layak(waktuLokasiMs: Long?, sekarang: Long = System.currentTimeMillis()): Boolean =
        waktuLokasiMs != null && (sekarang - waktuLokasiMs) <= UMUR_MAKS_MS

    // Minta lokasi baru. Hasilnya dilaporkan sekali:
    // - dapat sebelum batas waktu -> pakai lokasi itu
    // - lewat batas waktu / gagal -> pakai catatan lama, asal belum basi
    // - tidak ada sama sekali -> null
    @SuppressLint("MissingPermission")
    fun cariSekarang(
        context: Context,
        timeoutMs: Long = TIMEOUT_MS,
        selesai: (Location?) -> Unit
    ) {
        val sudahDijawab = AtomicBoolean(false)
        val jawabSekali: (Location?) -> Unit = { lokasi ->
            if (sudahDijawab.compareAndSet(false, true)) selesai(lokasi)
        }

        val pembatalan = CancellationTokenSource()
        val handler = Handler(Looper.getMainLooper())
        val batasWaktu = Runnable {
            pembatalan.cancel()
            jawabSekali(cadangan(context))
        }

        val klien = runCatching { LocationServices.getFusedLocationProviderClient(context) }.getOrNull()
        val tugas = klien?.let {
            runCatching { it.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, pembatalan.token) }.getOrNull()
        }

        if (tugas == null) {
            jawabSekali(cadangan(context))
            return
        }

        tugas
            .addOnSuccessListener { lokasi ->
                handler.removeCallbacks(batasWaktu)
                jawabSekali(lokasi ?: cadangan(context))
            }
            .addOnFailureListener {
                handler.removeCallbacks(batasWaktu)
                jawabSekali(cadangan(context))
            }

        handler.postDelayed(batasWaktu, timeoutMs)
    }

    // Catatan lama yang masih layak, dipakai kalau pencarian gagal.
    private fun cadangan(context: Context): Location? =
        lastKnown(context)?.takeIf { layak(it.time) }
}
