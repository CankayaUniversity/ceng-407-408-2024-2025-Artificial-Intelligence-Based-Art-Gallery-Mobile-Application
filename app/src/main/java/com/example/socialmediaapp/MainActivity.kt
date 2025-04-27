package com.example.socialmediaapp

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.navigation.findNavController
import com.example.socialmediaapp.activities.BaseActivity
import com.example.socialmediaapp.fragments.HomeFragment
import com.example.socialmediaapp.fragments.OtherUsersFragment

@Suppress("DEPRECATION")
class MainActivity : BaseActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Eğer daha önce bir fragment yüklenmediyse HomeFragment'ı yükle
        if (savedInstanceState == null) {
            loadFragment(HomeFragment(),"Home")
        }

        // Girişten geldiyse HomeFragment'a yönlendir
        if (intent.getBooleanExtra("fromSignIn", false)) {
            loadFragment(HomeFragment(),"Home")
        }

        if (intent.getBooleanExtra("showOtherUser", false)) {
            val userId = intent.getStringExtra("userId")
            val fragment = OtherUsersFragment().apply {
                arguments = Bundle().apply {
                    putString("userId", userId)
                }
            }
            loadFragment(fragment, "Profile")
        } else if (savedInstanceState == null || intent.getBooleanExtra("fromSignIn", false)) {
            loadFragment(HomeFragment(), "Home")
        }
    }

    override fun getContentLayoutId(): Int {
        return R.layout.activty_main
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val fragmentCount = supportFragmentManager.backStackEntryCount
        if (fragmentCount > 0) {
            super.onBackPressed()
        } else {
            // Eğer kullanıcı HomeFragment'teyse çıkış yap
            if (supportFragmentManager.findFragmentById(R.id.fragment_container) is HomeFragment) {
                moveTaskToBack(true) // Uygulamayı arka plana al
            } else {
                super.onBackPressed()
            }
        }
    }
}
