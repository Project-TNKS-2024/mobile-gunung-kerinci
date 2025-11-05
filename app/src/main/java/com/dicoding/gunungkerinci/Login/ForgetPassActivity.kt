package com.dicoding.gunungkerinci.Login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dicoding.gunungkerinci.databinding.ActivityForgetPassBinding

class ForgetPassActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgetPassBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgetPassBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonSimpan.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
}