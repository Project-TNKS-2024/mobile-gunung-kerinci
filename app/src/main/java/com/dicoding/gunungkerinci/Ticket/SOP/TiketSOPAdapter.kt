package com.dicoding.gunungkerinci.Ticket.SOP

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.databinding.ItemSopTiketFooterBinding
import com.dicoding.gunungkerinci.databinding.ItemSopTiketHeaderBinding
import androidx.core.text.HtmlCompat
import com.dicoding.gunungkerinci.databinding.ItemSopTiketHtmlBinding

class TiketSOPAdapter (
    private val items: List<TiketSOPItem>,
    private val listener: SOPListener
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemViewType(position: Int): Int = when (items[position]) {
            is TiketSOPItem.Header -> 0
            is TiketSOPItem.Html -> 1
            is TiketSOPItem.Footer -> 2
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {

                0 -> HeaderHolder(
                    ItemSopTiketHeaderBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                )

                1 -> HtmlHolder(
                    ItemSopTiketHtmlBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                )


                else -> FooterHolder(
                    ItemSopTiketFooterBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                )
            }
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {

                is TiketSOPItem.Header -> {
                    val h = holder as HeaderHolder

                    h.binding.imgDownload.setOnClickListener {
                        listener.onDownloadClicked()
                    }

                    h.binding.textPDF.setOnClickListener {
                        listener.onDownloadClicked()
                    }

                    h.binding.buttonBack.setOnClickListener {
                        // Back ditangani Activity agar lebih aman
                        (h.itemView.context as? androidx.activity.ComponentActivity)
                            ?.onBackPressedDispatcher?.onBackPressed()
                    }
                }

                is TiketSOPItem.Html -> {
                    val h = holder as HtmlHolder

                    h.binding.tvHtml.text =
                        HtmlCompat.fromHtml(
                            item.html,
                            HtmlCompat.FROM_HTML_MODE_LEGACY)
                }

                is TiketSOPItem.Footer -> {
                    val f = holder as FooterHolder

                    // Ceklis
                    f.binding.checkBoxPersetujuan.setOnCheckedChangeListener { _, isChecked ->
                        listener.onCheckStateChanged(isChecked)
                    }

                    // Tombol Selanjutnya
                    f.binding.btnSelanjutnya.setOnClickListener {
                        listener.onNextClicked()
                    }
                }
            }

        }

        class HeaderHolder(val binding: ItemSopTiketHeaderBinding) :
            RecyclerView.ViewHolder(binding.root)

        class HtmlHolder(
            val binding: ItemSopTiketHtmlBinding
        ) : RecyclerView.ViewHolder(binding.root)

        class FooterHolder(val binding: ItemSopTiketFooterBinding) :
            RecyclerView.ViewHolder(binding.root)

    //Listener callback
    interface SOPListener {
        fun onDownloadClicked()
        fun onCheckStateChanged(checked: Boolean)
        fun onNextClicked()
        fun onBackClicked()
    }

}