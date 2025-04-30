package com.example.socialmediaapp.modal

data class Feed(
    val username:    String? ="",
    val comments:    Int?=0,
    var likes:       Int?=0,
    val postid:      String? = "",
    val userid:      String?= "",
    val image:       String?= "",
    val imageposter: String?= "",
    val time:        Long?= null,
    val caption:     String? ="",

    var likers: List<String>? = null // <-- Bunu ekle
    ){}