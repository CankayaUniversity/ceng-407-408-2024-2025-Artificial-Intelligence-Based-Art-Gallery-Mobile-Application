// Create ChangePasswordActivity.kt file
package com.example.socialmediaapp.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.example.socialmediaapp.R
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth

    override fun getContentLayoutId(): Int {
        return R.layout.activity_change_password
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set toolbar title
        setToolbarTitle("Change Password")

        auth = FirebaseAuth.getInstance()

        // Setup back button
        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            onBackPressed()
        }

        val newPasswordEditText = findViewById<EditText>(R.id.new_password_edit_text)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirm_password_edit_text)
        val changePasswordButton = findViewById<Button>(R.id.change_password_button)

        changePasswordButton.setOnClickListener {
            val newPassword = newPasswordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            // Validate inputs
            when {
                newPassword.isEmpty() -> {
                    Toast.makeText(this, "New password cannot be empty", Toast.LENGTH_SHORT).show()
                }
                newPassword.length < 6 -> {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                }
                newPassword != confirmPassword -> {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    updatePassword(newPassword)
                }
            }
        }
    }

    private fun updatePassword(newPassword: String) {
        val user = auth.currentUser

        if (user != null) {
            user.updatePassword(newPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to update password: ${task.exception?.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "User not authenticated. Please sign in again", Toast.LENGTH_SHORT).show()
            // Redirect to sign in page
            auth.signOut()
            val intent = android.content.Intent(this, SignInAc::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}