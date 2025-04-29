package com.example.socialmediaapp.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
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

class AchievementsPageActivity : BaseActivity() {
    override fun getContentLayoutId(): Int {
        return R.layout.activity_achievements
    }

    private lateinit var achievementsContainer: LinearLayout
    private lateinit var emptyView: TextView

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToolbarTitle("Achievements")

        initializeViews()
        loadAchievements()
    }

    private fun initializeViews() {
        achievementsContainer = findViewById(R.id.achievementsContainer)
        emptyView = findViewById(R.id.emptyView)
    }

    private fun loadAchievements() {
        val currentUser = auth.currentUser ?: return
        achievementsContainer.removeAllViews()
        emptyView.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // First try to get achievements from the new UserAchievements collection
                val achievementsQuery = firestore.collection("UserAchievements")
                    .whereEqualTo("userid", currentUser.uid)
                    .orderBy("completedAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                if (!achievementsQuery.isEmpty) {
                    // Use the new UserAchievements collection
                    withContext(Dispatchers.Main) {
                        if (achievementsQuery.documents.isEmpty()) {
                            emptyView.visibility = View.VISIBLE
                            emptyView.text = "No achievements found"
                        } else {
                            for (doc in achievementsQuery.documents) {
                                val challengeName = doc.getString("achievementName") ?: "Unknown Achievement"
                                val constraint = doc.getLong("constraints")?.toInt() ?: 0
                                val progress = doc.getLong("progress")?.toInt() ?: 0
                                val points = doc.getLong("points")?.toInt() ?: 0
                                val type = doc.getString("type") ?: ""
                                val completedAt = doc.getTimestamp("completedAt")

                                // Format completed date for display
                                val completedDateStr = if (completedAt != null) {
                                    formatDate(completedAt.toDate())
                                } else {
                                    "Unknown date"
                                }

                                val achievementCard = createAchievementCard(
                                    challengeName,
                                    constraint,
                                    progress,
                                    points,
                                    type,
                                    completedDateStr,
                                    doc.id
                                )
                                achievementsContainer.addView(achievementCard)
                            }
                        }
                    }
                } else {
                    // Fall back to the old approach (filtering completed challenges from UserChallanges)
                    val challengesQuery = firestore.collection("UserChallanges")
                        .whereEqualTo("userid", currentUser.uid)
                        .whereEqualTo("completed", true)
                        .get()
                        .await()

                    withContext(Dispatchers.Main) {
                        if (challengesQuery.documents.isEmpty()) {
                            emptyView.visibility = View.VISIBLE
                            emptyView.text = "No achievements found"
                        } else {
                            for (doc in challengesQuery.documents) {
                                val challengeName = doc.getString("challangeName") ?:
                                doc.getString("challengeName") ?:
                                "Unknown Challenge"

                                val constraint = doc.getLong("constraints")?.toInt() ?: 0
                                val progress = doc.getLong("process")?.toInt() ?:
                                doc.getLong("progress")?.toInt() ?: 0
                                val points = doc.getLong("points")?.toInt() ?: 0
                                val type = doc.getString("type") ?: ""

                                // Use expiration as a placeholder since we don't have completedAt
                                val expirationValue = doc.get("expiration")
                                val dateStr = when (expirationValue) {
                                    is Timestamp -> formatDate(expirationValue.toDate())
                                    is String -> expirationValue
                                    is Long -> formatDate(Date(expirationValue))
                                    else -> "Unknown"
                                }

                                val achievementCard = createAchievementCard(
                                    challengeName,
                                    constraint,
                                    progress,
                                    points,
                                    type,
                                    dateStr,
                                    doc.id
                                )
                                achievementsContainer.addView(achievementCard)
                            }
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

    // Override the back button behavior
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}