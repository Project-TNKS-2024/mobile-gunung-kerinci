package com.dicoding.gunungkerinci.Login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.widget.Toast
import com.dicoding.gunungkerinci.databinding.ActivityForgetEmailBinding

class ForgetEmailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgetEmailBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgetEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.buttonSend.setOnClickListener {
            validateEmail()
        }
    }

    private fun validateEmail() {
        val email = binding.emailLupaEditText.text.toString().trim()

        //Email kosong
        if (email.isEmpty()) {
            binding.emailLupaEditText.error = "Email harus diisi"
            binding.emailLupaEditText.requestFocus()
            return
        }

        //Format email salah
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLupaEditText.error = "Format email salah"
            binding.emailLupaEditText.requestFocus()
            return
        }

        //Email valid dan link terkirim ke email
        Toast.makeText(this, "Link reset password telah dikirim ke $email", Toast.LENGTH_SHORT).show()

        //Pindah otomatis ke halaman ForgetPassActivity (untuk demo)
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, ForgetPassActivity::class.java)
            intent.putExtra("email", email)
            startActivity(intent)
            finish()
        }, 5000) //5 detik
    }
}