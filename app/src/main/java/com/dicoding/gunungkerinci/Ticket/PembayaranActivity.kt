package com.dicoding.gunungkerinci.Ticket

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
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.R
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