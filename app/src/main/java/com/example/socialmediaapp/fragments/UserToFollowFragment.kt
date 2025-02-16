package com.example.socialmediaapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.example.socialmediaapp.R
import com.example.socialmediaapp.adapters.UsersAdapter
import com.example.socialmediaapp.databinding.FragmentUserToFollowBinding
import com.example.socialmediaapp.modal.Users
import com.example.socialmediaapp.mvvm.ViewModel
import android.widget.Toast
import com.example.socialmediaapp.Utils
import com.example.socialmediaapp.adapters.OnFriendClicked

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [UserToFollowFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class UserToFollowFragment : Fragment(),OnFriendClicked {

    private lateinit var adapter : UsersAdapter
    private lateinit var vm : ViewModel
    private lateinit var binding: FragmentUserToFollowBinding
    var clickedOn: Boolean = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding = DataBindingUtil. inflate(inflater, R.layout.fragment_user_to_follow, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        vm = ViewModelProvider(this).get(ViewModel::class.java)
        binding.lifecycleOwner = viewLifecycleOwner

        adapter = UsersAdapter()

        // Back button clicked
        binding.backBtn.setOnClickListener {

        view.findNavController().navigate(R.id.action_userToFollowFragment_to_profileFragment)

        }


        vm.getAllUsers().observe(viewLifecycleOwner, Observer{
            adapter.setUserLIST(it)
            binding.rvFollow.adapter = adapter
        })

        adapter.setListener(this)


    }

    override fun onfriendListener(
        position: Int,
        user: Users
    ) {
        adapter.followUser(user)
    }


}