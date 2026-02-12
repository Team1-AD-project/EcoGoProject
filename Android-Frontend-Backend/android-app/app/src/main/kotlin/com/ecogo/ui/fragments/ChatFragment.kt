package com.ecogo.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
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

    // Prevent double-send while loading
    private var isSending = false

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
                    "Hi there! I'm LiNUS, your EcoGo campus assistant \uD83C\uDF3F\n\n" +
                            "I can help you with:\n" +
                            "\u2022 \uD83D\uDE8C Check bus arrivals\n" +
                            "\u2022 \uD83D\uDCCD Get travel recommendations\n" +
                            "\u2022 \uD83C\uDFAB Book a trip\n" +
                            "\u2022 \u2753 Answer green travel questions\n\n" +
                            "Try tapping a suggestion below, or type your question!",
                    false,
                    suggestions = listOf(
                        "\uD83D\uDE8C Bus Arrivals",
                        "\uD83D\uDCCD Travel Advice",
                        "\uD83C\uDFAB Book a Trip",
                        "\uD83D\uDCCB My Profile"
                    )
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
        // Send button click
        binding.buttonSend.setOnClickListener {
            val message = binding.editMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
            }
        }

        // Keyboard "Send" action
        binding.editMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val message = binding.editMessage.text.toString().trim()
                if (message.isNotEmpty()) {
                    sendMessage(message)
                }
                true
            } else {
                false
            }
        }
    }

    private fun sendMessage(message: String) {
        if (isSending) return
        isSending = true

        // Add user message
        adapter.addMessage(ChatMessageAdapter.ChatMessage(message, true))
        binding.editMessage.text?.clear()

        // Hide keyboard
        hideKeyboard()

        // Scroll to bottom
        scrollToBottom()

        // Show typing indicator
        val typingMessage = ChatMessageAdapter.ChatMessage(
            "\u2022\u2022\u2022",
            false,
            isTyping = true
        )
        adapter.addMessage(typingMessage)
        scrollToBottom()

        // Disable send button while loading
        binding.buttonSend.isEnabled = false
        binding.buttonSend.alpha = 0.5f

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = repository.sendChat(
                    ChatRequest(conversationId = conversationId, message = message)
                )

                // Remove typing indicator
                removeTypingIndicator()

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
                        val userFriendlyMsg = when {
                            err.message?.contains("SocketTimeout") == true ||
                            err.message?.contains("connect") == true ->
                                "Unable to reach the server. Please check your network connection and try again."
                            err.message?.contains("401") == true ||
                            err.message?.contains("Unauthorized") == true ->
                                "Session expired. Please log in again."
                            else ->
                                "Something went wrong. Please try again later."
                        }
                        adapter.addMessage(
                            ChatMessageAdapter.ChatMessage(
                                "\u26A0\uFE0F $userFriendlyMsg",
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
                removeTypingIndicator()
                val userFriendlyMsg = when {
                    e.message?.contains("SocketTimeout") == true ||
                    e.message?.contains("connect") == true ->
                        "Unable to reach the server. Please check your network connection and try again."
                    else ->
                        "Something went wrong. Please try again later."
                }
                adapter.addMessage(
                    ChatMessageAdapter.ChatMessage(
                        "\u26A0\uFE0F $userFriendlyMsg",
                        false
                    )
                )
            } finally {
                isSending = false
                if (_binding != null) {
                    binding.buttonSend.isEnabled = true
                    binding.buttonSend.alpha = 1.0f
                }
                scrollToBottom()
            }
        }
    }

    private fun removeTypingIndicator() {
        adapter.removeTypingIndicator()
    }

    private fun scrollToBottom() {
        if (_binding == null) return
        binding.recyclerChat.post {
            if (_binding != null && adapter.itemCount > 0) {
                binding.recyclerChat.smoothScrollToPosition(adapter.itemCount - 1)
            }
        }
    }

    private fun hideKeyboard() {
        if (_binding == null) return
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.editMessage.windowToken, 0)
    }

    private fun buildDisplayText(response: com.ecogo.data.ChatResponse): String {
        val sb = StringBuilder()

        val mainText = response.getDisplayText()
        sb.append(mainText)

        val citations = response.assistant?.citations
        if (!citations.isNullOrEmpty()) {
            sb.append("\n\n\uD83D\uDCDA Sources:")
            for (citation in citations) {
                sb.append("\n\u2022 ${citation.title}")
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
                    if (!options.isNullOrEmpty()) {
                        adapter.addMessage(
                            ChatMessageAdapter.ChatMessage(
                                text = "",
                                isUser = false,
                                suggestions = options
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
        scrollToBottom()
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
     */
    private fun safeNavigateDelayed(delayMs: Long = 800L, action: () -> Unit) {
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
        if (s.contains("from") && s.contains("to")) return s
        val normalized = s
            .replace("->", " to ")
            .replace("\u2192", " to ")
            .replace("\u2014", " to ")
            .replace("-", " to ")
        return if (normalized.contains(" to ")) "from $normalized" else s
    }

    private fun normalizePassengersForBackend(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return s
        val m = Regex("[1-8]").find(s)
        val n = m?.value ?: s
        return "${n} person(s)"
    }

    private fun normalizeDepartAtForBackend(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return s
        s = s.replace('/', '-')
        if (s.contains(" ") && !s.contains("T")) s = s.replace(' ', 'T')
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
                "\uD83C\uDF3F Green Travel Tips:\n" +
                        "\u2022 Short distance (<2km): Walk or cycle\n" +
                        "\u2022 Medium distance (2-10km): MRT or bus\n" +
                        "\u2022 Long distance (>10km): MRT or carpool\n\n" +
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
