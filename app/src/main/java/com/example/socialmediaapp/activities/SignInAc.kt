@file:Suppress("DEPRECATION")
package com.example.socialmediaapp.activities

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.socialmediaapp.MainActivity
import com.example.socialmediaapp.R
import com.example.socialmediaapp.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth

class SignInAc : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var pd: ProgressDialog
    private var passwordVisible = false

    // SharedPreferences for Remember Me functionality
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_NAME = "LoginPrefs"
    private val KEY_REMEMBER = "remember"
    private val KEY_EMAIL = "email"
    private val KEY_PASSWORD = "password"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up data binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_in)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // Check if Remember Me was enabled before
        if (sharedPreferences.getBoolean(KEY_REMEMBER, false)) {
            binding.cbRememberMe.isChecked = true
            binding.loginetemail.setText(sharedPreferences.getString(KEY_EMAIL, ""))
            binding.loginetpassword.setText(sharedPreferences.getString(KEY_PASSWORD, ""))

            // Auto sign-in option - uncomment if you want auto login
            // validateAndSignIn()
        }

        // Check if user is already signed in
        if(auth.currentUser != null) {
            startActivity(Intent(this, ProfilePageActivity::class.java))
            finish()
        }

        // Initialize progress dialog
        pd = ProgressDialog(this).apply {
            setCancelable(false)
            setMessage("Signing In")
        }

        // Set up click listeners
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Register button click
        binding.signInTextToSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpAc::class.java))
        }

        // Forgot password click
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordAc::class.java))
        }

        // Password visibility toggle
        binding.passwordVisibilityToggle.setOnClickListener {
            togglePasswordVisibility()
        }

        // Login button click
        binding.loginButton.setOnClickListener {
            validateAndSignIn()
        }

        // Remember Me checkbox change listener
        binding.cbRememberMe.setOnCheckedChangeListener { _, isChecked ->
            // If unchecked, clear saved credentials
            if (!isChecked) {
                clearSavedCredentials()
            }
        }
    }

    private fun saveCredentials(email: String, password: String) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(KEY_REMEMBER, true)
        editor.putString(KEY_EMAIL, email)
        editor.putString(KEY_PASSWORD, password)
        editor.apply()
    }

    private fun clearSavedCredentials() {
        val editor = sharedPreferences.edit()
        editor.putBoolean(KEY_REMEMBER, false)
        editor.remove(KEY_EMAIL)
        editor.remove(KEY_PASSWORD)
        editor.apply()
    }

    private fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible

        if (passwordVisible) {
            // Show password
            binding.loginetpassword.transformationMethod = null
            binding.passwordVisibilityToggle.setImageResource(R.drawable.ic_visibility)
        } else {
            // Hide password
            binding.loginetpassword.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.passwordVisibilityToggle.setImageResource(R.drawable.ic_visibility_off)
        }

        // Maintain cursor position
        binding.loginetpassword.setSelection(binding.loginetpassword.text.length)
    }

    private fun validateAndSignIn() {
        val email = binding.loginetemail.text.toString().trim()
        val password = binding.loginetpassword.text.toString().trim()

        // Validate input fields
        when {
            email.isEmpty() -> {
                Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show()
                binding.loginetemail.requestFocus()
            }
            password.isEmpty() -> {
                Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show()
                binding.loginetpassword.requestFocus()
            }
            else -> {
                // Attempt sign in
                signIn(email, password)
            }
        }
    }

    private fun signIn(email: String, password: String) {
        pd.show()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Save credentials if Remember Me is checked
                    if (binding.cbRememberMe.isChecked) {
                        saveCredentials(email, password)
                    } else {
                        clearSavedCredentials()
                    }

                    // Navigate to main activity
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    Toast.makeText(this, "Logged In Successfully", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                // Handle specific error types
                val errorMessage = when {
                    exception.message?.contains("no user record") == true ->
                        "No account exists with this email"
                    exception.message?.contains("password is invalid") == true ->
                        "Invalid password"
                    else -> "Authentication failed"
                }

                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                Log.e("Authentication", "Auth Failed: ${exception.message}")
            }
            .addOnCompleteListener {
                // Always dismiss dialog when complete
                pd.dismiss()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure progress dialog is dismissed
        if (::pd.isInitialized && pd.isShowing) {
            pd.dismiss()
        }
    }
}