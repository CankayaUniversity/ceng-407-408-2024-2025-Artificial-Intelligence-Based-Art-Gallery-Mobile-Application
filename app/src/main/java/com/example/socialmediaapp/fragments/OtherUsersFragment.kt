package com.example.socialmediaapp.fragments

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.socialmediaapp.R
import com.example.socialmediaapp.adapters.MyPostAdapter
import com.example.socialmediaapp.databinding.FragmentOtherUsersBinding
import com.example.socialmediaapp.modal.Posts
import com.example.socialmediaapp.mvvm.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class OtherUsersFragment : Fragment() {

    private lateinit var binding: FragmentOtherUsersBinding
    private lateinit var viewModel: ViewModel
    private var userId: String? = null
    private var isFollowing = false
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Create binding
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_other_users, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get userId from arguments
        userId = arguments?.let {
            OtherUsersFragmentArgs.fromBundle(it).userId
        }

        // Create ViewModel
        viewModel = ViewModelProvider(this).get(ViewModel::class.java)

        // Back button setup
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Observe user information
        userId?.let { id ->
            // Load user details
            viewModel.getOtherUser(id).observe(viewLifecycleOwner, Observer { user ->
                binding.usernameText.text = user.username
                Glide.with(requireContext()).load(user.image).into(binding.profileImage)
            })

            // Load user posts
            viewModel.getOtherUserPosts(id).observe(viewLifecycleOwner, Observer { posts ->
                val adapter = MyPostAdapter()
                adapter.setPostList(posts)

                // Set up click listener for posts
                adapter.setOnPostClickListener { post ->
                    post.postid?.let { postId ->
                        fetchArtworkDetailsAndShowDialog(postId, post)
                    }
                }

                binding.recyclerView.adapter = adapter
            })

            // Load user follower and following counts
            viewModel.getOtherUserStats(id).observe(viewLifecycleOwner) { stats ->
                binding.followersCountText.text = stats["followers"].toString()
                binding.followingCountText.text = stats["following"].toString()
            }

            // Load user post count
            viewModel.getOtherUserPostCount(id).observe(viewLifecycleOwner) { postCount ->
                binding.postsCountText.text = postCount.toString()
            }
        }

        // Set up follow button click listener
        binding.followButton.setOnClickListener {
            userId?.let { id ->
                if (isFollowing) {
                    // Unfollow the user
                    viewModel.unfollowUser(id).observe(viewLifecycleOwner) { success ->
                        if (success) {
                            updateFollowButton(false)
                            Toast.makeText(context, "Unfollowed", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to unfollow", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Follow the user
                    viewModel.followUser(id).observe(viewLifecycleOwner) { success ->
                        if (success) {
                            updateFollowButton(true)
                            Toast.makeText(context, "Followed", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to follow", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        userId?.let { id ->
            viewModel.checkIfFollowing(id).observe(viewLifecycleOwner) { following ->
                isFollowing = following
                updateFollowButton(following)
            }
        }
    }

    private fun updateFollowButton(following: Boolean) {
        isFollowing = following
        binding.followButton.apply {
            text = if (following) "Following" else "Follow"
            backgroundTintList = if (following) {
                ContextCompat.getColorStateList(requireContext(), R.color.button_blue)
            } else {
                ContextCompat.getColorStateList(requireContext(), R.color.button_blue)
            }
        }
    }

    private fun fetchArtworkDetailsAndShowDialog(postId: String, post: Posts) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Get the basic information from the Post
                val imageUrl = post.image ?: ""
                var title = post.caption ?: "Untitled"
                var story = ""
                val likes = post.likes ?: 0
                val comments = post.comments ?: 0

                // Fetch additional artwork details from Images collection
                val imagesQuery = firestore.collection("Images")
                    .whereEqualTo("postid", postId)
                    .limit(1)
                    .get()
                    .await()

                if (!imagesQuery.isEmpty) {
                    val imageDoc = imagesQuery.documents[0]
                    story = imageDoc.getString("story") ?: ""
                    title = imageDoc.getString("title") ?: title
                }

                // Show dialog on main thread
                withContext(Dispatchers.Main) {
                    showArtworkDetailsDialog(imageUrl, title, story, likes, comments, postId)
                }
            } catch (e: Exception) {
                Log.e("OtherUsersFragment", "Error fetching artwork details", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Failed to load artwork details: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showArtworkDetailsDialog(
        imageUrl: String,
        title: String,
        story: String,
        likes: Int,
        comments: Int,
        docId: String
    ) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_artwork_details)
        dialog.window?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(requireContext(), R.color.semi_transparent)))

        // Initialize dialog views
        val detailImageView = dialog.findViewById<ImageView>(R.id.detailImageView)
        val detailTitleTextView = dialog.findViewById<TextView>(R.id.detailTitleTextView)
        val detailStoryTextView = dialog.findViewById<TextView>(R.id.detailStoryTextView)
        val detailLikesTextView = dialog.findViewById<TextView>(R.id.detailLikesTextView)
        val detailCommentsTextView = dialog.findViewById<TextView>(R.id.detailCommentsTextView)
        val closeButton = dialog.findViewById<Button>(R.id.closeButton)

        // Load image and set text views
        Glide.with(this)
            .load(imageUrl)
            .fitCenter()
            .placeholder(R.drawable.placeholder_image2)
            .error(R.drawable.error_image)
            .into(detailImageView)

        detailTitleTextView.text = title
        detailStoryTextView.text = story
        detailLikesTextView.text = "$likes likes"
        detailCommentsTextView.text = "$comments comments"

        // Set close button listener
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}