package com.example.socialmediaapp.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaapp.modal.Users
import de.hdodenhof.circleimageview.CircleImageView
import android.view.View as AndroidViewView
import com.example.socialmediaapp.R
import com.example.socialmediaapp.mvvm.ViewModel

class SearchUsersAdapter(
    private var mContext: Context,
    private var mUser: List<Users>,
    private var isFragment: Boolean = false,
    private val viewModel: ViewModel, // Added ViewModel parameter
    private val onUserClicked: (String) -> Unit // Callback for user clicks
) : RecyclerView.Adapter<SearchUsersAdapter.ViewHolder>() {

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

        // Check following status using ViewModel
        user.userid?.let { userId ->
            viewModel.checkIfFollowing(userId).observeForever { isFollowing ->
                holder.followButton.text = if (isFollowing) "Following" else "Follow"
            }
        }

        // Set click listener on the entire item view
        holder.itemView.setOnClickListener {
            user.userid?.let { userId ->
                Log.d("SearchUsersAdapter", "User clicked: $userId")
                onUserClicked(userId)
            }
        }

        // Also set click listener on the user item layout specifically
        holder.useritem.setOnClickListener {
            user.userid?.let { userId ->
                Log.d("SearchUsersAdapter", "User item clicked: $userId")
                onUserClicked(userId)
            }
        }

        // Follow/Unfollow functionality using ViewModel
        holder.followButton.setOnClickListener {
            user.userid?.let { userId ->
                if (holder.followButton.text.toString() == "Follow") {
                    // Use ViewModel to follow user
                    viewModel.followUser(userId).observeForever { success ->
                        if (success) {
                            holder.followButton.text = "Following"
                        }
                    }
                } else {
                    // Use ViewModel to unfollow user
                    viewModel.unfollowUser(userId).observeForever { success ->
                        if (success) {
                            holder.followButton.text = "Follow"
                        }
                    }
                }
            }
        }
    }

    // ViewHolder class remains unchanged
    class ViewHolder(@NonNull itemView: AndroidViewView) : RecyclerView.ViewHolder(itemView) {
        var followButton: Button = itemView.findViewById(R.id.user_item_follow)
        var userNameTextView: TextView = itemView.findViewById(R.id.user_item_search_username)
        var useritem: LinearLayout = itemView.findViewById(R.id.user_item)
        var userFullnameTextView: TextView = itemView.findViewById(R.id.user_item_search_fullname)
        var userProfileImage: CircleImageView = itemView.findViewById(R.id.user_item_image)
    }
}