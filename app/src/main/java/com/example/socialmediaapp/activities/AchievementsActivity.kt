package com.example.socialmediaapp.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.example.socialmediaapp.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.HashSet

class AchievementsPageActivity : BaseActivity() {
    override fun getContentLayoutId(): Int {
        // Make sure this matches the exact name of your XML layout file
        return R.layout.activity_achievements
    }

    private lateinit var achievementsContainer: LinearLayout
    private lateinit var emptyView: TextView
    private lateinit var challengesTab: TextView
    private lateinit var achievementsTab: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_NAME = "ThemePrefs"
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Track already displayed achievements to prevent duplicates
    private val displayedAchievements = HashSet<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToolbarTitle("Achievements")

        // Initialize views AFTER super.onCreate has inflated the layout
        initializeViews()
        setupTabListeners()
        loadAchievements()

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val mainLayout = findViewById<ConstraintLayout>(R.id.achievements)

        if (sharedPreferences.getBoolean(PREF_NAME, false)) {
            mainLayout.setBackgroundColor("#3F51B5".toColorInt())
        }

    }

    private fun initializeViews() {
        // The correct way to find views in a child activity of BaseActivity
        val rootView = findViewById<View>(R.id.fragment_container)

        // Now find views within the inflated layout
        achievementsContainer = rootView.findViewById(R.id.achievementsContainer)
        emptyView = rootView.findViewById(R.id.emptyView)
        challengesTab = rootView.findViewById(R.id.challengesTab)
        achievementsTab = rootView.findViewById(R.id.achievementsTab)

        // Set achievements tab as selected initially
        challengesTab.isSelected = false
        achievementsTab.isSelected = true
        challengesTab.setBackgroundResource(R.drawable.tab_unselected_background)
        achievementsTab.setBackgroundResource(R.drawable.tab_selected_background)
    }

    private fun setupTabListeners() {
        challengesTab.setOnClickListener {
            // Navigate to challenges
            val intent = Intent(this, ChallengesPageActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        achievementsTab.setOnClickListener {
            // Already on achievements tab, do nothing
            if (!achievementsTab.isSelected) {
                challengesTab.isSelected = false
                achievementsTab.isSelected = true
                challengesTab.setBackgroundResource(R.drawable.tab_unselected_background)
                achievementsTab.setBackgroundResource(R.drawable.tab_selected_background)
                // No need to reload as we're already on this page
            }
        }
    }

    private fun loadAchievements() {
        val currentUser = auth.currentUser ?: return
        achievementsContainer.removeAllViews()
        emptyView.visibility = View.GONE
        displayedAchievements.clear() // Clear tracking set before loading

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val allAchievements = mutableListOf<AchievementData>()

                // Get achievements from the UserAchievements collection
                val achievementsQuery = firestore.collection("UserAchievements")
                    .whereEqualTo("userId", currentUser.uid)
                    .orderBy("completedAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                for (doc in achievementsQuery.documents) {
                    val challengeName = doc.getString("achievementName") ?:
                    doc.getString("challengeName") ?: "Unknown Achievement"
                    val constraint = doc.getLong("constraints")?.toInt() ?: 0
                    val progress = doc.getLong("progress")?.toInt() ?: 0
                    val points = doc.getLong("points")?.toInt() ?: 0
                    val type = doc.getString("type") ?: ""
                    val completedAt = doc.getTimestamp("completedAt")

                    // Generate a unique identifier for this achievement
                    // Normalize the challenge name to ensure consistent comparison
                    val uniqueKey = "${challengeName.lowercase().trim()}_${type}"

                    // Skip if we've already added this achievement to our list
                    if (uniqueKey in displayedAchievements) {
                        continue
                    }

                    // Add to displayed set to prevent duplicates
                    displayedAchievements.add(uniqueKey)

                    // Format completed date for display
                    val completedDateStr = if (completedAt != null) {
                        formatDate(completedAt.toDate())
                    } else {
                        "Unknown date"
                    }

                    allAchievements.add(
                        AchievementData(
                            uniqueKey = uniqueKey,
                            name = challengeName,
                            constraint = constraint,
                            progress = progress,
                            points = points,
                            type = type,
                            completedDate = completedDateStr,
                            docId = doc.id
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    if (allAchievements.isEmpty()) {
                        emptyView.visibility = View.VISIBLE
                        emptyView.text = "No achievements found"
                    } else {
                        // Display each achievement
                        for (achievement in allAchievements) {
                            val achievementCard = createAchievementCard(
                                challengeName = achievement.name,
                                constraint = achievement.constraint,
                                progress = achievement.progress,
                                points = achievement.points,
                                type = achievement.type,
                                completedDate = achievement.completedDate,
                                docId = achievement.docId
                            )
                            achievementsContainer.addView(achievementCard)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AchievementsActivity", "Error loading achievements", e)
                withContext(Dispatchers.Main) {
                    emptyView.visibility = View.VISIBLE
                    emptyView.text = "Failed to load achievements: ${e.message}"
                }
            }
        }
    }

    private fun createAchievementCard(
        challengeName: String,
        constraint: Int,
        progress: Int,
        points: Int,
        type: String,
        completedDate: String,
        docId: String
    ): CardView {
        // Make sure this layout file exists in your project
        val cardView = layoutInflater.inflate(
            R.layout.item_achievement_card,
            achievementsContainer,
            false
        ) as CardView

        val achievementNameTextView = cardView.findViewById<TextView>(R.id.achievementNameTextView)
        val progressBar = cardView.findViewById<ProgressBar>(R.id.achievementProgressBar)
        val progressTextView = cardView.findViewById<TextView>(R.id.progressTextView)
        val pointsTextView = cardView.findViewById<TextView>(R.id.pointsTextView)
        val completedTextView = cardView.findViewById<TextView>(R.id.completedTextView)
        val badgeIcon = cardView.findViewById<ImageView>(R.id.badgeIcon)

        // Set achievement information
        achievementNameTextView.text = challengeName
        pointsTextView.text = "$points pts"

        // Format completion date for display
        val formattedDate = formatCompletionDate(completedDate)
        completedTextView.text = "Completed: $formattedDate"

        // Set progress (should be 100% for achievements)
        progressBar.progress = 100
        progressTextView.text = "$constraint/$constraint"

        // Set badge icon based on type
        when (type.lowercase()) {
            "like" -> {
                badgeIcon.setImageResource(R.drawable.ic_like_badge)
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.like_challenge_bg))
            }
            "followers" -> {
                badgeIcon.setImageResource(R.drawable.ic_follower_badge)
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.follower_challenge_bg))
            }
            "comments" -> {
                badgeIcon.setImageResource(R.drawable.ic_comment_badge)
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.comment_challenge_bg))
            }
            "following" -> {
                badgeIcon.setImageResource(R.drawable.ic_following_badge)
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.following_challenge_bg))
            }
            else -> {
                badgeIcon.setImageResource(R.drawable.ic_achievement_badge)
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.default_challenge_bg))
            }
        }

        return cardView
    }

    // Data class to represent achievement information
    private data class AchievementData(
        val uniqueKey: String,  // Unique identifier to detect duplicates
        val name: String,
        val constraint: Int,
        val progress: Int,
        val points: Int,
        val type: String,
        val completedDate: String,
        val docId: String
    )

    private fun formatDate(date: Date): String {
        val outputFormat = SimpleDateFormat("MMMM d, yyyy 'at' hh:mm:ss a z", Locale.US)
        outputFormat.timeZone = TimeZone.getTimeZone("UTC")
        return outputFormat.format(date)
    }

    private fun formatCompletionDate(dateString: String): String {
        return try {
            val date = parseDate(dateString)
            if (date != null) {
                val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
                return outputFormat.format(date)
            }
            // If parseDate returns null, try to extract date info from the original string
            val parts = dateString.split(" at ")
            if (parts.isNotEmpty()) {
                return parts[0]
            }
            dateString // Return original if all else fails
        } catch (e: Exception) {
            Log.e("AchievementsActivity", "Error formatting date: $dateString", e)
            dateString // Return original string on error
        }
    }

    private fun parseDate(dateString: String): Date? {
        return try {
            // First try to parse as formatted date string
            val format = SimpleDateFormat("MMMM d, yyyy 'at' hh:mm:ss a z", Locale.US)
            format.parse(dateString)
        } catch (e: Exception) {
            try {
                // If that fails, try parsing as UTC timestamp
                val timestamp = dateString.toLong()
                Date(timestamp)
            } catch (e: Exception) {
                try {
                    // Try another common format matching your example
                    val format = SimpleDateFormat("MMM d, yyyy 'at' HH:mm:ss 'UTC'Z", Locale.US)
                    format.parse(dateString)
                } catch (e: Exception) {
                    Log.e("AchievementsActivity", "Error parsing date: $dateString", e)
                    null
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Set the proper tab selection state when returning to this activity
        challengesTab.isSelected = false
        achievementsTab.isSelected = true
        challengesTab.setBackgroundResource(R.drawable.tab_unselected_background)
        achievementsTab.setBackgroundResource(R.drawable.tab_selected_background)

        // Reload achievements in case data changed while away
        loadAchievements()
    }

    // Override the back button behavior
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}