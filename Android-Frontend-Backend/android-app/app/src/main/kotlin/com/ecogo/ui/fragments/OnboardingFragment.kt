package com.ecogo.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.ecogo.R
import com.ecogo.databinding.FragmentOnboardingBinding
import com.ecogo.ui.adapters.OnboardingAdapter

class OnboardingFragment : Fragment() {
    
    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: OnboardingAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d("DEBUG_ONBOARDING", "OnboardingFragment onViewCreated")
        
        adapter = OnboardingAdapter()
        binding.viewPager.adapter = adapter
        
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateProgressDots(position)
                if (position == 4) {  // Changed to 4 since there are now 5 pages (0-4)
                    binding.buttonNext.text = getString(R.string.onboarding_get_started)
                    binding.buttonNext.icon = null
                } else {
                    binding.buttonNext.text = getString(R.string.onboarding_next)
                    binding.buttonNext.setIconResource(R.drawable.ic_chevron_right)
                }
            }
        })
        
        updateProgressDots(0)
        
        binding.buttonNext.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem < 4) {  // Changed to 4
                binding.viewPager.setCurrentItem(currentItem + 1, true)
            } else {
                Log.d("DEBUG_ONBOARDING", "Next button clicked - attempting navigate to home")
                Toast.makeText(requireContext(), "Navigating to home...", Toast.LENGTH_SHORT).show()
                try {
                    findNavController().navigate(R.id.action_onboarding_to_home)
                    Log.d("DEBUG_ONBOARDING", "Navigate to home completed successfully")
                    Toast.makeText(requireContext(), "Navigation command executed", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("DEBUG_ONBOARDING", "Navigation to home FAILED: ${e.message}", e)
                    Toast.makeText(requireContext(), "Navigation error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        
        binding.textSkip.setOnClickListener {
            Log.d("DEBUG_ONBOARDING", "Skip button clicked - attempting navigate to home")
            try {
                findNavController().navigate(R.id.action_onboarding_to_home)
                Log.d("DEBUG_ONBOARDING", "Navigate to home completed from skip")
            } catch (e: Exception) {
                Log.e("DEBUG_ONBOARDING", "Navigation to home from skip FAILED: ${e.message}", e)
                Toast.makeText(requireContext(), "Navigation error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateProgressDots(position: Int) {
        // Note: layout only has 3 dots but we have 5 pages of content
        // Keeping as-is for simplicity; could be dynamically updated
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3)
        
        // Map 5 pages to 3 dots: pages 0-1 -> dot 0, page 2 -> dot 1, pages 3-4 -> dot 2
        val dotPosition = when (position) {
            0, 1 -> 0
            2 -> 1
            3, 4 -> 2
            else -> 0
        }
        
        dots.forEachIndexed { index, dot ->
            dot.setBackgroundResource(
                if (index == dotPosition) R.drawable.progress_dot_active else R.drawable.progress_dot
            )
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
