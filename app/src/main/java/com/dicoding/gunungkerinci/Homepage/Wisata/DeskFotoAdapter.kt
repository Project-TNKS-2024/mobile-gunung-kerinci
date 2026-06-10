package com.dicoding.gunungkerinci.Homepage.Wisata

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dicoding.gunungkerinci.databinding.ItemDeskWisataBinding

class DeskFotoAdapter(
    private val images: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<DeskFotoAdapter.ViewHolder>() {

    inner class ViewHolder(
        val binding: ItemDeskWisataBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding =
            ItemDeskWisataBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val imageUrl = images[position]

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .into(holder.binding.fotoWisata)

        holder.itemView.setOnClickListener {
            onClick(imageUrl)
        }
    }

    override fun getItemCount(): Int = images.size
}