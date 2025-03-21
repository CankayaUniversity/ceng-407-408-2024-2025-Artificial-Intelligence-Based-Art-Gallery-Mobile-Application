package com.example.socialmediaapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.example.socialmediaapp.R
import com.example.socialmediaapp.adapters.MyPostAdapter
import com.example.socialmediaapp.databinding.FragmentOtherUsersBinding
import com.example.socialmediaapp.mvvm.ViewModel

class OtherUsersFragment : Fragment() {

    private lateinit var binding: FragmentOtherUsersBinding
    private lateinit var viewModel: ViewModel
    private var userId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Binding'i oluştur
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_other_users, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Safe Args ile gelen userId'yi al
        userId = arguments?.let {
            OtherUsersFragmentArgs.fromBundle(it).userId
        }

        // ViewModel'i oluştur
        viewModel = ViewModelProvider(this).get(ViewModel::class.java)

        // Kullanıcı bilgilerini gözlemle
        userId?.let { id ->
            // Kullanıcı detaylarını yükle
            viewModel.getOtherUser(id).observe(viewLifecycleOwner, Observer { user ->
                binding.usernameText.text = user.username
                Glide.with(requireContext()).load(user.image).into(binding.profileImage)
            })

            // Kullanıcının gönderilerini yükle
            viewModel.getOtherUserPosts(id).observe(viewLifecycleOwner, Observer { posts ->
                val adapter = MyPostAdapter()
                adapter.setPostList(posts)
                binding.recyclerView.adapter = adapter
            })

            // Kullanıcının takipçi ve takip edilen sayısını yükle
            viewModel.getOtherUserStats(id).observe(viewLifecycleOwner) { stats ->
                binding.followersCountText.text = stats["followers"].toString()
                binding.followingCountText.text = stats["following"].toString()
            }

            // Kullanıcının gönderi sayısını yükle
            viewModel.getOtherUserPostCount(id).observe(viewLifecycleOwner) { postCount ->
                binding.postsCountText.text = postCount.toString()
            }
        }

        // Alt navbar'a profil resmini ekle
        viewModel.image.observe(viewLifecycleOwner, Observer { imageUrl ->
            Glide.with(requireContext()).load(imageUrl).into(binding.imageViewBottom)
        })

        // Alt navbar'ın tıklanabilirlik işlevleri
        setupBottomNavBar()
    }

    // Alt navbar için tıklanabilirlik
    private fun setupBottomNavBar() {
        binding.feed.setOnClickListener {
            view?.findNavController()?.navigate(R.id.action_otherUsersFragment_to_homeFragment)
        }

        binding.imageViewBottom.setOnClickListener {
            view?.findNavController()?.navigate(R.id.action_otherUsersFragment_to_profileFragment)
        }

        binding.addPost.setOnClickListener {
            view?.findNavController()?.navigate(R.id.action_otherUsersFragment_to_createPostFragment)
        }

        binding.addFriendsImage.setOnClickListener {
            view?.findNavController()?.navigate(R.id.action_otherUsersFragment_to_userToFollowFragment)
        }

        binding.settingsImage.setOnClickListener {
            view?.findNavController()?.navigate(R.id.action_otherUsersFragment_to_settingsFragment)
        }
    }
}
