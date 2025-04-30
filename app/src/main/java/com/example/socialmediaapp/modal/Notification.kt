package com.example.socialmediaapp.modal

data class Notification(
    val id: String = "",
    val userId: String = "",  // Receiver of notification
    val fromUserId: String = "", // Sender of notification
    val fromUsername: String = "",
    val fromUserImage: String = "",
    val type: String = "",  // "like", "comment", "follow"
    val postId: String = "", // For like and comment notifications
    val postCaption: String = "", // Caption text for the post
    val text: String = "",  // Comment text for comment notifications
    val time: Long = 0,
    val seen: Boolean = false
)
