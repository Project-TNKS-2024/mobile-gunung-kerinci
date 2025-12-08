package com.dicoding.gunungkerinci.Ticket.DataPendaki

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.gunungkerinci.Ticket.Pembayaran.RincianPembayaranTiketActivity
import com.dicoding.gunungkerinci.databinding.ActivityDetailDataPendakiBinding

class DetailDataPendakiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailDataPendakiBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailDataPendakiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tombol back
        binding.buttonBack.setOnClickListener {
            val intent = Intent(this, RincianPembayaranTiketActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        //SETUP RECYCLER VIEW DEFAULT
        binding.recyleViewDataPendaki.layoutManager = LinearLayoutManager(this)

        val listPendaki = listOf(
            Pendaki(
                nama = "Diah Ambarwati",
                idPendaki = "F12345678",
                kewarganegaraan = "WNI",
                noIdentitas = "1234567890123456",
                jenisKelamin = "Perempuan",
                tanggalLahir = "10 - 05 - 2000",
                alamat = "3711, Koto Keras, Sungai Penuh, Jambi, Indonesia",
                noTelepon = "+62 852 1234 5678",
                noDarurat = "+62 852 1234 5678",
                status = "Ketua"
            )
        )

        binding.recyleViewDataPendaki.adapter = DetailPendakiAdapter(listPendaki)
    }
}