package com.dicoding.gunungkerinci.Login

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import com.dicoding.gunungkerinci.databinding.ActivityVerificationBinding
import java.util.concurrent.TimeUnit

class VerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerificationBinding
    private var email: String? = null
    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 3 * 60 * 1000 //3 menit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Ambil email dari intent (dikirim dari RegistrationActivity)
        email = intent.getStringExtra("email")

        startTimer()

        binding.buttonKirimUlang.setOnClickListener {
            if (timeLeftInMillis <= 0) {
                sendVerificationEmail(email)
                resetTimer()
            } else {
                Toast.makeText(this, "Tunggu sampai waktu habis untuk kirim ulang", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun startTimer() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                binding.textTimer.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                timeLeftInMillis = 0
                binding.textTimer.text = "00:00"
                Toast.makeText(this@VerificationActivity, "Waktu habis, silakan kirim ulang email", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun resetTimer() {
        timeLeftInMillis = 3 * 60 * 1000 // reset 3 menit
        startTimer()
    }

    private fun sendVerificationEmail(email: String?) {
        if (email.isNullOrEmpty()) {
            Toast.makeText(this, "Email tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Ganti dengan API untuk kirim email verifikasi
        Toast.makeText(this, "Link verifikasi dikirim ke $email", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}