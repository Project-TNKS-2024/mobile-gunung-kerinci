package com.dicoding.gunungkerinci.Homepage.Wisata

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.ActivityBuktiPembayaranBinding
import com.dicoding.gunungkerinci.databinding.ActivityWisataBinding

class WisataActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWisataBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWisataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // BUTTON BACK
        binding.buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // DEFAULT DATA
        val listWisata = listOf(
            WisaataaItem(
                "Danau Gunung Tujuh",
                "Danau tertinggi di Asia Tenggara.",
                "Danau Gunung Tujuh berada di ketinggian 1950 mdpl, dikelilingi tujuh puncak gunung...",
                R.drawable.splash4
            ),
            WisaataaItem(
                "Gunung Kerinci",
                "Gunung Kerinci adalah gunung berapi tertinggi di Indonesia",
                "Gunung Kerinci adalah gunung berapi tertinggi di Indonesia dengan ketinggian 3.805 meter di atas permukaan laut. Terletak di Provinsi Jambi, gunung ini menjadi bagian dari Taman Nasional Kerinci Seblat.",
                R.drawable.splash1
            )
        )

        val adapter = WisataPageAdapter(listWisata)
        binding.rvWisata.layoutManager = LinearLayoutManager(this)
        binding.rvWisata.adapter = adapter
    }
}