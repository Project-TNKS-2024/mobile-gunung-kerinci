package com.dicoding.gunungkerinci.Ticket

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.widget.Toast
import com.dicoding.gunungkerinci.databinding.ActivityTiketFormDataPendakiBinding

class TiketFormDataPendakiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTiketFormDataPendakiBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTiketFormDataPendakiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Terima nama pendaki dari halaman sebelumnya
        val namaPendaki = intent.getStringExtra("nama_pendaki") ?: "Pendaki 1"
        binding.textIsiDataPendakian.text = namaPendaki

        // Tombol Back
        binding.buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // DISABLE SEMUA INPUT KECUALI:
        // 1. inputIdPendaki
        // 2. inputNoDarurat

        disableAllInputs()

        // Khusus nomor darurat → hanya angka
        binding.inputNoDarurat.inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        // BUTTON CARI Pendaki
        binding.btnCariPendaki.setOnClickListener {
            val id = binding.inputIdPendaki.text.toString()

            if (id.isEmpty()) {
                Toast.makeText(this, "Masukkan ID pendaki terlebih dahulu!", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Demo hasil pencarian
            binding.inputNamaDepan.setText("Diah")
            binding.inputNamaBelakang.setText("Sari")
            binding.inputNegara.setText("Indonesia")
            binding.inputTglLahir.setText("10-05-2000")
            binding.inputUsia.setText("24")
            binding.inputNoTelepon.setText("08123456789")

            Toast.makeText(this, "Data pendaki terisi otomatis", Toast.LENGTH_SHORT).show()
        }

        // HILANGKAN BUTTON NEXT
        // TAMPILKAN BUTTON PESAN TIKET
        binding.btnNext.visibility = android.view.View.GONE
        binding.buttonSimpan.visibility = android.view.View.VISIBLE

        // BUTTON PESAN TIKET
        binding.buttonSimpan.setOnClickListener {
            if (binding.inputNoDarurat.text.isEmpty()) {
                Toast.makeText(this, "Isi nomor darurat terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Data pendaki berhasil disimpan!", Toast.LENGTH_SHORT).show()

            // Kembalikan nilai ke activity sebelumnya
            val resultIntent = Intent()
            resultIntent.putExtra("pendaki_selesai", true)
            setResult(RESULT_OK, resultIntent)

            finish()
        }
    }

    private fun disableAllInputs() {
        val disableList = listOf(
            binding.inputNamaDepan,
            binding.inputNamaBelakang,
            binding.inputNegara,
            binding.inputTglLahir,
            binding.inputUsia,
            binding.inputNoTelepon
        )

        disableList.forEach {
            it.isEnabled = false
            it.isFocusable = false
            it.isClickable = false
        }

        // ID pendaki tetap bisa diinput → aktif
        binding.inputIdPendaki.isEnabled = true
        binding.inputIdPendaki.isFocusable = true
        binding.inputIdPendaki.isClickable = true

        // Nomor darurat tetap aktif
        binding.inputNoDarurat.isEnabled = true
        binding.inputNoDarurat.isFocusable = true
        binding.inputNoDarurat.isClickable = true
    }
}