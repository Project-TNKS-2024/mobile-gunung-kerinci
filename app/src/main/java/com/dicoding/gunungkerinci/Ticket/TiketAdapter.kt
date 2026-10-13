package com.dicoding.gunungkerinci.Ticket

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.databinding.ItemTiketBinding
import com.dicoding.gunungkerinci.model.BookingDetailData
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class TiketAdapter(
    private val listTiket: List<BookingDetailData>,
    private val onItemClick: (BookingDetailData) -> Unit
) : RecyclerView.Adapter<TiketAdapter.TiketViewHolder>() {

    inner class TiketViewHolder(
        private val binding: ItemTiketBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: BookingDetailData) {

            // Status Pembayaran
            val statusPembayaran = data.pembayaran
                ?.lastOrNull()
                ?.status
                ?.lowercase()

            binding.textStatusTiket.text = when (statusPembayaran) {
                "pending" -> "Menunggu Pembayaran"
                "failed" -> "Dibatalkan"
                "success" -> getStatusTiket(data)
                else -> "-"
            }

            // Tanggal booking
            binding.tanggalBooking.text =
                formatTanggal(data.tanggal_masuk)

            // Gate masuk & keluar
            binding.tanggalMasuk.text =
                formatTanggal(data.tanggal_masuk)

            binding.tanggalKeluar.text =
                formatTanggal(data.tanggal_keluar)

            // Jumlah pendaki
            val jumlahPendaki = data.pendakis.size

            binding.jumlahPendaki.text =
                jumlahPendaki.toString()

            // Total pembayaran
            binding.Total.text =
                formatRupiah(data.total_pembayaran)

            binding.root.setOnClickListener {
                onItemClick(data)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TiketViewHolder {

        val binding = ItemTiketBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return TiketViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: TiketViewHolder,
        position: Int
    ) {
        holder.bind(listTiket[position])
    }

    override fun getItemCount(): Int {
        return listTiket.size
    }

    private fun getStatusTiket(data: BookingDetailData): String {

        val tanggalMasuk = data.tanggal_masuk ?: return "Disetujui"
        val tanggalKeluar = data.tanggal_keluar ?: return "Disetujui"

        val hariIni = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(java.util.Date())

        return when {
            hariIni < tanggalMasuk -> {
                "Disetujui"
            }

            hariIni <= tanggalKeluar -> {
                "Aktif"
            }

            else -> {
                "Selesai"
            }
        }
    }

    private fun formatTanggal(tanggal: String?): String {

        if (tanggal.isNullOrBlank()) return "-"

        return try {
            val inputFormat =
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val outputFormat =
                SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))

            val date = inputFormat.parse(tanggal)

            outputFormat.format(date!!)
        } catch (e: Exception) {
            tanggal
        }
    }

    private fun formatRupiah(nominal: Int): String {

        return NumberFormat
            .getNumberInstance(Locale("id", "ID"))
            .format(nominal)
    }
}