// Create SettingsActivity.kt file
package com.example.socialmediaapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.socialmediaapp.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class SettingsActivity : BaseActivity() {

    override fun getContentLayoutId(): Int {
        return R.layout.activity_settings
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set toolbar title
        setToolbarTitle("Settings")

        // Setup back button
        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            onBackPressed()
        }

        // Setup change password option
        val changePasswordLayout = findViewById<ConstraintLayout>(R.id.change_password_layout)
        changePasswordLayout.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        // Setup language selector
        val languageLayout = findViewById<ConstraintLayout>(R.id.language_layout)
        val languageText = findViewById<TextView>(R.id.language_text)

        languageLayout.setOnClickListener {
            // For now, we'll just toggle between English and other language as an example
            if (languageText.text == "English") {
                languageText.text = "Turkish"
                // In a real implementation, we would change app language here
            } else {
                languageText.text = "English"
            }
        }

        // Setup dark mode toggle
        val darkModeSwitch = findViewById<Switch>(R.id.dark_mode_switch)

        // Initialize switch state based on current night mode
        darkModeSwitch.isChecked = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            // Save preference to SharedPreferences
            val sharedPrefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
            sharedPrefs.edit().putBoolean("DarkMode", isChecked).apply()
        }
    }
}