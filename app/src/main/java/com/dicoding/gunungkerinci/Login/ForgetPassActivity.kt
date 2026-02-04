package com.dicoding.gunungkerinci.Login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dicoding.gunungkerinci.databinding.ActivityForgetPassBinding
import com.dicoding.gunungkerinci.model.BaseResponse
import com.dicoding.gunungkerinci.model.ResetPasswordRequest
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgetPassActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgetPassBinding
    private var resetToken: String? = null   // token dari deeplink

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgetPassBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1️⃣ Ambil token dari deep link
        handleDeepLink(intent)

        binding.buttonSimpan.setOnClickListener {
            validateAndResetPassword()
        }
    }

    private fun handleDeepLink(intent: Intent?) {
        val data: Uri? = intent?.data
        resetToken = data?.lastPathSegment

        if (resetToken.isNullOrEmpty()) {
            Toast.makeText(this, "Token reset tidak valid", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun validateAndResetPassword() {
        val passwordBaru = binding.passwordBaruEditText.text.toString().trim()
        val passwordUlang = binding.passwordUlangEditText.text.toString().trim()

        if (passwordBaru.isEmpty()) {
            binding.passwordBaruEditText.error = "Kata sandi baru harus diisi"
            return
        }

        if (passwordBaru.length < 8) {
            binding.passwordBaruEditText.error = "Minimal 8 karakter"
            return
        }

        if (passwordBaru != passwordUlang) {
            binding.passwordUlangEditText.error = "Kata sandi tidak sesuai"
            return
        }

        resetPasswordApi(passwordBaru, passwordUlang)
    }

    private fun resetPasswordApi(passwordBaru: String, passwordUlang: String) {
        lifecycleScope.launch {
            try {
                val response = ApiConfig
                    .getApiService(this@ForgetPassActivity)
                    .resetPassword(
                        ResetPasswordRequest(
                            token = resetToken!!,
                            password = passwordBaru,
                            passwordConfirmation = passwordUlang
                        )
                    )

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(
                        this@ForgetPassActivity,
                        "Password berhasil diubah",
                        Toast.LENGTH_LONG
                    ).show()

                    startActivity(
                        Intent(this@ForgetPassActivity, LoginActivity::class.java)
                    )
                    finish()

                } else {
                    Toast.makeText(
                        this@ForgetPassActivity,
                        response.body()?.message ?: "Reset password gagal",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@ForgetPassActivity,
                    "Terjadi kesalahan: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
