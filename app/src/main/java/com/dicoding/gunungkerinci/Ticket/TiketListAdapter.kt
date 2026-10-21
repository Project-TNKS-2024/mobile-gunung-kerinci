package com.dicoding.gunungkerinci.Ticket

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.model.MyTiketItem
import com.dicoding.gunungkerinci.ui.pelacakan_jejak.TicketUiMapper

class TiketListAdapter(
    private val items: List<MyTiketItem>,
    private val onItemClick: (MyTiketItem) -> Unit
) : RecyclerView.Adapter<TiketListAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val status: TextView = view.findViewById(R.id.tvStatus)
        val createdAt: TextView = view.findViewById(R.id.tvCreatedAt)
        val masuk: TextView = view.findViewById(R.id.tvTanggalMasuk)
        val keluar: TextView = view.findViewById(R.id.tvTanggalKeluar)
        val pendaki: TextView = view.findViewById(R.id.tvPendaki)
        val total: TextView = view.findViewById(R.id.tvTotal)
        val codeRow: View = view.findViewById(R.id.layoutBookingCode)
        val code: TextView = view.findViewById(R.id.tvBookingCode)
        val noticeRow: View = view.findViewById(R.id.layoutTicketNotice)
        val noticeIcon: ImageView = view.findViewById(R.id.ivTicketNotice)
        val notice: TextView = view.findViewById(R.id.tvTicketNotice)
        val overstayRow: View = view.findViewById(R.id.layoutOverstay)
        val overstay: TextView = view.findViewById(R.id.tvOverstayLine)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_tiket_saya, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.status.text = TiketStatus.cardLabel(item)
        holder.createdAt.text = TicketUiMapper.formatTanggal(item.createdAt)
        holder.masuk.text = TicketUiMapper.formatTanggal(item.tanggalMasuk)
        holder.keluar.text = TicketUiMapper.formatTanggal(item.tanggalKeluar)
        holder.pendaki.text = "${(item.totalPendakiWni ?: 0) + (item.totalPendakiWna ?: 0)} Orang"
        holder.total.text = TicketUiMapper.formatRupiah(item.totalPembayaran)

        holder.codeRow.visibility = if (TiketStatus.isTicket(item.statusBooking)) View.VISIBLE else View.GONE
        val prefix = "ID Pemesanan: "
        val code = item.uniqueCode?.takeIf { it.isNotBlank() } ?: item.id
        holder.code.text = SpannableString(prefix + code).apply {
            setSpan(StyleSpan(Typeface.BOLD), prefix.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        background(holder.codeRow, "#C3E5C5")
        val notice = item.ticketNotice ?: item.cancellationReason
        holder.noticeRow.visibility = if (notice.isNullOrBlank()) View.GONE else View.VISIBLE
        holder.notice.text = notice
        val error = item.ticketNoticeType == "error" || item.statusBooking == 9
        background(holder.noticeRow, if (error) "#FDC0BB" else "#FEEBBB")
        holder.noticeIcon.setImageResource(if (error) R.drawable.ticket_error_figma else R.drawable.ticket_warning_figma)
        val labels = TiketStatus.overstayLabels(item.overstay)
        holder.overstayRow.visibility = if (labels.isEmpty()) View.GONE else View.VISIBLE
        holder.overstay.text = labels.joinToString("\n")
        background(holder.overstayRow, "#FEEBBB")
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    private fun background(view: View, color: String) {
        view.background = GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = 12 * view.resources.displayMetrics.density
        }
    }

    override fun getItemCount(): Int = items.size
}
