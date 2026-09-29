package com.dicoding.gunungkerinci.model

data class BookingDetailResponse(
    val success: Boolean,
    val message: String,
    val data: BookingDetailData,
    val errors: Any?
)

data class BookingDetailData(
    val id: String,
    val id_user: Int,
    val id_tiket: Int,
    val tanggal_masuk: String,
    val tanggal_keluar: String,
    val total_hari: Int,
    val gate_masuk: BookingDetailGate,
    val gate_keluar: BookingDetailGate,
    val status_booking: Int,
    val total_pembayaran: Int,
    val status_pembayaran: Int,
    val dataStruk: String?,
    val gktiket: BookingDetailJenisTiket?,
    val pendakis: List<BookingDetailPendaki>,
    val destinasi: BookingDestination,
    val pembayaran: List<PembayaranData>?

)

data class BookingDetailPendaki(
    val id: String,
    val booking_id: String,
    val id_bio: String,
    val usia: Int,
    val biodata: BookingDetailBiodata,
    val tagihan: Int? = null
)

data class BookingDetailBiodata(
    val id: String,
    val nik: String?,
    val first_name: String?,
    val last_name: String?,
    val no_hp: String?,
    val no_hp_darurat: String?,
    val jenis_kelamin: String?,
    val tanggal_lahir: String?,
    val dataNegara: BookingDetailNegara?,
    val dataProvinsi: BookingDetailWilayah?,
    val dataKabupaten: BookingDetailWilayah?,
    val dataKecamatan: BookingDetailWilayah?,
    val dataDesa: BookingDetailWilayah?
)

data class BookingDetailNegara(
    val name: String?,
    val flag: String?,
    val code: String?,
    val dial_code: String?
)

data class BookingDetailWilayah(
    val id: Int?,
    val name: String?
)

data class BookingDetailJenisTiket(
    val id: Int,
    val id_destinasi: Int?,
    val nama: String?,
    val min_pendaki: Int?,
    val penugasan: String?,
    val keterangan: String?,
    val tiket_pendaki: List<BookingDetailTiketPendaki>?
)

data class BookingDetailTiketPendaki(
    val id: Int,
    val id_paket_tiket: Int?,
    val kategori_pendaki: String?,
    val harga_masuk_wk: Int?,
    val harga_masuk_wd: Int?,
    val harga_kemah: Int?,
    val harga_traking: Int?,
    val harga_ansuransi: Int?,
    val masa_ansuransi: Int?
)

data class BookingDetailGate(
    val id: Int,
    val nama: String,
    val status: Int?,
    val id_destinasi: Int?,
    val max_pendaki_hari: Int?,
    val min_pendaki_booking: Int?,
    val lokasi: String?,
    val lokasi_maps: String?,
    val detail: String?
)

data class BookingDestination(
    val id: Int,
    val nama: String,
    val sop: String
)

data class PembayaranData(
    val id: String,
    val id_booking: String,
    val amount: String?,
    val status: String?,
    val payment_method: String?,
    val bukti_pembayaran: String?,
    val deadline: String?,
    val keterangan: String?
)