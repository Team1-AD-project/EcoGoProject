package com.ecogo.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ecogo.data.ChatRequest
import com.ecogo.databinding.FragmentChatBinding
import com.ecogo.ui.adapters.ChatMessageAdapter
import com.ecogo.repository.EcoGoRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.ecogo.api.ApiConfig


class ChatFragment : Fragment() {
    
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ChatMessageAdapter
    private val repository = EcoGoRepository()
    
    // Track conversation ID for multi-turn chat
    private var conversationId: String? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupButtons()
        setupAnimations()
    }
    
    private fun setupRecyclerView() {
        adapter = ChatMessageAdapter(
            mutableListOf(
                ChatMessageAdapter.ChatMessage(
                    "Hello! I'm the EcoGo Smart Assistant. I can help you with:\n" +
                    "- 🌿 Green travel tips in Singapore\n" +
                    "- 🚌 Bus arrival queries\n" +
                    "- 📍 Trip booking\n" +
                    "- 💡 Green travel Q&A\n\n" +
                    "How can I help you?",
                    false
                )
            ),
            onSuggestionClick = { suggestion ->
                sendMessage(suggestion)
            },
            onBookingCardClick = { card ->
                navigateToRoutePlanner(card)
            }
        )
        
        binding.recyclerChat.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = this@ChatFragment.adapter
        }
    }
    
    private fun setupButtons() {
        binding.buttonSend.setOnClickListener {
            val message = binding.editMessage.text.toString()
            if (message.isNotEmpty()) {
                sendMessage(message)
            }
        }
    }
    
    private fun sendMessage(message: String) {
        // Add user message
        adapter.addMessage(ChatMessageAdapter.ChatMessage(message, true))
        binding.editMessage.text?.clear()
        
        // Scroll to bottom
        binding.recyclerChat.scrollToPosition(adapter.itemCount - 1)
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = repository.sendChat(
                    ChatRequest(conversationId = conversationId, message = message)
                )

                val response = result.getOrNull()
                if (response != null) {
                    // Update conversation ID for multi-turn
                    conversationId = response.conversationId ?: conversationId
                    
                    // Build display text
                    val displayText = buildDisplayText(response)
                    adapter.addMessage(ChatMessageAdapter.ChatMessage(displayText, false))
                    
                    // Handle UI actions
                    handleUiActions(response.uiActions)
                } else {
                    val err = result.exceptionOrNull()
                    if (err != null) {
                        Log.e("ECOGO_CHAT", "sendChat failed. BASE_URL=${ApiConfig.BASE_URL}", err)
                        adapter.addMessage(
                            ChatMessageAdapter.ChatMessage(
                                "(Debug) Request failed: ${err.javaClass.simpleName}: ${err.message}\nBASE_URL=${ApiConfig.BASE_URL}",
                                false
                            )
                        )
                    } else {
                        val reply = generateSmartReply(message)
                        adapter.addMessage(ChatMessageAdapter.ChatMessage(reply, false))
                    }
                }
            } catch (e: Exception) {
                Log.e("ECOGO_CHAT", "sendMessage exception. BASE_URL=${ApiConfig.BASE_URL}", e)
                adapter.addMessage(
                    ChatMessageAdapter.ChatMessage(
                        "(Debug) Exception: ${e.javaClass.simpleName}: ${e.message}\nBASE_URL=${ApiConfig.BASE_URL}",
                        false
                    )
                )
            }
            
            binding.recyclerChat.scrollToPosition(adapter.itemCount - 1)
        }
    }
    
    private fun buildDisplayText(response: com.ecogo.data.ChatResponse): String {
        val sb = StringBuilder()
        
        val mainText = response.getDisplayText()
        sb.append(mainText)
        
        val citations = response.assistant?.citations
        if (!citations.isNullOrEmpty()) {
            sb.append("\n\n📚 Sources:")
            for (citation in citations) {
                sb.append("\n• ${citation.title}")
            }
        }
        
        return sb.toString()
    }
    
    private fun handleUiActions(actions: List<com.ecogo.data.UiAction>?) {
        if (actions.isNullOrEmpty()) return
        
        for (action in actions) {
            when (action.type) {
                "DEEPLINK" -> {
                    val url = action.payload?.get("url") as? String
                    if (url != null) {
                        handleDeeplink(url)
                    }
                }
                "SUGGESTIONS" -> {
                    @Suppress("UNCHECKED_CAST")
                    val options = action.payload?.get("options") as? List<String>
                    val filtered = options?.filter { !it.contains("Book a Trip") && !it.contains("My Profile") }
                    if (!filtered.isNullOrEmpty()) {
                        adapter.addMessage(
                            ChatMessageAdapter.ChatMessage(
                                text = "",
                                isUser = false,
                                suggestions = filtered
                            )
                        )
                    }
                }
                "SHOW_FORM" -> {
                    handleShowForm(action.payload)
                }
                "SHOW_CONFIRM" -> {
                    handleShowConfirm(action.payload)
                }
                "BOOKING_CARD" -> {
                    handleBookingCard(action.payload)
                }
            }
        }
        binding.recyclerChat.scrollToPosition(adapter.itemCount - 1)
    }
    
    private fun handleDeeplink(url: String) {
        when {
            url.startsWith("ecogo://booking/") -> {
                val bookingId = url.removePrefix("ecogo://booking/")
                Log.d("ECOGO_CHAT", "Deeplink: booking/$bookingId")
                safeNavigateDelayed {
                    val bundle = Bundle().apply {
                        putString("bookingId", bookingId)
                    }
                    findNavController().navigate(
                        com.ecogo.R.id.action_chat_to_routePlanner, bundle
                    )
                }
            }
            url.startsWith("ecogo://trip/") -> {
                val tripId = url.removePrefix("ecogo://trip/")
                Log.d("ECOGO_CHAT", "Deeplink: trip/$tripId")
                safeNavigateDelayed {
                    val bundle = Bundle().apply {
                        putString("tripId", tripId)
                    }
                    findNavController().navigate(
                        com.ecogo.R.id.action_chat_to_tripDetail, bundle
                    )
                }
            }
        }
    }

    /**
     * Safely navigate with a delay, respecting Fragment lifecycle.
     * Uses viewLifecycleOwner.lifecycleScope instead of Handler.postDelayed
     * to avoid IllegalStateException when Fragment is destroyed.
     */
    private fun safeNavigateDelayed(delayMs: Long = 1000L, action: () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            delay(delayMs)
            try {
                if (isAdded && view != null) {
                    action()
                }
            } catch (e: Exception) {
                Log.w("ECOGO_CHAT", "Safe navigation failed", e)
            }
        }
    }

    private fun handleShowForm(payload: Map<String, Any>?) {
        if (payload == null) return
        val title = payload["title"] as? String ?: "Form"
        @Suppress("UNCHECKED_CAST")
        val fields = payload["fields"] as? List<Map<String, Any>> ?: return

        val context = context ?: return

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
        }

        val editTexts = mutableListOf<Pair<String, EditText>>()
        for (field in fields) {
            val key = field["key"] as? String ?: continue
            val label = field["label"] as? String ?: key
            val editText = EditText(context).apply {
                hint = label
                isSingleLine = true
            }

            container.addView(editText)
            editTexts.add(key to editText)
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(container)
            .setPositiveButton("Submit") { _, _ ->
                // Use key=value format for consistent backend parsing.
                // Backend handleBookingFlow checks text.contains("=") first.
                val parts = editTexts.mapNotNull { (key, et) ->
                    val raw = et.text?.toString()?.trim().orEmpty()
                    if (raw.isEmpty()) return@mapNotNull null
                    val value = when (key) {
                        "route" -> formatRouteForBackend(raw)
                        "departAt" -> normalizeDepartAtForBackend(raw)
                        "passengers" -> normalizePassengersForBackend(raw)
                        else -> raw
                    }
                    "$key=$value"
                }
                if (parts.isNotEmpty()) sendMessage(parts.joinToString(", "))
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun formatRouteForBackend(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return s
        val lowered = s.lowercase()
        // If user already used "from...to...", keep it.
        if (s.contains("from") && s.contains("to")) return s
        // Normalize common separators to " to "
        val normalized = s
            .replace("->", " to ")
            .replace("→", " to ")
            .replace("—", " to ")
            .replace("-", " to ")
        return if (normalized.contains(" to ")) "from $normalized" else s
    }

    private fun normalizePassengersForBackend(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return s
        // Extract first digit 1-8; then append "person(s)" to satisfy backend regex.
        val m = Regex("[1-8]").find(s)
        val n = m?.value ?: s
        return "${n} person(s)"
    }

    private fun normalizeDepartAtForBackend(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return s
        s = s.replace('/', '-')
        if (s.contains(" ") && !s.contains("T")) s = s.replace(' ', 'T')
        // If only yyyy-MM-ddTHH:mm, append seconds
        if (Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$").matches(s)) s += ":00"
        return s
    }

    private fun handleShowConfirm(payload: Map<String, Any>?) {
        if (payload == null) return
        val title = payload["title"] as? String ?: "Confirm"
        val body = payload["body"] as? String ?: ""

        val context = context ?: return
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(body)
            .setPositiveButton("Confirm") { _, _ ->
                sendMessage("Confirm")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleBookingCard(payload: Map<String, Any>?) {
        if (payload == null) return
        val bookingId = payload["bookingId"] as? String ?: return
        val fromName = payload["fromName"] as? String ?: ""
        val toName = payload["toName"] as? String ?: ""
        val departAt = payload["departAt"] as? String
        val passengers = (payload["passengers"] as? Number)?.toInt() ?: 1
        val status = payload["status"] as? String ?: "confirmed"
        val tripId = payload["tripId"] as? String

        val card = ChatMessageAdapter.BookingCardData(
            bookingId = bookingId,
            tripId = tripId,
            fromName = fromName,
            toName = toName,
            departAt = departAt,
            passengers = passengers,
            status = status
        )
        adapter.addMessage(
            ChatMessageAdapter.ChatMessage(
                text = "",
                isUser = false,
                bookingCard = card
            )
        )
    }

    private fun navigateToRoutePlanner(card: ChatMessageAdapter.BookingCardData) {
        try {
            val bundle = Bundle().apply {
                putString("bookingId", card.bookingId)
                putString("fromName", card.fromName)
                putString("toName", card.toName)
                card.departAt?.let { putString("departAt", it) }
                putInt("passengers", card.passengers)
                putString("status", card.status)
                card.tripId?.let { putString("tripId", it) }
            }
            findNavController().navigate(
                com.ecogo.R.id.action_chat_to_tripDetail, bundle
            )
        } catch (e: Exception) {
            Log.w("ECOGO_CHAT", "Navigation to tripDetail failed", e)
        }
    }
    
    private fun generateSmartReply(message: String): String {
        val lowerMessage = message.lowercase()
        
        return when {
            lowerMessage.contains("activity") || lowerMessage.contains("event") -> {
                safeNavigateDelayed(1500L) {
                    findNavController()
                        .navigate(com.ecogo.R.id.action_chat_to_activities)
                }
                "Found some campus activities, redirecting you..."
            }
            lowerMessage.contains("route") || lowerMessage.contains("navigate") ||
            lowerMessage.contains("how to get") || lowerMessage.contains("directions") -> {
                safeNavigateDelayed(1500L) {
                    findNavController()
                        .navigate(com.ecogo.R.id.action_chat_to_routePlanner)
                }
                "Planning a green travel route for you..."
            }
            lowerMessage.contains("map") || lowerMessage.contains("location") -> {
                "Map feature coming soon, stay tuned!"
            }
            lowerMessage.contains("recommend") || lowerMessage.contains("suggest") || lowerMessage.contains("advice") -> {
                "🌿 Green Travel Tips:\n" +
                "- Short distance (<2km): Walk or cycle\n" +
                "- Medium distance (2-10km): MRT or bus\n" +
                "- Long distance (>10km): MRT or carpool\n\n" +
                "For detailed routes, tell me your origin and destination."
            }
            else -> "Sorry, unable to connect to the server. Please try again later."
        }
    }

    private fun setupAnimations() {
        val slideUp = AnimationUtils.loadAnimation(requireContext(), com.ecogo.R.anim.slide_up)
        binding.recyclerChat.startAnimation(slideUp)
        binding.inputContainer.startAnimation(slideUp)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
