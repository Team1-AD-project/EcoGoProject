package com.ecogo.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecogo.R
import com.ecogo.databinding.FragmentActivityDetailBinding
import com.ecogo.mapengine.ui.map.MapActivity
import com.ecogo.repository.EcoGoRepository
import com.ecogo.ui.adapters.FriendAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Activity Detail Page
 * Displays full activity information, supports join, check-in, and other actions
 */
class ActivityDetailFragment : Fragment() {

    private var _binding: FragmentActivityDetailBinding? = null
    private val binding get() = _binding!!
    
    private val args: ActivityDetailFragmentArgs by navArgs()
    private val repository = EcoGoRepository()
    
    private var activityId: String = ""
    private var isJoined = false
    private var activityStatus = "PUBLISHED"
    private var activityLat: Double? = null
    private var activityLng: Double? = null
    private var activityLocationName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        activityId = args.activityId
        
        setupUI()
        loadActivityDetail()
        setupAnimations()
    }
    
    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnJoin.setOnClickListener {
            if (isJoined) {
                leaveActivity()
            } else {
                joinActivity()
            }
        }
        
        binding.btnStartRoute.setOnClickListener {
            val lat = activityLat
            val lng = activityLng
            if (lat != null && lng != null) {
                val intent = Intent(requireContext(), MapActivity::class.java).apply {
                    putExtra(MapActivity.EXTRA_DEST_LAT, lat)
                    putExtra(MapActivity.EXTRA_DEST_LNG, lng)
                    putExtra(MapActivity.EXTRA_DEST_NAME, activityLocationName ?: "Activity Location")
                }
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "No location coordinates set for this activity", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnShare.setOnClickListener {
            // TODO: Share functionality (to be implemented in phase 3)
            android.widget.Toast.makeText(
                requireContext(),
                "Share functionality coming soon",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun loadActivityDetail() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.getActivityById(activityId)
            result.onSuccess { activity ->
                // Display activity information
                binding.textTitle.text = activity.title
                binding.textDescription.text = activity.description
                
                // Activity type and status
                binding.textType.text = when (activity.type) {
                    "ONLINE" -> "Online Activity"
                    "OFFLINE" -> "Offline Activity"
                    else -> activity.type
                }
                
                activityStatus = activity.status ?: "PUBLISHED"
                binding.textStatus.text = when (activityStatus) {
                    "DRAFT" -> "Draft"
                    "PUBLISHED" -> "Published"
                    "ONGOING" -> "Ongoing"
                    "ENDED" -> "Ended"
                    else -> activity.status
                }
                
                // Save coordinate information
                activityLat = activity.latitude
                activityLng = activity.longitude
                activityLocationName = activity.locationName

                // Time
                binding.textStartTime.text = activity.startTime ?: "TBD"
                binding.textEndTime.text = activity.endTime ?: "TBD"
                
                // Reward
                binding.textReward.text = "+${activity.rewardCredits} pts"
                
                // Participant count
                val currentParticipants = activity.currentParticipants
                val maxParticipants = activity.maxParticipants ?: Int.MAX_VALUE
                binding.textParticipants.text = "$currentParticipants / ${if (maxParticipants == Int.MAX_VALUE) "∞" else maxParticipants}"
                
                // Progress bar
                if (maxParticipants != Int.MAX_VALUE) {
                    val progress = (currentParticipants.toFloat() / maxParticipants * 100).toInt()
                    binding.progressParticipants.progress = progress
                    binding.progressParticipants.visibility = View.VISIBLE
                } else {
                    binding.progressParticipants.visibility = View.GONE
                }
                
                // Check if user has already joined
                val currentUserId = com.ecogo.auth.TokenManager.getUserId() ?: ""
                isJoined = activity.participantIds.contains(currentUserId)
                updateJoinButton()
                
                // Participant list (simplified, showing first few)
                // TODO: Should load detailed participant user info
                // binding.recyclerParticipants.apply {
                //     layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                //     adapter = FriendAdapter(participants, {}, {})
                // }
                
                // Update button availability
                updateButtonStates()
            }.onFailure { error: Throwable ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Load Failed")
                    .setMessage("Unable to load activity details: ${error.message}")
                    .setPositiveButton("OK") { _, _ ->
                        findNavController().navigateUp()
                    }
                    .show()
            }
        }
    }
    
    private fun updateJoinButton() {
        if (isJoined) {
            binding.btnJoin.text = "Leave Activity"
            binding.btnJoin.setIconResource(R.drawable.ic_check)
        } else {
            binding.btnJoin.text = "Join Activity"
            binding.btnJoin.icon = null
        }
    }
    
    private fun updateButtonStates() {
        // Update button availability based on activity status
        when (activityStatus) {
            "ENDED" -> {
                binding.btnJoin.isEnabled = false
                binding.btnStartRoute.isEnabled = false
            }
            "ONGOING" -> {
                binding.btnJoin.isEnabled = true
                binding.btnStartRoute.isEnabled = isJoined
            }
            else -> {
                binding.btnJoin.isEnabled = true
                binding.btnStartRoute.isEnabled = isJoined
            }
        }
    }
    
    private fun joinActivity() {
        val userId = com.ecogo.auth.TokenManager.getUserId() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.joinActivity(activityId, userId)
            result.onSuccess {
                isJoined = true
                updateJoinButton()
                updateButtonStates()
                
                // Show success message
                val dialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Success")
                    .setMessage("You have successfully joined the activity!")
                    .setPositiveButton("OK", null)
                    .create()
                dialog.show()
                
                // Reload activity detail to update participant count
                loadActivityDetail()
            }.onFailure { error: Throwable ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Join Failed")
                    .setMessage(error.message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun leaveActivity() {
        val userId = com.ecogo.auth.TokenManager.getUserId() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.leaveActivity(activityId, userId)
            result.onSuccess {
                isJoined = false
                updateJoinButton()
                updateButtonStates()
                
                android.widget.Toast.makeText(
                    requireContext(),
                    "Left the activity",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
                // Reload activity detail to update participant count
                loadActivityDetail()
            }.onFailure { error: Throwable ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Leave Failed")
                    .setMessage(error.message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun setupAnimations() {
        val slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        binding.cardInfo.startAnimation(slideUp)
        
        val popIn = AnimationUtils.loadAnimation(requireContext(), R.anim.pop_in)
        binding.cardDetails.startAnimation(popIn)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
