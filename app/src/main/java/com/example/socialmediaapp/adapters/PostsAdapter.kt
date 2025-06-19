package com.example.socialmediaapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.socialmediaapp.R
import com.example.socialmediaapp.fragments.OnPostClickListener
import com.example.socialmediaapp.modal.Posts

class PostsAdapter : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    private var postsList = listOf<Posts>()
    private var onPostClickListener: OnPostClickListener? = null
    private var isDynamicHeights = true // Enable dynamic heights by default

    fun setPosts(posts: List<Posts>) {
        val diffResult = DiffUtil.calculateDiff(PostsDiffCallback(postsList, posts))
        postsList = posts
        diffResult.dispatchUpdatesTo(this)
    }

    fun setOnPostClickListener(listener: OnPostClickListener) {
        this.onPostClickListener = listener
    }

    // Enable or disable dynamic heights
    fun setDynamicHeights(enabled: Boolean) {
        isDynamicHeights = enabled
        notifyDataSetChanged()
    }

    // Get span size for GridLayoutManager - Exact pattern: 1 big + 4 small repeating
    fun getSpanSize(position: Int, spanCount: Int): Int {
        return when (spanCount) {
            2 -> {
                // 2-column grid: every 5 items = 1 big (pos 0) + 4 small (pos 1,2,3,4)
                when (position % 5) {
                    0 -> 2    // Position 0,5,10,15... = BIG (full width)
                    else -> 1 // Positions 1,2,3,4 then 6,7,8,9 then 11,12,13,14... = SMALL
                }
            }
            3 -> {
                // 3-column grid adaptation
                when (position % 7) {
                    0 -> 3    // Position 0,7,14... = BIG (full width)
                    else -> 1 // Positions 1-6, then 8-13, then 15-20... = SMALL
                }
            }
            else -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.modern_post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postsList[position]
        holder.bind(post, position)
    }

    override fun getItemCount(): Int = postsList.size

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val postImage: ImageView = itemView.findViewById(R.id.postImage)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Add modern click animation
                    itemView.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .withEndAction {
                            itemView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start()
                            onPostClickListener?.onPostClick(postsList[position])
                        }
                        .start()
                }
            }
        }

        fun bind(post: Posts, position: Int) {
            // Apply dynamic height based on position for modern gallery feel
            if (isDynamicHeights) {
                applyDynamicHeight(position)
            }

            // Load image with modern Glide configuration
            Glide.with(itemView.context)
                .load(post.image)
                .centerCrop()
                .placeholder(R.drawable.placeholder_image2)
                .error(R.drawable.error_image)
                .transition(DrawableTransitionOptions.withCrossFade(300))
                .into(postImage)

            // Apply modern card styling based on position
            applyModernStyling(position)

            // Add smooth entrance animation
            addEntranceAnimation(position)
        }

        private fun applyDynamicHeight(position: Int) {
            val context = itemView.context
            val density = context.resources.displayMetrics.density

            // Calculate height based on the simple pattern
            val spanSize = getSpanSize(position, 2) // Use 2-column reference
            val baseHeight = (140 * density).toInt() // Base height

            val dynamicHeight = when (spanSize) {
                2 -> (baseHeight * 1.8).toInt() // Big items - much taller
                else -> baseHeight // Small items - normal height
            }

            // Apply the calculated height
            val layoutParams = postImage.layoutParams
            layoutParams.height = dynamicHeight
            postImage.layoutParams = layoutParams

            // Also apply to card container if exists
            if (itemView is CardView) {
                val cardParams = itemView.layoutParams
                cardParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                itemView.layoutParams = cardParams
            }
        }

        private fun applyModernStyling(position: Int) {
            // Apply styling based on the simple big/small pattern
            if (itemView is CardView) {
                val cardView = itemView as CardView
                val spanSize = getSpanSize(position, 2)

                when (spanSize) {
                    2 -> {
                        // Big items - featured styling
                        cardView.cardElevation = 8f
                        cardView.radius = 16f
                    }
                    else -> {
                        // Small items - standard styling
                        cardView.cardElevation = 4f
                        cardView.radius = 12f
                    }
                }
            }
        }

        private fun addEntranceAnimation(position: Int) {
            // Add subtle scale animation on bind for smooth entrance
            itemView.alpha = 0f
            itemView.scaleX = 0.9f
            itemView.scaleY = 0.9f

            itemView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(250)
                .setStartDelay((position % 6) * 50L) // Staggered animation
                .start()
        }
    }

    // DiffUtil callback for efficient list updates
    class PostsDiffCallback(
        private val oldList: List<Posts>,
        private val newList: List<Posts>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].postid == newList[newItemPosition].postid
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}