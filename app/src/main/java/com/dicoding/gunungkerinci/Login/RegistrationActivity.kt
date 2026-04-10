package com.dicoding.gunungkerinci.Login

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import com.dicoding.gunungkerinci.databinding.ActivityRegistrationBinding
import androidx.lifecycle.lifecycleScope
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.model.RegisterRequest
import com.dicoding.gunungkerinci.network.ApiConfig
import com.dicoding.gunungkerinci.pref.UserPreference
import kotlinx.coroutines.launch
import org.json.JSONObject

class RegistrationActivity : AppCompatActivity() {

    private  lateinit var binding: ActivityRegistrationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButton()
        handleOAuthResult(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthResult(intent)
    }

    override fun onResume() {
        super.onResume()
        handleOAuthResult(intent)
    }

    //BUTTON ACTIONS
    private fun setupButton() {

        //Button Daftar Manual
        binding.buttonRegis.setOnClickListener {
            validateManualRegistration()
        }

        //Registrasi dengan Google (BACKEND OAuth)
        binding.buttonGoogle.setOnClickListener {
            registerWithGoogle()
        }

        //Button Masuk Sekarang
        binding.buttonLogNow.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

    }

    // VALIDASI REGISTRASI MANUAL
    private fun validateManualRegistration() {
        val email = binding.emailEditText.text.toString().trim()
        val pass = binding.passwordEditText.text.toString().trim()
        val pass2 = binding.password2EditText.text.toString().trim()

        //validasi email kosong
        if (email.isEmpty()) {
            binding.emailEditText.error = "Email harus diisi"
            binding.emailEditText.requestFocus()
            return
        }
        //Validasi email salah
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.error = "Email tidak sesuai"
            binding.emailEditText.requestFocus()
            return
        }

        //Password Kosong
        if (pass.isEmpty()) {
            binding.passwordEditText.error = " Password harus diisi"
            binding.passwordEditText.requestFocus()
            return
        }
        //Password minimal 8 karakter
        if (pass.length < 8) {
            binding.passwordEditText.error = "Password diisi minimal 8 karakter"
            binding.passwordEditText.requestFocus()
            return
        }

        //Password ulang tidak cocok
        if (pass != pass2) {
            binding.password2EditText.error = "Password tidak sesuai"
            binding.password2EditText.requestFocus()
            return
        }

        registerManualApi(email, pass)

    }

    //REGISTER API
    private fun registerManualApi(email: String, password: String) {

        val request = RegisterRequest(
            email = email,
            password = password,
            password_confirmation = password
        )

        lifecycleScope.launch {
            try {
                val response = ApiConfig.getApiService(this@RegistrationActivity).register(request)


                if (response.isSuccessful && response.body()?.success == true) {

                    val token = response.body()?.data?.token
                    if (!token.isNullOrEmpty()) {
                        UserPreference(this@RegistrationActivity).saveToken(token)
                    }

                    Toast.makeText(
                        this@RegistrationActivity,
                        response.body()?.message,
                        Toast.LENGTH_LONG
                    ).show()

                    // 👉 PINDAH KE HALAMAN VERIFIKASI
                    val intent = Intent(
                        this@RegistrationActivity,
                        VerificationActivity::class.java
                    )
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()

                } else {
                    val errorBody = response.errorBody()?.string()
                    val json = JSONObject(errorBody ?: "{}")
                    val errors = json.optJSONObject("errors")

                    if (errors != null && errors.has("email")) {
                        binding.emailEditText.error = "Email sudah terdaftar"
                        binding.emailEditText.requestFocus()
                    } else {
                        Toast.makeText(
                            this@RegistrationActivity,
                            "Registrasi gagal",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@RegistrationActivity,
                    "Koneksi gagal: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ================= GOOGLE OAUTH (BACKEND) =================
    private fun registerWithGoogle() {
        lifecycleScope.launch {
            try {
                val response =
                    ApiConfig.getApiService(this@RegistrationActivity).googleRedirect()

                Log.d("GOOGLE_OAUTH", "code=${response.code()}")
                Log.d("GOOGLE_OAUTH", "body=${response.body()}")

                if (response.isSuccessful && response.body()?.success == true) {
                    val redirectUrl = response.body()?.data?.redirect_url
                    //openCustomTab(redirectUrl!!)

                    Log.d("OAUTH_DEBUG", "Redirect URL: $redirectUrl")

                    if (!redirectUrl.isNullOrEmpty()) {
                        openCustomTab(redirectUrl)
                    } else {
                        Log.e("OAUTH_DEBUG", "Redirect URL NULL")
                    }
                }
            } catch (e: Exception) {
                Log.e("GOOGLE_OAUTH", "error", e)
                Toast.makeText(
                    this@RegistrationActivity,
                    "Koneksi gagal: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ================= BUKA BROWSER GOOGLE =================
    private fun openCustomTab(url: String) {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(this, Uri.parse(url))
    }

    // ================= HANDLE OAUTH RESULT =================
    private fun handleOAuthResult(intent: Intent?) {
        val data = intent?.data ?: return

        if (data.scheme == "gunungkerinci" && data.host == "oauth") {
            val token = data.getQueryParameter("token")
            val isNewUser = data.getQueryParameter("is_new_user")

            val email = data.getQueryParameter("email")

            if (!token.isNullOrEmpty()) {
                UserPreference(this).saveToken(token)

                val pref = getSharedPreferences("auth", MODE_PRIVATE)
                pref.edit().apply {
                    putString("token", token)
                    if (!email.isNullOrEmpty()) {
                        putString("email", email)
                    }
                    apply()
                }

                if (isNewUser == "true") {
                    startActivity(Intent(this, VerificationActivity::class.java))
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                }
                finish()
            }
        }
    }

}