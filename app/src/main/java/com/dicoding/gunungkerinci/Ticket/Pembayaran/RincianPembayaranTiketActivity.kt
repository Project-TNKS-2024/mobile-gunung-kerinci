package com.dicoding.gunungkerinci.Ticket.Pembayaran

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.Ticket.DataPendaki.DetailDataPendakiActivity
import com.dicoding.gunungkerinci.databinding.ActivityRincianPembayaranTiketBinding

class RincianPembayaranTiketActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRincianPembayaranTiketBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRincianPembayaranTiketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tombol back di header
        binding.buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Tombol Batalkan → tampil popup
        binding.btnBatalkan.setOnClickListener {
            showPopupBatalkan()
        }

        // Tombol Simpan Draft → tampil popup draft
        binding.btnSimpanDraft.setOnClickListener {
            showPopupDraft()
        }

        binding.btnBayar.setOnClickListener{
            val intent = Intent(this, PembayaranActivity::class.java)
            startActivity(intent)
        }

        binding.lytDetailPendaki.setOnClickListener {
            val intent = Intent(this, DetailDataPendakiActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showPopupDraft() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_draft)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnBatal = dialog.findViewById<Button>(R.id.btnBatalDraft)
        val btnYakin = dialog.findViewById<Button>(R.id.btnYakinDraft)

        btnBatal.setOnClickListener {
            dialog.dismiss()
        }

        btnYakin.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "Draft pemesanan tiket berhasil disimpan", Toast.LENGTH_SHORT).show()

            // kembali ke home
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("navigate_home", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        dialog.show()
    }

    private fun showPopupBatalkan() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_pemesanan)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnBatal = dialog.findViewById<Button>(R.id.btnBatalPemesanan)
        val btnYakin = dialog.findViewById<Button>(R.id.btnYakinPemesanan)

        btnBatal.setOnClickListener {
            dialog.dismiss()
        }

        btnYakin.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "Pemesanan tiket dibatalkan", Toast.LENGTH_SHORT).show()

            // kembali ke home fragment di main activity
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("navigate_home", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        dialog.show()
    }
}