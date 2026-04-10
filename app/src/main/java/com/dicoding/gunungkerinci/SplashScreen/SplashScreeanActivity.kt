package com.dicoding.gunungkerinci.SplashScreen

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.dicoding.gunungkerinci.Login.RegistrationActivity
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.R

class SplashScreeanActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screean)

        // Tunggu 3 detik lalu pindah ke MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val sharedPref = getSharedPreferences("auth", MODE_PRIVATE)

            val isOnboardingFinished =
                sharedPref.getBoolean("ONBOARDING_FINISHED", false)

            val isLoggedIn =
                sharedPref.getBoolean("IS_LOGGED_IN", false)

            Log.d("SPLASH_DEBUG", "isLoggedIn=$isLoggedIn, onboarding=$isOnboardingFinished")

            when {
                isLoggedIn -> {
                    startActivity(Intent(this, MainActivity::class.java))
                }
                !isOnboardingFinished -> {
                    startActivity(Intent(this, OnboardingActivity::class.java))
                }
                else -> {
                    startActivity(Intent(this, RegistrationActivity::class.java))
                }
            }

            finish()

        }, 3000)
    }
}