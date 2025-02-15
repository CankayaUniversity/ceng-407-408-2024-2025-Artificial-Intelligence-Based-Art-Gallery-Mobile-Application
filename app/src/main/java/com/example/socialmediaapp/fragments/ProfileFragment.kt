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



    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        vm = ViewModelProvider(this).get(ViewModel::class.java)


        fbauth = FirebaseAuth.getInstance()



        binding.viewModel = vm
        binding.lifecycleOwner = viewLifecycleOwner

        pd = ProgressDialog(requireContext())

        adapter = MyPostAdapter()
        firestore = FirebaseFirestore.getInstance()


        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        binding.settingsImage.setOnClickListener{

            fbauth.signOut()

            // Start the login or sign-in activity
            val intent = Intent(requireContext(), SignInAc::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()



        }

        binding.addFriendsImage.setOnClickListener{

            view.findNavController().navigate(R.id.action_profileFragment_to_userToFollowFragment)

        }
        binding.addPost.setOnClickListener{

            view.findNavController().navigate(R.id.action_profileFragment_to_createPostFragment)

        }


        vm.name.observe(viewLifecycleOwner, Observer { it->


            binding.usernameText.text = it!!


        })

        vm.image.observe(viewLifecycleOwner, Observer { it->


            Glide.with(requireContext()).load(it).into(binding.profileImage)
            Glide.with(requireContext()).load(it).into(binding.imageViewBottom)


        })


        binding.feed.setOnClickListener {

            view.findNavController().navigate(R.id.action_profileFragment_to_homeFragment)


        }



        vm.getMyPosts().observe(viewLifecycleOwner, Observer {
            binding.postsCountText.setText(it.size.toString())
            adapter.setPostList(it)

        })


        binding.imagesRecycler.adapter = adapter


        binding.editProfileBtn.setOnClickListener{


            val customView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_dialog_layout, null)
            imageView = customView.findViewById<CircleImageView>(R.id.userProfileImage)
            editText = customView.findViewById<EditText>(R.id.edit_username)


            vm.name.observe(viewLifecycleOwner, Observer {

                editText.setText(it)

            })





            vm.image.observe(viewLifecycleOwner, Observer {

                Glide.with(requireContext()).load(it).into(imageView)




            })


            imageView.setOnClickListener {

                alertDialogProfile()

            }

            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("Edit Profile")
                .setView(customView)
                .setPositiveButton("Done") { dialog, which ->
                    // Handle OK button click
                    val inputText = editText.text.toString()


                    firestore.collection("Users").document(Utils.getUiLoggedIn()).update("username", inputText.toString())


                    // if user updates the username the previous posts must have the new user name
                    val collectionref = firestore.collection("Posts")
                    val query = collectionref.whereEqualTo("userid", Utils.getUiLoggedIn())

                    query.get().addOnSuccessListener { documents->

                        for (document in documents){

                            collectionref.document(document.id).update("username" , inputText)


                        }


                    }







                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()


        }


        binding.profileImage.setOnClickListener {

            val customView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_dialog_layout, null)
            imageView = customView.findViewById<CircleImageView>(R.id.userProfileImage)
            editText = customView.findViewById<EditText>(R.id.edit_username)




            vm.name.observe(viewLifecycleOwner, Observer {

                editText.setText(it)

            })





            vm.image.observe(viewLifecycleOwner, Observer {

                Glide.with(requireContext()).load(it).into(imageView)




            })


            imageView.setOnClickListener {

                alertDialogProfile()

            }

            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("Edit Profile")
                .setView(customView)
                .setPositiveButton("Done") { dialog, which ->
                    // Handle OK button click
                    val inputText = editText.text.toString()


                    firestore.collection("Users").document(Utils.getUiLoggedIn()).update("username", inputText.toString())


                    // if user updates the username the previous posts must have the new user name
                    val collectionref = firestore.collection("Posts")
                    val query = collectionref.whereEqualTo("userid", Utils.getUiLoggedIn())

                    query.get().addOnSuccessListener { documents->

                        for (document in documents){

                            collectionref.document(document.id).update("username" , inputText)


                        }


                    }







                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()



        }

        binding.addPost.setOnClickListener {


            view.findNavController().navigate(R.id.action_profileFragment_to_createPostFragment)

        }











    }

    fun alertDialogProfile(){

        val options = arrayOf<CharSequence>("Take Photo", "Choose from Gallery", "Cancel")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Choose your profile picture")
        builder.setItems(options) { dialog, item ->
            when {
                options[item] == "Take Photo" -> {

                    profilePhotoWithCamera()


                }
                options[item] == "Choose from Gallery" -> {
                    profileImageFromGallery()
                }
                options[item] == "Cancel" -> dialog.dismiss()
            }
        }
        builder.show()





    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun profilePhotoWithCamera() {

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(takePictureIntent, Utils.PROFILE_IMAGE_CAPTURE)
    }


    @SuppressLint("QueryPermissionsNeeded")
    private fun profileImageFromGallery() {
        val pickPictureIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
       // if (pickPictureIntent.resolveActivity(requireActivity().packageManager) != null) {
        startActivityForResult(pickPictureIntent, Utils.PROFILE_IMAGE_PICK)
      //  }

    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)




        if (resultCode == RESULT_OK) {
            when (requestCode) {
                Utils.PROFILE_IMAGE_CAPTURE -> {
                    val profilebitmap = data?.extras?.get("data") as Bitmap

                    uploadProfile(profilebitmap)
                }
                Utils.PROFILE_IMAGE_PICK -> {
                    val profileUri = data?.data
                    val profilebitmap =
                        MediaStore.Images.Media.getBitmap(context?.contentResolver, profileUri)
                    uploadProfile(profilebitmap)
                }
            }
        }





    }


    private fun uploadProfile(imageBitmap: Bitmap?){

        val baos = ByteArrayOutputStream()
        imageBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()


        profileBitmap = imageBitmap!!

        imageView.setImageBitmap(imageBitmap)

        val storagePath = storageRef.child("Profile/${Utils.getUiLoggedIn()}.jpg")
        val uploadTask = storagePath.putBytes(data)
        uploadTask.addOnSuccessListener {


            val task = it.metadata?.reference?.downloadUrl

            task?.addOnSuccessListener {

                uriProfile = it

                firestore.collection("Users").document(Utils.getUiLoggedIn()).update("image", uriProfile.toString())




                // if user updates the profileimage the previous posts must have the new user name
                val collectionref = firestore.collection("Posts")
                val query = collectionref.whereEqualTo("userid", Utils.getUiLoggedIn())

                query.get().addOnSuccessListener { documents->

                    for (document in documents){

                        collectionref.document(document.id).update("imageposter" , uriProfile.toString())


                    }


                }




                vm.image.value = uriProfile.toString()


            }






            Toast.makeText(context, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to upload image!", Toast.LENGTH_SHORT).show()
        }


    }



    fun logMainThreadWorkload() {
        val mainThreadCpuTime = Debug.threadCpuTimeNanos()

        Log.d("MainThreadWorkload", "CPU Time (ns): $mainThreadCpuTime")
    }

// Perform some work on the main thread
// ...
// Log the current workload on the main thread after performing work



}