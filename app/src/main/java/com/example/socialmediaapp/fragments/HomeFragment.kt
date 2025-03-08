package com.example.socialmediaapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment(),onDoubleTapClickListener  {


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

        binding.lifecycleOwner = viewLifecycleOwner

        vm.loadMyFeed().observe(viewLifecycleOwner, Observer{
            adapter.setFeedList(it)

            binding.feedRecycler.adapter = adapter

        })

        adapter.setListener(this)


        binding.imageViewBottom.setOnClickListener {

            view.findNavController().navigate(R.id.action_homeFragment_to_profileFragment)

        }

        vm.image.observe(viewLifecycleOwner, Observer {


            Glide.with(requireContext()).load(it).into(binding.imageViewBottom)


        })


    }


    override fun onDoubleTap(feed: Feed) {
        val currentUserId = Utils.getUiLoggedIn() // Replace with the current user's ID
        val postId = feed.postid

        val firestore = FirebaseFirestore.getInstance()
        val postRef = firestore.collection("Posts").document(postId!!)

        // Fetch current like count and likers from Firestore

        postRef.get()
            .addOnSuccessListener{document->

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

}