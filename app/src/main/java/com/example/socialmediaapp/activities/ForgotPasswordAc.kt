package com.example.socialmediaapp.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.example.socialmediaapp.R
import com.example.socialmediaapp.databinding.ActivityForgotPasswordBinding
import com.example.socialmediaapp.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordAc : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        binding = DataBindingUtil.setContentView(this,R.layout.activity_forgot_password)

        binding.btnSubmit.setOnClickListener{
            val email: String = binding.etForgotEmail.text.toString().trim { it <= ' ' }
            if (email.isEmpty()){
                Toast.makeText(
                    this@ForgotPasswordAc,
                    "Please enter an email.",
                    Toast.LENGTH_SHORT
                ).show()
            }else{
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener{task ->
                        if(task.isSuccessful){
                            Toast.makeText(
                                this@ForgotPasswordAc,
                                "Email sent successfully to reset your password!",
                                Toast.LENGTH_SHORT
                            ).show()

                            finish()
                        }else{
                            Toast.makeText(
                                this@ForgotPasswordAc,
                                task.exception!!.message.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }

    }
}