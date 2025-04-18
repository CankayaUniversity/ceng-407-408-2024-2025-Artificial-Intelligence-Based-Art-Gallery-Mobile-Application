package com.example.socialmediaapp.activities

import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.socialmediaapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

class ProfilePageActivity : BaseActivity() {
    override fun getContentLayoutId(): Int {
        return R.layout.activity_profile_page
    }

    private lateinit var profileImageView: ImageView
    private lateinit var usernameTextView: TextView
    private lateinit var likesCountTextView: TextView
    private lateinit var followsCountTextView: TextView
    private lateinit var followersCountTextView: TextView
    private lateinit var artworksGridLayout: GridLayout
    private lateinit var editProfileFab: FloatingActionButton
    private lateinit var artworksTitleTextView: TextView

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    private var selectedImageUri: Uri? = null
    private var totalLikes = 0

    // Pagination variables
    private val ARTWORKS_PER_PAGE = 12
    private var lastDocumentSnapshot: DocumentSnapshot? = null
    private var isLoadingMoreArtworks = false
    private var hasMoreArtworks = true

    // Activity result launcher for image selection
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                showProfileEditDialog(selectedImageUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToolbarTitle("My Profile")

        initializeViews()
        loadUserProfile()
        loadUserArtworks(true)

        editProfileFab.setOnClickListener {
            showImagePickerOptions()
        }
    }

