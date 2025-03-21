package com.example.socialmediaapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.socialmediaapp.MainActivity
import com.example.socialmediaapp.R
import com.google.android.material.bottomnavigation.BottomNavigationView

abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Inflate child layout into container
        val contentFrame = findViewById<FrameLayout>(R.id.fragment_container)
        LayoutInflater.from(this).inflate(getContentLayoutId(), contentFrame, true)

        setupBottomNavigation()
    }

    abstract fun getContentLayoutId(): Int

    // In BaseActivity.kt, modify the setupBottomNavigation() method

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Set the selected item based on current activity
        when (this) {
            is MainActivity -> bottomNavigation.selectedItemId = R.id.nav_home
            is ProfilePageActivity -> bottomNavigation.selectedItemId = R.id.nav_profile
            is ImageGenerationPageActivity -> bottomNavigation.selectedItemId = R.id.nav_create
            // Add other activity types if needed
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_notifications -> {
                    // If this is not already the notifications activity
                    false
                }
                R.id.nav_search -> {
                    // If this is not already the search activity
                    false
                }
                R.id.nav_home -> {
                    // If this is not already the home activity
                    if (this !is MainActivity) {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("fromSignIn", true) // This triggers navigation to HomeFragment
                        startActivity(intent)
                        finish()
                    }
                    true
                }
                R.id.nav_create -> {
                    // If this is not already the image generation activity
                    if (this !is ImageGenerationPageActivity) {
                        startActivity(Intent(this, ImageGenerationPageActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_profile -> {
                    // If this is not already the profile activity
                    if (this !is ProfilePageActivity) {
                        startActivity(Intent(this, ProfilePageActivity::class.java))
                        finish()
                    }
                    true
                }
                else -> false
            }
        }
    }


    // Method to update toolbar title
    fun setToolbarTitle(title: String) {
        findViewById<TextView>(R.id.toolbar_title)?.text = title
    }
}