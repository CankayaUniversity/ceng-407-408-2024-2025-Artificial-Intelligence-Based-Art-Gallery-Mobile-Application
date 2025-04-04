package com.example.socialmediaapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaapp.R
import com.example.socialmediaapp.fragments.OnPostClickListener
import com.example.socialmediaapp.modal.Posts

class PostsAdapter : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    private var postsList = listOf<Posts>()
    private var onPostClickListener: OnPostClickListener? = null

    fun setPosts(posts: List<Posts>) {
        this.postsList = posts
        notifyDataSetChanged()
    }

    fun setOnPostClickListener(listener: OnPostClickListener) {
        this.onPostClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post_grid_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postsList[position]
        holder.bind(post)
    }

    override fun getItemCount(): Int = postsList.size

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val postImage: ImageView = itemView.findViewById(R.id.post_image)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPostClickListener?.onPostClick(postsList[position])
                }
            }
        }

        fun bind(post: Posts) {
            // Load the post image using Glide
            Glide.with(itemView.context)
                .load(post.image)
                .placeholder(R.drawable.placeholder_image2)
                .into(postImage)
        }
    }
}