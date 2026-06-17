package com.dicoding.gunungkerinci.Homepage.Wisata

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dicoding.gunungkerinci.databinding.ActivityDeskWisataBinding
import android.os.Build
import android.text.Html
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch
import android.view.ViewGroup

class DeskWisataActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeskWisataBinding
    private var fullDescription = ""

    var lokasiMaps = ""

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

        binding.textLokasi.setOnClickListener {

            if (lokasiMaps.isNotEmpty()) {

                val gmmIntentUri =
                    Uri.parse(
                        "geo:0,0?q=${Uri.encode(lokasiMaps)}"
                    )

                val mapIntent =
                    Intent(Intent.ACTION_VIEW, gmmIntentUri)

                mapIntent.setPackage("com.google.android.apps.maps")

                startActivity(mapIntent)
            }
        }
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

                    fullDescription = htmlToText(destinasi.detail)

                    binding.Deskripsi.text = fullDescription

                    var isExpanded = false

                    binding.LebihBanyak.setOnClickListener {

                        if (isExpanded) {

                            binding.Deskripsi.maxLines = 5

                            binding.scrollDeskripsi.layoutParams.height =
                                ViewGroup.LayoutParams.WRAP_CONTENT

                            binding.LebihBanyak.text = "Lebih Banyak"

                            //isExpanded = false

                        } else {

                            binding.Deskripsi.maxLines = Int.MAX_VALUE

                            binding.scrollDeskripsi.layoutParams.height =
                                (450 * resources.displayMetrics.density).toInt()

                            binding.LebihBanyak.text = "Lebih Sedikit"

                            //isExpanded = true
                        }

                        binding.scrollDeskripsi.requestLayout()

                        //isExpanded = !isExpanded
                    }

                    if (destinasi.gates.isNotEmpty()) {

                        lokasiMaps = destinasi.gates[0].lokasi

                        binding.textLokasi.text = lokasiMaps

                        /*
                        findViewById<android.widget.TextView>(
                            com.dicoding.gunungkerinci.R.id.textLokasi
                        ).text =
                            destinasi.gates[0].lokasi*/
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