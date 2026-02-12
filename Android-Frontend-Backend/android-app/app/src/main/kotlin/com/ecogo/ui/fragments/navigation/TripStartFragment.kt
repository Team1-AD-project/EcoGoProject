package com.ecogo.ui.fragments.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.ecogo.R
import com.ecogo.data.MascotSize
import com.ecogo.databinding.FragmentTripStartBinding
import com.ecogo.viewmodel.NavigationViewModel

/**
 * Trip start confirmation page
 * Shows selected route info, estimated time, distance, points, etc.
 * Includes mascot ready-to-go animation
 */
class TripStartFragment : Fragment() {

    private var _binding: FragmentTripStartBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: NavigationViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripStartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[NavigationViewModel::class.java]
        
        setupMascot()
        setupUI()
        observeViewModel()
        setupAnimations()
    }
    
    private fun setupMascot() {
        binding.mascotStart.apply {
            mascotSize = MascotSize.LARGE
            // Waving ready-to-go animation
            waveAnimation()
        }
    }
    
    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnStartTrip.setOnClickListener {
            // Start button click animation
            val jump = AnimationUtils.loadAnimation(requireContext(), R.anim.jump)
            binding.btnStartTrip.startAnimation(jump)
            
            // Delay trip start slightly to let animation finish
            binding.root.postDelayed({
                startTrip()
            }, 300)
        }
    }
    
    private fun observeViewModel() {
        viewModel.currentRoute.observe(viewLifecycleOwner) { route ->
            route?.let {
                // Display route info
                binding.textRouteName.text = when (it.mode) {
                    com.ecogo.data.TransportMode.WALK -> "Walking Route"
                    com.ecogo.data.TransportMode.CYCLE -> "Cycling Route"
                    com.ecogo.data.TransportMode.BUS -> "Bus Route"
                    com.ecogo.data.TransportMode.MIXED -> "Mixed Route"
                }
                
                binding.textOrigin.text = it.origin.name
                binding.textDestination.text = it.destination.name
                
                // Estimated info
                binding.textEstimatedTime.text = "${it.duration} min"
                binding.textEstimatedDistance.text = String.format("%.1f km", it.distance)
                binding.textCarbonSaved.text = String.format("%.0f g CO₂", it.carbonSaved)
                binding.textPointsEarn.text = "+${it.points} points"
                
                // If there's a recommendation tag
                if (it.badge.isNotEmpty()) {
                    binding.badgeRecommended.visibility = View.VISIBLE
                    binding.badgeRecommended.text = it.badge
                } else {
                    binding.badgeRecommended.visibility = View.GONE
                }
            }
        }
    }
    
    private fun setupAnimations() {
        val popIn = AnimationUtils.loadAnimation(requireContext(), R.anim.pop_in)
        binding.cardRouteInfo.startAnimation(popIn)
        
        val slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        binding.cardEstimates.startAnimation(slideUp)
    }
    
    private fun startTrip() {
        // Start trip, navigate to in-progress page
        val action = TripStartFragmentDirections.actionTripStartToInProgress()
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
