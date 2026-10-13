package com.dicoding.gunungkerinci.Ticket.Barcode

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.R
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class TiketIndividuAdapter(
    private val listIndividu: List<TiketModel>
) : RecyclerView.Adapter<TiketIndividuAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNama: TextView = itemView.findViewById(R.id.tvNama)
        val tvNoSeri: TextView = itemView.findViewById(R.id.textNoSeri)
        val tvGerbangMasuk: TextView = itemView.findViewById(R.id.tvGerbangMasuk)
        val tvGerbangKeluar: TextView = itemView.findViewById(R.id.tvGerbangKeluar)
        val tvTanggalMasuk: TextView = itemView.findViewById(R.id.tvTanggalMasuk)
        val tvTanggalKeluar: TextView = itemView.findViewById(R.id.tvTanggalKeluar)
        val imgQr: ImageView = itemView.findViewById(R.id.imgQr)
        val tvKodePemesanan: TextView = itemView.findViewById(R.id.tvKodePemesanan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tiket_individu, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listIndividu[position]

        holder.tvNama.text = item.nama
        holder.tvNoSeri.text = item.noSeri
        holder.tvGerbangMasuk.text = item.gerbangMasuk
        holder.tvGerbangKeluar.text = item.gerbangKeluar
        holder.tvTanggalMasuk.text = item.tanggalMasuk
        holder.tvTanggalKeluar.text = item.tanggalKeluar

        try {
            val bitMatrix = QRCodeWriter().encode(
                item.qrContent,
                BarcodeFormat.QR_CODE,
                400,
                400
            )

            val bitmap = Bitmap.createBitmap(
                400,
                400,
                Bitmap.Config.RGB_565
            )

            for (x in 0 until 400) {
                for (y in 0 until 400) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    )
                }
            }

            holder.imgQr.setImageBitmap(bitmap)
        } catch (e: Exception) {
            holder.imgQr.setImageResource(R.drawable.qrcode)
        }

        holder.tvKodePemesanan.text = item.kodePemesanan
    }

    override fun getItemCount(): Int = listIndividu.size
}
