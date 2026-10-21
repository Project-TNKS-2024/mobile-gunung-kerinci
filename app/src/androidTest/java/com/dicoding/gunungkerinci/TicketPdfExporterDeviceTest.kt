package com.dicoding.gunungkerinci

import android.graphics.pdf.PdfRenderer
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import com.dicoding.gunungkerinci.Ticket.Barcode.TicketPdfExporter
import com.dicoding.gunungkerinci.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4


import java.io.File
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

@RunWith(AndroidJUnit4::class)


class TicketPdfExporterDeviceTest {
    private val data = TiketData(
        "booking-id", uniqueCode = "TNKS-REAL-CODE", tanggalMasuk = "2026-10-16", tanggalKeluar = "2026-10-18",
        totalPendakiWni = 3, gateMasuk = TiketGate("Kersik Tuo"), gateKeluar = TiketGate("Kersik Tuo"),
        pendakis = listOf("Satria", "Mukhtada", "Opinur").map { TiketAnggota(it, TiketBiodata(it, "")) }
    )

    @Test fun teamDownloadCreatesReadablePdf() = checkPdf(data, false, 1)
    @Test fun individualDownloadCreatesPagePerMember() = checkPdf(data, true, 3)

    @Test fun largeTeamListIsPaginated() {
        val many = data.copy(pendakis = (1..60).map { TiketAnggota("$it", TiketBiodata("Pendaki $it", "")) })
        val file = File.createTempFile("ticket-large-", ".pdf")
        try {
            val pages = file.outputStream().use { TicketPdfExporter.write(many, false, it) }
            assertTrue(pages > 1)
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { assertEquals(pages, it.pageCount) }
            }
        } finally { file.delete() }
    }

    private fun checkPdf(ticket: TiketData, individual: Boolean, expectedPages: Int) {
        val file = File.createTempFile("ticket-", ".pdf")
        try {
            val pages = file.outputStream().use { TicketPdfExporter.write(ticket, individual, it) }
            assertEquals(expectedPages, pages)
            assertTrue(file.readBytes().take(4).toByteArray().toString(Charsets.US_ASCII) == "%PDF")
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    assertEquals(expectedPages, renderer.pageCount)
                    renderer.openPage(0).use { page ->
                        assertTrue(page.width > 0 && page.height > 0)
                        val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                        try {
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val pixels = IntArray(bitmap.width * bitmap.height)
                            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                            val image = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width, bitmap.height, pixels)))
                            assertEquals(ticket.uniqueCode, MultiFormatReader().decode(image).text)
                        } finally { bitmap.recycle() }
                    }
                }
            }
        } finally { file.delete() }
    }
}
