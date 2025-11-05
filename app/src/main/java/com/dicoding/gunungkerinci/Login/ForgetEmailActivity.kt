package com.dicoding.gunungkerinci.Login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dicoding.gunungkerinci.databinding.ActivityForgetEmailBinding

class ForgetEmailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgetEmailBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgetEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.buttonSend.setOnClickListener {
            val intent = Intent(this, ForgetPassActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}