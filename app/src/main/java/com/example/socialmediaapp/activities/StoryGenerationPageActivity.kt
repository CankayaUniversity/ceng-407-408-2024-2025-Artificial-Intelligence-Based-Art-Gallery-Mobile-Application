package com.example.socialmediaapp.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.socialmediaapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.UUID
import android.content.ContentValues
import android.media.MediaScannerConnection
import android.provider.MediaStore

class StoryGenerationPageActivity: BaseActivity() {
    override fun getContentLayoutId(): Int {
        return R.layout.activity_story_generation_page
    }
    private lateinit var resultImageView: ImageView
    private lateinit var storyTitleTextView: TextView
    private lateinit var storyContentTextView: TextView
    private lateinit var downloadButton: ImageView
    private lateinit var shareButton: ImageButton
    private lateinit var sendButton: Button
    private lateinit var publicRadioButton: RadioButton
    private lateinit var followersRadioButton: RadioButton
    private lateinit var tagDisplay: TextView
    private lateinit var toolbarTitle : TextView

    private var imageUrl: String? = null
    private var story: String? = null
    private var prompt: String? = null
    private var generatedTag: String = ""

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize views
        resultImageView = findViewById(R.id.resultImageView)
        storyTitleTextView = findViewById(R.id.storyTitleTextView)
        storyContentTextView = findViewById(R.id.storyContentTextView)
        downloadButton = findViewById(R.id.downloadButton)
        shareButton = findViewById(R.id.shareButton)
        sendButton = findViewById(R.id.sendButton)
        publicRadioButton = findViewById(R.id.publicRadioButton)
        followersRadioButton = findViewById(R.id.followersRadioButton)
        tagDisplay = findViewById(R.id.tagDisplay)
        toolbarTitle = findViewById(R.id.toolbar_title)

        // Get data from intent
        imageUrl = intent.getStringExtra("IMAGE_URL")
        story = intent.getStringExtra("STORY")
        prompt = intent.getStringExtra("PROMPT")

        // Set title based on prompt
        val title = generateTitleFromPrompt(prompt ?: "Generated Story")
        storyTitleTextView.text = title

        // Generate and display caption
        preGenerateCaption()

