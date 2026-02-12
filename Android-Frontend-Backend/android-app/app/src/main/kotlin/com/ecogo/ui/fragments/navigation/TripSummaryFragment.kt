package com.ecogo.ui.fragments.navigation

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.ecogo.R
import com.ecogo.data.MascotEmotion
import com.ecogo.data.MascotSize
import com.ecogo.databinding.FragmentTripSummaryBinding
import com.ecogo.ui.dialogs.AchievementUnlockDialog
import com.ecogo.viewmodel.NavigationViewModel

/**
 * Trip summary page - Reward ceremony
 * Shows trip statistics, earned points and achievements
 */
class TripSummaryFragment : Fragment() {

    private var _binding: FragmentTripSummaryBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: NavigationViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[NavigationViewModel::class.java]
        
        setupMascot()
        setupUI()
        displaySummary()
        setupAnimations()
    }
    
    private fun setupMascot() {
        binding.mascotCelebrate.apply {
            mascotSize = MascotSize.LARGE
            setEmotion(MascotEmotion.CELEBRATING)
            // Play celebration animation
            celebrateAnimation()
            
            val spin = AnimationUtils.loadAnimation(requireContext(), R.anim.spin)
            startAnimation(spin)
        }
    }
    
    private fun setupUI() {
        binding.btnClose.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnViewLeaderboard.setOnClickListener {
            findNavController().navigate(R.id.communityFragment)
        }
        
        binding.btnRedeem.setOnClickListener {
            findNavController().navigate(R.id.voucherFragment)
        }
        
        binding.btnAgain.setOnClickListener {
            // Return to route planner
            findNavController().navigate(R.id.routePlannerFragment)
        }
        
        binding.btnShare.setOnClickListener {
            // TODO: Share feature (to be implemented in phase 3)
            // Show temporary message
            android.widget.Toast.makeText(
                requireContext(),
                "Share feature coming in a future update",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun displaySummary() {
        viewModel.currentTrip.observe(viewLifecycleOwner) { trip ->
            trip?.let {
                // Only show stats when trip is completed
                if (it.status != com.ecogo.data.TripStatus.COMPLETED) return@observe
                
                // Display trip statistics
                binding.textDistance.text = String.format("%.2f", it.actualDistance)
                binding.textDuration.text = formatDuration(it.endTime!! - it.startTime)
                binding.textCarbonSaved.text = String.format("%.0f", it.actualCarbonSaved)
                
                // Animate points display
                animatePointsIncrease(0, it.pointsEarned)
                
                // If there's an achievement unlock
                if (it.achievementUnlocked != null) {
                    showAchievementDialog(it.achievementUnlocked!!)
                }
                
                // Calculate eco rating
                val rating = calculateEcoRating(it.actualCarbonSaved)
                binding.textEcoRating.text = rating
                updateRatingColor(rating)
            }
        }
    }
    
    private fun setupAnimations() {
        // Show stat cards one by one
        val popIn = AnimationUtils.loadAnimation(requireContext(), R.anim.pop_in)
        
        binding.root.postDelayed({
            binding.cardDistance.visibility = View.VISIBLE
            binding.cardDistance.startAnimation(popIn)
        }, 200)
        
        binding.root.postDelayed({
            binding.cardDuration.visibility = View.VISIBLE
            binding.cardDuration.startAnimation(popIn)
        }, 400)
        
        binding.root.postDelayed({
            binding.cardCarbon.visibility = View.VISIBLE
            binding.cardCarbon.startAnimation(popIn)
        }, 600)
        
        binding.root.postDelayed({
            binding.cardPoints.visibility = View.VISIBLE
            binding.cardPoints.startAnimation(popIn)
        }, 800)
    }
    
    private fun animatePointsIncrease(from: Int, to: Int) {
        val animator = ValueAnimator.ofInt(from, to)
        animator.duration = 1500
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            binding.textPoints.text = "+$value"
        }
        animator.start()
    }
    
    private fun showAchievementDialog(achievementId: String) {
        // Delay showing achievement unlock dialog
        binding.root.postDelayed({
            val achievement = com.ecogo.data.MockData.ACHIEVEMENTS.find { it.id == achievementId }
            achievement?.let {
                val dialog = AchievementUnlockDialog(
                    requireContext(),
                    it,
                    onDismiss = {}
                )
                dialog.show()
            }
        }, 1500)
    }
    
    private fun formatDuration(durationMillis: Long): String {
        val minutes = (durationMillis / 1000 / 60).toInt()
        val seconds = ((durationMillis / 1000) % 60).toInt()
        return if (minutes > 0) {
            "$minutes"
        } else {
            "< 1"
        }
    }
    
    private fun calculateEcoRating(carbonSaved: Double): String {
        return when {
            carbonSaved >= 500 -> "A+"
            carbonSaved >= 300 -> "A"
            carbonSaved >= 200 -> "B+"
            carbonSaved >= 100 -> "B"
            else -> "C"
        }
    }
    
    private fun updateRatingColor(rating: String) {
        val color = when (rating) {
            "A+", "A" -> R.color.primary
            "B+", "B" -> R.color.secondary
            else -> R.color.warning
        }
        binding.textEcoRating.setTextColor(requireContext().getColor(color))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
