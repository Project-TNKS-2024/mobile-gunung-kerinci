package com.dicoding.gunungkerinci.Ticket.SOP

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.Ticket.TiketDataPendakiActivity
import com.dicoding.gunungkerinci.databinding.ActivityTiketSopBinding

class TiketSOPActivity : AppCompatActivity(), SOPAdapter.SOPListener {

    private lateinit var binding: ActivityTiketSopBinding
    private var isChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTiketSopBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sopList = mutableListOf<SOPItem>()

        sopList.add(SOPItem.Header)

        sopList.add(SOPItem.Title("A. Jalur Pendakian"))
        sopList.add(SOPItem.Content("1. Jalur pendakian melalui ..."))
        sopList.add(SOPItem.Content("2. Jalur pendakian melalui ..."))

        sopList.add(SOPItem.Title("B. Ketentuan"))
        sopList.add(SOPItem.Subtitle("Peraturan Umum"))
        sopList.add(SOPItem.Content("1. Para pendaki wajib menjaga ..."))
        sopList.add(SOPItem.Content("2. Setiap calon pendaki ..."))
        sopList.add(SOPItem.Content("3. ... dan seterusnya"))

        sopList.add(SOPItem.Title("C. Kegiatan Pendakian Dilarang"))
        sopList.add(SOPItem.Content("1. Membawa satwa ..."))
        sopList.add(SOPItem.Content("2. Mengambil tanaman ..."))
        sopList.add(SOPItem.Content("3. Membakar hutan ..."))
        sopList.add(SOPItem.Content("..."))

        sopList.add(SOPItem.Title("D. Sanksi"))
        sopList.add(SOPItem.Content("1. Bagi pendaki yang ..."))
        sopList.add(SOPItem.Content("2. Sanksi administratif ..."))

        sopList.add(SOPItem.Footer)

        binding.rvSop.layoutManager = LinearLayoutManager(this)
        binding.rvSop.adapter = SOPAdapter(sopList, this)

    }

    //Download dokumen sop
    override fun onDownloadClicked() {
        showPopupDownload()
    }

    //Pop up dokumen terunduh
    private fun showPopupDownload() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_dokunduh)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)
        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            if (dialog.isShowing) dialog.dismiss()
        }, 5000)
    }

    override fun onCheckStateChanged(checked: Boolean) {
        isChecked = checked
    }

    override fun onBackClicked() {
        onBackPressedDispatcher.onBackPressed()
    }
    override fun onNextClicked() {
        if (!isChecked) {
            Toast.makeText(this, "Harap centang persetujuan terlebih dahulu", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Jika sudah centang → lanjut ke activity berikutnya
        val intent = Intent(this, TiketDataPendakiActivity::class.java)
        startActivity(intent)
    }
}