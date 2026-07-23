package com.dicoding.gunungkerinci.model

data class PaketTiketResponse(
    val success: Boolean,
    val message: String,
    val data: PaketData
)

data class PaketData(
    val destinasi: DestinasiPaket,
    val paket: List<PaketTiket>
)

data class DestinasiPaket(
    val id: Int,
    val nama: String
)

data class PaketTiket(
    val id: Int,
    val nama: String,
    val keterangan: String,
    val min_pendaki: Int,
    val max_pendaki: Int,
    val tiket_pendaki: List<TiketPendaki>
)

data class TiketPendaki(
    val id: Int,
    val id_paket_tiket: Int,
    val kategori_pendaki: String,
    val harga_masuk_wk: Int,
    val harga_masuk_wd: Int,
    val harga_kemah: Int,
    val harga_traking: Int,
    val harga_ansuransi: Int,
    val masa_ansuransi: Int
)