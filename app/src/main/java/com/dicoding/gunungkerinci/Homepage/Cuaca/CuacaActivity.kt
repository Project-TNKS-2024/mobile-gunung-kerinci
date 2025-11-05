package com.dicoding.gunungkerinci.Homepage.Cuaca

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.R

class CuacaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cuaca)

        val rv = findViewById<RecyclerView>(R.id.rvCuaca)
        rv.layoutManager = LinearLayoutManager(this)

        val items = listOf(
            CuacaItem.Header("21°C", "Gerimis Ringan", R.drawable.gerimis_ringan),
            CuacaItem.CardCuaca("Perkiraan Cuaca Jalur Kersik Tuo", "Selasa, 1 April 2025 - Hujan"),
            CuacaItem.CardCuaca("Perkiraan Cuaca Jalur Solok Selatan", "Senin, 15 Januari 2025 - Hujan Ringan"),
            CuacaItem.CardPendaki("8/87")
        )

        rv.adapter = CuacaAdapter(items)
    }
}
