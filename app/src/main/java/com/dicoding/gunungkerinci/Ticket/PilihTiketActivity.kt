package com.dicoding.gunungkerinci.Ticket

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.ActivityFormLaporanBinding
import com.dicoding.gunungkerinci.databinding.ActivityPilihTiketBinding

class PilihTiketActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPilihTiketBinding

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
            intent.putExtra("jenis_tiket", "umum") // opsional
            startActivity(intent)
        }

        // Tombol ROMBONGAN → ke cek kuota tiket
        binding.buttonRombongan.setOnClickListener {
            val intent = Intent(this, CekKuotaTiketActivity::class.java)
            intent.putExtra("jenis_tiket", "rombongan") // opsional
            startActivity(intent)
        }
    }
}