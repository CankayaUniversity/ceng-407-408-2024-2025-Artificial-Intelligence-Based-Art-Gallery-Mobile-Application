package com.example.socialmediaapp.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
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

class StoryGenerationPageActivity: BaseActivity() {
    override fun getContentLayoutId(): Int {
        return R.layout.activity_story_generation_page
    }
    private lateinit var resultImageView: ImageView
    private lateinit var storyTitleTextView: TextView
    private lateinit var storyContentTextView: TextView
    private lateinit var downloadButton: ImageView
    private lateinit var shareButton: ImageButton
    private lateinit var sendButton: Button  // New send button
    private lateinit var publicRadioButton: RadioButton
    private lateinit var followersRadioButton: RadioButton
    private lateinit var addTagsButton: Button

    private var imageUrl: String? = null
    private var story: String? = null
    private var prompt: String? = null

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
        sendButton = findViewById(R.id.sendButton)  // Make sure to add this button in your layout
        publicRadioButton = findViewById(R.id.publicRadioButton)
        followersRadioButton = findViewById(R.id.followersRadioButton)
        addTagsButton = findViewById(R.id.addTagsButton)

        // Get data from intent
        imageUrl = intent.getStringExtra("IMAGE_URL")
        story = intent.getStringExtra("STORY")
        prompt = intent.getStringExtra("PROMPT")

        // Set title based on prompt
        val title = generateTitleFromPrompt(prompt ?: "Generated Story")
        storyTitleTextView.text = title

        // Load image using Glide
        imageUrl?.let {
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.placeholder_image)
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

        addTagsButton.setOnClickListener {
            Toast.makeText(this, "Add tags feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateTitleFromPrompt(prompt: String): String {
        // Simple algorithm to generate a title - extract first few words and capitalize them
        val words = prompt.split(" ")
        val titleWords = if (words.size > 4) words.subList(0, 4) else words
        return titleWords.joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }.take(30) + if (prompt.length > 30) "..." else ""
    }

    private fun downloadImage() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val drawable = resultImageView.drawable
                if (drawable is BitmapDrawable) {
                    val bitmap = drawable.bitmap
                    val filename = "ArtMinds_${System.currentTimeMillis()}.jpg"
                    val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename)

                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@StoryGenerationPageActivity,
                            "Image saved to: ${file.absolutePath}",
                            Toast.LENGTH_LONG
                        ).show()
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

    // Save the content to Firebase
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
                val uploadTask = storageRef.putBytes(imageData).await()
                val imageDownloadUrl = storageRef.downloadUrl.await().toString()

                // Get visibility setting
                val isPublic = publicRadioButton.isChecked

                // Create a document in Firestore
                val creationData = hashMapOf(
                    "userId" to currentUser.uid,
                    "title" to storyTitleTextView.text.toString(),
                    "story" to storyContentTextView.text.toString(),
                    "prompt" to (prompt ?: ""),
                    "imageUrl" to imageDownloadUrl,
                    "public" to isPublic,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "likes" to 0,
                    "comments" to 0
                )

                // Add directly to Images collection with a generated ID
                val creationId = firestore.collection("Images")
                    .document() // This will generate a random ID
                    .set(creationData)
                    .await()

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
                }
            }
        }
    }
}