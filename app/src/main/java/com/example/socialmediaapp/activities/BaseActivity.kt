package com.example.socialmediaapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.example.socialmediaapp.MainActivity
import com.example.socialmediaapp.R
import com.example.socialmediaapp.fragments.HomeFragment
import com.example.socialmediaapp.fragments.SearchFragment
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
    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    abstract fun getContentLayoutId(): Int

    // In BaseActivity.kt, modify the setupBottomNavigation() method

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        when (this) {
            is MainActivity -> bottomNavigation.selectedItemId = R.id.nav_home
            is ProfilePageActivity -> bottomNavigation.selectedItemId = R.id.nav_profile
            is ImageGenerationPageActivity -> bottomNavigation.selectedItemId = R.id.nav_create

        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment()) // Fragment yükleme
                    true
                }
                R.id.nav_create -> {
                    if (this !is ImageGenerationPageActivity) {
                        startActivity(Intent(this, ImageGenerationPageActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_profile -> {
                    if (this !is ProfilePageActivity) {
                        startActivity(Intent(this, ProfilePageActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_search ->{
                    loadFragment(SearchFragment()) // Fragment yükleme
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