package com.example.socialmediaapp.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmediaapp.R
import com.google.firebase.Timestamp
import java.util.Date

class CommentAdapter : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {
    private var commentsList = listOf<Map<String, Any>>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentsList[position]

        holder.usernameTextView.text = comment["username"] as? String ?: "Unknown"
        holder.commentTextView.text = comment["comment"] as? String ?: ""

        // Format the timestamp
        val timestamp = comment["time"] as? Timestamp
        if (timestamp != null) {
            val date = Date(timestamp.seconds * 1000)
            val timeFormatted = DateUtils.getRelativeTimeSpanString(
                date.time,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            )
            holder.timeTextView.text = timeFormatted
        } else {
            holder.timeTextView.text = ""
        }
    }

    override fun getItemCount(): Int = commentsList.size

    fun setComments(comments: List<Map<String, Any>>) {
        this.commentsList = comments
        notifyDataSetChanged()
    }

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameTextView: TextView = itemView.findViewById(R.id.commentUsername)
        val commentTextView: TextView = itemView.findViewById(R.id.commentText)
        val timeTextView: TextView = itemView.findViewById(R.id.commentTime)
    }
}

