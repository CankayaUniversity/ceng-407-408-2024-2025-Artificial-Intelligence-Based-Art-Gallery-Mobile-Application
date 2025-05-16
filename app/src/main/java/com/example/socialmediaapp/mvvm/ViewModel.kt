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
import com.example.socialmediaapp.modal.Notification
import com.example.socialmediaapp.modal.Users
import com.google.firebase.firestore.FieldValue
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
                Log.e("Firebase", "\n" + "An error occurred while retrieving user information: ${exception.message}")
            }

        return user
    }

    fun getOtherUserPosts(userId: String): LiveData<List<Posts>> {
        val posts = MutableLiveData<List<Posts>>()
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("Posts").whereEqualTo("userid", userId).addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Log.e("Firebase", "\n" + "An error occurred while retrieving posts: ${exception.message}")
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
                Log.e("Firebase", "An error occurred while retrieving user statistics: ${exception.message}")
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
                Log.e("Firebase", "An error occurred while getting the post count: ${exception.message}")
            }

        return postCount
    }

    // Update followUser in ViewModel.kt to include notification
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
                                                                    // Get current user info for notification
                                                                    getCurrentUserInfo { username, userImage ->
                                                                        // Create follow notification
                                                                        createNotification(
                                                                            toUserId = userId,
                                                                            fromUserId = currentUserId,
                                                                            fromUsername = username,
                                                                            fromUserImage = userImage,
                                                                            type = "follow"
                                                                        )
                                                                    }

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
                Log.e("Firebase", "Error while following: ${e.message}")
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
                Log.e("Firebase", "An error occurred while unfollowing: ${e.message}")
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
                Log.e("Firebase", "\n" + "An error occurred while checking tracking status: ${exception.message}")
                isFollowing.postValue(false)
            }

        return isFollowing
    }





    // Modify addComment function in ViewModel.kt
    fun addComment(postId: String, commentText: String) {
        val firestore = FirebaseFirestore.getInstance()

        // Current user's ID and name
        val userId = Utils.getUiLoggedIn()
        val username = name.value ?: "Unknown"
        val userImage = image.value ?: ""

        // Comment data
        val commentData = mapOf(
            "userId" to userId,
            "username" to username,
            "comment" to commentText,
            "time" to com.google.firebase.Timestamp.now()
        )

        val postRef = firestore.collection("Posts").document(postId)

        // Add comment to subcollection
        postRef.collection("comments")
            .add(commentData)
            .addOnSuccessListener {
                Log.d("Firestore", "Comment added successfully.")

                // Increment comment count
                postRef.update("comments", com.google.firebase.firestore.FieldValue.increment(1))
                    .addOnSuccessListener {
                        Log.d("Firestore", "Comment count updated.")

                        // Get post info to create notification
                        postRef.get().addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {
                                val post = documentSnapshot.toObject(Posts::class.java)
                                val postOwnerId = post?.userid ?: ""
                                val postCaption = post?.caption ?: ""

                                // Create notification for post owner
                                createNotification(
                                    toUserId = postOwnerId,
                                    fromUserId = userId,
                                    fromUsername = username,
                                    fromUserImage = userImage,
                                    type = "comment",
                                    postId = postId,
                                    postCaption = postCaption,
                                    text = commentText
                                )
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Failed to update comment count: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error adding comment: ${e.message}")
            }
    }

    // Add this function to ViewModel.kt to get current user's info
    fun getCurrentUserInfo(callback: (String, String) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("Users").document(Utils.getUiLoggedIn())
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username") ?: "Unknown"
                    val userImage = document.getString("image") ?: ""
                    callback(username, userImage)
                } else {
                    callback("Unknown", "")
                }
            }
            .addOnFailureListener {
                callback("Unknown", "")
            }
    }




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


    fun updatePostLikeStatus(postId: String, isLiked: Boolean, userId: String) {
        // Get a reference to the post in Firestore
        val postRef = FirebaseFirestore.getInstance().collection("Posts").document(postId)

        if (isLiked) {
            // Like the post
            postRef.update(
                "likes", FieldValue.increment(1),
                "likers", FieldValue.arrayUnion(userId)
            )
        } else {
            // Unlike the post
            postRef.update(
                "likes", FieldValue.increment(-1),
                "likers", FieldValue.arrayRemove(userId)
            )
        }

        // No need to reload the entire feed - the UI will update with the local change
    }



    fun createNotification(
        toUserId: String,
        fromUserId: String,
        fromUsername: String,
        fromUserImage: String,
        type: String,
        postId: String = "",
        postCaption: String = "",
        text: String = ""
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val notificationId = firestore.collection("Notifications").document().id

        val notification = Notification(
            id = notificationId,
            userId = toUserId,
            fromUserId = fromUserId,
            fromUsername = fromUsername,
            fromUserImage = fromUserImage,
            type = type,
            postId = postId,
            postCaption = postCaption,
            text = text,
            time = System.currentTimeMillis(),
            seen = false
        )

        // Don't create notifications for user's own actions
        if (toUserId == fromUserId) {
            return
        }

        firestore.collection("Notifications").document(notificationId)
            .set(notification)
            .addOnSuccessListener {
                Log.d("Notification", "Notification created successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Notification", "Error creating notification: ${e.message}")
            }
    }

    // Function to get notifications for current user
    fun getNotifications(): LiveData<List<Notification>> {
        val notifications = MutableLiveData<List<Notification>>()
        val firestore = FirebaseFirestore.getInstance()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                firestore.collection("Notifications")
                    .whereEqualTo("userId", Utils.getUiLoggedIn())
                    .orderBy("time", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, exception ->
                        if (exception != null) {
                            Log.e("Firebase", "Error fetching notifications: ${exception.message}")
                            return@addSnapshotListener
                        }

                        val notificationList = snapshot?.documents?.mapNotNull {
                            it.toObject(Notification::class.java)
                        } ?: emptyList()

                        notifications.postValue(notificationList)
                    }
            } catch (e: Exception) {
                Log.e("Firebase", "Exception in getNotifications: ${e.message}")
            }
        }

        return notifications
    }

    // Function to mark notification as seen
    fun markNotificationAsSeen(notificationId: String) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("Notifications").document(notificationId)
            .update("seen", true)
            .addOnSuccessListener {
                Log.d("Notification", "Notification marked as seen")
            }
            .addOnFailureListener { e ->
                Log.e("Notification", "Error marking notification as seen: ${e.message}")
            }
    }

    // Function to get unread notification count
    fun getUnreadNotificationCount(): LiveData<Int> {
        val count = MutableLiveData<Int>()
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("Notifications")
            .whereEqualTo("userId", Utils.getUiLoggedIn())
            .whereEqualTo("seen", false)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("Firebase", "Error fetching unread notifications: ${exception.message}")
                    count.postValue(0)
                    return@addSnapshotListener
                }

                count.postValue(snapshot?.size() ?: 0)
            }

        return count
    }


}