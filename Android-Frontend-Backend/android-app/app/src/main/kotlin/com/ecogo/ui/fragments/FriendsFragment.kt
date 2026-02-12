package com.ecogo.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecogo.data.MockData
import com.ecogo.databinding.FragmentFriendsBinding
import com.ecogo.repository.EcoGoRepository
import com.ecogo.ui.adapters.FriendAdapter
import com.ecogo.ui.adapters.FriendActivityAdapter
import kotlinx.coroutines.launch

class FriendsFragment : Fragment() {

    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!
    private val repository = EcoGoRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupActions()
        loadData()
    }

    private fun setupRecyclerViews() {
        binding.recyclerFriends.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = FriendAdapter(emptyList(), 
                onMessageClick = { friend ->
                    // Navigate to chat page
                    findNavController().navigate(com.ecogo.R.id.action_friends_to_chat)
                },
                onFriendClick = { friend ->
                    // Navigate to friend details (profile page)
                    findNavController().navigate(com.ecogo.R.id.action_friends_to_profile)
                }
            )
        }

        binding.recyclerFriendActivities.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = FriendActivityAdapter(emptyList())
        }

        binding.recyclerFriendRequests.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = FriendAdapter(emptyList(),
                onMessageClick = { friend ->
                    // Accept friend request
                    acceptFriendRequest(friend)
                },
                onFriendClick = { friend ->
                    // View request details
                    findNavController().navigate(com.ecogo.R.id.action_friends_to_profile)
                }
            )
        }
    }

    private fun setupActions() {
        binding.buttonAddFriend.setOnClickListener {
            // Open add friend dialog or page
            // TODO: Implement add friend functionality
        }
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Load friends list
            val friends = repository.getFriends("user123").getOrElse { MockData.FRIENDS }
            binding.recyclerFriends.adapter = FriendAdapter(friends,
                onMessageClick = { friend ->
                    findNavController().navigate(com.ecogo.R.id.action_friends_to_chat)
                },
                onFriendClick = { friend ->
                    findNavController().navigate(com.ecogo.R.id.action_friends_to_profile)
                }
            )

            // Update friend count
            binding.textFriendCount.text = "${friends.size} friends"

            // Show/hide empty state
            if (friends.isEmpty()) {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.recyclerFriends.visibility = View.GONE
            } else {
                binding.layoutEmptyState.visibility = View.GONE
                binding.recyclerFriends.visibility = View.VISIBLE
            }

            // Load friend activities
            val activities = repository.getFriendActivities("user123").getOrElse { MockData.FRIEND_ACTIVITIES }
            binding.recyclerFriendActivities.adapter = FriendActivityAdapter(activities)
        }
    }

    private fun acceptFriendRequest(friend: com.ecogo.data.Friend) {
        viewLifecycleOwner.lifecycleScope.launch {
            // TODO: Call API to accept friend request
            loadData() // Reload data
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
