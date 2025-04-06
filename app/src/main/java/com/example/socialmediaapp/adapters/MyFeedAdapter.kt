@file:Suppress("DEPRECATION")

package com.example.socialmediaapp.adapters

import android.annotation.SuppressLint
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

class MyFeedAdapter: RecyclerView.Adapter<FeedHolder>() {
    var feedlist = listOf<Feed>()
    private  var listener : onDoubleTapClickListener ?=  null
    private var userClickListener: onUserClickListener? = null
    private var commentClickListener: onCommentClickListener? = null

    fun setCommentClickListener(listener: onCommentClickListener) {
        this.commentClickListener = listener
    }



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

        // Set username in Row-1
        holder.userNamePoster.text = feed.username

        // Set Caption in Row-2
        holder.userNameCaption.text = feed.caption

        // Set time in Row-3
        val timeMillis = if (feed.time != null) {
            // Check if timestamp needs conversion or is already in milliseconds
            if (feed.time < 1000000000000L) {
                // If timestamp is in seconds, convert to milliseconds
                feed.time * 1000
            } else {
                // If timestamp is already in milliseconds, use as is
                feed.time
            }
        } else {
            System.currentTimeMillis() // Fallback
        }

        val date = Date(timeMillis)
        val instagramTimeFormat = DateUtils.getRelativeTimeSpanString(
            date.time,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        )
        holder.time.text = instagramTimeFormat

        // Set Image and Profile Picture
        Glide.with(holder.itemView.context).load(feed.image).into(holder.feedImage)
        Glide.with(holder.itemView.context).load(feed.imageposter).into(holder.userPosterImage)

        // Set Likes
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

        holder.commentButton.setOnClickListener {
            val comment = holder.commentInput.text.toString()
            if (comment.isNotEmpty()) {
                commentClickListener?.addComment(feed.postid!!, comment)
                holder.commentInput.text.clear()
            } else {
                Toast.makeText(holder.itemView.context, "Yorum boş olamaz!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int {
        return feedlist.size
    }

    fun setFeedList(list: List<Feed>){
        this.feedlist = list
    }

    fun setListener(listener: onDoubleTapClickListener){
        this.listener = listener
    }

    fun setUserClickListener(listener: onUserClickListener) {
        this.userClickListener = listener
    }



}



class FeedHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val userNamePoster: TextView = itemView.findViewById(R.id.feedtopusername)
    val userNameCaption: TextView = itemView.findViewById(R.id.feedusernamecaption) // Caption text
    val userPosterImage: CircleImageView = itemView.findViewById(R.id.userimage)
    val feedImage: ImageView = itemView.findViewById(R.id.feedImage)
    val time: TextView = itemView.findViewById(R.id.feedtime)
    val likecount: TextView = itemView.findViewById(R.id.likecount)
    val commentButton: Button = itemView.findViewById(R.id.commentButton)
    val commentInput: EditText = itemView.findViewById(R.id.commentInput)
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