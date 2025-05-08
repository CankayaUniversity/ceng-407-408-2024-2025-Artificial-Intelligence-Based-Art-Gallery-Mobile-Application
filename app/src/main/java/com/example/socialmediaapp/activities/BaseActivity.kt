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
import com.example.socialmediaapp.fragments.NotificationFragment


abstract class BaseActivity : AppCompatActivity() {

    // Önceki fragment'ı takip etmek için değişken eklendi
    private var currentFragment: Fragment? = null

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
        // fragment'ı back stack'e ekle
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()

        currentFragment = fragment

        setToolbarTitle(title)
    }

    abstract fun getContentLayoutId(): Int

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // intent'ten hedef fragment'ı al
        val targetFragment = intent.getStringExtra("targetFragment")

        // Seçili item'ı belirle
        when {
            this is MainActivity && targetFragment == "notifications" ->
                bottomNavigation.selectedItemId = R.id.nav_notifications
            this is MainActivity && targetFragment == "search" ->
                bottomNavigation.selectedItemId = R.id.nav_search
            this is MainActivity ->
                bottomNavigation.selectedItemId = R.id.nav_home
            this is ProfilePageActivity ->
                bottomNavigation.selectedItemId = R.id.nav_profile
            this is ImageGenerationPageActivity ->
                bottomNavigation.selectedItemId = R.id.nav_create


            this is AchievementsPageActivity -> {
                // Hiçbir item seçili olmasın, navbar butonlarının arka plan rengini değiştirmesin
                bottomNavigation.menu.setGroupCheckable(0, false, true)
                bottomNavigation.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_UNLABELED
            }
            this is SettingsActivity -> {
                bottomNavigation.menu.setGroupCheckable(0, false, true)
                bottomNavigation.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_UNLABELED
            }
            this is ChallengesPageActivity -> {
                bottomNavigation.menu.setGroupCheckable(0, false, true)
                bottomNavigation.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_UNLABELED
            }
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (this !is MainActivity) {
                        startActivity(Intent(this, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        finish()
                    } else {
                        // Ana ekrana dönmek için tüm fragmentları temizle
                        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                        loadFragment(HomeFragment(), "Home")
                    }
                    true
                }
                R.id.nav_create -> {
                    if (this !is ImageGenerationPageActivity) {
                        startActivity(Intent(this, ImageGenerationPageActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        finish()
                    } else {
                        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

                        setToolbarTitle("Create")
                    }
                    true
                }
                R.id.nav_profile -> {
                    if (this !is ProfilePageActivity) {
                        startActivity(Intent(this, ProfilePageActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        finish()
                    } else {
                        // Eğer zaten ProfilePageActivity'deysek ama farklı bir fragment'ta olabiliriz
                        // Profil ana ekranını yeniden yükle
                        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

                        setToolbarTitle("Profile")
                    }
                    true
                }
                R.id.nav_search -> {
                    /*
                    supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    loadFragment(SearchFragment(), "Search")

                    true
                    */

                    if (this !is MainActivity) {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("targetFragment", "search")
                        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        finish()
                    } else {

                        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                        loadFragment(SearchFragment(), "Search")
                    }
                    true

                }
                R.id.nav_notifications -> {
                    if (this !is MainActivity) {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("targetFragment", "notifications")
                        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        finish()
                    } else {

                        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                        loadFragment(NotificationFragment(), "Notifications")
                    }
                    true
                }
                else -> false
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {

            if (supportFragmentManager.backStackEntryCount == 0) {
                // Back stack boşsa, aktiviteye göre bottom navigation seçimini güncelle
                when (this) {
                    is MainActivity -> bottomNavigation.selectedItemId = R.id.nav_home
                    is ProfilePageActivity -> bottomNavigation.selectedItemId = R.id.nav_profile
                    is ImageGenerationPageActivity -> bottomNavigation.selectedItemId = R.id.nav_create
                }
            }
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
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

    private fun setupTrophyIcon() {
        val trophyIcon = findViewById<ImageView>(R.id.trophy_icon)

        trophyIcon.setOnClickListener {
            // Check if already on ChallengesActivity to avoid recreation
            if (this !is ChallengesPageActivity) {
                // Navigate to ChallengesActivity
                val intent = Intent(this, ChallengesPageActivity::class.java)
                startActivity(intent)
            }
            else {

                supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

                setToolbarTitle("Challenges")
            }
        }
    }
}

