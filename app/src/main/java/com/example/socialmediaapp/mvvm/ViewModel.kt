package com.example.socialmediaapp.mvvm
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmediaapp.Utils
import com.example.socialmediaapp.modal.Posts
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.viewModelScope
import com.example.socialmediaapp.modal.Feed
import com.example.socialmediaapp.modal.Users
import com.google.firebase.firestore.auth.User
import kotlinx.coroutines.launch

class ViewModel: ViewModel() {

    val name = MutableLiveData<String>()
    val image = MutableLiveData<String>()
    val followers = MutableLiveData<String>()
    val following = MutableLiveData<String>()


    init {

        getCurrentUser()

    }

    fun getCurrentUser() = viewModelScope.launch(Dispatchers.IO) {

        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("Users").document(Utils.getUiLoggedIn()).addSnapshotListener { value, error ->


            if (error!=null){

                return@addSnapshotListener

            }

            if (value!=null && value.exists()){

                val users = value.toObject(Users::class.java)
                name.value  = users!!.username!!
                image.value = users.image!!
                followers.value = users.followers!!.toString()
                following.value = users.following!!.toString()

            }

        }


    }



    fun getMyPosts(): LiveData<List<Posts>> {
        val posts = MutableLiveData<List<Posts>>()
        val firestore = FirebaseFirestore.getInstance()

        // on the background thread
        // ! Using coroutines here might be unnecessary,
        // Since Firestore is already launching another thread to perform the operation
        // So in terms of performance, using less threads as possible is better for
        // reducing the thread overheads
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firestore.collection("Posts")
                    .whereEqualTo("userid", Utils.getUiLoggedIn())
                    .addSnapshotListener { snapshot, exception ->
                        if (exception != null) {
                            // Handle the exception here
                            return@addSnapshotListener
                        }

                        val postList = snapshot?.documents?.mapNotNull {
                            it.toObject(Posts::class.java)
                        }
                            ?.sortedByDescending { it.time
                            }

                        posts.postValue(postList!!) // Switch back to the main thread
                    }
            } catch (e: Exception) {
                // Handle any exceptions that occur during the Firestore operation
            }
        }



        return posts
    }


    fun getAllUsers(): LiveData<List<Users>> {

        val users = MutableLiveData<List<Users>>()

        val firestore = FirebaseFirestore.getInstance()

        // on the background thread
        // ! Using coroutines here might be unnecessary,
        // Since Firestore is already launching another thread to perform the operation
        // So in terms of performance, using less threads as possible is better for
        // reducing the thread overheads
        viewModelScope.launch(Dispatchers.IO)
        {
            try {
                firestore.collection("Users").addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        // Handle the exception here
                        return@addSnapshotListener
                    }

                    val usersList = mutableListOf<Users>()
                    snapshot?.documents?.forEach { document ->
                        val user = document.toObject(Users::class.java)
                        // gets the userlist that is not the current user
                        if (user != null && user.userid != Utils.getUiLoggedIn()) {
                            usersList.add(user)
                        }
                    }

                    users.postValue(usersList) // Switch back to the main thread
                }
            } catch (e: Exception) {
                // Handle any exceptions that occur during the Firestore operation
            }
        }

        return users
    }


    fun loadMyFeed(): LiveData<List<Feed>>
    {
        val firestore = FirebaseFirestore.getInstance()

        val feeds = MutableLiveData<List<Feed>>()


        viewModelScope.launch(Dispatchers.IO)
        {
            getThePeopleIFollow{list->

                try {

                    firestore.collection("Posts").whereIn("userid", list)
                        .addSnapshotListener{value,error->

                            if (error != null) {
                                return@addSnapshotListener
                            }

                            val feed = mutableListOf<Feed>()
                            value?.documents?.forEach{ documentSnapshot->
                                val pModal = documentSnapshot.toObject(Feed::class.java)
                                pModal?.let {
                                    feed.add(it)
                                }
                            }
                            // Displaying the latest posts first
                            val sortedFeed = feed.sortedByDescending { it.time }
                            feeds.postValue(sortedFeed)

                        }

                }catch (e: Exception){}


            }

        }


        return feeds
    }

    // get the ids of those who I follow
    fun getThePeopleIFollow(callback: (List<String>) -> Unit)
    {
        val firestore = FirebaseFirestore.getInstance()

        val ifollowlist = mutableListOf<String>()
        ifollowlist.add(Utils.getUiLoggedIn())

        firestore.collection("Follow").document(Utils.getUiLoggedIn())
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val followingIds = documentSnapshot.get("following_id") as? List<String>
                    val updatedList = followingIds?.toMutableList() ?: mutableListOf()

                    ifollowlist.addAll(updatedList)

                    Log.e("ListOfFeed", ifollowlist.toString())
                    callback(ifollowlist)
                } else {
                    callback(ifollowlist)
                }
            }
    }


    fun getOtherUser(userId: String): LiveData<Users> {
        val user = MutableLiveData<Users>()
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("Users").document(userId).get()
            .addOnSuccessListener { documentSnapshot ->
                val userInfo = documentSnapshot.toObject(Users::class.java) ?: Users(username = "Unknown", image = "")
                    user.postValue(userInfo)

            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Kullanıcı bilgisi alınırken hata oluştu: ${exception.message}")
            }

        return user
    }

    fun getOtherUserPosts(userId: String): LiveData<List<Posts>> {
        val posts = MutableLiveData<List<Posts>>()
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("Posts").whereEqualTo("userid", userId).addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Log.e("Firebase", "Gönderiler alınırken hata oluştu: ${exception.message}")
                return@addSnapshotListener
            }

            val postList = snapshot?.documents?.mapNotNull {
                it.toObject(Posts::class.java)
            }?.sortedByDescending { it.time }

            posts.postValue(postList ?: emptyList())
        }

        return posts
    }

    fun getOtherUserStats(userId: String): LiveData<Map<String, Int>> {
        val stats = MutableLiveData<Map<String, Int>>()
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("Users").document(userId).get()
            .addOnSuccessListener { documentSnapshot ->
                val followers = documentSnapshot.getLong("followers")?.toInt() ?: 0
                val following = documentSnapshot.getLong("following")?.toInt() ?: 0
                stats.postValue(mapOf("followers" to followers, "following" to following))
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Kullanıcı istatistikleri alınırken hata oluştu: ${exception.message}")
            }

        return stats
    }

    fun getOtherUserPostCount(userId: String): LiveData<Int> {
        val postCount = MutableLiveData<Int>()
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("Posts").whereEqualTo("userid", userId).get()
            .addOnSuccessListener { querySnapshot ->
                postCount.postValue(querySnapshot.size())
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Post sayısı alınırken hata oluştu: ${exception.message}")
            }

        return postCount
    }

    fun addComment(postId: String, commentText: String) {
        val firestore = FirebaseFirestore.getInstance()

        // Giriş yapan kullanıcının ID'si ve adı
        val userId = Utils.getUiLoggedIn()
        val username = name.value ?: "Unknown"

        // Yorum verisi
        val commentData = mapOf(
            "userId" to userId,
            "username" to username,
            "comment" to commentText,
            "time" to com.google.firebase.Timestamp.now()
        )

        // Yorumları "comments" alt koleksiyonuna ekle
        firestore.collection("Posts")
            .document(postId)
            .collection("comments")
            .add(commentData)
            .addOnSuccessListener {
                Log.d("Firestore", "Yorum başarıyla eklendi.")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Yorum eklenirken hata oluştu: ${e.message}")
            }
    }




}