package com.example.socialmediaapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaapp.R
import com.example.socialmediaapp.Utils
import com.example.socialmediaapp.modal.Notification
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private var notifications: List<Notification> = listOf()
    private var listener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(notification: Notification)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    fun setNotifications(notifications: List<Notification>) {
        this.notifications = notifications
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.notification_item, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.bind(notification)
    }

    override fun getItemCount(): Int = notifications.size

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userImageView: ImageView = itemView.findViewById(R.id.notification_user_image)
        private val notificationTextView: TextView = itemView.findViewById(R.id.notification_text)
        private val timeTextView: TextView = itemView.findViewById(R.id.notification_time)
        private val unreadIndicator: View = itemView.findViewById(R.id.unread_indicator)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onItemClick(notifications[position])
                }
            }
        }

        fun bind(notification: Notification) {
            // Load user image
            Glide.with(itemView.context)
                .load(notification.fromUserImage)
                .placeholder(R.drawable.person)
                .into(userImageView)

            // Set notification text based on type
            when (notification.type) {
                "like" -> {
                    val caption = if (notification.postCaption.length > 30) {
                        notification.postCaption.substring(0, 27) + "..."
                    } else notification.postCaption

                    notificationTextView.text = "${notification.fromUsername} liked your post: \"$caption\""
                }
                "comment" -> {
                    val caption = if (notification.postCaption.length > 30) {
                        notification.postCaption.substring(0, 27) + "..."
                    } else notification.postCaption

                    val comment = if (notification.text.length > 30) {
                        notification.text.substring(0, 27) + "..."
                    } else notification.text

                    notificationTextView.text = "${notification.fromUsername} commented on your post \"$caption\": \"$comment\""
                }
                "follow" -> {
                    notificationTextView.text = "${notification.fromUsername} started following you"
                }
            }

            // Set time
            timeTextView.text = Utils.getTimeAgo(notification.time)

            // Set unread indicator
            unreadIndicator.visibility = if (notification.seen) View.GONE else View.VISIBLE
        }
    }
}