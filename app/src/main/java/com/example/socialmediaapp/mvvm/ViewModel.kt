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

    //private val _posts = MutableLiveData<List<Posts>>()

    private val _feeds = MutableLiveData<List<Feed>>()
    fun loadMyFeed(): LiveData<List<Feed>> {
        val firestore = FirebaseFirestore.getInstance()

        viewModelScope.launch(Dispatchers.IO) {
            getThePeopleIFollow { followedUserIds ->
                val filteredUserIds = followedUserIds.filter { it != Utils.getUiLoggedIn() }

                if (filteredUserIds.isEmpty()) {
                    _feeds.postValue(emptyList())
                    return@getThePeopleIFollow
                }

                firestore.collection("Posts")
                    .whereIn("userid", filteredUserIds)
                    .addSnapshotListener { value, error ->
                        if (error != null) {
                            _feeds.postValue(emptyList())
                            return@addSnapshotListener
                        }

                        val feed = value?.documents?.mapNotNull {
                            it.toObject(Feed::class.java)
                        } ?: emptyList()

                        _feeds.postValue(feed.sortedByDescending { it.time })
                    }
            }
        }

        return _feeds
    }




/*
    fun loadMyFeed(): LiveData<List<Feed>> {
        val firestore = FirebaseFirestore.getInstance()
        val feeds = MutableLiveData<List<Feed>>()

        viewModelScope.launch(Dispatchers.IO) {
            getThePeopleIFollow { followedUserIds ->
                try {
                    // Ensure current user's ID is NOT included when fetching posts
                    val filteredUserIds = followedUserIds.filter { it != Utils.getUiLoggedIn() }

                    // If no followed users, return empty list
                    if (filteredUserIds.isEmpty()) {
                        feeds.postValue(emptyList())
                        return@getThePeopleIFollow
                    }

                    firestore.collection("Posts")
                        .whereIn("userid", filteredUserIds)
                        .addSnapshotListener { value, error ->
                            if (error != null) {
                                Log.e("FeedLoad", "Error loading feed: ${error.message}")
                                feeds.postValue(emptyList())
                                return@addSnapshotListener
                            }

                            val feed = mutableListOf<Feed>()
                            value?.documents?.forEach { documentSnapshot ->
                                val pModal = documentSnapshot.toObject(Feed::class.java)
                                pModal?.let {
                                    feed.add(it)
                                }
                            }

                            // Displaying the latest posts first
                            val sortedFeed = feed.sortedByDescending { it.time }
                            feeds.postValue(sortedFeed)
                        }

                } catch (e: Exception) {
                    Log.e("FeedLoad", "Exception in loadMyFeed: ${e.message}")
                    feeds.postValue(emptyList())
                }
            }
        }

        return feeds
    }
*/
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

    // Added for follow functionality
    fun followUser(userId: String): LiveData<Boolean> {
        val success = MutableLiveData<Boolean>()
        val firestore = FirebaseFirestore.getInstance()
        val currentUserId = Utils.getUiLoggedIn()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Current user's following list update
                firestore.collection("Follow").document(currentUserId)
                    .get()
                    .addOnSuccessListener { currentUserDoc ->
                        val followingList = if (currentUserDoc.exists()) {
                            (currentUserDoc.get("following_id") as? List<String>)?.toMutableList() ?: mutableListOf()
                        } else {
                            mutableListOf()
                        }

                        if (userId !in followingList) {
                            followingList.add(userId)

                            // Update the following list
                            firestore.collection("Follow").document(currentUserId)
                                .set(mapOf("following_id" to followingList))
                                .addOnSuccessListener {
                                    // 2. Update the target user's followers count
                                    firestore.collection("Users").document(userId)
                                        .get()
                                        .addOnSuccessListener { targetUserDoc ->
                                            val currentFollowers = targetUserDoc.getLong("followers")?.toInt() ?: 0

                                            // Increment followers count
                                            firestore.collection("Users").document(userId)
                                                .update("followers", currentFollowers + 1)
                                                .addOnSuccessListener {
                                                    // 3. Update current user's following count
                                                    firestore.collection("Users").document(currentUserId)
                                                        .get()
                                                        .addOnSuccessListener { currentUserInfo ->
                                                            val currentFollowing = currentUserInfo.getLong("following")?.toInt() ?: 0

                                                            firestore.collection("Users").document(currentUserId)
                                                                .update("following", currentFollowing + 1)
                                                                .addOnSuccessListener {
                                                                    success.postValue(true)
                                                                }
                                                                .addOnFailureListener {
                                                                    success.postValue(false)
                                                                }
                                                        }
                                                }
                                                .addOnFailureListener {
                                                    success.postValue(false)
                                                }
                                        }
                                }
                                .addOnFailureListener {
                                    success.postValue(false)
                                }
                        } else {
                            // Already following
                            success.postValue(true)
                        }
                    }
                    .addOnFailureListener {
                        success.postValue(false)
                    }
            } catch (e: Exception) {
                Log.e("Firebase", "Takip edilirken hata oluştu: ${e.message}")
                success.postValue(false)
            }
        }

        return success
    }

    // Added for unfollow functionality
    fun unfollowUser(userId: String): LiveData<Boolean> {
        val success = MutableLiveData<Boolean>()
        val firestore = FirebaseFirestore.getInstance()
        val currentUserId = Utils.getUiLoggedIn()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Current user's following list update
                firestore.collection("Follow").document(currentUserId)
                    .get()
                    .addOnSuccessListener { currentUserDoc ->
                        if (currentUserDoc.exists()) {
                            val followingList = (currentUserDoc.get("following_id") as? List<String>)?.toMutableList() ?: mutableListOf()

                            if (userId in followingList) {
                                followingList.remove(userId)

                                // Update the following list
                                firestore.collection("Follow").document(currentUserId)
                                    .set(mapOf("following_id" to followingList))
                                    .addOnSuccessListener {
                                        // 2. Update the target user's followers count
                                        firestore.collection("Users").document(userId)
                                            .get()
                                            .addOnSuccessListener { targetUserDoc ->
                                                val currentFollowers = targetUserDoc.getLong("followers")?.toInt() ?: 0
                                                val newFollowers = if (currentFollowers > 0) currentFollowers - 1 else 0

                                                // Decrement followers count
                                                firestore.collection("Users").document(userId)
                                                    .update("followers", newFollowers)
                                                    .addOnSuccessListener {
                                                        // 3. Update current user's following count
                                                        firestore.collection("Users").document(currentUserId)
                                                            .get()
                                                            .addOnSuccessListener { currentUserInfo ->
                                                                val currentFollowing = currentUserInfo.getLong("following")?.toInt() ?: 0
                                                                val newFollowing = if (currentFollowing > 0) currentFollowing - 1 else 0

                                                                firestore.collection("Users").document(currentUserId)
                                                                    .update("following", newFollowing)
                                                                    .addOnSuccessListener {
                                                                        success.postValue(true)
                                                                    }
                                                                    .addOnFailureListener {
                                                                        success.postValue(false)
                                                                    }
                                                            }
                                                    }
                                                    .addOnFailureListener {
                                                        success.postValue(false)
                                                    }
                                            }
                                    }
                                    .addOnFailureListener {
                                        success.postValue(false)
                                    }
                            } else {
                                // Already not following
                                success.postValue(true)
                            }
                        } else {
                            // No following document exists, so not following anyway
                            success.postValue(true)
                        }
                    }
                    .addOnFailureListener {
                        success.postValue(false)
                    }
            } catch (e: Exception) {
                Log.e("Firebase", "Takipten çıkarken hata oluştu: ${e.message}")
                success.postValue(false)
            }
        }

        return success
    }

    fun checkIfFollowing(userId: String): LiveData<Boolean> {
        val isFollowing = MutableLiveData<Boolean>()
        val firestore = FirebaseFirestore.getInstance()
        val currentUserId = Utils.getUiLoggedIn()

        firestore.collection("Follow").document(currentUserId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val followingList = documentSnapshot.get("following_id") as? List<String> ?: emptyList()
                    isFollowing.postValue(userId in followingList)
                } else {
                    isFollowing.postValue(false)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Takip durumu kontrol edilirken hata oluştu: ${exception.message}")
                isFollowing.postValue(false)
            }

        return isFollowing
    }

    // Added to support direct profile visit functionality
    fun likePost(postId: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        val firestore = FirebaseFirestore.getInstance()
        val currentUserId = Utils.getUiLoggedIn()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Get the current post to check likes
                firestore.collection("Posts").document(postId)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        val currentLikes = documentSnapshot.getLong("likes")?.toInt() ?: 0
                        val likedBy = documentSnapshot.get("likedBy") as? MutableList<String> ?: mutableListOf()

                        if (currentUserId !in likedBy) {
                            // Add user to liked list and increment likes
                            likedBy.add(currentUserId)

                            val updates = mapOf(
                                "likes" to currentLikes + 1,
                                "likedBy" to likedBy
                            )

                            firestore.collection("Posts").document(postId)
                                .update(updates)
                                .addOnSuccessListener {
                                    result.postValue(true)
                                }
                                .addOnFailureListener {
                                    result.postValue(false)
                                }
                        } else {
                            // User already liked this post
                            result.postValue(true)
                        }
                    }
                    .addOnFailureListener {
                        result.postValue(false)
                    }
            } catch (e: Exception) {
                Log.e("Firebase", "Post beğenilirken hata oluştu: ${e.message}")
                result.postValue(false)
            }
        }

        return result
    }

    // Added to support direct profile visit functionality
    fun unlikePost(postId: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        val firestore = FirebaseFirestore.getInstance()
        val currentUserId = Utils.getUiLoggedIn()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Get the current post to check likes
                firestore.collection("Posts").document(postId)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        val currentLikes = documentSnapshot.getLong("likes")?.toInt() ?: 0
                        val likedBy = documentSnapshot.get("likedBy") as? MutableList<String> ?: mutableListOf()

                        if (currentUserId in likedBy) {
                            // Remove user from liked list and decrement likes
                            likedBy.remove(currentUserId)
                            val newLikes = if (currentLikes > 0) currentLikes - 1 else 0

                            val updates = mapOf(
                                "likes" to newLikes,
                                "likedBy" to likedBy
                            )

                            firestore.collection("Posts").document(postId)
                                .update(updates)
                                .addOnSuccessListener {
                                    result.postValue(true)
                                }
                                .addOnFailureListener {
                                    result.postValue(false)
                                }
                        } else {
                            // User hasn't liked this post
                            result.postValue(true)
                        }
                    }
                    .addOnFailureListener {
                        result.postValue(false)
                    }
            } catch (e: Exception) {
                Log.e("Firebase", "Post beğeni kaldırılırken hata oluştu: ${e.message}")
                result.postValue(false)
            }
        }

        return result
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

        val postRef = firestore.collection("Posts").document(postId)

        // Yorum verisini alt koleksiyona ekle
        postRef.collection("comments")
            .add(commentData)
            .addOnSuccessListener {
                Log.d("Firestore", "Yorum başarıyla eklendi.")

                // Yorum sayısını artır
                postRef.update("comments", com.google.firebase.firestore.FieldValue.increment(1))
                    .addOnSuccessListener {
                        Log.d("Firestore", "Yorum sayısı güncellendi.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Yorum sayısı güncellenemedi: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Yorum eklenirken hata oluştu: ${e.message}")
            }
    }


