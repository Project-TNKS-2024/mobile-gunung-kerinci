package com.dicoding.gunungkerinci.network

/**
 * Konfigurasi alamat backend untuk pengujian — disamakan dengan repo `mobile-gunung-kerinci-vibe-coding`.
 *
 * Pilih BASE_URL sesuai cara menjalankan aplikasi:
 * - Emulator Android Studio    : "http://10.0.2.2:8000/"    (10.0.2.2 = localhost PC dilihat dari emulator)
 * - Real device + adb reverse  : "http://127.0.0.1:8000/"    (jalankan dulu: adb reverse tcp:8000 tcp:8000)
 * - Real device tanpa adb      : "http://192.168.x.x:8000/"  (IP Wi-Fi laptop/PC)
 * - Production                 : "https://tnks.mukhtada.my.id/"
 *
 * Backend dijalankan dengan: php artisan serve --host=0.0.0.0 --port=8000
 *
 * Catatan:
 * - Nilai BASE_URL di repo vibe-coding sama-sama "http://127.0.0.1:8000/".
 * - FALLBACK_TOKEN debug di repo itu TIDAK disalin ke sini — token tidak boleh hard-code di aplikasi.
 */
object DebugNetworkConfig {
    const val BASE_URL = "http://127.0.0.1:8000/"
}
