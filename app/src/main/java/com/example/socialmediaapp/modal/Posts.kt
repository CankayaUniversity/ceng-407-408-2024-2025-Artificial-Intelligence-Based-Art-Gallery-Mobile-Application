package com.example.socialmediaapp.modal

data class Posts (val username: String? = "",
                  var image:String?="",
                  val time: Long?=null,
                  var caption: String?="",
                  var likes: Int?= 0,
                  val userid: String?="",
                  var postid: String?="",
                  var comments: Int? = null,
                  val profileImage: String?= "",){
    var title: String? = null
    var story: String? = null
}