package com.ecogo.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.ecogo.data.FacultyCarbonData

class CommunityAdapter(private val faculties: List<FacultyCarbonData>) :
    RecyclerView.Adapter<CommunityAdapter.CommunityViewHolder>() {

    private val maxCarbon = faculties.maxOfOrNull { it.totalCarbon } ?: 1.0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_community, parent, false)
        return CommunityViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommunityViewHolder, position: Int) {
        holder.bind(faculties[position], position + 1, maxCarbon)
    }

    override fun getItemCount() = faculties.size

    class CommunityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rank: TextView = itemView.findViewById(R.id.text_rank)
        private val name: TextView = itemView.findViewById(R.id.text_name)
        private val points: TextView = itemView.findViewById(R.id.text_points)
        private val progress: View = itemView.findViewById(R.id.view_progress)

        fun bind(faculty: FacultyCarbonData, position: Int, maxCarbon: Double) {
            rank.text = "#$position"
            name.text = faculty.faculty
            points.text = "%.2f".format(faculty.totalCarbon)

            val rankColor = if (position <= 3) {
                ContextCompat.getColor(itemView.context, R.color.primary)
            } else {
                ContextCompat.getColor(itemView.context, R.color.text_secondary)
            }
            rank.setTextColor(rankColor)

            // Progress bar proportional to max carbon
            val progressPercentage = if (maxCarbon > 0) {
                (faculty.totalCarbon / maxCarbon).coerceIn(0.0, 1.0).toFloat()
            } else 0f

            progress.post {
                val parentWidth = (progress.parent as? View)?.width ?: itemView.width
                val params = progress.layoutParams
                params.width = (parentWidth * progressPercentage * 0.5).toInt()
                progress.layoutParams = params
            }
        }
    }
}
