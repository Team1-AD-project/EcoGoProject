package com.ecogo.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.core.content.ContextCompat
import com.ecogo.data.MockData
import com.ecogo.databinding.FragmentProfileBinding
import com.ecogo.ui.adapters.AchievementAdapter
import com.ecogo.ui.adapters.HistoryAdapter
import com.ecogo.ui.adapters.ShopItemAdapter
import com.ecogo.repository.EcoGoRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var currentPoints = 1250
    private val repository = EcoGoRepository()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupShopRecyclerView()
        setupBadgeRecyclerView()
        setupHistoryRecyclerView()
        setupTabs()
        setupAnimations()
        setupActions()
        loadHistory()
    }
    
    private fun setupUI() {
        binding.textPoints.text = "$currentPoints pts"
        binding.textName.text = "Alex Tan"
        binding.textFaculty.text = "Computer Science • Year 2"
    }
    
    private fun setupShopRecyclerView() {
        val adapter = ShopItemAdapter(MockData.SHOP_ITEMS) { item ->
            if (currentPoints >= item.cost) {
                currentPoints -= item.cost
                binding.textPoints.text = "$currentPoints pts"
                // In real app, update item.owned and refresh adapter
            }
        }
        
        binding.recyclerShop.apply {
            layoutManager = GridLayoutManager(context, 2)
            this.adapter = adapter
        }
    }

    private fun setupBadgeRecyclerView() {
        binding.recyclerAchievements.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = AchievementAdapter(MockData.ACHIEVEMENTS)
        }
    }

    private fun setupHistoryRecyclerView() {
        binding.recyclerHistory.apply {
            layoutManager = GridLayoutManager(context, 1)
            adapter = HistoryAdapter(MockData.HISTORY)
        }
    }

    private fun loadHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            val history = repository.getHistory().getOrElse { MockData.HISTORY }
            binding.recyclerHistory.adapter = HistoryAdapter(history)
        }
    }

    private fun setupTabs() {
        fun setActiveTab(tab: String) {
            binding.recyclerShop.visibility = if (tab == "closet") View.VISIBLE else View.GONE
            binding.recyclerAchievements.visibility = if (tab == "badges") View.VISIBLE else View.GONE
            binding.recyclerHistory.visibility = if (tab == "history") View.VISIBLE else View.GONE

            val primary = ContextCompat.getColor(requireContext(), com.ecogo.R.color.primary)
            val secondary = ContextCompat.getColor(requireContext(), com.ecogo.R.color.text_secondary)
            val transparent = ContextCompat.getColor(requireContext(), android.R.color.transparent)

            binding.tabCloset.setTextColor(if (tab == "closet") primary else secondary)
            binding.tabBadges.setTextColor(if (tab == "badges") primary else secondary)
            binding.tabHistory.setTextColor(if (tab == "history") primary else secondary)

            binding.tabCloset.setTypeface(null, if (tab == "closet") android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            binding.tabBadges.setTypeface(null, if (tab == "badges") android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            binding.tabHistory.setTypeface(null, if (tab == "history") android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

            binding.tabClosetIndicator.setBackgroundColor(if (tab == "closet") primary else transparent)
            binding.tabBadgesIndicator.setBackgroundColor(if (tab == "badges") primary else transparent)
            binding.tabHistoryIndicator.setBackgroundColor(if (tab == "history") primary else transparent)
        }

        binding.tabCloset.setOnClickListener { setActiveTab("closet") }
        binding.tabBadges.setOnClickListener { setActiveTab("badges") }
        binding.tabHistory.setOnClickListener { setActiveTab("history") }
        setActiveTab("closet")
    }

    private fun setupAnimations() {
        val breathe = AnimationUtils.loadAnimation(requireContext(), com.ecogo.R.anim.breathe)
        val jump = AnimationUtils.loadAnimation(requireContext(), com.ecogo.R.anim.jump)
        binding.imageAvatar.startAnimation(breathe)
        binding.imageAvatar.setOnClickListener {
            binding.imageAvatar.startAnimation(jump)
        }
    }

    private fun setupActions() {
        binding.buttonSettings.setOnClickListener {
            findNavController().navigate(com.ecogo.R.id.action_profile_to_settings)
        }
        binding.buttonRedeem.setOnClickListener {
            findNavController().navigate(com.ecogo.R.id.voucherFragment)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
