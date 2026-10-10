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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.Ticket.TiketDataPendakiActivity
import com.dicoding.gunungkerinci.databinding.ActivityTiketSopBinding
import com.dicoding.gunungkerinci.model.SetujuiSNKRequest
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch

class TiketSOPActivity : AppCompatActivity(), TiketSOPAdapter.SOPListener {

    private lateinit var binding: ActivityTiketSopBinding
    private var isChecked = false

    private lateinit var tiketSopAdapter: TiketSOPAdapter

    private val sopList = mutableListOf<TiketSOPItem>()

    private var bookingId = ""

    private var totalPendaki = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTiketSopBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvSop.layoutManager = LinearLayoutManager(this)

        tiketSopAdapter = TiketSOPAdapter(
            sopList,
            this
        )

        binding.rvSop.adapter = tiketSopAdapter

        bookingId = intent.getStringExtra("booking_id") ?: ""

        totalPendaki = intent.getIntExtra("total_pendaki", 1)

        loadSOP()
    }

    private fun loadSOP() {
        lifecycleScope.launch {
            try {
                val response = ApiConfig.getApiService(this@TiketSOPActivity)
                    .getBookingDetail(bookingId)

                if (!response.isSuccessful) {
                    Toast.makeText(
                        this@TiketSOPActivity,
                        "Gagal mengambil SOP",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val body = response.body()

                if (body?.success != true) {
                    Toast.makeText(
                        this@TiketSOPActivity,
                        body?.message ?: "Data tidak ditemukan",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val sopHtml =
                    body.data.destinasi.sop

                sopList.clear()
                sopList.add(TiketSOPItem.Header)

                sopList.add(
                    TiketSOPItem.Html(
                        sopHtml
                    )
                )

                sopList.add(TiketSOPItem.Footer)
                tiketSopAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Toast.makeText(
                    this@TiketSOPActivity,
                    e.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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
            Toast.makeText(this, "Harap centang persetujuan terlebih dahulu",
                Toast.LENGTH_SHORT)
                .show()
            return
        }
        setujuiSNK()
    }

    private fun setujuiSNK() {
        lifecycleScope.launch {
            try {

                val request = SetujuiSNKRequest(
                    id = bookingId,
                    snk = true
                )

                val response = ApiConfig.getApiService(this@TiketSOPActivity)
                    .setujuiSNK(request)

                if (!response.isSuccessful) {
                    Toast.makeText(
                        this@TiketSOPActivity,
                        "Gagal menyimpan persetujuan",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val body = response.body()

                if (body?.success == true) {

                    Toast.makeText(
                        this@TiketSOPActivity,
                        body.message,
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(
                        this@TiketSOPActivity,
                        TiketDataPendakiActivity::class.java
                    )

                    intent.putExtra(
                        "booking_id",
                        bookingId
                    )

                    intent.putExtra(
                        "total_pendaki",
                        totalPendaki
                    )

                    startActivity(intent)
                    finish()

                } else {

                    Toast.makeText(
                        this@TiketSOPActivity,
                        body?.message ?: "Persetujuan gagal",
                        Toast.LENGTH_SHORT
                    ).show()

                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@TiketSOPActivity,
                    e.message,
                    Toast.LENGTH_SHORT
                ).show()

            }
        }
    }
}