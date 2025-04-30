@file:Suppress("DEPRECATION")

package com.example.socialmediaapp.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmediaapp.R
import de.hdodenhof.circleimageview.CircleImageView
import android.view.LayoutInflater
import com.bumptech.glide.Glide
import com.example.socialmediaapp.modal.Feed
import java.util.Date
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore

class MyFeedAdapter: RecyclerView.Adapter<FeedHolder>() {
    var feedlist = listOf<Feed>()
    private var likeListener: onLikeClickListener? = null
    private var userClickListener: onUserClickListener? = null
    private var commentClickListener: onCommentClickListener? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FeedHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.feeditem, parent, false)
        return FeedHolder(view)
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onBindViewHolder(holder: FeedHolder, position: Int) {
        val feed = feedlist[position]

        val currentUserId = com.example.socialmediaapp.Utils.getUiLoggedIn()

        // Set the appropriate like icon based on whether the user has liked this post
        updateLikeIcon(holder, feed, currentUserId)

        holder.userNamePoster.text = feed.username
        holder.userNameCaption.text = feed.caption

        val timeMillis = if (feed.time != null) {
            if (feed.time < 1000000000000L) feed.time * 1000 else feed.time
        } else {
            System.currentTimeMillis()
        }

        val date = Date(timeMillis)
        val instagramTimeFormat = DateUtils.getRelativeTimeSpanString(
            date.time,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        )
        holder.time.text = instagramTimeFormat

        Glide.with(holder.itemView.context).load(feed.image).into(holder.feedImage)
        Glide.with(holder.itemView.context).load(feed.imageposter).into(holder.userPosterImage)

        holder.likecount.text = "${feed.likes} Likes"

        // Set up like icon click listener
        holder.likeIcon.setOnClickListener {
            likeListener?.onLikeClick(feed)
        }

        holder.userNamePoster.setOnClickListener {
            userClickListener?.onUserClick(feed.userid!!)
        }

        holder.userPosterImage.setOnClickListener {
            userClickListener?.onUserClick(feed.userid!!)
        }

        holder.commentIcon.setOnClickListener {
            showCommentBottomSheet(holder.itemView.context, feed)
        }

        // Add story icon click listener
        holder.itemView.findViewById<ImageView>(R.id.storyIcon).setOnClickListener {
            // Get the story information from Firebase
            showStoryBottomSheet(holder.itemView.context, feed)
        }
    }

    // Helper method to update the like icon
    private fun updateLikeIcon(holder: FeedHolder, feed: Feed, currentUserId: String) {
        if (feed.likers?.contains(currentUserId) == true) {
            holder.likeIcon.setImageResource(R.drawable.ic_like_filled)
        } else {
            holder.likeIcon.setImageResource(R.drawable.ic_like)
        }
    }

    private fun showStoryBottomSheet(context: Context, feed: Feed) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.layout_story_bottom_sheet, null)
        bottomSheetDialog.setContentView(view)

        val storyTitle = view.findViewById<TextView>(R.id.storyTitleText)
        val storyContent = view.findViewById<TextView>(R.id.storyContentText)
        val storyImage = view.findViewById<ImageView>(R.id.storyImageView)
        val closeButton = view.findViewById<Button>(R.id.closeStoryButton)

        // Load image
        Glide.with(context)
            .load(feed.image)
            .centerCrop()
            .into(storyImage)

        // Get the story content from Firebase Images collection
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("Images")
            .whereEqualTo("postid", feed.postid)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val imageDoc = documents.documents[0]
                    val story = imageDoc.getString("story") ?: "No story available for this artwork."
                    val title = imageDoc.getString("title") ?: feed.caption ?: "Untitled"

                    storyTitle.text = title
                    storyContent.text = story
                } else {
                    storyTitle.text = feed.caption ?: "Untitled"
                    storyContent.text = "No story available for this artwork."
                }
            }
            .addOnFailureListener { e ->
                storyTitle.text = feed.caption ?: "Untitled"
                storyContent.text = "Failed to load story: ${e.message}"
            }

        closeButton.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun showCommentBottomSheet(context: Context, feed: Feed) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.layout_comment_bottom_sheet, null)
        bottomSheetDialog.setContentView(view)

        val commentInput = view.findViewById<EditText>(R.id.bottomSheetCommentInput)
        val commentButton = view.findViewById<Button>(R.id.bottomSheetCommentButton)
        val commentsRecyclerView = view.findViewById<RecyclerView>(R.id.commentsRecyclerView)

        // Set up RecyclerView for comments
        commentsRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        val commentAdapter = CommentAdapter()
        commentsRecyclerView.adapter = commentAdapter

        // Get ViewModel instance to fetch comments
        val viewModel = androidx.lifecycle.ViewModelProvider((context as androidx.fragment.app.FragmentActivity))
            .get(com.example.socialmediaapp.mvvm.ViewModel::class.java)

        // Load comments for this post
        viewModel.getPostComments(feed.postid!!).observe(context as androidx.lifecycle.LifecycleOwner) { comments ->
            commentAdapter.setComments(comments)

            // If there are no comments, show a message
            val noCommentsView = view.findViewById<TextView>(R.id.noCommentsText)
            if (comments.isEmpty()) {
                noCommentsView?.visibility = View.VISIBLE
                commentsRecyclerView.visibility = View.GONE
            } else {
                noCommentsView?.visibility = View.GONE
                commentsRecyclerView.visibility = View.VISIBLE
            }
        }

        commentButton.setOnClickListener {
            val comment = commentInput.text.toString()
            if (comment.isNotEmpty()) {
                commentClickListener?.addComment(feed.postid!!, comment)
                commentInput.text.clear()
                // Note: We don't dismiss the dialog so user can see their comment added
            } else {
                Toast.makeText(context, "Comment cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }

        bottomSheetDialog.show()
    }

    override fun getItemCount(): Int {
        return feedlist.size
    }

    fun setFeedList(list: List<Feed>) {
        // Use DiffUtil to efficiently update only changed items
        val diffCallback = FeedDiffCallback(this.feedlist, list)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.feedlist = list

        // Use diffResult to notify adapter of specific changes
        diffResult.dispatchUpdatesTo(this)
    }

    fun setLikeListener(listener: onLikeClickListener) {
        this.likeListener = listener
    }

    fun setUserClickListener(listener: onUserClickListener) {
        this.userClickListener = listener
    }

    fun setCommentClickListener(listener: onCommentClickListener) {
        this.commentClickListener = listener
    }
}

// Add a DiffUtil.Callback implementation to efficiently update the RecyclerView
class FeedDiffCallback(
    private val oldList: List<Feed>,
    private val newList: List<Feed>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].postid == newList[newItemPosition].postid
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        // Compare relevant fields that might change
        return oldItem.likes == newItem.likes &&
                oldItem.likers?.size == newItem.likers?.size &&
                oldItem.caption == newItem.caption
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        // If needed, you can add specific change payloads here
        return super.getChangePayload(oldItemPosition, newItemPosition)
    }
}

// FeedHolder güncellemesi:
class FeedHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val userNamePoster: TextView = itemView.findViewById(R.id.feedtopusername)
    val userNameCaption: TextView = itemView.findViewById(R.id.feedusernamecaption)
    val userPosterImage: CircleImageView = itemView.findViewById(R.id.userimage)
    val feedImage: ImageView = itemView.findViewById(R.id.feedImage)
    val time: TextView = itemView.findViewById(R.id.feedtime)
    val likecount: TextView = itemView.findViewById(R.id.likecount)
    val commentIcon: ImageView = itemView.findViewById(R.id.commentIcon)
    val likeIcon: ImageView = itemView.findViewById(R.id.likeIcon)
    val cardView: androidx.cardview.widget.CardView = itemView.findViewById(R.id.cardview)
}

// Ekstra: dp to px dönüşümü
fun Int.dpToPx(context: android.content.Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

interface onLikeClickListener {
    // Create feed modal
    fun onLikeClick(feed: Feed)
}

interface onUserClickListener {
    fun onUserClick(userId: String)
}

interface onCommentClickListener {
    fun addComment(postId: String, comment: String)
}