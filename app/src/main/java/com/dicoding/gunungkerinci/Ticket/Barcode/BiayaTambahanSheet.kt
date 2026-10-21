package com.dicoding.gunungkerinci.Ticket.Barcode

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.DialogRincianBiayaBinding
import com.dicoding.gunungkerinci.model.OverstayPendaki
import com.dicoding.gunungkerinci.model.OverstaySummary
import com.dicoding.gunungkerinci.ui.pelacakan_jejak.TicketUiMapper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Panel bawah rincian biaya tambahan. Membaca ringkasan overstay dari
 * [BarcodeTiketActivity.currentData] (respons detail terakhir yang berhasil
 * diambil); tidak melakukan fetch kedua.
 */
class BiayaTambahanSheet : BottomSheetDialogFragment() {

    private var _binding: DialogRincianBiayaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogRincianBiayaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val summary = (activity as? BarcodeTiketActivity)?.currentData?.overstay
        render(summary)
    }

    private fun render(os: OverstaySummary?) {
        val pendakis = os?.pendakis.orEmpty()
        val container = binding.layoutContent
        container.removeAllViews()

        val withInfo = pendakis.filter { it.chargeStatus in setOf("estimate", "final", "needs_review") }
        if (withInfo.isEmpty()) {
            container.addView(text("Tidak ada biaya tambahan.", size = 14f))
            return
        }

        withInfo.forEach { p -> renderPendaki(container, p) }

        container.addView(divider())

        renderFooter(container, os)
    }

    private fun renderPendaki(container: LinearLayout, p: OverstayPendaki) {
        val name = p.fullName?.ifBlank { "-" } ?: "-"
        val state = BiayaTambahan.hikeStateLabel(p.hikeState)
        val suffix = when {
            p.chargeStatus == "final" && p.paymentState == "paid" -> " · Lunas"
            p.chargeStatus == "final" && p.paymentState == "unpaid" -> " · Biaya tambahan belum dibayar"
            p.chargeStatus == "estimate" -> " · Estimasi"
            p.chargeStatus == "needs_review" -> " · Menunggu pemeriksaan petugas"
            else -> ""
        }
        container.addView(text("$name · $state$suffix", size = 15f, bold = true))

        if ((p.overdueDays ?: 0) > 0) {
            container.addView(text("Hari tambahan: ${p.overdueDays}", size = 12f, color = R.color.softgrey))
        }

        p.items.orEmpty().forEach { item ->
            val tanggal = TicketUiMapper.formatTanggal(item.tanggal)
            val kategori = BiayaTambahan.kategoriLabel(item.kategori)
            val hari = BiayaTambahan.dayTypeLabel(item.dayType)
            val harga = TicketUiMapper.formatRupiah(item.harga)
            container.addView(text("$tanggal · $kategori · $hari", value = harga))
        }

        // Subtotal / status nominal.
        val subtotal = when (p.chargeStatus) {
            "needs_review" -> "Menunggu pemeriksaan petugas"
            else -> TicketUiMapper.formatRupiah(p.amount)
        }
        container.addView(text("Subtotal", value = subtotal))

        p.actualCheckoutAt?.let { checkout ->
            container.addView(text("Waktu turun", value = formatWib(checkout)))
        }
        p.paidAt?.let { paid ->
            container.addView(text("Waktu pembayaran", value = formatWib(paid)))
        }

        container.addView(divider())
    }

    private fun renderFooter(container: LinearLayout, os: OverstaySummary?) {
        val team = os?.team
        val pendakis = os?.pendakis.orEmpty()

        if (BiayaTambahan.hasEstimate(os)) {
            container.addView(text("Total estimasi", value = TicketUiMapper.formatRupiah(team?.estimateTotal)))
            container.addView(text("Nominal final ditentukan setelah petugas mengonfirmasi waktu turun.", size = 12f, color = R.color.softgrey))
        }

        if (BiayaTambahan.hasFinal(os)) {
            container.addView(text("Total final", value = TicketUiMapper.formatRupiah(team?.finalTotal), bold = true))
        }

        val unpaid = BiayaTambahan.unpaidFinalTotal(pendakis)
        if (unpaid != null && unpaid > 0) {
            container.addView(text("Belum dibayar", value = TicketUiMapper.formatRupiah(unpaid), bold = true))
            container.addView(text("Pembayaran dilakukan di pos kepada petugas.", size = 12f, color = R.color.softgrey))
        } else if (BiayaTambahan.hasNeedsReview(os)) {
            container.addView(text("Total belum lengkap karena sebagian biaya menunggu pemeriksaan petugas.", size = 12f, color = R.color.softgrey))
        }
    }

    // ---- pembangun view sederhana ----

    private fun text(
        text: String,
        value: String? = null,
        size: Float = 13f,
        bold: Boolean = false,
        color: Int = R.color.black2
    ): TextView {
        val tv = TextView(requireContext())
        tv.text = if (value != null) "$text: $value" else text
        tv.setTextColor(ContextCompat.getColor(requireContext(), color))
        tv.textSize = size
        tv.setTypeface(null, if (bold) Typeface.BOLD else Typeface.NORMAL)
        tv.setPadding(0, dp(4), 0, 0)
        return tv
    }

    private fun divider(): View {
        val v = View(requireContext())
        v.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
        ).apply { topMargin = dp(12); bottomMargin = dp(8) }
        v.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.pstlabu))
        return v
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun formatWib(iso: String?): String =
        com.dicoding.gunungkerinci.util.DateTimeFormat.apiWibLongDateTime(iso) ?: "-"

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): BiayaTambahanSheet = BiayaTambahanSheet()
    }
}
