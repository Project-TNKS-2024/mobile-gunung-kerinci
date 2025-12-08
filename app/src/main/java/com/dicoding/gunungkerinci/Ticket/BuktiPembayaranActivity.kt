package com.dicoding.gunungkerinci.Ticket

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.Ticket.Barcode.BarcodeTiketActivity
import com.dicoding.gunungkerinci.databinding.ActivityBuktiPembayaranBinding
import java.text.SimpleDateFormat
import java.util.Locale

class BuktiPembayaranActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBuktiPembayaranBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBuktiPembayaranBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // BUTTON BACK
        binding.buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // BUTTON BERANDA
        binding.btnBeranda.setOnClickListener {
            //val intent = Intent(this, MainActivity::class.java)
            val intent = Intent(this, BarcodeTiketActivity::class.java)
            intent.putExtra("navigate_home", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        // LAYOUT DOWNLOAD INVOICE → Popup Dokunduh
        binding.lytUnduh.setOnClickListener {
            showPopupUnduh()
        }

        // OTOMATIS UPDATE STATUS SETELAH 5 DETIK
        Handler(Looper.getMainLooper()).postDelayed({
            updateStatusPembayaran()
        }, 5000)
    }

    // UPDATE TABEL RIWAYAT PEMBAYARAN SETELAH 5 DETIK
    private fun updateStatusPembayaran() {

        // Format tanggal hari ini
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val today = "20-03-2025"
        //val today = sdf.format(Date())

        // Cari komponen Tabel
        val tvTanggal = findViewById<TextView>(R.id.tvTanggal)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvKet = findViewById<TextView>(R.id.tvKet)

        // Update kolom
        tvTanggal.text = today
        tvStatus.text = "Disetujui"
        tvKet.text = "-"

        // Ubah background status menjadi hijau sukses
        tvStatus.setBackgroundColor(Color.parseColor("#4CB050"))
        tvStatus.setTextColor(Color.parseColor("#FFFFFF"))

        // Tampilkan layout approved
        binding.layoutApproved.visibility = android.view.View.VISIBLE

        Toast.makeText(this, "Pembayaran berhasil diverifikasi", Toast.LENGTH_SHORT).show()
    }

    // POPUP DOKUMEN UNDUH
    private fun showPopupUnduh() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_dokunduh)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.show()

        // Popup otomatis hilang setelah 2 detik
        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
        }, 2000)

    }
}