package com.dicoding.gunungkerinci.Ticket.Pembayaran

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.ActivityPembayaranBinding
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class PembayaranActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPembayaranBinding
    private var selectedFileUri: Uri? = null

    private var bookingId = ""

    // Launcher File Manager
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->

        if (uri != null) {

            val fileSize = getFileSize(uri)

            // ==========================================
            // CEK UKURAN FILE
            // ==========================================

            if (fileSize != null && fileSize > 1024 * 1024) {

                Toast.makeText(
                    this,
                    "Ukuran file maksimal 1 MB",
                    Toast.LENGTH_SHORT
                ).show()

                // Jangan simpan file yang terlalu besar
                selectedFileUri = null

                // Kembalikan tampilan input
                binding.txtFileName.text =
                    "Ukuran gambar maks 1 MB"

                binding.txtFileName.visibility =
                    View.VISIBLE

                return@registerForActivityResult
            }

            // ==========================================
            // FILE LOLOS VALIDASI
            // ==========================================

            selectedFileUri = uri

            val fileName =
                getFileName(uri)
                    ?: "File dipilih"

            Toast.makeText(
                this,
                "File berhasil dipilih!",
                Toast.LENGTH_SHORT
            ).show()

            binding.txtFileName.text =
                fileName

            binding.txtFileName.visibility =
                View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPembayaranBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookingId =
            intent.getStringExtra("booking_id").orEmpty()

        if (bookingId.isBlank()) {
            Toast.makeText(
                this,
                "Booking ID tidak ditemukan",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        ambilDataPayment()

        // Tombol back di header
        binding.buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Tombol Batalkan → tampil popup
        binding.btnBatalkan.setOnClickListener {
            showPopupBatalkan()
        }

        // DROPDOWN METODE PEMBAYARAN
        setupMetodePembayaranDropdown()

        // PILIH FILE → membuka file manager
        binding.btnPilihFile.setOnClickListener {
            filePickerLauncher.launch("*/*")  // semua jenis file
        }

        // EVENT EXPANDABLE: TRANSFER ANTAR BANK
        binding.sectionTransfer.setOnClickListener {
            toggleTransferSection()
        }

        // EVENT EXPANDABLE: QRIS
        binding.sectionQris.setOnClickListener {
            toggleQrisSection()
        }

        initQrisTabs()

        binding.copy.setOnClickListener {
            val textToCopy = binding.textNoRek.text.toString()

            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Nomor Rekening", textToCopy)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, "Nomor rekening disalin", Toast.LENGTH_SHORT).show()
        }

        //setupCaraPembayaranList()

        // ====== BUTTON KIRIM ======
        binding.btnKirim.setOnClickListener {

            // ==========================================
            // VALIDASI METODE PEMBAYARAN
            // ==========================================

            val metode =
                binding.dropdownGerbangMasuk
                    .text
                    .toString()
                    .trim()

            if (metode.isEmpty()) {

                Toast.makeText(
                    this,
                    "Pilih metode pembayaran terlebih dahulu!",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // ==========================================
            // VALIDASI FILE BUKTI PEMBAYARAN
            // ==========================================

            if (selectedFileUri == null) {

                Toast.makeText(
                    this,
                    "Unggah bukti pembayaran terlebih dahulu!",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // ==========================================
            // KIRIM KE API
            // ==========================================

            kirimBuktiPembayaran()
        }

    }

    private fun ambilDataPayment() {

        lifecycleScope.launch {

            try {

                Log.d(
                    "PAYMENT_API",
                    "Booking ID: $bookingId"
                )

                val response =
                    ApiConfig
                        .getApiService(this@PembayaranActivity)
                        .getPayment(bookingId)

                Log.d(
                    "PAYMENT_API",
                    "HTTP Code: ${response.code()}"
                )

                if (!response.isSuccessful) {

                    Log.e(
                        "PAYMENT_API",
                        "Error: ${response.errorBody()?.string()}"
                    )

                    Toast.makeText(
                        this@PembayaranActivity,
                        "Gagal mengambil data pembayaran",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                val body = response.body()

                Log.d(
                    "PAYMENT_API",
                    "Response: $body"
                )

                if (body?.success != true) {

                    Toast.makeText(
                        this@PembayaranActivity,
                        body?.message
                            ?: "Data pembayaran tidak ditemukan",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                val data = body.data

                if (data == null) {

                    Toast.makeText(
                        this@PembayaranActivity,
                        "Data pembayaran kosong",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                Log.d(
                    "PAYMENT_DATA",
                    "Booking ID: ${data.booking.id}"
                )

                Log.d(
                    "PAYMENT_DATA",
                    "Total pembayaran: ${data.booking.total_pembayaran}"
                )

                Log.d(
                    "PAYMENT_DATA",
                    "Gerbang masuk: ${data.booking.gate_masuk.nama}"
                )

                Log.d(
                    "PAYMENT_DATA",
                    "Gerbang keluar: ${data.booking.gate_keluar.nama}"
                )

                Log.d(
                    "PAYMENT_DATA",
                    "Nomor rekening: ${data.bank?.text1}"
                )

                Log.d(
                    "PAYMENT_DATA",
                    "Nama rekening: ${data.bank?.text2}"
                )

                Log.d(
                    "PAYMENT_DATA",
                    "QRIS: ${data.qris}"
                )

                // ==============================
                // TAMPILKAN DATA KE UI
                // ==============================

                // Total pembayaran
                val totalPembayaran =
                    data.booking.total_pembayaran

                binding.textTotalPembayaran.text =
                    "Rp${String.format("%,d", totalPembayaran).replace(',', '.')}"


                // Nomor rekening
                binding.textNoRek.text =
                    data.bank?.text1 ?: "-"


                // Nama rekening
                binding.textNamaRekening.text =
                    data.bank?.text2 ?: "-"

            } catch (e: Exception) {

                Log.e(
                    "PAYMENT_API",
                    "Exception: ${e.message}",
                    e
                )

                Toast.makeText(
                    this@PembayaranActivity,
                    "Terjadi kesalahan: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun uriToFile(uri: Uri): File? {

        return try {

            val fileName = getFileName(uri) ?: "bukti_pembayaran"

            val file = File(
                cacheDir,
                fileName
            )

            contentResolver.openInputStream(uri)?.use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            file

        } catch (e: Exception) {

            Log.e(
                "PAYMENT_UPLOAD",
                "Gagal membuat file sementara: ${e.message}",
                e
            )

            null
        }
    }

    private fun getFileSize(uri: Uri): Long? {

        return try {

            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->

                val sizeIndex =
                    cursor.getColumnIndex(OpenableColumns.SIZE)

                if (sizeIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getLong(sizeIndex)
                } else {
                    null
                }
            }

        } catch (e: Exception) {

            Log.e(
                "PAYMENT_UPLOAD",
                "Gagal membaca ukuran file: ${e.message}",
                e
            )

            null
        }
    }

    private fun getFileName(uri: Uri): String? {

        var fileName: String? = null

        contentResolver.query(
            uri,
            null,
            null,
            null,
            null
        )?.use { cursor ->

            val nameIndex =
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

            if (nameIndex >= 0 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }

        return fileName
    }

    private fun kirimBuktiPembayaran() {

        val uri = selectedFileUri

        if (uri == null) {

            Toast.makeText(
                this,
                "Unggah bukti pembayaran terlebih dahulu!",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (bookingId.isBlank()) {

            Toast.makeText(
                this,
                "Booking ID tidak ditemukan",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        lifecycleScope.launch {

            try {

                binding.btnKirim.isEnabled = false

                Log.d(
                    "PAYMENT_UPLOAD",
                    "Booking ID: $bookingId"
                )

                Log.d(
                    "PAYMENT_UPLOAD",
                    "File URI: $uri"
                )

                // ==========================================
                // 1. Tentukan metode pembayaran
                // ==========================================

                val metodeTampilan =
                    binding.dropdownGerbangMasuk
                        .text
                        .toString()
                        .trim()

                val metodeApi =
                    if (metodeTampilan.equals(
                            "QRIS",
                            ignoreCase = true
                        )
                    ) {
                        "scan"
                    } else {
                        "transfer"
                    }

                Log.d(
                    "PAYMENT_UPLOAD",
                    "Metode tampilan: $metodeTampilan"
                )

                Log.d(
                    "PAYMENT_UPLOAD",
                    "Metode API: $metodeApi"
                )

                // ==========================================
                // 2. Ubah URI menjadi file sementara
                // ==========================================

                val file = uriToFile(uri)

                if (file == null) {

                    Toast.makeText(
                        this@PembayaranActivity,
                        "File bukti pembayaran tidak dapat dibaca",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                // ==========================================
                // 3. Cek ukuran file
                // ==========================================

                val fileSizeKb =
                    file.length() / 1024

                Log.d(
                    "PAYMENT_UPLOAD",
                    "Nama file: ${file.name}"
                )

                Log.d(
                    "PAYMENT_UPLOAD",
                    "Ukuran file: ${fileSizeKb} KB"
                )

                if (fileSizeKb > 1024) {

                    Toast.makeText(
                        this@PembayaranActivity,
                        "Ukuran file maksimal 2 MB",
                        Toast.LENGTH_SHORT
                    ).show()

                    file.delete()

                    return@launch
                }

                // ==========================================
                // 4. Cek tipe file
                // ==========================================

                val mimeType =
                    contentResolver.getType(uri)
                        ?: "application/octet-stream"

                Log.d(
                    "PAYMENT_UPLOAD",
                    "MIME Type: $mimeType"
                )

                val tipeDiizinkan =
                    mimeType == "image/jpeg" ||
                            mimeType == "image/png" ||
                            mimeType == "application/pdf"

                if (!tipeDiizinkan) {

                    Toast.makeText(
                        this@PembayaranActivity,
                        "File harus JPG, JPEG, PNG, atau PDF",
                        Toast.LENGTH_SHORT
                    ).show()

                    file.delete()

                    return@launch
                }

                // ==========================================
                // 5. Buat RequestBody
                // ==========================================

                val idRequestBody =
                    bookingId.toRequestBody(
                        "text/plain".toMediaTypeOrNull()
                    )

                val metodeRequestBody =
                    metodeApi.toRequestBody(
                        "text/plain".toMediaTypeOrNull()
                    )

                val fileRequestBody =
                    file.asRequestBody(
                        mimeType.toMediaTypeOrNull()
                    )

                val filePart =
                    MultipartBody.Part.createFormData(
                        "bukti_pembayaran",
                        file.name,
                        fileRequestBody
                    )

                // ==========================================
                // 6. Kirim ke API
                // ==========================================

                Log.d(
                    "PAYMENT_UPLOAD",
                    "Mengirim bukti pembayaran ke API..."
                )

                val response =
                    ApiConfig
                        .getApiService(
                            this@PembayaranActivity
                        )
                        .addPayment(
                            idRequestBody,
                            metodeRequestBody,
                            filePart
                        )

                Log.d(
                    "PAYMENT_UPLOAD",
                    "HTTP Code: ${response.code()}"
                )

                if (!response.isSuccessful) {

                    Log.e(
                        "PAYMENT_UPLOAD",
                        "Error: ${response.errorBody()?.string()}"
                    )

                    Toast.makeText(
                        this@PembayaranActivity,
                        "Gagal mengirim bukti pembayaran",
                        Toast.LENGTH_SHORT
                    ).show()

                    file.delete()

                    return@launch
                }

                val body =
                    response.body()

                Log.d(
                    "PAYMENT_UPLOAD",
                    "Response: $body"
                )

                if (body?.success != true) {

                    Toast.makeText(
                        this@PembayaranActivity,
                        body?.message
                            ?: "Bukti pembayaran gagal dikirim",
                        Toast.LENGTH_SHORT
                    ).show()

                    file.delete()

                    return@launch
                }

                // ==========================================
                // 7. Berhasil
                // ==========================================

                Log.d(
                    "PAYMENT_UPLOAD",
                    "Bukti pembayaran berhasil dikirim"
                )

                Toast.makeText(
                    this@PembayaranActivity,
                    body.message
                        ?: "Bukti pembayaran berhasil dikirim",
                    Toast.LENGTH_SHORT
                ).show()

                file.delete()

                // ==========================================
                // 8. Pindah ke halaman Bukti Pembayaran
                // ==========================================

                val intent =
                    Intent(
                        this@PembayaranActivity,
                        BuktiPembayaranActivity::class.java
                    )

                intent.putExtra(
                    "booking_id",
                    bookingId
                )

                startActivity(intent)

                finish()

            } catch (e: Exception) {

                Log.e(
                    "PAYMENT_UPLOAD",
                    "Exception: ${e.message}",
                    e
                )

                Toast.makeText(
                    this@PembayaranActivity,
                    "Terjadi kesalahan: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

            } finally {

                binding.btnKirim.isEnabled = true
            }
        }
    }

    private fun setupCaraPembayaranList() {

        binding.rvCara.layoutManager = LinearLayoutManager(this)

        val listCara = listOf(
            CaraItem(1, "Datangi ATM terdekat."),
            CaraItem(2, "Masukkan kartu ATM BRI."),
            CaraItem(3, "Pilih bahasa selama transaksi."),
            CaraItem(4, "Masukkan PIN kartu ATM."),
            CaraItem(5, "Pilih menu Transfer Lainnya dan klik Transfer."),
            CaraItem(6, "Masukkan kode bank BRI 002 diikuti nomor rekening BRI yang dituju, misal 002(445XXXXXXXXXX)"),
            CaraItem(7, "Masukkan nominal yang ingin ditransfer lalu klik Benar atau Ya."),
            CaraItem(8, "Lanjut pilih jenis rekening Tabungan atau Giro."),
            CaraItem(9, "Transaksi segera diproses."),
            CaraItem(10, "Keluar struk atau info di layar jika transfer berhasil.")
        )

        val adapter = CaraAdapter(listCara)
        binding.rvCara.adapter = adapter
    }

    private fun initQrisTabs() {
        // Default: tab kanan aktif
        setActiveQrisTab(isLeft = false)

        binding.tvMenuLeft.setOnClickListener {
            setActiveQrisTab(isLeft = true)
        }

        binding.tvMenuRight.setOnClickListener {
            setActiveQrisTab(isLeft = false)
        }
    }

    private fun setActiveQrisTab(isLeft: Boolean) {
        if (isLeft) {
            // LEFT ACTIVE
            binding.tvMenuLeft.setBackgroundResource(R.drawable.bg_qris_left_selected)
            binding.tvMenuRight.setBackgroundResource(R.drawable.bg_tab_right_inactive)

            binding.tvMenuLeft.setTextColor(getColor(R.color.black2))
            binding.tvMenuRight.setTextColor(getColor(R.color.darkgrey))

            binding.imgQris.setImageResource(R.drawable.ic_empty) // gambar versi Kersik Tuo

        } else {
            // RIGHT ACTIVE
            binding.tvMenuLeft.setBackgroundResource(R.drawable.bg_tab_left_inactive)
            binding.tvMenuRight.setBackgroundResource(R.drawable.bg_qris_right_selected)

            binding.tvMenuLeft.setTextColor(getColor(R.color.black2))
            binding.tvMenuRight.setTextColor(getColor(R.color.black2))

            binding.imgQris.setImageResource(R.drawable.qris_bangunrejo) // gambar versi Bangun Rejo
        }
    }


    // Dropdown Metode Pembayaran
    private fun setupMetodePembayaranDropdown() {
        val listMetode = listOf(
            "Transfer antar Bank",
            "QRIS"
        )

        val adapter = ArrayAdapter(this,
            android.R.layout.simple_list_item_1,
            listMetode
        )

        binding.dropdownGerbangMasuk.setAdapter(adapter)

        // agar dropdown muncul saat diklik
        binding.dropdownGerbangMasuk.setOnClickListener {
            binding.dropdownGerbangMasuk.showDropDown()
        }
    }

    // ========== FUNGSI EXPAND / COLLAPSE TRANSFER ==========
    private fun toggleTransferSection() {
        val isVisible = binding.contentTransfer.isVisible

        // Tutup QRIS jika sedang terbuka
        binding.contentQris.visibility = View.GONE
        binding.iconUpQris.visibility = View.GONE
        binding.iconDownQris.visibility = View.VISIBLE

        if (isVisible) {
            // Tutup Transfer
            binding.contentTransfer.visibility = View.GONE
            binding.iconUpTf.visibility = View.GONE
            binding.iconDownTf.visibility = View.VISIBLE
        } else {
            // Buka Transfer
            binding.contentTransfer.visibility = View.VISIBLE
            binding.iconUpTf.visibility = View.VISIBLE
            binding.iconDownTf.visibility = View.GONE

            setupCaraPembayaranList()
        }
    }

    // ========== FUNGSI EXPAND / COLLAPSE QRIS ==========
    private fun toggleQrisSection() {
        val isVisible = binding.contentQris.isVisible

        // Tutup Transfer jika sedang terbuka
        binding.contentTransfer.visibility = View.GONE
        binding.iconUpTf.visibility = View.GONE
        binding.iconDownTf.visibility = View.VISIBLE

        if (isVisible) {
            // Tutup QRIS
            binding.contentQris.visibility = View.GONE
            binding.iconUpQris.visibility = View.GONE
            binding.iconDownQris.visibility = View.VISIBLE
        } else {
            // Buka QRIS
            binding.contentQris.visibility = View.VISIBLE
            binding.iconUpQris.visibility = View.VISIBLE
            binding.iconDownQris.visibility = View.GONE
        }
    }

    private fun showPopupBatalkan() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_pemesanan)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnBatal = dialog.findViewById<Button>(R.id.btnBatalPemesanan)
        val btnYakin = dialog.findViewById<Button>(R.id.btnYakinPemesanan)

        btnBatal.setOnClickListener {
            dialog.dismiss()
        }

        btnYakin.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "Pemesanan tiket dibatalkan", Toast.LENGTH_SHORT).show()

            // kembali ke home fragment di main activity
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("navigate_home", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        dialog.show()
    }
}