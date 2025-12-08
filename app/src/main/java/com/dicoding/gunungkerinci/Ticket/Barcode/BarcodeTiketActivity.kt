package com.dicoding.gunungkerinci.Ticket.Barcode

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.TranslateAnimation
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.ActivityBarcodeTiketBinding

class BarcodeTiketActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBarcodeTiketBinding
    private var indicatorWidth = 0
    private var startPositionTim = 0
    private var startPositionIndividu = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeTiketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInitialIndicator()
        setupRecyclerIndividu()
        setupClickMenu()
        setupNavigationButtons()
    }

    // ---------------------------------------------
    // SETUP INDIKATOR AWAL (sesuaikan ukuran menu)
    // ---------------------------------------------
    private fun setupInitialIndicator() {
        binding.menuContainer.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {

                    // Hitung ukuran indicator = lebar menu TIM
                    indicatorWidth = binding.tvMenuTim.width
                    binding.viewIndicator.layoutParams.width = indicatorWidth

                    startPositionTim = binding.tvMenuTim.left
                    startPositionIndividu = binding.tvMenuIndividu.left

                    moveIndicator(startPositionTim)

                    binding.menuContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        )
    }

    // ---------------------------------------------
    // ANIMASI PINDAH INDIKATOR
    // ---------------------------------------------
    private fun moveIndicator(targetX: Int) {
        val animation = TranslateAnimation(
            binding.viewIndicator.translationX,
            targetX.toFloat(),
            0f,
            0f
        )
        animation.duration = 250
        animation.fillAfter = true
        binding.viewIndicator.startAnimation(animation)
    }

    // ---------------------------------------------
    // MENU TIM & INDIVIDU
    // ---------------------------------------------
    private fun setupClickMenu() {

        val primary = getColor(R.color.primary)
        val black2 = getColor(R.color.black2)

        binding.tvMenuTim.setOnClickListener {
            // ubah warna
            binding.tvMenuTim.setTextColor(primary)
            binding.tvMenuIndividu.setTextColor(black2)

            // pindahkan indikator
            moveIndicator(startPositionTim)

            // tampilkan layout TIM
            binding.layoutTim.visibility = View.VISIBLE
            binding.rvIndividu.visibility = View.GONE
        }

        binding.tvMenuIndividu.setOnClickListener {
            // ubah warna
            binding.tvMenuIndividu.setTextColor(primary)
            binding.tvMenuTim.setTextColor(black2)

            // pindahkan indikator
            moveIndicator(startPositionIndividu)

            // tampilkan layout INDIVIDU
            binding.layoutTim.visibility = View.GONE
            binding.rvIndividu.visibility = View.VISIBLE
        }
    }

    // ---------------------------------------------
    // SETUP RECYCLER VIEW INDIVIDU (DUMMY)
    // ---------------------------------------------
    private fun setupRecyclerIndividu() {
        val dataIndividu = listOf(
            TiketModel(
                nama = "Diah Ambarwati",
                noSeri = "001-01",
                gerbangMasuk = "Kersik Tuo",
                gerbangKeluar = "Camping Ground",
                tanggalMasuk = "01 April 2025",
                tanggalKeluar = "02 April 2025",
                kodePemesanan = "ABCD1234",
                qrImageRes = R.drawable.qrcode
            )
        )

        binding.rvIndividu.layoutManager = LinearLayoutManager(this)
        binding.rvIndividu.adapter = TiketIndividuAdapter(dataIndividu)
    }

    // ---------------------------------------------
    // BACK & BERANDA BUTTON
    // ---------------------------------------------
    private fun setupNavigationButtons() {
        binding.buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnBeranda.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("navigate_home", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }

}