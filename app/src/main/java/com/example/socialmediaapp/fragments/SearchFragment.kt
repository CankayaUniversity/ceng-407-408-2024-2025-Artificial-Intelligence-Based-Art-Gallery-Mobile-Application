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
import com.example.socialmediaapp.adapters.PostsAdapter
import com.example.socialmediaapp.adapters.SearchUsersAdapter
import com.example.socialmediaapp.modal.Posts
import com.example.socialmediaapp.modal.Users
import com.example.socialmediaapp.mvvm.ViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class SearchFragment : Fragment(), OnPostClickListener {

    private var recyclerView: RecyclerView? = null
    private var postsRecyclerView: RecyclerView? = null
    private var userAdapter: SearchUsersAdapter? = null
    private var postsAdapter: PostsAdapter? = null
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

        // Initialize RecyclerViews
        recyclerView = view.findViewById(R.id.recyclerview_search)
        recyclerView?.setHasFixedSize(true)
        recyclerView?.layoutManager = LinearLayoutManager(context)

        // Initialize posts RecyclerView (make sure to add this to your layout)
        postsRecyclerView = view.findViewById(R.id.recyclerview_all_posts)
        postsRecyclerView?.setHasFixedSize(true)
        postsRecyclerView?.layoutManager = GridLayoutManager(context, 3)

        // Initialize toggle button (make sure to add this to your layout)
        toggleButton = view.findViewById(R.id.toggle_view_button)
        toggleButton?.setOnClickListener {
            toggleView()
        }

        // Initialize user list and adapter
        mUser = ArrayList()
        userAdapter = context?.let { SearchUsersAdapter(it, mUser as ArrayList<Users>, true) }
        recyclerView?.adapter = userAdapter

        // Initialize posts adapter
        postsAdapter = PostsAdapter()
        postsAdapter?.setOnPostClickListener(this)
        postsRecyclerView?.adapter = postsAdapter

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
                if (searchItem!!.text.toString() == "") {
                    // Switch back to showing posts when search is cleared
                    if (isShowingUsers) {
                        isShowingUsers = false
                        setViewState()
                    }
                } else {
                    // Switch to showing users when text is entered
                    if (!isShowingUsers) {
                        isShowingUsers = true
                        setViewState()
                    }
                    searchUser(s.toString().toLowerCase(Locale.ROOT))
                }
            }
        })

        // Set initial view state
        setViewState()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm = ViewModelProvider(this).get(ViewModel::class.java)

        // Load all posts from other users
        vm.getAllPostsExceptCurrentUser().observe(viewLifecycleOwner, Observer { posts ->
            postsAdapter?.setPosts(posts)
        })
    }

    private fun toggleView() {
        isShowingUsers = !isShowingUsers
        if (!isShowingUsers) {
            // Clear search text when switching to posts view
            searchItem?.setText("")
        }
        setViewState()
    }

    private fun setViewState() {
        if (isShowingUsers) {
            recyclerView?.visibility = View.VISIBLE
            postsRecyclerView?.visibility = View.GONE
            toggleButton?.setImageResource(R.drawable.ic_home_active)
        } else {
            recyclerView?.visibility = View.GONE
            postsRecyclerView?.visibility = View.VISIBLE
            toggleButton?.setImageResource(R.drawable.search)
        }
        // Search bar always visible
        searchItem?.visibility = View.VISIBLE
    }

    private fun searchUser(input: String) {
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
                if (user != null) {
                    mUser?.add(user)
                }
            }
            userAdapter?.notifyDataSetChanged()
        }
    }

    private fun retrieveUser() {
        val usersCollection = FirebaseFirestore.getInstance().collection("Users")

        usersCollection.addSnapshotListener { value, error ->
            if (error != null) {
                Toast.makeText(context, "Could not read from Database", Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }

            if (searchItem!!.text.toString().isEmpty()) {
                mUser?.clear()
                for (document in value?.documents ?: emptyList()) {
                    val user = document.toObject(Users::class.java)
                    if (user != null) {
                        mUser?.add(user)
                    }
                }
                userAdapter?.notifyDataSetChanged()
            }
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