package com.example.artminds_ai

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class ResultActivity : AppCompatActivity() {

    private lateinit var resultImageView: ImageView
    private lateinit var promptTextView: TextView
    private lateinit var storyTitleTextView: TextView
    private lateinit var storyTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // Enable the back button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Generated Content"

        // Initialize views
        resultImageView = findViewById(R.id.resultImageView)
        promptTextView = findViewById(R.id.promptTextView)
        storyTitleTextView = findViewById(R.id.storyTitleTextView)
        storyTextView = findViewById(R.id.storyTextView)

        // Get data from intent
        val imageUrl = intent.getStringExtra("IMAGE_URL") ?: ""
        val story = intent.getStringExtra("STORY")
        val prompt = intent.getStringExtra("PROMPT") ?: "No prompt provided"

        // Set the prompt text
        promptTextView.text = "Prompt: $prompt"

        // Load the image
        if (imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .into(resultImageView)
        }

        // Set the story text if available
        if (!story.isNullOrEmpty()) {
            storyTitleTextView.visibility = View.VISIBLE
            storyTextView.visibility = View.VISIBLE
            storyTextView.text = story
        } else {
            storyTitleTextView.visibility = View.GONE
            storyTextView.visibility = View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}