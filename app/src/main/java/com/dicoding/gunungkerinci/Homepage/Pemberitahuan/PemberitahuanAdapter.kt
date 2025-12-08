package com.dicoding.gunungkerinci.Homepage.Pemberitahuan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.R

class PemberitahuanAdapter(private val list: List<PemberitahuanItem>) :
    RecyclerView.Adapter<PemberitahuanAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardViewPemberitahuan)
        val judul: TextView = view.findViewById(R.id.JudulPemberitahuan)
        val desk: TextView = view.findViewById(R.id.DeskPemberitahuan)
        val waktu: TextView = view.findViewById(R.id.WaktuPemberitahuan)
        val tanda: ImageView = view.findViewById(R.id.Tanda)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pemberitahuan, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]

        holder.judul.text = item.judul
        holder.desk.text = item.deskripsi
        holder.waktu.text = item.waktu

        // 🔵 Background biru untuk item pertama
        if (item.isBlue) {
            holder.card.setCardBackgroundColor(
                holder.itemView.context.getColor(R.color.softblue)
            )
            holder.tanda.setImageResource(R.drawable.ic_circle) // tanda biru
        } else {
            holder.card.setCardBackgroundColor(
                holder.itemView.context.getColor(R.color.white)
            )
            holder.tanda.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = list.size
}
