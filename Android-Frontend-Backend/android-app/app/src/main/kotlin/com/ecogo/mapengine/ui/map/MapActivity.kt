package com.ecogo.mapengine.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.ecogo.R
import com.ecogo.databinding.ActivityMapBinding
import com.ecogo.mapengine.data.model.TransportMode
import com.ecogo.mapengine.service.DirectionsService
import com.ecogo.mapengine.service.LocationManager
import com.ecogo.mapengine.service.LocationTrackingService
import com.ecogo.mapengine.service.NavigationManager
import com.ecogo.mapengine.ml.HybridTransportModeDetector
import com.ecogo.mapengine.data.repository.NavigationHistoryRepository
import com.ecogo.mapengine.data.repository.TripRepository
import com.ecogo.mapengine.data.model.PolylinePoint
import com.ecogo.mapengine.data.model.TransportModeSegment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode

/**
 * Main map page
 * Implements Google Maps integration, trip tracking, route recommendation, place search
 */
class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private val viewModel: MapViewModel by viewModels()

    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var transportModeDetector: HybridTransportModeDetector

    // Map markers
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var routePolyline: Polyline? = null

    // Transit multi-segment route (different color per segment)
    private var transitSegmentPolylines: MutableList<Polyline> = mutableListOf()

    // Current transit route step data (used to maintain multi-color display in navigation mode)
    private var currentTransitSteps: List<com.ecogo.mapengine.data.model.RouteStep>? = null

    // Flag indicating whether route selection is being handled (prevents observer from re-drawing)
    private var isHandlingRouteSelection = false

    // Real-time track
    private var trackPolyline: Polyline? = null
    private var isFollowingUser = true  // Whether to follow user location
    private var isVipUserFlag = false   // Whether current user is VIP (hide ads for VIP)

    // Navigation route (traveled/remaining)
    private var traveledPolyline: Polyline? = null    // Traveled route (gray)
    private var remainingPolyline: Polyline? = null   // Remaining route (blue)
    private var isNavigationMode = false              // Whether in navigation mode

    // Route step adapter
    private val routeStepAdapter = RouteStepAdapter()

    // Route option adapter
    private val routeOptionAdapter = RouteOptionAdapter { selectedRoute ->
        onRouteSelected(selectedRoute)
    }

    // Origin and destination locations
    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null
    private var originName: String = MY_LOCATION_LABEL
    private var destinationName: String = ""

    // Flag indicating whether searching for origin or destination
    private var isSearchingOrigin = false

    // Milestone tracking (for displaying encouragement messages)
    private val milestones = listOf(1000f, 2000f, 3000f, 5000f, 10000f) // Unit: meters
    private var reachedMilestones = mutableSetOf<Float>()

    // Navigation record related
    private var navigationStartTime: Long = 0  // Navigation start time
    private var detectedTransportMode: String? = null  // AI-detected transport mode (dominant mode)
    private var backendTripId: String? = null  // Backend real tripId (returned by TripRepository.startTrip)

    // Transport mode segment records (records exactly what the UI displays, passed directly to database)
    private val modeSegments = mutableListOf<MapActivityHelper.ModeSegment>()
    private var lastMlConfidence: Float = 0f  // Most recent ML confidence

    // Trip timer
    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerStartTime = 0L
    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsed = SystemClock.elapsedRealtime() - timerStartTime
            val timeStr = MapActivityHelper.formatElapsedTime(elapsed)
            binding.tvTimer.text = getString(R.string.timer_format, timeStr)
            timerHandler.postDelayed(this, 1000)
        }
    }

    companion object {
        private const val TAG = "MapActivity"
        private const val MY_LOCATION_LABEL = "My Location"
        const val EXTRA_DEST_LAT = "extra_dest_lat"
        const val EXTRA_DEST_LNG = "extra_dest_lng"
        const val EXTRA_DEST_NAME = "extra_dest_name"
    }

    /** Read local VIP status */
    private fun isVipUser(): Boolean {
        val prefs = getSharedPreferences("EcoGoPrefs", MODE_PRIVATE)
        return prefs.getBoolean("is_vip", false)
    }

    private fun mlLabelToDictMode(label: com.ecogo.mapengine.ml.TransportModeLabel): String =
        MapActivityHelper.mlLabelToDictMode(label)

    private fun isGreenMode(dictMode: String): Boolean =
        MapActivityHelper.isGreenMode(dictMode)

    private fun getDominantMode(): com.ecogo.mapengine.ml.TransportModeLabel =
        MapActivityHelper.getDominantMode(modeSegments)

    private fun buildTransportModeSegments(totalDistanceMeters: Double): List<com.ecogo.mapengine.data.model.TransportModeSegment> =
        MapActivityHelper.buildTransportModeSegments(modeSegments, totalDistanceMeters)

    // Location permission request
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                enableMyLocation()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                enableMyLocation()
            }
            else -> {
                Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Notification permission request (Android 13+)
    private val notificationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "Notification permission is required to display tracking status", Toast.LENGTH_SHORT).show()
        }
    }

    // Places Autocomplete launcher
    private val autocompleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        handleAutocompleteResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Places SDK
        initPlaces()

        // Initialize Directions API
        DirectionsService.init(this)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize transport mode detector (hybrid: Snap to Roads preferred, local sensors as fallback)
        val apiKey = getGoogleMapsApiKey()
        transportModeDetector = HybridTransportModeDetector(
            context = this,
            googleMapsApiKey = apiKey
        )

        // Initialize map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        setupUI()
        observeViewModel()
        observeLocationManager()
        observeNavigationManager()
        observeTransportModeDetector()

        // Request notification permission (Android 13+)
        requestNotificationPermission()

        setupAdCarousel()
    }

    // ==================== Ad carousel logic ====================
    private val adHandler = Handler(Looper.getMainLooper())
    private var adRunnable: Runnable? = null
    private var currentAdIndex = 0

    private fun setupAdCarousel() {
        lifecycleScope.launch {
            binding.layoutAdCarousel.visibility = View.GONE
            try {
                com.ecogo.auth.TokenManager.init(this@MapActivity)

                if (isUserVipCached()) {
                    isVipUserFlag = true
                    Log.d(TAG, "User is VIP (cached/pref), hiding advertisement carousel")
                    return@launch
                }

                val repository = com.ecogo.EcoGoApplication.repository
                val profile = repository.getMobileUserProfile().getOrNull()
                if (profile == null) {
                    Log.d(TAG, "Profile fetch failed, defaulting to hidden ads")
                    return@launch
                }

                if (isProfileVip(profile)) {
                    isVipUserFlag = true
                    Log.d(TAG, "User is VIP/Admin (network), hiding advertisement carousel")
                    updateVipCache(profile)
                    return@launch
                }

                val ads = repository.getAdvertisements().getOrNull()?.filter {
                    it.position == "banner" && it.status == "Active"
                } ?: emptyList()

                if (ads.isNotEmpty()) {
                    // Don't show ads during active trip tracking
                    if (viewModel.tripState.value is TripState.Tracking) {
                        Log.d(TAG, "Ads loaded but trip is tracking, keeping ads hidden")
                    } else {
                        binding.layoutAdCarousel.visibility = View.VISIBLE
                    }
                    setupAdViewPager(ads)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching ads", e)
                binding.layoutAdCarousel.visibility = View.GONE
            }
        }
    }

    private fun isUserVipCached(): Boolean {
        val prefs = getSharedPreferences("EcoGoPrefs", android.content.Context.MODE_PRIVATE)
        return com.ecogo.auth.TokenManager.isVipActive() || prefs.getBoolean("is_vip", false)
    }

    private fun isProfileVip(profile: com.ecogo.api.MobileProfileResponse): Boolean {
        return (profile.vipInfo?.active == true) ||
                (profile.userInfo?.vip?.active == true) ||
                (profile.vipInfo?.plan != null) ||
                (profile.userInfo?.vip?.plan != null) ||
                (profile.userInfo?.isAdmin == true)
    }

    private fun updateVipCache(profile: com.ecogo.api.MobileProfileResponse) {
        com.ecogo.auth.TokenManager.saveToken(
            token = com.ecogo.auth.TokenManager.getToken() ?: "",
            userId = profile.userInfo.userid,
            username = profile.userInfo.nickname,
            vipActive = true
        )
    }

    private fun setupAdViewPager(ads: List<com.ecogo.data.Advertisement>) {
        val adapter = AdAdapter(ads) { ad ->
            // Navigate on ad click
            if (ad.linkUrl.isNotEmpty()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(ad.linkUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening ad link: ${ad.linkUrl}", e)
                    Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.viewPagerAd.adapter = adapter
        
        // Bind TabLayout indicator
        com.google.android.material.tabs.TabLayoutMediator(
            binding.tabLayoutAdIndicator, binding.viewPagerAd
        ) { _, _ -> }.attach()

        // Auto carousel logic
        adRunnable = Runnable {
            if (ads.size > 1) {
                currentAdIndex = (currentAdIndex + 1) % ads.size
                binding.viewPagerAd.setCurrentItem(currentAdIndex, true)
                adHandler.postDelayed(adRunnable!!, 5000) // Switch every 5 seconds
            }
        }
        
        // Register page change callback, handle conflict between manual swipe and auto carousel
        binding.viewPagerAd.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentAdIndex = position
                // Reset timer to avoid immediate auto-switch after manual swipe
                adHandler.removeCallbacks(adRunnable!!)
                adHandler.postDelayed(adRunnable!!, 5000)
            }
        })

        // Start carousel
        adHandler.postDelayed(adRunnable!!, 5000)
    }



    class AdAdapter(
        private val ads: List<com.ecogo.data.Advertisement>,
        private val onItemClick: (com.ecogo.data.Advertisement) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<AdAdapter.AdViewHolder>() {

        inner class AdViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            val imageView: android.widget.ImageView = itemView.findViewById(R.id.ivAdImage)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): AdViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_map_ad, parent, false)
            return AdViewHolder(view)
        }

        override fun onBindViewHolder(holder: AdViewHolder, position: Int) {
            val ad = ads[position]
            // Load image using Glide (assuming Glide is integrated in the project)
            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(ad.imageUrl)
                .placeholder(R.drawable.placeholder_image) // Ensure this resource exists or replace with another default image
                .centerCrop()
                .into(holder.imageView)

            holder.itemView.setOnClickListener { onItemClick(ad) }
        }

        override fun getItemCount(): Int = ads.size
    }

    /**
     * Request notification permission
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * Initialize Places SDK
     */
    private fun initPlaces() {
        try {
            if (!Places.isInitialized()) {
                // Get API Key from AndroidManifest.xml
                val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                val apiKey = appInfo.metaData?.getString("com.google.android.geo.API_KEY") ?: ""
                if (apiKey.isNotEmpty()) {
                    Places.initialize(applicationContext, apiKey)
                    Log.d(TAG, "Places SDK initialized with key: ${apiKey.take(10)}...")
                } else {
                    Log.e(TAG, "Google Maps API Key not found in manifest")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Places SDK: ${e.message}", e)
        }
    }

    /**
     * Set up UI event listeners
     */
    private fun setupUI() {
        // Initialize bottom area: show ad placeholder (now changed to carousel), hidden by default to prevent flicker
        // binding.layoutAdCarousel.visibility = View.GONE // Handled by XML
        binding.cardBottomPanel.visibility = View.GONE

        // Back button click - return to main interface
        binding.fabBack.setOnClickListener {
            finish()
        }
        binding.btnRouteInfoBack.setOnClickListener {
            finish()
        }

        // Origin input field click
        binding.etOrigin.setOnClickListener {
            isSearchingOrigin = true
            launchPlaceAutocomplete()
        }

        // Destination input field click
        binding.etDestination.setOnClickListener {
            isSearchingOrigin = false
            launchPlaceAutocomplete()
        }

        // Swap origin and destination button
        binding.btnSwap.setOnClickListener {
            swapOriginAndDestination()
        }

        // Transport mode selection listener
        binding.chipGroupTransport.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            // Check if destination has been selected
            if (destinationLatLng == null) {
                Toast.makeText(this, "Please select a destination first", Toast.LENGTH_SHORT).show()
                binding.chipGroupTransport.clearCheck()
                binding.chipWalking.isChecked = true  // Reset to default
                return@setOnCheckedStateChangeListener
            }

            // Determine transport mode based on selected Chip
            val mode = when (checkedIds.first()) {
                R.id.chipDriving -> TransportMode.DRIVING
                R.id.chipTransit -> TransportMode.BUS
                R.id.chipCycling -> TransportMode.CYCLING
                R.id.chipWalking -> TransportMode.WALKING
                else -> TransportMode.WALKING
            }

            Log.d(TAG, "Transport mode selected: ${mode.displayName} (${mode.value})")

            // Call ViewModel to fetch route
            viewModel.fetchRouteByMode(mode)
        }

        // Initialize route steps RecyclerView
        binding.rvRouteSteps.apply {
            adapter = routeStepAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@MapActivity)
        }

        // Initialize route option RecyclerView (horizontal scroll)
        binding.rvRouteOptions.apply {
            adapter = routeOptionAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                this@MapActivity,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )
        }

        // Trip tracking button
        binding.btnTracking.setOnClickListener {
            when (viewModel.tripState.value) {
                is TripState.Idle, is TripState.Completed -> {
                    startLocationTracking()
                    viewModel.startTracking()
                }
                is TripState.Tracking -> {
                    stopLocationTracking()
                    viewModel.stopTracking()
                }
                else -> { /* Ignore other states */ }
            }
        }

        // Location button
        binding.fabMyLocation.setOnClickListener {
            isFollowingUser = true
            moveToCurrentLocation()
            // Reset origin to current location
            resetOriginToMyLocation()
        }
    }

    /**
     * Start location tracking service
     */
    private fun startLocationTracking() {
        Log.d(TAG, "Starting location tracking service")
        val intent = Intent(this, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // Check if a route exists; if so, enter navigation mode
        val routePoints = viewModel.routePoints.value
        if (!routePoints.isNullOrEmpty()) {
            isNavigationMode = true
            NavigationManager.setRoute(routePoints)
            NavigationManager.startNavigation()

            Log.d(TAG, "Navigation mode started with ${routePoints.size} points")
        } else {
            isNavigationMode = false
            Log.d(TAG, "Track recording mode started (no route)")
        }

        // Save current transit route step data
        val route = viewModel.recommendedRoute.value
        val steps = route?.route_steps
        val hasTransitSteps = steps?.any { it.travel_mode == "TRANSIT" } == true
        val hasStepPolylines = steps?.any { !it.polyline_points.isNullOrEmpty() } == true

        if (hasTransitSteps && hasStepPolylines) {
            // Transit route (with step polylines): clear and redraw multi-color route
            currentTransitSteps = steps
            clearAllRoutePolylines()
            drawTransitRoute(steps!!)
            Log.d(TAG, "Transit navigation: drew ${steps.size} colored segments (with step polylines)")
        } else if (hasTransitSteps && !steps.isNullOrEmpty() && !routePoints.isNullOrEmpty()) {
            // Transit route (without step polylines): split overview route by distance ratio
            currentTransitSteps = steps
            clearAllRoutePolylines()
            drawTransitRouteFallback(routePoints, steps)
            Log.d(TAG, "Transit navigation: drew ${steps.size} colored segments (fallback)")
        } else {
            // Non-transit route: clear all
            currentTransitSteps = null
            clearAllRoutePolylines()
        }

        // Reset milestone tracking
        reachedMilestones.clear()

        // Record navigation start time
        navigationStartTime = System.currentTimeMillis()
        detectedTransportMode = null
        backendTripId = null
        modeSegments.clear()
        lastMlConfidence = 0f
        hasTriggeredArrival = false

        isFollowingUser = true

        // Remove old location update callback
        removeLocationUpdates()

        // Start hybrid transport mode detection (Snap to Roads preferred, local sensors as fallback)
        transportModeDetector.startDetection()
        Log.d(TAG, "Hybrid transport mode detection started (Snap to Roads preferred)")

        // Request GPS location updates
        @SuppressLint("MissingPermission")
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 2000  // Update every 2 seconds
            fastestInterval = 1000
            smallestDisplacement = 5f  // Trigger update every 5 meters
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        Log.d(TAG, "GPS location updates requested (2s interval, 5m displacement)")

        // Start timer
        startTimer()

        // Call backend API to start trip, obtain real tripId
        startTripOnBackend()
    }

    /**
     * Start trip timer
     */
    private fun startTimer() {
        timerStartTime = SystemClock.elapsedRealtime()
        binding.tvTimer.visibility = View.VISIBLE
        binding.tvTimer.text = getString(R.string.timer_format, "00:00")
        timerHandler.post(timerRunnable)
    }

    /**
     * Stop trip timer
     */
    private fun stopTimer() {
        timerHandler.removeCallbacks(timerRunnable)
    }

    /**
     * Hide timer
     */
    private fun hideTimer() {
        timerHandler.removeCallbacks(timerRunnable)
        binding.tvTimer.visibility = View.GONE
    }

    /**
     * Stop location tracking service
     */
    private fun stopLocationTracking() {
        Log.d(TAG, "Stopping location tracking service")
        val intent = Intent(this, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
        }
        startService(intent)

        // Stop timer (keep displaying final elapsed time)
        stopTimer()

        // Save navigation history (if valid data exists)
        saveNavigationHistory()

        // Call backend API to complete trip (tracking -> completed)
        completeTripOnBackend()

        // Stop navigation
        if (isNavigationMode) {
            NavigationManager.stopNavigation()
            isNavigationMode = false
            currentTransitSteps = null

            // Clear navigation route
            transitSegmentPolylines.forEach { it.remove() }
            transitSegmentPolylines.clear()
            traveledPolyline?.remove()
            traveledPolyline = null
            remainingPolyline?.remove()
            remainingPolyline = null
        }

        // Remove GPS location update callback
        removeLocationUpdates()
        Log.d(TAG, "GPS location updates removed")

        // Stop transport mode detection
        transportModeDetector.stopDetection()
        Log.d(TAG, "Transport mode detection stopped")
    }

    /**
     * Save navigation history
     */
    private fun saveNavigationHistory() {
        // Check if there is valid navigation data
        if (navigationStartTime == 0L) {
            Log.w(TAG, "Navigation start time not set, skipping history save")
            return
        }

        val origin = originLatLng ?: viewModel.currentLocation.value
        val destination = destinationLatLng

        if (origin == null || destination == null) {
            Log.w(TAG, "Origin or destination not set, skipping history save")
            return
        }

        // Get route data
        val routePoints = viewModel.routePoints.value ?: emptyList()
        val trackPoints = if (isNavigationMode) {
            NavigationManager.traveledPoints.value ?: emptyList()
        } else {
            LocationManager.trackPoints.value ?: emptyList()
        }

        // If no track points, skip saving
        if (trackPoints.isEmpty()) {
            Log.w(TAG, "No track points recorded, skipping history save")
            return
        }

        // Get distance data
        val totalDistance = viewModel.routePoints.value?.let { points ->
            // Calculate total route distance (if a planned route exists)
            viewModel.recommendedRoute.value?.total_distance?.times(1000) ?: 0.0
        } ?: 0.0

        val traveledDistance = if (isNavigationMode) {
            NavigationManager.traveledDistance.value?.toDouble() ?: 0.0
        } else {
            LocationManager.totalDistance.value?.toDouble() ?: 0.0
        }

        // Get transport mode
        val transportMode = viewModel.selectedTransportMode.value?.value ?: "walk"

        // Get eco data (prefer local detection results)
        val dominantLabel = getDominantMode()
        val dominantDictMode = mlLabelToDictMode(dominantLabel)
        val carbonResult = viewModel.carbonResult.value
        val totalCarbon = carbonResult?.total_carbon_emission ?: 0.0
        val carbonSaved = carbonResult?.carbon_saved
            ?: (calculateRealTimeCarbonSaved(traveledDistance.toFloat()) / 1000.0) // Convert to kg
        val isGreenTrip = isGreenMode(dominantDictMode)
        val greenPoints = carbonResult?.green_points ?: 0

        // Get route type
        val routeType = viewModel.recommendedRoute.value?.route_type

        // Save data on background thread
        lifecycleScope.launch {
            try {
                val repository = NavigationHistoryRepository.getInstance()
                val historyId = repository.saveNavigationHistory(
                    tripId = null, // Pass backend trip_id if available
                    userId = null, // Pass user ID if user system is available
                    startTime = navigationStartTime,
                    endTime = System.currentTimeMillis(),
                    origin = origin,
                    originName = originName,
                    destination = destination,
                    destinationName = destinationName,
                    routePoints = routePoints,
                    trackPoints = trackPoints,
                    totalDistance = totalDistance,
                    traveledDistance = traveledDistance,
                    transportMode = transportMode,
                    detectedMode = detectedTransportMode,
                    totalCarbon = totalCarbon,
                    carbonSaved = carbonSaved,
                    isGreenTrip = isGreenTrip,
                    greenPoints = greenPoints,
                    routeType = routeType
                )

                Log.d(TAG, "Navigation history saved successfully with ID: $historyId")

                // Optionally show a save success prompt here
                // runOnUiThread {
                //     Toast.makeText(this@MapActivity, "Trip saved", Toast.LENGTH_SHORT).show()
                // }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save navigation history", e)
            }
        }
    }

    /**
     * Call TripRepository.startTrip() to create a trip record on the backend and obtain the real tripId
     */
    private fun startTripOnBackend() {
        val startLocation = viewModel.currentLocation.value
        if (startLocation == null) {
            Log.w(TAG, "No current location, will retry startTrip after GPS fix")
            // Delayed retry: wait for GPS fix before creating trip
            lifecycleScope.launch {
                kotlinx.coroutines.delay(3000)
                val retryLocation = viewModel.currentLocation.value
                if (retryLocation != null && backendTripId == null) {
                    Log.d(TAG, "Retrying startTrip after GPS fix")
                    doStartTripApi(retryLocation.latitude, retryLocation.longitude)
                }
            }
            return
        }

        doStartTripApi(startLocation.latitude, startLocation.longitude)
    }

    private fun doStartTripApi(lat: Double, lng: Double) {
        lifecycleScope.launch {
            try {
                val tripRepo = TripRepository.getInstance()
                Log.d(TAG, "Calling startTrip API: lat=$lat, lng=$lng, token=${tripRepo.getAuthToken().take(20)}...")
                val result = tripRepo.startTrip(
                    startLat = lat,
                    startLng = lng,
                    startPlaceName = originName.ifEmpty { "Origin" },
                    startAddress = originName.ifEmpty { "Unknown address" }
                )

                result.fold(
                    onSuccess = { tripId ->
                        backendTripId = tripId
                        Log.d(TAG, "Trip started on backend: tripId=$tripId")
                        runOnUiThread {
                            viewModel.setBackendTripId(tripId)
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to start trip on backend: ${error.message}", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error starting trip on backend", e)
            }
        }
    }

    /**
     * Call TripRepository.completeTrip() to change backend trip status from tracking to completed
     * Includes: dominant transport mode, is_green_trip, full polyline_points, ML confidence
     */
    private fun completeTripOnBackend() {
        // Prefer backend real tripId, fall back to ViewModel's tripId
        val tripId = backendTripId ?: viewModel.currentTripId.value
        if (tripId == null || tripId.startsWith("MOCK_") || tripId == "restored-trip") {
            Log.w(TAG, "No valid backend tripId ($tripId), skipping completeTrip API call")
            return
        }

        val endLocation = viewModel.currentLocation.value
        if (endLocation == null) {
            Log.w(TAG, "No current location, skipping completeTrip API call")
            return
        }

        // Collect all track points (polyline_points)
        val trackPoints: List<LatLng> = if (isNavigationMode) {
            NavigationManager.traveledPoints.value ?: emptyList()
        } else {
            LocationManager.trackPoints.value ?: emptyList()
        }

        if (trackPoints.isEmpty()) {
            Log.w(TAG, "No track points, skipping completeTrip API call")
            return
        }

        // Calculate traveled distance (meters)
        val distanceMeters = if (isNavigationMode) {
            NavigationManager.traveledDistance.value?.toDouble() ?: 0.0
        } else {
            LocationManager.totalDistance.value?.toDouble() ?: 0.0
        }

        // Set end time for the last segment
        modeSegments.lastOrNull()?.endTime = System.currentTimeMillis()

        // Build transport mode segment list (pass exactly what the UI displays)
        val segments = buildTransportModeSegments(distanceMeters)

        // Use Helper to prepare trip completion data
        val completionData = MapActivityHelper.prepareTripCompletionData(
            modeSegments = modeSegments,
            lastMlConfidence = lastMlConfidence,
            userSelectedModeValue = viewModel.selectedTransportMode.value?.value,
            distanceMeters = distanceMeters,
            selectedTransportMode = viewModel.selectedTransportMode.value
        )

        val detectedMode = completionData.detectedMode
        val userSelectedMode = completionData.userSelectedMode
        val greenTrip = completionData.isGreenTrip
        val carbonSavedGrams = completionData.carbonSavedGrams
        val confidence = completionData.mlConfidence

        Log.d(TAG, "Completing trip on backend: tripId=$tripId, " +
                "segments=${segments.map { "${it.mode}(${it.subDuration}s)" }}, " +
                "detectedMode=$detectedMode, userMode=$userSelectedMode, " +
                "isGreen=$greenTrip, points=${trackPoints.size}, distance=${distanceMeters}m, " +
                "carbonSaved=${carbonSavedGrams}g, confidence=$confidence")

        lifecycleScope.launch {
            try {
                val tripRepo = TripRepository.getInstance()
                val result = tripRepo.completeTrip(
                    tripId = tripId,
                    endLat = endLocation.latitude,
                    endLng = endLocation.longitude,
                    endPlaceName = destinationName.ifEmpty { "Destination" },
                    endAddress = destinationName.ifEmpty { "Unknown address" },
                    distance = distanceMeters,
                    trackPoints = trackPoints,
                    transportMode = userSelectedMode,
                    detectedMode = detectedMode,
                    mlConfidence = confidence,
                    carbonSaved = carbonSavedGrams,
                    isGreenTrip = greenTrip,
                    transportModeSegments = segments
                )

                result.fold(
                    onSuccess = { response ->
                        backendTripId = null
                        Log.d(TAG, "Trip completed on backend: ${response.tripId}, status=${response.status}")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to complete trip on backend: ${error.message}", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error completing trip on backend", e)
            }
        }
    }

    /**
     * Observe LocationManager location updates
     */
    private fun observeLocationManager() {
        // Observe current location
        LocationManager.currentLocation.observe(this) { latLng ->
            onCurrentLocationUpdated(latLng)
        }

        // Observe track points (draw only in non-navigation mode)
        LocationManager.trackPoints.observe(this) { points ->
            if (points.isNotEmpty() && !isNavigationMode) {
                drawTrackPolyline(points)
            }
        }

        // Observe total distance
        LocationManager.totalDistance.observe(this) { distance ->
            if (LocationManager.isTracking.value == true && !isNavigationMode) {
                updateTrackingInfo(distance)
            }
        }
    }

    /**
     * Handle current location update (extracted from observeLocationManager to reduce cognitive complexity)
     */
    private fun onCurrentLocationUpdated(latLng: LatLng) {
        // Update ViewModel
        viewModel.updateCurrentLocation(latLng)

        // If tracking and follow mode is enabled, move camera
        if (LocationManager.isTracking.value == true && isFollowingUser) {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        }

        // Update transport mode detector location (for GPS speed)
        if (LocationManager.isTracking.value == true) {
            val location = android.location.Location("gps").apply {
                latitude = latLng.latitude
                longitude = latLng.longitude
                time = System.currentTimeMillis()
            }
            lifecycleScope.launch {
                transportModeDetector.updateLocation(location)
            }
            Log.d(TAG, "Location updated for detector: lat=${latLng.latitude}, lng=${latLng.longitude}")
        }

        // In navigation mode, check if approaching destination (backup check in case currentRouteIndex observer didn't trigger)
        if (isNavigationMode && NavigationManager.isNavigating.value == true) {
            if (NavigationManager.hasReachedDestination()) {
                onReachedDestination()
            }
        }
    }

    /**
     * Observe NavigationManager navigation state
     */
    private fun observeNavigationManager() {
        // Observe traveled route
        NavigationManager.traveledPoints.observe(this) { points ->
            if (isNavigationMode && points.isNotEmpty()) {
                drawTraveledRoute(points)
            }
        }

        // Observe remaining route
        NavigationManager.remainingPoints.observe(this) { points ->
            if (isNavigationMode && points.isNotEmpty()) {
                // During transit navigation, multi-color segments already display the route, no need to draw remaining route separately
                if (currentTransitSteps != null) {
                    return@observe
                }
                drawRemainingRoute(points)
            }
        }

        // Observe traveled distance
        NavigationManager.traveledDistance.observe(this) { distance ->
            if (isNavigationMode) {
                updateNavigationInfo(distance)
            }
        }

        // Observe whether destination has been reached
        NavigationManager.currentRouteIndex.observe(this) { _ ->
            if (NavigationManager.hasReachedDestination()) {
                onReachedDestination()
            }
        }
    }

    /**
     * Get Google Maps API Key
     */
    private fun getGoogleMapsApiKey(): String? {
        return try {
            val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            ai.metaData?.getString("com.google.android.geo.API_KEY")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get Google Maps API Key: ${e.message}")
            null
        }
    }

    /**
     * GPS location callback (used to pass to hybrid detector)
     */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                // Pass GPS location to hybrid detector
                lifecycleScope.launch {
                    transportModeDetector.updateLocation(location)
                }
                // Update current location (for ViewModel)
                val latLng = LatLng(location.latitude, location.longitude)
                viewModel.updateCurrentLocation(latLng)
            }
        }
    }

    /**
     * Remove location updates
     */
    private fun removeLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /**
     * Observe transport mode detector
     */
    private fun observeTransportModeDetector() {
        lifecycleScope.launch {
            Log.d(TAG, "Started observing transport mode detector")
            transportModeDetector.detectedMode.collect { prediction ->
                Log.d(TAG, "Received prediction: $prediction")
                prediction?.let {
                    onTransportModeDetected(it)
                }
            }
        }
    }

    /**
     * Handle detected transport mode
     */
    private fun onTransportModeDetected(prediction: com.ecogo.mapengine.ml.TransportModePrediction) {
        if (!LocationManager.isTracking.value!!) return

        val now = System.currentTimeMillis()
        lastMlConfidence = prediction.confidence

        // Record by segment: record exactly what the UI displays
        val lastSegment = modeSegments.lastOrNull()
        if (lastSegment == null || lastSegment.mode != prediction.mode) {
            lastSegment?.endTime = now
            modeSegments.add(MapActivityHelper.ModeSegment(mode = prediction.mode, startTime = now))
        } else {
            lastSegment.endTime = now
        }

        detectedTransportMode = mlLabelToDictMode(prediction.mode)

        val modeIcon = MapActivityHelper.getModeIcon(prediction.mode)
        val modeText = MapActivityHelper.getModeText(prediction.mode)

        val confidencePercent = (prediction.confidence * 100).toInt()

        // Update UI to display detected transport mode (at a prominent top position)
        runOnUiThread {
            if (binding.cardRouteInfo.visibility == View.VISIBLE) {
                // Display current transport mode at route type position
                if (isNavigationMode) {
                    binding.tvRouteType.text = "$modeIcon Current: $modeText ($confidencePercent%)"
                } else {
                    binding.tvRouteType.text = "$modeIcon Detected: $modeText ($confidencePercent%)"
                }
            }
        }

        Log.d(TAG, "Detected transport mode: $modeText, confidence: ${prediction.confidence}")
    }

    /**
     * Detect whether running on an emulator
     * Checks multiple device properties for reliability
     */
    private fun isRunningOnEmulator(): Boolean {
        return MapActivityHelper.isRunningOnEmulator()
    }

    /**
     * Draw traveled route (gray)
     */
    private fun drawTraveledRoute(points: List<LatLng>) {
        traveledPolyline?.remove()

        if (points.size < 2) return

        traveledPolyline = googleMap?.addPolyline(
            PolylineOptions()
                .addAll(points)
                .width(12f)
                .color(ContextCompat.getColor(this, R.color.route_traveled))
                .geodesic(true)
                .jointType(JointType.ROUND)
                .startCap(RoundCap())
                .endCap(RoundCap())
        )
    }

    /**
     * Draw remaining route (blue)
     */
    private fun drawRemainingRoute(points: List<LatLng>) {
        remainingPolyline?.remove()

        if (points.size < 2) return

        remainingPolyline = googleMap?.addPolyline(
            PolylineOptions()
                .addAll(points)
                .width(12f)
                .color(ContextCompat.getColor(this, R.color.route_remaining))
                .geodesic(true)
                .jointType(JointType.ROUND)
                .startCap(RoundCap())
                .endCap(RoundCap())
        )
    }

    /**
     * Update navigation info display
     */
    private fun updateNavigationInfo(traveledMeters: Float) {
        val traveledKm = traveledMeters / 1000f
        val remainingMeters = NavigationManager.remainingDistance.value ?: 0f
        val remainingKm = remainingMeters / 1000f

        if (binding.cardRouteInfo.visibility == View.VISIBLE) {
            // Get real-time carbon emission info and encouragement message
            val encouragementMessage = generateEncouragementMessage(traveledMeters)
            binding.tvCarbonSaved.text = encouragementMessage
            binding.tvDuration.text = String.format("Remaining: %.2f km", remainingKm)

            // Check if a milestone has been reached
            checkMilestones(traveledMeters)
        }
    }

    private fun calculateRealTimeCarbonSaved(distanceMeters: Float): Double =
        MapActivityHelper.calculateRealTimeCarbonSaved(distanceMeters, viewModel.selectedTransportMode.value)

    private fun generateEncouragementMessage(distanceMeters: Float): String =
        MapActivityHelper.generateEncouragementMessage(distanceMeters, viewModel.selectedTransportMode.value)

    private fun checkMilestones(distanceMeters: Float) {
        val milestone = MapActivityHelper.checkMilestone(distanceMeters, milestones, reachedMilestones)
        if (milestone != null) {
            reachedMilestones.add(milestone)
            showMilestoneToast(milestone)
        }
    }

    private fun showMilestoneToast(milestoneMeters: Float) {
        val message = MapActivityHelper.generateMilestoneMessage(milestoneMeters, viewModel.selectedTransportMode.value)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // Prevent onReachedDestination from triggering repeatedly
    private var hasTriggeredArrival = false

    /**
     * Reached destination
     */
    private fun onReachedDestination() {
        if (hasTriggeredArrival) return
        hasTriggeredArrival = true

        Toast.makeText(this, "You have arrived at your destination!", Toast.LENGTH_LONG).show()
        // Automatically stop trip
        stopLocationTracking()
        viewModel.stopTracking()
    }

    /**
     * Draw real-time track
     */
    private fun drawTrackPolyline(points: List<LatLng>) {
        trackPolyline?.remove()

        if (points.size < 2) return

        trackPolyline = googleMap?.addPolyline(
            PolylineOptions()
                .addAll(points)
                .width(10f)
                .color(ContextCompat.getColor(this, R.color.green_primary))
                .geodesic(true)
                .jointType(JointType.ROUND)
                .startCap(RoundCap())
                .endCap(RoundCap())
        )
    }

    /**
     * Update tracking info display
     */
    private fun updateTrackingInfo(distanceMeters: Float) {
        val distanceKm = distanceMeters / 1000f
        // Display real-time distance on route info card
        if (binding.cardRouteInfo.visibility == View.VISIBLE) {
            // Use the same encouragement message as navigation
            val encouragementMessage = generateEncouragementMessage(distanceMeters)
            binding.tvCarbonSaved.text = encouragementMessage

            // Check if a milestone has been reached
            checkMilestones(distanceMeters)
        }
    }

    /**
     * Launch Places Autocomplete
     */
    private fun launchPlaceAutocomplete() {
        try {
            // Ensure Places SDK is initialized
            if (!Places.isInitialized()) {
                initPlaces()
                if (!Places.isInitialized()) {
                    Log.e(TAG, "Places SDK failed to initialize")
                    Toast.makeText(this, "Place search service not initialized, please check API key configuration", Toast.LENGTH_LONG).show()
                    return
                }
            }

            val fields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS
            )

            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(this)

            autocompleteLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching autocomplete: ${e.message}", e)
            Toast.makeText(this, "Search service temporarily unavailable: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Handle Autocomplete result
     */
    private fun handleAutocompleteResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> handlePlaceSelected(result)
            AutocompleteActivity.RESULT_ERROR -> {
                result.data?.let { data ->
                    val status = Autocomplete.getStatusFromIntent(data)
                    Log.e(TAG, "Autocomplete error: ${status.statusMessage}")
                    Toast.makeText(this, "Search error: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
                }
            }
            Activity.RESULT_CANCELED -> {
                Log.d(TAG, "Autocomplete canceled")
            }
        }
    }

    /**
     * Handle user-selected place (extracted from handleAutocompleteResult to reduce cognitive complexity)
     */
    private fun handlePlaceSelected(result: ActivityResult) {
        val data = result.data ?: return
        val place = Autocomplete.getPlaceFromIntent(data)
        val latLng = place.latLng ?: return

        if (isSearchingOrigin) {
            applyOriginFromPlace(latLng, place)
        } else {
            applyDestinationFromPlace(latLng, place)
        }

        updateStartButtonVisibility()
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        fitBoundsIfReady()
    }

    private fun applyOriginFromPlace(latLng: LatLng, place: com.google.android.libraries.places.api.model.Place) {
        originLatLng = latLng
        originName = place.name ?: place.address ?: "Origin"
        binding.etOrigin.setText(originName)
        updateOriginMarker(latLng, originName)
        viewModel.setOrigin(latLng)

        if (destinationLatLng != null) {
            binding.cardTransportModes.visibility = View.VISIBLE
            binding.chipDriving.isChecked = true
            viewModel.fetchRouteByMode(TransportMode.DRIVING)
        }
    }

    private fun applyDestinationFromPlace(latLng: LatLng, place: com.google.android.libraries.places.api.model.Place) {
        destinationLatLng = latLng
        destinationName = place.name ?: place.address ?: "Destination"
        binding.etDestination.setText(destinationName)
        updateDestinationMarker(latLng, destinationName)
        viewModel.setDestination(latLng)

        binding.cardTransportModes.visibility = View.VISIBLE

        if (originLatLng != null || viewModel.currentLocation.value != null) {
            binding.chipDriving.isChecked = true
            viewModel.fetchRouteByMode(TransportMode.DRIVING)
        }
    }

    /**
     * Swap origin and destination
     */
    private fun swapOriginAndDestination() {
        // Swap locations
        val tempLatLng = originLatLng
        val tempName = originName

        originLatLng = destinationLatLng
        originName = destinationName

        destinationLatLng = tempLatLng
        destinationName = tempName

        // Update UI
        binding.etOrigin.setText(if (originLatLng != null) originName else MY_LOCATION_LABEL)
        binding.etDestination.setText(destinationName)

        // Update markers
        originLatLng?.let {
            updateOriginMarker(it, originName)
            viewModel.setOrigin(it)  // Update origin after swap
        }
        destinationLatLng?.let {
            updateDestinationMarker(it, destinationName)
            viewModel.setDestination(it)
        }

        // Clear route
        clearAllRoutePolylines()
        binding.cardRouteInfo.visibility = View.GONE

        // Automatically fetch default driving route (if both origin and destination are set)
        if (originLatLng != null && destinationLatLng != null) {
            binding.chipDriving.isChecked = true
            viewModel.fetchRouteByMode(TransportMode.DRIVING)
        }

        // Update start button visibility
        updateStartButtonVisibility()
    }

    /**
     * Reset origin to current location
     */
    @SuppressLint("MissingPermission")
    private fun resetOriginToMyLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                originLatLng = latLng
                originName = MY_LOCATION_LABEL
                binding.etOrigin.setText(originName)
                originMarker?.remove()
                originMarker = null
                viewModel.setOrigin(latLng)  // Reset origin to current location
                updateStartButtonVisibility()  // Update start button visibility
            }
        }
    }

    /**
     * If both origin and destination are set, adjust camera to show both points
     */
    private fun fitBoundsIfReady() {
        val origin = originLatLng ?: viewModel.currentLocation.value
        val destination = destinationLatLng

        if (origin != null && destination != null) {
            val boundsBuilder = LatLngBounds.Builder()
            boundsBuilder.include(origin)
            boundsBuilder.include(destination)
            val bounds = boundsBuilder.build()
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
        }
    }

    /**
     * Update start trip button visibility
     * Only show the button when both origin and destination are set, and hide the ad placeholder
     */
    private fun updateStartButtonVisibility() {
        val hasOrigin = originLatLng != null || viewModel.currentLocation.value != null
        val hasDestination = destinationLatLng != null

        if (hasOrigin && hasDestination) {
            // Origin and destination selected: hide ads, show button
            binding.layoutAdCarousel.visibility = View.GONE
            binding.cardBottomPanel.visibility = View.VISIBLE
        } else {
            // Not fully selected: show ads only for non-VIP users, hide button
            if (!isVipUserFlag) {
                binding.layoutAdCarousel.visibility = View.VISIBLE
            }
            binding.cardBottomPanel.visibility = View.GONE
        }
    }

    /**
     * Observe ViewModel data changes
     */
    private fun observeViewModel() {
        // Observe current location
        viewModel.currentLocation.observe(this) { location ->
            if (originName == MY_LOCATION_LABEL && originLatLng == null) {
                originLatLng = location
            }
        }

        // Observe destination
        viewModel.destination.observe(this) { destination ->
            destination?.let { destinationLatLng = it }
        }

        // Observe trip state
        viewModel.tripState.observe(this) { state -> updateTrackingUI(state) }

        // Observe recommended route
        viewModel.recommendedRoute.observe(this) { route ->
            route?.let { updateRouteInfo(it) }
        }

        // Observe route points
        viewModel.routePoints.observe(this) { points -> handleRoutePointsUpdate(points) }

        // Observe carbon footprint result
        viewModel.carbonResult.observe(this) { result ->
            result?.let { handleCarbonResult(it) }
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        // Observe success messages
        viewModel.successMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccessMessage()
            }
        }
    }

    /**
     * Handle route points update (extracted from observeViewModel to reduce cognitive complexity)
     */
    private fun handleRoutePointsUpdate(points: List<LatLng>) {
        if (isHandlingRouteSelection) {
            isHandlingRouteSelection = false
            Log.d(TAG, "routePoints observer: skipped (handling route selection)")
            return
        }

        val route = viewModel.recommendedRoute.value
        val steps = route?.route_steps
        val analysis = MapActivityHelper.analyzeRouteSteps(steps)

        Log.d(TAG, "routePoints observer: points=${points.size}, hasTransitSteps=${analysis.hasTransitSteps}, hasStepPolylines=${analysis.hasStepPolylines}, stepsCount=${steps?.size ?: 0}")

        if (analysis.hasTransitSteps && analysis.hasStepPolylines) {
            drawTransitRoute(steps!!)
        } else if (analysis.hasTransitSteps && !steps.isNullOrEmpty()) {
            Log.d(TAG, "routePoints observer: fallback - splitting overview polyline by step distances")
            drawTransitRouteFallback(points, steps)
        } else {
            drawRoute(points)
        }
    }

    /**
     * Handle carbon footprint calculation result (extracted from observeViewModel to reduce cognitive complexity)
     */
    private fun handleCarbonResult(result: com.ecogo.mapengine.data.model.CarbonCalculateData) {
        if (result.carbon_saved > 0) {
            com.ecogo.mapengine.util.GreenTravelStats.recordGreenTrip(this, result.carbon_saved)
        }

        val message = MapActivityHelper.generateTripCompletionMessage(
            result.is_green_trip, result.carbon_saved, result.green_points
        )
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        binding.tvCumulativeImpact.text = com.ecogo.mapengine.util.GreenTravelStats.formatWeeklyImpact(this)
    }

    /**
     * Map ready callback
     */
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Configure map
        map.apply {
            uiSettings.isZoomControlsEnabled = false
            uiSettings.isCompassEnabled = true
            uiSettings.isMapToolbarEnabled = false

            // Map click can also set destination
            setOnMapClickListener { latLng ->
                // If tracking, prevent modifying destination
                if (viewModel.tripState.value is TripState.Tracking) {
                    return@setOnMapClickListener
                }

                // Show confirmation dialog
                androidx.appcompat.app.AlertDialog.Builder(this@MapActivity)
                    .setTitle("Set Destination")
                    .setMessage("Set this location as destination?")
                    .setPositiveButton("OK") { dialog, _ ->
                        destinationLatLng = latLng
                        destinationName = "Location on map"
                        binding.etDestination.setText(destinationName)
                        updateDestinationMarker(latLng, destinationName)
                        viewModel.setDestination(latLng)

                        // Show transport mode selection card
                        binding.cardTransportModes.visibility = View.VISIBLE

                        // Automatically fetch default route (driving)
                        if (originLatLng != null || viewModel.currentLocation.value != null) {
                            binding.chipDriving.isChecked = true
                            viewModel.fetchRouteByMode(TransportMode.DRIVING)
                        }

                        // Update start button visibility
                        updateStartButtonVisibility()

                        fitBoundsIfReady()
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }

            // Long press to clear destination
            setOnMapLongClickListener {
                if (viewModel.tripState.value !is TripState.Tracking) {
                    clearDestination()
                }
            }

            // Stop following when map is moved
            setOnCameraMoveStartedListener { reason ->
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    isFollowingUser = false
                }
            }
        }

        // Request location permission
        checkLocationPermission()

        // If there is an ongoing trip, restore route display
        if (LocationManager.isTracking.value == true) {
            Log.d(TAG, "Map ready - restoring routes for ongoing trip")
            restoreRoutesOnMap()
        }

        // Check if navigated from activity details, auto-set destination
        handleActivityDestination()
    }

    /**
     * Read activity destination coordinates from Intent extras and auto-set on the map
     */
    private fun handleActivityDestination() {
        val lat = intent.getDoubleExtra(EXTRA_DEST_LAT, Double.NaN)
        val lng = intent.getDoubleExtra(EXTRA_DEST_LNG, Double.NaN)
        if (lat.isNaN() || lng.isNaN()) return

        val name = intent.getStringExtra(EXTRA_DEST_NAME) ?: "Event Location"
        val latLng = LatLng(lat, lng)

        destinationLatLng = latLng
        destinationName = name
        binding.etDestination.setText(name)
        updateDestinationMarker(latLng, name)
        viewModel.setDestination(latLng)

        // Show transport mode selection card
        binding.cardTransportModes.visibility = View.VISIBLE

        // Move camera to destination
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

        Log.d(TAG, "Activity destination set: $name ($lat, $lng)")
    }

    /**
     * Clear destination
     */
    private fun clearDestination() {
        destinationLatLng = null
        destinationName = ""
        binding.etDestination.setText("")
        destinationMarker?.remove()
        destinationMarker = null
        clearAllRoutePolylines()
        binding.cardRouteInfo.visibility = View.GONE
        binding.cardTransportModes.visibility = View.GONE
        // Hide button; only show ads for non-VIP users
        binding.cardBottomPanel.visibility = View.GONE
        if (!isVipUserFlag) {
            binding.layoutAdCarousel.visibility = View.VISIBLE
        }
        viewModel.clearDestination()
    }

    /**
     * Check location permission
     */
    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableMyLocation()
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    /**
     * Enable my location layer
     */
    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        googleMap?.isMyLocationEnabled = true
        googleMap?.uiSettings?.isMyLocationButtonEnabled = false

        // Get current location
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                originLatLng = latLng
                viewModel.updateCurrentLocation(latLng)
                moveToCurrentLocation()
                updateStartButtonVisibility()  // Update start button visibility
            }
        }
    }

    /**
     * Move camera to current location
     */
    private fun moveToCurrentLocation() {
        val location = LocationManager.currentLocation.value
            ?: originLatLng
            ?: viewModel.currentLocation.value

        location?.let {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 16f))
        }
    }

    /**
     * Update origin marker
     */
    private fun updateOriginMarker(location: LatLng, title: String) {
        originMarker?.remove()
        originMarker = googleMap?.addMarker(
            MarkerOptions()
                .position(location)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )
    }

    /**
     * Update destination marker
     */
    private fun updateDestinationMarker(location: LatLng, title: String) {
        destinationMarker?.remove()
        destinationMarker = googleMap?.addMarker(
            MarkerOptions()
                .position(location)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
    }

    /**
     * Draw route (use different colors and styles based on transport mode)
     */
    private fun drawRoute(points: List<LatLng>) {
        // Clear all previous routes
        clearAllRoutePolylines()

        if (points.isEmpty()) return

        // Choose color and style based on current transport mode
        val mode = viewModel.selectedTransportMode.value
        val color = when (mode) {
            TransportMode.DRIVING -> ContextCompat.getColor(this, R.color.route_driving)
            TransportMode.WALKING -> ContextCompat.getColor(this, R.color.route_walking)
            TransportMode.CYCLING -> ContextCompat.getColor(this, R.color.route_cycling)
            TransportMode.BUS -> ContextCompat.getColor(this, R.color.route_bus)
            TransportMode.SUBWAY -> ContextCompat.getColor(this, R.color.route_subway)
            else -> ContextCompat.getColor(this, R.color.route_driving)
        }
        val width = if (mode == TransportMode.WALKING) 8f else 12f

        val polylineOptions = PolylineOptions()
            .addAll(points)
            .width(width)
            .color(color)
            .geodesic(true)
            .jointType(JointType.ROUND)
            .startCap(RoundCap())
            .endCap(RoundCap())

        // Walking uses dashed line
        if (mode == TransportMode.WALKING) {
            polylineOptions.pattern(listOf(Dot(), Gap(10f)))
        }

        Log.d(TAG, "drawRoute: mode=${mode?.displayName}, points=${points.size}, color=#${Integer.toHexString(color)}")

        routePolyline = googleMap?.addPolyline(polylineOptions)

        // Adjust camera to show full route
        if (points.size >= 2) {
            val boundsBuilder = LatLngBounds.Builder()
            points.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        }
    }

    /**
     * Draw transit/subway multi-segment route (different color per segment)
     *
     * @param steps route step list (containing polyline_points and travel_mode)
     */
    private fun drawTransitRoute(steps: List<com.ecogo.mapengine.data.model.RouteStep>) {
        // Clear all previous routes
        clearAllRoutePolylines()

        Log.d(TAG, "drawTransitRoute: ${steps.size} steps total")

        val allPoints = mutableListOf<LatLng>()
        var segmentCount = 0

        for ((index, step) in steps.withIndex()) {
            val hasPolyline = step.polyline_points != null
            val pointCount = step.polyline_points?.size ?: 0
            Log.d(TAG, "  Step $index: mode=${step.travel_mode}, vehicle=${step.transit_details?.vehicle_type}, hasPolyline=$hasPolyline, points=$pointCount")

            val stepPoints = step.polyline_points?.map {
                LatLng(it.lat, it.lng)
            } ?: continue

            if (stepPoints.size < 2) continue

            segmentCount++
            allPoints.addAll(stepPoints)

            // Select color based on transport mode and vehicle type
            val color = getColorForTransitStep(step)

            // Walking segments use dashed line, others use solid line
            val isWalking = step.travel_mode == "WALKING"

            val polylineOptions = PolylineOptions()
                .addAll(stepPoints)
                .width(if (isWalking) 8f else 14f)
                .color(color)
                .geodesic(true)
                .jointType(JointType.ROUND)
                .startCap(RoundCap())
                .endCap(RoundCap())

            // Walking segments use dashed line style
            if (isWalking) {
                polylineOptions.pattern(listOf(Dot(), Gap(10f)))
            }

            googleMap?.addPolyline(polylineOptions)?.let {
                transitSegmentPolylines.add(it)
            }
        }

        Log.d(TAG, "drawTransitRoute: drew $segmentCount colored segments, ${allPoints.size} total points")

        // Adjust camera to show full route
        if (allPoints.size >= 2) {
            val boundsBuilder = LatLngBounds.Builder()
            allPoints.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        }
    }

    /**
     * Fallback: when steps lack polyline_points, split overview route by distance ratio and color
     *
     * @param overviewPoints all points of the overview polyline
     * @param steps route steps (containing travel_mode and distance)
     */
    private fun drawTransitRouteFallback(overviewPoints: List<LatLng>, steps: List<com.ecogo.mapengine.data.model.RouteStep>) {
        clearAllRoutePolylines()

        if (overviewPoints.size < 2 || steps.isEmpty()) {
            drawRoute(overviewPoints)
            return
        }

        val totalStepDistance = steps.sumOf { it.distance }
        if (totalStepDistance <= 0) {
            drawRoute(overviewPoints)
            return
        }

        Log.d(TAG, "drawTransitRouteFallback: ${overviewPoints.size} points, ${steps.size} steps, totalDist=$totalStepDistance m")

        var pointIndex = 0
        for ((stepIdx, step) in steps.withIndex()) {
            pointIndex = drawFallbackSegment(overviewPoints, step, stepIdx, steps.size, totalStepDistance, pointIndex)
        }

        // Adjust camera
        val boundsBuilder = LatLngBounds.Builder()
        overviewPoints.forEach { boundsBuilder.include(it) }
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120))
    }

    /**
     * Draw a single step segment in fallback mode (extracted from drawTransitRouteFallback to reduce complexity)
     * @return updated pointIndex
     */
    private fun drawFallbackSegment(
        overviewPoints: List<LatLng>,
        step: com.ecogo.mapengine.data.model.RouteStep,
        stepIdx: Int,
        totalSteps: Int,
        totalStepDistance: Double,
        pointIndex: Int
    ): Int {
        val totalPoints = overviewPoints.size
        val pointsForStep = MapActivityHelper.calculateFallbackPointsPerStep(
            totalPoints, step.distance, totalStepDistance, stepIdx == totalSteps - 1, pointIndex
        )

        val endIndex = (pointIndex + pointsForStep).coerceAtMost(totalPoints)
        if (endIndex <= pointIndex || pointIndex >= totalPoints) return endIndex

        val segmentPoints = overviewPoints.subList(pointIndex, endIndex)
        if (segmentPoints.size < 2) return endIndex

        val color = getColorForTransitStep(step)
        val isWalking = step.travel_mode == "WALKING"

        val polylineOptions = PolylineOptions()
            .addAll(segmentPoints)
            .width(if (isWalking) 8f else 14f)
            .color(color)
            .geodesic(true)
            .jointType(JointType.ROUND)
            .startCap(RoundCap())
            .endCap(RoundCap())

        if (isWalking) {
            polylineOptions.pattern(listOf(Dot(), Gap(10f)))
        }

        googleMap?.addPolyline(polylineOptions)?.let { transitSegmentPolylines.add(it) }

        Log.d(TAG, "  Fallback step $stepIdx: mode=${step.travel_mode}, vehicle=${step.transit_details?.vehicle_type}, points=${segmentPoints.size}")
        return endIndex
    }

    /**
     * Get color for transit step
     */
    private fun getColorForTransitStep(step: com.ecogo.mapengine.data.model.RouteStep): Int {
        val colorName = MapActivityHelper.getTransitStepColorName(step.travel_mode, step.transit_details?.vehicle_type)
        return when (colorName) {
            "walking" -> ContextCompat.getColor(this, R.color.route_walking)
            "subway" -> ContextCompat.getColor(this, R.color.route_subway)
            "bus" -> ContextCompat.getColor(this, R.color.route_bus)
            "rail" -> ContextCompat.getColor(this, R.color.route_rail)
            "tram" -> ContextCompat.getColor(this, R.color.route_tram)
            "driving" -> ContextCompat.getColor(this, R.color.route_driving)
            "cycling" -> ContextCompat.getColor(this, R.color.route_cycling)
            else -> ContextCompat.getColor(this, R.color.route_remaining)
        }
    }

    /**
     * Clear all route Polylines
     */
    private fun clearAllRoutePolylines() {
        routePolyline?.remove()
        routePolyline = null
        traveledPolyline?.remove()
        traveledPolyline = null
        remainingPolyline?.remove()
        remainingPolyline = null
        trackPolyline?.remove()
        trackPolyline = null
        transitSegmentPolylines.forEach { it.remove() }
        transitSegmentPolylines.clear()
    }

    /**
     * Update route info card
     */
    private fun updateRouteInfo(route: com.ecogo.mapengine.data.model.RouteRecommendData) {
        binding.cardRouteInfo.visibility = View.VISIBLE

        // Use Helper to build route info text
        val hasTransitSteps = route.route_steps?.any { it.travel_mode == "TRANSIT" } == true
        val infoTexts = MapActivityHelper.buildRouteInfoTexts(
            routeType = route.route_type,
            carbonSaved = route.carbon_saved,
            totalCarbon = route.total_carbon,
            totalDistance = route.total_distance,
            estimatedDuration = route.estimated_duration,
            duration = route.duration,
            hasAlternatives = !route.route_alternatives.isNullOrEmpty(),
            hasTransitSteps = hasTransitSteps
        )

        binding.tvRouteType.text = infoTexts.headerText
        binding.tvCarbonSaved.text = infoTexts.carbonSavedText

        val carbonColor = android.graphics.Color.parseColor(infoTexts.carbonColorHex)
        binding.tvCarbonSaved.setTextColor(carbonColor)

        binding.tvDuration.text = infoTexts.durationText

        // Show cumulative eco contribution (only for green travel modes)
        if (infoTexts.showCumulativeImpact) {
            binding.tvCumulativeImpact.visibility = View.VISIBLE
            binding.tvCumulativeImpact.text = com.ecogo.mapengine.util.GreenTravelStats.formatWeeklyImpact(this)
        } else {
            binding.tvCumulativeImpact.visibility = View.GONE
        }

        // Show route selection list (transit mode only and with alternatives)
        if (infoTexts.showRouteOptions) {
            binding.rvRouteOptions.visibility = View.VISIBLE
            routeOptionAdapter.setRoutes(route.route_alternatives!!)
        } else {
            binding.rvRouteOptions.visibility = View.GONE
        }

        // Show detailed step list (transit mode only)
        if (infoTexts.showRouteSteps && !route.route_steps.isNullOrEmpty()) {
            binding.rvRouteSteps.visibility = View.VISIBLE
            routeStepAdapter.setSteps(route.route_steps)
        } else {
            binding.rvRouteSteps.visibility = View.GONE
        }
    }

    private fun calculateEcoRating(totalCarbon: Double, distance: Double): String =
        MapActivityHelper.calculateEcoRating(totalCarbon, distance)

    /**
     * Handle user route selection
     */
    private fun onRouteSelected(route: com.ecogo.mapengine.data.model.RouteAlternative) {
        Log.d(TAG, "Route selected: ${route.summary}")

        // Check if step-level polyline data is available for multi-color rendering
        val hasTransitSteps = route.route_steps.any { it.travel_mode == "TRANSIT" }
        val hasStepPolylines = route.route_steps.any { !it.polyline_points.isNullOrEmpty() }
        val points = route.route_points.map { com.google.android.gms.maps.model.LatLng(it.lat, it.lng) }

        if (hasTransitSteps && hasStepPolylines) {
            // Use step-level polyline for multi-color rendering
            drawTransitRoute(route.route_steps)
        } else if (hasTransitSteps && route.route_steps.isNotEmpty()) {
            // Fallback: split overview route by distance ratio and color
            drawTransitRouteFallback(points, route.route_steps)
        } else {
            // Non-transit: single color rendering
            drawRoute(points)
        }

        // Update ViewModel route points (for navigation), set flag to avoid observer re-drawing
        isHandlingRouteSelection = true
        val allPoints = route.route_points.map { LatLng(it.lat, it.lng) }
        viewModel.updateRoutePointsForSelectedAlternative(allPoints)

        // Update route info
        binding.tvCarbonSaved.text = String.format("Carbon saved: %.2f kg", route.total_carbon)
        binding.tvDuration.text = "Estimated: ${route.estimated_duration} min"

        // Update detailed steps
        if (hasTransitSteps) {
            binding.rvRouteSteps.visibility = View.VISIBLE
            routeStepAdapter.setSteps(route.route_steps)
        } else {
            binding.rvRouteSteps.visibility = View.GONE
        }

        Toast.makeText(this, "Switched to: ${route.summary}", Toast.LENGTH_SHORT).show()
    }

    /**
     * Update trip tracking UI
     */
    private fun updateTrackingUI(state: TripState) {
        when (state) {
            is TripState.Idle -> {
                binding.btnTracking.text = getString(R.string.start_tracking)
                binding.btnTracking.isEnabled = true
                binding.chipGroupTransport.visibility = View.VISIBLE
                binding.cardSearch.visibility = View.VISIBLE
                hideTimer()
                // Clear tracking trajectory
                trackPolyline?.remove()
                trackPolyline = null
                transitSegmentPolylines.forEach { it.remove() }
                transitSegmentPolylines.clear()
            }
            is TripState.Starting -> {
                binding.btnTracking.text = "Starting..."
                binding.btnTracking.isEnabled = false
            }
            is TripState.Tracking -> {
                binding.btnTracking.text = getString(R.string.stop_tracking)
                binding.btnTracking.isEnabled = true
                binding.chipGroupTransport.visibility = View.GONE
                binding.cardSearch.visibility = View.GONE
                binding.layoutAdCarousel.visibility = View.GONE
                // Show tracking info card
                binding.cardRouteInfo.visibility = View.VISIBLE

                // Show detecting transport mode
                binding.tvRouteType.text = "🔄 Detecting transport mode..."

                if (isNavigationMode) {
                    // Navigation mode
                    binding.tvCarbonSaved.text = "Traveled: 0.00 km"
                    val remainingKm = (NavigationManager.remainingDistance.value ?: 0f) / 1000f
                    binding.tvDuration.text = String.format("Remaining: %.2f km", remainingKm)
                } else {
                    // Pure track recording mode
                    binding.tvCarbonSaved.text = "Traveled: 0.00 km"
                    binding.tvDuration.text = "Recording GPS track in real time"
                }
            }
            is TripState.Stopping -> {
                binding.btnTracking.text = "Stopping..."
                binding.btnTracking.isEnabled = false
            }
            is TripState.Completed -> {
                binding.btnTracking.text = getString(R.string.start_tracking)
                binding.btnTracking.isEnabled = true
                binding.chipGroupTransport.visibility = View.VISIBLE
                binding.cardSearch.visibility = View.VISIBLE
                binding.cardRouteInfo.visibility = View.GONE
                hideTimer()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if there's an ongoing trip and restore UI state
        // Note: map may not be ready yet, so only restore non-map-related state here
        restoreTrackingStateIfNeeded()
    }

    /**
     * Restore tracking state (if there's an ongoing trip)
     */
    private fun restoreTrackingStateIfNeeded() {
        if (LocationManager.isTracking.value == true) {
            Log.d(TAG, "Restoring tracking state - trip is in progress")

            // Restore ViewModel state
            viewModel.restoreTrackingState()

            // Restore navigation mode flag
            if (NavigationManager.isNavigating.value == true) {
                isNavigationMode = true
            }

            // If map is ready, restore route display
            if (googleMap != null) {
                restoreRoutesOnMap()
            }

            // Restore timer (using saved start time)
            if (timerStartTime == 0L) {
                // If no timer start time, use current time (will reset timer, but better than nothing)
                timerStartTime = SystemClock.elapsedRealtime()
            }
            binding.tvTimer.visibility = View.VISIBLE
            timerHandler.post(timerRunnable)

            // Restore transport mode detection
            if (!transportModeDetector.isDetecting()) {
                transportModeDetector.startDetection()
            }

            isFollowingUser = true
        }
    }

    /**
     * Restore route display on map
     */
    private fun restoreRoutesOnMap() {
        Log.d(TAG, "Restoring routes on map, isNavigationMode=$isNavigationMode")

        if (isNavigationMode && NavigationManager.isNavigating.value == true) {
            // Navigation mode: draw traveled route and remaining route
            NavigationManager.traveledPoints.value?.let { points ->
                if (points.isNotEmpty()) {
                    Log.d(TAG, "Drawing traveled route with ${points.size} points")
                    drawTraveledRoute(points)
                }
            }
            NavigationManager.remainingPoints.value?.let { points ->
                if (points.isNotEmpty()) {
                    Log.d(TAG, "Drawing remaining route with ${points.size} points")
                    drawRemainingRoute(points)
                }
            }
        } else {
            // Pure track recording mode: draw track
            LocationManager.trackPoints.value?.let { points ->
                if (points.isNotEmpty()) {
                    Log.d(TAG, "Drawing track polyline with ${points.size} points")
                    drawTrackPolyline(points)
                }
            }
        }

        // If there's a destination marker, re-draw it
        destinationLatLng?.let { latLng ->
            updateDestinationMarker(latLng, destinationName)
        }

        // If there's an origin marker and it's not "My Location", re-draw it
        originLatLng?.let { latLng ->
            if (originName != MY_LOCATION_LABEL) {
                updateOriginMarker(latLng, originName)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear timer to prevent memory leaks
        timerHandler.removeCallbacks(timerRunnable)
        
        // Stop ad rotation
        adRunnable?.let { adHandler.removeCallbacks(it) }

        // Remove location update callbacks
        removeLocationUpdates()

        // Note: No longer automatically stopping tracking!
        // Foreground service will continue running, user needs to manually stop the trip
        // Tracking only stops when user explicitly clicks the stop button

        // Clear transport mode detector (paused when Activity is destroyed, but service continues tracking)
        if (this::transportModeDetector.isInitialized) {
            transportModeDetector.stopDetection()
            transportModeDetector.cleanup()
        }
    }
}
