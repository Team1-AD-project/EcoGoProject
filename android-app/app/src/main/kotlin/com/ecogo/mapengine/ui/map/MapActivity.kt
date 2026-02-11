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
 * 地图主页面
 * 实现 Google Maps 集成、行程追踪、路线推荐、地点搜索
 */
class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private val viewModel: MapViewModel by viewModels()

    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var transportModeDetector: HybridTransportModeDetector

    // 地图标记
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var routePolyline: Polyline? = null

    // 公交多段路线（每段不同颜色）
    private var transitSegmentPolylines: MutableList<Polyline> = mutableListOf()

    // 当前公交路线的步骤数据（导航模式下用于保持多色显示）
    private var currentTransitSteps: List<com.ecogo.mapengine.data.model.RouteStep>? = null

    // 标记是否正在处理路线选择（防止 observer 重复绘制）
    private var isHandlingRouteSelection = false

    // 实时轨迹
    private var trackPolyline: Polyline? = null
    private var isFollowingUser = true  // 是否跟随用户位置

    // 导航路线（已走/未走）
    private var traveledPolyline: Polyline? = null    // 已走过的路线（灰色）
    private var remainingPolyline: Polyline? = null   // 剩余路线（蓝色）
    private var isNavigationMode = false              // 是否在导航模式

    // 路线步骤适配器
    private val routeStepAdapter = RouteStepAdapter()

    // 路线选择适配器
    private val routeOptionAdapter = RouteOptionAdapter { selectedRoute ->
        onRouteSelected(selectedRoute)
    }

    // 起点和终点位置
    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null
    private var originName: String = "我的位置"
    private var destinationName: String = ""

    // 标记当前搜索的是起点还是终点
    private var isSearchingOrigin = false

    // 里程碑追踪（用于显示鼓励信息）
    private val milestones = listOf(1000f, 2000f, 3000f, 5000f, 10000f) // 单位：米
    private var reachedMilestones = mutableSetOf<Float>()

    // 导航记录相关
    private var navigationStartTime: Long = 0  // 导航开始时间
    private var detectedTransportMode: String? = null  // AI检测到的交通方式（主要方式）
    private var backendTripId: String? = null  // 后端真实 tripId（由 TripRepository.startTrip 返回）

    // 交通方式检测历史（累计整段行程所有检测结果，用于统计主要方式）
    private val modeDetectionHistory = mutableListOf<com.ecogo.mapengine.ml.TransportModeLabel>()
    private var lastMlConfidence: Float = 0f  // 最近一次 ML 置信度

    // 行程计时器
    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerStartTime = 0L
    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsed = SystemClock.elapsedRealtime() - timerStartTime
            val seconds = (elapsed / 1000) % 60
            val minutes = (elapsed / 1000 / 60) % 60
            val hours = elapsed / 1000 / 3600
            val timeStr = if (hours > 0)
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            else
                String.format("%02d:%02d", minutes, seconds)
            binding.tvTimer.text = getString(R.string.timer_format, timeStr)
            timerHandler.postDelayed(this, 1000)
        }
    }

    companion object {
        private const val TAG = "MapActivity"
        const val EXTRA_DEST_LAT = "extra_dest_lat"
        const val EXTRA_DEST_LNG = "extra_dest_lng"
        const val EXTRA_DEST_NAME = "extra_dest_name"
    }

    /** 读取本地 VIP 状态 */
    private fun isVipUser(): Boolean {
        val prefs = getSharedPreferences("EcoGoPrefs", MODE_PRIVATE)
        return prefs.getBoolean("is_vip", false)
    }

    /**
     * ML 标签 → transport_modes_dict 的值映射
     * 对应 MongoDB transport_modes_dict 集合中的 mode 字段
     */
    private fun mlLabelToDictMode(label: com.ecogo.mapengine.ml.TransportModeLabel): String {
        return when (label) {
            com.ecogo.mapengine.ml.TransportModeLabel.WALKING -> "walk"
            com.ecogo.mapengine.ml.TransportModeLabel.CYCLING -> "bike"
            com.ecogo.mapengine.ml.TransportModeLabel.BUS -> "bus"
            com.ecogo.mapengine.ml.TransportModeLabel.SUBWAY -> "subway"
            com.ecogo.mapengine.ml.TransportModeLabel.DRIVING -> "car"
            else -> "walk"
        }
    }

    /**
     * 根据 transport_modes_dict 判断是否为绿色出行
     * walk/bike/bus/subway → true，car → false
     */
    private fun isGreenMode(dictMode: String): Boolean {
        return dictMode != "car"
    }

    /**
     * 获取整段行程的主要交通方式（频率最高的 ML 标签）
     */
    private fun getDominantMode(): com.ecogo.mapengine.ml.TransportModeLabel {
        if (modeDetectionHistory.isEmpty()) {
            return com.ecogo.mapengine.ml.TransportModeLabel.WALKING
        }
        return modeDetectionHistory.groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key
            ?: com.ecogo.mapengine.ml.TransportModeLabel.WALKING
    }

    // 定位权限请求
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

    // 通知权限请求 (Android 13+)
    private val notificationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "需要通知权限来显示追踪状态", Toast.LENGTH_SHORT).show()
        }
    }

    // Places Autocomplete 启动器
    private val autocompleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        handleAutocompleteResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化 Places SDK
        initPlaces()

        // 初始化 Directions API
        DirectionsService.init(this)

        // 初始化定位客户端
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 初始化交通方式检测器（混合方案：优先 Snap to Roads，备用本地传感器）
        val apiKey = getGoogleMapsApiKey()
        transportModeDetector = HybridTransportModeDetector(
            context = this,
            googleMapsApiKey = apiKey
        )

        // 初始化地图
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupUI()
        observeViewModel()
        observeLocationManager()
        observeNavigationManager()
        observeTransportModeDetector()

        // 请求通知权限 (Android 13+)
        requestNotificationPermission()

        setupAdCarousel()
    }

    // ==================== 广告轮播逻辑 ====================
    private val adHandler = Handler(Looper.getMainLooper())
    private var adRunnable: Runnable? = null
    private var currentAdIndex = 0

    private fun setupAdCarousel() {
        // 获取广告数据
        // 获取广告数据
        lifecycleScope.launch {
            binding.layoutAdCarousel.visibility = View.GONE // Default to hidden
            try {
                // 0. Init TokenManager just in case
                com.ecogo.auth.TokenManager.init(this@MapActivity)

                // 1. Check cached VIP status first (fastest) - Custom Prefs AND TokenManager
                val prefs = getSharedPreferences("EcoGoPrefs", android.content.Context.MODE_PRIVATE)
                val isVipPref = prefs.getBoolean("is_vip", false)

                if (com.ecogo.auth.TokenManager.isVipActive() || isVipPref) {
                    Log.d(TAG, "User is VIP (cached/pref), hiding advertisement carousel")
                    return@launch
                }

                // 2. Double check with Repository (in case cache is stale)
                val repository = com.ecogo.EcoGoApplication.repository
                val profileResult = repository.getMobileUserProfile()
                
                val profile = profileResult.getOrNull()
                // Fail-safe: If we cannot fetch profile to verify status, do not show ads to potential VIPs
                // Or if user is VIP, hide ads.
                if (profile == null) {
                    Log.d(TAG, "Profile fetch failed, defaulting to hidden ads")
                    return@launch
                }

                val isVip = (profile?.vipInfo?.active == true) ||
                            (profile?.userInfo?.vip?.active == true) ||
                            (profile?.vipInfo?.plan != null) ||
                            (profile?.userInfo?.vip?.plan != null) ||
                            (profile?.userInfo?.isAdmin == true)

                if (isVip) {
                    Log.d(TAG, "User is VIP/Admin (network), hiding advertisement carousel")
                    // Update cache for next time
                    com.ecogo.auth.TokenManager.init(this@MapActivity)
                    if (profile != null) {
                        com.ecogo.auth.TokenManager.saveToken(
                            token = com.ecogo.auth.TokenManager.getToken() ?: "",
                            userId = profile.userInfo.userid,
                            username = profile.userInfo.nickname,
                            vipActive = true
                        )
                    }
                    binding.layoutAdCarousel.visibility = View.GONE // Ensure GONE
                    return@launch
                }

                // If not VIP, proceed to fetch ads
                val result = repository.getAdvertisements()

                val ads = result.getOrNull()?.filter {
                    it.position == "banner" && it.status == "Active"
                } ?: emptyList()

                if (ads.isNotEmpty() && !isVip) { // Double check !isVip
                    binding.layoutAdCarousel.visibility = View.VISIBLE
                    setupAdViewPager(ads)
                } else {
                    // 没有广告时隐藏轮播区
                    binding.layoutAdCarousel.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching ads", e)
                binding.layoutAdCarousel.visibility = View.GONE
            }
        }
    }

    private fun setupAdViewPager(ads: List<com.ecogo.data.Advertisement>) {
        val adapter = AdAdapter(ads) { ad ->
            // 点击广告跳转
            if (ad.linkUrl.isNotEmpty()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(ad.linkUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening ad link: ${ad.linkUrl}", e)
                    Toast.makeText(this, "无法打开链接", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.viewPagerAd.adapter = adapter
        
        // 绑定 TabLayout 指示器
        com.google.android.material.tabs.TabLayoutMediator(
            binding.tabLayoutAdIndicator, binding.viewPagerAd
        ) { _, _ -> }.attach()

        // 自动轮播逻辑
        adRunnable = Runnable {
            if (ads.size > 1) {
                currentAdIndex = (currentAdIndex + 1) % ads.size
                binding.viewPagerAd.setCurrentItem(currentAdIndex, true)
                adHandler.postDelayed(adRunnable!!, 5000) // 5秒切换
            }
        }
        
        // 注册页面切换回调，处理手动滑动与自动轮播的冲突
        binding.viewPagerAd.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentAdIndex = position
                // 重置计时器，避免手动滑动后立即自动切换
                adHandler.removeCallbacks(adRunnable!!)
                adHandler.postDelayed(adRunnable!!, 5000)
            }
        })

        // 开始轮播
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
            // 使用 Glide 加载图片 (假设项目已集成 Glide)
            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(ad.imageUrl)
                .placeholder(R.drawable.placeholder_image) // 需要确保有此资源或替换为其他默认图
                .centerCrop()
                .into(holder.imageView)

            holder.itemView.setOnClickListener { onItemClick(ad) }
        }

        override fun getItemCount(): Int = ads.size
    }

    /**
     * 请求通知权限
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
     * 初始化 Places SDK
     */
    private fun initPlaces() {
        try {
            if (!Places.isInitialized()) {
                // 从 AndroidManifest.xml 获取 API Key
                val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                val apiKey = appInfo.metaData?.getString("com.google.android.geo.API_KEY") ?: ""
                if (apiKey.isNotEmpty()) {
                    Places.initialize(applicationContext, apiKey)
                    Log.d(TAG, "Places SDK initialized with key: ${apiKey.take(10)}...")
                } else {
                    Log.e(TAG, "Google Maps API Key not found in manifest")
                    Toast.makeText(this, "API密钥未配置", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Places SDK: ${e.message}", e)
            Toast.makeText(this, "地点服务初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 设置 UI 事件监听
     */
    private fun setupUI() {
        // 初始化底部区域：显示广告占位(现改为轮播)，默认隐藏防闪烁
        // binding.layoutAdCarousel.visibility = View.GONE // Handled by XML
        binding.cardBottomPanel.visibility = View.GONE

        // 起点输入框点击
        binding.etOrigin.setOnClickListener {
            isSearchingOrigin = true
            launchPlaceAutocomplete()
        }

        // 终点输入框点击
        binding.etDestination.setOnClickListener {
            isSearchingOrigin = false
            launchPlaceAutocomplete()
        }

        // 交换起点终点按钮
        binding.btnSwap.setOnClickListener {
            swapOriginAndDestination()
        }

        // 交通方式选择监听器
        binding.chipGroupTransport.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            // 检查是否选择了目的地
            if (destinationLatLng == null) {
                Toast.makeText(this, "请先选择目的地", Toast.LENGTH_SHORT).show()
                binding.chipGroupTransport.clearCheck()
                binding.chipWalking.isChecked = true  // 重置为默认
                return@setOnCheckedStateChangeListener
            }

            // 根据选中的 Chip 确定交通方式
            val mode = when (checkedIds.first()) {
                R.id.chipDriving -> TransportMode.DRIVING
                R.id.chipTransit -> TransportMode.BUS
                R.id.chipCycling -> TransportMode.CYCLING
                R.id.chipWalking -> TransportMode.WALKING
                else -> TransportMode.WALKING
            }

            Log.d(TAG, "Transport mode selected: ${mode.displayName} (${mode.value})")

            // 调用 ViewModel 获取路线
            viewModel.fetchRouteByMode(mode)
        }

        // 初始化路线步骤 RecyclerView
        binding.rvRouteSteps.apply {
            adapter = routeStepAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@MapActivity)
        }

        // 初始化路线选择 RecyclerView（横向滚动）
        binding.rvRouteOptions.apply {
            adapter = routeOptionAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                this@MapActivity,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )
        }

        // 行程追踪按钮
        binding.btnTracking.setOnClickListener {
            when (viewModel.tripState.value) {
                is TripState.Idle, is TripState.Completed -> {
                    // 检查是否有路线
                    val hasRoute = !viewModel.routePoints.value.isNullOrEmpty()
                    if (!hasRoute) {
                        // 提示用户先获取路线
                        Toast.makeText(
                            this,
                            "提示：请先点击\"低碳路线\"或\"平衡路线\"获取导航路线",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    startLocationTracking()
                    viewModel.startTracking()
                }
                is TripState.Tracking -> {
                    stopLocationTracking()
                    viewModel.stopTracking()
                }
                else -> { /* 忽略其他状态 */ }
            }
        }

        // 定位按钮
        binding.fabMyLocation.setOnClickListener {
            isFollowingUser = true
            moveToCurrentLocation()
            // 重置起点为当前位置
            resetOriginToMyLocation()
        }
    }

    /**
     * 启动位置追踪服务
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

        // 检查是否有路线，如果有则进入导航模式
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

        // 保存当前公交路线步骤数据
        val route = viewModel.recommendedRoute.value
        val steps = route?.route_steps
        val hasTransitSteps = steps?.any { it.travel_mode == "TRANSIT" } == true
        val hasStepPolylines = steps?.any { !it.polyline_points.isNullOrEmpty() } == true

        if (hasTransitSteps && hasStepPolylines) {
            // 公交路线（有步骤polyline）：清除后重绘多色路线
            currentTransitSteps = steps
            clearAllRoutePolylines()
            drawTransitRoute(steps!!)
            Log.d(TAG, "Transit navigation: drew ${steps.size} colored segments (with step polylines)")
        } else if (hasTransitSteps && !steps.isNullOrEmpty() && !routePoints.isNullOrEmpty()) {
            // 公交路线（无步骤polyline）：按距离比例切割overview路线
            currentTransitSteps = steps
            clearAllRoutePolylines()
            drawTransitRouteFallback(routePoints, steps)
            Log.d(TAG, "Transit navigation: drew ${steps.size} colored segments (fallback)")
        } else {
            // 非公交路线：清除所有
            currentTransitSteps = null
            clearAllRoutePolylines()
        }

        // 重置里程碑追踪
        reachedMilestones.clear()

        // 记录导航开始时间
        navigationStartTime = System.currentTimeMillis()
        detectedTransportMode = null
        backendTripId = null
        modeDetectionHistory.clear()
        lastMlConfidence = 0f
        hasTriggeredArrival = false

        isFollowingUser = true

        // 移除旧的位置更新回调
        removeLocationUpdates()

        // 启动混合交通方式检测（优先 Snap to Roads，备用本地传感器）
        transportModeDetector.startDetection()
        Log.d(TAG, "Hybrid transport mode detection started (Snap to Roads preferred)")

        // 请求 GPS 位置更新
        @SuppressLint("MissingPermission")
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 2000  // 2秒更新一次
            fastestInterval = 1000
            smallestDisplacement = 5f  // 5米触发一次更新
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        Log.d(TAG, "GPS location updates requested (2s interval, 5m displacement)")

        // 启动计时器
        startTimer()

        // 调用后端 API 开始行程，获取真实 tripId
        startTripOnBackend()
    }

    /**
     * 启动行程计时器
     */
    private fun startTimer() {
        timerStartTime = SystemClock.elapsedRealtime()
        binding.tvTimer.visibility = View.VISIBLE
        binding.tvTimer.text = getString(R.string.timer_format, "00:00")
        timerHandler.post(timerRunnable)
    }

    /**
     * 停止行程计时器
     */
    private fun stopTimer() {
        timerHandler.removeCallbacks(timerRunnable)
    }

    /**
     * 隐藏计时器
     */
    private fun hideTimer() {
        timerHandler.removeCallbacks(timerRunnable)
        binding.tvTimer.visibility = View.GONE
    }

    /**
     * 停止位置追踪服务
     */
    private fun stopLocationTracking() {
        Log.d(TAG, "Stopping location tracking service")
        val intent = Intent(this, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
        }
        startService(intent)

        // 停止计时器（保留显示最终用时）
        stopTimer()

        // 保存导航历史记录（如果有有效数据）
        saveNavigationHistory()

        // 调用后端 API 完成行程（将 tracking→completed）
        completeTripOnBackend()

        // 停止导航
        if (isNavigationMode) {
            NavigationManager.stopNavigation()
            isNavigationMode = false
            currentTransitSteps = null

            // 清除导航路线
            transitSegmentPolylines.forEach { it.remove() }
            transitSegmentPolylines.clear()
            traveledPolyline?.remove()
            traveledPolyline = null
            remainingPolyline?.remove()
            remainingPolyline = null
        }

        // 移除 GPS 位置更新回调
        removeLocationUpdates()
        Log.d(TAG, "GPS location updates removed")

        // 停止交通方式检测
        transportModeDetector.stopDetection()
        Log.d(TAG, "Transport mode detection stopped")
    }

    /**
     * 保存导航历史记录
     */
    private fun saveNavigationHistory() {
        // 检查是否有有效的导航数据
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

        // 获取路线数据
        val routePoints = viewModel.routePoints.value ?: emptyList()
        val trackPoints = if (isNavigationMode) {
            NavigationManager.traveledPoints.value ?: emptyList()
        } else {
            LocationManager.trackPoints.value ?: emptyList()
        }

        // 如果没有轨迹点，跳过保存
        if (trackPoints.isEmpty()) {
            Log.w(TAG, "No track points recorded, skipping history save")
            return
        }

        // 获取距离数据
        val totalDistance = viewModel.routePoints.value?.let { points ->
            // 计算路线总距离（如果有规划路线）
            viewModel.recommendedRoute.value?.total_distance?.times(1000) ?: 0.0
        } ?: 0.0

        val traveledDistance = if (isNavigationMode) {
            NavigationManager.traveledDistance.value?.toDouble() ?: 0.0
        } else {
            LocationManager.totalDistance.value?.toDouble() ?: 0.0
        }

        // 获取交通方式
        val transportMode = viewModel.selectedTransportMode.value?.value ?: "walking"

        // 获取环保数据（优先使用本地检测结果）
        val dominantLabel = getDominantMode()
        val dominantDictMode = mlLabelToDictMode(dominantLabel)
        val carbonResult = viewModel.carbonResult.value
        val totalCarbon = carbonResult?.total_carbon_emission ?: 0.0
        val carbonSaved = carbonResult?.carbon_saved
            ?: (calculateRealTimeCarbonSaved(traveledDistance.toFloat()) / 1000.0) // 转为 kg
        val isGreenTrip = isGreenMode(dominantDictMode)
        val greenPoints = carbonResult?.green_points ?: 0

        // 获取路线类型
        val routeType = viewModel.recommendedRoute.value?.route_type

        // 在后台线程保存数据
        lifecycleScope.launch {
            try {
                val repository = NavigationHistoryRepository.getInstance()
                val historyId = repository.saveNavigationHistory(
                    tripId = null, // 如果有后端trip_id可以传入
                    userId = null, // 如果有用户系统可以传入用户ID
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

                // 可以在这里显示保存成功的提示（可选）
                // runOnUiThread {
                //     Toast.makeText(this@MapActivity, "行程已保存", Toast.LENGTH_SHORT).show()
                // }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save navigation history", e)
            }
        }
    }

    /**
     * 调用 TripRepository.startTrip() 在后端创建行程记录，获取真实 tripId
     */
    private fun startTripOnBackend() {
        val startLocation = viewModel.currentLocation.value
        if (startLocation == null) {
            Log.w(TAG, "No current location, skipping startTrip API call")
            return
        }

        lifecycleScope.launch {
            try {
                val tripRepo = TripRepository.getInstance()
                val result = tripRepo.startTrip(
                    startLat = startLocation.latitude,
                    startLng = startLocation.longitude,
                    startPlaceName = originName.ifEmpty { "起点" },
                    startAddress = originName.ifEmpty { "未知地址" }
                )

                result.fold(
                    onSuccess = { tripId ->
                        backendTripId = tripId
                        Log.d(TAG, "Trip started on backend: tripId=$tripId")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to start trip on backend: ${error.message}", error)
                        // 后端失败不影响本地追踪
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error starting trip on backend", e)
            }
        }
    }

    /**
     * 调用 TripRepository.completeTrip() 将后端行程状态从 tracking→completed
     * 包含：主要交通方式、is_green_trip、全程 polyline_points、ML 置信度
     */
    private fun completeTripOnBackend() {
        // 优先使用后端真实 tripId，回退到 ViewModel 的 tripId
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

        // 收集全程轨迹点（polyline_points）
        val trackPoints: List<LatLng> = if (isNavigationMode) {
            NavigationManager.traveledPoints.value ?: emptyList()
        } else {
            LocationManager.trackPoints.value ?: emptyList()
        }

        if (trackPoints.isEmpty()) {
            Log.w(TAG, "No track points, skipping completeTrip API call")
            return
        }

        // 计算行驶距离（米）
        val distanceMeters = if (isNavigationMode) {
            NavigationManager.traveledDistance.value?.toDouble() ?: 0.0
        } else {
            LocationManager.totalDistance.value?.toDouble() ?: 0.0
        }

        // 获取主要交通方式（频率最高，映射到 dict 值）
        val dominantLabel = getDominantMode()
        val dominantDictMode = mlLabelToDictMode(dominantLabel)

        // 基于 transport_modes_dict 判断是否绿色出行
        val greenTrip = isGreenMode(dominantDictMode)

        // 碳排放减少量（克）
        val carbonSavedGrams = calculateRealTimeCarbonSaved(distanceMeters.toFloat()).toLong()

        // ML 置信度
        val confidence = if (lastMlConfidence > 0f) lastMlConfidence.toDouble() else null

        Log.d(TAG, "Completing trip on backend: tripId=$tripId, mode=$dominantDictMode, " +
                "isGreen=$greenTrip, points=${trackPoints.size}, distance=${distanceMeters}m, " +
                "carbonSaved=${carbonSavedGrams}g, confidence=$confidence")

        lifecycleScope.launch {
            try {
                val tripRepo = TripRepository.getInstance()
                val result = tripRepo.completeTrip(
                    tripId = tripId,
                    endLat = endLocation.latitude,
                    endLng = endLocation.longitude,
                    endPlaceName = destinationName.ifEmpty { "终点" },
                    endAddress = destinationName.ifEmpty { "未知地址" },
                    distance = distanceMeters,
                    trackPoints = trackPoints,
                    transportMode = dominantDictMode,
                    detectedMode = dominantDictMode,
                    mlConfidence = confidence,
                    carbonSaved = carbonSavedGrams,
                    isGreenTrip = greenTrip
                )

                result.fold(
                    onSuccess = { response ->
                        backendTripId = null
                        Log.d(TAG, "Trip completed on backend: ${response.tripId}, status=${response.status}")
                        runOnUiThread {
                            Toast.makeText(this@MapActivity, "行程已同步到服务器", Toast.LENGTH_SHORT).show()
                        }
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
     * 观察 LocationManager 的位置更新
     */
    private fun observeLocationManager() {
        // 观察当前位置
        LocationManager.currentLocation.observe(this) { latLng ->
            // 更新 ViewModel
            viewModel.updateCurrentLocation(latLng)

            // 如果正在追踪且开启了跟随模式，移动相机
            if (LocationManager.isTracking.value == true && isFollowingUser) {
                googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
            }

            // 更新交通方式检测器的位置（用于 GPS 速度）
            if (LocationManager.isTracking.value == true) {
                val location = android.location.Location("gps").apply {
                    latitude = latLng.latitude
                    longitude = latLng.longitude
                    time = System.currentTimeMillis()
                    // 注意：这里的 speed 需要从实际的 Location 对象获取
                    // 当前使用默认值 0，实际应该从 LocationTrackingService 获取
                }
                lifecycleScope.launch {
                    transportModeDetector.updateLocation(location)
                }
                Log.d(TAG, "Location updated for detector: lat=${latLng.latitude}, lng=${latLng.longitude}")
            }

            // 导航模式下检查是否接近目的地（备用检查，防止 currentRouteIndex observer 未触发）
            if (isNavigationMode && NavigationManager.isNavigating.value == true) {
                if (NavigationManager.hasReachedDestination()) {
                    onReachedDestination()
                }
            }
        }

        // 观察轨迹点（仅在非导航模式下绘制）
        LocationManager.trackPoints.observe(this) { points ->
            if (points.isNotEmpty() && !isNavigationMode) {
                drawTrackPolyline(points)
            }
        }

        // 观察总距离
        LocationManager.totalDistance.observe(this) { distance ->
            if (LocationManager.isTracking.value == true && !isNavigationMode) {
                updateTrackingInfo(distance)
            }
        }
    }

    /**
     * 观察 NavigationManager 的导航状态
     */
    private fun observeNavigationManager() {
        // 观察已走过的路线
        NavigationManager.traveledPoints.observe(this) { points ->
            if (isNavigationMode && points.isNotEmpty()) {
                drawTraveledRoute(points)
            }
        }

        // 观察剩余路线
        NavigationManager.remainingPoints.observe(this) { points ->
            if (isNavigationMode && points.isNotEmpty()) {
                // 公交路线导航时，多色分段已经显示了路线，不需要单独画剩余路线
                if (currentTransitSteps != null) {
                    return@observe
                }
                drawRemainingRoute(points)
            }
        }

        // 观察已行进距离
        NavigationManager.traveledDistance.observe(this) { distance ->
            if (isNavigationMode) {
                updateNavigationInfo(distance)
            }
        }

        // 观察是否到达目的地
        NavigationManager.currentRouteIndex.observe(this) { _ ->
            if (NavigationManager.hasReachedDestination()) {
                onReachedDestination()
            }
        }
    }

    /**
     * 获取 Google Maps API Key
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
     * GPS 位置回调（用于传递给混合检测器）
     */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                // 传递 GPS 位置给混合检测器
                lifecycleScope.launch {
                    transportModeDetector.updateLocation(location)
                }
                // 更新当前位置（用于 ViewModel）
                val latLng = LatLng(location.latitude, location.longitude)
                viewModel.updateCurrentLocation(latLng)
            }
        }
    }

    /**
     * 移除位置更新
     */
    private fun removeLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /**
     * 观察交通方式检测器
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
     * 处理检测到的交通方式
     */
    private fun onTransportModeDetected(prediction: com.ecogo.mapengine.ml.TransportModePrediction) {
        if (!LocationManager.isTracking.value!!) return

        // 累积检测历史（用于统计主要交通方式）
        modeDetectionHistory.add(prediction.mode)
        lastMlConfidence = prediction.confidence

        // 使用频率最高的交通方式作为 detected_mode（映射到 dict 值）
        val dominantLabel = getDominantMode()
        detectedTransportMode = mlLabelToDictMode(dominantLabel)

        val modeIcon = when (prediction.mode) {
            com.ecogo.mapengine.ml.TransportModeLabel.WALKING -> "🚶"
            com.ecogo.mapengine.ml.TransportModeLabel.CYCLING -> "🚴"
            com.ecogo.mapengine.ml.TransportModeLabel.BUS -> "🚌"
            com.ecogo.mapengine.ml.TransportModeLabel.SUBWAY -> "🚇"
            com.ecogo.mapengine.ml.TransportModeLabel.DRIVING -> "🚗"
            else -> "❓"
        }

        val modeText = when (prediction.mode) {
            com.ecogo.mapengine.ml.TransportModeLabel.WALKING -> "步行"
            com.ecogo.mapengine.ml.TransportModeLabel.CYCLING -> "骑行"
            com.ecogo.mapengine.ml.TransportModeLabel.BUS -> "公交"
            com.ecogo.mapengine.ml.TransportModeLabel.SUBWAY -> "地铁"
            com.ecogo.mapengine.ml.TransportModeLabel.DRIVING -> "驾车"
            else -> "未知"
        }

        val confidencePercent = (prediction.confidence * 100).toInt()

        // 更新 UI 显示检测到的交通方式（在顶部显著位置）
        runOnUiThread {
            if (binding.cardRouteInfo.visibility == View.VISIBLE) {
                // 在路线类型位置显示当前交通方式
                if (isNavigationMode) {
                    binding.tvRouteType.text = "$modeIcon 当前交通: $modeText ($confidencePercent%)"
                } else {
                    binding.tvRouteType.text = "$modeIcon 检测到: $modeText ($confidencePercent%)"
                }
            }
        }

        Log.d(TAG, "检测到交通方式: $modeText, 置信度: ${prediction.confidence}")
    }

    /**
     * 检测是否运行在模拟器上
     * 检查多个设备属性以提高可靠性
     */
    private fun isRunningOnEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT)
    }

    /**
     * 绘制已走过的路线（灰色）
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
     * 绘制剩余路线（蓝色）
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
     * 更新导航信息显示
     */
    private fun updateNavigationInfo(traveledMeters: Float) {
        val traveledKm = traveledMeters / 1000f
        val remainingMeters = NavigationManager.remainingDistance.value ?: 0f
        val remainingKm = remainingMeters / 1000f

        if (binding.cardRouteInfo.visibility == View.VISIBLE) {
            // 获取实时碳排放信息和鼓励消息
            val encouragementMessage = generateEncouragementMessage(traveledMeters)
            binding.tvCarbonSaved.text = encouragementMessage
            binding.tvDuration.text = String.format("剩余: %.2f 公里", remainingKm)

            // 检查是否到达里程碑
            checkMilestones(traveledMeters)
        }
    }

    /**
     * 计算实时碳排放减少量（单位：克）
     */
    private fun calculateRealTimeCarbonSaved(distanceMeters: Float): Double {
        val distanceKm = distanceMeters / 1000.0
        val mode = viewModel.selectedTransportMode.value

        // 碳排放因子 (kg CO2 / km)
        val emissionFactor = when (mode) {
            TransportMode.WALKING, TransportMode.CYCLING -> 0.0
            TransportMode.BUS, TransportMode.SUBWAY -> 0.05
            else -> 0.15  // DRIVING 或其他
        }

        val currentModeCarbon = distanceKm * emissionFactor
        val drivingCarbon = distanceKm * 0.15  // 与驾车对比
        val carbonSaved = (drivingCarbon - currentModeCarbon) * 1000  // 转为克

        return carbonSaved.coerceAtLeast(0.0)
    }

    /**
     * 生成鼓励消息
     */
    private fun generateEncouragementMessage(distanceMeters: Float): String {
        val mode = viewModel.selectedTransportMode.value
        val carbonSavedGrams = calculateRealTimeCarbonSaved(distanceMeters)

        return when (mode) {
            TransportMode.WALKING, TransportMode.CYCLING -> {
                // 步行/骑行：显示减碳量和鼓励
                if (carbonSavedGrams >= 1) {
                    String.format("已减碳 %.0f g | 继续加油 💪", carbonSavedGrams)
                } else {
                    "绿色出行 | 继续加油 💪"
                }
            }
            TransportMode.BUS, TransportMode.SUBWAY -> {
                // 公交/地铁：显示绿色出行进行中
                if (carbonSavedGrams >= 1) {
                    String.format("绿色出行进行中 🚌 | 已减碳 %.0f g", carbonSavedGrams)
                } else {
                    "绿色出行进行中 🚌"
                }
            }
            else -> {
                // 驾车或其他：只显示距离
                String.format("已行进: %.2f 公里", distanceMeters / 1000f)
            }
        }
    }

    /**
     * 检查并显示里程碑
     */
    private fun checkMilestones(distanceMeters: Float) {
        for (milestone in milestones) {
            if (distanceMeters >= milestone && !reachedMilestones.contains(milestone)) {
                reachedMilestones.add(milestone)
                showMilestoneToast(milestone)
                break  // 每次只显示一个里程碑
            }
        }
    }

    /**
     * 显示里程碑Toast
     */
    private fun showMilestoneToast(milestoneMeters: Float) {
        val mode = viewModel.selectedTransportMode.value
        val carbonSavedGrams = calculateRealTimeCarbonSaved(milestoneMeters)

        val message = when (mode) {
            TransportMode.WALKING -> {
                String.format("恭喜！您已步行 %.0f 米，减碳 %.0f g 🎉", milestoneMeters, carbonSavedGrams)
            }
            TransportMode.CYCLING -> {
                String.format("恭喜！您已骑行 %.0f 米，减碳 %.0f g 🚴", milestoneMeters, carbonSavedGrams)
            }
            TransportMode.BUS, TransportMode.SUBWAY -> {
                String.format("恭喜！您已出行 %.0f 米，减碳 %.0f g 🌱", milestoneMeters, carbonSavedGrams)
            }
            else -> {
                String.format("恭喜！您已出行 %.0f 米", milestoneMeters)
            }
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // 防止 onReachedDestination 重复触发
    private var hasTriggeredArrival = false

    /**
     * 到达目的地
     */
    private fun onReachedDestination() {
        if (hasTriggeredArrival) return
        hasTriggeredArrival = true

        Toast.makeText(this, "您已到达目的地！", Toast.LENGTH_LONG).show()
        // 自动停止行程
        stopLocationTracking()
        viewModel.stopTracking()
    }

    /**
     * 绘制实时轨迹
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
     * 更新追踪信息显示
     */
    private fun updateTrackingInfo(distanceMeters: Float) {
        val distanceKm = distanceMeters / 1000f
        // 可以在路线信息卡片显示实时距离
        if (binding.cardRouteInfo.visibility == View.VISIBLE) {
            // 使用与导航相同的鼓励消息
            val encouragementMessage = generateEncouragementMessage(distanceMeters)
            binding.tvCarbonSaved.text = encouragementMessage

            // 检查是否到达里程碑
            checkMilestones(distanceMeters)
        }
    }

    /**
     * 启动 Places Autocomplete
     */
    private fun launchPlaceAutocomplete() {
        try {
            // 确保 Places SDK 已初始化
            if (!Places.isInitialized()) {
                initPlaces()
                if (!Places.isInitialized()) {
                    Log.e(TAG, "Places SDK failed to initialize")
                    Toast.makeText(this, "地点搜索服务未初始化，请检查API密钥配置", Toast.LENGTH_LONG).show()
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
            Toast.makeText(this, "搜索服务暂不可用: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 处理 Autocomplete 返回结果
     */
    private fun handleAutocompleteResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                result.data?.let { data ->
                    val place = Autocomplete.getPlaceFromIntent(data)
                    val latLng = place.latLng

                    if (latLng != null) {
                        if (isSearchingOrigin) {
                            // 设置起点
                            originLatLng = latLng
                            originName = place.name ?: place.address ?: "起点"
                            binding.etOrigin.setText(originName)
                            updateOriginMarker(latLng, originName)
                            viewModel.setOrigin(latLng)  // 使用 setOrigin 而不是 updateCurrentLocation

                            // 如果终点已设置，自动获取默认路线（驾车）
                            if (destinationLatLng != null) {
                                binding.cardTransportModes.visibility = View.VISIBLE
                                binding.chipDriving.isChecked = true
                                viewModel.fetchRouteByMode(TransportMode.DRIVING)
                            }
                        } else {
                            // 设置终点
                            destinationLatLng = latLng
                            destinationName = place.name ?: place.address ?: "目的地"
                            binding.etDestination.setText(destinationName)
                            updateDestinationMarker(latLng, destinationName)
                            viewModel.setDestination(latLng)

                            // 显示交通方式选择卡片
                            binding.cardTransportModes.visibility = View.VISIBLE

                            // 自动获取默认路线（驾车）
                            if (originLatLng != null || viewModel.currentLocation.value != null) {
                                binding.chipDriving.isChecked = true
                                viewModel.fetchRouteByMode(TransportMode.DRIVING)
                            }
                        }

                        // 更新开始按钮可见性
                        updateStartButtonVisibility()

                        // 移动相机到选择的位置
                        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                        // 如果起点和终点都已设置，调整相机显示两点
                        fitBoundsIfReady()
                    }
                }
            }
            AutocompleteActivity.RESULT_ERROR -> {
                result.data?.let { data ->
                    val status = Autocomplete.getStatusFromIntent(data)
                    Log.e(TAG, "Autocomplete error: ${status.statusMessage}")
                    Toast.makeText(this, "搜索出错: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
                }
            }
            Activity.RESULT_CANCELED -> {
                Log.d(TAG, "Autocomplete canceled")
            }
        }
    }

    /**
     * 交换起点和终点
     */
    private fun swapOriginAndDestination() {
        // 交换位置
        val tempLatLng = originLatLng
        val tempName = originName

        originLatLng = destinationLatLng
        originName = destinationName

        destinationLatLng = tempLatLng
        destinationName = tempName

        // 更新 UI
        binding.etOrigin.setText(if (originLatLng != null) originName else "我的位置")
        binding.etDestination.setText(destinationName)

        // 更新标记
        originLatLng?.let {
            updateOriginMarker(it, originName)
            viewModel.setOrigin(it)  // 交换后更新起点
        }
        destinationLatLng?.let {
            updateDestinationMarker(it, destinationName)
            viewModel.setDestination(it)
        }

        // 清除路线
        clearAllRoutePolylines()
        binding.cardRouteInfo.visibility = View.GONE

        // 自动获取默认驾车路线（如果起点和终点都已设置）
        if (originLatLng != null && destinationLatLng != null) {
            binding.chipDriving.isChecked = true
            viewModel.fetchRouteByMode(TransportMode.DRIVING)
        }

        // 更新开始按钮可见性
        updateStartButtonVisibility()
    }

    /**
     * 重置起点为当前位置
     */
    @SuppressLint("MissingPermission")
    private fun resetOriginToMyLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                originLatLng = latLng
                originName = "我的位置"
                binding.etOrigin.setText(originName)
                originMarker?.remove()
                originMarker = null
                viewModel.setOrigin(latLng)  // 重置起点为当前位置
                updateStartButtonVisibility()  // 更新开始按钮可见性
            }
        }
    }

    /**
     * 如果起点和终点都设置了，调整相机显示两点
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
     * 更新开始行程按钮的可见性
     * 只有当起点和终点都设置后才显示按钮，同时隐藏广告占位
     */
    private fun updateStartButtonVisibility() {
        val hasOrigin = originLatLng != null || viewModel.currentLocation.value != null
        val hasDestination = destinationLatLng != null

        if (hasOrigin && hasDestination) {
            // 选择了起点和终点：隐藏广告，显示按钮
            binding.layoutAdCarousel.visibility = View.GONE
            binding.cardBottomPanel.visibility = View.VISIBLE
        } else {
            // 未选择完：显示广告，隐藏按钮
            binding.layoutAdCarousel.visibility = View.VISIBLE
            binding.cardBottomPanel.visibility = View.GONE
        }
    }

    /**
     * 观察 ViewModel 数据变化
     */
    private fun observeViewModel() {
        // 观察当前位置
        viewModel.currentLocation.observe(this) { location ->
            // 如果起点是"我的位置"，更新起点
            if (originName == "我的位置" && originLatLng == null) {
                originLatLng = location
            }
        }

        // 观察目的地
        viewModel.destination.observe(this) { destination ->
            destination?.let {
                destinationLatLng = it
            }
        }

        // 观察行程状态
        viewModel.tripState.observe(this) { state ->
            updateTrackingUI(state)
        }

        // 观察推荐路线
        viewModel.recommendedRoute.observe(this) { route ->
            route?.let { updateRouteInfo(it) }
        }

        // 观察路线点
        viewModel.routePoints.observe(this) { points ->
            // 如果正在处理路线选择，跳过（避免重复绘制）
            if (isHandlingRouteSelection) {
                isHandlingRouteSelection = false
                Log.d(TAG, "routePoints observer: skipped (handling route selection)")
                return@observe
            }

            // 如果是公交模式且有步骤数据，使用多色绘制
            val route = viewModel.recommendedRoute.value
            val steps = route?.route_steps
            val hasTransitSteps = steps?.any { it.travel_mode == "TRANSIT" } == true
            val hasStepPolylines = steps?.any { !it.polyline_points.isNullOrEmpty() } == true

            Log.d(TAG, "routePoints observer: points=${points.size}, hasTransitSteps=$hasTransitSteps, hasStepPolylines=$hasStepPolylines, stepsCount=${steps?.size ?: 0}")

            if (hasTransitSteps && hasStepPolylines) {
                // 优先使用步骤级别的 polyline 绘制多色路线
                drawTransitRoute(steps!!)
            } else if (hasTransitSteps && !steps.isNullOrEmpty()) {
                // 步骤有 TRANSIT 但没有 polyline_points：按步骤距离比例切割 overview 路线并着色
                Log.d(TAG, "routePoints observer: fallback - splitting overview polyline by step distances")
                drawTransitRouteFallback(points, steps)
            } else {
                drawRoute(points)
            }
        }

        // 观察碳足迹结果
        viewModel.carbonResult.observe(this) { result ->
            result?.let {
                val carbonSavedStr = String.format("%.2f", it.carbon_saved)

                // 记录绿色出行统计（如果有减碳）
                if (it.carbon_saved > 0) {
                    com.ecogo.mapengine.util.GreenTravelStats.recordGreenTrip(this, it.carbon_saved)
                }

                // 显示完成消息
                val message = if (it.is_green_trip) {
                    "🎉 绿色出行完成！减碳 $carbonSavedStr kg，获得 ${it.green_points} 积分"
                } else {
                    "行程完成，碳排放 $carbonSavedStr kg"
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()

                // 更新累计统计显示
                binding.tvCumulativeImpact.text = com.ecogo.mapengine.util.GreenTravelStats.formatWeeklyImpact(this)
            }
        }

        // 观察加载状态
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // 观察错误消息
        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        // 观察成功消息
        viewModel.successMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccessMessage()
            }
        }
    }

    /**
     * 地图准备就绪回调
     */
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // 配置地图
        map.apply {
            uiSettings.isZoomControlsEnabled = false
            uiSettings.isCompassEnabled = true
            uiSettings.isMapToolbarEnabled = false

            // 地图点击也可以设置目的地
            setOnMapClickListener { latLng ->
                // 如果正在追踪，禁止修改目的地
                if (viewModel.tripState.value is TripState.Tracking) {
                    return@setOnMapClickListener
                }

                // 显示确认对话框
                androidx.appcompat.app.AlertDialog.Builder(this@MapActivity)
                    .setTitle("设置目的地")
                    .setMessage("是否将此位置设置为目的地？")
                    .setPositiveButton("确定") { dialog, _ ->
                        destinationLatLng = latLng
                        destinationName = "地图上的位置"
                        binding.etDestination.setText(destinationName)
                        updateDestinationMarker(latLng, destinationName)
                        viewModel.setDestination(latLng)

                        // 显示交通方式选择卡片
                        binding.cardTransportModes.visibility = View.VISIBLE

                        // 自动获取默认路线（驾车）
                        if (originLatLng != null || viewModel.currentLocation.value != null) {
                            binding.chipDriving.isChecked = true
                            viewModel.fetchRouteByMode(TransportMode.DRIVING)
                        }

                        // 更新开始按钮可见性
                        updateStartButtonVisibility()

                        fitBoundsIfReady()
                        dialog.dismiss()
                    }
                    .setNegativeButton("取消") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }

            // 长按清除目的地
            setOnMapLongClickListener {
                if (viewModel.tripState.value !is TripState.Tracking) {
                    clearDestination()
                }
            }

            // 地图移动时停止跟随
            setOnCameraMoveStartedListener { reason ->
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    isFollowingUser = false
                }
            }
        }

        // 请求定位权限
        checkLocationPermission()

        // 如果有正在进行的行程，恢复路线显示
        if (LocationManager.isTracking.value == true) {
            Log.d(TAG, "Map ready - restoring routes for ongoing trip")
            restoreRoutesOnMap()
        }

        // 检查是否从活动详情跳转过来，自动设置目的地
        handleActivityDestination()
    }

    /**
     * 从 Intent extras 中读取活动目的地坐标，自动设置到地图
     */
    private fun handleActivityDestination() {
        val lat = intent.getDoubleExtra(EXTRA_DEST_LAT, Double.NaN)
        val lng = intent.getDoubleExtra(EXTRA_DEST_LNG, Double.NaN)
        if (lat.isNaN() || lng.isNaN()) return

        val name = intent.getStringExtra(EXTRA_DEST_NAME) ?: "活动地点"
        val latLng = LatLng(lat, lng)

        destinationLatLng = latLng
        destinationName = name
        binding.etDestination.setText(name)
        updateDestinationMarker(latLng, name)
        viewModel.setDestination(latLng)

        // 显示交通方式选择卡片
        binding.cardTransportModes.visibility = View.VISIBLE

        // 移动相机到目的地
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

        Log.d(TAG, "Activity destination set: $name ($lat, $lng)")
    }

    /**
     * 清除目的地
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
        // 隐藏按钮；VIP 用户不显示广告
        binding.cardBottomPanel.visibility = View.GONE
        binding.layoutAdCarousel.visibility = View.VISIBLE
        viewModel.clearDestination()
    }

    /**
     * 检查定位权限
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
     * 启用我的位置图层
     */
    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        googleMap?.isMyLocationEnabled = true
        googleMap?.uiSettings?.isMyLocationButtonEnabled = false

        // 获取当前位置
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                originLatLng = latLng
                viewModel.updateCurrentLocation(latLng)
                moveToCurrentLocation()
                updateStartButtonVisibility()  // 更新开始按钮可见性
            }
        }
    }

    /**
     * 移动相机到当前位置
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
     * 更新起点标记
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
     * 更新目的地标记
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
     * 绘制路线（根据交通方式使用不同颜色和样式）
     */
    private fun drawRoute(points: List<LatLng>) {
        // 清除之前的所有路线
        clearAllRoutePolylines()

        if (points.isEmpty()) return

        // 根据当前交通方式选择颜色和样式
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

        // 步行使用虚线
        if (mode == TransportMode.WALKING) {
            polylineOptions.pattern(listOf(Dot(), Gap(10f)))
        }

        Log.d(TAG, "drawRoute: mode=${mode?.displayName}, points=${points.size}, color=#${Integer.toHexString(color)}")

        routePolyline = googleMap?.addPolyline(polylineOptions)

        // 调整相机显示完整路线
        if (points.size >= 2) {
            val boundsBuilder = LatLngBounds.Builder()
            points.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        }
    }

    /**
     * 绘制公交/地铁多段路线（每段交通方式不同颜色）
     *
     * @param steps 路线步骤列表（包含 polyline_points 和 travel_mode）
     */
    private fun drawTransitRoute(steps: List<com.ecogo.mapengine.data.model.RouteStep>) {
        // 清除之前的所有路线
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

            // 根据交通方式和车辆类型选择颜色
            val color = getColorForTransitStep(step)

            // 步行段用虚线，其他用实线
            val isWalking = step.travel_mode == "WALKING"

            val polylineOptions = PolylineOptions()
                .addAll(stepPoints)
                .width(if (isWalking) 8f else 14f)
                .color(color)
                .geodesic(true)
                .jointType(JointType.ROUND)
                .startCap(RoundCap())
                .endCap(RoundCap())

            // 步行段使用虚线样式
            if (isWalking) {
                polylineOptions.pattern(listOf(Dot(), Gap(10f)))
            }

            googleMap?.addPolyline(polylineOptions)?.let {
                transitSegmentPolylines.add(it)
            }
        }

        Log.d(TAG, "drawTransitRoute: drew $segmentCount colored segments, ${allPoints.size} total points")

        // 调整相机显示完整路线
        if (allPoints.size >= 2) {
            val boundsBuilder = LatLngBounds.Builder()
            allPoints.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        }
    }

    /**
     * 回退方案：当步骤没有 polyline_points 时，按距离比例切割 overview 路线并着色
     *
     * @param overviewPoints overview polyline 的所有点
     * @param steps 路线步骤（包含 travel_mode 和 distance）
     */
    private fun drawTransitRouteFallback(overviewPoints: List<LatLng>, steps: List<com.ecogo.mapengine.data.model.RouteStep>) {
        clearAllRoutePolylines()

        if (overviewPoints.size < 2 || steps.isEmpty()) {
            drawRoute(overviewPoints)
            return
        }

        // 计算总距离
        val totalStepDistance = steps.sumOf { it.distance }
        if (totalStepDistance <= 0) {
            drawRoute(overviewPoints)
            return
        }

        Log.d(TAG, "drawTransitRouteFallback: ${overviewPoints.size} points, ${steps.size} steps, totalDist=$totalStepDistance m")

        // 按每个步骤的距离比例分配 overview 点
        var pointIndex = 0
        val totalPoints = overviewPoints.size

        for ((stepIdx, step) in steps.withIndex()) {
            val ratio = step.distance / totalStepDistance
            // 计算该步骤分配到的点数（至少2个，最后一步取剩余所有点）
            val pointsForStep = if (stepIdx == steps.size - 1) {
                totalPoints - pointIndex
            } else {
                (totalPoints * ratio).toInt().coerceAtLeast(2)
            }

            val endIndex = (pointIndex + pointsForStep).coerceAtMost(totalPoints)
            if (endIndex <= pointIndex || pointIndex >= totalPoints) continue

            val segmentPoints = overviewPoints.subList(pointIndex, endIndex)
            if (segmentPoints.size < 2) {
                pointIndex = endIndex
                continue
            }

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

            googleMap?.addPolyline(polylineOptions)?.let {
                transitSegmentPolylines.add(it)
            }

            Log.d(TAG, "  Fallback step $stepIdx: mode=${step.travel_mode}, vehicle=${step.transit_details?.vehicle_type}, points=${segmentPoints.size}")

            pointIndex = endIndex
        }

        // 调整相机
        val boundsBuilder = LatLngBounds.Builder()
        overviewPoints.forEach { boundsBuilder.include(it) }
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120))
    }

    /**
     * 获取交通步骤对应的颜色
     */
    private fun getColorForTransitStep(step: com.ecogo.mapengine.data.model.RouteStep): Int {
        return when (step.travel_mode) {
            "WALKING" -> ContextCompat.getColor(this, R.color.route_walking)
            "TRANSIT" -> {
                // 根据车辆类型进一步区分颜色
                when (step.transit_details?.vehicle_type?.uppercase()) {
                    "SUBWAY", "METRO_RAIL" -> ContextCompat.getColor(this, R.color.route_subway)
                    "BUS", "INTERCITY_BUS", "TROLLEYBUS" -> ContextCompat.getColor(this, R.color.route_bus)
                    "RAIL", "HEAVY_RAIL", "COMMUTER_TRAIN", "HIGH_SPEED_TRAIN", "LONG_DISTANCE_TRAIN" ->
                        ContextCompat.getColor(this, R.color.route_rail)
                    "TRAM", "MONORAIL" -> ContextCompat.getColor(this, R.color.route_tram)
                    else -> ContextCompat.getColor(this, R.color.route_bus)
                }
            }
            "DRIVING" -> ContextCompat.getColor(this, R.color.route_driving)
            "BICYCLING" -> ContextCompat.getColor(this, R.color.route_cycling)
            else -> ContextCompat.getColor(this, R.color.route_remaining)
        }
    }

    /**
     * 清除所有路线 Polyline
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
     * 更新路线信息卡片
     */
    private fun updateRouteInfo(route: com.ecogo.mapengine.data.model.RouteRecommendData) {
        binding.cardRouteInfo.visibility = View.VISIBLE

        // 路线类型
        val routeTypeText = when (route.route_type) {
            "low_carbon" -> "低碳路线"
            "balanced" -> "平衡路线"
            else -> "推荐路线"
        }
        binding.tvRouteType.text = routeTypeText

        // 碳减排 - 绿色出行强化显示
        val carbonSavedText = if (route.carbon_saved > 0) {
            String.format("🌍 比驾车减少 %.2f kg 碳排放", route.carbon_saved)
        } else {
            String.format("碳排放: %.2f kg", route.total_carbon)
        }
        binding.tvCarbonSaved.text = carbonSavedText

        // 根据碳排放设置颜色编码（绿色=低碳，黄色=中碳，红色=高碳）
        val carbonColor = when {
            route.total_carbon == 0.0 -> android.graphics.Color.parseColor("#4CAF50") // 绿色 - 零碳
            route.total_carbon < 0.5 -> android.graphics.Color.parseColor("#8BC34A") // 浅绿 - 低碳
            route.total_carbon < 1.5 -> android.graphics.Color.parseColor("#FFC107") // 黄色 - 中碳
            else -> android.graphics.Color.parseColor("#FF5722") // 红色 - 高碳
        }
        binding.tvCarbonSaved.setTextColor(carbonColor)

        // 环保评级（星级）
        val ecoRating = calculateEcoRating(route.total_carbon, route.total_distance)
        val ratingText = "环保指数: $ecoRating"
        binding.tvRouteType.text = "$routeTypeText  $ratingText"

        // 预计时间 (使用新字段 estimated_duration，兼容旧字段 duration)
        val durationMinutes = route.estimated_duration.takeIf { it > 0 } ?: route.duration ?: 0
        val durationText = "预计: $durationMinutes 分钟"
        binding.tvDuration.text = durationText

        // 显示累计环保贡献（仅绿色出行方式显示）
        if (route.carbon_saved > 0) {
            binding.tvCumulativeImpact.visibility = View.VISIBLE
            binding.tvCumulativeImpact.text = com.ecogo.mapengine.util.GreenTravelStats.formatWeeklyImpact(this)
        } else {
            binding.tvCumulativeImpact.visibility = View.GONE
        }

        // 显示路线选择列表（仅公交模式且有多条路线）
        if (!route.route_alternatives.isNullOrEmpty()) {
            binding.rvRouteOptions.visibility = View.VISIBLE
            routeOptionAdapter.setRoutes(route.route_alternatives)
        } else {
            binding.rvRouteOptions.visibility = View.GONE
        }

        // 显示详细步骤列表（仅公交模式显示详细步骤）
        val hasTransitSteps = route.route_steps?.any { it.travel_mode == "TRANSIT" } == true
        if (hasTransitSteps && !route.route_steps.isNullOrEmpty()) {
            binding.rvRouteSteps.visibility = View.VISIBLE
            routeStepAdapter.setSteps(route.route_steps)
        } else {
            binding.rvRouteSteps.visibility = View.GONE
        }
    }

    /**
     * 计算环保评级（星级）
     * 基于碳排放量和距离计算环保指数
     */
    private fun calculateEcoRating(totalCarbon: Double, distance: Double): String {
        // 计算每公里碳排放
        val carbonPerKm = if (distance > 0) totalCarbon / distance else totalCarbon

        // 根据每公里碳排放计算星级（0-5星）
        val stars = when {
            carbonPerKm == 0.0 -> "⭐⭐⭐⭐⭐" // 零碳 - 5星
            carbonPerKm < 0.03 -> "⭐⭐⭐⭐" // 地铁级别 - 4星
            carbonPerKm < 0.06 -> "⭐⭐⭐" // 公交级别 - 3星
            carbonPerKm < 0.10 -> "⭐⭐" // 混合出行 - 2星
            else -> "⭐" // 高碳 - 1星
        }

        return stars
    }

    /**
     * 处理用户选择路线
     */
    private fun onRouteSelected(route: com.ecogo.mapengine.data.model.RouteAlternative) {
        Log.d(TAG, "Route selected: ${route.summary}")

        // 检查是否有步骤级别的 polyline 数据用于多色绘制
        val hasTransitSteps = route.route_steps.any { it.travel_mode == "TRANSIT" }
        val hasStepPolylines = route.route_steps.any { !it.polyline_points.isNullOrEmpty() }
        val points = route.route_points.map { com.google.android.gms.maps.model.LatLng(it.lat, it.lng) }

        if (hasTransitSteps && hasStepPolylines) {
            // 使用步骤级别的 polyline 多色绘制
            drawTransitRoute(route.route_steps)
        } else if (hasTransitSteps && route.route_steps.isNotEmpty()) {
            // 回退：按距离比例切割 overview 路线并着色
            drawTransitRouteFallback(points, route.route_steps)
        } else {
            // 非公交：单色绘制
            drawRoute(points)
        }

        // 更新 ViewModel 的路线点（用于导航），设置标记避免 observer 重复绘制
        isHandlingRouteSelection = true
        val allPoints = route.route_points.map { LatLng(it.lat, it.lng) }
        viewModel.updateRoutePointsForSelectedAlternative(allPoints)

        // 更新路线信息
        binding.tvCarbonSaved.text = String.format("减碳: %.2f kg", route.total_carbon)
        binding.tvDuration.text = "预计: ${route.estimated_duration} 分钟"

        // 更新详细步骤
        if (hasTransitSteps) {
            binding.rvRouteSteps.visibility = View.VISIBLE
            routeStepAdapter.setSteps(route.route_steps)
        } else {
            binding.rvRouteSteps.visibility = View.GONE
        }

        Toast.makeText(this, "已切换到: ${route.summary}", Toast.LENGTH_SHORT).show()
    }

    /**
     * 更新行程追踪 UI
     */
    private fun updateTrackingUI(state: TripState) {
        when (state) {
            is TripState.Idle -> {
                binding.btnTracking.text = getString(R.string.start_tracking)
                binding.btnTracking.isEnabled = true
                binding.chipGroupTransport.visibility = View.VISIBLE
                binding.cardSearch.visibility = View.VISIBLE
                hideTimer()
                // 清除追踪轨迹
                trackPolyline?.remove()
                trackPolyline = null
                transitSegmentPolylines.forEach { it.remove() }
                transitSegmentPolylines.clear()
            }
            is TripState.Starting -> {
                binding.btnTracking.text = "正在开始..."
                binding.btnTracking.isEnabled = false
            }
            is TripState.Tracking -> {
                binding.btnTracking.text = getString(R.string.stop_tracking)
                binding.btnTracking.isEnabled = true
                binding.chipGroupTransport.visibility = View.GONE
                binding.cardSearch.visibility = View.GONE
                // 显示追踪信息卡片
                binding.cardRouteInfo.visibility = View.VISIBLE

                // 显示正在检测交通方式
                binding.tvRouteType.text = "🔄 正在检测交通方式..."

                if (isNavigationMode) {
                    // 导航模式
                    binding.tvCarbonSaved.text = "已行进: 0.00 公里"
                    val remainingKm = (NavigationManager.remainingDistance.value ?: 0f) / 1000f
                    binding.tvDuration.text = String.format("剩余: %.2f 公里", remainingKm)
                } else {
                    // 纯轨迹记录模式
                    binding.tvCarbonSaved.text = "已行进: 0.00 公里"
                    binding.tvDuration.text = "实时记录GPS轨迹"
                }
            }
            is TripState.Stopping -> {
                binding.btnTracking.text = "正在结束..."
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
        // 检查是否有正在进行的行程，恢复UI状态
        // 注意：地图可能还没准备好，所以这里只恢复非地图相关的状态
        restoreTrackingStateIfNeeded()
    }

    /**
     * 恢复追踪状态（如果有正在进行的行程）
     */
    private fun restoreTrackingStateIfNeeded() {
        if (LocationManager.isTracking.value == true) {
            Log.d(TAG, "Restoring tracking state - trip is in progress")

            // 恢复 ViewModel 状态
            viewModel.restoreTrackingState()

            // 恢复导航模式标志
            if (NavigationManager.isNavigating.value == true) {
                isNavigationMode = true
            }

            // 如果地图已准备好，恢复路线显示
            if (googleMap != null) {
                restoreRoutesOnMap()
            }

            // 恢复计时器（使用保存的开始时间）
            if (timerStartTime == 0L) {
                // 如果没有计时器开始时间，使用当前时间（会导致时间重置，但比没有好）
                timerStartTime = SystemClock.elapsedRealtime()
            }
            binding.tvTimer.visibility = View.VISIBLE
            timerHandler.post(timerRunnable)

            // 恢复交通方式检测
            if (!transportModeDetector.isDetecting()) {
                transportModeDetector.startDetection()
            }

            isFollowingUser = true
        }
    }

    /**
     * 在地图上恢复路线显示
     */
    private fun restoreRoutesOnMap() {
        Log.d(TAG, "Restoring routes on map, isNavigationMode=$isNavigationMode")

        if (isNavigationMode && NavigationManager.isNavigating.value == true) {
            // 导航模式：绘制已走过的路线和剩余路线
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
            // 纯轨迹记录模式：绘制轨迹
            LocationManager.trackPoints.value?.let { points ->
                if (points.isNotEmpty()) {
                    Log.d(TAG, "Drawing track polyline with ${points.size} points")
                    drawTrackPolyline(points)
                }
            }
        }

        // 如果有目的地标记，重新绘制
        destinationLatLng?.let { latLng ->
            updateDestinationMarker(latLng, destinationName)
        }

        // 如果有起点标记且不是"我的位置"，重新绘制
        originLatLng?.let { latLng ->
            if (originName != "我的位置") {
                updateOriginMarker(latLng, originName)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清除计时器，防止内存泄漏
        timerHandler.removeCallbacks(timerRunnable)
        
        // 停止广告轮播
        adRunnable?.let { adHandler.removeCallbacks(it) }

        // 移除位置更新回调
        removeLocationUpdates()

        // 注意：不再自动停止追踪！
        // 前台服务会继续运行，用户需要手动停止行程
        // 只有在用户明确点击停止按钮时才会停止追踪

        // 清除交通方式检测器（Activity销毁时暂停，但服务继续追踪）
        if (this::transportModeDetector.isInitialized) {
            transportModeDetector.stopDetection()
            transportModeDetector.cleanup()
        }
    }
}
