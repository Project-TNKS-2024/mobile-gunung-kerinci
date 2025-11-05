package com.dicoding.gunungkerinci.Homepage.Sop

import android.app.Activity
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.R

class SopAdapter (private val items: List<SopItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TITLE = 1
        private const val TYPE_SUBTITLE = 2
        private const val TYPE_TEXT = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is SopItem.Header -> TYPE_HEADER
            is SopItem.Title -> TYPE_TITLE
            is SopItem.Subtitle -> TYPE_SUBTITLE
            is SopItem.Text -> TYPE_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_sop_header, parent, false)
                HeaderViewHolder(view)
            }

            TYPE_TITLE -> {
                val view = inflater.inflate(R.layout.item_sop_title, parent, false)
                TitleViewHolder(view)
            }

            TYPE_SUBTITLE -> {
                val view = inflater.inflate(R.layout.item_sop_subtitle, parent, false)
                SubtitleViewHolder(view)
            }

            TYPE_TEXT -> {
                val view = inflater.inflate(R.layout.item_sop_text, parent, false)
                TextViewHolder(view)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SopItem.Header -> (holder as HeaderViewHolder).bind()
            is SopItem.Title -> (holder as TitleViewHolder).bind(item)
            is SopItem.Subtitle -> (holder as SubtitleViewHolder).bind(item)
            is SopItem.Text -> (holder as TextViewHolder).bind((item))
        }
    }

    // ViewHolders
    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind() {
            val backBtn = itemView.findViewById<ImageButton>(R.id.buttonBack)
            backBtn.setOnClickListener {
                (itemView.context as? Activity)?.finish()
            }
        }
    }

    class TitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: SopItem.Title) {
            val tv = itemView.findViewById<TextView>(R.id.tvTitle)
            tv.text = item.text
            //itemView.findViewById<TextView>(R.id.tvTitle).text = item.text
        }
    }

    class SubtitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: SopItem.Subtitle) {
            val tv = itemView.findViewById<TextView>(R.id.tvSubtitle)
            tv.text = item.text
            //itemView.findViewById<TextView>(R.id.tvSubtitle).text = item.text
        }
    }

    class TextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: SopItem.Text) {
            val tv = itemView.findViewById<TextView>(R.id.tvText)
            tv.text = item.text
            //itemView.findViewById<TextView>(R.id.tvText).text =
            //    Html.fromHtml(item.text, Html.FROM_HTML_MODE_LEGACY)
        }
    }
}