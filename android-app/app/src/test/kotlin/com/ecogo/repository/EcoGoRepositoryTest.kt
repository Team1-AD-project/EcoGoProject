package com.ecogo.repository

import com.ecogo.api.ApiResponse
import com.ecogo.api.ApiService
import com.ecogo.data.Activity
import com.ecogo.data.Challenge
import com.ecogo.data.Weather
import com.ecogo.data.Voucher
import com.ecogo.data.UserChallengeProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class EcoGoRepositoryTest {

    companion object {
        private const val MSG_OK = "OK"
        private const val CODE_SUCCESS = 200
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockApi: ApiService
    private lateinit var repository: EcoGoRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockApi = mock()
        repository = EcoGoRepository(api = mockApi)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== getAllActivities ====================

    @Test
    fun `getAllActivities success returns activity list`() = runTest {
        val activities = listOf(
            Activity(id = "1", title = "Tree Planting"),
            Activity(id = "2", title = "Beach Cleanup")
        )
        whenever(mockApi.getAllActivities()).thenReturn(
            ApiResponse(code = CODE_SUCCESS, message = MSG_OK, data = activities)
        )

        val result = repository.getAllActivities()

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("Tree Planting", result.getOrNull()?.first()?.title)
    }

    @Test
    fun `getAllActivities failure returns error`() = runTest {
        whenever(mockApi.getAllActivities()).thenReturn(
            ApiResponse(code = 500, message = "Server Error", data = null)
        )

        val result = repository.getAllActivities()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Server Error") == true)
    }

    @Test
    fun `getAllActivities exception returns failure`() = runTest {
        whenever(mockApi.getAllActivities()).thenThrow(RuntimeException("Network error"))

        val result = repository.getAllActivities()

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    // ==================== getChallenges ====================

    @Test
    fun `getChallenges success returns challenge list`() = runTest {
        val challenges = listOf(
            Challenge(id = "c1", title = "Walk 10km", description = "Walk", type = "GREEN_TRIPS_DISTANCE", target = 10.0, reward = 100)
        )
        whenever(mockApi.getAllChallenges()).thenReturn(
            ApiResponse(code = CODE_SUCCESS, message = MSG_OK, data = challenges)
        )

        val result = repository.getChallenges()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    // ==================== getWeather ====================

    @Test
    fun `getWeather success returns weather data`() = runTest {
        val weather = Weather(
            temperature = 30.0,
            description = "Sunny",
            icon = "sun",
            humidity = "80%",
            airQuality = 42,
            aqiLevel = "Good",
            recommendation = "Great day for cycling!"
        )
        whenever(mockApi.getWeather()).thenReturn(
            ApiResponse(code = CODE_SUCCESS, message = MSG_OK, data = weather)
        )

        val result = repository.getWeather()

        assertTrue(result.isSuccess)
        assertEquals(30.0, result.getOrNull()?.temperature ?: 0.0, 0.01)
        assertEquals("Sunny", result.getOrNull()?.description)
    }

    // ==================== joinActivity ====================

    @Test
    fun `joinActivity success returns updated activity`() = runTest {
        val activity = Activity(id = "a1", title = "Cycling Event", participantIds = listOf("user-1"))
        whenever(mockApi.joinActivity("a1", "user-1")).thenReturn(
            ApiResponse(code = CODE_SUCCESS, message = "Joined", data = activity)
        )

        val result = repository.joinActivity("a1", "user-1")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.participantIds?.contains("user-1") == true)
    }

    // ==================== acceptChallenge ====================

    @Test
    fun `acceptChallenge success returns progress`() = runTest {
        val progress = UserChallengeProgress(
            id = "p1", challengeId = "c1", userId = "u1",
            status = "IN_PROGRESS", current = 0.0, target = 10.0,
            progressPercent = 0.0, joinedAt = "2026-02-11"
        )
        whenever(mockApi.joinChallenge("c1", "u1")).thenReturn(
            ApiResponse(code = CODE_SUCCESS, message = MSG_OK, data = progress)
        )

        val result = repository.acceptChallenge("c1", "u1")

        assertTrue(result.isSuccess)
        assertEquals("IN_PROGRESS", result.getOrNull()?.status)
    }

    // ==================== getJoinedActivitiesCount ====================

    @Test
    fun `getJoinedActivitiesCount filters by userId`() = runTest {
        val activities = listOf(
            Activity(id = "1", title = "A", participantIds = listOf("user-1", "user-2")),
            Activity(id = "2", title = "B", participantIds = listOf("user-2")),
            Activity(id = "3", title = "C", participantIds = listOf("user-1"))
        )
        whenever(mockApi.getAllActivities()).thenReturn(
            ApiResponse(code = CODE_SUCCESS, message = MSG_OK, data = activities)
        )

        val result = repository.getJoinedActivitiesCount("user-1")

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
    }
}
