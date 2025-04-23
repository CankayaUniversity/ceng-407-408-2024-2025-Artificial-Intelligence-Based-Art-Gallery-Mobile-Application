package com.example.socialmediaapp.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaapp.modal.Users
import de.hdodenhof.circleimageview.CircleImageView
import android.view.View as AndroidViewView
import com.example.socialmediaapp.R
import com.example.socialmediaapp.Utils
import com.example.socialmediaapp.fragments.SearchFragmentDirections
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class SearchUsersAdapter(
    private var mContext: Context,
    private var mUser: List<Users>,
    private var isFragment: Boolean = false
) : RecyclerView.Adapter<SearchUsersAdapter.ViewHolder>() {

    private val firebaseUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.user_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = mUser.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = mUser[position]

        // Set user details
        holder.userNameTextView.text = user.username
        Glide.with(holder.itemView.context).load(user.image).into(holder.userProfileImage)

        // Check following status
        checkFollowingStatus(user.userid.toString(), holder.followButton)

        // Navigate to user's profile
        holder.useritem.setOnClickListener {
            // view ->
            // Use Navigation Components to navigate
            /// val action = SearchFragmentDirections.actionSearchFragmentToOtherUsersFragment(user.userid ?: "")
            // view.findNavController().navigate(action)
        }

        // Follow/Unfollow functionality
        holder.followButton.setOnClickListener {
            if (holder.followButton.text.toString() == "Follow") {
                followUser(user)
            } else {
                unfollowUser(user)
            }
        }
    }

    // Existing followUser, unfollowUser, and checkFollowingStatus methods remain the same...

    // ViewHolder class remains unchanged
    class ViewHolder(@NonNull itemView: AndroidViewView) : RecyclerView.ViewHolder(itemView) {
        var followButton: Button = itemView.findViewById(R.id.user_item_follow)
        var userNameTextView: TextView = itemView.findViewById(R.id.user_item_search_username)
        var useritem: LinearLayout = itemView.findViewById(R.id.user_item)
        var userFullnameTextView: TextView = itemView.findViewById(R.id.user_item_search_fullname)
        var userProfileImage: CircleImageView = itemView.findViewById(R.id.user_item_image)
    }

    // Existing helper methods for following and checking follow status
    fun followUser(user: Users) {
        val firestore = FirebaseFirestore.getInstance()
        val userDocRef = firestore.collection("Users").document(Utils.getUiLoggedIn())

        // Increment the logged-in user's following count
        userDocRef.update("following", FieldValue.increment(1))

        val userToFollowDocRef = firestore.collection("Users").document(user.userid!!)

        // Increment the user being followed's followers count
        userToFollowDocRef.update("followers", FieldValue.increment(1))

        val followDocRef = firestore.collection("Follow").document(Utils.getUiLoggedIn())

        followDocRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                // Collection exists, update the list
                val existingIds = documentSnapshot.get("following_id") as? List<String>
                val newIds = existingIds?.toMutableList() ?: mutableListOf()
                newIds.add(user.userid)
                followDocRef.update(hashMapOf("following_id" to newIds) as Map<String, Any>)
            } else {
                // Logged user following a new user
                val followData = hashMapOf(
                    "following_id" to listOf(user.userid)
                )
                followDocRef.set(followData)
            }
        }
    }

    fun unfollowUser(user: Users) {
        val firestore = FirebaseFirestore.getInstance()
        val userDocRef = firestore.collection("Users").document(Utils.getUiLoggedIn())
        val userToUnfollowDocRef = firestore.collection("Users").document(user.userid!!)

        userDocRef.get().addOnSuccessListener { userSnapshot ->
            if (userSnapshot.exists()) {
                val followingCount = userSnapshot.getLong("following") ?: 0

                firestore.runTransaction { transaction ->
                    val userToUnfollowSnapshot = transaction.get(userToUnfollowDocRef)

                    val followersCount = userToUnfollowSnapshot.getLong("followers") ?: 0
                    val newFollowersCount = if (followersCount > 0) followersCount - 1 else 0
                    transaction.update(userToUnfollowDocRef, "followers", newFollowersCount)

                    val newFollowingCount = if (followingCount > 0) followingCount - 1 else 0
                    transaction.update(userDocRef, "following", newFollowingCount)
                }.addOnSuccessListener {
                    firestore.collection("Follow").document(Utils.getUiLoggedIn()).get().addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            val followingIds = documentSnapshot.get("following_id") as? List<String>
                            val newIds = followingIds?.toMutableList() ?: mutableListOf()

                            if (newIds.contains(user.userid)) {
                                newIds.remove(user.userid)
                            }

                            if (newIds.isEmpty()) {
                                documentSnapshot.reference.delete()
                            } else {
                                val followDocRef = firestore.collection("Follow").document(Utils.getUiLoggedIn())
                                followDocRef.set(hashMapOf("following_id" to newIds))
                            }
                        }
                    }
                }.addOnFailureListener { exception ->
                    // Handle transaction failure
                    Log.e("Unfollow", "Failed to unfollow user", exception)
                }
            }
        }
    }

    private fun checkFollowingStatus(uid: String, followButton: Button) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val followRef = FirebaseFirestore.getInstance()
            .collection("Follow")
            .document(currentUserId)

        followRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Firestore Error", error.message.toString())
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val followingIds = snapshot.get("following_id") as? List<String>

                if (followingIds != null && followingIds.contains(uid)) {
                    followButton.text = "Following"
                } else {
                    followButton.text = "Follow"
                }
            } else {
                followButton.text = "Follow"
            }
        }
    }
}