package com.dicoding.gunungkerinci.SplashScreen

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.dicoding.gunungkerinci.Homepage.Sop.SopActivity
import com.dicoding.gunungkerinci.Login.LoginActivity
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.R
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator

class OnboardingActivity: AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var buttonSkip: LinearLayout
    private lateinit var buttonMasuk: Button
    private lateinit var dotsIndicator: WormDotsIndicator

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPageronboard)
        buttonSkip = findViewById(R.id.buttonSkip)
        buttonMasuk = findViewById(R.id.buttonMasuk)
        dotsIndicator = findViewById(R.id.dotsIndicator)

        val items = listOf(
            OnboardingItem(
                "Jelajahi Gunung Kerinci dengan Mudah",
                "Pesan tiket pendakian secara online, tanpa ribet!",
                R.drawable.splash1
            ),
            OnboardingItem(
                "Akses Cepat & Parktis",
                "Scan QR langsung di gerbang untuk masuk tanpa antre",
                R.drawable.splash1
            ),
            OnboardingItem(
                "Pendakian Lebih Aman & Nyaman",
                "Dapatkan info cuaca dan kondisi keamanan gunung secara real-time",
                R.drawable.splash1
            )
        )

        val adapter = OnboardingAdapter(this, items)

        buttonSkip.setOnClickListener {
            goToMain()
        }

        buttonMasuk.setOnClickListener {
            goToMain()
        }

        viewPager.adapter = adapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == items.size - 1) {
                    buttonSkip.visibility = View.GONE
                    buttonMasuk.visibility = View.VISIBLE
                } else {
                    buttonSkip.visibility = View.VISIBLE
                    buttonMasuk.visibility = View.GONE
                }
            }
        })

        dotsIndicator.setViewPager2(viewPager)
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}