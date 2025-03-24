package com.example.socialmediaapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.socialmediaapp.R
import com.example.socialmediaapp.adapters.MyFeedAdapter
import com.example.socialmediaapp.adapters.onDoubleTapClickListener
import com.example.socialmediaapp.databinding.FragmentHomeBinding
import com.example.socialmediaapp.modal.Feed
import com.example.socialmediaapp.mvvm.ViewModel
import androidx.navigation.findNavController
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.example.socialmediaapp.Utils
import com.example.socialmediaapp.adapters.onUserClickListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.example.socialmediaapp.adapters.onCommentClickListener






class HomeFragment : Fragment(),onDoubleTapClickListener, onUserClickListener  {


    // Parameters
    private lateinit var vm : ViewModel
    private lateinit var binding : FragmentHomeBinding
    private lateinit var adapter : MyFeedAdapter

    private var postid : String = ""


    private var userwholiked : String = ""

    private var idofpostowner : String = ""


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        vm = ViewModelProvider(this).get(ViewModel::class.java)
        adapter = MyFeedAdapter()



        adapter.setCommentClickListener(object : onCommentClickListener{
            override fun addComment(postId: String, comment: String) {
                if (comment.isNotEmpty()) {
                    vm.addComment(postId, comment)
                    Toast.makeText(requireContext(), "Yorum eklendi!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Yorum boş olamaz!", Toast.LENGTH_SHORT).show()
                }
            }
        })


        adapter.setUserClickListener(this)

        binding.lifecycleOwner = viewLifecycleOwner

        vm.loadMyFeed().observe(viewLifecycleOwner, Observer{
            adapter.setFeedList(it)

            binding.feedRecycler.adapter = adapter

        })

        adapter.setListener(this)






    }


    override fun onDoubleTap(feed: Feed) {
        val currentUserId = Utils.getUiLoggedIn() // Replace with the current user's ID
        val postId = feed.postid

        val firestore = FirebaseFirestore.getInstance()
        val postRef = firestore.collection("Posts").document(postId!!)

        // Fetch current like count and likers from Firestore

        postRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()){


                    val likes = document.getLong("likes")?.toInt() ?: 0
                    val likers = document.get("likers") as? List<String>

                    if (!likers.isNullOrEmpty() && likers.contains(currentUserId)){

                        // User has already liked the post
                        println("You have already liked this post!")
                    }else{
                        // Increment like count and update likers
                        postRef.update(
                            "likes", likes + 1,
                            "likers", FieldValue.arrayUnion(currentUserId)
                        )

                            .addOnSuccessListener {
                                println("Post liked!")
                            }

                            .addOnFailureListener { exception ->
                                println("Failed to update like: $exception")
                            }

                    }



                }

            }

    }



    override fun onUserClick(userId: String) {
        // Safe Args kullanarak OtherUsersFragment'a yönlendirme yap
        val action = HomeFragmentDirections.actionHomeFragmentToOtherUsersFragment(userId)
        view?.findNavController()?.navigate(action)
    }

}
