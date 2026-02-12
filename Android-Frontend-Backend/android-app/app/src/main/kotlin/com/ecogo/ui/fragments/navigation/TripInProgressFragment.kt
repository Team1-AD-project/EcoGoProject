package com.ecogo.ui.fragments.navigation

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecogo.R
import com.ecogo.data.MascotEmotion
import com.ecogo.data.MascotSize
import com.ecogo.data.NavigationState
import com.ecogo.databinding.FragmentTripInProgressBinding
import com.ecogo.ui.adapters.RouteStepAdapter
import com.ecogo.viewmodel.NavigationViewModel

/**
 * Trip in progress page - Gamification focus
 * Real-time progress display, dynamic feedback, points accumulation, etc.
 */
class TripInProgressFragment : Fragment() {

    private var _binding: FragmentTripInProgressBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: NavigationViewModel
    private lateinit var stepAdapter: RouteStepAdapter
    
    private var currentPoints = 0
    private var lastMilestoneDistance = 0.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripInProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[NavigationViewModel::class.java]
        
        setupMascot()
        setupRecyclerView()
        setupUI()
        observeViewModel()
    }
    
    private fun setupMascot() {
        binding.mascotLive.apply {
            mascotSize = MascotSize.MEDIUM
            setEmotion(MascotEmotion.NORMAL)
        }
    }
    
    private fun setupRecyclerView() {
        stepAdapter = RouteStepAdapter(emptyList())
        binding.recyclerNextSteps.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = stepAdapter
        }
    }
    
    private fun setupUI() {
        binding.btnCompleteTrip.setOnClickListener {
            completeTrip()
        }
        
        binding.btnCancelTrip.setOnClickListener {
            // Cancel trip
            viewModel.cancelNavigation()
            findNavController().navigateUp()
        }
    }
    
    private fun observeViewModel() {
        // Observe navigation state
        viewModel.navigationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                NavigationState.NAVIGATING -> {
                    updateNavigatingUI()
                }
                NavigationState.COMPLETED -> {
                    navigateToSummary()
                }
                else -> {}
            }
        }
        
        // Observe current route
        viewModel.currentRoute.observe(viewLifecycleOwner) { route ->
            route?.let {
                binding.textDestination.text = it.destination.name
                // Update step list (show only first 3 steps)
                stepAdapter.updateSteps(it.steps.take(3))
            }
        }
        
        // Observe current trip
        viewModel.currentTrip.observe(viewLifecycleOwner) { trip ->
            trip?.let {
                // Update progress bar
                val progress = if (it.route.distance > 0) {
                    ((it.actualDistance / it.route.distance) * 100).toInt()
                } else 0
                
                binding.progressTrip.progress = progress
                binding.textProgress.text = "$progress%"
                
                // Update distance
                binding.textDistanceCovered.text = String.format("%.2f km", it.actualDistance)
                binding.textDistanceRemaining.text = String.format("%.2f km", 
                    it.route.distance - it.actualDistance)
                
                // Check milestone
                checkMilestone(it.actualDistance)
            }
        }
        
        // Observe real-time carbon reduction
        viewModel.realTimeCarbonSaved.observe(viewLifecycleOwner) { carbon ->
            val points = (carbon * 0.5).toInt()
            if (points > currentPoints) {
                animatePointsIncrease(currentPoints, points)
                currentPoints = points
            }
            binding.textCarbonSaved.text = String.format("%.0f g", carbon)
        }
    }
    
    private fun updateNavigatingUI() {
        binding.mascotLive.setEmotion(MascotEmotion.HAPPY)
    }
    
    private fun showOffRouteAlert() {
        // Off-route alert
        binding.mascotLive.setEmotion(MascotEmotion.CONFUSED)
        Toast.makeText(requireContext(), "You seem to have gone off route", Toast.LENGTH_SHORT).show()
    }
    
    private fun checkMilestone(distance: Double) {
        // Show milestone popup every 1 km
        val currentKm = distance.toInt()
        val lastKm = lastMilestoneDistance.toInt()
        
        if (currentKm > lastKm && currentKm > 0) {
            lastMilestoneDistance = distance
            showMilestoneDialog(currentKm)
        }
    }
    
    private fun showMilestoneDialog(km: Int) {
        // Mascot celebration animation
        binding.mascotLive.celebrateAnimation()
        
        Toast.makeText(
            requireContext(),
            "Great job! Completed $km km\nCurrent points +$currentPoints",
            Toast.LENGTH_LONG
        ).show()
    }
    
    private fun animatePointsIncrease(from: Int, to: Int) {
        val animator = ValueAnimator.ofInt(from, to)
        animator.duration = 500
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            binding.textPointsRealtime.text = "+$value"
        }
        animator.start()
        
        // Add blink effect
        val popIn = AnimationUtils.loadAnimation(requireContext(), R.anim.pop_in)
        binding.textPointsRealtime.startAnimation(popIn)
    }
    
    private fun completeTrip() {
        // Complete trip
        viewModel.endNavigation()
        navigateToSummary()
    }
    
    private fun navigateToSummary() {
        val action = TripInProgressFragmentDirections.actionInProgressToSummary()
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
