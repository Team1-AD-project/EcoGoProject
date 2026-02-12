package com.ecogo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecogo.data.*
import com.ecogo.utils.CarbonCalculator
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Navigation ViewModel
 * Manages navigation-related state and data
 */
class NavigationViewModel : ViewModel() {
    
    // Navigation state
    private val _navigationState = MutableLiveData(NavigationState.IDLE)
    val navigationState: LiveData<NavigationState> = _navigationState

    // Current route
    private val _currentRoute = MutableLiveData<NavRoute?>()
    val currentRoute: LiveData<NavRoute?> = _currentRoute

    // Route options list
    private val _routeOptions = MutableLiveData<List<NavRoute>>()
    val routeOptions: LiveData<List<NavRoute>> = _routeOptions

    // Current trip
    private val _currentTrip = MutableLiveData<Trip?>()
    val currentTrip: LiveData<Trip?> = _currentTrip

    // Trip status
    private val _tripStatus = MutableLiveData<TripStatus>()
    val tripStatus: LiveData<TripStatus> = _tripStatus

    // Trip history list
    private val _tripHistory = MutableLiveData<List<Trip>>(emptyList())
    val tripHistory: LiveData<List<Trip>> = _tripHistory

    // Selected origin and destination
    private val _selectedOrigin = MutableLiveData<NavLocation?>()
    val selectedOrigin: LiveData<NavLocation?> = _selectedOrigin

    private val _selectedDestination = MutableLiveData<NavLocation?>()
    val selectedDestination: LiveData<NavLocation?> = _selectedDestination

    // Selected transport mode
    private val _selectedMode = MutableLiveData(TransportMode.WALK)
    val selectedMode: LiveData<TransportMode> = _selectedMode

    // Real-time carbon emission savings
    private val _realTimeCarbonSaved = MutableLiveData(0.0)
    val realTimeCarbonSaved: LiveData<Double> = _realTimeCarbonSaved

    // Real-time points
    private val _realTimePoints = MutableLiveData(0)
    val realTimePoints: LiveData<Int> = _realTimePoints
    
    /**
     * Set origin
     */
    fun setOrigin(location: NavLocation) {
        _selectedOrigin.value = location
        // Auto-calculate routes if both origin and destination are selected
        if (_selectedDestination.value != null) {
            calculateRoutes()
        }
    }

    /**
     * Set destination
     */
    fun setDestination(location: NavLocation) {
        _selectedDestination.value = location
        // Auto-calculate routes if both origin and destination are selected
        if (_selectedOrigin.value != null) {
            calculateRoutes()
        }
    }

    /**
     * Set transport mode
     */
    fun setTransportMode(mode: TransportMode) {
        _selectedMode.value = mode
        // Recalculate routes
        if (_selectedOrigin.value != null && _selectedDestination.value != null) {
            calculateRoutes()
        }
    }

