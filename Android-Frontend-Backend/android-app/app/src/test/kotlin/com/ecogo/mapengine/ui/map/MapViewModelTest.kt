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

    private val testOrigin = LatLng(1.2966, 103.7764)
    private val testDestination = LatLng(1.3000, 103.8000)

    private val sampleRouteData = RouteRecommendData(
        route_type = "low_carbon",
        total_distance = 2.5,
        estimated_duration = 15,
        carbon_saved = 0.3,
        route_points = listOf(GeoPoint(103.7764, 1.2966), GeoPoint(103.8, 1.3))
    )

    private val TEST_START_TIME = "2026-02-11T10:00:00"
    private val TRIP_ID_123 = "trip-123"
    private val TRIP_ID_CALC = "trip-calc"
    private val TRIP_ID_456 = "trip-456"

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
        viewModel.updateCurrentLocation(testOrigin)
        assertEquals(testOrigin, viewModel.currentLocation.value)
    }

    @Test
    fun `updateCurrentLocation sets origin when origin is null`() {
        assertNull(viewModel.origin.value)
        viewModel.updateCurrentLocation(testOrigin)
        assertEquals(testOrigin, viewModel.origin.value)
    }

    @Test
    fun `updateCurrentLocation does not overwrite existing origin`() {
        val manualOrigin = LatLng(2.0, 104.0)
        viewModel.setOrigin(manualOrigin)
        viewModel.updateCurrentLocation(testOrigin)
        assertEquals(manualOrigin, viewModel.origin.value)
        assertEquals(testOrigin, viewModel.currentLocation.value)
    }

    @Test
    fun `setOrigin updates origin`() {
        viewModel.setOrigin(testOrigin)
        assertEquals(testOrigin, viewModel.origin.value)
    }

    @Test
    fun `setDestination updates destination`() {
        viewModel.setDestination(testDestination)
        assertEquals(testDestination, viewModel.destination.value)
    }

    @Test
    fun `swapOriginDestination swaps values`() {
        viewModel.setOrigin(testOrigin)
        viewModel.setDestination(testDestination)

        viewModel.swapOriginDestination()

        assertEquals(testDestination, viewModel.origin.value)
        assertEquals(testOrigin, viewModel.destination.value)
    }

    @Test
    fun `swapOriginDestination clears existing route`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)
        viewModel.setDestination(testDestination)

        whenever(mockRepository.getLowestCarbonRoute(any(), any(), any())).thenReturn(
            Result.success(sampleRouteData)
        )
        viewModel.fetchLowCarbonRoute()
        advanceUntilIdle()
        assertNotNull(viewModel.recommendedRoute.value)

        viewModel.swapOriginDestination()
        assertNull(viewModel.recommendedRoute.value)
        assertTrue(viewModel.routePoints.value?.isEmpty() ?: true)
    }

    @Test
    fun `swapOriginDestination with null origin only swaps destination`() {
        viewModel.setDestination(testDestination)
        viewModel.swapOriginDestination()
        assertEquals(testDestination, viewModel.origin.value)
    }

    @Test
    fun `clearDestination nullifies destination and route`() {
        viewModel.setDestination(testDestination)
        viewModel.clearDestination()
        assertNull(viewModel.destination.value)
        assertNull(viewModel.recommendedRoute.value)
    }

    // ==================== Trip Tracking ====================

    @Test
    fun `startTracking without location sets error`() {
        viewModel.startTracking()
        assertNotNull(viewModel.errorMessage.value)
    }

    @Test
    fun `startTracking success transitions to Tracking state`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)

        whenever(mockRepository.startTripTracking(any(), any(), anyOrNull())).thenReturn(
            Result.success(TripTrackData(trip_id = TRIP_ID_123, start_time = TEST_START_TIME))
        )

        viewModel.startTracking()
        advanceUntilIdle()

        val state = viewModel.tripState.value
        assertTrue(state is TripState.Tracking)
        assertEquals(TRIP_ID_123, (state as TripState.Tracking).tripId)
        assertEquals(TRIP_ID_123, viewModel.currentTripId.value)
        assertNotNull(viewModel.successMessage.value)
    }

    @Test
    fun `startTracking failure reverts to Idle state`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)

        whenever(mockRepository.startTripTracking(any(), any(), anyOrNull())).thenReturn(
            Result.failure(RuntimeException("Server error"))
        )

        viewModel.startTracking()
        advanceUntilIdle()

        assertEquals(TripState.Idle, viewModel.tripState.value)
        assertNotNull(viewModel.errorMessage.value)
    }

    @Test
    fun `startTracking sets loading state`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)

        whenever(mockRepository.startTripTracking(any(), any(), anyOrNull())).thenReturn(
            Result.success(TripTrackData(trip_id = TRIP_ID_123, start_time = TEST_START_TIME))
        )

        viewModel.startTracking()
        advanceUntilIdle()

        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `stopTracking without tripId sets Completed state`() {
        // stopTracking() simply transitions to Completed and clears tripId
        viewModel.stopTracking()
        assertEquals(TripState.Completed, viewModel.tripState.value)
        assertNull(viewModel.currentTripId.value)
    }

    @Test
    fun `stopTracking success completes trip and calculates carbon`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)
        whenever(mockRepository.startTripTracking(any(), any(), anyOrNull())).thenReturn(
            Result.success(TripTrackData(trip_id = TRIP_ID_123, start_time = TEST_START_TIME))
        )
        viewModel.startTracking()
        advanceUntilIdle()

        whenever(mockRepository.saveTrip(
            eq(TRIP_ID_123), any(), any(), anyOrNull(), any(), any()
        )).thenReturn(
            Result.success(TripSaveData(trip_id = TRIP_ID_123, status = "saved"))
        )

        whenever(mockRepository.calculateCarbon(eq(TRIP_ID_123), any())).thenReturn(
            Result.success(CarbonCalculateData(
                trip_id = TRIP_ID_123,
                total_carbon_emission = 0.5,
                carbon_saved = 0.3,
                green_points = 10
            ))
        )

        viewModel.stopTracking()
        advanceUntilIdle()

        assertEquals(TripState.Completed, viewModel.tripState.value)
        assertNull(viewModel.currentTripId.value)
        assertNotNull(viewModel.successMessage.value)
    }

    @Test
    fun `stopTracking after startTracking sets Completed state`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)
        whenever(mockRepository.startTripTracking(any(), any(), anyOrNull())).thenReturn(
            Result.success(TripTrackData(trip_id = "trip-789", start_time = TEST_START_TIME))
        )
        viewModel.startTracking()
        advanceUntilIdle()

        // stopTracking() is a simple UI state transition; actual saving is handled by MapActivity
        viewModel.stopTracking()

        assertEquals(TripState.Completed, viewModel.tripState.value)
        assertNull(viewModel.currentTripId.value)
    }

    @Test
    fun `stopTracking with carbon calculation failure still completes trip`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)
        whenever(mockRepository.startTripTracking(any(), any(), anyOrNull())).thenReturn(
            Result.success(TripTrackData(trip_id = TRIP_ID_CALC, start_time = TEST_START_TIME))
        )
        viewModel.startTracking()
        advanceUntilIdle()

        whenever(mockRepository.saveTrip(
            eq(TRIP_ID_CALC), any(), any(), anyOrNull(), any(), any()
        )).thenReturn(
            Result.success(TripSaveData(trip_id = TRIP_ID_CALC, status = "saved"))
        )

        whenever(mockRepository.calculateCarbon(eq(TRIP_ID_CALC), any())).thenReturn(
            Result.failure(RuntimeException("Carbon calc failed"))
        )

        viewModel.stopTracking()
        advanceUntilIdle()

        assertEquals(TripState.Completed, viewModel.tripState.value)
    }

    @Test
    fun `cancelTracking without tripId does nothing`() {
        viewModel.cancelTracking("user cancelled")
        assertEquals(TripState.Idle, viewModel.tripState.value)
    }

    @Test
    fun `cancelTracking success transitions to Idle`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)
        whenever(mockRepository.startTripTracking(any(), any(), anyOrNull())).thenReturn(
            Result.success(TripTrackData(trip_id = TRIP_ID_456, start_time = TEST_START_TIME))
        )
        viewModel.startTracking()
        advanceUntilIdle()

        whenever(mockRepository.cancelTripTracking(eq(TRIP_ID_456), any(), anyOrNull())).thenReturn(
            Result.success(TripCancelData(trip_id = TRIP_ID_456, status = "canceled"))
        )
        viewModel.cancelTracking("test")
        advanceUntilIdle()

        assertEquals(TripState.Idle, viewModel.tripState.value)
        assertNull(viewModel.currentTripId.value)
        assertNotNull(viewModel.successMessage.value)
    }

    @Test
    fun `cancelTracking failure sets error`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)
        whenever(mockRepository.startTripTracking(any(), any(), anyOrNull())).thenReturn(
            Result.success(TripTrackData(trip_id = "trip-err", start_time = TEST_START_TIME))
        )
        viewModel.startTracking()
        advanceUntilIdle()
        viewModel.clearError()

        whenever(mockRepository.cancelTripTracking(eq("trip-err"), any(), anyOrNull())).thenReturn(
            Result.failure(RuntimeException("Cancel failed"))
        )
        viewModel.cancelTracking()
        advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)
    }

    // ==================== Route Fetching - Low Carbon ====================

    @Test
    fun `fetchLowCarbonRoute without origin sets error`() {
        viewModel.setDestination(testDestination)
        viewModel.fetchLowCarbonRoute()
        assertNotNull(viewModel.errorMessage.value)
    }

    @Test
    fun `fetchLowCarbonRoute without destination sets error`() {
        viewModel.updateCurrentLocation(testOrigin)
        viewModel.fetchLowCarbonRoute()
        assertNotNull(viewModel.errorMessage.value)
    }

    @Test
    fun `fetchLowCarbonRoute success sets recommendedRoute`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)
        viewModel.setDestination(testDestination)

        whenever(mockRepository.getLowestCarbonRoute(any(), any(), any())).thenReturn(
            Result.success(sampleRouteData)
        )

        viewModel.fetchLowCarbonRoute()
        advanceUntilIdle()

        assertNotNull(viewModel.recommendedRoute.value)
        assertEquals(2, viewModel.routePoints.value?.size)
        assertNotNull(viewModel.successMessage.value)
    }

    @Test
    fun `fetchLowCarbonRoute failure sets error`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)
        viewModel.setDestination(testDestination)

        whenever(mockRepository.getLowestCarbonRoute(any(), any(), any())).thenReturn(
            Result.failure(RuntimeException("Route not found"))
        )

        viewModel.fetchLowCarbonRoute()
        advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)
    }

    @Test
    fun `fetchLowCarbonRoute uses green_route as fallback`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)
        viewModel.setDestination(testDestination)

        val routeWithGreenRoute = RouteRecommendData(
            route_type = "low_carbon",
            total_distance = 2.0,
            estimated_duration = 10,
            carbon_saved = 0.2,
            route_points = null,
            green_route = listOf(GeoPoint(103.77, 1.29), GeoPoint(103.80, 1.30))
        )

        whenever(mockRepository.getLowestCarbonRoute(any(), any(), any())).thenReturn(
            Result.success(routeWithGreenRoute)
        )

        viewModel.fetchLowCarbonRoute()
        advanceUntilIdle()

        assertEquals(2, viewModel.routePoints.value?.size)
    }

    // ==================== Route Fetching - Balanced ====================

    @Test
    fun `fetchBalancedRoute without origin sets error`() {
        viewModel.setDestination(testDestination)
        viewModel.fetchBalancedRoute()
        assertEquals("无法获取起点位置", viewModel.errorMessage.value)
    }

    @Test
    fun `fetchBalancedRoute without destination sets error`() {
        viewModel.updateCurrentLocation(testOrigin)
        viewModel.fetchBalancedRoute()
        assertEquals("请先设置目的地", viewModel.errorMessage.value)
    }

    @Test
    fun `fetchBalancedRoute success sets recommendedRoute`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)
        viewModel.setDestination(testDestination)

        val balancedRoute = sampleRouteData.copy(route_type = "balance", carbon_saved = 0.15)
        whenever(mockRepository.getBalancedRoute(any(), any(), any())).thenReturn(
            Result.success(balancedRoute)
        )

        viewModel.fetchBalancedRoute()
        advanceUntilIdle()

        assertNotNull(viewModel.recommendedRoute.value)
        assertEquals(2, viewModel.routePoints.value?.size)
        assertNotNull(viewModel.successMessage.value)
        assertTrue(viewModel.successMessage.value!!.contains("平衡路线"))
    }

    @Test
    fun `fetchBalancedRoute failure sets error`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)
        viewModel.setDestination(testDestination)

        whenever(mockRepository.getBalancedRoute(any(), any(), any())).thenReturn(
            Result.failure(RuntimeException("Service unavailable"))
        )

        viewModel.fetchBalancedRoute()
        advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)
    }

    @Test
    fun `fetchBalancedRoute uses green_route as fallback`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)
        viewModel.setDestination(testDestination)

        val routeWithGreenRoute = RouteRecommendData(
            route_type = "balance",
            total_distance = 3.0,
            estimated_duration = 20,
            carbon_saved = 0.1,
            route_points = null,
            green_route = listOf(GeoPoint(103.77, 1.29))
        )
        whenever(mockRepository.getBalancedRoute(any(), any(), any())).thenReturn(
            Result.success(routeWithGreenRoute)
        )

        viewModel.fetchBalancedRoute()
        advanceUntilIdle()

        assertEquals(1, viewModel.routePoints.value?.size)
    }

    // ==================== Route Fetching - By Transport Mode ====================

    @Test
    fun `fetchRouteByMode without origin sets error`() {
        viewModel.setDestination(testDestination)
        viewModel.fetchRouteByMode(TransportMode.DRIVING)
        assertEquals("无法获取起点位置", viewModel.errorMessage.value)
    }

    @Test
    fun `fetchRouteByMode without destination sets error`() {
        viewModel.updateCurrentLocation(testOrigin)
        viewModel.fetchRouteByMode(TransportMode.WALKING)
        assertEquals("请先设置目的地", viewModel.errorMessage.value)
    }

    @Test
    fun `fetchRouteByMode sets selectedTransportMode`() {
        viewModel.updateCurrentLocation(testOrigin)
        viewModel.setDestination(testDestination)
        viewModel.fetchRouteByMode(TransportMode.CYCLING)
        assertEquals(TransportMode.CYCLING, viewModel.selectedTransportMode.value)
    }

    @Test
    fun `fetchRouteByMode DRIVING success`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)
        viewModel.setDestination(testDestination)

        val drivingRoute = RouteRecommendData(
            route_type = "car",
            total_distance = 5.0,
            estimated_duration = 12,
            carbon_saved = 0.0,
            route_points = listOf(GeoPoint(103.77, 1.29), GeoPoint(103.80, 1.30))
        )
        whenever(mockRepository.getRouteByTransportMode(any(), any(), any(), eq(TransportMode.DRIVING))).thenReturn(
            Result.success(drivingRoute)
        )

        viewModel.fetchRouteByMode(TransportMode.DRIVING)
        advanceUntilIdle()

        assertNotNull(viewModel.recommendedRoute.value)
        assertEquals(2, viewModel.routePoints.value?.size)
        assertTrue(viewModel.successMessage.value!!.contains("驾车"))
    }

    @Test
    fun `fetchRouteByMode WALKING success`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)
        viewModel.setDestination(testDestination)

        val walkingRoute = RouteRecommendData(
            route_type = "walk",
            total_distance = 1.2,
            estimated_duration = 15,
            carbon_saved = 0.25,
            route_points = listOf(GeoPoint(103.77, 1.29))
        )
        whenever(mockRepository.getRouteByTransportMode(any(), any(), any(), eq(TransportMode.WALKING))).thenReturn(
            Result.success(walkingRoute)
        )

        viewModel.fetchRouteByMode(TransportMode.WALKING)
        advanceUntilIdle()

        assertTrue(viewModel.successMessage.value!!.contains("步行"))
    }

    @Test
    fun `fetchRouteByMode BUS success`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)
        viewModel.setDestination(testDestination)

        val busRoute = RouteRecommendData(
            route_type = "bus",
            total_distance = 8.0,
            estimated_duration = 25,
            carbon_saved = 0.9,
            route_points = listOf(GeoPoint(103.77, 1.29), GeoPoint(103.78, 1.30), GeoPoint(103.80, 1.31))
        )
        whenever(mockRepository.getRouteByTransportMode(any(), any(), any(), eq(TransportMode.BUS))).thenReturn(
            Result.success(busRoute)
        )

        viewModel.fetchRouteByMode(TransportMode.BUS)
        advanceUntilIdle()

        assertEquals(3, viewModel.routePoints.value?.size)
        assertTrue(viewModel.successMessage.value!!.contains("公交"))
    }

    @Test
    fun `fetchRouteByMode SUBWAY success`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)
        viewModel.setDestination(testDestination)

        val subwayRoute = RouteRecommendData(
            route_type = "subway",
            total_distance = 10.0,
            estimated_duration = 20,
            carbon_saved = 1.5,
            route_points = listOf(GeoPoint(103.77, 1.29), GeoPoint(103.85, 1.35))
        )
        whenever(mockRepository.getRouteByTransportMode(any(), any(), any(), eq(TransportMode.SUBWAY))).thenReturn(
            Result.success(subwayRoute)
        )

        viewModel.fetchRouteByMode(TransportMode.SUBWAY)
        advanceUntilIdle()

        assertTrue(viewModel.successMessage.value!!.contains("地铁"))
    }

    @Test
    fun `fetchRouteByMode CYCLING success`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)
        viewModel.setDestination(testDestination)

        val cyclingRoute = RouteRecommendData(
            route_type = "bike",
            total_distance = 3.5,
            estimated_duration = 12,
            carbon_saved = 0.7,
            route_points = listOf(GeoPoint(103.77, 1.29))
        )
        whenever(mockRepository.getRouteByTransportMode(any(), any(), any(), eq(TransportMode.CYCLING))).thenReturn(
            Result.success(cyclingRoute)
        )

        viewModel.fetchRouteByMode(TransportMode.CYCLING)
        advanceUntilIdle()

        assertTrue(viewModel.successMessage.value!!.contains("骑行"))
    }

    @Test
    fun `fetchRouteByMode failure sets error`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)
        viewModel.setDestination(testDestination)

        whenever(mockRepository.getRouteByTransportMode(any(), any(), any(), any())).thenReturn(
            Result.failure(RuntimeException("No route available"))
        )

        viewModel.fetchRouteByMode(TransportMode.DRIVING)
        advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)
    }

    @Test
    fun `fetchRouteByMode failure with null message uses default`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)
        viewModel.setDestination(testDestination)

        whenever(mockRepository.getRouteByTransportMode(any(), any(), any(), any())).thenReturn(
            Result.failure(RuntimeException())
        )

        viewModel.fetchRouteByMode(TransportMode.DRIVING)
        advanceUntilIdle()

        assertEquals("路线获取失败", viewModel.errorMessage.value)
    }

    @Test
    fun `fetchRouteByMode with null route_points uses empty list`() = runTest {
        viewModel.updateCurrentLocation(testOrigin)
        viewModel.setDestination(testDestination)

        val routeNoPoints = RouteRecommendData(
            route_type = "car",
            total_distance = 5.0,
            estimated_duration = 12,
            carbon_saved = 0.0,
            route_points = null
        )
        whenever(mockRepository.getRouteByTransportMode(any(), any(), any(), any())).thenReturn(
            Result.success(routeNoPoints)
        )

        viewModel.fetchRouteByMode(TransportMode.DRIVING)
        advanceUntilIdle()

        assertTrue(viewModel.routePoints.value?.isEmpty() ?: true)
    }

    // ==================== Utility Methods ====================

    @Test
    fun `updateRoutePointsForSelectedAlternative updates route points`() {
        val points = listOf(LatLng(1.0, 103.0), LatLng(1.1, 103.1), LatLng(1.2, 103.2))
        viewModel.updateRoutePointsForSelectedAlternative(points)
        assertEquals(3, viewModel.routePoints.value?.size)
        assertEquals(points, viewModel.routePoints.value)
    }

    @Test
    fun `updateRoutePointsForSelectedAlternative with empty list`() {
        viewModel.updateRoutePointsForSelectedAlternative(emptyList())
        assertTrue(viewModel.routePoints.value?.isEmpty() ?: true)
    }

    @Test
    fun `clearError clears error message`() {
        viewModel.startTracking()
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

    @Test
    fun `initial selectedTransportMode is null`() {
        assertNull(viewModel.selectedTransportMode.value)
    }

    @Test
    fun `initial isLoading is false`() {
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `initial currentTripId is null`() {
        assertNull(viewModel.currentTripId.value)
    }

    @Test
    fun `initial recommendedRoute is null`() {
        assertNull(viewModel.recommendedRoute.value)
    }

    @Test
    fun `initial carbonResult is null`() {
        assertNull(viewModel.carbonResult.value)
    }

    @Test
    fun `initial routePoints is null`() {
        assertNull(viewModel.routePoints.value)
    }

    // ==================== TripState ====================

    @Test
    fun `TripState Tracking holds tripId`() {
        val state = TripState.Tracking("my-trip-id")
        assertEquals("my-trip-id", state.tripId)
    }

    @Test
    fun `TripState Idle is singleton`() {
        assertSame(TripState.Idle, TripState.Idle)
    }

    @Test
    fun `TripState Starting is singleton`() {
        assertSame(TripState.Starting, TripState.Starting)
    }

    @Test
    fun `TripState Stopping is singleton`() {
        assertSame(TripState.Stopping, TripState.Stopping)
    }

    @Test
    fun `TripState Completed is singleton`() {
        assertSame(TripState.Completed, TripState.Completed)
    }

    @Test
    fun `TripState Tracking equality`() {
        assertEquals(TripState.Tracking("a"), TripState.Tracking("a"))
        assertNotEquals(TripState.Tracking("a"), TripState.Tracking("b"))
    }
}
