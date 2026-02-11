package com.ecogo.ui.fragments.navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecogo.R
import com.ecogo.api.RetrofitClient
import com.ecogo.data.LocationType
import com.ecogo.data.MockData
import com.ecogo.data.NavLocation
import com.ecogo.data.TransportMode
import com.ecogo.databinding.FragmentRoutePlannerBinding
import com.ecogo.ui.adapters.RouteOptionAdapter
import com.ecogo.viewmodel.NavigationViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Route planner Fragment.
 * Supports receiving bookingId or tripId arguments from chatbot deeplinks.
 */
class RoutePlannerFragment : Fragment() {

    private var _binding: FragmentRoutePlannerBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: NavigationViewModel
    private lateinit var routeAdapter: RouteOptionAdapter
    
    private var selectedMode = TransportMode.WALK

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoutePlannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[NavigationViewModel::class.java]
        
        setupRecyclerView()
        setupUI()
        observeViewModel()
        
        // Check if we have trip/booking data from chatbot card or deeplink
        val tripId = arguments?.getString("tripId")
        val bookingId = arguments?.getString("bookingId")
        val fromName = arguments?.getString("fromName")
        val toName = arguments?.getString("toName")
        when {
            // If fromName and toName are passed directly (from booking card), use them immediately
            !fromName.isNullOrEmpty() && !toName.isNullOrEmpty() -> {
                applyBookingCardData(fromName, toName,
                    arguments?.getString("departAt"),
                    arguments?.getInt("passengers", 0) ?: 0)
            }
            !tripId.isNullOrEmpty() -> loadTripData(tripId)
            !bookingId.isNullOrEmpty() -> loadBookingData(bookingId)
            else -> setDefaultLocations()
        }
    }
    
    private fun setupRecyclerView() {
        routeAdapter = RouteOptionAdapter { route ->
            viewModel.selectRoute(route)
            binding.btnStartNavigation.isEnabled = true
        }
        
        binding.recyclerRoutes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = routeAdapter
        }
    }
    
    private fun setupUI() {
        binding.originContainer.setOnClickListener {
            val action = RoutePlannerFragmentDirections
                .actionRoutePlannerToLocationSearch(isSelectingOrigin = true)
            findNavController().navigate(action)
        }
        
        binding.destinationContainer.setOnClickListener {
            val action = RoutePlannerFragmentDirections
                .actionRoutePlannerToLocationSearch(isSelectingOrigin = false)
            findNavController().navigate(action)
        }
        
        binding.modeWalk.setOnClickListener {
            selectMode(TransportMode.WALK)
        }
        
        binding.modeCycle.setOnClickListener {
            selectMode(TransportMode.CYCLE)
        }
        
        binding.modeBus.setOnClickListener {
            selectMode(TransportMode.BUS)
        }
        
        binding.btnStartNavigation.setOnClickListener {
            viewModel.startNavigation()
            val action = RoutePlannerFragmentDirections.actionRoutePlannerToTripStart()
            findNavController().navigate(action)
        }
    }
    
    private fun selectMode(mode: TransportMode) {
        selectedMode = mode
        viewModel.setTransportMode(mode)
        
        val primaryColor = requireContext().getColor(R.color.primary)
        val whiteColor = requireContext().getColor(android.R.color.white)
        
        binding.modeWalk.setCardBackgroundColor(
            if (mode == TransportMode.WALK) primaryColor else whiteColor
        )
        binding.modeCycle.setCardBackgroundColor(
            if (mode == TransportMode.CYCLE) primaryColor else whiteColor
        )
        binding.modeBus.setCardBackgroundColor(
            if (mode == TransportMode.BUS) primaryColor else whiteColor
        )
    }
    
    private fun observeViewModel() {
        viewModel.selectedOrigin.observe(viewLifecycleOwner) { origin ->
            origin?.let {
                binding.textOrigin.text = it.name
            }
        }
        
        viewModel.selectedDestination.observe(viewLifecycleOwner) { destination ->
            destination?.let {
                binding.textDestination.text = it.name
            }
        }
        
        viewModel.routeOptions.observe(viewLifecycleOwner) { routes ->
            routeAdapter.updateRoutes(routes)
        }
    }

    /**
     * Apply booking data passed directly from the chatbot booking card.
     * No API call needed -- the card already carries from/to/departAt/passengers.
     */
    private fun applyBookingCardData(fromName: String, toName: String, departAt: String?, passengers: Int) {
        val origin = findLocationByName(fromName)
        val destination = findLocationByName(toName)

        origin?.let { viewModel.setOrigin(it) }
        destination?.let { viewModel.setDestination(it) }

        selectMode(TransportMode.BUS)

        val infoMsg = buildString {
            append("$fromName → $toName")
            if (passengers > 0) append(" | ${passengers} pax")
            if (!departAt.isNullOrBlank()) append(" | $departAt")
        }
        view?.let { Snackbar.make(it, infoMsg, Snackbar.LENGTH_LONG).show() }
    }

    /**
     * Load trip data from the backend and pre-fill origin/destination.
     */
    private fun loadTripData(tripId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getTripDetail(tripId)
                }
                if (response.success && response.data != null) {
                    val trip = response.data

                    // Use trip's start/end location names for origin/destination
                    val originName = trip.startLocation?.placeName ?: trip.startLocation?.address
                    val destName = trip.endLocation?.placeName ?: trip.endLocation?.address

                    if (originName != null) {
                        val origin = findLocationByName(originName)
                        origin?.let { viewModel.setOrigin(it) }
                    }
                    if (destName != null) {
                        val destination = findLocationByName(destName)
                        destination?.let { viewModel.setDestination(it) }
                    }

                    // Select transport mode based on detected mode
                    when (trip.detectedMode?.lowercase()) {
                        "bus", "public_transport" -> selectMode(TransportMode.BUS)
                        "cycle", "bicycle" -> selectMode(TransportMode.CYCLE)
                        else -> selectMode(TransportMode.BUS)
                    }

                    val infoMsg = buildString {
                        append("Trip loaded: ${originName ?: "Unknown"} → ${destName ?: "Unknown"}")
                        if (trip.carbonStatus != null) append(" | ${trip.carbonStatus}")
                    }
                    view?.let { Snackbar.make(it, infoMsg, Snackbar.LENGTH_LONG).show() }
                } else {
                    Log.w("RoutePlanner", "Trip not found: $tripId")
                    setDefaultLocations()
                }
            } catch (e: Exception) {
                Log.e("RoutePlanner", "Failed to load trip $tripId", e)
                setDefaultLocations()
            }
        }
    }

    /**
     * Load booking data from the backend and pre-fill origin/destination.
     */
    private fun loadBookingData(bookingId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getBookingDetail(bookingId)
                }
                if (response.success && response.data != null) {
                    val booking = response.data

                    // Try to match booking locations to known campus locations
                    val origin = findLocationByName(booking.fromName)
                    val destination = findLocationByName(booking.toName)

                    origin?.let { viewModel.setOrigin(it) }
                    destination?.let { viewModel.setDestination(it) }

                    // Default to bus mode for bookings
                    selectMode(TransportMode.BUS)

                    // Show booking info
                    val infoMsg = buildString {
                        append("Booking loaded: ${booking.fromName} → ${booking.toName}")
                        if (booking.passengers > 0) append(" | ${booking.passengers} pax")
                        if (!booking.departAt.isNullOrBlank()) append(" | ${booking.departAt}")
                    }
                    view?.let { Snackbar.make(it, infoMsg, Snackbar.LENGTH_LONG).show() }
                } else {
                    Log.w("RoutePlanner", "Booking not found: $bookingId")
                    setDefaultLocations()
                }
            } catch (e: Exception) {
                Log.e("RoutePlanner", "Failed to load booking $bookingId", e)
                setDefaultLocations()
            }
        }
    }

    /**
     * Find a NavLocation by name from known campus locations.
     * Falls back to creating a temporary NavLocation if no exact match.
     */
    private fun findLocationByName(name: String): NavLocation? {
        // Try exact match first
        val exact = MockData.CAMPUS_LOCATIONS.find {
            it.name.equals(name, ignoreCase = true)
        }
        if (exact != null) return exact

        // Try partial match
        val partial = MockData.CAMPUS_LOCATIONS.find {
            it.name.contains(name, ignoreCase = true) || name.contains(it.name, ignoreCase = true)
        }
        if (partial != null) return partial

        // Create a temporary location with default coordinates (NUS campus center)
        return NavLocation(
            id = "chatbot_${name.hashCode()}",
            name = name,
            address = name,
            latitude = 1.2966,
            longitude = 103.7764,
            type = LocationType.OTHER,
            icon = "📍"
        )
    }
    
    private fun setDefaultLocations() {
        val origin = MockData.CAMPUS_LOCATIONS.find { it.id == "4" } // PGP
        val destination = MockData.CAMPUS_LOCATIONS.find { it.id == "1" } // COM1
        
        origin?.let { viewModel.setOrigin(it) }
        destination?.let { viewModel.setDestination(it) }
        
        selectMode(TransportMode.WALK)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
