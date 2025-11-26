package com.dicoding.gunungkerinci.Profile

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.ActivityProfileLanguageBinding

class ProfileLanguageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileLanguageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Tombol kembali
        binding.buttonBack.setOnClickListener {
            finish()
        }

        //LOGIKA PILIH RADIO BUTTON
        binding.rbIndonesia.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.rbInggris.isChecked = false
            }
        }

        binding.rbInggris.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.rbIndonesia.isChecked = false

                // Tampilkan pesan bahwa English belum tersedia
                Toast.makeText(
                    this,
                    "Tampilan untuk sekarang hanya tersedia dalam Bahasa Indonesia.",
                    Toast.LENGTH_LONG
                ).show()

                // Kembalikan pilihan ke Bahasa Indonesia
                binding.rbIndonesia.isChecked = true
                binding.rbInggris.isChecked = false
            }
        }

        //Tombol simpan
        binding.btnSimpan.setOnClickListener {
            // Hanya Bahasa Indonesia yang valid → tampilkan popup custom
            if (binding.rbIndonesia.isChecked) {
                showPopupBahasa()
            }
        }
    }

    //POPUP BAHASA CUSTOM
    private fun showPopupBahasa() {

        val dialogView = LayoutInflater.from(this).inflate(R.layout.popup_bahasa, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Tombol Batal
        dialogView.findViewById<android.widget.Button>(R.id.btnBatalBahasa).setOnClickListener {
            dialog.dismiss()
        }

        // Tombol Simpan
        dialogView.findViewById<android.widget.Button>(R.id.btnSimpanBahasa).setOnClickListener {
            dialog.dismiss()
            finish() // kembali ke ProfileFragment
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

    }
}