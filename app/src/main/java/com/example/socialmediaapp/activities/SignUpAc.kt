@file:Suppress("DEPRECATION")

package com.example.socialmediaapp.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.example.socialmediaapp.R
import com.example.socialmediaapp.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast

class SignUpAc : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var pd: ProgressDialog
    private lateinit var firestore: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        binding = DataBindingUtil.setContentView(this,R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        pd = ProgressDialog(this)

        binding.signUpTextToSignIn.setOnClickListener {
            startActivity(Intent(this, SignInAc::class.java))

        }


        binding.signUpButton.setOnClickListener {

            if (binding.signUpnetemail.text.isNotEmpty() && binding.signUpetpassword.text.isNotEmpty()
                && binding.signUpName.text.isNotEmpty())
            {
                val email = binding.signUpnetemail.text.toString()
                val password = binding.signUpetpassword.text.toString()
                val name =  binding.signUpName.text.toString()

                signInUp(name,email,password)
            }

            if (binding.signUpnetemail.text.isEmpty() ||
                binding.signUpetpassword.text.isEmpty() || binding.signUpName.text.isEmpty())
            {
                Toast.makeText(this, "Fill the fields", Toast.LENGTH_SHORT).show()
            }



        }



    }


    private fun signInUp(name: String, email: String, password: String)
    {

        pd.show()
        pd.setMessage("Registering User")
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener{task->

            if (task.isSuccessful){


                val user = auth.currentUser


                val hashMap = hashMapOf("userid" to user!!.uid,
                    "image" to "https://upload.wikimedia.org/wikipedia/en/b/bd/Doraemon_character.png",
                    "username" to name,
                    "email" to email,
                    "followers" to 0,
                    "following" to 0)



                firestore.collection("Users").document(user.uid).set(hashMap).addOnSuccessListener {
                    pd.dismiss()

                    // Sign out the user before navigating
                    auth.signOut()

                    // Navigate back to Sign In page
                    val intent = Intent(this, SignInAc::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish() // Ensure the sign-up screen is closed
                }


            }else{
                pd.dismiss()
                Toast.makeText(this, task.exception.toString(), Toast.LENGTH_SHORT).show()
            }




        }

    }


}