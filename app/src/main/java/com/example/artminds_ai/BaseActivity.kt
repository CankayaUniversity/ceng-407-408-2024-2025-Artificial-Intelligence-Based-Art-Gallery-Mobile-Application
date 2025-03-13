package com.example.artminds_ai

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val contentFrame = findViewById<FrameLayout>(R.id.fragment_container)
        val contentView = LayoutInflater.from(this).inflate(getContentLayoutId(), contentFrame, false)
        contentFrame.addView(contentView)

        setupBottomNavigation()
    }

    abstract fun getContentLayoutId(): Int

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_notifications -> {
                    // Handle notifications click
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