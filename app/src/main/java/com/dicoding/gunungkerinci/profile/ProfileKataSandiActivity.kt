package com.dicoding.gunungkerinci.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dicoding.gunungkerinci.databinding.ActivityProfileKataSandiBinding
import com.dicoding.gunungkerinci.databinding.PopupBerhasilBinding
import com.dicoding.gunungkerinci.databinding.PopupDataBinding
import com.dicoding.gunungkerinci.model.GantiPasswordRequest
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch
import org.json.JSONObject

class ProfileKataSandiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileKataSandiBinding
    private val api by lazy { ApiConfig.getApiService(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileKataSandiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonBack.setOnClickListener { finish() }

        binding.btnSimpan.setOnClickListener {
            validateInput()
        }
    }

    // ================= VALIDASI INPUT =================
    private fun validateInput() {
        val passLama = binding.passLamaEditText.text.toString().trim()
        val passBaru = binding.passBaruEditText.text.toString().trim()
        val passKonf = binding.passBaruKonfEditText.text.toString().trim()

        binding.passLamaTextLayout.error = null
        binding.passBaruTextLayout.error = null
        binding.passBaruKonfTextLayout.error = null

        if (passLama.isEmpty()) {
            binding.passLamaTextLayout.error = "Kata sandi lama wajib diisi"
            return
        }

        if (passBaru.isEmpty()) {
            binding.passBaruTextLayout.error = "Kata sandi baru wajib diisi"
            return
        }

        if (passBaru.length < 8) {
            binding.passBaruTextLayout.error = "Minimal 8 karakter"
            return
        }

        if (passBaru == passLama) {
            binding.passBaruTextLayout.error =
                "Kata sandi baru tidak boleh sama dengan kata sandi lama"
            return
        }

        if (passKonf != passBaru) {
            binding.passBaruKonfTextLayout.error =
                "Konfirmasi kata sandi tidak sesuai"
            return
        }

        showPopupKonfirmasi(passLama, passBaru, passKonf)
    }

    // ================= POPUP KONFIRMASI =================
    private fun showPopupKonfirmasi(passLama: String, passBaru: String, passKonf: String) {
        val dialogBinding = PopupDataBinding.inflate(LayoutInflater.from(this))

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialogBinding.btnBatalData.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSimpanData.setOnClickListener {
            dialog.dismiss()
            requestGantiPassword(passLama, passBaru, passKonf)
        }

        dialog.show()
    }

    // ================= API CALL =================
    private fun requestGantiPassword(passLama: String, passBaru: String, passKonf: String) {

        val token = "Bearer ${getToken()}"

        val body = GantiPasswordRequest(
            password = passBaru,
            passwordConfirmation = passKonf
        )

        lifecycleScope.launch {
            try {
                val response = api.gantiPassword(token, body)
                if (response.isSuccessful && response.body()?.success == true) {
                    showPopupSukses()
                } else {
                    handleError(response.errorBody()?.string())
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ProfileKataSandiActivity,
                    "Koneksi gagal: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ================= HANDLE ERROR (FIXED) =================
    private fun handleError(errorBody: String?) {
        val errorMessage = errorBody ?: "Gagal mengubah kata sandi"
        try {
            // Coba parse sebagai JSON Object
            val jsonObject = JSONObject(errorMessage)
            val message = jsonObject.getString("message")
            binding.passLamaTextLayout.error = message
        } catch (e: Exception) {
            // Jika gagal, berarti itu hanya String biasa
            binding.passLamaTextLayout.error = errorMessage
        }
    }

    // ================= POPUP SUKSES =================
    private fun showPopupSukses() {
        val dialogBinding = PopupBerhasilBinding.inflate(LayoutInflater.from(this))

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialog.show()

        dialogBinding.root.postDelayed({
            dialog.dismiss()
            finish()
        }, 2000)
    }

    // ================= TOKEN =================
    private fun getToken(): String {
        return getSharedPreferences("auth", MODE_PRIVATE)
            .getString("token", "") ?: ""
    }
}
