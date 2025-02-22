package com.example.socialmediaapp.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.example.socialmediaapp.R
import com.example.socialmediaapp.activities.SignInAc
import com.example.socialmediaapp.databinding.FragmentProfileBinding
import com.example.socialmediaapp.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SettingsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@Suppress("DEPRECATION")
class SettingsFragment : Fragment() {
    private lateinit var binding: FragmentSettingsBinding
    lateinit var fbauth : FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        fbauth = FirebaseAuth.getInstance()


        binding.signout.setOnClickListener{

            fbauth.signOut()

            // Start the login or sign-in activity
            val intent = Intent(requireContext(), SignInAc::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()

        }

        binding.changePassword.setOnClickListener {

            view.findNavController().navigate(R.id.action_settingsFragment_to_changePasswordFragment)

        }

        binding.backButton.setOnClickListener {

            view.findNavController().navigate(R.id.action_settingsFragment_to_profileFragment)

        }



    }

}