/*
    fun loadMyFeed(): LiveData<List<Feed>> {
        val firestore = FirebaseFirestore.getInstance()

        viewModelScope.launch(Dispatchers.IO) {
            getThePeopleIFollow { followedUserIds ->
                val filteredUserIds = followedUserIds.filter { it != Utils.getUiLoggedIn() }

                if (filteredUserIds.isEmpty()) {
                    _feeds.postValue(emptyList())
                    return@getThePeopleIFollow
                }

                firestore.collection("Posts")
                    .whereIn("userid", filteredUserIds)
                    .addSnapshotListener { value, error ->
                        if (error != null) {
                            _feeds.postValue(emptyList())
                            return@addSnapshotListener
                        }

                        val feed = value?.documents?.mapNotNull {
                            it.toObject(Feed::class.java)
                        } ?: emptyList()

                        _feeds.postValue(feed.sortedByDescending { it.time })
                    }
            }
        }

        return _feeds
    }
*/




    fun sortFeedDescendingDate() {
        //_feeds.value = _feeds.value?.sortedByDescending { it.time }
        loadMyFeed()
    }

    fun sortFeedAscendingDate() {
        _feeds.value = _feeds.value?.sortedBy { it.time }
    }

    fun sortFeedMostLiked() {
        _feeds.value = _feeds.value?.sortedByDescending { it.likes }
    }

    fun sortFeedMostCommented() {
        _feeds.value = _feeds.value?.sortedByDescending { it.comments }
    }


    private val _posts = MutableLiveData<List<Posts>>()
    val posts: LiveData<List<Posts>> get() = _posts

    fun getAllPostsExceptCurrentUser(): LiveData<List<Posts>> {
        val firestore = FirebaseFirestore.getInstance()
        val currentUserId = Utils.getUiLoggedIn()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                firestore.collection("Posts")
                    .whereNotEqualTo("userid", currentUserId)
                    .addSnapshotListener { snapshot, exception ->
                        if (exception != null) {
                            Log.e("Firebase", "Error fetching posts: ${exception.message}")
                            _posts.postValue(emptyList())
                            return@addSnapshotListener
                        }

                        val postList = snapshot?.documents?.mapNotNull {
                            it.toObject(Posts::class.java)
                        }?.sortedByDescending { it.likes }

                        _posts.postValue(postList ?: emptyList())
                    }
            } catch (e: Exception) {
                Log.e("Firebase", "Exception in getAllPostsExceptCurrentUser: ${e.message}")
                _posts.postValue(emptyList())
            }
        }

        return _posts
    }


    fun sortFeedMostLikedSearch() {
        getAllPostsExceptCurrentUser()
    }

    fun sortFeedDescendingDateSearch() {
        _posts.value = _posts.value?.sortedByDescending { it.time }
    }

    fun sortFeedAscendingDateSearch() {
        _posts.value = _posts.value?.sortedBy { it.time }
    }


    fun getPostComments(postId: String): LiveData<List<Map<String, Any>>> {
        val comments = MutableLiveData<List<Map<String, Any>>>()
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("Posts")
            .document(postId)
            .collection("comments")
            .orderBy("time", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("Firestore", "Error getting comments: ${exception.message}")
                    comments.postValue(emptyList())
                    return@addSnapshotListener
                }

                val commentsList = snapshot?.documents?.mapNotNull { doc ->
                    doc.data
                } ?: emptyList()

                comments.postValue(commentsList)
            }

        return comments
    }



}