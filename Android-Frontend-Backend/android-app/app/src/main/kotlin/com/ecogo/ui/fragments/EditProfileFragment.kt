package com.ecogo.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ecogo.R
import com.ecogo.api.MobileProfileResponse

import com.ecogo.api.TransportPreferencesWrapper

import com.ecogo.api.UpdateProfileRequest
import com.ecogo.auth.TokenManager
import com.ecogo.databinding.FragmentEditProfileBinding
import com.ecogo.repository.EcoGoRepository
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val repository = EcoGoRepository()

    // Cached profile data for comparison
    private var cachedProfile: MobileProfileResponse? = null

    companion object {
        private const val TAG = "EditProfileFragment"

        // NUS Faculty list
        val FACULTIES = listOf(
            "School of Computing",
            "Faculty of Engineering",
            "Faculty of Science",
            "Faculty of Arts and Social Sciences",
            "Business School",
            "Faculty of Law",
            "Faculty of Dentistry",
            "Yong Loo Lin School of Medicine",
            "School of Design and Environment",
            "Yong Siew Toh Conservatory of Music",
            "Faculty of Nursing",
            "School of Public Health",
            "School of Continuing and Lifelong Education",
            "College of Humanities and Sciences"
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Selected transport modes
    private val selectedTransportModes = mutableSetOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFacultyDropdown()
        setupTransportList()
        setupActions()

        // First load local cache as fallback, then overlay with API data
        loadLocalFallback()
        loadProfile()
    }

    private fun setupTransportList() {
        binding.recyclerTransportModes.layoutManager = 
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())
    }

    private fun setupFacultyDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            FACULTIES
        )
        binding.editFaculty.setAdapter(adapter)
    }

    private fun setupActions() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSaveBottom.setOnClickListener {
            saveProfile()
        }
    }

    /**
     * Load local fallback data from SharedPreferences.
     * Tries both "ecogo_profile" and "EcoGoPrefs" (registration wizard) as sources.
     */
    private fun loadLocalFallback() {
        // Try ecogo_profile first (saved from previous edits)
        val editPrefs = requireContext().getSharedPreferences("ecogo_profile", android.content.Context.MODE_PRIVATE)
        // Then try EcoGoPrefs (saved during registration wizard)
        val regPrefs = requireContext().getSharedPreferences("EcoGoPrefs", android.content.Context.MODE_PRIVATE)

        // Nickname: from registration prefs
        val nickname = regPrefs.getString("username", null)
        if (!nickname.isNullOrEmpty()) {
            binding.editNickname.setText(nickname)
        }

        // Faculty: from registration prefs
        val faculty = regPrefs.getString("faculty", null)
        if (!faculty.isNullOrEmpty()) {
            binding.editFaculty.setText(faculty, false)
        }

        // Campus info: try edit prefs first, then registration prefs
        val dormitory = editPrefs.getString("dormitoryOrResidence", null)
            ?: regPrefs.getString("dormitory", null)
        val building = editPrefs.getString("mainTeachingBuilding", null)
            ?: regPrefs.getString("teaching_building", null)
        val studySpot = editPrefs.getString("favoriteStudySpot", null)
            ?: regPrefs.getString("study_spot", null)

        if (!dormitory.isNullOrEmpty()) binding.editDormitory.setText(dormitory)
        if (!building.isNullOrEmpty()) binding.editTeachingBuilding.setText(building)
        if (!studySpot.isNullOrEmpty()) binding.editStudySpot.setText(studySpot)

        // Weekly goals
        val weeklyGoals = editPrefs.getInt("weeklyGoals", -1).let {
            if (it > 0) it else regPrefs.getInt("weekly_goal", 20)
        }
        binding.editWeeklyGoals.setText(weeklyGoals.toString())

        // Transport prefs from registration
        val transportSet = regPrefs.getStringSet("transport_prefs", null)
        if (!transportSet.isNullOrEmpty()) {
            selectedTransportModes.addAll(transportSet)
        }

        // Notification preferences
        binding.switchNewChallenges.isChecked = editPrefs.getBoolean("newChallenges",
            regPrefs.getBoolean("notify_challenges", true))
        binding.switchActivityReminders.isChecked = editPrefs.getBoolean("activityReminders",
            regPrefs.getBoolean("notify_reminders", true))
        binding.switchFriendActivity.isChecked = editPrefs.getBoolean("friendActivity",
            regPrefs.getBoolean("notify_friends", false))

        Log.d(TAG, "Local fallback loaded: nickname=$nickname, faculty=$faculty")
    }

    private fun loadProfile() {
        val userId = TokenManager.getUserId()
        if (userId == null) {
            Log.w(TAG, "UserId is null, cannot load profile from API")
            Toast.makeText(context, "Please log in to load profile data", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Loading profile from API for userId: $userId")

        // Show loading state
        binding.btnSaveBottom.isEnabled = false
        binding.btnSaveBottom.text = "Loading..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Load Transport Modes first
                val modesResult = repository.getTransportModes()
                val availableModes = modesResult.getOrNull() ?: listOf("walk", "bike", "bus", "subway", "car", "electric_bike")

                val result = repository.getMobileUserProfile()
                val profile = result.getOrNull()

                if (profile != null) {
                    cachedProfile = profile
                    populateFields(profile, availableModes)
                    Log.d(TAG, "Profile loaded from API: ${profile.userInfo.nickname}")
                } else {
                    val error = result.exceptionOrNull()
                    Log.w(TAG, "Failed to load profile from API: ${error?.message}")
                    Log.w(TAG, "Using local fallback data. Fields may be incomplete.")
                    Toast.makeText(context, "Could not load profile from server, showing local data", Toast.LENGTH_LONG).show()

                    // Still setup transport modes adapter with local data
                    setupTransportAdapter(availableModes)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile: ${e.message}", e)
                Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()

                // Setup transport adapter with fallback
                val fallbackModes = listOf("walk", "bike", "bus", "subway", "car", "electric_bike")
                setupTransportAdapter(fallbackModes)
            } finally {
                if (_binding != null) {
                    binding.btnSaveBottom.isEnabled = true
                    binding.btnSaveBottom.text = getString(R.string.edit_profile_save_changes)
                }
            }
        }
    }

    private fun populateFields(profile: MobileProfileResponse, availableModes: List<String>) {
        val userInfo = profile.userInfo
        val prefs = userInfo.preferences

        // Basic info — overwrite local fallback with API data
        binding.editNickname.setText(userInfo.nickname)
        binding.editFaculty.setText(userInfo.faculty ?: "", false)

        // Campus info — only overwrite if API has non-null values
        if (!prefs?.dormitoryOrResidence.isNullOrEmpty()) {
            binding.editDormitory.setText(prefs?.dormitoryOrResidence ?: "")
        }
        if (!prefs?.mainTeachingBuilding.isNullOrEmpty()) {
            binding.editTeachingBuilding.setText(prefs?.mainTeachingBuilding ?: "")
        }
        if (!prefs?.favoriteStudySpot.isNullOrEmpty()) {
            binding.editStudySpot.setText(prefs?.favoriteStudySpot ?: "")
        }
        
        // Weekly goal
        val goal = prefs?.weeklyGoals ?: 20
        if (goal > 0) {
            binding.editWeeklyGoals.setText(goal.toString())
        }

        // Transport preferences — merge API data with local
        prefs?.preferredTransport?.let { transports ->
            selectedTransportModes.clear()
            selectedTransportModes.addAll(transports)
        }
        
        // Setup RecyclerView Adapter
        setupTransportAdapter(availableModes)

        // Notification preferences
        binding.switchNewChallenges.isChecked = prefs?.newChallenges ?: true
        binding.switchActivityReminders.isChecked = prefs?.activityReminders ?: true
        binding.switchFriendActivity.isChecked = prefs?.friendActivity ?: false
    }

    /**
     * Setup transport mode RecyclerView adapter
     */
    private fun setupTransportAdapter(availableModes: List<String>) {
        val adapter = com.ecogo.ui.adapters.TransportModeAdapter(availableModes, selectedTransportModes) { _ ->
            // Transport selection updated via the shared set
        }
        binding.recyclerTransportModes.adapter = adapter
    }

    private fun saveProfile() {
        val userId = TokenManager.getUserId()
        if (userId == null) {
            Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Gather data from fields
        val nickname = binding.editNickname.text?.toString()?.trim() ?: ""
        val faculty = binding.editFaculty.text?.toString()?.trim()
        val dormitory = binding.editDormitory.text?.toString()?.trim()
        val teachingBuilding = binding.editTeachingBuilding.text?.toString()?.trim()
        val studySpot = binding.editStudySpot.text?.toString()?.trim()
        val weeklyGoalsText = binding.editWeeklyGoals.text?.toString()?.trim()
        val weeklyGoals = weeklyGoalsText?.toIntOrNull() ?: 20

        // Validate nickname
        if (nickname.isEmpty()) {
            binding.layoutNickname.error = getString(R.string.edit_profile_error_nickname)
            return
        } else {
            binding.layoutNickname.error = null
        }

        // Transport preferences are already in selectedTransportModes

        val newChallenges = binding.switchNewChallenges.isChecked
        val activityReminders = binding.switchActivityReminders.isChecked
        val friendActivity = binding.switchFriendActivity.isChecked

        // Build request using new wrapper
        val request = UpdateProfileRequest(
            nickname = nickname,
            faculty = faculty?.ifEmpty { null },
            preferences = com.ecogo.api.UpdatePreferencesWrapper(
                preferredTransport = selectedTransportModes.toList().ifEmpty { null },
                dormitoryOrResidence = dormitory?.ifEmpty { null },
                mainTeachingBuilding = teachingBuilding?.ifEmpty { null },
                favoriteStudySpot = studySpot?.ifEmpty { null },
                weeklyGoals = weeklyGoals,
                newChallenges = newChallenges,
                activityReminders = activityReminders,
                friendActivity = friendActivity
            )

        )

        // Disable save buttons during request
        binding.btnSaveBottom.isEnabled = false
        binding.btnSaveBottom.text = getString(R.string.edit_profile_saving)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = repository.updateUserProfile(userId, request)

                if (result.isSuccess) {
                    // Save to local SharedPreferences for immediate access
                    saveLocalPreferences(
                        nickname = nickname,
                        faculty = faculty ?: "",
                        dormitory = dormitory ?: "",
                        teachingBuilding = teachingBuilding ?: "",
                        studySpot = studySpot ?: "",
                        weeklyGoals = weeklyGoals,
                        newChallenges = newChallenges,
                        activityReminders = activityReminders,
                        friendActivity = friendActivity
                    )

                    Log.d(TAG, "Profile updated successfully")
                    Toast.makeText(context, getString(R.string.edit_profile_save_success), Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e(TAG, "Failed to update profile: $error")
                    Toast.makeText(context, getString(R.string.edit_profile_save_error), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving profile: ${e.message}", e)
                Toast.makeText(context, getString(R.string.edit_profile_save_error), Toast.LENGTH_SHORT).show()
            } finally {
                if (_binding != null) {
                    binding.btnSaveBottom.isEnabled = true
                    binding.btnSaveBottom.text = getString(R.string.edit_profile_save_changes)
                }
            }
        }
    }

    /**
     * Save to BOTH SharedPreferences stores for consistency
     */
    private fun saveLocalPreferences(
        nickname: String,
        faculty: String,
        dormitory: String,
        teachingBuilding: String,
        studySpot: String,
        weeklyGoals: Int,
        newChallenges: Boolean,
        activityReminders: Boolean,
        friendActivity: Boolean
    ) {
        // Save to ecogo_profile (for Edit Profile fallback)
        requireContext().getSharedPreferences("ecogo_profile", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("dormitoryOrResidence", dormitory)
            .putString("mainTeachingBuilding", teachingBuilding)
            .putString("favoriteStudySpot", studySpot)
            .putInt("weeklyGoals", weeklyGoals)
            .putBoolean("newChallenges", newChallenges)
            .putBoolean("activityReminders", activityReminders)
            .putBoolean("friendActivity", friendActivity)
            .apply()

        // Also update EcoGoPrefs (used by registration wizard and other fragments)
        requireContext().getSharedPreferences("EcoGoPrefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("username", nickname)
            .putString("faculty", faculty)
            .putString("dormitory", dormitory)
            .putString("teaching_building", teachingBuilding)
            .putString("study_spot", studySpot)
            .putInt("weekly_goal", weeklyGoals)
            .putBoolean("notify_challenges", newChallenges)
            .putBoolean("notify_reminders", activityReminders)
            .putBoolean("notify_friends", friendActivity)
            .apply()

        Log.d(TAG, "Local preferences saved to both stores")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
