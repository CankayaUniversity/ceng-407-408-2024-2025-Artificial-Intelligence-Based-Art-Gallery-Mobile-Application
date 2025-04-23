package com.example.socialmediaapp.activities

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.socialmediaapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AchievementsActivity : BaseActivity() {
    private lateinit var challengesTab: TextView
    private lateinit var achievementsTab: TextView

    // Lists to hold dynamic achievement views
    private val achievementContainers = mutableListOf<ConstraintLayout>()
    private val achievementCheckmarks = mutableListOf<ImageView>()
    private val achievementTexts = mutableListOf<TextView>()

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun getContentLayoutId(): Int {
        return R.layout.activity_achievements
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set toolbar title
        setToolbarTitle("Achievements")

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        initializeViews()

        // Set up tab navigation
        setupTabNavigation()

        // Load achievements from Firebase
        loadAchievements()
    }

    private fun initializeViews() {
        // Tab views
        challengesTab = findViewById(R.id.challenges_tab)
        achievementsTab = findViewById(R.id.achievements_tab)

        // Find all achievement containers, checkmarks, and text views
        for (i in 1..7) { // Assuming 7 achievement views in layout
            val containerId = resources.getIdentifier("achievement_${i}_container", "id", packageName)
            val checkmarkId = resources.getIdentifier("achievement_${i}_checkmark", "id", packageName)
            val textId = resources.getIdentifier("achievement_${i}_text", "id", packageName)

            if (containerId != 0 && checkmarkId != 0 && textId != 0) {
                val container = findViewById<ConstraintLayout>(containerId)
                val checkmark = findViewById<ImageView>(checkmarkId)
                val text = findViewById<TextView>(textId)

                achievementContainers.add(container)
                achievementCheckmarks.add(checkmark)
                achievementTexts.add(text)
            }
        }
    }

    private fun setupTabNavigation() {
        // Set achievements tab as active
        achievementsTab.setBackgroundResource(R.drawable.tab_selected_background)
        challengesTab.setBackgroundResource(android.R.color.transparent)

        // Set up tab click listeners
        achievementsTab.setOnClickListener {
            // Already on achievements tab, do nothing
        }

        challengesTab.setOnClickListener {
            // Navigate to challenges activity
            startActivity(android.content.Intent(this, ChallengesActivity::class.java))
            finish()
        }
    }

    private fun loadAchievements() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            // Handle not logged in state
            showToast("Please login to view achievements")
            return
        }

        // Show loading indicator
        showProgressDialog("Loading achievements...")

        // Get all achievements from Firestore
        firestore.collection("Achievements")
            .get()
            .addOnSuccessListener { documents ->
                hideProgressDialog()

                val achievements = mutableListOf<Map<String, Any>>()

                for (document in documents) {
                    val data = document.data
                    data["id"] = document.id
                    achievements.add(data)
                }

                // Sort achievements by category and required_count
                achievements.sortWith(compareBy<Map<String, Any>> { it["category"] as String }
                    .thenBy { it["required_count"] as Long })

                // Display achievements
                displayAchievements(userId, achievements)
            }
            .addOnFailureListener { e ->
                hideProgressDialog()
                showToast("Failed to load achievements: ${e.message}")
            }
    }

    private fun displayAchievements(userId: String, achievements: List<Map<String, Any>>) {
        // Display at most as many achievements as we have views for
        val displayCount = minOf(achievements.size, achievementContainers.size)

        for (i in 0 until displayCount) {
            val achievement = achievements[i]
            val container = achievementContainers[i]
            val checkmark = achievementCheckmarks[i]
            val textView = achievementTexts[i]

            // Show the container
            container.visibility = View.VISIBLE

            // Set achievement text
            val title = achievement["title"] as? String ?: achievement["description"] as? String ?: "Unknown Achievement"
            textView.text = title

            // Set onclick listener for achievement container
            container.setOnClickListener {
                // Show achievement details if clicked
                showAchievementDetails(achievement)
            }

            // Check if user has unlocked this achievement
            val achievementId = achievement["id"] as String
            checkAchievementStatus(userId, achievementId, checkmark)
        }

        // Hide any unused containers
        for (i in displayCount until achievementContainers.size) {
            achievementContainers[i].visibility = View.GONE
        }
    }

    private fun showAchievementDetails(achievement: Map<String, Any>) {
        val title = achievement["title"] as? String ?: "Achievement"
        val description = achievement["description"] as? String ?: ""
        val points = achievement["points"] as? Long ?: 0
        val requiredCount = achievement["required_count"] as? Long ?: 0
        val category = achievement["category"] as? String ?: "general"

        // Create and show details dialog
        val message = """
            Description: $description
            Category: $category
            Required: $requiredCount
            Points: $points
        """.trimIndent()

        showAlertDialog(title, message)
    }

    private fun checkAchievementStatus(userId: String, achievementId: String, checkmark: ImageView) {
        firestore.collection("Users")
            .document(userId)
            .collection("UnlockedAchievements")
            .document(achievementId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Achievement unlocked
                    checkmark.setImageResource(R.drawable.ic_checked_circle)
                } else {
                    // Achievement not unlocked
                    checkmark.setImageResource(R.drawable.ic_unchecked_circle)
                }
            }
            .addOnFailureListener {
                // Handle error
                checkmark.setImageResource(R.drawable.ic_unchecked_circle)
            }
    }

    // Method to check and update achievement progress based on user action
    fun checkAchievementProgress(actionType: String) {
        val userId = auth.currentUser?.uid ?: return

        // Get the count of actions performed by the user
        when (actionType) {
            "creation" -> countUserGeneratedArtworks(userId)
            "likes" -> countUserLikedArtworks(userId)
            "followers" -> countUserFollowers(userId)
            "following" -> countUserFollowing(userId)
            "challenges" -> countCompletedChallenges(userId)
        }
    }

    private fun countUserGeneratedArtworks(userId: String) {
        firestore.collection("Artworks")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val count = documents.size().toLong()
                // Check if any achievements are unlocked with this count
                checkForUnlockedAchievements(userId, "creation", count)
            }
    }

    private fun countUserLikedArtworks(userId: String) {
        firestore.collection("Users")
            .document(userId)
            .collection("LikedArtworks")
            .get()
            .addOnSuccessListener { documents ->
                val count = documents.size().toLong()
                // Check if any achievements are unlocked with this count
                checkForUnlockedAchievements(userId, "likes", count)
            }
    }

    private fun countUserFollowers(userId: String) {
        firestore.collection("Users")
            .document(userId)
            .collection("Followers")
            .get()
            .addOnSuccessListener { documents ->
                val count = documents.size().toLong()
                // Check if any achievements are unlocked with this count
                checkForUnlockedAchievements(userId, "followers", count)
            }
    }

    private fun countUserFollowing(userId: String) {
        firestore.collection("Users")
            .document(userId)
            .collection("Following")
            .get()
            .addOnSuccessListener { documents ->
                val count = documents.size().toLong()
                // Check if any achievements are unlocked with this count
                checkForUnlockedAchievements(userId, "following", count)
            }
    }

    private fun countCompletedChallenges(userId: String) {
        firestore.collection("Users")
            .document(userId)
            .collection("CompletedChallenges")
            .get()
            .addOnSuccessListener { documents ->
                val count = documents.size().toLong()
                // Check if any achievements are unlocked with this count
                checkForUnlockedAchievements(userId, "challenges", count)
            }
    }

    private fun checkForUnlockedAchievements(userId: String, category: String, count: Long) {
        // Query achievements related to this action type
        firestore.collection("Achievements")
            .whereEqualTo("category", category)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val requiredCount = document.getLong("required_count") ?: 0

                    // If user meets or exceeds required count
                    if (count >= requiredCount) {
                        // Check if already unlocked
                        val achievementId = document.id
                        checkAndUnlockAchievement(userId, achievementId)
                    }
                }
            }
    }

    private fun checkAndUnlockAchievement(userId: String, achievementId: String) {
        firestore.collection("Users")
            .document(userId)
            .collection("UnlockedAchievements")
            .document(achievementId)
            .get()
            .addOnSuccessListener { unlockDoc ->
                if (unlockDoc == null || !unlockDoc.exists()) {
                    // Unlock the achievement
                    unlockAchievement(userId, achievementId)
                }
            }
    }

    private fun unlockAchievement(userId: String, achievementId: String) {
        // Get achievement details
        firestore.collection("Achievements")
            .document(achievementId)
            .get()
            .addOnSuccessListener { achievementDoc ->
                if (achievementDoc != null && achievementDoc.exists()) {
                    val points = achievementDoc.getLong("points") ?: 0
                    val title = achievementDoc.getString("title") ?: "Achievement"

                    // Record unlocked achievement
                    val achievementData = hashMapOf(
                        "unlockedAt" to com.google.firebase.Timestamp.now(),
                        "achievementId" to achievementId,
                        "points" to points
                    )

                    firestore.collection("Users")
                        .document(userId)
                        .collection("UnlockedAchievements")
                        .document(achievementId)
                        .set(achievementData)
                        .addOnSuccessListener {
                            // Show achievement unlock notification
                            showToast("Achievement Unlocked: $title")

                            // Update UI
                            refreshAchievements()

                            // Update user's total points
                            updateUserPoints(userId, points)
                        }
                }
            }
    }

    private fun refreshAchievements() {
        // Reload achievements to reflect changes
        loadAchievements()
    }

    private fun updateUserPoints(userId: String, points: Long) {
        // Update user's points in Firestore
        firestore.collection("Users")
            .document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                if (userDoc != null && userDoc.exists()) {
                    val currentPoints = userDoc.getLong("points") ?: 0
                    val newPoints = currentPoints + points

                    firestore.collection("Users")
                        .document(userId)
                        .update("points", newPoints)
                }
            }
    }

    // Helper methods for UI feedback
    private fun showToast(message: String) {
        // Assuming BaseActivity has a showToast method
        // If not, implement it or use Android's Toast directly
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showProgressDialog(message: String) {
        // Assuming BaseActivity has a showProgressDialog method
        // If not, implement it or use a custom ProgressDialog
    }

    private fun hideProgressDialog() {
        // Assuming BaseActivity has a hideProgressDialog method
        // If not, implement it
    }

    private fun showAlertDialog(title: String, message: String) {
        // Show a simple alert dialog with achievement details
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("OK", null)
        builder.show()
    }
}