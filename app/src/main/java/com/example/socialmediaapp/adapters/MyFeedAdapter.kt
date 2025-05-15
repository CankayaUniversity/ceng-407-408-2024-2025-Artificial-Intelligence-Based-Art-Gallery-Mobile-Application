@file:Suppress("DEPRECATION")

package com.example.socialmediaapp.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore

class MyFeedAdapter: RecyclerView.Adapter<FeedHolder>() {
    var feedlist = listOf<Feed>()
    private var likeListener: onLikeClickListener? = null
    private var userClickListener: onUserClickListener? = null
    private var commentClickListener: onCommentClickListener? = null
    private var commentTextWatcher: TextWatcher? = null

    // Set maximum comment length to 2000 characters
    private val MAX_COMMENT_LENGTH = 2000

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

        // BottomSheet'in tam yükseklikte açılmasını sağlayalım
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        val behavior = BottomSheetBehavior.from(bottomSheet!!)

        // Ekranın %80'ini kaplasın - bu sayede butonlar her zaman görünür olacak
        val displayMetrics = context.resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val peekHeight = (screenHeight * 0.8).toInt()
        behavior.peekHeight = peekHeight

        val commentInput = view.findViewById<EditText>(R.id.bottomSheetCommentInput)
        val commentButton = view.findViewById<Button>(R.id.bottomSheetCommentButton)
        val commentsRecyclerView = view.findViewById<RecyclerView>(R.id.commentsRecyclerView)
        val characterCounterView = view.findViewById<TextView>(R.id.characterCounter)

        // EditText için tag ekle - bu sayede HomeFragment'taki TextWatcher'da erişebiliriz
        commentInput.tag = "commentEditText"

        // EditText ayarları
        commentInput.isVerticalScrollBarEnabled = true
        commentInput.movementMethod = android.text.method.ScrollingMovementMethod.getInstance()

        // EditText için maksimum yükseklik ayarlama - buton her zaman görünür olacak şekilde
        val maxHeight = (100 * context.resources.displayMetrics.density).toInt() // 100dp
        commentInput.maxHeight = maxHeight

        // Karakter sayacı
        characterCounterView.text = "0/$MAX_COMMENT_LENGTH"

        // InputFilter ile karakter sayısını sınırla
        val maxLengthFilter = android.text.InputFilter.LengthFilter(MAX_COMMENT_LENGTH)
        commentInput.filters = arrayOf(maxLengthFilter)

        // RecyclerView ayarları
        commentsRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        val commentAdapter = CommentAdapter()
        commentsRecyclerView.adapter = commentAdapter

        // ViewModel ile yorumları al
        val viewModel = androidx.lifecycle.ViewModelProvider((context as androidx.fragment.app.FragmentActivity))
            .get(com.example.socialmediaapp.mvvm.ViewModel::class.java)

