package com.example.socialmediaapp.fragments

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmediaapp.MainActivity
import com.example.socialmediaapp.R
import com.example.socialmediaapp.Utils
import com.example.socialmediaapp.activities.BaseActivity
import com.example.socialmediaapp.adapters.MyFeedAdapter
import com.example.socialmediaapp.adapters.onCommentClickListener
import com.example.socialmediaapp.adapters.onLikeClickListener
import com.example.socialmediaapp.adapters.onUserClickListener
import com.example.socialmediaapp.databinding.FragmentHomeBinding
import com.example.socialmediaapp.modal.Feed
import com.example.socialmediaapp.mvvm.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment(), onLikeClickListener, onUserClickListener {
    private lateinit var vm: ViewModel
    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: MyFeedAdapter
    private var scrollPosition: Int = 0
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_NAME = "ThemePrefs"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModel()
        setupFeedAdapter()
        observeFeed()
        setupFilterSpinner()
        setupScrollListener()
        sharedPreferences = this.requireActivity().getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val mainLayout = this.activity?.findViewById<ConstraintLayout>(R.id.home)

        if (sharedPreferences.getBoolean(PREF_NAME, false)) {
            if (mainLayout != null) {
                mainLayout.setBackgroundColor("#3F51B5".toColorInt())
            }
        }
    }



    private fun initViewModel() {
        vm = ViewModelProvider(this).get(ViewModel::class.java)
    }

    private fun setupFeedAdapter() {
        adapter = MyFeedAdapter()
        adapter.setLikeListener(this@HomeFragment)
        adapter.setUserClickListener(this@HomeFragment)
        adapter.setCommentClickListener(object : onCommentClickListener {
            override fun addComment(postId: String, comment: String) {
                if (comment.isNotEmpty()) {
                    vm.addComment(postId, comment)
                    Toast.makeText(requireContext(), "Comment added!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Comment can not be empty!", Toast.LENGTH_SHORT).show()
                }
            }
        })
        binding.feedRecycler.adapter = adapter
    }

    private fun setupScrollListener() {
        binding.feedRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                // Save the current first visible item position
                val layoutManager = recyclerView.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
                scrollPosition = layoutManager.findFirstVisibleItemPosition()
            }
        })
    }

    private fun observeFeed() {
        binding.lifecycleOwner = viewLifecycleOwner
        vm.loadMyFeed().observe(viewLifecycleOwner, Observer { feedList ->
            val previousSize = adapter.feedlist.size

            // Update adapter with new data
            adapter.setFeedList(feedList)

            // Notify adapter of the new data
            if (previousSize == 0) {
                // If this is the first load, use notifyDataSetChanged
                adapter.notifyDataSetChanged()
            } else {
                // Otherwise notify that the dataset has been modified to maintain scroll position
                adapter.notifyItemRangeChanged(0, feedList.size)

                // Restore scroll position
                (binding.feedRecycler.layoutManager as androidx.recyclerview.widget.LinearLayoutManager)
                    .scrollToPosition(scrollPosition)
            }
        })
    }

    private fun setupFilterSpinner() {
        val spinner = requireActivity().findViewById<Spinner>(R.id.filter_spinner)
        val spinnerAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.filter_options,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinner.adapter = spinnerAdapter
        spinner.setSelection(0)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedOption = parent.getItemAtPosition(position).toString()
                Toast.makeText(requireContext(), "Filtered by $selectedOption", Toast.LENGTH_SHORT).show()

                // Save current scroll position before sorting
                val layoutManager = binding.feedRecycler.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
                scrollPosition = layoutManager.findFirstVisibleItemPosition()

                when (selectedOption) {
                    "Newest" -> vm.sortFeedDescendingDate()
                    "Oldest" -> vm.sortFeedAscendingDate()
                    "Most Liked" -> vm.sortFeedMostLiked()
                    "Most Commented" -> vm.sortFeedMostCommented()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No action needed
            }
        }
    }

    override fun onLikeClick(feed: Feed) {
        val currentUserId = Utils.getUiLoggedIn()
        val postId = feed.postid ?: return

        val firestore = FirebaseFirestore.getInstance()
        val postRef = firestore.collection("Posts").document(postId)

        // Find the position of the item in the adapter
        val position = adapter.feedlist.indexOfFirst { it.postid == postId }
        if (position == -1) return // Item not found in list

        // Save the current scroll position
        val layoutManager = binding.feedRecycler.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
        scrollPosition = layoutManager.findFirstVisibleItemPosition()

        postRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val likes = document.getLong("likes")?.toInt() ?: 0
                val likers = document.get("likers") as? List<String> ?: listOf()

                if (likers.contains(currentUserId)) {
                    // User has already liked the post - Unlike it
                    postRef.update(
                        "likes", kotlin.math.max(0, likes - 1), // Ensure likes don't go below 0
                        "likers", FieldValue.arrayRemove(currentUserId)
                    ).addOnSuccessListener {
                        Toast.makeText(requireContext(), "Post unliked!", Toast.LENGTH_SHORT).show()

                        // Update the feed item locally for immediate UI feedback
                        feed.likes = (feed.likes ?: 0) - 1
                        if (feed.likers != null) {
                            val updatedLikers = feed.likers!!.toMutableList()
                            updatedLikers.remove(currentUserId)
                            feed.likers = updatedLikers
                        }

                        // Notify only the changed item
                        adapter.notifyItemChanged(position)

                        // Restore scroll position
                        layoutManager.scrollToPosition(scrollPosition)
                    }
                } else {
                    // User hasn't liked the post yet - Like it
                    postRef.update(
                        "likes", likes + 1,
                        "likers", FieldValue.arrayUnion(currentUserId)
                    ).addOnSuccessListener {
                        Toast.makeText(requireContext(), "Post liked!", Toast.LENGTH_SHORT).show()

                        // Update the feed item locally for immediate UI feedback
                        feed.likes = (feed.likes ?: 0) + 1
                        if (feed.likers == null) {
                            feed.likers = listOf(currentUserId)
                        } else {
                            val updatedLikers = feed.likers!!.toMutableList()
                            updatedLikers.add(currentUserId)
                            feed.likers = updatedLikers
                        }

                        // Create notification for post owner
                        val postOwnerId = feed.userid ?: ""

                        // Get current user info for notification
                        vm.getCurrentUserInfo { username, userImage ->
                            // Create like notification
                            vm.createNotification(
                                toUserId = postOwnerId,
                                fromUserId = currentUserId,
                                fromUsername = username,
                                fromUserImage = userImage,
                                type = "like",
                                postId = postId,
                                postCaption = feed.caption ?: ""
                            )
                        }

                        // Notify only the changed item
                        adapter.notifyItemChanged(position)

                        // Restore scroll position
                        layoutManager.scrollToPosition(scrollPosition)
                    }
                }
            }
        }
    }

    override fun onUserClick(userId: String) {
        navigateToUserProfile(userId)
    }

    private fun navigateToUserProfile(userId: String) {
        try {
            Log.d("Navigation", "Navigating to user profile with ID: $userId")

            // Create an intent to the activity that hosts OtherUsersFragment
            val intent = Intent(requireActivity(), MainActivity::class.java).apply {
                // Pass the ID as an extra
                putExtra("userId", userId)
                // Add a flag to indicate we want to show the OtherUsersFragment
                putExtra("showOtherUser", true)
            }

            startActivity(intent)
        } catch (e: Exception) {
            Log.e("Navigation Error", "Failed to navigate using Intent", e)
            Toast.makeText(
                context,
                "Navigation error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}