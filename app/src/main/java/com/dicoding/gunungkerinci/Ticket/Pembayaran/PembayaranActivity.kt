package com.dicoding.gunungkerinci.Ticket.Pembayaran

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.Ticket.BuktiPembayaranActivity
import com.dicoding.gunungkerinci.databinding.ActivityPembayaranBinding

class PembayaranActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPembayaranBinding

    // URI file yang dipilih user
    private var selectedFileUri: Uri? = null

    // Launcher File Manager
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedFileUri = uri

            // Tampilkan nama file di dalam kotak pilih file
            val fileName = uri.lastPathSegment ?: "File dipilih"
            Toast.makeText(this, "File berhasil dipilih!", Toast.LENGTH_SHORT).show()

            binding.txtFileName.text = fileName
            binding.txtFileName.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPembayaranBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

            // Validasi metode pembayaran
            val metode = binding.dropdownGerbangMasuk.text.toString().trim()
            if (metode.isEmpty()) {
                Toast.makeText(this, "Pilih metode pembayaran terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validasi file bukti pembayaran
            if (selectedFileUri == null) {
                Toast.makeText(this, "Unggah bukti pembayaran terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Jika semua valid → pindah halaman
            val intent = Intent(this, BuktiPembayaranActivity::class.java)
            startActivity(intent)
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