package com.dicoding.gunungkerinci.Ticket.Barcode

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.TranslateAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.ActivityBarcodeTiketBinding
import com.dicoding.gunungkerinci.model.OverstayPendaki
import com.dicoding.gunungkerinci.model.OverstaySummary
import com.dicoding.gunungkerinci.model.TiketData
import com.dicoding.gunungkerinci.network.ApiConfig
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class BarcodeTiketActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBarcodeTiketBinding
    private var indicatorWidth = 0
    private var startPositionTim = 0
    private var startPositionIndividu = 0
    private var bookingId = ""

    /** Respons detail tiket terakhir yang berhasil diambil (dipakai panel biaya). */
    internal var currentData: TiketData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeTiketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInitialIndicator()
        setupClickMenu()
        setupNavigationButtons()

        bookingId = intent.getStringExtra("booking_id").orEmpty()
        if (bookingId.isBlank()) {
            showError("Booking ID tidak ditemukan. Kembali ke daftar tiket.")
            return
        }
        // loadTiket() dipanggil di onResume agar tidak dobel dengan refresh.
    }

    override fun onResume() {
        super.onResume()
        if (bookingId.isNotBlank()) loadTiket()
    }

    // ---------------------------------------------
    // LOAD TIKET ASLI
    // ---------------------------------------------
    private fun loadTiket() {
        binding.scrollContent.visibility = View.GONE
        binding.btnUnduhTiket.visibility = View.GONE
        binding.layoutBiayaRow.visibility = View.GONE
        binding.tvOverstayTitle.visibility = View.GONE
        binding.tvOverstayBody.text = "Memuat tiket…"
        binding.layoutOverstayBanner.visibility = View.VISIBLE
        binding.btnRetry.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val response = ApiConfig.getApiService(this@BarcodeTiketActivity).getTiket(bookingId)
                if (!response.isSuccessful) {
                    showError("Tiket tidak ditemukan atau belum tersedia.")
                    return@launch
                }
                val body = response.body()
                val data = body?.data
                if (body?.success != true || data == null) {
                    showError(body?.message ?: "Tiket tidak tersedia.")
                    return@launch
                }
                render(data)
            } catch (e: Exception) {
                showError("Gagal memuat tiket. Coba lagi.")
            }
        }
    }

    private fun render(data: TiketData) {
        currentData = data
        val code = data.uniqueCode.orEmpty()
        val qr = if (code.isNotBlank()) generateQr(code) else null
        if (qr == null) {
            showError("QR tiket belum dapat ditampilkan. Coba muat ulang tiket.")
            return
        }

        // ---- TIM ----
        val ketua = data.pendakis?.firstOrNull()?.biodata
        val namaKetua = listOf(ketua?.firstName, ketua?.lastName)
            .filterNotNull().joinToString(" ").trim().ifBlank { "-" }

        binding.includeTim.tvNamaPemesan.text = namaKetua
        binding.includeTim.tvJumlahAnggota.text = formatJumlah(data)
        binding.includeTim.tvGerbangMasuk.text = data.gateMasuk?.nama ?: "-"
        binding.includeTim.tvGerbangKeluar.text = data.gateKeluar?.nama ?: "-"
        binding.includeTim.tvTanggalMasuk.text = formatTanggal(data.tanggalMasuk)
        binding.includeTim.tvTanggalKeluar.text = formatTanggal(data.tanggalKeluar)
        binding.includeTim.tvKodePemesananTim.text = code.ifBlank { "-" }
        binding.includeTim.textNoSeri.text = ""
        binding.includeTim.rvQRCodeTim.setImageBitmap(qr)

        // ---- INDIVIDU ----
        val overstayById = data.overstay?.pendakis?.associateBy { it.id }
        val listIndividu = data.pendakis?.map { p ->
            val os = overstayById?.get(p.id)
            TiketModel(
                nama = listOf(p.biodata?.firstName, p.biodata?.lastName)
                    .filterNotNull().joinToString(" ").trim().ifBlank { "-" },
                statusPendaki = os?.let { statusLabel(it) },
                noSeri = "",
                gerbangMasuk = data.gateMasuk?.nama ?: "-",
                gerbangKeluar = data.gateKeluar?.nama ?: "-",
                tanggalMasuk = formatTanggal(data.tanggalMasuk),
                tanggalKeluar = formatTanggal(data.tanggalKeluar),
                kodePemesanan = code,
                qrBitmap = qr
            )
        } ?: emptyList()

        binding.rvIndividu.layoutManager = LinearLayoutManager(this)
        binding.rvIndividu.adapter = TiketIndividuAdapter(listIndividu)

        binding.scrollContent.visibility = View.VISIBLE
        binding.btnUnduhTiket.visibility = View.VISIBLE
        binding.btnRetry.visibility = View.GONE
        binding.tvOverstayTitle.visibility = View.VISIBLE
        renderOverstayBanner(data.overstay)
        renderBiayaRow(data.overstay)
    }

    private fun renderOverstayBanner(os: OverstaySummary?) {
        val team = os?.team
        val overdue = team?.overdueActiveCount ?: 0
        val unpaid = team?.unpaidFinalCount ?: 0

        if (overdue <= 0 && unpaid <= 0) {
            binding.layoutOverstayBanner.visibility = View.GONE
            return
        }

        binding.layoutOverstayBanner.visibility = View.VISIBLE

        if (overdue > 0) {
            binding.tvOverstayTitle.text = "Durasi tiket telah berakhir"
            binding.tvOverstayBody.text =
                "$overdue anggota belum terkonfirmasi turun. " +
                    "Estimasi biaya tambahan: ${formatRupiah(team?.estimateTotal)}"
        } else {
            binding.tvOverstayTitle.text = "Biaya tambahan belum dibayar"
            binding.tvOverstayBody.text =
                "$unpaid tagihan final belum dilunasi. Pembayaran dilakukan di pos kepada petugas."
        }
    }

    private fun statusLabel(p: OverstayPendaki): String {
        val base = when (p.hikeState) {
            "mendaki" -> "Sedang mendaki"
            "turun" -> "Sudah turun"
            "batal" -> "Batal mendaki"
            else -> "Belum masuk"
        }
        val extra = when {
            p.chargeStatus == "final" && p.paymentState == "paid" -> " · Lunas"
            p.chargeStatus == "final" && p.paymentState == "unpaid" -> " · Biaya tambahan belum dibayar"
            p.chargeStatus == "estimate" && (p.overdueDays ?: 0) > 0 -> " · Lewat jadwal turun"
            else -> ""
        }
        return base + extra
    }

    private fun renderBiayaRow(os: OverstaySummary?) {
        if (!BiayaTambahan.hasInfo(os)) {
            binding.layoutBiayaRow.visibility = View.GONE
            return
        }
        binding.layoutBiayaRow.visibility = View.VISIBLE
        binding.tvBiayaStatus.text = biayaStatusText(os)
    }

    private fun biayaStatusText(os: OverstaySummary?): String {
        val team = os?.team
        return when {
            (team?.unpaidFinalCount ?: 0) > 0 -> "Biaya tambahan · Belum dibayar"
            BiayaTambahan.hasNeedsReview(os) -> "Biaya tambahan · Menunggu pemeriksaan"
            BiayaTambahan.hasFinal(os) -> "Biaya tambahan · Lunas"
            BiayaTambahan.hasEstimate(os) -> "Biaya tambahan · Estimasi"
            else -> "Biaya tambahan"
        }
    }

    private fun openBiayaSheet() {
        BiayaTambahanSheet.newInstance().show(supportFragmentManager, "biaya_tambahan")
    }

    private fun generateQr(code: String, size: Int = 512): Bitmap? {
        return try {
            BarcodeEncoder().encodeBitmap(code, BarcodeFormat.QR_CODE, size, size)
        } catch (e: Exception) {
            null
        }
    }

    private fun formatJumlah(data: TiketData): String {
        val wni = data.totalPendakiWni ?: 0
        val wna = data.totalPendakiWna ?: 0
        val parts = mutableListOf<String>()
        if (wni > 0) parts.add("$wni WNI")
        if (wna > 0) parts.add("$wna WNA")
        return if (parts.isEmpty()) "-" else parts.joinToString(" + ")
    }

    private fun formatTanggal(iso: String?): String {
        if (iso.isNullOrBlank()) return "-"
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val output = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
            output.format(input.parse(iso.take(10))!!)
        } catch (e: Exception) {
            iso
        }
    }

    private fun formatRupiah(n: Int?): String {
        val v = n ?: 0
        val s = v.toString().reversed().chunked(3).joinToString(".").reversed()
        return "Rp $s"
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        binding.scrollContent.visibility = View.GONE
        binding.btnUnduhTiket.visibility = View.GONE
        binding.layoutBiayaRow.visibility = View.GONE
        binding.includeTim.rvQRCodeTim.setImageDrawable(null)
        binding.rvIndividu.adapter = null
        binding.tvOverstayTitle.visibility = View.GONE
        binding.tvOverstayBody.text = message
        binding.tvOverstayBody.visibility = View.VISIBLE
        binding.layoutOverstayBanner.visibility = View.VISIBLE
        binding.btnRetry.visibility = if (bookingId.isNotBlank()) View.VISIBLE else View.GONE
    }

    // ---------------------------------------------
    // SETUP INDIKATOR AWAL (sesuaikan ukuran menu)
    // ---------------------------------------------
    private fun setupInitialIndicator() {
        binding.menuContainer.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    indicatorWidth = binding.tvMenuTim.width
                    binding.viewIndicator.layoutParams.width = indicatorWidth
                    startPositionTim = binding.tvMenuTim.left
                    startPositionIndividu = binding.tvMenuIndividu.left
                    moveIndicator(startPositionTim)
                    binding.menuContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        )
    }

    // ---------------------------------------------
    // ANIMASI PINDAH INDIKATOR
    // ---------------------------------------------
    private fun moveIndicator(targetX: Int) {
        val animation = TranslateAnimation(
            binding.viewIndicator.translationX,
            targetX.toFloat(),
            0f,
            0f
        )
        animation.duration = 250
        animation.fillAfter = true
        binding.viewIndicator.startAnimation(animation)
    }

    // ---------------------------------------------
    // MENU TIM & INDIVIDU
    // ---------------------------------------------
    private fun setupClickMenu() {
        val primary = getColor(R.color.primary)
        val black2 = getColor(R.color.black2)

        binding.tvMenuTim.setOnClickListener {
            binding.tvMenuTim.setTextColor(primary)
            binding.tvMenuIndividu.setTextColor(black2)
            moveIndicator(startPositionTim)
            binding.layoutTim.visibility = View.VISIBLE
            binding.rvIndividu.visibility = View.GONE
        }

        binding.tvMenuIndividu.setOnClickListener {
            binding.tvMenuIndividu.setTextColor(primary)
            binding.tvMenuTim.setTextColor(black2)
            moveIndicator(startPositionIndividu)
            binding.layoutTim.visibility = View.GONE
            binding.rvIndividu.visibility = View.VISIBLE
        }
    }

    // ---------------------------------------------
    // BACK & BERANDA BUTTON
    // ---------------------------------------------
    private fun setupNavigationButtons() {
        binding.btnRetry.setOnClickListener { loadTiket() }
        binding.layoutBiayaRow.setOnClickListener { openBiayaSheet() }
        binding.buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnBeranda.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("navigate_home", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }
}
