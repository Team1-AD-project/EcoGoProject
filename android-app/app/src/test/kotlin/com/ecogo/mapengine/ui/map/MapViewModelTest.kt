package com.ecogo.mapengine.ui.map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.ecogo.mapengine.data.model.*
import com.ecogo.mapengine.data.repository.IMapRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepository: IMapRepository
    private lateinit var viewModel: MapViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mock()
        viewModel = MapViewModel(repositoryOverride = mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== Location ====================

    @Test
    fun `updateCurrentLocation sets currentLocation`() {
        val latLng = LatLng(1.2966, 103.7764)
        viewModel.updateCurrentLocation(latLng)
        assertEquals(latLng, viewModel.currentLocation.value)
    }

    @Test
    fun `updateCurrentLocation sets origin when origin is null`() {
        val latLng = LatLng(1.2966, 103.7764)
        assertNull(viewModel.origin.value)
        viewModel.updateCurrentLocation(latLng)
        assertEquals(latLng, viewModel.origin.value)
    }

    @Test
    fun `setDestination updates destination`() {
        val dest = LatLng(1.3000, 103.7800)
        viewModel.setDestination(dest)
        assertEquals(dest, viewModel.destination.value)
    }

    @Test
    fun `swapOriginDestination swaps values`() {
        val origin = LatLng(1.0, 2.0)
        val dest = LatLng(3.0, 4.0)
        viewModel.setOrigin(origin)
        viewModel.setDestination(dest)

        viewModel.swapOriginDestination()

        assertEquals(dest, viewModel.origin.value)
        assertEquals(origin, viewModel.destination.value)
    }

    @Test
    fun `clearDestination nullifies destination and route`() {
        viewModel.setDestination(LatLng(1.0, 2.0))
        viewModel.clearDestination()
        assertNull(viewModel.destination.value)
    }

    // ==================== Trip Tracking ====================

    @Test
    fun `startTracking without location sets error`() {
        // No currentLocation set
        viewModel.startTracking()
        assertNotNull(viewModel.errorMessage.value)
    }

    @Test
    fun `startTracking success transitions to Tracking state`() = runTest {
        val latLng = LatLng(1.2966, 103.7764)
        viewModel.updateCurrentLocation(latLng)

        whenever(mockRepository.startTripTracking(any(), any(), anyOrNull())).thenReturn(
            Result.success(TripTrackData(trip_id = "trip-123", start_time = "2026-02-11T10:00:00"))
        )

        viewModel.startTracking()
        advanceUntilIdle()

        val state = viewModel.tripState.value
        assertTrue(state is TripState.Tracking)
        assertEquals("trip-123", (state as TripState.Tracking).tripId)
        assertEquals("trip-123", viewModel.currentTripId.value)
    }

    @Test
    fun `startTracking failure reverts to Idle state`() = runTest {
        val latLng = LatLng(1.2966, 103.7764)
        viewModel.updateCurrentLocation(latLng)

        whenever(mockRepository.startTripTracking(any(), any(), anyOrNull())).thenReturn(
            Result.failure(RuntimeException("Server error"))
        )

        viewModel.startTracking()
        advanceUntilIdle()

        assertEquals(TripState.Idle, viewModel.tripState.value)
        assertNotNull(viewModel.errorMessage.value)
    }

    @Test
    fun `stopTracking without tripId sets error`() {
        viewModel.stopTracking()
        assertNotNull(viewModel.errorMessage.value)
    }

    @Test
    fun `cancelTracking without tripId does nothing`() {
        // Should not crash
        viewModel.cancelTracking("user cancelled")
        // tripState should remain Idle
        assertEquals(TripState.Idle, viewModel.tripState.value)
    }

    @Test
    fun `cancelTracking success transitions to Idle`() = runTest {
        // First set up a tracking state
        val latLng = LatLng(1.2966, 103.7764)
        viewModel.updateCurrentLocation(latLng)

        whenever(mockRepository.startTripTracking(any(), any(), anyOrNull())).thenReturn(
            Result.success(TripTrackData(trip_id = "trip-456", start_time = "2026-02-11T10:00:00"))
        )
        viewModel.startTracking()
        advanceUntilIdle()

        // Now cancel
        whenever(mockRepository.cancelTripTracking(eq("trip-456"), any(), anyOrNull())).thenReturn(
            Result.success(TripCancelData(trip_id = "trip-456", status = "canceled"))
        )
        viewModel.cancelTracking("test")
        advanceUntilIdle()

        assertEquals(TripState.Idle, viewModel.tripState.value)
        assertNull(viewModel.currentTripId.value)
    }

    // ==================== Route Fetching ====================

    @Test
    fun `fetchLowCarbonRoute without origin sets error`() {
        // No origin or currentLocation set, but destination is set
        viewModel.setDestination(LatLng(1.3, 103.8))
        viewModel.fetchLowCarbonRoute()
        assertNotNull(viewModel.errorMessage.value)
    }

    @Test
    fun `fetchLowCarbonRoute without destination sets error`() {
        viewModel.updateCurrentLocation(LatLng(1.2966, 103.7764))
        // No destination set
        viewModel.fetchLowCarbonRoute()
        assertNotNull(viewModel.errorMessage.value)
    }

    @Test
    fun `fetchLowCarbonRoute success sets recommendedRoute`() = runTest {
        viewModel.updateCurrentLocation(LatLng(1.2966, 103.7764))
        viewModel.setDestination(LatLng(1.3, 103.8))

        val routeData = RouteRecommendData(
            route_type = "low_carbon",
            total_distance = 2.5,
            estimated_duration = 15,
            carbon_saved = 0.3,
            route_points = listOf(GeoPoint(103.7764, 1.2966), GeoPoint(103.8, 1.3))
        )
        whenever(mockRepository.getLowestCarbonRoute(any(), any(), any())).thenReturn(
            Result.success(routeData)
        )

        viewModel.fetchLowCarbonRoute()
        advanceUntilIdle()

        assertNotNull(viewModel.recommendedRoute.value)
        assertEquals(2, viewModel.routePoints.value?.size)
    }

    // ==================== Utility ====================

    @Test
    fun `clearError clears error message`() {
        viewModel.startTracking() // triggers error (no location)
        assertNotNull(viewModel.errorMessage.value)
        viewModel.clearError()
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `clearSuccessMessage clears success`() {
        viewModel.clearSuccessMessage()
        assertNull(viewModel.successMessage.value)
    }

    @Test
    fun `initial tripState is Idle`() {
        assertEquals(TripState.Idle, viewModel.tripState.value)
    }
}
