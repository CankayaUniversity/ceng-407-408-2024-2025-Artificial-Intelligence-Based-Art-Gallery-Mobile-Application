package com.example.socialmediaapp.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class ChallengesPageActivity : BaseActivity() {
    override fun getContentLayoutId(): Int {
        return R.layout.activity_challenges_page
    }

    private lateinit var challengesContainer: LinearLayout
    private lateinit var emptyView: TextView
    private lateinit var challengesTab: TextView
    private lateinit var achievementsTab: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_NAME = "ThemePrefs"
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Set of document IDs to avoid duplication
    private val loadedChallengeIds = HashSet<String>()
    private val TAG = "ChallengesPageActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToolbarTitle("Challenges")

        initializeViews()
        setupTabListeners()
        loadChallenges()

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val mainLayout = findViewById<ConstraintLayout>(R.id.challenges)

        if (sharedPreferences.getBoolean(PREF_NAME, false)) {
            mainLayout.setBackgroundColor("#3F51B5".toColorInt())
        }

    }

    private fun initializeViews() {
        challengesContainer = findViewById(R.id.challengesContainer)
        emptyView = findViewById(R.id.emptyView)
        challengesTab = findViewById(R.id.challengesTab)
        achievementsTab = findViewById(R.id.achievementsTab)

        // Set challenges tab as selected initially
        challengesTab.isSelected = true
        achievementsTab.isSelected = false
        challengesTab.setBackgroundResource(R.drawable.tab_selected_background)
        achievementsTab.setBackgroundResource(R.drawable.tab_unselected_background)
    }

    private fun setupTabListeners() {
        challengesTab.setOnClickListener {
            // Already on challenges tab, do nothing
            if (!challengesTab.isSelected) {
                challengesTab.isSelected = true
                achievementsTab.isSelected = false
                challengesTab.setBackgroundResource(R.drawable.tab_selected_background)
                achievementsTab.setBackgroundResource(R.drawable.tab_unselected_background)
            }
        }

        achievementsTab.setOnClickListener {
            // Navigate to achievements
            val intent = Intent(this, AchievementsPageActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun loadChallenges() {
        val currentUser = auth.currentUser ?: return
        challengesContainer.removeAllViews()
        loadedChallengeIds.clear()
        emptyView.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Debug the current user ID
                Log.d(TAG, "Current user ID: ${currentUser.uid}")

                // Query user challenges from Firebase - note the collection name fix
                val challengesQuery = firestore.collection("UserChallanges") // Fixed collection name
                    .whereEqualTo("userid", currentUser.uid)
                    .get()
                    .await()

                Log.d(TAG, "Query returned ${challengesQuery.documents.size} documents")

                withContext(Dispatchers.Main) {
                    if (challengesQuery.documents.isEmpty()) {
                        emptyView.visibility = View.VISIBLE
                        Log.d(TAG, "No challenges found")
                    } else {
                        var hasValidChallenges = false

                        for (doc in challengesQuery.documents) {
                            val docId = doc.id
                            Log.d(TAG, "Processing document: $docId")

                            // Skip if we've already processed this challenge
                            if (loadedChallengeIds.contains(docId)) {
                                continue
                            }

                            // Check if challenge is marked as completed
                            val completed = doc.getBoolean("completed") ?: false
                            if (completed) {
                                Log.d(TAG, "Challenge $docId is completed, skipping")
                                continue
                            }

                            // Get challenge data with field name fallbacks
                            val challengeName = doc.getString("challangeName") ?:
                            doc.getString("challengeName") ?:
                            "Unknown Challenge"

                            // Handle constraints/constraint field
                            val constraint = doc.getLong("constraints")?.toInt() ?:
                            doc.getLong("constraint")?.toInt() ?: 0

                            // Handle process/progress field
                            val progress = doc.getLong("process")?.toInt() ?:
                            doc.getLong("progress")?.toInt() ?: 0

                            val points = doc.getLong("points")?.toInt() ?: 0

                            val type = doc.getString("type") ?: ""

                            // Get and validate expiration date
                            val expirationValue = doc.get("expiration")
                            var expirationDate: Date? = null

                            when (expirationValue) {
                                is Timestamp -> expirationDate = expirationValue.toDate()
                                is String -> {
                                    try {
                                        val format = SimpleDateFormat("MMMM d, yyyy 'at' HH:mm:ss 'UTC'Z", Locale.US)
                                        expirationDate = format.parse(expirationValue)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error parsing date string: $expirationValue", e)
                                        try {
                                            // Try alternative date format
                                            val format = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm:ss'AM' 'UTC'Z", Locale.US)
                                            expirationDate = format.parse(expirationValue.toString())
                                        } catch (e2: Exception) {
                                            Log.e(TAG, "Failed second attempt to parse date: $expirationValue", e2)
                                        }
                                    }
                                }
                                is Long -> expirationDate = Date(expirationValue)
                            }

                            Log.d(TAG, "Challenge details: name=$challengeName, constraint=$constraint, progress=$progress, points=$points, type=$type")
                            Log.d(TAG, "Expiration: $expirationValue parsed to $expirationDate")

                            // Skip expired challenges
                            if (expirationDate != null && expirationDate.before(Date())) {
                                Log.d(TAG, "Challenge $docId is expired, skipping")
                                continue
                            }

                            // Format expiration date for display
                            val expirationText = if (expirationDate != null) {
                                formatDateForDisplay(expirationDate)
                            } else {
                                "No expiration"
                            }

                            // Create challenge card
                            val challengeCard = createChallengeCard(
                                challengeName,
                                constraint,
                                progress,
                                points,
                                type,
                                expirationText,
                                docId
                            )

                            challengesContainer.addView(challengeCard)
                            loadedChallengeIds.add(docId)
                            hasValidChallenges = true
                            Log.d(TAG, "Added challenge card for: $challengeName")
                        }

                        // Show empty view if no valid challenges were found
                        if (!hasValidChallenges) {
                            Log.d(TAG, "No valid challenges found after processing")
                            emptyView.visibility = View.VISIBLE
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading challenges", e)
                withContext(Dispatchers.Main) {
                    emptyView.visibility = View.VISIBLE
                    emptyView.text = "Failed to load challenges: ${e.message}"
                }
            }
        }
    }

    private fun createChallengeCard(
        challengeName: String,
        constraint: Int,
        progress: Int,
        points: Int,
        type: String,
        expirationText: String,
        docId: String
    ): CardView {
        val cardView = layoutInflater.inflate(
            R.layout.item_challenge_card,
            challengesContainer,
            false
        ) as CardView

        val challengeNameTextView = cardView.findViewById<TextView>(R.id.challengeNameTextView)
        val progressBar = cardView.findViewById<ProgressBar>(R.id.challengeProgressBar)
        val progressTextView = cardView.findViewById<TextView>(R.id.progressTextView)
        val pointsTextView = cardView.findViewById<TextView>(R.id.pointsTextView)
        val expirationTextView = cardView.findViewById<TextView>(R.id.expirationTextView)
        val badgeIcon = cardView.findViewById<ImageView>(R.id.badgeIcon)

        // Set challenge information
        challengeNameTextView.text = challengeName
        pointsTextView.text = "$points pts"
        expirationTextView.text = "Expires: $expirationText"

        // Calculate and set progress
        val progressPercentage = if (constraint > 0) {
            (progress.toFloat() / constraint.toFloat() * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
        progressBar.progress = progressPercentage
        progressTextView.text = "$progress/$constraint"

        // Set badge icon based on type
        when (type.lowercase()) {
            "like" -> {
                badgeIcon.setImageResource(R.drawable.ic_like_badge)
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.like_challenge_bg))
            }
            "comments" -> {
                badgeIcon.setImageResource(R.drawable.ic_comment_badge)
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.comment_challenge_bg))
            }
            "followers" -> {
                badgeIcon.setImageResource(R.drawable.ic_follower_badge)
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.follower_challenge_bg))
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

    private fun formatDateForDisplay(date: Date): String {
        val now = Date()
        val diffInMillis = date.time - now.time
        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

        return when {
            diffInDays <= 0 -> "Today"
            diffInDays == 1L -> "Tomorrow"
            diffInDays < 7 -> "$diffInDays days"
            else -> {
                val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
                outputFormat.format(date)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Set the proper tab selection state when returning to this activity
        challengesTab.isSelected = true
        achievementsTab.isSelected = false
        challengesTab.setBackgroundResource(R.drawable.tab_selected_background)
        achievementsTab.setBackgroundResource(R.drawable.tab_unselected_background)

        // Reload challenges in case data changed while away
        loadChallenges()
    }

    // Override the back button behavior
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}