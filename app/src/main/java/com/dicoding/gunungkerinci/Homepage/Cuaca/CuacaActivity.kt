package com.dicoding.gunungkerinci.Homepage.Cuaca

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dicoding.gunungkerinci.databinding.ActivityCuacaBinding
import androidx.lifecycle.lifecycleScope
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch
import android.util.Log


class CuacaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCuacaBinding



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCuacaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // BUTTON BACK
        binding.buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        loadInfoGunung()
    }

    private fun loadInfoGunung() {
        lifecycleScope.launch {
            try {
                Log.d("CUACA", "Mulai request")

                val response =
                    ApiConfig.getApiService(this@CuacaActivity)
                        .getDetailDestinasi(1)

                Log.d("CUACA", "Response code = ${response.code()}")

                if (response.isSuccessful) {
                    val destinasi =
                        response.body()?.data

                    Log.d("CUACA", "Nama destinasi = ${destinasi?.nama}")
                    Log.d("CUACA", "Jumlah gate = ${destinasi?.gates?.size}")

                    destinasi?.gates?.forEach {
                        Log.d(
                            "CUACA",
                            "Gate ${it.nama} max=${it.max_pendaki_hari}"
                        )
                    }

                }

            } catch (e: Exception) {
                //e.printStackTrace()
                Log.e("CUACA", "ERROR", e)
            }
        }
    }
}
