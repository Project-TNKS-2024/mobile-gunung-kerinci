package com.dicoding.gunungkerinci.Homepage.Panduan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.R

class PanduanAdapter(private var items: List<PanduanItem>):
    RecyclerView.Adapter<PanduanAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvNumber: TextView = view.findViewById(R.id.tvNumber)
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_panduan, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvNumber.text = "#${position + 1}"
        holder.tvTitle.text = item.tittle ?: ""
        holder.tvDescription.text = item.description

        // Sembunyikan title kalau null/empty
        holder.tvTitle.visibility = if (item.tittle.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<PanduanItem>) {
        items = newList
        notifyDataSetChanged()
    }

}