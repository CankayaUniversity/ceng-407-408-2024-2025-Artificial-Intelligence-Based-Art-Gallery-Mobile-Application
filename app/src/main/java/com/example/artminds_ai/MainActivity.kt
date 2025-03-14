package com.example.artminds_ai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

//Inherits BaseActivity layout
class MainActivity : BaseActivity() {
    override fun getContentLayoutId(): Int {
        return R.layout.activity_main
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find the button by ID
        val navigateButton = findViewById<Button>(R.id.navigateButton)

        // Set click listener
        navigateButton.setOnClickListener {
            val intent = Intent(this, StoryGenerationPageActivity::class.java)
            startActivity(intent)
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_notifications -> {
                    true
                }
                R.id.nav_search -> {
                    // Handle search click
                    true
                }
                R.id.nav_home -> {
                    // Handle home click
                    true
                }
                R.id.nav_discover -> {
                    // Handle discover click
                    true
                }
                R.id.nav_profile -> {
                    // Handle profile click
                    true
                }
                else -> false
            }
        }
    }
}