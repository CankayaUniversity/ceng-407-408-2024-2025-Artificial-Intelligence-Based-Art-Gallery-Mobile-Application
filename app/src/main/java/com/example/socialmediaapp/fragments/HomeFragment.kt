package com.example.socialmediaapp.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.socialmediaapp.MainActivity
import com.example.socialmediaapp.R
import com.example.socialmediaapp.Utils
import com.example.socialmediaapp.adapters.MyFeedAdapter
import com.example.socialmediaapp.adapters.onCommentClickListener
import com.example.socialmediaapp.adapters.onLikeClickListener
import com.example.socialmediaapp.adapters.onUserClickListener
import com.example.socialmediaapp.databinding.FragmentHomeBinding
import com.example.socialmediaapp.modal.Feed
import com.example.socialmediaapp.mvvm.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.bottomsheet.BottomSheetDialog

class HomeFragment : Fragment(), onLikeClickListener, onUserClickListener {
    private lateinit var vm: ViewModel
    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: MyFeedAdapter

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
                    Toast.makeText(requireContext(), "Yorum eklendi!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Yorum boş olamaz!", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun observeFeed() {
        binding.lifecycleOwner = viewLifecycleOwner
        vm.loadMyFeed().observe(viewLifecycleOwner, Observer { feedList ->
            adapter.setFeedList(feedList)
            binding.feedRecycler.adapter = adapter
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

                when (selectedOption) {
                    "Descending Date" -> vm.sortFeedDescendingDate()
                    "Ascending Date" -> vm.sortFeedAscendingDate()
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

        postRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val likes = document.getLong("likes")?.toInt() ?: 0
                val likers = document.get("likers") as? List<String>

                if (!likers.isNullOrEmpty() && likers.contains(currentUserId)) {
                    println("You have already liked this post!")
                } else {
                    postRef.update(
                        "likes", likes + 1,
                        "likers", FieldValue.arrayUnion(currentUserId)
                    ).addOnSuccessListener {
                        println("Post liked!")
                        // Feed listesi yeniden yüklensin
                        vm.loadMyFeed().observe(viewLifecycleOwner) { updatedFeedList ->
                            adapter.setFeedList(updatedFeedList)
                            adapter.notifyDataSetChanged()
                        }
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