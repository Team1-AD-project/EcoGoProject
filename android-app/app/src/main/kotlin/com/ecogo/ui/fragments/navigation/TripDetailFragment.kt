package com.ecogo.ui.fragments.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecogo.databinding.FragmentTripDetailBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Displays confirmed booking details passed from the chatbot booking card.
 * A single card showing from, to, departure time, passengers, booking ID, and status.
 */
class TripDetailFragment : Fragment() {

    private var _binding: FragmentTripDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fromName = arguments?.getString("fromName") ?: "--"
        val toName = arguments?.getString("toName") ?: "--"
        val departAt = arguments?.getString("departAt")
        val passengers = arguments?.getInt("passengers", 0) ?: 0
        val bookingId = arguments?.getString("bookingId") ?: "--"
        val status = arguments?.getString("status") ?: "confirmed"

        binding.textFrom.text = fromName
        binding.textTo.text = toName
        binding.textDepartAt.text = formatDepartAt(departAt)
        binding.textPassengers.text = if (passengers > 0) "$passengers" else "--"
        binding.textBookingId.text = bookingId
        binding.textStatus.text = status.replaceFirstChar { it.uppercase() }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    /**
     * Format an ISO-ish departAt string into a human-readable form.
     * e.g. "2026-02-10T11:30:00" -> "Feb 10, 2026  11:30"
     */
    private fun formatDepartAt(raw: String?): String {
        if (raw.isNullOrBlank()) return "--"
        return try {
            val normalized = raw.replace("T", " ").replace("  ", " ")
            val patterns = listOf(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm"
            )
            var parsed: LocalDateTime? = null
            for (p in patterns) {
                try {
                    parsed = LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern(p))
                    break
                } catch (_: Exception) { }
            }
            if (parsed != null) {
                val out = DateTimeFormatter.ofPattern("MMM d, yyyy  HH:mm", Locale.ENGLISH)
                parsed.format(out)
            } else {
                raw
            }
        } catch (_: Exception) {
            raw
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
