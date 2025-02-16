package com.example.socialmediaapp.mvvm
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmediaapp.Utils
import com.example.socialmediaapp.modal.Posts
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.viewModelScope
import com.example.socialmediaapp.modal.Users
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


}