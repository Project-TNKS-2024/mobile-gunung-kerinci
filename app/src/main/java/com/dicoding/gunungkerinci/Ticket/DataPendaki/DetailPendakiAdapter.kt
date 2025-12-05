package com.dicoding.gunungkerinci.Ticket.DataPendaki

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.databinding.ItemDetailDataPendakiBinding

class DetailPendakiAdapter(
    private val listPendaki: List<Pendaki>
) : RecyclerView.Adapter<DetailPendakiAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDetailDataPendakiBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDetailDataPendakiBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = listPendaki.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = listPendaki[position]

        holder.binding.apply {
            // Tampilkan nomor pendaki
            numberPendaki.text = (position + 1).toString()

            // Ketua / Anggota
            ketPendaki.text = p.status

            // Isi setiap data sesuai XML
            namaPendaki.text = p.nama
            kewarganegaraan.text = p.kewarganegaraan
            noIdentitas.text = p.noIdentitas
            jenisKelamin.text = p.jenisKelamin
            tanggalLahir.text = p.tanggalLahir
            alamat.text = p.alamat
            noTelepon.text = p.noTelepon
            noTeleponDarurat.text = p.noDarurat
        }
    }
}