    private fun initializeViews() {
        profileImageView = findViewById(R.id.profileImageView)
        usernameTextView = findViewById(R.id.usernameTextView)
        likesCountTextView = findViewById(R.id.likesCountTextView)
        followsCountTextView = findViewById(R.id.followsCountTextView)
        followersCountTextView = findViewById(R.id.followersCountTextView)
        artworksGridLayout = findViewById(R.id.artworksGridLayout)
        editProfileFab = findViewById(R.id.editProfileFab)
        artworksTitleTextView = findViewById(R.id.artworksTitleTextView)
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser ?: return

        // Load user data from Firestore
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userDoc = firestore.collection("Users")
                    .document(currentUser.uid)
                    .get()
                    .await()

                val username = userDoc.getString("username") ?: currentUser.displayName ?: "User"
                val photoUrl = userDoc.getString("image") ?: currentUser.photoUrl?.toString()
                val followers = userDoc.getLong("followers")?.toInt() ?: 0
                val follows = userDoc.getLong("following")?.toInt() ?: 0  // Changed from "follows" to "following" to match database

                // Calculate total likes from all artworks
                val artworksQuery = firestore.collection("Posts")
                    .whereEqualTo("userid", currentUser.uid)
                    .get()
                    .await()

                totalLikes = 0
                for (doc in artworksQuery.documents) {
                    totalLikes += doc.getLong("likes")?.toInt() ?: 0
                }

                withContext(Dispatchers.Main) {
                    // Update UI with user data
                    usernameTextView.text = username
                    likesCountTextView.text = totalLikes.toString()
                    followsCountTextView.text = follows.toString()
                    followersCountTextView.text = followers.toString()

                    // Load profile image
                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this@ProfilePageActivity)
                            .load(photoUrl)
                            .circleCrop()
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .into(profileImageView)
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfilePage", "Error loading profile", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfilePageActivity, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadUserArtworks(isInitialLoad: Boolean = false) {
        val currentUser = auth.currentUser ?: return

        if (isLoadingMoreArtworks) return
        isLoadingMoreArtworks = true

        if (isInitialLoad) {
            artworksGridLayout.removeAllViews()
            lastDocumentSnapshot = null
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Debug log
                Log.d("ProfilePage", "Querying for artworks with userId: ${currentUser.uid}")

                // Updated: Query Posts collection where userid matches current user
                var query = firestore.collection("Posts")
                    .whereEqualTo("userid", currentUser.uid)
                    .orderBy("time", Query.Direction.DESCENDING)  // Changed from "createdAt" to "time" to match StoryGenerationPageActivity
                    .limit(ARTWORKS_PER_PAGE.toLong())

                lastDocumentSnapshot?.let {
                    query = query.startAfter(it)
                }

                val artworksQuery = query.get().await()

                // Logging to debug
                Log.d("ProfilePage", "Loaded ${artworksQuery.documents.size} artworks for user ${currentUser.uid}")
                for (doc in artworksQuery.documents) {
                    Log.d("ProfilePage", "Artwork found: ${doc.id} - ${doc.getString("caption")}")
                }

                // Check if there are more artworks to load
                hasMoreArtworks = artworksQuery.documents.size == ARTWORKS_PER_PAGE

                if (artworksQuery.documents.isNotEmpty()) {
                    lastDocumentSnapshot = artworksQuery.documents.last()
                }

                withContext(Dispatchers.Main) {
                    // Update the title of artworks section with total count
                    val totalArtworksCount = if (isInitialLoad) {
                        artworksQuery.documents.size
                    } else {
                        artworksGridLayout.childCount + artworksQuery.documents.size
                    }

                    artworksTitleTextView.text = "My Artworks - $totalArtworksCount Posts"

                    // Create and add artwork cards to the grid
                    for (doc in artworksQuery.documents) {
                        // Get the image URL from the "image" field in Posts collection
                        val imageUrl = doc.getString("image")
                        if (imageUrl.isNullOrEmpty()) {
                            Log.e("ProfilePage", "Missing image URL for document ${doc.id}")
                            continue
                        }

                        val caption = doc.getString("caption") ?: "Untitled"
                        val likes = doc.getLong("likes")?.toInt() ?: 0
                        val comments = doc.getLong("comments")?.toInt() ?: 0
                        val postId = doc.id

                        // Fetch additional artwork details from Images collection using postId
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val imagesQuery = firestore.collection("Images")
                                    .whereEqualTo("postid", postId)
                                    .limit(1)
                                    .get()
                                    .await()

                                var story = ""
                                var title = caption // Default to caption if title not found

                                if (!imagesQuery.isEmpty) {
                                    val imageDoc = imagesQuery.documents[0]
                                    story = imageDoc.getString("story") ?: ""
                                    title = imageDoc.getString("title") ?: caption
                                }

                                withContext(Dispatchers.Main) {
                                    // Create artwork card view with the additional information
                                    val cardView = createArtworkCardView(imageUrl, title, story, likes, comments, postId)
                                    artworksGridLayout.addView(cardView)
                                }
                            } catch (e: Exception) {
                                Log.e("ProfilePage", "Error fetching artwork details for ${postId}", e)
                            }
                        }
                    }

                    // Show a message if no artworks are found
                    if (artworksGridLayout.childCount == 0 && artworksQuery.documents.isEmpty()) {
                        artworksTitleTextView.text = "No Artworks Yet"
                    }

                    isLoadingMoreArtworks = false
                }
            } catch (e: Exception) {
                Log.e("ProfilePage", "Error loading artworks", e)
                withContext(Dispatchers.Main) {
                    isLoadingMoreArtworks = false
                    Toast.makeText(this@ProfilePageActivity, "Failed to load artworks: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createArtworkCardView(
        imageUrl: String,
        title: String,
        story: String,
        likes: Int,
        comments: Int,
        docId: String
    ): CardView {
        // Inflate artwork card layout
        val cardView = layoutInflater.inflate(
            R.layout.item_artwork_card,
            artworksGridLayout,
            false
        ) as CardView

        // Set margin and layout parameters
        val params = GridLayout.LayoutParams()
        params.width = 0
        params.height = GridLayout.LayoutParams.WRAP_CONTENT
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.setMargins(8, 8, 8, 8)
        cardView.layoutParams = params

        // Set up views in the card
        val artworkImageView = cardView.findViewById<ImageView>(R.id.artworkImageView)
        val likesTextView = cardView.findViewById<TextView>(R.id.likesTextView)
        val commentsTextView = cardView.findViewById<TextView>(R.id.commentsTextView)

        // Load image with Glide
        Glide.with(this)
            .load(imageUrl)
            .centerCrop()
            .placeholder(R.drawable.placeholder_image2)
            .error(R.drawable.error_image)
            .into(artworkImageView)

        // Set text views
        likesTextView.text = likes.toString()
        commentsTextView.text = comments.toString()

        // Set click listener to show artwork details
        cardView.setOnClickListener {
            showArtworkDetailsDialog(imageUrl, title, story, likes, comments, docId)
        }

        return cardView
    }

    private fun showArtworkDetailsDialog(
        imageUrl: String,
        title: String,
        story: String,
        likes: Int,
        comments: Int,
        docId: String
    ) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_artwork_details)
        dialog.window?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.semi_transparent)))

        // Initialize dialog views
        val detailImageView = dialog.findViewById<ImageView>(R.id.detailImageView)
        val detailTitleTextView = dialog.findViewById<TextView>(R.id.detailTitleTextView)
        val detailStoryTextView = dialog.findViewById<TextView>(R.id.detailStoryTextView)
        val detailLikesTextView = dialog.findViewById<TextView>(R.id.detailLikesTextView)
        val detailCommentsTextView = dialog.findViewById<TextView>(R.id.detailCommentsTextView)
        val closeButton = dialog.findViewById<Button>(R.id.closeButton)

        // Load image and set text views
        Glide.with(this)
            .load(imageUrl)
            .fitCenter()
            .placeholder(R.drawable.placeholder_image2)
            .error(R.drawable.error_image)
            .into(detailImageView)

        detailTitleTextView.text = title
        detailStoryTextView.text = story
        detailLikesTextView.text = "$likes likes"
        detailCommentsTextView.text = "$comments comments"

        // Set close button listener
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showProfilePictureEditDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_edit_profile_picture)

        val galleryButton = dialog.findViewById<Button>(R.id.chooseFromGalleryButton)
        val photoButton = dialog.findViewById<Button>(R.id.takePhotoButton)
        val backButton = dialog.findViewById<Button>(R.id.backToEditButton)

        galleryButton.setOnClickListener {
            // Open gallery
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        photoButton.setOnClickListener {
            // Launch camera intent
            // Note: You'd need to implement camera functionality
            Toast.makeText(this, "Camera functionality coming soon", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        backButton.setOnClickListener {
            // Return to previous menu
            showImagePickerOptions()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showImagePickerOptions() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_image_picker)

        // Initialize dialog views
        val galleryButton = dialog.findViewById<Button>(R.id.galleryButton)
        val editNameButton = dialog.findViewById<Button>(R.id.editNameButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)

        galleryButton.setOnClickListener {
            // Open change profile picture dialog
            showProfilePictureEditDialog()
            dialog.dismiss()
        }

        editNameButton.setOnClickListener {
            // Show dialog to edit username
            showUsernameEditDialog()
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            // Close the menu
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showProfileEditDialog(imageUri: Uri?) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_edit_profile)

        // Initialize dialog views
        val previewImageView = dialog.findViewById<ImageView>(R.id.previewImageView)
        val saveButton = dialog.findViewById<Button>(R.id.saveButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)

        // Load selected image into preview
        if (imageUri != null) {
            Glide.with(this)
                .load(imageUri)
                .circleCrop()
                .into(previewImageView)
        }

        // Set button listeners
        saveButton.setOnClickListener {
            if (imageUri != null) {
                uploadProfileImage(imageUri)
            }
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            // Close current menu
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showUsernameEditDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_edit_username)

        // Initialize dialog views
        val usernameEditText = dialog.findViewById<EditText>(R.id.usernameEditText)
        val saveButton = dialog.findViewById<Button>(R.id.saveButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)

        // Set current username
        usernameEditText.setText(usernameTextView.text)

        // Set button listeners
        saveButton.setOnClickListener {
            val newUsername = usernameEditText.text.toString().trim()
            if (newUsername.isNotEmpty()) {
                updateUsername(newUsername)
            }
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun uploadProfileImage(imageUri: Uri) {
        val currentUser = auth.currentUser ?: return
        val progressDialog = ProgressDialog(this).apply {
            setCancelable(false)
            setMessage("Updating profile image...")
            show()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Get bitmap from uri
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
                val imageData = baos.toByteArray()

                // Create a unique file name
                val imageName = "ProfileImages/${currentUser.uid}/${UUID.randomUUID()}.jpg"
                val storageRef = storage.reference.child(imageName)

                // Upload the image
                storageRef.putBytes(imageData).await()
                val photoUrl = storageRef.downloadUrl.await().toString()

                // Update Firestore user document
                val updates = hashMapOf<String, Any>(
                    "image" to photoUrl
                )

                firestore.collection("Users")
                    .document(currentUser.uid)
                    .update(updates)
                    .await()

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    // Update UI
                    Glide.with(this@ProfilePageActivity)
                        .load(photoUrl)
                        .circleCrop()
                        .into(profileImageView)

                    Toast.makeText(
                        this@ProfilePageActivity,
                        "Profile image updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("ProfilePage", "Error uploading profile image", e)
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@ProfilePageActivity,
                        "Failed to update profile image: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun updateUsername(newUsername: String) {
        val currentUser = auth.currentUser ?: return
        val progressDialog = ProgressDialog(this).apply {
            setCancelable(false)
            setMessage("Updating username...")
            show()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Update Firestore user document
                val updates = hashMapOf<String, Any>(
                    "username" to newUsername
                )

                firestore.collection("Users")
                    .document(currentUser.uid)
                    .update(updates)
                    .await()

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    // Update UI
                    usernameTextView.text = newUsername

                    Toast.makeText(
                        this@ProfilePageActivity,
                        "Username updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("ProfilePage", "Error updating username", e)
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@ProfilePageActivity,
                        "Failed to update username: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Add a method to load more artworks when user scrolls down
    fun loadMoreArtworks() {
        if (hasMoreArtworks && !isLoadingMoreArtworks) {
            loadUserArtworks(false)
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure FAB is visible after coming back to this activity
        editProfileFab.visibility = View.VISIBLE

        // Refresh data when returning to this activity
        refreshProfileData()
    }

    private fun refreshProfileData() {
        // Clear existing data
        artworksGridLayout.removeAllViews()

        // Reload data
        loadUserProfile()
        loadUserArtworks(true)
    }

    // Override the back button behavior
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}