    /**
     * Calculate routes (multiple options)
     */
    fun calculateRoutes() {
        val origin = _selectedOrigin.value ?: return
        val destination = _selectedDestination.value ?: return
        val mode = _selectedMode.value ?: TransportMode.WALK
        
        _navigationState.value = NavigationState.PLANNING
        
        viewModelScope.launch {
            try {
                // TODO: Call real route calculation API
                // Currently using mock data
                val routes = generateMockRoutes(origin, destination, mode)
                _routeOptions.value = routes

                // Set recommended route as current route
                routes.firstOrNull { it.isRecommended }?.let {
                    _currentRoute.value = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _navigationState.value = NavigationState.IDLE
            }
        }
    }
    
    /**
     * Select route
     */
    fun selectRoute(route: NavRoute) {
        _currentRoute.value = route
    }
    
    /**
     * Start navigation
     */
    fun startNavigation() {
        val route = _currentRoute.value ?: return
        
        val trip = Trip(
            id = UUID.randomUUID().toString(),
            route = route,
            startTime = System.currentTimeMillis(),
            endTime = null,
            status = TripStatus.ACTIVE,
            actualDistance = 0.0,
            actualCarbonSaved = 0.0,
            pointsEarned = 0
        )
        
        _currentTrip.value = trip
        _tripStatus.value = TripStatus.ACTIVE
        _navigationState.value = NavigationState.NAVIGATING
        _realTimeCarbonSaved.value = 0.0
        _realTimePoints.value = 0
    }
    
    /**
     * Pause navigation
     */
    fun pauseNavigation() {
        _currentTrip.value?.let { trip ->
            _currentTrip.value = trip.copy(status = TripStatus.PAUSED)
            _tripStatus.value = TripStatus.PAUSED
        }
    }
    
    /**
     * Resume navigation
     */
    fun resumeNavigation() {
        _currentTrip.value?.let { trip ->
            _currentTrip.value = trip.copy(status = TripStatus.ACTIVE)
            _tripStatus.value = TripStatus.ACTIVE
        }
    }
    
    /**
     * End navigation
     */
    fun endNavigation() {
        _currentTrip.value?.let { trip ->
            val completedTrip = trip.copy(
                endTime = System.currentTimeMillis(),
                status = TripStatus.COMPLETED,
                actualCarbonSaved = _realTimeCarbonSaved.value ?: trip.route.carbonSaved,
                pointsEarned = _realTimePoints.value ?: trip.route.points
            )
            
            _currentTrip.value = completedTrip
            _tripStatus.value = TripStatus.COMPLETED
            _navigationState.value = NavigationState.COMPLETED
            
            // Save to history
            saveTripToHistory(completedTrip)
        }
    }
    
    /**
     * Cancel navigation
     */
    fun cancelNavigation() {
        _currentTrip.value?.let { trip ->
            _currentTrip.value = trip.copy(
                status = TripStatus.CANCELLED,
                endTime = System.currentTimeMillis()
            )
            _tripStatus.value = TripStatus.CANCELLED
        }
        
        resetNavigation()
    }
    
    /**
     * Reset navigation state
     */
    fun resetNavigation() {
        _navigationState.value = NavigationState.IDLE
        _currentRoute.value = null
        _currentTrip.value = null
        _selectedOrigin.value = null
        _selectedDestination.value = null
        _realTimeCarbonSaved.value = 0.0
        _realTimePoints.value = 0
    }
    
    /**
     * Update real-time carbon emission savings
     */
    fun updateRealTimeCarbonSaved(distance: Double) {
        val mode = _currentRoute.value?.mode ?: TransportMode.WALK
        val carbonSaved = CarbonCalculator.calculateSavings(mode, distance / 1000.0)
        _realTimeCarbonSaved.value = carbonSaved
        _realTimePoints.value = CarbonCalculator.calculatePoints(carbonSaved)
    }
    
    /**
     * Save trip to history
     */
    private fun saveTripToHistory(trip: Trip) {
        val currentHistory = _tripHistory.value?.toMutableList() ?: mutableListOf()
        currentHistory.add(0, trip) // Add to the beginning of the list
        _tripHistory.value = currentHistory

        // TODO: Persist to database or SharedPreferences
    }

    /**
     * Load trip history
     */
    fun loadTripHistory() {
        viewModelScope.launch {
            // TODO: Load from database or SharedPreferences
            // Currently using an empty list
        }
    }

    /**
     * Generate mock route data
     */
    private fun generateMockRoutes(
        origin: NavLocation,
        destination: NavLocation,
        preferredMode: TransportMode
    ): List<NavRoute> {
        val routes = mutableListOf<NavRoute>()
        
        // Calculate straight-line distance (rough estimate)
        val distance = calculateMockDistance(
            origin.latitude, origin.longitude,
            destination.latitude, destination.longitude
        )
        
        // Generate walking route
        routes.add(generateRoute(origin, destination, TransportMode.WALK, distance, "🌿 Eco-friendly"))

        // Generate cycling route
        routes.add(generateRoute(origin, destination, TransportMode.CYCLE, distance, "⚡ Fastest"))

        // Generate bus route
        routes.add(generateRoute(origin, destination, TransportMode.BUS, distance * 1.2, "⚖️ Balanced"))
        
        return routes
    }
    
    private fun generateRoute(
        origin: NavLocation,
        destination: NavLocation,
        mode: TransportMode,
        distance: Double,
        badge: String
    ): NavRoute {
        val distanceKm = distance / 1000.0
        val duration = calculateDuration(mode, distanceKm)
        val carbonEmission = CarbonCalculator.calculateEmission(mode, distanceKm)
        val carbonSaved = CarbonCalculator.calculateSavings(mode, distanceKm)
        val points = CarbonCalculator.calculatePoints(carbonSaved)
        
        return NavRoute(
            id = UUID.randomUUID().toString(),
            origin = origin,
            destination = destination,
            mode = mode,
            distance = distanceKm,
            duration = duration,
            carbonEmission = carbonEmission,
            carbonSaved = carbonSaved,
            points = points,
            steps = emptyList(),
            polyline = "",
            isRecommended = mode == TransportMode.WALK || badge.contains("Eco"),
            badge = badge
        )
    }
    
    private fun calculateMockDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        // Simplified distance calculation (meters)
        val latDiff = Math.abs(lat2 - lat1) * 111000
        val lonDiff = Math.abs(lon2 - lon1) * 111000 * Math.cos(Math.toRadians(lat1))
        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff)
    }
    
    private fun calculateDuration(mode: TransportMode, distanceKm: Double): Int {
        // Estimate time based on transport mode (minutes)
        val speed = when (mode) {
            TransportMode.WALK -> 5.0    // 5 km/h
            TransportMode.CYCLE -> 15.0  // 15 km/h
            TransportMode.BUS -> 20.0    // 20 km/h
            TransportMode.MIXED -> 12.0  // 12 km/h
        }
        return ((distanceKm / speed) * 60).toInt()
    }
}
