package com.ecogo.ui.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecogo.R
import com.ecogo.data.MascotSize
import com.ecogo.mapengine.ui.map.MapActivity
import com.ecogo.data.Outfit
import com.ecogo.data.RecommendationRequest
import com.ecogo.databinding.FragmentHomeBinding
import com.ecogo.ui.adapters.HighlightAdapter
import com.ecogo.ui.adapters.HomeStatAdapter
import com.ecogo.ui.adapters.HomeStat
import com.ecogo.repository.EcoGoRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ecogo.api.NextBusApiClient
import com.ecogo.utils.NotificationUtil


class HomeFragment : Fragment() {

    companion object {
        private const val TAG = "HomeFragment"
        private const val STATUS_ON_TIME = "On Time"
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val repository = EcoGoRepository()

    // User's current outfit (in production, fetch from user data)
    private val currentOutfit = Outfit(head = "none", face = "none", body = "shirt_nus", badge = "a1")

    // ====== Local cache for instant UI display ======
    private val cache: SharedPreferences by lazy {
        requireContext().getSharedPreferences("home_cache", android.content.Context.MODE_PRIVATE)
    }

    private fun cached(key: String, fallback: String = "--"): String =
        cache.getString(key, fallback) ?: fallback

    private fun cachedInt(key: String, fallback: Int = 0): Int =
        cache.getInt(key, fallback)

    private fun save(vararg pairs: Pair<String, Any>) {
        cache.edit().apply {
            for ((k, v) in pairs) {
                when (v) {
                    is String -> putString(k, v)
                    is Int -> putInt(k, v)
                    else -> putString(k, v.toString())
                }
            }
        }.apply()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupRecyclerView()
        setupAnimations()
        setupActions()
        loadData()
    }

    private fun setupUI() {
        // Immediately set greeting from cached data to avoid flicker
        val cachedUsername = com.ecogo.auth.TokenManager.getUsername()
        val welcomeText = cached("welcome_text", "")
        if (welcomeText.isNotBlank()) {
            binding.textWelcome.text = welcomeText
        } else if (!cachedUsername.isNullOrBlank()) {
            binding.textWelcome.text = "Hello, $cachedUsername"
        }

        // Show CACHED data instantly — real data will overwrite from loadData()
        // Bus info (not cached — changes too frequently, show "--" until fresh)
        binding.textBusNumber.text = "--"
        binding.textBusTime.text = "--"
        binding.textBusRoute.text = ""
        // Monthly points
        binding.textMonthlyPoints.text = cached("monthly_points")
        binding.textPointsChange.text = ""
        // Community / Faculty score
        binding.textSocScore.text = cached("soc_score")
        binding.textSocScoreLabel.text = cached("soc_score_label", "SoC Score")
        binding.textSocRank.text = ""
        // Carbon footprint
        binding.textCo2Saved.text = cached("co2_saved")
        binding.textTreeEquivalent.text = cached("tree_equivalent")
        binding.textCarbonPeriod.text = cached("carbon_period", "")
        // Weather
        binding.textTemperature.text = cached("weather_temp")
        binding.textWeatherCondition.text = cached("weather_condition")
        binding.textAqiValue.text = cached("weather_aqi")
        binding.textHumidity.text = cached("weather_humidity")
        // Daily goal
        binding.progressSteps.progress = cachedInt("goal_step_pct")
        binding.progressTrips.progress = cachedInt("goal_trip_pct")
        binding.progressCo2.progress = cachedInt("goal_co2_pct")
        binding.textStepsProgress.text = cached("goal_steps_text")
        binding.textTripsProgress.text = cached("goal_trips_text")
        binding.textCo2Progress.text = cached("goal_co2_text")
        // Notification — hidden until loaded
        binding.cardNotification.visibility = View.GONE

        binding.textLocation.text = java.time.LocalDate.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", java.util.Locale.ENGLISH))

        // Set up mascot avatar
        binding.mascotAvatar.apply {
            mascotSize = MascotSize.MEDIUM
            outfit = currentOutfit
            waveAnimation()
        }
    }

    private fun setupRecyclerView() {
        // Monthly Highlights显示统计数据
        binding.recyclerHighlights.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = HomeStatAdapter(emptyList()) { stat ->
                // 点击统计卡片跳转到月度亮点页面
                findNavController().navigate(R.id.action_home_to_monthlyHighlights)
            }
        }
        // Activities显示活动列表
        binding.recyclerActivities.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = HighlightAdapter(emptyList()) { activity ->
                // 跳转到活动详情页，传递活动ID
                val action = HomeFragmentDirections.actionHomeToActivityDetail(activity.id ?: "")
                findNavController().navigate(action)
            }
        }
    }

    /**
     * Optimization: use concurrent and lazy loading strategies
     * 1. Critical data loaded immediately (bus info)
     * 2. Secondary data loaded concurrently
     * 3. Non-critical data loaded with delay
     */
    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Priority 1: Load critical data immediately (bus info)
            loadBusInfo()

            // Priority 2: Load secondary data concurrently
            launch { loadMonthlyHighlightsStats() }
            launch { loadHomeActivities() }
            launch { loadUserProfile() }
            launch { loadMonthlyPoints() }

            // Priority 3: Delayed loading for non-critical data (after 200ms)
            kotlinx.coroutines.delay(200)
            launch { loadNotifications() }
            launch { loadDailyGoal() }
            launch { loadWeather() }
        }
    }

    private suspend fun loadUserProfile() {
        val result = repository.getMobileUserProfile()
        if (_binding == null) return
        if (result.isFailure) {
            Log.w(TAG, "loadUserProfile failed", result.exceptionOrNull())
            return
        }
        val profile = result.getOrNull() ?: return
        val userInfo = profile.userInfo

        // Dynamic Greeting
        val isVip = (profile.vipInfo?.active == true) ||
                    (profile.userInfo.vip?.active == true) ||
                    (profile.vipInfo?.plan != null) ||
                    (profile.userInfo.vip?.plan != null) ||
                    (profile.userInfo.isAdmin == true)

        val displayNickname = if (isVip) "${userInfo.nickname} (VIP)" else userInfo.nickname
        val welcomeText = "Hello, $displayNickname"
        binding.textWelcome.text = welcomeText
        save("welcome_text" to welcomeText)

        // Load SoC Score independently
        loadSocScore()

        // Show churn toast after login
        val userId = com.ecogo.auth.TokenManager.getUserId()
        if (!userId.isNullOrBlank()) {
            val level = repository.fetchMyChurnRisk(userId)
            if (_binding == null || !isAdded) return
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                kotlinx.coroutines.delay(800)
                if (_binding != null && isAdded) {
                    NotificationUtil.showChurnNotification(requireContext(), level)
                }
            }
        }

        if (_binding == null) return

        // Update Carbon Footprint
        val carbon = userInfo.totalCarbon
        val trees = if (carbon > 0) carbon / 18.0 else 0.0
        val co2Text = "%.1f kg".format(carbon)
        val treeText = "%.1f trees".format(trees)
        binding.textCo2Saved.text = co2Text
        binding.textTreeEquivalent.text = treeText
        save("co2_saved" to co2Text, "tree_equivalent" to treeText)

        // Update Carbon Period if stats available
        profile.stats?.let { stats ->
            val periodText = "Total · ${stats.totalTrips} eco trips"
            binding.textCarbonPeriod.text = periodText
            save("carbon_period" to periodText)
        }
    }

    private suspend fun loadSocScore() {
        val scoreResult = repository.getFacultyTotalCarbon()
        if (_binding == null) return
        if (scoreResult.isFailure) {
            Log.w(TAG, "loadSocScore failed", scoreResult.exceptionOrNull())
        }
        val data = scoreResult.getOrNull()
        val score = data?.totalCarbon ?: 0.0

        val scoreText = "%.2f kg".format(score)
        binding.textSocScore.text = scoreText

        val facultyName = data?.faculty ?: "School of Computing"
        val abbreviation = getFacultyAbbreviation(facultyName)
        val labelText = "$abbreviation Score"
        binding.textSocScoreLabel.text = labelText
        save("soc_score" to scoreText, "soc_score_label" to labelText)
    }

    private fun getFacultyAbbreviation(facultyName: String): String {
        return when (facultyName) {
            "School of Computing" -> "SoC"
            "Faculty of Science" -> "FoS"
            "Faculty of Engineering" -> "FoE"
            "Business School" -> "Biz"
            "School of Design and Environment" -> "SDE"
            "Faculty of Arts and Social Sciences" -> "FASS"
            "Yong Loo Lin School of Medicine" -> "YLLSoM"
            "Saw Swee Hock School of Public Health" -> "SSHSPH"
            "Faculty of Law" -> "Law"
            "School of Music" -> "Music"
            else -> {
                // Fallback: Initials of capitalized words
                // e.g., "College of Humanities and Sciences" -> "CHS"
                facultyName.split(" ")
                    .filter { it.isNotEmpty() && it[0].isUpperCase() }
                    .map { it[0] }
                    .joinToString("")
            }
        }
    }

    private suspend fun loadMonthlyPoints() {
        val result = repository.getMobilePointsHistory()
        if (result.isFailure) {
            Log.w(TAG, "loadMonthlyPoints failed", result.exceptionOrNull())
        }
        val historyResult = result.getOrElse { emptyList() }
        val now = java.time.LocalDate.now()
        val currentMonth = now.month
        val currentYear = now.year

        val monthlyPoints = historyResult.filter { item ->
            // Filter by sources: trip, challenge, leaderboard, admin
            val validSources = setOf("trip", "challenge", "leaderboard", "admin")
            if (item.source !in validSources) return@filter false

            try {
                // Format: "2026-01-30T06:16:29.699"
                val date = java.time.LocalDateTime.parse(item.createdAt)
                date.month == currentMonth && date.year == currentYear
            } catch (e: Exception) {
                false
            }
        }.sumOf { item ->
            val type = item.changeType.uppercase()
            // If points are already negative, just use them.
            // If points are positive but type indicates deduction, negate them.
            if (item.points < 0) {
                item.points
            } else if (type.contains("DEDUCT") || type.contains("DECREASE") || type.contains("SPEND") || type.contains("REMOVE")) {
                -item.points
            } else {
                item.points
            }
        }

        if (_binding == null) return
        val pointsText = monthlyPoints.toString()
        binding.textMonthlyPoints.text = pointsText
        save("monthly_points" to pointsText)
    }

    private suspend fun loadBusInfo() {
        // 1) Default to UTOWN; if a stop was selected in Routes, use that saved preference
        val (stopCode, stopLabel) = getPreferredStopForHome()

        val (routeName, etaMin, status) = runCatching {
            val resp = NextBusApiClient.api.getShuttleService(stopCode)

            val shuttles = resp.ShuttleServiceResult?.shuttles.orEmpty()
            val first = shuttles.firstOrNull()

            val name = first?.name?.trim().orEmpty().ifEmpty { "-" }
            val eta = first?._etas?.firstOrNull()?.eta ?: -1
            val statusText = statusFromEta(eta)

            Triple(name, eta, statusText)
        }.getOrElse { e ->
            android.util.Log.e("NEXTBUS", "Home loadBusInfo failed", e)
            Triple("-", -1, STATUS_ON_TIME)
        }

        // Guard: view may be destroyed while coroutine was suspended
        if (_binding == null) return

        binding.textBusNumber.text = routeName
        binding.textBusTime.text = if (etaMin >= 0) "$etaMin min" else "-"
        binding.textBusRoute.text = "from $stopLabel"
    }

    private fun getPreferredStopForHome(): Pair<String, String> {
        val sp = requireContext().getSharedPreferences("nextbus_pref", android.content.Context.MODE_PRIVATE)
        val code = sp.getString("stop_code", null) ?: "UTOWN"
        val label = sp.getString("stop_label", null) ?: "University Town (UTown)"
        return code to label
    }

    private fun statusFromEta(eta: Int): String {
        if (eta < 0) return STATUS_ON_TIME
        return when {
            eta <= 2 -> "Arriving"
            eta <= 8 -> STATUS_ON_TIME
            eta <= 30 -> "Delayed"
            else -> "Scheduled"
        }
    }



    private suspend fun loadMonthlyHighlightsStats() {
        val userId = com.ecogo.auth.TokenManager.getUserId() ?: return
        val stats = mutableListOf<HomeStat>()

        // 1. Get points data
        val pointsResult = repository.getCurrentPoints().getOrNull()
        val userProfile = repository.getMobileUserProfile().getOrNull()
        val currentPoints = pointsResult?.currentPoints
            ?: userProfile?.userInfo?.currentPoints?.toLong()
            ?: 0L

        stats.add(HomeStat(
            icon = "⭐",
            title = "Total Points",
            value = "$currentPoints",
            subtitle = "current balance",
            color = "#FCD34D"
        ))

        // 2. Get count of activities user has joined
        val joinedActivitiesCount = repository.getJoinedActivitiesCount(userId).getOrNull() ?: 0
        stats.add(HomeStat(
            icon = "🎯",
            title = "Activities",
            value = "$joinedActivitiesCount",
            subtitle = "joined this month",
            color = "#A78BFA"
        ))

        // 3. Get count of challenges user has joined
        val joinedChallengesCount = repository.getJoinedChallengesCount(userId).getOrNull() ?: 0
        stats.add(HomeStat(
            icon = "🏆",
            title = "Challenges",
            value = "$joinedChallengesCount",
            subtitle = "joined this month",
            color = "#F97316"
        ))

        if (_binding == null) return
        (binding.recyclerHighlights.adapter as? HomeStatAdapter)?.updateData(stats)
    }

    private suspend fun loadHomeActivities() {
        val result = repository.getAllActivities()
        if (result.isFailure) {
            Log.w(TAG, "loadHomeActivities failed", result.exceptionOrNull())
        }
        val activitiesResult = result.getOrElse { emptyList() }
        if (_binding == null) return
        binding.recyclerActivities.adapter = HighlightAdapter(activitiesResult.take(5)) { activity ->
            val action = HomeFragmentDirections.actionHomeToActivityDetail(activity.id ?: "")
            findNavController().navigate(action)
        }
    }

    private fun setupAnimations() {
        val breathe = AnimationUtils.loadAnimation(requireContext(), com.ecogo.R.anim.breathe)
        val slideUp = AnimationUtils.loadAnimation(requireContext(), com.ecogo.R.anim.slide_up)
        val popIn = AnimationUtils.loadAnimation(requireContext(), com.ecogo.R.anim.pop_in)

        binding.cardRecommendation.startAnimation(popIn)
        binding.cardNextBus.startAnimation(breathe)
        binding.cardMap.startAnimation(slideUp)
        // Mascot has built-in animation, no need to start extra
    }

    private fun setupActions() {
        binding.buttonOpenMap.setOnClickListener {
            // Navigate to map engine page
            startActivity(Intent(requireContext(), MapActivity::class.java))
        }
        binding.textViewAll.setOnClickListener {
            // Navigate to monthly highlights page
            findNavController().navigate(R.id.action_home_to_monthlyHighlights)
        }

        binding.textViewAllActivities.setOnClickListener {
            // Navigate to activities list page
            findNavController().navigate(R.id.action_home_to_activities)
        }

        // Click mascot to navigate to Profile
        binding.mascotAvatar.setOnClickListener {
            findNavController().navigate(com.ecogo.R.id.profileFragment)
        }
        binding.buttonGo.setOnClickListener {
            submitDestination()
        }

        // Support keyboard "Go" action on the input field
        binding.editPlan.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                submitDestination()
                true
            } else false
        }

        // Monthly points card -> Profile page
        binding.cardMonthlyPoints.setOnClickListener {
            findNavController().navigate(com.ecogo.R.id.profileFragment)
        }

        // Community score card -> Community page
        binding.cardCommunityScore.setOnClickListener {
            findNavController().navigate(com.ecogo.R.id.communityFragment)
        }

        // Next bus card -> Routes page
        binding.cardNextBus.setOnClickListener {
            findNavController().navigate(com.ecogo.R.id.routesFragment)
        }

        // Map preview card -> Map engine page (entire card is clickable)
        binding.cardMap.setOnClickListener {
            startActivity(Intent(requireContext(), MapActivity::class.java))
        }

        // === New feature click events ===

        // Notification banner close button
        binding.buttonCloseNotification.setOnClickListener {
            binding.cardNotification.visibility = View.GONE
        }

        // Carbon footprint card click
        binding.cardCarbonFootprint.setOnClickListener {
            findNavController().navigate(com.ecogo.R.id.profileFragment)
        }

        // Weather card click
        binding.cardWeather.setOnClickListener {
            // Can navigate to weather details or map page
        }

        // Daily goal card click
        binding.cardDailyGoal.setOnClickListener {
            findNavController().navigate(com.ecogo.R.id.profileFragment)
        }

        // === Shortcut entry click events ===

        // Voucher shortcut entry
        binding.cardVoucherShortcut.setOnClickListener {
            findNavController().navigate(com.ecogo.R.id.action_home_to_voucher)
        }

        // Challenges shortcut entry
        binding.cardChallengesShortcut.setOnClickListener {
            findNavController().navigate(com.ecogo.R.id.action_home_to_challenges)
        }
    }

    private fun submitDestination() {
        val destination = binding.editPlan.text?.toString()?.trim().orEmpty()
        if (destination.isNotEmpty()) {
            requestRecommendation(destination)
            binding.editPlan.text?.clear()
            // Hide keyboard after submission
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.editPlan.windowToken, 0)
        }
    }

    private fun requestRecommendation(destination: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val response = repository.getRecommendation(RecommendationRequest(destination)).getOrElse { null }
            if (_binding == null) return@launch
            if (response != null) {
                val slideUp = AnimationUtils.loadAnimation(requireContext(), com.ecogo.R.anim.slide_up)
                binding.layoutRecommendationResult.visibility = View.VISIBLE
                binding.layoutRecommendationResult.startAnimation(slideUp)
                binding.textRecommendationTag.text = response.tag
                binding.textRecommendationResult.text = response.text

                binding.textAskAnother.setOnClickListener {
                    binding.layoutRecommendationResult.visibility = View.GONE
                }
            }
        }
    }

    // === New feature helper methods ===

    private fun loadNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            val userId = com.ecogo.auth.TokenManager.getUserId() ?: return@launch
            val notifications = repository.getNotifications(userId).getOrNull()
            if (_binding == null) return@launch
            val unreadNotif = notifications?.firstOrNull()
            if (unreadNotif != null) {
                binding.cardNotification.visibility = View.VISIBLE
                binding.textNotificationTitle.text = unreadNotif.title
                binding.textNotificationMessage.text = unreadNotif.message
            }
        }
    }

    private fun loadDailyGoal() {
        viewLifecycleOwner.lifecycleScope.launch {
            val tripsResult = repository.getMyTripHistory()
            if (tripsResult.isFailure) {
                Log.w(TAG, "loadDailyGoal (trip history) failed", tripsResult.exceptionOrNull())
                return@launch
            }
            if (_binding == null) return@launch

            val allTrips = tripsResult.getOrNull().orEmpty()
            val todayStr = java.time.LocalDate.now().toString() // "2026-02-13"

            // Filter today's trips by startTime (format: "2026-02-13T08:30:00")
            val todayTrips = allTrips.filter { trip ->
                trip.startTime?.startsWith(todayStr) == true
            }

            val totalDistance = todayTrips.sumOf { it.distance ?: 0.0 }
            val ecoTripCount = todayTrips.count { it.isGreenTrip == true }
            val totalCarbonSaved = todayTrips.sumOf { it.carbonSaved ?: 0.0 }

            // Progress bars: use reasonable daily targets for percentage
            val distTarget = 5.0   // 5 km daily target
            val tripTarget = 3     // 3 eco trips daily target
            val co2Target = 2.0    // 2 kg CO₂ daily target

            val distPct = ((totalDistance / distTarget) * 100).toInt().coerceIn(0, 100)
            val tripPct = ((ecoTripCount.toFloat() / tripTarget) * 100).toInt().coerceIn(0, 100)
            val co2Pct = ((totalCarbonSaved / co2Target) * 100).toInt().coerceIn(0, 100)

            val distText = "${"%.1f".format(totalDistance)} km"
            val tripText = "$ecoTripCount eco trips"
            val co2Text = "${"%.2f".format(totalCarbonSaved)} kg saved"

            binding.progressSteps.progress = distPct
            binding.progressTrips.progress = tripPct
            binding.progressCo2.progress = co2Pct
            binding.textStepsProgress.text = distText
            binding.textTripsProgress.text = tripText
            binding.textCo2Progress.text = co2Text
            save(
                "goal_step_pct" to distPct,
                "goal_trip_pct" to tripPct,
                "goal_co2_pct" to co2Pct,
                "goal_steps_text" to distText,
                "goal_trips_text" to tripText,
                "goal_co2_text" to co2Text
            )
        }
    }

    private fun loadWeather() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.getWeather()
            if (_binding == null) return@launch

            if (result.isFailure) {
                Log.w(TAG, "loadWeather failed", result.exceptionOrNull())
            }
            if (result.isSuccess) {
                val weather = result.getOrNull()
                if (weather != null) {
                    val tempText = "${weather.temperature}°C"
                    val condText = weather.description
                    val aqiText = "AQI ${weather.airQuality}"
                    binding.textTemperature.text = tempText
                    binding.textWeatherCondition.text = condText
                    binding.textAqiValue.text = aqiText
                    binding.imageWeatherIcon.setImageResource(getWeatherIcon(weather.description))
                    save(
                        "weather_temp" to tempText,
                        "weather_condition" to condText,
                        "weather_aqi" to aqiText
                    )
                }
            }
        }
    }

    // Returns the corresponding weather icon resource ID based on description
    private fun getWeatherIcon(description: String): Int {
        val desc = description.lowercase() // Convert to lowercase for matching

        return when {
            // --- 1. Rainy (contains rain, thunder, storm, etc.) ---
            desc.contains("rain") ||
                    desc.contains("shower") ||
                    desc.contains("drizzle") ||
                    desc.contains("thunder") ||
                    desc.contains("storm") -> {
                R.drawable.ic_weather_rain // Rain icon
            }

            // --- 2. Cloudy (contains cloud, fog, mist, etc.) ---
            desc.contains("cloud") ||
                    desc.contains("overcast") ||
                    desc.contains("fog") ||
                    desc.contains("mist") ||
                    desc.contains("haze") -> {
                R.drawable.ic_weather_cloudy // Cloudy icon
            }

            // --- 3. Sunny (contains sun, clear) ---
            desc.contains("sun") ||
                    desc.contains("clear") -> {
                R.drawable.ic_weather_sunny // Sunny icon
            }

            // --- 4. Default/unknown ---
            else -> R.drawable.ic_weather_cloudy
        }
    }

// ----------------------- Churn alert related ---------------------------------
    private fun churnToastMessage(riskLevel: String?): String {
        return when (riskLevel?.uppercase()) {
            "LOW" -> "EcoGo: Steady state! Complete one green trip to earn extra points~"
            "MEDIUM" -> "EcoGo: Here's a mini challenge - complete it to earn bonus points!"
            "HIGH" -> "EcoGo: You've been less active lately. Here's a limited-time voucher, come check it out!"
            "INSUFFICIENT_DATA" -> "EcoGo: Use a bit more and we can give you more accurate suggestions~"
            else -> "EcoGo: Welcome back!"
        }
    }

    private fun shouldShowChurnToastToday(): Boolean {
        val sp = requireContext().getSharedPreferences("ecogo_pref", android.content.Context.MODE_PRIVATE)
        val today = java.time.LocalDate.now().toString() // "2026-02-07"
        val lastShown = sp.getString("churn_toast_last_date", null)
        if (lastShown != today) {
            sp.edit().putString("churn_toast_last_date", today).apply()
            return true
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch { loadBusInfo() }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}