package com.dicoding.gunungkerinci.SplashScreen

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.R

class SplashScreeanActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screean)

        // Tunggu 3 detik lalu pindah ke MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish() // agar splash tidak bisa kembali dengan tombol back
        }, 3000) // 3000 ms = 3 detik
    }
}