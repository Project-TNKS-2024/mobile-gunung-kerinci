package com.dicoding.gunungkerinci.Homepage.Wisata

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.ActivityDeskWisataBinding
import android.os.Build
import android.text.Html
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch

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
        /*
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
         */

        val destinasiId =
            intent.getIntExtra(
                "id_destinasi",
                0
            )

        loadDetailDestinasi(destinasiId)
    }

    private fun loadDetailDestinasi(
        destinasiId: Int
    ) {

        lifecycleScope.launch {

            try {

                val response =
                    ApiConfig.getApiService(this@DeskWisataActivity)
                        .getDetailDestinasi(destinasiId)

                if (response.isSuccessful) {

                    val destinasi =
                        response.body()?.data
                            ?: return@launch

                    binding.JudulWisata.text =
                        destinasi.nama

                    binding.Deskripsi.text =
                        htmlToText(destinasi.detail)

                    if (destinasi.gates.isNotEmpty()) {

                        findViewById<android.widget.TextView>(
                            com.dicoding.gunungkerinci.R.id.textLokasi
                        ).text =
                            destinasi.gates[0].lokasi
                    }

                    val imageUrls =
                        destinasi.gambar_destinasi.map {

                            "https://eticket-tnks.fst.unja.ac.id/${it.src}"

                        }

                    if (imageUrls.isNotEmpty()) {

                        Glide.with(this@DeskWisataActivity)
                            .load(imageUrls[0])
                            .into(binding.imageDesk)
                    }

                    binding.recyclerViewFotoWisata.layoutManager =
                        LinearLayoutManager(
                            this@DeskWisataActivity,
                            LinearLayoutManager.HORIZONTAL,
                            false
                        )

                    binding.recyclerViewFotoWisata.adapter =
                        DeskFotoAdapter(imageUrls) { selectedImage ->

                            Glide.with(this@DeskWisataActivity)
                                .load(selectedImage)
                                .into(binding.imageDesk)

                        }
                }

            } catch (e: Exception) {

                e.printStackTrace()

            }
        }
    }

    private fun htmlToText(html: String): String {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(
                html,
                Html.FROM_HTML_MODE_COMPACT
            ).toString()
        } else {
            Html.fromHtml(html).toString()
        }
    }
}