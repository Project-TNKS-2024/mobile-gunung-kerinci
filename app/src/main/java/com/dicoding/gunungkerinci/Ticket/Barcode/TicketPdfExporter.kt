package com.dicoding.gunungkerinci.Ticket.Barcode

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.dicoding.gunungkerinci.model.TiketData
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.io.OutputStream

/** PDF lokal dari respons tiket asli; QR tetap memakai unique_code yang dikenali petugas. */
internal object TicketPdfExporter {
    fun write(data: TiketData, individual: Boolean, output: OutputStream): Int {
        val code = requireNotNull(data.uniqueCode?.takeIf { it.isNotBlank() }) { "Kode tiket belum tersedia" }
        val names = data.pendakis.orEmpty().map { member ->
            listOfNotNull(member.biodata?.firstName, member.biodata?.lastName)
                .joinToString(" ").trim().ifBlank { "Pendaki" }
        }
        require(!individual || names.isNotEmpty()) { "Data tiket individu belum tersedia" }
        val qr = BarcodeEncoder().encodeBitmap(code, BarcodeFormat.QR_CODE, 512, 512)
        try {
            val document = PdfDocument()
            try {
                var page: PdfDocument.Page? = null
                var pageCount = 0
                var y = 0f
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                fun newPage() {
                    page?.let { document.finishPage(it) }
                    pageCount++
                    page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageCount).create())
                    page!!.canvas.drawColor(Color.WHITE)
                    y = 52f
                }
                fun line(value: String, size: Float = 12f, bold: Boolean = false) {
                    paint.textSize = size
                    paint.color = Color.rgb(24, 47, 70)
                    paint.typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
                    val height = size * 1.55f
                    for (paragraph in value.split('\n')) {
                        var remaining = paragraph.ifBlank { " " }
                        while (remaining.isNotEmpty()) {
                            if (y + height > 796f) newPage()
                            val count = paint.breakText(remaining, true, 511f, null).coerceAtLeast(1)
                            page!!.canvas.drawText(remaining.take(count), 42f, y, paint)
                            remaining = remaining.drop(count)
                            y += height
                        }
                    }
                }
                val ticketNames: List<String?> = if (individual) names else listOf(null)
                for (name in ticketNames) {
                    newPage()
                    line("GUNUNG KERINCI", 24f, true)
                    line(if (individual) "E-Tiket Individu" else "E-Tiket Tim", 16f, true)
                    y += 14f
                    name?.let { line("Nama pendaki: $it", bold = true) }
                    line("Kode booking: $code", bold = true)
                    line("Pintu masuk: ${data.gateMasuk?.nama ?: "—"}")
                    line("Pintu keluar: ${data.gateKeluar?.nama ?: "—"}")
                    line("Tanggal masuk: ${data.tanggalMasuk?.take(10) ?: "—"}")
                    line("Tanggal keluar: ${data.tanggalKeluar?.take(10) ?: "—"}")
                    line("Jumlah pendaki: ${data.totalPendakiWni ?: 0} WNI + ${data.totalPendakiWna ?: 0} WNA")
                    y += 16f
                    if (y + 190f > 796f) newPage()
                    page!!.canvas.drawBitmap(qr, null, Rect(42, y.toInt(), 212, y.toInt() + 170), null)
                    y += 190f
                    line("Tunjukkan QR booking ini kepada petugas.", 10f)
                    if (individual) {
                        line("QR terhubung ke booking rombongan yang sama.", 10f)
                    } else {
                        y += 12f
                        line("Daftar pendaki", 14f, true)
                        names.forEachIndexed { index, member -> line("${index + 1}. $member") }
                    }
                    y += 12f
                    line("Berlaku sesuai jadwal dan status booking. Konfirmasi kepulangan kepada petugas.", 10f)
                }
                page?.let { document.finishPage(it) }
                document.writeTo(output)
                return pageCount
            } finally { document.close() }
        } finally { qr.recycle() }
    }
}
