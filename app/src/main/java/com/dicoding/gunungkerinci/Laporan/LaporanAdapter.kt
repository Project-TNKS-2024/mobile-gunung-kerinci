package com.dicoding.gunungkerinci.Laporan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.R

class LaporanAdapter(
    private val list: List<LaporanModel>
) : RecyclerView.Adapter<LaporanAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fotoProfile = itemView.findViewById<ImageView>(R.id.fotoProfile)
        val tvUser = itemView.findViewById<TextView>(R.id.tvUser)
        val tvWaktu = itemView.findViewById<TextView>(R.id.tvWaktu)
        val tvLokasiA = itemView.findViewById<TextView>(R.id.tvLokasiA)
        val tvLokasiB = itemView.findViewById<TextView>(R.id.tvLokasiB)
        val tvDeskripsi = itemView.findViewById<TextView>(R.id.tvDeskripsi)

        val img1 = itemView.findViewById<ImageView>(R.id.img1)
        val img2 = itemView.findViewById<ImageView>(R.id.img2)
        val img3 = itemView.findViewById<ImageView>(R.id.img3)
        val img4 = itemView.findViewById<ImageView>(R.id.img4)

        val imageSlots = listOf(img1, img2, img3, img4)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_laporan, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.tvUser.text = item.nama
        holder.tvWaktu.text = item.waktu
        holder.tvLokasiA.text = item.lokasiA
        holder.tvLokasiB.text = item.lokasiB
        holder.tvDeskripsi.text = item.deskripsi

        //foto profile default
        holder.fotoProfile.setImageResource(R.drawable.ic_profile)

        // tampilkan jumlah foto sesuai jumlah yang ada
        for (i in holder.imageSlots.indices) {
            if (i < item.foto.size) {
                holder.imageSlots[i].visibility = View.VISIBLE
                holder.imageSlots[i].setImageResource(item.foto[i])
            } else {
                holder.imageSlots[i].visibility = View.GONE
            }
        }
    }
}
