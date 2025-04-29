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
import com.google.firebase.auth.FirebaseAuth
import android.widget.ImageButton
import android.widget.ImageView



abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Only inflate child layout if this is a fresh creation
        if (savedInstanceState == null) {
            val contentFrame = findViewById<FrameLayout>(R.id.fragment_container)
            LayoutInflater.from(this).inflate(getContentLayoutId(), contentFrame, true)
            setupBottomNavigation()
        } else {
            setupBottomNavigation()
        }

        setupMenuButton()
        setupTrophyIcon()
    }

    fun loadFragment(fragment: Fragment, title: String) {
        // Clear any existing back stack
        for (i in 0 until supportFragmentManager.backStackEntryCount) {
            supportFragmentManager.popBackStack()
        }

        // Replace fragment
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .commitNow()

        // Toolbar başlığını güncelle
        setToolbarTitle(title)
    }

    abstract fun getContentLayoutId(): Int

    // In BaseActivity.kt, modify the setupBottomNavigation() method

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Set the selected item without triggering the listener
        when (this) {
            is MainActivity -> bottomNavigation.selectedItemId = R.id.nav_home
            is ProfilePageActivity -> bottomNavigation.selectedItemId = R.id.nav_profile
            is ImageGenerationPageActivity -> bottomNavigation.selectedItemId = R.id.nav_create
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (this !is MainActivity) {
                        startActivity(Intent(this, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        finish()
                    } else {
                        loadFragment(HomeFragment(), "Home")
                    }
                    true
                }
                R.id.nav_create -> {
                    if (this !is ImageGenerationPageActivity) {
                        startActivity(Intent(this, ImageGenerationPageActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        finish()
                    }
                    true
                }
                R.id.nav_profile -> {
                    if (this !is ProfilePageActivity) {
                        startActivity(Intent(this, ProfilePageActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        finish()
                    }
                    true
                }
                R.id.nav_search -> {
                    loadFragment(SearchFragment(), "Search")
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

    private fun setupMenuButton() {
        val menuButton = findViewById<ImageButton>(R.id.menu_button)

        menuButton.setOnClickListener {
            val menuPopupHandler = MenuPopupHandler(this)
            menuPopupHandler.showMenuPopup()
        }
    }

    // Add the new method to handle trophy icon click
    private fun setupTrophyIcon() {
        val trophyIcon = findViewById<ImageView>(R.id.trophy_icon)

        trophyIcon.setOnClickListener {
            // Check if already on ChallengesActivity to avoid recreation
            if (this !is ChallengesPageActivity) {
                // Navigate to ChallengesActivity
                val intent = Intent(this, ChallengesPageActivity::class.java)
                startActivity(intent)
            }
        }
    }
}