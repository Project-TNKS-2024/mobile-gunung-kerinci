package com.dicoding.gunungkerinci.Homepage.Wisata

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.R
import com.google.android.material.imageview.ShapeableImageView

class WisataPageAdapter (private val items: List<WisaataaItem>) :
    RecyclerView.Adapter<WisataPageAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val foto: ShapeableImageView = view.findViewById(R.id.fotoWisata)
        val judul: TextView = view.findViewById(R.id.JudulWisata)
        val deskripsi: TextView = view.findViewById(R.id.DeskSingkat)
        val seeDetail: TextView = view.findViewById(R.id.SeeDetail)
        val buttonRute: Button = view.findViewById(R.id.buttonRute)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wisata_page, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.foto.setImageResource(item.imageRes)
        holder.judul.text = item.title
        holder.deskripsi.text = item.shortDesc

        holder.seeDetail.setOnClickListener {
            val ctx = holder.itemView.context
            val intent = Intent(ctx, DeskWisataActivity::class.java)

            intent.putExtra("judul", item.title)
            intent.putExtra("foto", item.imageRes)
            intent.putExtra("deskripsi", item.longDesc)

            ctx.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size
}
