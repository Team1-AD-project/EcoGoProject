package com.ecogo.mapengine.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecogo.mapengine.data.model.*
import com.ecogo.mapengine.data.repository.IMapRepository
import com.ecogo.mapengine.data.repository.MapRepository
import com.ecogo.mapengine.data.repository.MockMapRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

/**
 * Map ViewModel
 * Manages UI state and business logic for the map page
 *
 * @param useMockData whether to use mock data (default true for testing)
 */
class MapViewModel(
    useMockData: Boolean = false,  // Switch: true = Mock data, false = Real API
    repositoryOverride: IMapRepository? = null  // For unit test injection
) : ViewModel() {

    // Select Repository based on switch (can be overridden via repositoryOverride for testing)
    private val repository: IMapRepository = repositoryOverride ?: if (useMockData) {
        MockMapRepository()
    } else {
        MapRepository()
    }

    // Simulated user ID (should be obtained from login state in production)
    private val userId: String = "test-user-001"

    companion object {
        private const val ERR_NO_ORIGIN = "Unable to get origin location"
        private const val ERR_NO_DESTINATION = "Please set a destination first"
    }

    // ========================================
    // UI State
    // ========================================

    // Current location
    private val _currentLocation = MutableLiveData<LatLng>()
    val currentLocation: LiveData<LatLng> = _currentLocation

    // Origin (for route planning)
    private val _origin = MutableLiveData<LatLng?>()
    val origin: LiveData<LatLng?> = _origin

    // Destination
    private val _destination = MutableLiveData<LatLng?>()
    val destination: LiveData<LatLng?> = _destination

    // Trip state
    private val _tripState = MutableLiveData<TripState>(TripState.Idle)
    val tripState: LiveData<TripState> = _tripState

    // Current trip ID
    private val _currentTripId = MutableLiveData<String?>()
    val currentTripId: LiveData<String?> = _currentTripId

    // Recommended route
    private val _recommendedRoute = MutableLiveData<RouteRecommendData?>()
    val recommendedRoute: LiveData<RouteRecommendData?> = _recommendedRoute

    // Route points (for drawing Polyline)
    private val _routePoints = MutableLiveData<List<LatLng>>()
    val routePoints: LiveData<List<LatLng>> = _routePoints

    // Carbon footprint calculation result
    private val _carbonResult = MutableLiveData<CarbonCalculateData?>()
    val carbonResult: LiveData<CarbonCalculateData?> = _carbonResult

    // Error message
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Success message (for Toast)
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    // Currently selected transport mode
    private val _selectedTransportMode = MutableLiveData<TransportMode?>(null)
    val selectedTransportMode: LiveData<TransportMode?> = _selectedTransportMode

    // ========================================
    // Location Updates
    // ========================================

    /**
     * Update current location
     */
    fun updateCurrentLocation(latLng: LatLng) {
        _currentLocation.value = latLng
        // If origin hasn't been set, use current location as default
        if (_origin.value == null) {
            _origin.value = latLng
        }
    }

    /**
     * Set origin
     */
    fun setOrigin(latLng: LatLng) {
        _origin.value = latLng
    }

    /**
     * Set destination
     */
    fun setDestination(latLng: LatLng) {
        _destination.value = latLng
    }

    /**
     * Swap origin and destination
     */
    fun swapOriginDestination() {
        val tempOrigin = _origin.value
        val tempDest = _destination.value

        if (tempDest != null) {
            _origin.value = tempDest
        }
        if (tempOrigin != null) {
            _destination.value = tempOrigin
        }

        // If there's a route, need to re-fetch
        if (_recommendedRoute.value != null) {
            _recommendedRoute.value = null
            _routePoints.value = emptyList()
        }
    }

    /**
     * Clear destination
     */
    fun clearDestination() {
        _destination.value = null
        _recommendedRoute.value = null
        _routePoints.value = emptyList()
    }

    // ========================================
    // Trip Tracking
    // ========================================

    /**
     * 开始行程追踪（仅更新 UI 状态）
     * 实际的后端 API 调用由 MapActivity.startTripOnBackend() 通过 TripRepository 完成
     */
    fun startTracking() {
        _tripState.value = TripState.Tracking("pending")
    }

    /**
     * 设置后端返回的真实 tripId，更新追踪状态
     * 由 MapActivity 在 TripRepository.startTrip() 成功后调用
     */
    fun setBackendTripId(tripId: String) {
        _currentTripId.value = tripId
        _tripState.value = TripState.Tracking(tripId)
    }

    /**
     * Stop trip tracking and save
     */
    fun stopTracking() {
        // Only responsible for updating UI state; actual saving is done by MapActivity.completeTripOnBackend()
        _tripState.value = TripState.Completed
        _currentTripId.value = null
    }

    /**
     * 取消行程（仅更新 UI 状态）
     * 实际的后端取消调用由 MapActivity 通过 TripRepository 完成
     */
    fun cancelTracking(reason: String? = null) {
        _tripState.value = TripState.Idle
        _currentTripId.value = null
        _successMessage.value = "行程已取消"
    }

    // ========================================
    // Route Recommendations
    // ========================================

    /**
     * Fetch low carbon route
     */
    fun fetchLowCarbonRoute() {
        val start = _origin.value ?: _currentLocation.value ?: run {
            _errorMessage.value = ERR_NO_ORIGIN
            return
        }
        val end = _destination.value ?: run {
            _errorMessage.value = ERR_NO_DESTINATION
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.getLowestCarbonRoute(
                userId = userId,
                startPoint = GeoPoint.fromLatLng(start),
                endPoint = GeoPoint.fromLatLng(end)
            )

            result.fold(
                onSuccess = { data ->
                    _recommendedRoute.value = data
                    // Prefer new route_points, fall back to legacy green_route
                    val points = data.route_points ?: data.green_route ?: emptyList()
                    _routePoints.value = points.map { it.toLatLng() }
                    _successMessage.value = "Low carbon route found, estimated saving ${String.format("%.2f", data.carbon_saved)} kg CO₂"
                },
                onFailure = { error ->
                    _errorMessage.value = error.message
                }
            )
            _isLoading.value = false
        }
    }

    /**
     * Fetch balanced route
     */
    fun fetchBalancedRoute() {
        val start = _origin.value ?: _currentLocation.value ?: run {
            _errorMessage.value = ERR_NO_ORIGIN
            return
        }
        val end = _destination.value ?: run {
            _errorMessage.value = ERR_NO_DESTINATION
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.getBalancedRoute(
                userId = userId,
                startPoint = GeoPoint.fromLatLng(start),
                endPoint = GeoPoint.fromLatLng(end)
            )

            result.fold(
                onSuccess = { data ->
                    _recommendedRoute.value = data
                    val points = data.route_points ?: data.green_route ?: emptyList()
                    _routePoints.value = points.map { it.toLatLng() }
                    _successMessage.value = "Balanced route found, estimated saving ${String.format("%.2f", data.carbon_saved)} kg CO₂"
                },
                onFailure = { error ->
                    _errorMessage.value = error.message
                }
            )
            _isLoading.value = false
        }
    }

    /**
     * Fetch route by transport mode
     */
    fun fetchRouteByMode(mode: TransportMode) {
        // Record selected transport mode
        _selectedTransportMode.value = mode

        val start = _origin.value ?: _currentLocation.value ?: run {
            _errorMessage.value = ERR_NO_ORIGIN
            return
        }
        val end = _destination.value ?: run {
            _errorMessage.value = ERR_NO_DESTINATION
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.getRouteByTransportMode(
                userId = userId,
                startPoint = GeoPoint.fromLatLng(start),
                endPoint = GeoPoint.fromLatLng(end),
                transportMode = mode
            )

            result.fold(
                onSuccess = { data ->
                    _recommendedRoute.value = data
                    val points = data.route_points ?: emptyList()
                    _routePoints.value = points.map { it.toLatLng() }

                    val distance = String.format("%.2f", data.total_distance)
                    val duration = data.estimated_duration
                    val carbonSaved = String.format("%.2f", data.carbon_saved)
                    _successMessage.value = "${mode.displayName} route: ${distance} km, est. ${duration} min, carbon saved ${carbonSaved} kg"
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to fetch route"
                }
            )
            _isLoading.value = false
        }
    }

    // ========================================
    // Carbon Footprint Calculation
    // ========================================

    /**
     * Calculate carbon footprint
     */
    private fun calculateCarbon(tripId: String, transportModes: List<String>) {
        viewModelScope.launch {
            val result = repository.calculateCarbon(tripId, transportModes)

            result.fold(
                onSuccess = { data ->
                    _carbonResult.value = data
                },
                onFailure = { error ->
                    _errorMessage.value = error.message
                }
            )
        }
    }

    /**
     * Update route points (when user selects a different route alternative)
     * Only updates route points without triggering re-draw (drawing is handled by MapActivity)
     */
    fun updateRoutePointsForSelectedAlternative(points: List<LatLng>) {
        _routePoints.value = points
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    /**
     * Restore tracking state
     * When user returns to MapActivity, restore UI state if there's an ongoing trip
     */
    fun restoreTrackingState() {
        // Check if LocationManager is tracking
        if (com.ecogo.mapengine.service.LocationManager.isTracking.value == true) {
            // If current state is not Tracking, restore to Tracking state
            if (_tripState.value !is TripState.Tracking) {
                // Use a restored tripId (if no saved tripId, use placeholder)
                val tripId = _currentTripId.value ?: "restored-trip"
                _tripState.value = TripState.Tracking(tripId)
            }
        }
    }
}

/**
 * Trip State
 */
sealed class TripState {
    object Idle : TripState()                       // Idle
    object Starting : TripState()                   // Starting
    data class Tracking(val tripId: String) : TripState()  // Tracking
    object Stopping : TripState()                   // Stopping
    object Completed : TripState()                  // Completed
}
