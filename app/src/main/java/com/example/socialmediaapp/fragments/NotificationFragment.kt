package com.example.socialmediaapp.fragments

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialmediaapp.MainActivity
import com.example.socialmediaapp.R
import com.example.socialmediaapp.Utils
import com.example.socialmediaapp.adapters.NotificationAdapter
import com.example.socialmediaapp.databinding.FragmentNotificationBinding
import com.example.socialmediaapp.modal.Notification
import com.example.socialmediaapp.mvvm.ViewModel

class NotificationFragment : Fragment() {
    private lateinit var vm: ViewModel
    private lateinit var binding: FragmentNotificationBinding
    private lateinit var adapter: NotificationAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_NAME = "ThemePrefs"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_notification, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModel()
        setupRecyclerView()
        observeNotifications()

        sharedPreferences = this.requireActivity().getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val mainLayout = this.activity?.findViewById<ConstraintLayout>(R.id.notifications)

        if (sharedPreferences.getBoolean(PREF_NAME, false)) {
            if (mainLayout != null) {
                mainLayout.setBackgroundColor("#3F51B5".toColorInt())
            }
        }
    }

    private fun initViewModel() {
        vm = ViewModelProvider(this).get(ViewModel::class.java)
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter()
        binding.notificationsRecyclerView.adapter = adapter
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Set click listener
        adapter.setOnItemClickListener(object : NotificationAdapter.OnItemClickListener {
            override fun onItemClick(notification: Notification) {
                // Mark notification as seen
                vm.markNotificationAsSeen(notification.id)

                when (notification.type) {
                    "like", "comment" -> {
                        // Navigate to post details
                        navigateToPostDetails(notification.postId)
                    }
                    "follow" -> {
                        // Navigate to user profile
                        navigateToUserProfile(notification.fromUserId)
                    }
                }
            }
        })
    }

    private fun observeNotifications() {
        vm.getNotifications().observe(viewLifecycleOwner, Observer { notifications ->
            if (notifications.isEmpty()) {
                binding.emptyNotificationsText.visibility = View.VISIBLE
            } else {
                binding.emptyNotificationsText.visibility = View.GONE
            }

            adapter.setNotifications(notifications)
        })
    }

    private fun navigateToPostDetails(postId: String) {
        // Intent to navigate to post details
        val intent = Intent(requireActivity(), MainActivity::class.java).apply {
            putExtra("postId", postId)
            putExtra("showPostDetails", true)
        }
        startActivity(intent)
    }

    private fun navigateToUserProfile(userId: String) {
        // Intent to navigate to user profile
        val intent = Intent(requireActivity(), MainActivity::class.java).apply {
            putExtra("userId", userId)
            putExtra("showOtherUser", true)
        }
        startActivity(intent)
    }
}