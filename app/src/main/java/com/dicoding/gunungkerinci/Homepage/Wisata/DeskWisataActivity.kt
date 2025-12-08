package com.dicoding.gunungkerinci.Homepage.Wisata

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.ActivityDeskWisataBinding

class DeskWisataActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeskWisataBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeskWisataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // BUTTON BACK
        binding.buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // GET DATA
        val judul = intent.getStringExtra("judul") ?: "Wisata"
        val deskripsi = intent.getStringExtra("deskripsi") ?: ""
        val foto = intent.getIntExtra("foto", 0)

        // SET UI
        binding.JudulWisata.text = judul
        binding.Deskripsi.text = deskripsi
        binding.imageDesk.setImageResource(foto)

        // 3 FOTO DEFAULT
        val fotoList = listOf(
            R.drawable.splash1,
            R.drawable.splash3,
            R.drawable.splash4
        )

        binding.recyclerViewFotoWisata.adapter = DeskFotoAdapter(fotoList)
    }
}