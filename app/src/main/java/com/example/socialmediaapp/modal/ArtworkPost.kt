package com.example.socialmediaapp.modal

import com.google.firebase.Timestamp

data class ArtworkPost(
    val docId: String = "",
    val userId: String = "",
    val username: String = "",
    val userImageUrl: String = "",
    val imageUrl: String = "",
    val title: String = "",
    val story: String = "",
    var likes: Int = 0,
    var comments: Int = 0,
    var isLiked: Boolean = false,
    val createdAt: Timestamp? = null
)