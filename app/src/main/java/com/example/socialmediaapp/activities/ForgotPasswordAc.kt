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
import com.google.firebase.firestore.FirebaseFirestore

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

                val db = FirebaseFirestore.getInstance()

                db.collection("Users").whereEqualTo("email", email).get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val documents = task.result
                            if (documents != null && !documents.isEmpty) {
                                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                                    .addOnCompleteListener { resetTask ->
                                        if (resetTask.isSuccessful) {
                                            Toast.makeText(
                                                this@ForgotPasswordAc,
                                                "Email sent successfully to reset your password!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            finish()
                                        } else {
                                            Toast.makeText(
                                                this@ForgotPasswordAc,
                                                resetTask.exception!!.message.toString(),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(
                                    this@ForgotPasswordAc,
                                    "There is no user connected to this email address.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
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




/*FirebaseAuth.getInstance().sendPasswordResetEmail(email)
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
}*/