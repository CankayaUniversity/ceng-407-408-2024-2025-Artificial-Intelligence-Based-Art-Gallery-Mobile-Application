package com.example.socialmediaapp.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaapp.modal.Users
import de.hdodenhof.circleimageview.CircleImageView
import android.view.View as AndroidViewView
import com.example.socialmediaapp.R
import com.example.socialmediaapp.Utils
import com.example.socialmediaapp.fragments.ProfileFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class SearchUsersAdapter(private var mContext:Context,
    private var mUser:List<Users>,
    private var isFragment:Boolean=false):RecyclerView.Adapter<SearchUsersAdapter.ViewHolder>()
{
    private val firebaseUser:FirebaseUser?= com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchUsersAdapter.ViewHolder{

        //to make user item available in search item
        val view=LayoutInflater.from(mContext).inflate(R.layout.user_item_layout,parent,false)
        return SearchUsersAdapter.ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mUser.size
    }

    override fun onBindViewHolder(holder: SearchUsersAdapter.ViewHolder, position: Int)
    {
        //to display the user data
        val user=mUser[position]
        holder.userNameTextView.text = user.username
        //holder.userFullnameTextView.text = user.username
        Glide.with(holder.itemView.context).load(user.image).into(holder.userProfileImage)

        checkFollowingStatus(user.userid.toString(),holder.followButton)

        //to go to searched user's profile
        holder.useritem.setOnClickListener(object :AndroidViewView.OnClickListener{

            override fun onClick(v: AndroidViewView?){
                val pref=mContext.getSharedPreferences("PREFS",Context.MODE_PRIVATE).edit()
                pref.putString("profileid",user.userid)
                pref.apply()
                // This part of the code could be incorrect!!!!
                (mContext as FragmentActivity).supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment,ProfileFragment()).commit()
            }
        })

        holder.followButton.setOnClickListener{
            if(holder.followButton.text.toString()=="Follow")
            {
                followUser(user)
            }

            else{
                unfollowUser(user)
            }
        }

    }

    fun followUser(user: Users)
    {
        val firestore = FirebaseFirestore.getInstance()
        // Get reference to the logged-in user's document
        val userDocRef = firestore.collection("Users").document(Utils.getUiLoggedIn())
        // Increment the logged-in user's following count
        userDocRef.update("following", FieldValue.increment(1))

        // Get reference to the user being followed
        val userToFollowDocRef = firestore.collection("Users").document(user.userid!!)

        // Increment the user being followed's followers count
        userToFollowDocRef.update("followers", FieldValue.increment(1))


        // Add a new document to the Follow collection to represent the follow relationship
        val followDocRef = firestore.collection("Follow").document(Utils.getUiLoggedIn())

        followDocRef.get().addOnSuccessListener{documentSnapshot ->

            if(documentSnapshot.exists())
            {
                // Collection exists, update the list
                val existingIds = documentSnapshot.get("following_id") as? List<String>
                val newIds = existingIds?.toMutableList() ?: mutableListOf()
                newIds.add(user.userid!!)
                followDocRef.update(hashMapOf("following_id" to newIds) as Map<String, Any>)
            }
            else{
                // Logged user following a new user
                // Collection doesn't exist, create the list
                val followData = hashMapOf(
                    "following_id" to listOf(user.userid!!)
                )
                followDocRef.set(followData)
            }
        }
    }

    fun unfollowUser(user: Users)
    {
        val firestore = FirebaseFirestore.getInstance()
        val userDocRef = firestore.collection("Users").document(Utils.getUiLoggedIn())
        val userToUnfollowDocRef = firestore.collection("Users").document(user.userid!!)

        // Fetch the following count value using a get() call instead of addSnapshotListener()
        userDocRef.get().addOnSuccessListener { userSnapshot ->
            if (userSnapshot.exists()) {
                val followingCount = userSnapshot.getLong("following") ?: 0

                // Update the "followers" and "following" fields using a transaction
                firestore.runTransaction { transaction ->
                    val userToUnfollowSnapshot = transaction.get(userToUnfollowDocRef)

                    // Decrement the "followers" count of the user to unfollow
                    val followersCount = userToUnfollowSnapshot.getLong("followers") ?: 0
                    val newFollowersCount = if (followersCount > 0) followersCount - 1 else 0
                    transaction.update(userToUnfollowDocRef, "followers", newFollowersCount)

                    // Decrement the "following" count of the logged-in user
                    val newFollowingCount = if (followingCount > 0) followingCount - 1 else 0
                    transaction.update(userDocRef, "following", newFollowingCount)
                }.addOnSuccessListener {
                    // Transaction completed successfully

                    // Delete the documents in the "Follow" collection
                    firestore.collection("Follow").document(Utils.getUiLoggedIn()).get().addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            val followingIds = documentSnapshot.get("following_id") as? List<String>
                            val newIds = followingIds?.toMutableList() ?: mutableListOf()

                            if (newIds.contains(user.userid)) {
                                newIds.remove(user.userid)
                            }

                            if (newIds.isEmpty()) {
                                // Delete the document if the list is empty
                                documentSnapshot.reference.delete()
                            } else {
                                // Update the Firestore document with the modified list
                                val followDocRef = firestore.collection("Follow").document(Utils.getUiLoggedIn())
                                followDocRef.set(hashMapOf("following_id" to newIds))
                            }
                        }
                    }


                }.addOnFailureListener { exception ->
                    // Transaction failed
                }
            }
        }

    }

    private fun checkFollowingStatus(uid: String, followButton: Button) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Reference to the Follow document in Firestore
        val followRef = FirebaseFirestore.getInstance()
            .collection("Follow")
            .document(currentUserId)

        // Add a Firestore listener
        followRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Firestore Error", error.message.toString())
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                // Get the list of following IDs
                val followingIds = snapshot.get("following_id") as? List<String>

                // Check if the user ID is in the list
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


    // Encapsulating ViewHolder
    class ViewHolder(@NonNull itemView: AndroidViewView):RecyclerView.ViewHolder(itemView)
    {
        // user_item_follow exits
        var followButton:Button=itemView.findViewById(R.id.user_item_follow)
        // user_item_search_username exits
        var userNameTextView:TextView=itemView.findViewById(R.id.user_item_search_username)
        // user_item exits
        var useritem:LinearLayout=itemView.findViewById(R.id.user_item)
        // user_item_search_fullname exits
        var userFullnameTextView:TextView=itemView.findViewById(R.id.user_item_search_fullname)
        // user_item_image exits
        var userProfileImage:CircleImageView=itemView.findViewById(R.id.user_item_image)


    }
}

