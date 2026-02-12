package com.ecogo.mapengine.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ecogo.mapengine.ui.map.MapActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng

/**
 * Foreground location service
 * Used to continuously obtain user location during trip tracking
 */
class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var isTracking = false

    companion object {
        private const val TAG = "LocationTrackingService"

        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.ecogo.mapengine.action.START_TRACKING"
        const val ACTION_STOP = "com.ecogo.mapengine.action.STOP_TRACKING"

        // Location update interval
        private const val UPDATE_INTERVAL_MS = 3000L  // 3 seconds
        private const val FASTEST_INTERVAL_MS = 2000L // Fastest 2 seconds
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        setupLocationCallback()
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Create notification channel
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Trip Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows trip tracking status"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create foreground notification
     */
    private fun createNotification(distance: Float = 0f): Notification {
        val intent = Intent(this, MapActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val distanceText = if (distance > 0) {
            String.format("Traveled %.2f km", distance / 1000)
        } else {
            "Recording your green travel route"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EcoGo Trip Tracking")
            .setContentText(distanceText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Update notification
     */
    private fun updateNotification(distance: Float) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification(distance))
    }

    /**
     * Set up location callback
     */
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    Log.d(TAG, "Location update: ${location.latitude}, ${location.longitude}")

                    val latLng = LatLng(location.latitude, location.longitude)

                    // Update LocationManager (route recording)
                    LocationManager.updateLocation(location.latitude, location.longitude)

                    // Update NavigationManager (navigation route matching)
                    if (NavigationManager.isNavigating.value == true) {
                        NavigationManager.updateLocation(latLng)
                    }

                    // Update notification with distance
                    val distance = if (NavigationManager.isNavigating.value == true) {
                        NavigationManager.traveledDistance.value ?: 0f
                    } else {
                        LocationManager.totalDistance.value ?: 0f
                    }
                    if (distance > 0) {
                        updateNotification(distance)
                    }
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                Log.d(TAG, "Location availability: ${availability.isLocationAvailable}")
            }
        }
    }

    /**
     * Start tracking
     */
    @SuppressLint("MissingPermission")
    private fun startTracking() {
        if (isTracking) {
            Log.d(TAG, "Already tracking")
            return
        }

        Log.d(TAG, "Starting tracking")
        isTracking = true

        // Notify LocationManager to start tracking
        LocationManager.startTracking()

        // Start foreground service
        startForeground(NOTIFICATION_ID, createNotification())

        // Configure high-accuracy location request
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            setWaitForAccurateLocation(true)
            setMinUpdateDistanceMeters(5f)  // Minimum 5 meters movement to trigger update
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    /**
     * Stop tracking
     */
    private fun stopTracking() {
        if (!isTracking) {
            Log.d(TAG, "Not tracking")
            return
        }

        Log.d(TAG, "Stopping tracking")
        isTracking = false

        // Notify LocationManager to stop tracking
        LocationManager.stopTracking()

        // Stop location updates
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        if (isTracking) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            LocationManager.stopTracking()
        }
    }
}
