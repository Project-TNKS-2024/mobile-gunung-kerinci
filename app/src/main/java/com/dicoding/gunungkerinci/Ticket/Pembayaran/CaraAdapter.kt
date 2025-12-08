package com.dicoding.gunungkerinci.Ticket.Pembayaran

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.R

class CaraAdapter( private val listCara: List<CaraItem>):
RecyclerView.Adapter<CaraAdapter.ViewHolder>() {
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nomor: TextView = view.findViewById(R.id.tf_number)
        val isi: TextView = view.findViewById(R.id.tf_isi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cara, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listCara[position]
        holder.nomor.text = item.nomor.toString()
        holder.isi.text = item.isi
    }

    override fun getItemCount(): Int = listCara.size
}