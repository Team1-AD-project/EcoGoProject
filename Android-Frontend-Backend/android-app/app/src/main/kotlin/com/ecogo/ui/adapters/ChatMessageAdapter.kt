package com.ecogo.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ChatMessageAdapter(
    private val messages: MutableList<ChatMessage>,
    private val onSuggestionClick: ((String) -> Unit)? = null,
    private val onBookingCardClick: ((BookingCardData) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_USER = 1
        const val VIEW_TYPE_AI = 2
        const val VIEW_TYPE_SUGGESTIONS = 3
        const val VIEW_TYPE_BOOKING_CARD = 4
        const val VIEW_TYPE_TYPING = 5
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.isTyping -> VIEW_TYPE_TYPING
            message.bookingCard != null -> VIEW_TYPE_BOOKING_CARD
            !message.suggestions.isNullOrEmpty() && message.text.isEmpty() -> VIEW_TYPE_SUGGESTIONS
            message.isUser -> VIEW_TYPE_USER
            else -> VIEW_TYPE_AI
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_user, parent, false)
                UserMessageViewHolder(view)
            }
            VIEW_TYPE_SUGGESTIONS -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_suggestions, parent, false)
                SuggestionsViewHolder(view)
            }
            VIEW_TYPE_BOOKING_CARD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_booking_card, parent, false)
                BookingCardViewHolder(view)
            }
            VIEW_TYPE_TYPING -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_ai, parent, false)
                TypingViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_ai, parent, false)
                AiMessageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is AiMessageViewHolder -> holder.bind(message)
            is SuggestionsViewHolder -> holder.bind(message, onSuggestionClick)
            is BookingCardViewHolder -> holder.bind(message, onBookingCardClick)
            is TypingViewHolder -> holder.bind()
        }
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: ChatMessage) {
        // If this is a combined message (text + suggestions), split into two items
        if (message.text.isNotEmpty() && !message.suggestions.isNullOrEmpty() && !message.isUser) {
            // First add the text message
            messages.add(ChatMessage(text = message.text, isUser = false))
            notifyItemInserted(messages.size - 1)
            // Then add suggestions as separate row
            messages.add(ChatMessage(text = "", isUser = false, suggestions = message.suggestions))
            notifyItemInserted(messages.size - 1)
        } else {
            messages.add(message)
            notifyItemInserted(messages.size - 1)
        }
    }

    fun removeTypingIndicator() {
        val index = messages.indexOfLast { it.isTyping }
        if (index >= 0) {
            messages.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text: TextView = itemView.findViewById(R.id.text_message)

        fun bind(message: ChatMessage) {
            text.text = message.text
        }
    }

    class AiMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text: TextView = itemView.findViewById(R.id.text_message)

        fun bind(message: ChatMessage) {
            text.text = message.text
        }
    }

    class TypingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text: TextView = itemView.findViewById(R.id.text_message)

        fun bind() {
            text.text = "LiNUS is typing..."
            text.alpha = 0.6f
        }
    }

    class SuggestionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chipGroup: ChipGroup = itemView.findViewById(R.id.chipGroupSuggestions)

        fun bind(message: ChatMessage, onSuggestionClick: ((String) -> Unit)?) {
            chipGroup.removeAllViews()
            message.suggestions?.forEach { suggestion ->
                val chip = Chip(itemView.context).apply {
                    text = suggestion
                    isClickable = true
                    isCheckable = false
                    setChipBackgroundColorResource(R.color.surface)
                    setTextColor(ContextCompat.getColor(itemView.context, R.color.primary))
                    chipStrokeWidth = 2f
                    setChipStrokeColorResource(R.color.primary)
                    chipCornerRadius = 20f * itemView.context.resources.displayMetrics.density
                    chipMinHeight = 36f * itemView.context.resources.displayMetrics.density
                    setOnClickListener {
                        onSuggestionClick?.invoke(suggestion)
                    }
                }
                chipGroup.addView(chip)
            }
        }
    }

    class BookingCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.card_booking)
        private val textRoute: TextView = itemView.findViewById(R.id.text_route)
        private val textDepartAt: TextView = itemView.findViewById(R.id.text_depart_at)
        private val textPassengers: TextView = itemView.findViewById(R.id.text_passengers)
        private val textStatus: TextView = itemView.findViewById(R.id.text_status)

        fun bind(message: ChatMessage, onBookingCardClick: ((BookingCardData) -> Unit)?) {
            val data = message.bookingCard ?: return
            textRoute.text = "${data.fromName} \u2192 ${data.toName}"
            textDepartAt.text = data.departAt ?: "Not set"
            textPassengers.text = "${data.passengers} passenger(s)"
            textStatus.text = data.status.replaceFirstChar { it.uppercase() }
            card.setOnClickListener {
                onBookingCardClick?.invoke(data)
            }
        }
    }

    data class ChatMessage(
        val text: String,
        val isUser: Boolean,
        val suggestions: List<String>? = null,
        val bookingCard: BookingCardData? = null,
        val isTyping: Boolean = false
    )

    data class BookingCardData(
        val bookingId: String,
        val tripId: String? = null,
        val fromName: String,
        val toName: String,
        val departAt: String?,
        val passengers: Int,
        val status: String
    )
}
