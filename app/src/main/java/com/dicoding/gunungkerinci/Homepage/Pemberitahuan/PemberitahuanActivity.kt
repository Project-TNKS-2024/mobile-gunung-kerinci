package com.dicoding.gunungkerinci.Homepage.Pemberitahuan

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.R

class PemberitahuanActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pemberitahuan)

        val rv = findViewById<RecyclerView>(R.id.rvSop)
        rv.layoutManager = LinearLayoutManager(this)

        val listNotifikasi = listOf(
            PemberitahuanItem(
                judul = "Sistem Pemeliharaan Jalur Pendakian",
                deskripsi = "Pendaki harap berhati-hati, beberapa jalur licin akibat hujan.",
                waktu = "Hari ini, 08:30",
                isBlue = true
            ),
            PemberitahuanItem(
                judul = "Cuaca Cerah untuk 3 Hari Kedepan",
                deskripsi = "Perkiraan cuaca menunjukkan kondisi cerah. Pendakian aman dilakukan.",
                waktu = "Kemarin, 17:45",
                isBlue = false
            )
        )

        rv.adapter = PemberitahuanAdapter(listNotifikasi)

        // tombol back
        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

    }
}
