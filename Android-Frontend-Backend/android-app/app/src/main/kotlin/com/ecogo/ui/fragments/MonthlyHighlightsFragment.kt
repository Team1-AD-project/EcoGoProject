package com.ecogo.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecogo.R
import com.ecogo.databinding.FragmentMonthlyHighlightsBinding
import com.ecogo.repository.EcoGoRepository
import com.ecogo.ui.adapters.MonthlyActivityAdapter
import com.ecogo.ui.adapters.MonthlyChallengeAdapter
import com.ecogo.ui.adapters.ChallengeWithProgress
import com.ecogo.ui.adapters.MonthStatAdapter

import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Monthly highlights page
 * Displays featured activities, statistics, and achievements for the current month
 */
class MonthlyHighlightsFragment : Fragment() {

    companion object {
        private const val DEFAULT_USER_ID = "user123"
        private const val CO2_FORMAT = "%.2f kg CO\u2082"
    }

    private var _binding: FragmentMonthlyHighlightsBinding? = null
    private val binding get() = _binding!!

    private val repository = EcoGoRepository()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMonthlyHighlightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupRecyclerViews()
        loadData()
        setupAnimations()
    }
    
    private fun setupUI() {
        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Set current month
        val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        binding.textCurrentMonth.text = currentMonth
        
        // View all joined activities
        binding.btnViewAllActivities.setOnClickListener {
            val action = MonthlyHighlightsFragmentDirections
                .actionMonthlyHighlightsToJoinedActivities(showJoinedOnly = true)
            findNavController().navigate(action)
        }
        
        // View full leaderboard
        binding.btnViewFullLeaderboard.setOnClickListener {
            findNavController().navigate(R.id.communityFragment)
        }

        // View all challenges
        binding.btnViewAllChallenges.setOnClickListener {
            findNavController().navigate(R.id.action_monthlyHighlights_to_challenges)
        }
    }
    
    private fun setupRecyclerViews() {
        // Monthly stats cards (horizontal scroll)
        binding.recyclerMonthStats.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = MonthStatAdapter(emptyList())
        }
        
        // Featured activities list (grid layout)
        binding.recyclerFeaturedActivities.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = MonthlyActivityAdapter(emptyList()) { activity ->
                // Navigate to activity detail
                val action = MonthlyHighlightsFragmentDirections
                    .actionMonthlyHighlightsToActivityDetail(activity.id ?: "")
                findNavController().navigate(action)
            }
        }
        
        // Challenges list
        binding.recyclerChallenges.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = MonthlyChallengeAdapter(emptyList()) { challenge ->
                findNavController().navigate(R.id.action_monthlyHighlights_to_challenges)
            }
        }

    }
    
    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Load monthly statistics
            loadMonthlyStats()

            // Load featured activities
            loadFeaturedActivities()

            // Load user's monthly achievements
            loadMonthlyAchievements()

            // Load challenge progress
            loadChallenges()

            // Load leaderboard top 3
            loadLeaderboardTop3()

        }
    }
    
    private suspend fun loadMonthlyStats() {
        val userId = com.ecogo.auth.TokenManager.getUserId() ?: DEFAULT_USER_ID
        val stats = mutableListOf<MonthStat>()



        // 1. Get real points data
        val pointsResult = repository.getCurrentPoints().getOrNull()
        val userProfile = repository.getMobileUserProfile().getOrNull()


        val currentPoints = pointsResult?.currentPoints
            ?: userProfile?.userInfo?.currentPoints?.toLong()
            ?: 0L


        stats.add(MonthStat(
            icon = "⭐",
            title = "Total Points",
            value = "$currentPoints",
            subtitle = "current balance",
            color = "#FCD34D"
        ))

        // 2. Get joined activities count
        val joinedActivitiesCount = repository.getJoinedActivitiesCount(userId).getOrNull() ?: 0

        stats.add(MonthStat(
            icon = "🎯",
            title = "Activities",
            value = "$joinedActivitiesCount",
            subtitle = "joined this month",
            color = "#A78BFA"
        ))

        // 3. Get joined challenges count
        val joinedChallengesCount = repository.getJoinedChallengesCount(userId).getOrNull() ?: 0

        stats.add(MonthStat(
            icon = "🏆",
            title = "Challenges",
            value = "$joinedChallengesCount",
            subtitle = "joined this month",
            color = "#F97316"
        ))

        // 4. CO2 reduction (try from API, fallback to mock)
        val carbon = repository.getCarbonFootprint(userId, "monthly").getOrNull()
        if (carbon != null) {
            stats.add(MonthStat(
                icon = "🌱",
                title = "CO₂ Saved",
                value = "${String.format("%.1f", carbon.co2Saved)} kg",
                subtitle = "${carbon.equivalentTrees} trees equivalent",
                color = "#34D399"
            ))

            // Eco-friendly trip count
            val totalTrips = carbon.tripsByBus + carbon.tripsByWalking + carbon.tripsByBicycle
            stats.add(MonthStat(
                icon = "🚌",
                title = "Eco Trips",
                value = "$totalTrips",
                subtitle = "trips this month",
                color = "#60A5FA"
            ))
        }

        // 5. Consecutive check-in days (try from API)
        val checkInStatus = repository.getCheckInStatus(userId).getOrNull()
        if (checkInStatus != null) {
            stats.add(MonthStat(
                icon = "🔥",
                title = "Check-in Streak",
                value = "${checkInStatus.consecutiveDays}",
                subtitle = "days in a row",
                color = "#F87171"
            ))
        }

        (binding.recyclerMonthStats.adapter as? MonthStatAdapter)?.updateData(stats)
    }
    
    private suspend fun loadFeaturedActivities() {
        val userId = com.ecogo.auth.TokenManager.getUserId() ?: DEFAULT_USER_ID
        val activitiesResult = repository.getAllActivities().getOrElse { emptyList() }

        // Filter activities user has joined
        val joinedActivities = activitiesResult
            .filter { it.participantIds.contains(userId) }
            .sortedByDescending { it.rewardCredits }

        // Show at most 2 on the overview
        val displayActivities = joinedActivities.take(2)

        if (joinedActivities.isEmpty()) {
            // No joined activities, hide RecyclerView and show empty state
            binding.recyclerFeaturedActivities.visibility = View.GONE
            binding.textNoJoinedActivities.visibility = View.VISIBLE
            binding.textActivityCount.text = "0 Joined Activities"
        } else {
            binding.recyclerFeaturedActivities.visibility = View.VISIBLE
            binding.textNoJoinedActivities.visibility = View.GONE
            (binding.recyclerFeaturedActivities.adapter as? MonthlyActivityAdapter)?.updateData(displayActivities)
            binding.textActivityCount.text = "${joinedActivities.size} Joined Activities"
        }
    }
    
    private suspend fun loadMonthlyAchievements() {
        // TODO: Load this month's unlocked achievements from backend
        // Using mock data here
//        binding.textAchievementsUnlocked.text = "3 Achievements Unlocked This Month"
        
        // Display achievement badges (if any)
//        binding.layoutAchievements.visibility = View.VISIBLE
    }
    
    private suspend fun loadChallenges() {
        val userId = com.ecogo.auth.TokenManager.getUserId() ?: DEFAULT_USER_ID

        // Get user's joined challenges from backend
        val joinedChallenges = repository.getJoinedChallenges(userId).getOrElse { emptyList() }

        if (joinedChallenges.isEmpty()) {
            binding.recyclerChallenges.visibility = View.GONE
            binding.textNoJoinedChallenges.visibility = View.VISIBLE
            binding.textChallengeCount.text = "0 Joined Challenges"
            return
        }

        binding.recyclerChallenges.visibility = View.VISIBLE
        binding.textNoJoinedChallenges.visibility = View.GONE
        binding.textChallengeCount.text = "${joinedChallenges.size} Joined Challenges"

        // Get user progress for each challenge
        val challengeItems = joinedChallenges.map { challenge ->
            val progress = repository.getChallengeProgress(challenge.id, userId).getOrNull()
            ChallengeWithProgress(challenge, progress)
        }

        (binding.recyclerChallenges.adapter as? MonthlyChallengeAdapter)?.updateData(challengeItems)
    }
    
    private suspend fun loadLeaderboardTop3() {
        try {
            val response = com.ecogo.api.RetrofitClient.apiService.getMobileLeaderboardRankings(
                type = "MONTHLY", page = 0, size = 3
            )
            if (response.code == 200 && response.data != null) {
                val rankings = response.data.rankingsPage.content
                if (rankings.isNotEmpty()) {
                    val r1 = rankings[0]
                    binding.textRank1Name.text = r1.nickname
                    binding.textRank1Subtitle.text = String.format(CO2_FORMAT, r1.carbonSaved)
                }
                if (rankings.size > 1) {
                    val r2 = rankings[1]
                    binding.textRank2Name.text = r2.nickname
                    binding.textRank2Subtitle.text = String.format(CO2_FORMAT, r2.carbonSaved)
                }
                if (rankings.size > 2) {
                    val r3 = rankings[2]
                    binding.textRank3Name.text = r3.nickname
                    binding.textRank3Subtitle.text = String.format(CO2_FORMAT, r3.carbonSaved)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MonthlyHighlights", "Error loading leaderboard top 3", e)
        }
    }

    private fun setupAnimations() {
        val slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        val popIn = AnimationUtils.loadAnimation(requireContext(), R.anim.pop_in)
        
        binding.cardHeader.startAnimation(slideUp)
        binding.cardStats.startAnimation(popIn)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Monthly statistics data class
data class MonthStat(
    val icon: String,
    val title: String,
    val value: String,
    val subtitle: String,
    val color: String
)
