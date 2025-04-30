package com.example.socialmediaapp

import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Utils {

    companion object {
        private val auth = FirebaseAuth.getInstance()
        private var userid : String = ""

        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_IMAGE_PICK = 2
        const val PROFILE_IMAGE_CAPTURE = 3
        const val PROFILE_IMAGE_PICK = 4

        fun getUiLoggedIn(): String {
            if (auth.currentUser != null) {
                userid = auth.currentUser!!.uid
            }
            return userid
        }

        fun getTime(): Long {
            // in seconds
            val unixTimestamp: Long = System.currentTimeMillis() / 1000
            return unixTimestamp
        }

        // Moved to companion object to make it static
        fun getTimeAgo(timestamp: Long): String {
            val currentTime = System.currentTimeMillis()
            val timeDifference = currentTime - timestamp

            // Convert to appropriate time unit
            val seconds = timeDifference / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            return when {
                seconds < 60 -> "just now"
                minutes < 60 -> "$minutes minutes ago"
                hours < 24 -> "$hours hours ago"
                days < 7 -> "$days days ago"
                else -> {
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }
}