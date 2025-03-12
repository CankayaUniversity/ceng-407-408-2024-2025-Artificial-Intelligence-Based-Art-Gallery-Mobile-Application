@file:Suppress("DEPRECATION")

package com.example.socialmediaapp.adapters

import android.annotation.SuppressLint
import android.text.Html
import android.text.Spanned
import android.text.format.DateUtils
import android.view.GestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.menu.ListMenuItemView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.socialmediaapp.R
import de.hdodenhof.circleimageview.CircleImageView
import android.view.LayoutInflater
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat
import com.bumptech.glide.Glide
import com.example.socialmediaapp.modal.Feed
import java.util.Date

class MyFeedAdapter: RecyclerView.Adapter<FeedHolder>() {
    var feedlist = listOf<Feed>()
    private  var listener : onDoubleTapClickListener ?=  null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FeedHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.feeditem, parent, false)
        return FeedHolder(view)

    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: FeedHolder, position: Int) {
        val feed = feedlist[position]

        // Set username in Row-1
        holder.userNamePoster.text = feed.username

        // Set Caption in Row-2
        holder.userNameCaption.text = feed.caption

        // Set time in Row-3
        val date = Date(feed.time!!.toLong() * 1000)
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


}



class FeedHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val userNamePoster: TextView = itemView.findViewById(R.id.feedtopusername)
    val userNameCaption: TextView = itemView.findViewById(R.id.feedusernamecaption) // Caption text
    val userPosterImage: CircleImageView = itemView.findViewById(R.id.userimage)
    val feedImage: ImageView = itemView.findViewById(R.id.feedImage)
    val time: TextView = itemView.findViewById(R.id.feedtime)
    val likecount: TextView = itemView.findViewById(R.id.likecount)
}


interface onDoubleTapClickListener{

    // Create feed modal
    fun onDoubleTap(feed: Feed)
}