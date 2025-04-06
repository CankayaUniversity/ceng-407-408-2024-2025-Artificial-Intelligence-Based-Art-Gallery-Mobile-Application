@file:Suppress("DEPRECATION")

package com.example.socialmediaapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmediaapp.R
import com.example.socialmediaapp.adapters.MyFeedAdapter
import com.google.firebase.auth.FirebaseAuth
import com.example.socialmediaapp.adapters.PostsAdapter
import com.example.socialmediaapp.adapters.SearchUsersAdapter
import com.example.socialmediaapp.modal.Posts
import com.example.socialmediaapp.modal.Users
import com.example.socialmediaapp.mvvm.ViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class SearchFragment : Fragment(), OnPostClickListener {

    // View and ResycleView attributes
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

    private lateinit var vm: ViewModel

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

        // Initialize user list and adapter
        mUser = ArrayList()
        userAdapter = context?.let { SearchUsersAdapter(it, mUser as ArrayList<Users>, true) }
        recyclerView?.adapter = userAdapter

        // Initialize all posts adapter
        allPostsAdapter = PostsAdapter()
        allPostsAdapter?.setOnPostClickListener(this)
        allPostsRecyclerView?.adapter = allPostsAdapter

        //Initialize user posts adapter
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
                } else {
                    // Show search results (both users and their posts)
                    searchResultsContainer?.visibility = View.VISIBLE
                    allPostsRecyclerView?.visibility = View.GONE

                    // Search for users with matching username
                    searchUser(searchText.toLowerCase(Locale.ROOT))

                    // Search for posts with matching caption
                    searchPostsByCaption(searchText.toLowerCase(Locale.ROOT))
                }
            }
        })

        // Set initial view states
        searchResultsContainer?.visibility = View.GONE
        allPostsRecyclerView?.visibility = View.VISIBLE

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm = ViewModelProvider(this).get(ViewModel::class.java)

        // Load all posts from other users
        vm.getAllPostsExceptCurrentUser().observe(viewLifecycleOwner, Observer { posts ->
            allPostsAdapter?.setPosts(posts)
        })
    }

    private fun toggleView() {
        if (searchItem!!.text.toString().isEmpty()) {
            // Only toggle view when search is empty
            isShowingUsers = !isShowingUsers
            if (isShowingUsers) {
                // Show all users
                searchResultsContainer?.visibility = View.VISIBLE
                allPostsRecyclerView?.visibility = View.GONE
                toggleButton?.setImageResource(R.drawable.ic_home_active)
                retrieveAllUsers()
            } else {
                // Show all posts
                searchResultsContainer?.visibility = View.GONE
                allPostsRecyclerView?.visibility = View.VISIBLE
                toggleButton?.setImageResource(R.drawable.search)
            }
        }
    }

    private fun searchPostsByCaption(input: String) {
        val postsCollection = FirebaseFirestore.getInstance().collection("Posts")

        postsCollection
            .orderBy("caption")
            .startAt(input)
            .endAt(input + "\uf8ff")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e("Firestore Error", error.message.toString())
                    return@addSnapshotListener
                }

                val postsList = ArrayList<Posts>()
                for (document in value?.documents ?: emptyList()) {
                    val post = document.toObject(Posts::class.java)
                    if (post != null) {
                        postsList.add(post)
                    }
                }
                userPostsAdapter?.setPosts(postsList)
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

                    // Fetch posts for the first matching user
                    if (mUser?.size == 1) {
                        user.userid?.let { fetchUserPosts(it) }
                    }
                }
            }
            userAdapter?.notifyDataSetChanged()
        }
    }

    private fun fetchUserPosts(userId: String) {
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
                        postsList.add(post)
                    }
                }
                userPostsAdapter?.setPosts(postsList)

                // Make sure the user posts recycler view is visible
                userPostsRecyclerView?.visibility = View.VISIBLE
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

    override fun onPostClick(post: Posts) {
        // Handle post click - navigate to post detail or perform other actions
        Toast.makeText(context, "Post clicked: ${post.caption}", Toast.LENGTH_SHORT).show()

        // Example navigation to post detail (you'll need to implement this)
        // val action = SearchFragmentDirections.actionSearchFragmentToPostDetailFragment(post.postid!!)
        // view?.findNavController()?.navigate(action)
    }
}

// Post click listener interface
interface OnPostClickListener {
    fun onPostClick(post: Posts)
}