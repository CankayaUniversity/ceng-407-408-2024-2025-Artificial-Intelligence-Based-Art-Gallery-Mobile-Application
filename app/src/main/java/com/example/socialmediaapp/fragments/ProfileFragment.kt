package com.example.socialmediaapp.fragments

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.example.socialmediaapp.R
import com.example.socialmediaapp.Utils
import com.example.socialmediaapp.activities.SignInAc
import com.example.socialmediaapp.adapters.MyPostAdapter
import com.example.socialmediaapp.databinding.FragmentProfileBinding
import com.example.socialmediaapp.mvvm.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream
import java.util.UUID
import android.os.Debug

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@Suppress("DEPRECATION")
class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var vm : ViewModel
    private lateinit var storageRef: StorageReference
    lateinit var storage: FirebaseStorage

    lateinit var profileBitmap: Bitmap
    var uriProfile: Uri? = null


    private lateinit var pd : ProgressDialog
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter : MyPostAdapter
    private lateinit var  imageView : CircleImageView


    private lateinit var editText : EditText
    lateinit var fbauth : FirebaseAuth


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)

        return binding.root
    }



}