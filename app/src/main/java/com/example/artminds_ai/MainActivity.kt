package com.example.artminds_ai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find the button by ID
        val navigateButton = findViewById<Button>(R.id.navigateButton)

        // Set click listener
        navigateButton.setOnClickListener {
            val intent = Intent(this, ImageGenerationPageActivity::class.java)
            startActivity(intent)
        }
    }
}