package com.dicoding.gunungkerinci.Homepage.Wisata

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.R
import com.google.android.material.imageview.ShapeableImageView

class DeskFotoAdapter (private val images: List<Int>) :
    RecyclerView.Adapter<DeskFotoAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ShapeableImageView = view.findViewById(R.id.fotoWisata)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_desk_wisata, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.image.setImageResource(images[position])
    }

    override fun getItemCount(): Int = images.size
}