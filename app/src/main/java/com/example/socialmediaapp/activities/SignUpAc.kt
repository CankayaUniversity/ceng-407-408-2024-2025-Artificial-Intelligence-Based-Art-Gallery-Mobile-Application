@file:Suppress("DEPRECATION")
package com.example.socialmediaapp.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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

        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        pd = ProgressDialog(this)

        // Set up back button functionality if it exists in the layout
        try {
            val backButton = findViewById<View>(R.id.backButton)
            backButton?.setOnClickListener {
                onBackPressed()
            }
        } catch (e: Exception) {
            // Button doesn't exist in the layout, ignore
        }

        binding.signUpTextToSignIn.setOnClickListener {
            startActivity(Intent(this, SignInAc::class.java))
        }

        binding.signUpButton.setOnClickListener {
            // Get username and surname if they exist in the layout
            var username = ""
            var surname = ""

            val password = binding.signUpetpassword.text.toString()
            val confirmPassword = binding.signUpetconfirmpassword.text.toString()

            // Password ve Confrim Password eslesme kontrol
            if (password != confirmPassword) {
                Toast.makeText(this, "Password fields do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            try {
                // Try to find the username field
                val usernameField = findViewById<View>(R.id.signUpUsername)
                if (usernameField != null) {
                    // Field exists but we'll use name as username for database
                    username = binding.signUpName.text.toString()
                }
            } catch (e: Exception) {
                // Field doesn't exist
            }

            try {
                // Try to find the surname field
                val surnameField = findViewById<View>(R.id.signUpSurname)
                if (surnameField != null) {
                    // Field exists, but we'll ignore for now as original code doesn't use it
                    surname = ""
                }
            } catch (e: Exception) {
                // Field doesn't exist
            }

            // Check required fields using original logic
            if (binding.signUpnetemail.text.isNotEmpty() &&
                binding.signUpetpassword.text.isNotEmpty() &&
                binding.signUpName.text.isNotEmpty()) {

                val email = binding.signUpnetemail.text.toString()
                val password = binding.signUpetpassword.text.toString()
                val name = binding.signUpName.text.toString()

                // Use the original name as the username to maintain compatibility
                signInUp(name, email, password, username, surname)
            } else {
                Toast.makeText(this, "Fill the required fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInUp(name: String, email: String, password: String, username: String, surname: String) {
        pd.show()
        pd.setMessage("Registering User")

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser

                val hashMap = hashMapOf(
                    "userid" to user!!.uid,
                    "image" to "https://upload.wikimedia.org/wikipedia/en/b/bd/Doraemon_character.png",
                    "username" to username,
                    "email" to email,
                    "followers" to 0,
                    "following" to 0,
                    "user" to name,
                    "surname" to surname
                )

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
            } else {
                pd.dismiss()
                Toast.makeText(this, task.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }
}