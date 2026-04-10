package com.dicoding.gunungkerinci.Login

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.databinding.ActivityLoginBinding
import com.dicoding.gunungkerinci.model.LoginRequest
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

            //setupAction()
        binding.buttonMasuk.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (!validateInput(email, password)) return@setOnClickListener
            login(email, password)
        }

        binding.buttonGoogle.setOnClickListener {
            loginWithGoogle()
        }

        binding.buttonRegisNow.setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
        }

        binding.buttonForgetPass.setOnClickListener {
            startActivity(Intent(this, ForgetEmailActivity::class.java))
        }
    }

    private fun loginWithGoogle() {
        lifecycleScope.launch {
            try {
                val response = ApiConfig
                    .getApiService(this@LoginActivity)
                    .googleRedirect()

                if (response.isSuccessful && response.body()?.success == true) {

                    val redirectUrl =
                        response.body()?.data?.redirect_url

                    if (!redirectUrl.isNullOrEmpty()) {
                        openGoogleOAuth(redirectUrl)
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "URL Google tidak tersedia",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Gagal menghubungkan Google",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "Koneksi gagal: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.emailEditText.error = "Email wajib diisi"
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.error = "Format email tidak valid"
            return false
        }

        if (password.isEmpty()) {
            binding.passwordEditText.error = "Password wajib diisi"
            return false
        }

        if (password.length < 8) {
            binding.passwordEditText.error = "Password minimal 8 karakter"
            return false
        }

        return true
    }

    private fun login(email: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = ApiConfig.getApiService(this@LoginActivity)
                    .login(LoginRequest(email, password))

                if (response.isSuccessful && response.body()?.success == true) {

                    val token = response.body()?.data?.token ?: ""

                    saveLoginSession(token, email)

                    Toast.makeText(
                        this@LoginActivity,
                        "Login berhasil",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)

                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Email atau password salah",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "Koneksi gagal: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ✅ SIMPAN TOKEN + EMAIL
    private fun saveLoginSession(token: String, email: String) {
        val pref = getSharedPreferences("auth", MODE_PRIVATE)
        pref.edit()
            .putString("token", token)
            .putString("email", email)
            .putBoolean("IS_LOGGED_IN", true)
            .apply()
    }

    private fun openGoogleOAuth(url: String) {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(this, Uri.parse(url))
    }

}


