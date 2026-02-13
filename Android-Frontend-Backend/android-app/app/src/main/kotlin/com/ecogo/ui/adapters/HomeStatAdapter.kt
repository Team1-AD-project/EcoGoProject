package com.ecogo.ui.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R

data class HomeStat(
    val icon: String,
    val title: String,
    val value: String,
    val subtitle: String,
    val color: String
)

class HomeStatAdapter(
    private var stats: List<HomeStat>,
    private val onItemClick: (HomeStat) -> Unit = {}
) : RecyclerView.Adapter<HomeStatAdapter.HomeStatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeStatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_stat, parent, false)
        return HomeStatViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeStatViewHolder, position: Int) {
        holder.bind(stats[position], onItemClick)
    }

    override fun getItemCount(): Int = stats.size

    fun updateData(newStats: List<HomeStat>) {
        stats = newStats
        notifyDataSetChanged()
    }

    class HomeStatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: TextView = itemView.findViewById(R.id.text_stat_icon)
        private val value: TextView = itemView.findViewById(R.id.text_stat_value)
        private val title: TextView = itemView.findViewById(R.id.text_stat_title)
        private val subtitle: TextView = itemView.findViewById(R.id.text_stat_subtitle)

        fun bind(stat: HomeStat, onItemClick: (HomeStat) -> Unit) {
            icon.text = stat.icon
            value.text = stat.value
            title.text = stat.title
            subtitle.text = stat.subtitle

            // Apply color to icon background with rounded rectangle
            try {
                val color = Color.parseColor(stat.color)
                val bgDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 14f * itemView.resources.displayMetrics.density
                    setColor(Color.argb(30, Color.red(color), Color.green(color), Color.blue(color)))
                }
                icon.background = bgDrawable
                subtitle.setTextColor(color)
            } catch (e: Exception) {
                // Use default color if parsing fails
            }

            itemView.setOnClickListener {
                onItemClick(stat)
            }
        }
    }
}
