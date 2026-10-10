package com.dicoding.gunungkerinci.Ticket

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.dicoding.gunungkerinci.databinding.ActivityPilihTiketBinding
import com.dicoding.gunungkerinci.model.PaketTiketResponse
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch

class PilihTiketActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPilihTiketBinding

    private var paketUmumId = 0
    private var paketRombonganId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPilihTiketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tombol Back → kembali ke halaman sebelumnya
        binding.buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Tombol UMUM → ke cek kuota tiket
        binding.buttonUmum.setOnClickListener {
            val intent = Intent(this, CekKuotaTiketActivity::class.java)
            //intent.putExtra("jenis_tiket", "umum") // opsional

            intent.putExtra("paket_id", paketUmumId)
            startActivity(intent)
        }

        // Tombol ROMBONGAN → ke cek kuota tiket
        binding.buttonRombongan.setOnClickListener {
            val intent = Intent(this, CekKuotaTiketActivity::class.java)
            //intent.putExtra("jenis_tiket", "rombongan") // opsional

            intent.putExtra("paket_id", paketRombonganId)
            startActivity(intent)
        }

        loadPaketTiket()
    }

    private fun loadPaketTiket() {
        lifecycleScope.launch {
            try {
                val response =
                    ApiConfig.getApiService(this@PilihTiketActivity)
                        .getPaketDestinasi(1)

                if (!response.isSuccessful) return@launch

                val paketList =
                    response.body()?.data?.paket ?: return@launch

                paketList.forEach {
                    when {
                        it.nama.equals("Umum", true) -> {
                            paketUmumId = it.id
                            binding.textUmum.text = it.nama
                            binding.penUmum.text = it.keterangan
                        }

                        it.nama.equals("rombongan", true) -> {
                            paketRombonganId = it.id
                            binding.textRombongan.text = it.nama
                            binding.penRombongan.text = it.keterangan
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }
}