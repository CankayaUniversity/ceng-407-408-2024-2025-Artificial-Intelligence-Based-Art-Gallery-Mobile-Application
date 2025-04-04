package com.example.socialmediaapp.fragments

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.socialmediaapp.R
import com.example.socialmediaapp.activities.SignInAc
import com.example.socialmediaapp.databinding.FragmentChangePasswordBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ChangePasswordFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ChangePasswordFragment : Fragment() {

    private lateinit var binding: FragmentChangePasswordBinding

    lateinit var fbauth : FirebaseAuth
    private var user: FirebaseUser? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_change_password, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        fbauth = FirebaseAuth.getInstance()


        user = fbauth.currentUser

        binding.btnSubmit.setOnClickListener{
            val newpass: String = binding.etNewPassword.text.toString().trim { it <= ' ' }
            val confpass: String = binding.etConfirmPassword.text.toString().trim { it <= ' ' }


            if (TextUtils.isEmpty(newpass) || TextUtils.isEmpty(confpass)) {
                Toast.makeText(activity, "Please fill the new password and confirm password fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            // Eşleşme kontrol
            if (!newpass.equals(confpass)) {
                Toast.makeText(activity, "Passwords are not matching.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            // Şifreyi güncelle
            user!!.updatePassword(newpass).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(activity, "The password has been changed succesffuly. You need to log in again.", Toast.LENGTH_SHORT).show()

                    fbauth.signOut()

                    // Start the login or sign-in activity
                    val intent = Intent(requireContext(), SignInAc::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()

                } else {
                    Toast.makeText(activity, "The password could not be updated.", Toast.LENGTH_SHORT).show()
                }
            }

        }


    }

}