package com.dicoding.gunungkerinci.Homepage

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dicoding.gunungkerinci.Homepage.Wisata.DeskWisataActivity
import com.dicoding.gunungkerinci.R
import com.google.android.material.imageview.ShapeableImageView

class WisataAdapter (private val items: List<WisataItem>) :
    RecyclerView.Adapter<WisataAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val foto: ShapeableImageView = view.findViewById(R.id.fotoWisata)
        val judul: TextView = view.findViewById(R.id.JudulWisata)
        val deskripsi: TextView = view.findViewById(R.id.DeskSingkat)
        val lihatDetail: TextView = view.findViewById(R.id.SeeDetail)
        //val btnRute: Button = view.findViewById(R.id.buttonRute)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_wisata, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .into(holder.foto)

        holder.judul.text = item.title
        holder.deskripsi.text = item.shortDesc

        // Klik lihat detail
        holder.lihatDetail.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DeskWisataActivity::class.java)

            intent.putExtra("id_destinasi", item.id)

            context.startActivity(intent)
        }

        holder.foto.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DeskWisataActivity::class.java)

            intent.putExtra("id_destinasi", item.id)

            context.startActivity(intent)
        }

        /*
        // Klik button rute (sementara hanya toast / atau nanti bikin page rute)
        holder.btnRute.setOnClickListener {
            // TODO: Tambah activity rute jika ada
        }*/
    }

    override fun getItemCount(): Int = items.size
}