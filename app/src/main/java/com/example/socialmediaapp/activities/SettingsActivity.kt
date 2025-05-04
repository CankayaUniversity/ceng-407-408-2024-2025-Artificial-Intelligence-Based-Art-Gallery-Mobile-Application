// Create SettingsActivity.kt file
package com.example.socialmediaapp.activities

//import com.example.socialmediaapp.databinding.ActivitySettingsBinding
import android.R.id.toggle
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.databinding.DataBindingUtil
import androidx.savedstate.SavedStateRegistry
import com.example.socialmediaapp.R
import com.example.socialmediaapp.databinding.ActivitySignInBinding
import kotlin.properties.Delegates


class SettingsActivity() : BaseActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_NAME = "ThemePrefs"

    override fun getContentLayoutId(): Int {
        return R.layout.activity_settings
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode", "InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set toolbar title
        setToolbarTitle("Settings")

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)

        // Setup back button
        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            onBackPressed()
        }

        val mainLayout = findViewById<ConstraintLayout>(R.id.settings)

        if (sharedPreferences.getBoolean(PREF_NAME, false)) {
            mainLayout.setBackgroundColor("#3F51B5".toColorInt())
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

        // Setup light mode toggle
        val lightModeSwitch = findViewById<Switch>(R.id.light_mode_switch)
        lightModeSwitch.isChecked = sharedPreferences.getBoolean(PREF_NAME, false)

        val editor = sharedPreferences.edit()

        // Check if the light mode switch is on or off
        lightModeSwitch.setOnClickListener {
            if (lightModeSwitch.isChecked) {
                editor.putBoolean(PREF_NAME, true)
                editor.apply()
                editor.commit()
                mainLayout.setBackgroundColor("#3F51B5".toColorInt())
            }

            else {
                editor.putBoolean(PREF_NAME, false)
                editor.apply()
                editor.commit()
                mainLayout.setBackgroundColor("#10142F".toColorInt())
            }
        }

    }


}