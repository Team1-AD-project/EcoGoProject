package com.ecogo.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecogo.R
import com.ecogo.data.MascotSize
import com.ecogo.mapengine.ui.map.MapActivity
import com.ecogo.data.MockData
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
import com.ecogo.api.ShuttleServiceResponse
import com.ecogo.utils.NotificationUtil


class HomeFragment : Fragment() {

    companion object {
        private const val TAG = "HomeFragment"
        private const val DEFAULT_USER_ID = "user123"
        private const val STATUS_ON_TIME = "On Time"
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val repository = EcoGoRepository()

    // User's current outfit (in production, fetch from user data)
    private val currentOutfit = Outfit(head = "none", face = "none", body = "shirt_nus", badge = "a1")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return try {
            Log.d(TAG, "HomeFragment onCreateView - inflating binding")
            Toast.makeText(context, "HomeFragment loading...", Toast.LENGTH_SHORT).show()
            _binding = FragmentHomeBinding.inflate(inflater, container, false)
            Log.d(TAG, "HomeFragment binding inflated successfully")
            Toast.makeText(context, "HomeFragment loaded successfully!", Toast.LENGTH_SHORT).show()
            binding.root
        } catch (e: Exception) {
            Log.e(TAG, "HomeFragment onCreateView FAILED: ${e.message}", e)
            Toast.makeText(context, "HomeFragment creation failed: ${e.message}", Toast.LENGTH_LONG).show()
            throw e
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            Log.d(TAG, "HomeFragment onViewCreated - starting setup")

            try {
                setupUI()
                Log.d(TAG, "setupUI completed")
            } catch (e: Exception) {
                Log.e(TAG, "setupUI FAILED: ${e.message}", e)
                Toast.makeText(requireContext(), "setupUI failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }

            try {
                setupRecyclerView()
                Log.d(TAG, "setupRecyclerView completed")
            } catch (e: Exception) {
                Log.e(TAG, "setupRecyclerView FAILED: ${e.message}", e)
            }

            try {
                setupAnimations()
                Log.d(TAG, "setupAnimations completed")
            } catch (e: Exception) {
                Log.e(TAG, "setupAnimations FAILED: ${e.message}", e)
            }

            try {
                setupActions()
                Log.d(TAG, "setupActions completed")
            } catch (e: Exception) {
                Log.e(TAG, "setupActions FAILED: ${e.message}", e)
            }

            try {
                loadData()
            } catch (e: Exception) {
                Log.e(TAG, "loadData FAILED: ${e.message}", e)
            }

            Log.d(TAG, "HomeFragment onViewCreated completed")
        } catch (e: Exception) {
            Log.e(TAG, "HomeFragment onViewCreated FAILED: ${e.message}", e)
            Toast.makeText(requireContext(), "HomeFragment initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupUI() {
        try {
            binding.textBusNumber.text = "D1"
            binding.textBusTime.text = "2 min"
            binding.textBusRoute.text = "to UTown"
            binding.textMonthlyPoints.text = "880"
            binding.textPointsChange.text = "" // Removed "+150 this week" per user request
            binding.textSocScore.text = "5,530 kg" // Added kg unit
            binding.textSocRank.text = "" // Removed "Rank #1" per user request
            binding.textLocation.text = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", java.util.Locale.ENGLISH))

            Log.d(TAG, "Basic UI setup completed, setting up mascot")

            // Set up mascot avatar
            try {
                binding.mascotAvatar.apply {
                    mascotSize = MascotSize.MEDIUM
                    outfit = currentOutfit
                    Log.d(TAG, "Mascot configured, starting wave animation")
                    // Play wave animation on entry
                    waveAnimation()
                }
                Log.d(TAG, "Mascot setup completed")
            } catch (e: Exception) {
                Log.e(TAG, "Mascot setup FAILED: ${e.message}", e)
                Toast.makeText(requireContext(), "Mascot initialization failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "setupUI FAILED: ${e.message}", e)
            Toast.makeText(requireContext(), "UI initialization failed: ${e.message}", Toast.LENGTH_SHORT).show()
            throw e
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
            launch { loadCarbonFootprint() }
            launch { loadWeather() }
        }
    }

    private suspend fun loadUserProfile() {
        // val userId = com.ecogo.auth.TokenManager.getUserId() ?: return // userId not needed for this call
        val result = repository.getMobileUserProfile()
        val profile = result.getOrNull()
        if (profile != null) {
            val userInfo = profile.userInfo
            // Update UI with real data on Main thread

            // Dynamic Greeting
            val isVip = (profile.vipInfo?.active == true) || 
                        (profile.userInfo.vip?.active == true) || 
                        (profile.vipInfo?.plan != null) ||
                        (profile.userInfo.vip?.plan != null) ||
                        (profile.userInfo.isAdmin == true)
            
            val displayNickname = if (isVip) "${userInfo.nickname} (VIP)" else userInfo.nickname
            binding.textWelcome.text = "Hello, $displayNickname"

            // Update Rank (Removed per user request)
            /*
            profile.stats?.let { stats ->
                binding.textSocRank.text = "Rank #${stats.monthlyRank}"
            }
            */

            // Load SoC Score independently
            loadSocScore()

            // Show churn toast after login
            // Only show once, avoid showing on every home refresh
            //if (shouldShowChurnToastToday()){
                val userId = com.ecogo.auth.TokenManager.getUserId()
                if (!userId.isNullOrBlank()) {
                    val level = repository.fetchMyChurnRisk(userId)
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        kotlinx.coroutines.delay(800)
                        Log.d("CHURN", "TokenManager userId = ${com.ecogo.auth.TokenManager.getUserId()}")
                        NotificationUtil.showChurnNotification(requireContext(), level)
                    }
                }

            //}


            // Update Carbon Footprint (Added)
            val carbon = userInfo.totalCarbon
            val trees = if (carbon > 0) carbon / 18.0 else 0.0
            binding.textCo2Saved.text = "%.1f kg".format(carbon)
            binding.textTreeEquivalent.text = "%.1f trees".format(trees) // User asked for 1 decimal place

            // Update Carbon Period if stats available
            profile.stats?.let { stats ->
                binding.textCarbonPeriod.text = "Total · ${stats.totalTrips} eco trips"
            }
        }
    }

    private suspend fun loadSocScore() {
        val scoreResult = repository.getFacultyTotalCarbon()
        val data = scoreResult.getOrNull()
        val score = data?.totalCarbon ?: 0.0
        
        // 1. Format Score to 2 decimal places with kg unit
        binding.textSocScore.text = "%.2f kg".format(score)

        // 2. Dynamic Faculty Abbreviation
        val facultyName = data?.faculty ?: "School of Computing" // Default or fallback
        val abbreviation = getFacultyAbbreviation(facultyName)
        binding.textSocScoreLabel.text = "$abbreviation Score"
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
        val historyResult = repository.getMobilePointsHistory().getOrElse { emptyList() }
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

        binding.textMonthlyPoints.text = monthlyPoints.toString()
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

        // 2) Update Home card UI (currently showing these three fields)
        binding.textBusNumber.text = routeName
        binding.textBusTime.text = if (etaMin >= 0) "$etaMin min" else "-"
        binding.textBusRoute.text = "from $stopLabel"

        // If you also want to show status on Home (check if fragment_home.xml has a corresponding TextView)
        // e.g.: binding.textBusStatus.text = status
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
        val userId = com.ecogo.auth.TokenManager.getUserId() ?: DEFAULT_USER_ID
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

        (binding.recyclerHighlights.adapter as? HomeStatAdapter)?.updateData(stats)
    }

    private suspend fun loadHomeActivities() {
        val activitiesResult = repository.getAllActivities().getOrElse { MockData.ACTIVITIES }
        // Display up to 5 activities on home page using horizontal scrolling cards
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
            val destination = binding.editPlan.text?.toString()?.trim().orEmpty()
            if (destination.isNotEmpty()) {
                requestRecommendation(destination)
                binding.editPlan.text?.clear()
            }
        }
        binding.buttonPlanMic?.setOnClickListener {
            // Voice input placeholder: can integrate voice recognition later
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

    private fun requestRecommendation(destination: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val response = repository.getRecommendation(RecommendationRequest(destination)).getOrElse { null }
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
            val notifications = repository.getNotifications(DEFAULT_USER_ID).getOrNull()
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
            val goal = repository.getDailyGoal(DEFAULT_USER_ID).getOrNull()
            if (goal != null) {
                val stepProgress = (goal.currentSteps.toFloat() / goal.stepGoal * 100).toInt().coerceIn(0, 100)
                val tripProgress = (goal.currentTrips.toFloat() / goal.tripGoal * 100).toInt().coerceIn(0, 100)
                val co2Progress = (goal.currentCo2Saved / goal.co2SavedGoal * 100).toInt().coerceIn(0, 100)

                binding.progressSteps.progress = stepProgress
                binding.progressTrips.progress = tripProgress
                binding.progressCo2.progress = co2Progress
                binding.textStepsProgress.text = "✔ ${goal.currentTrips}/${goal.tripGoal} eco trips"
                binding.textTripsProgress.text = "✔ ${"%.1f".format(goal.currentTrips.toFloat())}/${goal.tripGoal} eco trips"
                binding.textCo2Progress.text = "✔ ${"%.2f".format(goal.currentCo2Saved)} kg saved"
            }
        }
    }

    private fun loadCarbonFootprint() {
        // Deprecated: Carbon footprint is now loaded from UserProfile (totalCarbon)
        // Kept empty to avoid breaking older calls if any, but removed from loadData()
    }

    private fun loadWeather() {
        viewLifecycleOwner.lifecycleScope.launch {
            // 1. Get Result
            val result = repository.getWeather()

            // 2. Check success or failure
            if (result.isSuccess) {
                val weather = result.getOrNull()
                if (weather != null) {
                    // --- Success: update text ---
                    binding.textTemperature.text = "${weather.temperature}°C"
                    binding.textWeatherCondition.text = weather.description
                    binding.textAqiValue.text = "AQI ${weather.airQuality}"

                    // Set weather icon
                    // 1. Get the icon resource ID using the helper function
                    val iconResId = getWeatherIcon(weather.description)

                    // 2. Set the image on the ImageView
                    binding.imageWeatherIcon.setImageResource(iconResId)

                    android.util.Log.d("HomeFragment", "Weather loaded successfully: ${weather.description}")
                }
            } else {
                // --- Failure: log error ---
                val error = result.exceptionOrNull()
                android.util.Log.e("HomeFragment", "Weather loading failed", error)
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