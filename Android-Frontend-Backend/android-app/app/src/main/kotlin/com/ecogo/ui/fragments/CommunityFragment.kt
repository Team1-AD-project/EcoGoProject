package com.ecogo.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecogo.data.FacultyCarbonData
import com.ecogo.databinding.FragmentCommunityBinding
import com.ecogo.ui.adapters.CommunityAdapter
import com.ecogo.ui.adapters.LeaderboardAdapter
import com.ecogo.repository.EcoGoRepository
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class CommunityFragment : Fragment() {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!
    private val repository = EcoGoRepository()
    private var currentIndividualType = "DAILY" // DAILY or MONTHLY

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTabs()
        setupRecyclerView()
        setupAnimations()
        setupActions()
        loadFacultyLeaderboard()
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showFacultyLeaderboard()
                    1 -> showIndividualLeaderboard()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })
    }

    private fun showFacultyLeaderboard() {
        binding.layoutFacultyLeaderboard.visibility = View.VISIBLE
        binding.layoutIndividualLeaderboard.visibility = View.GONE
        loadFacultyLeaderboard()
    }

    private fun showIndividualLeaderboard() {
        binding.layoutFacultyLeaderboard.visibility = View.GONE
        binding.layoutIndividualLeaderboard.visibility = View.VISIBLE
        setupIndividualToggle()
        loadIndividualRankings(currentIndividualType)
    }

    private fun setupRecyclerView() {
        binding.recyclerCommunity.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = CommunityAdapter(emptyList())
        }

        binding.recyclerIndividual.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = LeaderboardAdapter(emptyList())
        }
    }

    private fun setupIndividualToggle() {
        binding.btnDaily.setOnClickListener {
            currentIndividualType = "DAILY"
            updateToggleStyle()
            loadIndividualRankings("DAILY")
        }

        binding.btnMonthly.setOnClickListener {
            currentIndividualType = "MONTHLY"
            updateToggleStyle()
            loadIndividualRankings("MONTHLY")
        }

        updateToggleStyle()
    }

    private fun updateToggleStyle() {
        val ctx = requireContext()
        if (currentIndividualType == "DAILY") {
            binding.btnDaily.backgroundTintList =
                ContextCompat.getColorStateList(ctx, com.ecogo.R.color.primary)
            binding.btnDaily.setTextColor(ContextCompat.getColor(ctx, android.R.color.white))

            binding.btnMonthly.backgroundTintList =
                ContextCompat.getColorStateList(ctx, android.R.color.transparent)
            binding.btnMonthly.setTextColor(
                ContextCompat.getColor(ctx, com.ecogo.R.color.text_secondary)
            )
            binding.btnMonthly.strokeColor =
                ContextCompat.getColorStateList(ctx, com.ecogo.R.color.text_secondary)
        } else {
            binding.btnMonthly.backgroundTintList =
                ContextCompat.getColorStateList(ctx, com.ecogo.R.color.primary)
            binding.btnMonthly.setTextColor(ContextCompat.getColor(ctx, android.R.color.white))

            binding.btnDaily.backgroundTintList =
                ContextCompat.getColorStateList(ctx, android.R.color.transparent)
            binding.btnDaily.setTextColor(
                ContextCompat.getColor(ctx, com.ecogo.R.color.text_secondary)
            )
            binding.btnDaily.strokeColor =
                ContextCompat.getColorStateList(ctx, com.ecogo.R.color.text_secondary)
        }
    }

    private fun setupActions() {
        // Challenges button
        binding.btnChallenges.setOnClickListener {
            findNavController().navigate(com.ecogo.R.id.action_community_to_challenges)
        }

        // Activities button
        binding.btnActivities.setOnClickListener {
            findNavController().navigate(com.ecogo.R.id.action_community_to_activities)
        }
    }

    // =============== Faculty Leaderboard ===============

    private fun loadFacultyLeaderboard() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressFaculty.visibility = View.VISIBLE
            binding.recyclerCommunity.visibility = View.GONE

            val result = repository.getFacultyMonthlyCarbonStats()

            binding.progressFaculty.visibility = View.GONE

            val faculties = result.getOrNull()
            if (faculties != null && faculties.isNotEmpty()) {
                // Data is already sorted descending from repository
                binding.recyclerCommunity.adapter = CommunityAdapter(faculties)
                binding.recyclerCommunity.visibility = View.VISIBLE

                // Update leader card
                val leader = faculties.first()
                binding.textLeaderName.text = leader.faculty
                binding.textLeaderPoints.text = "%.2f kg CO₂ saved this month".format(leader.totalCarbon)
            } else {
                // Fallback: try the old faculties API
                val fallback = repository.getFaculties().getOrNull()
                if (fallback != null && fallback.isNotEmpty()) {
                    val carbonList = fallback
                        .map { FacultyCarbonData(faculty = it.name, totalCarbon = it.score.toDouble()) }
                        .sortedByDescending { it.totalCarbon }
                    binding.recyclerCommunity.adapter = CommunityAdapter(carbonList)
                    binding.recyclerCommunity.visibility = View.VISIBLE

                    val leader = carbonList.first()
                    binding.textLeaderName.text = leader.faculty
                    binding.textLeaderPoints.text = "%.2f kg CO₂ saved".format(leader.totalCarbon)
                } else {
                    binding.textLeaderName.text = "—"
                    binding.textLeaderPoints.text = "No data available"
                    binding.recyclerCommunity.adapter = CommunityAdapter(emptyList())
                    binding.recyclerCommunity.visibility = View.VISIBLE
                }
            }
        }
    }

    // =============== Individual Leaderboard ===============

    private fun loadIndividualRankings(type: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressIndividual.visibility = View.VISIBLE
            binding.recyclerIndividual.visibility = View.GONE
            binding.textIndividualEmpty.visibility = View.GONE

            val result = repository.getIndividualRankings(type)

            binding.progressIndividual.visibility = View.GONE

            val stats = result.getOrNull()
            val rankings = stats?.rankingsPage?.content ?: emptyList()

            if (rankings.isEmpty()) {
                binding.textIndividualEmpty.visibility = View.VISIBLE
            } else {
                binding.recyclerIndividual.adapter = LeaderboardAdapter(rankings)
                binding.recyclerIndividual.visibility = View.VISIBLE
            }
        }
    }

    private fun setupAnimations() {
        val popIn = AnimationUtils.loadAnimation(requireContext(), com.ecogo.R.anim.pop_in)
        binding.cardLeader.startAnimation(popIn)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
