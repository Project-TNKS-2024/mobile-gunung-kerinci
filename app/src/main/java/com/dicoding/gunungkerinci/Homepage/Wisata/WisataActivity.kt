package com.dicoding.gunungkerinci.Homepage.Wisata

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.ActivityWisataBinding
import android.os.Build
import android.text.Html
import androidx.lifecycle.lifecycleScope
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch

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
        /*
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
        binding.rvWisata.adapter = adapter */

        loadDestinasi()
    }

    private fun loadDestinasi() {

        lifecycleScope.launch {
            try {
                val response =
                    ApiConfig.getApiService(this@WisataActivity)
                        .getDestinasi()
                if (response.isSuccessful) {
                    val result = response.body()
                    val wisataList = mutableListOf<WisaataaItem>()
                    result?.data?.forEach { destinasi ->
                        val imageUrl =
                            if (destinasi.gambar_destinasi.isNotEmpty()) {
                                "https://eticket-tnks.fst.unja.ac.id/${destinasi.gambar_destinasi[0].src}"
                            } else {
                                ""
                            }
                        val plainText =
                            htmlToText(destinasi.detail)
                        val shortDesc =
                            if (plainText.length > 100)
                                plainText.substring(0, 100) + "..."
                            else
                                plainText
                        wisataList.add(
                            WisaataaItem(
                                id = destinasi.id,
                                title = destinasi.nama,
                                shortDesc = shortDesc,
                                longDesc = plainText,
                                imageUrl = imageUrl
                            )
                        )
                    }
                    binding.rvWisata.layoutManager =
                        LinearLayoutManager(this@WisataActivity)
                    binding.rvWisata.adapter =
                        WisataPageAdapter(wisataList)

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun htmlToText(html: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            Html.fromHtml(html).toString()
        }

    }
}