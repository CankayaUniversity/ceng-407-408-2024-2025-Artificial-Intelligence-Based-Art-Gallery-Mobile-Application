// 1. First, create a MenuActivity.kt file to handle the menu popup
package com.example.socialmediaapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import com.example.socialmediaapp.R
import com.google.firebase.auth.FirebaseAuth

class MenuPopupHandler(private val activity: BaseActivity) {

    private var popupWindow: PopupWindow? = null

    fun showMenuPopup() {
        // Inflate the popup layout
        val inflater = activity.layoutInflater
        val popupView = inflater.inflate(R.layout.popup_menu, null)

        // Create the popup window
        popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        // Set up buttons
        val settingsButton = popupView.findViewById<Button>(R.id.settings_button)
        val signOutButton = popupView.findViewById<Button>(R.id.sign_out_button)

        // Set up click listeners
        settingsButton.setOnClickListener {
            popupWindow?.dismiss()
            // Navigate to settings page
            val intent = Intent(activity, SettingsActivity::class.java)
            activity.startActivity(intent)
        }

        signOutButton.setOnClickListener {
            popupWindow?.dismiss()
            // Sign out user
            FirebaseAuth.getInstance().signOut()

            // Navigate to sign in screen
            val intent = Intent(activity, SignInAc::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            activity.startActivity(intent)
            activity.finish()
        }

        // Show popup at menu button location
        val menuButton = activity.findViewById<android.widget.ImageButton>(R.id.menu_button)
        popupWindow?.showAsDropDown(menuButton, 0, 0, Gravity.END)
    }

    fun dismissPopup() {
        popupWindow?.dismiss()
    }
}

