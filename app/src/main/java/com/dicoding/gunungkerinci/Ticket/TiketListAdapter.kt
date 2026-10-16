package com.dicoding.gunungkerinci.Ticket

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.model.MyTiketItem
import com.dicoding.gunungkerinci.ui.pelacakan_jejak.TicketUiMapper

/**
 * Daftar booking pada tab Draft dan Tiket Saya. Satu adapter sederhana; aksi
 * saat diklik ditentukan oleh fragment (lanjut draft vs buka barcode tiket).
 */
class TiketListAdapter(
    private val items: List<MyTiketItem>,
    private val onItemClick: (MyTiketItem) -> Unit
) : RecyclerView.Adapter<TiketListAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvKetua: TextView = view.findViewById(R.id.tvKetua)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvCreatedAt: TextView = view.findViewById(R.id.tvCreatedAt)
        val tvOverstayLine: TextView = view.findViewById(R.id.tvOverstayLine)
        val tvTanggalMasuk: TextView = view.findViewById(R.id.tvTanggalMasuk)
        val tvTanggalKeluar: TextView = view.findViewById(R.id.tvTanggalKeluar)
        val tvGerbang: TextView = view.findViewById(R.id.tvGerbang)
        val tvPendaki: TextView = view.findViewById(R.id.tvPendaki)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tiket_saya, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvKetua.text = TiketStatus.ketuaName(item)
        holder.tvStatus.text = TicketUiMapper.statusLabel(item.statusBooking)
        holder.tvCreatedAt.text = "Pemesanan ${TicketUiMapper.formatTanggal(item.createdAt)}"
        holder.tvTanggalMasuk.text = TicketUiMapper.formatTanggal(item.tanggalMasuk)
        holder.tvTanggalKeluar.text = TicketUiMapper.formatTanggal(item.tanggalKeluar)
        holder.tvGerbang.text = item.gateMasuk?.nama ?: "-"
        holder.tvPendaki.text = TiketStatus.pendakiLabel(item)
        holder.tvTotal.text = TicketUiMapper.formatRupiah(item.totalPembayaran)

        val labels = TiketStatus.overstayLabels(item.overstay)
        if (labels.isEmpty()) {
            holder.tvOverstayLine.visibility = View.GONE
        } else {
            holder.tvOverstayLine.visibility = View.VISIBLE
            holder.tvOverstayLine.text = labels.joinToString("\n")
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
