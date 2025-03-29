package com.example.socialmediaapp.modal

import com.google.firebase.Timestamp

data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userImageUrl: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null
)