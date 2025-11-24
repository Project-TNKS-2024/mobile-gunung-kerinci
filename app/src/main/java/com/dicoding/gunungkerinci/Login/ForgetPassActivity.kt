package com.dicoding.gunungkerinci.Login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.dicoding.gunungkerinci.databinding.ActivityForgetPassBinding

class ForgetPassActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgetPassBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgetPassBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonSimpan.setOnClickListener {
            validateNewPassword()
        }

    }

    private fun validateNewPassword() {
        val passwordBaru = binding.passwordBaruEditText.text.toString().trim()
        val passwordUlang = binding.passwordUlangEditText.text.toString().trim()

        // Password baru kosong
        if (passwordBaru.isEmpty()) {
            binding.passwordBaruEditText.error = "Kata sandi baru harus diisi"
            binding.passwordBaruEditText.requestFocus()
            return
        }

        // Password baru minimal 8 karakter
        if (passwordBaru.length < 8) {
            binding.passwordBaruEditText.error = "Minimal 8 karakter"
            binding.passwordBaruEditText.requestFocus()
            return
        }

        // Password ulang kosong
        if (passwordUlang.isEmpty()) {
            binding.passwordUlangEditText.error = "Ulangi kata sandi harus diisi"
            binding.passwordUlangEditText.requestFocus()
            return
        }

        // Password ulang tidak sama
        if (passwordBaru != passwordUlang) {
            binding.passwordUlangEditText.error = "Kata sandi tidak sesuai"
            binding.passwordUlangEditText.requestFocus()
            return
        }

        // Jika lolos semua validasi
        Toast.makeText(
            this,
            "Kata sandi Anda telah berhasil diubah.",
            Toast.LENGTH_SHORT
        ).show()

        // Pindah ke Login
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}