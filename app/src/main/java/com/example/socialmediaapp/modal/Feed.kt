package com.example.socialmediaapp.modal

data class Feed(
    val username: String? ="", val comments: String?= "",
    val likes: Int?=0, val postid: String? = "", val userid: String?= "",
    val image: String?= "", val imageposter: String?= "", val time: Long?= null, val caption: String? ="",

    ){


}