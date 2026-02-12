package com.ecogo.mapengine.ml

import android.util.Log
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Google Snap to Roads API 接口
 * API 文档: https://developers.google.com/maps/documentation/roads/overview
 */
interface SnapToRoadsApi {
    
    /**
     * Call Google Snap to Roads API
     * @param path Coordinate sequence, format: "lat1,lng1|lat2,lng2|..."
     * @param key Corresponding API Key
     * @param interpolate Whether to interpolate (optional)
     * @return SnapToRoadsResponse containing snapped points and road information
     */
    @GET("v1/snapToRoads")
    suspend fun snapToRoads(
        @Query("path") path: String,
        @Query("key") key: String,
        @Query("interpolate") interpolate: Boolean = true
    ): SnapToRoadsResponse
}

/**
 * Snap to Roads API 响应数据结构
 */
data class SnapToRoadsResponse(
    @SerializedName("snappedPoints")
    val snappedPoints: List<SnappedPoint>?,
    
    @SerializedName("warningMessage")
    val warningMessage: String?,
    
    @SerializedName("error")
    val error: String?
)

/**
 * Single snapped point
 */
data class SnappedPoint(
    @SerializedName("location")
    val location: LocationPoint?,
    
    @SerializedName("originalIndex")
    val originalIndex: Int = 0,
    
    @SerializedName("placeId")
    val placeId: String?
)

/**
 * Location information
 */
data class LocationPoint(
    @SerializedName("latitude")
    val latitude: Double = 0.0,
    
    @SerializedName("longitude")
    val longitude: Double = 0.0
)

/**
 * Snap to Roads HTTP 客户端
 */
object SnapToRoadsHttpClient {
    
    private const val BASE_URL = "https://roads.googleapis.com/"
    private const val TAG = "SnapToRoadsHttpClient"
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val api = retrofit.create(SnapToRoadsApi::class.java)
    
    /**
     * Call Snap to Roads API
     * @param gpsPoints GPS coordinate point list
     * @param apiKey Google Maps API Key
     * @return Snapped points with road information
     */
    suspend fun snapToRoads(
        gpsPoints: List<com.google.android.gms.maps.model.LatLng>,
        apiKey: String
    ): List<SnappedPoint>? = withContext(Dispatchers.IO) {
        try {
            if (gpsPoints.isEmpty()) {
                Log.w(TAG, "GPS points list is empty")
                return@withContext null
            }
            
            if (gpsPoints.size < 2) {
                Log.w(TAG, "GPS points count < 2, skipping API call")
                return@withContext null
            }
            
            // Build path string: lat1,lng1|lat2,lng2|...
            val pathString = gpsPoints.joinToString("|") { point ->
                "${point.latitude},${point.longitude}"
            }
            
            Log.d(TAG, "Calling Snap to Roads API with ${gpsPoints.size} points")
            Log.d(TAG, "Path: ${pathString.take(100)}...")  // Log only first 100 characters
            
            // Call API
            val response = api.snapToRoads(
                path = pathString,
                key = apiKey,
                interpolate = true
            )
            
            // Check for errors
            if (!response.error.isNullOrEmpty()) {
                Log.e(TAG, "API Error: ${response.error}")
                return@withContext null
            }
            
            if (response.warningMessage != null) {
                Log.w(TAG, "API Warning: ${response.warningMessage}")
            }
            
            // Return snapped points
            val snappedCount = response.snappedPoints?.size ?: 0
            Log.d(TAG, "Snap to Roads API returned $snappedCount snapped points")
            
            return@withContext response.snappedPoints
            
        } catch (e: Exception) {
            Log.e(TAG, "Snap to Roads API call failed: ${e.message}", e)
            return@withContext null
        }
    }
}
