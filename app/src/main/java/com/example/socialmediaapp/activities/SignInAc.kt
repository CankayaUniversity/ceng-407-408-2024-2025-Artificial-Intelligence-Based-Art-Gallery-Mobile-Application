@file:Suppress("DEPRECATION")

package com.example.socialmediaapp.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.example.socialmediaapp.R
import com.example.socialmediaapp.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.example.socialmediaapp.MainActivity
import android.util.Log

class SignInAc : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var pd: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this,R.layout.activity_sign_in)

        auth = FirebaseAuth.getInstance()

        binding.signInTextToSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpAc::class.java))

        }

        if(auth.currentUser != null)
        {
            startActivity(Intent(this, MainActivity::class.java))
        }

        pd = ProgressDialog(this)

        binding.loginButton.setOnClickListener {
            val email = binding.loginetemail.text.toString().trim()
            val password = binding.loginetpassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            signIn(email, password)
        }

    }

    private fun signIn(email: String, password: String)
    {
        pd.show()
        pd.setMessage("Signing In...")

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                pd.dismiss()
                Toast.makeText(this, "Logged In", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("fromSignIn",true) // Add Extra flag

                startActivity(intent)
                finish()
            }
        }.addOnFailureListener { exception ->
            pd.dismiss()
            val errorMessage = exception.message ?: "Authentication failed"
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            Log.e("Authentication", "Auth Failed: $errorMessage")
        }


    }


}