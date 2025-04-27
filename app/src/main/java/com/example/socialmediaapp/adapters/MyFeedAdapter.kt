
@file:Suppress("DEPRECATION")

package com.example.socialmediaapp.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import android.view.GestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmediaapp.R
import de.hdodenhof.circleimageview.CircleImageView
import android.view.LayoutInflater
import android.view.MotionEvent
import com.bumptech.glide.Glide
import com.example.socialmediaapp.modal.Feed
import java.util.Date
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.GestureDetectorCompat
import com.google.android.material.bottomsheet.BottomSheetDialog

class MyFeedAdapter: RecyclerView.Adapter<FeedHolder>() {
    var feedlist = listOf<Feed>()
    private var listener: onDoubleTapClickListener? = null
    private var userClickListener: onUserClickListener? = null
    private var commentClickListener: onCommentClickListener? = null

    @SuppressLint("ClickableViewAccessibility")
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

        val doubleClickGestureDetector = GestureDetector(holder.itemView.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                listener?.onDoubleTap(feed)
                return true
            }
        })

        holder.itemView.setOnTouchListener { _, event ->
            doubleClickGestureDetector.onTouchEvent(event)
            true
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
        this.feedlist = list
    }

    fun setListener(listener: onDoubleTapClickListener) {
        this.listener = listener
    }

    fun setUserClickListener(listener: onUserClickListener) {
        this.userClickListener = listener
    }

    fun setCommentClickListener(listener: onCommentClickListener) {
        this.commentClickListener = listener
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
    val cardView: androidx.cardview.widget.CardView = itemView.findViewById(R.id.cardview)
}

// Ekstra: dp to px dönüşümü
fun Int.dpToPx(context: android.content.Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}


interface onDoubleTapClickListener{

    // Create feed modal
    fun onDoubleTap(feed: Feed)
}


interface onUserClickListener {
    fun onUserClick(userId: String)
}

interface onCommentClickListener {
    fun addComment(postId: String, comment: String)
}