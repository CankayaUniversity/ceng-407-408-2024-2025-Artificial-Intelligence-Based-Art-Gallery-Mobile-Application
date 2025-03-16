package com.example.socialmediaapp.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

class StoryGenerationPageActivity: BaseActivity() {
    override fun getContentLayoutId(): Int {
        return R.layout.activity_story_generation_page
    }
    private lateinit var resultImageView: ImageView
    private lateinit var storyTitleTextView: TextView
    private lateinit var storyContentTextView: TextView
    private lateinit var downloadButton: ImageView
    private lateinit var shareButton: ImageButton
    private lateinit var publicRadioButton: RadioButton
    private lateinit var followersRadioButton: RadioButton
    private lateinit var addTagsButton: Button

    private var imageUrl: String? = null
    private var story: String? = null
    private var prompt: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize views
        resultImageView = findViewById(R.id.resultImageView)
        storyTitleTextView = findViewById(R.id.storyTitleTextView)
        storyContentTextView = findViewById(R.id.storyContentTextView)
        downloadButton = findViewById(R.id.downloadButton)
        shareButton = findViewById(R.id.shareButton)
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
}