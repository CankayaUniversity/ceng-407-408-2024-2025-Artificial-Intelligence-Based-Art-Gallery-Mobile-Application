package com.example.socialmediaapp.fragments

import android.app.Dialog
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.socialmediaapp.MainActivity
import com.example.socialmediaapp.R
import com.example.socialmediaapp.adapters.PostsAdapter
import com.example.socialmediaapp.adapters.SearchUsersAdapter
import com.example.socialmediaapp.modal.Posts
import com.example.socialmediaapp.modal.Users
import com.example.socialmediaapp.mvvm.ViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class SearchFragment : Fragment(), OnPostClickListener {

    // View and RecyclerView attributes
    private var recyclerView: RecyclerView? = null
    private var allPostsRecyclerView: RecyclerView? = null
    private var userPostsRecyclerView: RecyclerView? = null
    private var searchResultsContainer: View? = null

    // Adapter attributes
    private var userAdapter: SearchUsersAdapter? = null
    private var allPostsAdapter: PostsAdapter? = null
    private var userPostsAdapter: PostsAdapter? = null

    private var mUser: MutableList<Users>? = null
    private var searchItem: EditText? = null
    private var toggleButton: FloatingActionButton? = null
    private var isShowingUsers = false

    // Add this label to display the search results title
    private var searchResultsTitle: TextView? = null

    private lateinit var vm: ViewModel

    // Firestore reference
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        searchResultsContainer = view.findViewById(R.id.search_results_container)

        // Initialize RecyclerViews
        recyclerView = view.findViewById(R.id.recyclerview_search)
        recyclerView?.setHasFixedSize(true)
        recyclerView?.layoutManager = LinearLayoutManager(context)

        // Initialize posts RecyclerViews
        allPostsRecyclerView = view.findViewById(R.id.recyclerview_all_posts)
        allPostsRecyclerView?.setHasFixedSize(true)
        allPostsRecyclerView?.layoutManager = GridLayoutManager(context, 3)

        // Initialize User-specific Posts RecyclerView
        userPostsRecyclerView = view.findViewById(R.id.recyclerview_user_posts)
        userPostsRecyclerView?.setHasFixedSize(true)
        userPostsRecyclerView?.layoutManager = GridLayoutManager(context, 3)

        // Initialize toggle button
        toggleButton = view.findViewById(R.id.toggle_view_button)
        toggleButton?.setOnClickListener {
            toggleView()
        }

        // Initialize user list
        mUser = ArrayList()

        // Initialize ViewModel first
        vm = ViewModelProvider(this).get(ViewModel::class.java)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFilterSpinner()

        // Initialize adapter with the fragment as context AND the ViewModel
        userAdapter = context?.let {
            SearchUsersAdapter(
                it,
                mUser as ArrayList<Users>,
                true,
                vm, // Pass the ViewModel to the adapter
                { userId ->
                    // Handle navigation to other user's profile
                    navigateToUserProfile(userId)
                }
            )
        }
        recyclerView?.adapter = userAdapter

        // Initialize all posts adapter
        allPostsAdapter = PostsAdapter()
        allPostsAdapter?.setOnPostClickListener(this)
        allPostsRecyclerView?.adapter = allPostsAdapter

        // Initialize user posts adapter
        userPostsAdapter = PostsAdapter()
        userPostsAdapter?.setOnPostClickListener(this)
        userPostsRecyclerView?.adapter = userPostsAdapter

        // Initialize search field
        searchItem = view.findViewById(R.id.searchitem)

        // Set up text change listener for search
        searchItem!!.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Not needed
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = searchItem!!.text.toString()

                if (searchText.isEmpty()) {
                    // Show regular feed when search is empty
                    searchResultsContainer?.visibility = View.GONE
                    allPostsRecyclerView?.visibility = View.VISIBLE
                    userPostsRecyclerView?.visibility = View.GONE
                } else {
                    // Show search results (both users and their posts)
                    searchResultsContainer?.visibility = View.VISIBLE
                    allPostsRecyclerView?.visibility = View.GONE
                    userPostsRecyclerView?.visibility = View.VISIBLE

                    // Search for users with matching username
                    searchUser(searchText.toLowerCase(Locale.ROOT))
                }
            }
        })

        // Set initial view states
        searchResultsContainer?.visibility = View.GONE
        allPostsRecyclerView?.visibility = View.VISIBLE
        userPostsRecyclerView?.visibility = View.GONE

        // Load all posts from other users
        vm.getAllPostsExceptCurrentUser().observe(viewLifecycleOwner, Observer { posts ->
            allPostsAdapter?.setPosts(posts)
        })
    }

    // Safe navigation method
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

    private fun toggleView() {
        if (searchItem!!.text.toString().isEmpty()) {
            // Only toggle view when search is empty
            isShowingUsers = !isShowingUsers
            if (isShowingUsers) {
                // Show all users
                searchResultsContainer?.visibility = View.VISIBLE
                allPostsRecyclerView?.visibility = View.GONE
                userPostsRecyclerView?.visibility = View.GONE
                toggleButton?.setImageResource(R.drawable.ic_home_active)
                retrieveAllUsers()
            } else {
                // Show all posts
                searchResultsContainer?.visibility = View.GONE
                allPostsRecyclerView?.visibility = View.VISIBLE
                userPostsRecyclerView?.visibility = View.GONE
                toggleButton?.setImageResource(R.drawable.search)
            }
        }
    }

    private fun searchUser(input: String) {
        // Get current user ID
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        val query = FirebaseFirestore.getInstance().collection("Users")
            .orderBy("username")
            .startAt(input)
            .endAt(input + "\uf8ff")

        query.addSnapshotListener { value, error ->
            if (error != null) {
                Log.e("Firestore Error", error.message.toString())
                return@addSnapshotListener
            }

            mUser?.clear()

            for (document in value?.documents ?: emptyList()) {
                val user = document.toObject(Users::class.java)
                // Only add users that are not the current user
                if (user != null && user.userid != currentUserId) {
                    mUser?.add(user)

                    // Fetch posts for every matching user
                    user.userid?.let {
                        fetchUserPosts(it, user.username ?: "User")
                    }
                }
            }
            userAdapter?.notifyDataSetChanged()

            // Update UI based on search results
            if (mUser?.isEmpty() == true) {
                // No users found
                searchResultsTitle?.text = "No users found"
                userPostsRecyclerView?.visibility = View.GONE
            }
        }
    }

    private fun fetchUserPosts(userId: String, username: String) {
        val postsCollection = FirebaseFirestore.getInstance().collection("Posts")
        postsCollection.whereEqualTo("userid", userId)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(context, "Could not read from Database", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                val postsList = ArrayList<Posts>()
                for (document in value?.documents ?: emptyList()) {
                    val post = document.toObject(Posts::class.java)
                    if (post != null) {
                        post.postid = document.id  // Make sure postid is set
                        postsList.add(post)
                    }
                }

                // Display the user's artwork
                userPostsAdapter?.setPosts(postsList)

                // Update the title to show whose artwork we're displaying
                searchResultsTitle?.text = "$username's Artwork"

                // Update UI based on search results
                if (postsList.isEmpty()) {
                    searchResultsTitle?.text = "$username has no artwork yet"
                } else {
                    // Make sure the user posts recycler view is visible
                    userPostsRecyclerView?.visibility = View.VISIBLE
                }
            }
    }

    private fun retrieveAllUsers() {
        // Get current user ID
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        val usersCollection = FirebaseFirestore.getInstance().collection("Users")

        usersCollection.addSnapshotListener { value, error ->
            if (error != null) {
                Toast.makeText(context, "Could not read from Database", Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }

            mUser?.clear()
            for (document in value?.documents ?: emptyList()) {
                val user = document.toObject(Users::class.java)
                // Only add users that are not the current user
                if (user != null && user.userid != currentUserId) {
                    mUser?.add(user)
                }
            }
            userAdapter?.notifyDataSetChanged()
        }
    }

    // This is where we implement the enhanced artwork details functionality
    override fun onPostClick(post: Posts) {
        // Get post ID
        val postId = post.postid ?: return

        // Fetch additional artwork details and show dialog
        fetchArtworkDetailsAndShowDialog(postId, post)
    }

    private fun fetchArtworkDetailsAndShowDialog(postId: String, post: Posts) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val imageUrl = post.image ?: ""
                var title = post.caption ?: "Untitled"
                var story = ""
                val likes = post.likes ?: 0
                val comments = post.comments ?: 0
                val userId = post.userid ?: ""

                var userName = ""
                var userImageUrl = ""

                // Fetch artwork details from Images collection
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

                // Fetch user info from Users collection
                if (userId.isNotEmpty()) {
                    try {
                        val userDoc = firestore.collection("Users").document(userId).get().await()
                        if (userDoc.exists()) {
                            userName = userDoc.getString("username") ?: "Unknown Artist"
                            userImageUrl = userDoc.getString("image") ?: ""
                            Log.d("ArtworkDetails", "Fetched artist name: $userName")
                            Log.d("ArtworkDetails", "Fetched artist image URL: $userImageUrl")
                        } else {
                            Log.w("ArtworkDetails", "User document does not exist for ID: $userId")
                        }
                    } catch (e: Exception) {
                        Log.e("ArtworkDetails", "Error fetching user document", e)
                    }
                }

                withContext(Dispatchers.Main) {
                    showArtworkDetailsDialog(imageUrl, title, story, likes, comments, postId, userId, userName, userImageUrl)
                }
            } catch (e: Exception) {
                Log.e("SearchFragment", "Error fetching artwork details", e)
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
        docId: String,
        userId: String,
        userName: String,
        userImageUrl: String
    ) {
        // Make sure we're still attached to a context
        val currentContext = context ?: return

        val dialog = Dialog(currentContext)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_artwork_details)
        dialog.window?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(currentContext, R.color.semi_transparent)))

        // Initialize dialog views
        val detailImageView = dialog.findViewById<ImageView>(R.id.detailImageView)
        val detailTitleTextView = dialog.findViewById<TextView>(R.id.detailTitleTextView)
        val detailStoryTextView = dialog.findViewById<TextView>(R.id.detailStoryTextView)
        val detailLikesTextView = dialog.findViewById<TextView>(R.id.detailLikesTextView)
        val detailCommentsTextView = dialog.findViewById<TextView>(R.id.detailCommentsTextView)
        val closeButton = dialog.findViewById<Button>(R.id.closeButton)

        // Artist information views
        val artistImageView = dialog.findViewById<CircleImageView>(R.id.artistImageView)
        val artistNameTextView = dialog.findViewById<TextView>(R.id.artistNameTextView)
        val artistInfoContainer = dialog.findViewById<View>(R.id.artistInfoContainer)

        // Show artist information section
        artistInfoContainer.visibility = View.VISIBLE

        // Load main artwork image
        Glide.with(currentContext)
            .load(imageUrl)
            .fitCenter()
            .placeholder(R.drawable.placeholder_image2)
            .error(R.drawable.error_image)
            .into(detailImageView)

        // Set text data
        detailTitleTextView.text = title
        detailStoryTextView.text = if (story.isNotEmpty()) story else "No story available for this artwork."
        detailLikesTextView.text = "$likes likes"
        detailCommentsTextView.text = "$comments comments"

        // Set artist information
        artistNameTextView.text = userName

        // Fixed implementation for artist profile image loading
        if (userImageUrl.isNotEmpty()) {
            Log.d("ArtworkDetails", "Loading artist image from URL: $userImageUrl")

            // Use a simpler Glide implementation with the view's context
            Glide.with(artistImageView)  // Use the view's context directly
                .load(userImageUrl)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(artistImageView)
        } else {
            Log.d("ArtworkDetails", "No artist image URL available, using default")
            artistImageView.setImageResource(R.drawable.ic_profile)
        }

        // Set click listener for artist container to navigate to profile
        artistInfoContainer.setOnClickListener {
            if (userId.isNotEmpty()) {
                navigateToUserProfile(userId)
                dialog.dismiss()
            }
        }

        // Set close button listener
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupFilterSpinner() {
        val spinner = requireActivity().findViewById<Spinner>(R.id.filter_spinner)
        val spinnerAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.filter_options_search,
            R.layout.spinner_item
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
                    "Newest" -> vm.sortFeedDescendingDateSearch()
                    "Oldest" -> vm.sortFeedAscendingDateSearch()
                    "Most Liked" -> vm.sortFeedMostLikedSearch()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No action needed
            }
        }
    }
}

// Post click listener interface
interface OnPostClickListener {
    fun onPostClick(post: Posts)
}