        // Load image using Glide
        imageUrl?.let {
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.placeholder_image2)
                .error(R.drawable.error_image)
                .into(resultImageView)
        }

        // Set story content
        storyContentTextView.text = story ?: "No story was generated."

        // Set up click listeners
        downloadButton.setOnClickListener {
            downloadImage()
        }

        shareButton.setOnClickListener {
            shareContent()
        }

        // Add send button click listener
        sendButton.setOnClickListener {
            saveToFirebase()
        }

        toolbarTitle.text = "Your Generation Result"
        //toolbarTitle.gravity = Gravity.CLIP_VERTICAL
        // Coloring the default radio button option to app's color
        publicRadioButton.buttonTintList = ColorStateList.valueOf("#3389FF".toColorInt())

        // Public radio button color change part
        publicRadioButton.setOnClickListener {
            if(!publicRadioButton.isChecked) {
                publicRadioButton.buttonTintList = ColorStateList.valueOf(Color.GRAY)
                followersRadioButton.buttonTintList = ColorStateList.valueOf("#3389FF".toColorInt())
            }
            else {
                publicRadioButton.buttonTintList = ColorStateList.valueOf("#3389FF".toColorInt())
                followersRadioButton.buttonTintList = ColorStateList.valueOf(Color.GRAY)
            }
        }

        // Followers radio button color change part
        followersRadioButton.setOnClickListener {
            if(!publicRadioButton.isChecked) {
                publicRadioButton.buttonTintList = ColorStateList.valueOf(Color.GRAY)
                followersRadioButton.buttonTintList = ColorStateList.valueOf("#3389FF".toColorInt())
            }
            else {
                publicRadioButton.buttonTintList = ColorStateList.valueOf("#3389FF".toColorInt())
                followersRadioButton.buttonTintList = ColorStateList.valueOf(Color.GRAY)
            }
        }

    }

    // Function to generate a caption early - call this when loading the page
    private fun preGenerateCaption() {
        val caption = generateCaption(storyTitleTextView.text.toString(), prompt ?: "")
        generatedTag = caption
        tagDisplay.text = "Tag: $caption"
    }

    private fun generateTitleFromPrompt(prompt: String): String {
        // Simple algorithm to generate a title - extract first few words and capitalize them
        val words = prompt.split(" ")
        val titleWords = if (words.size > 4) words.subList(0, 4) else words
        return titleWords.joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }.take(30) + if (prompt.length > 30) "..." else ""
    }

    // Function to generate a short caption for art creation (max 10 characters)
    private fun generateCaption(title: String, prompt: String): String {
        // Extract meaningful words from title and prompt
        val combinedText = "$title $prompt".lowercase(Locale.getDefault())

        // Remove special characters and extra spaces
        val cleanedText = combinedText.replace(Regex("[^a-zA-Z0-9\\s]"), "").trim()

        // Split into words
        val words = cleanedText.split(Regex("\\s+"))

        // Try to find meaningful keywords
        val keywords = words.filter { it.length > 3 }
            .sortedByDescending { it.length }
            .take(1)  // Take only the most significant keyword

        // If we found a good keyword, use it; otherwise generate something random
        val baseTag = if (keywords.isNotEmpty()) {
            keywords[0].take(7)  // Take at most 7 characters from the keyword
        } else {
            // Fallback to using the first few characters of the title
            title.take(7).lowercase(Locale.getDefault()).replace(Regex("[^a-z]"), "")
        }

        // Add a short random element (3 characters) to ensure uniqueness
        val randomPart = UUID.randomUUID().toString().substring(0, 3)

        // Combine keyword with random part, ensuring total length <= 10 characters
        val fullTag = baseTag + randomPart
        return fullTag.take(10)
    }

    private fun downloadImage() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val drawable = resultImageView.drawable
                if (drawable is BitmapDrawable) {
                    val bitmap = drawable.bitmap
                    val filename = "ArtMinds_${System.currentTimeMillis()}.jpg"

                    // Option 1: For Android 10 (Q) and above - use MediaStore
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ArtMinds")
                        }

                        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        uri?.let {
                            contentResolver.openOutputStream(it)?.use { stream ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                            }

                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@StoryGenerationPageActivity,
                                    "Image saved to your gallery",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    // Option 2: For devices below Android 10
                    else {
                        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        val artMindsDir = File(storageDir, "ArtMinds")
                        if (!artMindsDir.exists()) {
                            artMindsDir.mkdirs()
                        }

                        val file = File(artMindsDir, filename)
                        FileOutputStream(file).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                        }

                        // Broadcast to refresh gallery
                        MediaScannerConnection.scanFile(
                            this@StoryGenerationPageActivity,
                            arrayOf(file.toString()),
                            arrayOf("image/jpeg"),
                            null
                        )

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@StoryGenerationPageActivity,
                                "Image saved to: Pictures/ArtMinds",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@StoryGenerationPageActivity,
                            "Failed to save image: Image not loaded",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@StoryGenerationPageActivity,
                        "Failed to save image: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Log the error
                    Log.e("DownloadImage", "Error saving image", e)
                }
            }
        }
    }

    // Share the images and story on third party applications
    private fun shareContent() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val drawable = resultImageView.drawable
                if (drawable is BitmapDrawable) {
                    val bitmap = drawable.bitmap
                    val filename = "ArtMinds_share_${System.currentTimeMillis()}.jpg"
                    val file = File(cacheDir, filename)

                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    }

                    val uri = FileProvider.getUriForFile(
                        this@StoryGenerationPageActivity,
                        "${packageName}.fileprovider",
                        file
                    )

                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, "${storyTitleTextView.text}\n\n${storyContentTextView.text}\n\nCreated with ArtMinds AI")
                        type = "image/jpeg"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    withContext(Dispatchers.Main) {
                        startActivity(Intent.createChooser(shareIntent, "Share your creation"))
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@StoryGenerationPageActivity,
                            "Failed to share: Image not loaded",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@StoryGenerationPageActivity,
                        "Failed to share: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Save the content to Firebase (modified to save in both Posts and Images collections)
    private fun saveToFirebase() {
        // Check if user is logged in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in to save your creation", Toast.LENGTH_SHORT).show()
            return
        }

        // Show progress dialog
        val progressDialog = android.app.ProgressDialog(this).apply {
            setCancelable(false)
            setMessage("Saving your creation...")
            show()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val drawable = resultImageView.drawable
                if (drawable !is BitmapDrawable) {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@StoryGenerationPageActivity,
                            "Failed to save: Image not loaded",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                // Get bitmap from ImageView
                val bitmap = drawable.bitmap

                // Upload image to Firebase Storage
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
                val imageData = baos.toByteArray()

                // Create a unique file name
                val imageName = "Images/${currentUser.uid}/${UUID.randomUUID()}.jpg"
                val storageRef = storage.reference.child(imageName)

                // Upload the image
                storageRef.putBytes(imageData).await()
                val imageDownloadUrl = storageRef.downloadUrl.await().toString()

                // Fetch additional user information needed for Feed object
                val userDoc = firestore.collection("Users")
                    .document(currentUser.uid)
                    .get()
                    .await()

                val username = userDoc.getString("username") ?: currentUser.displayName ?: "User"
                val userImageUrl = userDoc.getString("image") ?: ""

                // Get visibility setting
                val isPublic = publicRadioButton.isChecked

                // Use the already generated caption
                val caption = generatedTag
                if (caption.isEmpty()) {
                    // Generate a new one if needed
                    generatedTag = generateCaption(storyTitleTextView.text.toString(), prompt ?: "")
                }

                // Current timestamp for post
                val timestamp = System.currentTimeMillis()

                // Create data for Posts collection (for Feed display)
                val postData = hashMapOf(
                    "username" to username,
                    "userid" to currentUser.uid,
                    "image" to imageDownloadUrl, // The artwork image
                    "imageposter" to userImageUrl, // User's profile image
                    "caption" to storyTitleTextView.text.toString(), // Using title as caption
                    "time" to timestamp,
                    "postid" to "", // Will update after document creation
                    "likes" to 0,
                    "comments" to 0
                )

                // Add to Posts collection with a generated ID
                val postDocRef = firestore.collection("Posts").document()
                val postId = postDocRef.id

                // Update the postid field with the actual document ID
                postData["postid"] = postId

                // Save to Posts collection
                postDocRef.set(postData).await()

                // Create data for Images collection (preserving all artwork attributes)
                val imageDocData = hashMapOf(
                    "userid" to currentUser.uid,
                    "title" to storyTitleTextView.text.toString(),
                    "story" to storyContentTextView.text.toString(),
                    "prompt" to (prompt ?: ""),
                    "imageUrl" to imageDownloadUrl,
                    "public" to isPublic,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "likes" to 0,
                    "comments" to 0,
                    "caption" to generatedTag,
                    "postid" to postId // Reference to the post document
                )

                // Save to Images collection
                firestore.collection("Images").document().set(imageDocData).await()

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@StoryGenerationPageActivity,
                        "Your creation has been saved successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@StoryGenerationPageActivity,
                        "Failed to save to Firebase: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Log the error for debugging
                    Log.e("SaveToFirebase", "Error saving to Firebase", e)
                }
            }
        }
    }
}