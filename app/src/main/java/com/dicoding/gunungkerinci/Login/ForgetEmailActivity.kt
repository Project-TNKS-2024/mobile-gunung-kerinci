package com.dicoding.gunungkerinci.Login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.dicoding.gunungkerinci.databinding.ActivityForgetEmailBinding
import com.dicoding.gunungkerinci.model.ForgotPasswordRequest
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch
import org.json.JSONObject

class ForgetEmailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgetEmailBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgetEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.buttonSend.setOnClickListener {
            //validateEmail()
            sendForgotPassword()
        }
    }

    private fun sendForgotPassword() {
        val email = binding.emailLupaEditText.text.toString().trim()

        // Validasi email
        if (email.isEmpty()) {
            binding.emailLupaEditText.error = "Email harus diisi"
            binding.emailLupaEditText.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLupaEditText.error = "Format email tidak valid"
            binding.emailLupaEditText.requestFocus()
            return
        }

        // Call API
        lifecycleScope.launch {
            try {
                val response = ApiConfig.getApiService(this@ForgetEmailActivity)
                    .forgotPassword(ForgotPasswordRequest(email))

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    //EMAIL TERDAFTAR
                    if (body.success) {
                        Toast.makeText(
                            this@ForgetEmailActivity,
                            body.message ?: "Link reset password telah dikirim",
                            Toast.LENGTH_LONG
                        ).show()

                        //Tetap di halaman ini
                        return@launch
                    }
                }

                //EMAIL TIDAK TERDAFTAR atau ERROR VALIDASI
                val errorBody = response.errorBody()?.string()
                if (!errorBody.isNullOrEmpty()) {
                    val json = JSONObject(errorBody)
                    val errors = json.optJSONObject("errors")

                    if (errors != null && errors.has("email")) {
                        binding.emailLupaEditText.error = "Email Anda belum terdaftar"
                        binding.emailLupaEditText.requestFocus()
                        return@launch
                    }
                }

                //ERROR UMUM
                Toast.makeText(
                    this@ForgetEmailActivity,
                    "Gagal mengirim email reset password",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@ForgetEmailActivity,
                    "Terjadi kesalahan: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /*
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
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("email", email)
            startActivity(intent)
            finish()
        }, 5000) //5 detik
    }
     */
}