        // Yorumları yükle
        viewModel.getPostComments(feed.postid!!).observe(context as androidx.lifecycle.LifecycleOwner) { comments ->
            commentAdapter.setComments(comments)

            val noCommentsView = view.findViewById<TextView>(R.id.noCommentsText)
            if (comments.isEmpty()) {
                noCommentsView?.visibility = View.VISIBLE
                commentsRecyclerView.visibility = View.GONE
            } else {
                noCommentsView?.visibility = View.GONE
                commentsRecyclerView.visibility = View.VISIBLE
            }

            // Yorumlar yüklendiğinde BottomSheet'in boyutunu güncelle
            bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let {
                BottomSheetBehavior.from(it).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }

        // EditText'e onEditorAction listener ekleyerek enter tuşunu yakalama
        commentInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER)) {

                // Eğer karakter limiti aşılmışsa, metni kırp
                if (commentInput.text.length > MAX_COMMENT_LENGTH) {
                    commentInput.setText(commentInput.text.toString().substring(0, MAX_COMMENT_LENGTH))
                    commentInput.setSelection(MAX_COMMENT_LENGTH)
                    Toast.makeText(context, "Maksimum $MAX_COMMENT_LENGTH karakter girebilirsiniz.", Toast.LENGTH_SHORT).show()
                }

                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        // TextWatcher for comment input with character counting
        commentInput.addTextChangedListener(object : TextWatcher {
            private var previousText = ""
            private var isLimitReached = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                previousText = s?.toString() ?: ""
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentLength = s?.length ?: 0

                // Update character counter
                characterCounterView.apply {
                    text = "$currentLength/$MAX_COMMENT_LENGTH"

                    // Change text color based on remaining characters
                    when {
                        currentLength >= MAX_COMMENT_LENGTH -> {
                            setTextColor(android.graphics.Color.RED)
                            if (!isLimitReached) {
                                isLimitReached = true
                                Toast.makeText(context, "Maksimum karakter limitine ulaştınız!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        currentLength > MAX_COMMENT_LENGTH * 0.9 -> {
                            setTextColor(android.graphics.Color.RED)
                            isLimitReached = false
                        }
                        currentLength > MAX_COMMENT_LENGTH * 0.75 -> {
                            setTextColor(android.graphics.Color.parseColor("#FF9800")) // Orange
                            isLimitReached = false
                        }
                        else -> {
                            setTextColor(android.graphics.Color.parseColor("#9E9E9E")) // Gray
                            isLimitReached = false
                        }
                    }
                }

                // Show warnings when approaching the limit
                if (currentLength > MAX_COMMENT_LENGTH * 0.9 && currentLength < MAX_COMMENT_LENGTH) {
                    val remainingChars = MAX_COMMENT_LENGTH - currentLength
                    if (remainingChars == 200 || remainingChars == 100 || remainingChars == 50 || remainingChars == 20 || remainingChars == 10) {
                        Toast.makeText(context, "$remainingChars karakter kaldı", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (s == null) return

                // If text exceeds 2000 characters, revert to previous state or trim
                if (s.length > MAX_COMMENT_LENGTH) {
                    // Safely trim the text
                    val limitedText = if (previousText.length <= MAX_COMMENT_LENGTH) {
                        previousText
                    } else {
                        previousText.substring(0, MAX_COMMENT_LENGTH)
                    }

                    // Replace the current text with the limited version
                    s.replace(0, s.length, limitedText)

                    // Move cursor to the end
                    commentInput.setSelection(s.length)

                    // Show warning (but not too frequently)
                    if (!isLimitReached) {
                        Toast.makeText(context, "Maksimum karakter limitine ulaştınız!", Toast.LENGTH_SHORT).show()
                        isLimitReached = true
                    }
                }

                // Call external TextWatcher if available
                commentTextWatcher?.afterTextChanged(s)
            }
        })

        // Comment button click listener
        commentButton.setOnClickListener {
            val comment = commentInput.text.toString().trim()

            if (comment.isEmpty()) {
                Toast.makeText(context, "Yorum boş olamaz!", Toast.LENGTH_SHORT).show()
            } else if (comment.length > MAX_COMMENT_LENGTH) {
                Toast.makeText(context, "Yorum maksimum $MAX_COMMENT_LENGTH karakter olabilir!", Toast.LENGTH_SHORT).show()
            } else {
                commentClickListener?.addComment(feed.postid!!, comment)
                commentInput.text.clear()
                characterCounterView.text = "0/$MAX_COMMENT_LENGTH"
                characterCounterView.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
                Toast.makeText(context, "Yorum eklendi!", Toast.LENGTH_SHORT).show()
            }
        }

        // Ensure BottomSheet is fully open and scrollable
        bottomSheetDialog.setOnShowListener {
            val parentLayout = view.parent as View
            parentLayout.post {
                // Make BottomSheet fully expanded
                val bottomSheetParent = view.parent as View
                val behavior = BottomSheetBehavior.from(bottomSheetParent)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED

                // Limit BottomSheet height (80% of screen)
                val displayMetrics = context.resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels
                bottomSheetParent.layoutParams.height = (screenHeight * 0.8).toInt()
                bottomSheetParent.requestLayout()
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

    // Method to set text watcher for comments
    fun setCommentTextWatcher(watcher: TextWatcher) {
        this.commentTextWatcher = watcher
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

// FeedHolder class
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

// Utility function for dp to px conversion
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