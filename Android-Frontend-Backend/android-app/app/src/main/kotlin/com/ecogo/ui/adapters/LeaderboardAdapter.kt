package com.ecogo.ui.adapters

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.ecogo.data.IndividualRanking

class LeaderboardAdapter(
    private val rankings: List<IndividualRanking>
) : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ranking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(rankings[position])
    }

    override fun getItemCount() = rankings.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textRank: TextView = itemView.findViewById(R.id.text_rank)
        private val textNickname: TextView = itemView.findViewById(R.id.text_nickname)
        private val textUserId: TextView = itemView.findViewById(R.id.text_user_id)
        private val textCarbonSaved: TextView = itemView.findViewById(R.id.text_carbon_saved)
        private val textVipBadge: TextView = itemView.findViewById(R.id.text_vip_badge)

        fun bind(ranking: IndividualRanking) {
            textRank.text = "#${ranking.rank}"
            textNickname.text = ranking.nickname.ifBlank { "N/A" }
            textUserId.text = ranking.userId
            textCarbonSaved.text = "%.2f".format(ranking.carbonSaved)
            textVipBadge.visibility = if (ranking.isVip) View.VISIBLE else View.GONE

            // Style rank badge based on position
            val (rankBgColor, rankTextColor) = when (ranking.rank) {
                1 -> Pair(0x20FFD700.toInt(), 0xFFB8860B.toInt()) // Gold
                2 -> Pair(0x20C0C0C0.toInt(), 0xFF808080.toInt()) // Silver
                3 -> Pair(0x20CD7F32.toInt(), 0xFFCD7F32.toInt()) // Bronze
                else -> Pair(0x10000000, ContextCompat.getColor(itemView.context, R.color.text_secondary))
            }
            textRank.setTextColor(rankTextColor)
            val rankBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(rankBgColor)
            }
            textRank.background = rankBg
        }
    }
}
