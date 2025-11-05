package com.dicoding.gunungkerinci.Homepage.Panduan

import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.R

class PanduanActivity : AppCompatActivity() {

    private lateinit var tvMenuLeft: TextView
    private lateinit var tvMenuRight: TextView
    private lateinit var viewIndicator: View
    private lateinit var menuContainer: FrameLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var adapter: PanduanAdapter

    private val dataPemesanan = listOf(
        PanduanItem("Login atau Daftar", "Setelah berhasil masuk, pastikan akun anda " +
                "sudah di valiadasi oleh admin dengan melalukan validasi pada halaman ‘Akun’."),
        PanduanItem("Pesan Tiket", "Lakukan pemesanan tiket dengan ‘pesan tiket’ " +
                "melalui beranda atau memilih menu ‘Tiket’, kemudian ‘Tambah Tiket’. Pilih jenis " +
                "tiket dan cek kuota  yang tersedia untuk memastikan kuota pendakian masih ada pada " +
                "tanggal yang Anda pilih."),
        PanduanItem("Setujui Syarat dan Ketentuan", "Baca dan setujui syarat serta " +
                "ketentuan yang berlaku sesuai SOP pendakian Gunung Kerinci."),
        PanduanItem("Isi Formulir Pendaftaran", "Lengkapi formulir pendaftaran dengan " +
                "memasukan kode pendaki yang akan melakukan pendakian. Pastikan pendaki yang akan " +
                "melakukan pendakian telah memiliki akun yang tervalidasi sebelumnya."),
        PanduanItem("Pembayaran", "Lakukan pembayaran dan unggah bukti pembayaran, " +
                "kemudian tunggu validasi pembayaran oleh admin."),
        PanduanItem("Gunakan QR Tiket", "Setelah pembayaran berhasil divalidasi, anda " +
                "akan mendapatkan kode QR tiket. Scan kode QR tersebut di pintu masuk untuk memulai " +
                "pendakian.")
    )

    //private val dataKosong = emptyList<PanduanItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panduan)

        //Inisialisasi View
        tvMenuLeft = findViewById(R.id.tvMenuLeft)
        tvMenuRight = findViewById(R.id.tvMenuRight)
        viewIndicator = findViewById(R.id.viewIndicator)
        menuContainer = findViewById(R.id.menuContainer)
        recyclerView = findViewById(R.id.rvPanduan)
        layoutEmpty = findViewById(R.id.layoutEmpty)

        //Setup RecyclerView
        adapter = PanduanAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Default pilih menu kiri
        tvMenuLeft.post {
            selectTab(tvMenuLeft, false)
            showRecyclerView()
            adapter.updateList(dataPemesanan)
        }

        //Klik menu Kiri
        tvMenuLeft.setOnClickListener {
            selectTab(tvMenuLeft, true)
            showRecyclerView()
            adapter.updateList(dataPemesanan)
        }

        //Klik menu kanan
        tvMenuRight.setOnClickListener {
            selectTab(tvMenuRight, true)
            showEmptyLayout()
        }

        // Tombol back
        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener {
            finish()
        }
    }

    private fun selectTab(selected: TextView, animate: Boolean) {
        val colorPrimary = ContextCompat.getColor(this, R.color.primary)
        val colorGray = ContextCompat.getColor(this, R.color.darkgrey)

        //Reset semua tab ke abu-abu
        tvMenuLeft.setTextColor(colorGray)
        tvMenuRight.setTextColor(colorGray)
        tvMenuLeft.typeface = Typeface.DEFAULT
        tvMenuRight.typeface = Typeface.DEFAULT

        //Tab yang dipilih -> biru dan bold
        selected.setTextColor(colorPrimary)
        selected.typeface = Typeface.DEFAULT_BOLD

        //Geser indicator
        menuContainer.post {
            val targetX = selected.left
            val width = selected.width

            val lp = viewIndicator.layoutParams
            lp.width = width
            viewIndicator.layoutParams = lp

            if (animate) {
                viewIndicator.animate().x(targetX.toFloat()).setDuration(200).start()
            } else {
                viewIndicator.x = targetX.toFloat()
            }
        }
    }

    private fun showRecyclerView() {
        recyclerView.visibility = View.VISIBLE
        layoutEmpty.visibility = View.GONE
    }

    private fun showEmptyLayout() {
        recyclerView.visibility = View.GONE
        layoutEmpty.visibility = View.VISIBLE
    }
}