package com.dicoding.gunungkerinci.ui.pelacakan_jejak

internal object CheckpointQrCheckInValidator {
    fun errorMessage(
        qrCodeValue: String?,
        latitude: Double? = null,
        longitude: Double? = null,
        qrPostId: Int? = null,
        qrPostName: String? = null,
        posTerdekatId: Int? = null,
        posTerdekatName: String? = null
    ): String? {
        if (qrCodeValue.isNullOrBlank()) return "QR code tidak boleh kosong"
        if (latitude != null && (latitude < -90.0 || latitude > 90.0)) return "Latitude tidak valid"
        if (longitude != null && (longitude < -180.0 || longitude > 180.0)) return "Longitude tidak valid"

        // QR = bukti fisik keberadaan, jadi QR yang di-scan harus milik pos yang sedang
        // didatangi pendaki (pos terdekat menurut deteksi GPS yang sudah berjalan).
        //
        // Kalau pos terdekat belum diketahui — GPS mati, atau pendaki sedang di luar semua
        // radius pos — check-in tetap diloloskan supaya pendaki tidak gagal absen di gunung.
        // Validasi jarak penuh tetap tanggung jawab server.
        if (qrPostId != null && posTerdekatId != null && qrPostId != posTerdekatId) {
            return "QR ini milik ${qrPostName ?: "pos lain"}, bukan " +
                "${posTerdekatName ?: "pos terdekatmu"}. " +
                "Scan QR pos yang kamu datangi, atau pakai Check-in Manual."
        }

        return null
    }
}
