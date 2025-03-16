package com.example.socialmediaapp.modal

data class Posts (val username: String? = "",
                  val image:String?="",
                  val time: Long?=null,
                  val caption: String?="",
                  val likes: Int?= 0,
                  val userid: String?="",
                  val postid: String?="",
                  val profileImage: String?= "",){
}