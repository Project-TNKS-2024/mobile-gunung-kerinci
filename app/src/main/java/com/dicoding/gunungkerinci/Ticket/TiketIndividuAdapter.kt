package com.dicoding.gunungkerinci.Ticket

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.R

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
        holder.imgQr.setImageResource(item.qrImageRes)
        holder.tvKodePemesanan.text = item.kodePemesanan
    }

    override fun getItemCount(): Int = listIndividu.size
}
