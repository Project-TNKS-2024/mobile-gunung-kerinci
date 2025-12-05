package com.dicoding.gunungkerinci.Ticket.SOP

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.databinding.ItemSopTiketFooterBinding
import com.dicoding.gunungkerinci.databinding.ItemSopTiketHeaderBinding
import com.dicoding.gunungkerinci.databinding.ItemSopTiketSubtitleBinding
import com.dicoding.gunungkerinci.databinding.ItemSopTiketTextBinding
import com.dicoding.gunungkerinci.databinding.ItemSopTitleBinding

class SOPAdapter (
    private val items: List<SOPItem>,
    private val listener: SOPListener
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemViewType(position: Int): Int = when (items[position]) {
            is SOPItem.Header -> 0
            is SOPItem.Title -> 1
            is SOPItem.Subtitle -> 2
            is SOPItem.Content -> 3
            is SOPItem.Footer -> 4
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {

                0 -> HeaderHolder(
                    ItemSopTiketHeaderBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                )

                1 -> TitleHolder(
                    ItemSopTitleBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                )

                2 -> SubtitleHolder(
                    ItemSopTiketSubtitleBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                )

                3 -> ContentHolder(
                    ItemSopTiketTextBinding.inflate(
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

                is SOPItem.Header -> {
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

                is SOPItem.Title -> (holder as TitleHolder).binding.tvTitle.text = item.text

                is SOPItem.Subtitle -> (holder as SubtitleHolder).binding.tvSubtitle.text = item.text

                is SOPItem.Content -> (holder as ContentHolder).binding.tvText.text = item.text

                is SOPItem.Footer -> {
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

        class TitleHolder(val binding: ItemSopTitleBinding) :
            RecyclerView.ViewHolder(binding.root)

        class SubtitleHolder(val binding: ItemSopTiketSubtitleBinding) :
            RecyclerView.ViewHolder(binding.root)

        class ContentHolder(val binding: ItemSopTiketTextBinding) :
            RecyclerView.ViewHolder(binding.root)

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