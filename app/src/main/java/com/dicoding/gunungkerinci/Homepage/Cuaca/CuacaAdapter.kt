package com.dicoding.gunungkerinci.Homepage.Cuaca

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.R

class CuacaAdapter(private val items: List<CuacaItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CUACA = 1
        private const val TYPE_PENDAKI = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is CuacaItem.Header -> TYPE_HEADER
            is CuacaItem.CardCuaca -> TYPE_CUACA
            is CuacaItem.CardPendaki -> TYPE_PENDAKI
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.item_header_cuaca, parent, false))
            TYPE_CUACA -> CuacaViewHolder(inflater.inflate(R.layout.item_card_cuaca, parent, false))
            TYPE_PENDAKI -> PendakiViewHolder(inflater.inflate(R.layout.item_card_pendaki, parent, false))
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is CuacaItem.Header -> (holder as HeaderViewHolder).bind(item)
            is CuacaItem.CardCuaca -> (holder as CuacaViewHolder).bind(item)
            is CuacaItem.CardPendaki -> (holder as PendakiViewHolder).bind(item)
        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTemp = view.findViewById<TextView>(R.id.tvTemperature)
        private val tvCond = view.findViewById<TextView>(R.id.tvCondition)
        private val img = view.findViewById<ImageView>(R.id.imgWeather)

        fun bind(item: CuacaItem.Header) {
            tvTemp.text = item.temperature
            tvCond.text = item.condition
            img.setImageResource(item.iconRes)
        }
    }

    class CuacaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle = view.findViewById<TextView>(R.id.tvCardTitle)
        private val tvForecast = view.findViewById<TextView>(R.id.tvForecast)

        fun bind(item: CuacaItem.CardCuaca) {
            tvTitle.text = item.title
            tvForecast.text = item.forecast
        }
    }

    class PendakiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvJumlah = view.findViewById<TextView>(R.id.tvJumlah)
        fun bind(item: CuacaItem.CardPendaki) {
            tvJumlah.text = item.jumlah
        }
    }
}
