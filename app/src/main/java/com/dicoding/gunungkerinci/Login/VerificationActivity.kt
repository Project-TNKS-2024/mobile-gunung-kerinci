package com.dicoding.gunungkerinci.Login

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.dicoding.gunungkerinci.databinding.ActivityVerificationBinding
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class VerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerificationBinding
    private var countDownTimer: CountDownTimer? = null

    //Timer
    private var TOTAL_TIME: Long = 10 * 1000 //3 menit
    private var timeLeftInMillis = TOTAL_TIME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Toast.makeText(
            this,
            "Silakan cek email Anda untuk verifikasi",
            Toast.LENGTH_SHORT
        ).show()

        startTimer()

        //Tombol kirim ulang link
        binding.buttonKirimUlang.setOnClickListener {
            if (timeLeftInMillis <= 0) {
                resendVerificationEmail()
            } else {
                Toast.makeText(
                    this,
                    "Silakan tunggu sampai waktu habis untuk kirim ulang",
                    Toast.LENGTH_SHORT
                ).show()
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

                Toast.makeText(
                    this@VerificationActivity,
                    "Waktu habis, silakan kirim ulang email",
                    Toast.LENGTH_SHORT
                ).show()

            }
        }.start()
    }

    private fun resetTimer() {
        timeLeftInMillis = TOTAL_TIME
        startTimer()
    }

    private fun resendVerificationEmail() {
        lifecycleScope.launch {
            try {
                val response = ApiConfig.getApiService(this@VerificationActivity).resendEmailVerification()

                if (response.isSuccessful && response.body()?.success == true) {

                    Toast.makeText(
                        this@VerificationActivity,
                        "Email verifikasi berhasil dikirim ulang",
                        Toast.LENGTH_LONG
                    ).show()

                    resetTimer()

                } else {
                    Toast.makeText(
                        this@VerificationActivity,
                        "Gagal mengirim email verifikasi",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@VerificationActivity,
                    "Koneksi gagal: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}