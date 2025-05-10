package com.example.socialmediaapp.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import com.example.socialmediaapp.modal.Posts
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.socialmediaapp.R

class MyPostAdapter: RecyclerView.Adapter<PostHolder>() {

    var mypostlist = listOf<Posts>()
    private var onPostClickListener: ((Posts) -> Unit)? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PostHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.postitems, parent, false)
        return PostHolder(view)
    }

    override fun onBindViewHolder(
        holder: PostHolder,
        position: Int
    ) {
        val post = mypostlist[position]
        Glide.with(holder.itemView.context).load(post.image).into(holder.image)

        // Set click listener on the image
        holder.image.setOnClickListener {
            onPostClickListener?.invoke(post)
        }
    }

    override fun getItemCount(): Int {
        return mypostlist.size
    }

    fun setPostList(list: List<Posts>){
        val diffResult = DiffUtil.calculateDiff(MyDiffCallback(mypostlist, list))
        mypostlist = list
        diffResult.dispatchUpdatesTo(this)
    }

    // Add a method to set the click listener
    fun setOnPostClickListener(listener: (Posts) -> Unit) {
        onPostClickListener = listener
    }
}

class PostHolder(itemView: View) : ViewHolder(itemView){
    val image: ImageView = itemView.findViewById(R.id.postImage)
}

class MyDiffCallback(
    private val oldList: List<Posts>,
    private val newList: List<Posts>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}