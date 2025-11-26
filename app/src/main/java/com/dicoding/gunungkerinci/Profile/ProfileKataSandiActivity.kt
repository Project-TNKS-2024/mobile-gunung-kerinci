package com.dicoding.gunungkerinci.Profile

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.ActivityProfileKataSandiBinding

class ProfileKataSandiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileKataSandiBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileKataSandiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Tombol Kembali
        binding.buttonBack.setOnClickListener {
            finish()
        }

        //Tombol Simpan
        binding.btnSimpan.setOnClickListener{
            validatePassword()
        }
    }

    private fun validatePassword() {

        val passLama = binding.passLamaEditText.text.toString().trim()
        val passBaru = binding.passBaruEditText.text.toString().trim()
        val passKonf = binding.passBaruKonfEditText.text.toString().trim()

        binding.passLamaTextLayout.error = null
        binding.passBaruTextLayout.error = null
        binding.passBaruKonfTextLayout.error = null

        //Password lama wajib diisi
        if (passLama.isEmpty()) {
            binding.passLamaTextLayout.error = "Kata sandi lama wajib diisi"
            binding.passLamaTextLayout.requestFocus()
            return
        }

        //Password baru wajib diisi
        if (passBaru.isEmpty()) {
            binding.passBaruTextLayout.error = "Kata sandi baru wajib diisi"
            binding.passBaruTextLayout.requestFocus()
            return
        }

        //Password minimal 8 karakter
        if (passBaru.length < 8) {
            binding.passBaruTextLayout.error = "Kata sandi diisi minimal 8 karakter"
            binding.passBaruTextLayout.requestFocus()
            return
        }

        //Password baru tidak boleh sama dengan password lama
        if (passBaru == passLama) {
            binding.passBaruTextLayout.error = "Kata sandi baru tidak boleh sama dengan kata sandi lama"
            binding.passBaruTextLayout.requestFocus()
            return
        }

        //Konfirmasi wajib diisi
        if (passKonf.isEmpty()) {
            binding.passBaruKonfTextLayout.error = "Konfirmasi kata sandi lama wajib diisi"
            binding.passBaruKonfTextLayout.requestFocus()
            return
        }

        //Konfirmasi harus sama
        if (passKonf != passBaru) {
            binding.passBaruKonfTextLayout.error = "Kata sandi harus berbeda, mohon periksa kembali"
            binding.passBaruKonfTextLayout.requestFocus()
            return
        }

        //Semua valid maka tampilkan popup
        AlertDialog.Builder(this)
            .setTitle("Berhasil")
            .setMessage("Kata sandi Anda berhasil diperbarui !")
            .setPositiveButton("Ok") { d, _ ->
                d.dismiss()
                finish() //Kembali ke halaman profile
            }
            .show()
    }
}