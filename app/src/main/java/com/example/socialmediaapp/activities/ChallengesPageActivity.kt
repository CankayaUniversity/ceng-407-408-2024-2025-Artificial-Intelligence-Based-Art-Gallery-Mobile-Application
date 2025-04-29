package com.example.socialmediaapp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.socialmediaapp.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ChallengesPageActivity : BaseActivity() {
    override fun getContentLayoutId(): Int {
        return R.layout.activity_challenges_page
    }

    private lateinit var challengesContainer: LinearLayout
    private lateinit var emptyView: TextView
    private lateinit var achievementsButton: TextView

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private var challengesListener: ListenerRegistration? = null
    private var userDataListener: ListenerRegistration? = null
    private var postsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToolbarTitle("Challenges")

        initializeViews()
        setupButtonListeners()
        setupUserDataListener()
        setupPostsListener()
        setupChallengesListener()
        loadChallenges()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup listeners when activity is destroyed
        challengesListener?.remove()
        userDataListener?.remove()
        postsListener?.remove()
    }

    private fun initializeViews() {
        challengesContainer = findViewById(R.id.challengesContainer)
        emptyView = findViewById(R.id.emptyView)
        achievementsButton = findViewById(R.id.achievementsButton)
    }

    private fun setupButtonListeners() {
        achievementsButton.setOnClickListener {
            val intent = Intent(this, AchievementsPageActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun setupChallengesListener() {
        val currentUser = auth.currentUser ?: return

        // Remove any existing listener
        challengesListener?.remove()

        // Setup real-time listener for challenge updates
        challengesListener = firestore.collection("UserChallanges")
            .whereEqualTo("userid", currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChallengesActivity", "Error listening for challenge updates", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    loadChallenges()
                }
            }
    }

    // Monitor all user data changes and update related challenges
    private fun setupUserDataListener() {
        val currentUser = auth.currentUser ?: return

        // Remove any existing listener
        userDataListener?.remove()

        // Setup real-time listener for user data updates
        userDataListener = firestore.collection("Users")
            .document(currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChallengesActivity", "Error listening for user data updates", error)
                    return@addSnapshotListener
                }

                snapshot?.let { document ->
                    // Update challenges based on user data
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            // Get user stats
                            val followers = document.getLong("followers")?.toInt() ?: 0
                            val following = document.getLong("following")?.toInt() ?: 0

                            // Update follower and following related challenges
                            updateChallengeType("Followers", followers)
                            updateChallengeType("Following", following)

                        } catch (e: Exception) {
                            Log.e("ChallengesActivity", "Error updating challenges from user data", e)
                        }
                    }
                }
            }
    }

    // Monitor posts for like and comment counts
    private fun setupPostsListener() {
        val currentUser = auth.currentUser ?: return

        // Remove any existing listener
        postsListener?.remove()

        // Setup real-time listener for post updates
        postsListener = firestore.collection("Posts")
            .whereEqualTo("userid", currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChallengesActivity", "Error listening for posts updates", error)
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    // Calculate total likes and comments from all posts
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            var totalLikes = 0
                            var totalComments = 0

                            for (document in querySnapshot.documents) {
                                totalLikes += document.getLong("likes")?.toInt() ?: 0
                                totalComments += document.getLong("comments")?.toInt() ?: 0
                            }

                            // Update like and comment related challenges
                            updateChallengeType("Like", totalLikes)
                            updateChallengeType("Comments", totalComments)

                            // Check if any challenges were completed and award points
                            updateUserPointsForCompletedChallenges()

                        } catch (e: Exception) {
                            Log.e("ChallengesActivity", "Error processing posts data", e)
                        }
                    }
                }
            }
    }

    // Update challenges of a specific type with the current value
    private suspend fun updateChallengeType(type: String, currentValue: Int) {
        val currentUser = auth.currentUser ?: return

        try {
            // Query for challenges of the specified type
            val challengesQuery = firestore.collection("UserChallanges")
                .whereEqualTo("userid", currentUser.uid)
                .whereEqualTo("type", type)
                .get()
                .await()

            Log.d("ChallengesActivity", "Updating $type challenges with value $currentValue")

            for (document in challengesQuery.documents) {
                Log.d("ChallengesActivity", "Challenge document: ${document.id}, type: $type")

                // First check if the field is "process" (misspelled in the database)
                if (document.contains("process")) {
                    Log.d("ChallengesActivity", "Updating 'process' field for challenge ${document.id}")
                    firestore.collection("UserChallanges")
                        .document(document.id)
                        .update("process", currentValue)
                        .await()
                }
                // Then check if the field is "progress" (correct spelling)
                else if (document.contains("progress")) {
                    Log.d("ChallengesActivity", "Updating 'progress' field for challenge ${document.id}")
                    firestore.collection("UserChallanges")
                        .document(document.id)
                        .update("progress", currentValue)
                        .await()
                }
                else {
                    Log.e("ChallengesActivity", "Challenge ${document.id} doesn't have process or progress field")
                }
            }
        } catch (e: Exception) {
            Log.e("ChallengesActivity", "Error updating challenge type: $type", e)
        }
    }

    private fun loadChallenges() {
        val currentUser = auth.currentUser ?: return
        challengesContainer.removeAllViews()
        emptyView.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentDate = Date() // Define current date for comparison

                val challengesQuery = firestore.collection("UserChallanges")
                    .whereEqualTo("userid", currentUser.uid)
                    .get()
                    .await()

                // Debug logging
                Log.d("ChallengesActivity", "Found ${challengesQuery.documents.size} total challenges")

                // Filter for active challenges (not expired and not completed)
                val validChallenges = challengesQuery.documents.filter { doc ->
                    val expirationValue = doc.get("expiration")
                    val completed = doc.getBoolean("completed") ?: false

                    if (expirationValue != null && !completed) {
                        val expirationDate = when (expirationValue) {
                            is String -> parseDate(expirationValue)
                            is Timestamp -> expirationValue.toDate()
                            is Long -> Date(expirationValue)
                            else -> null
                        }
                        expirationDate != null && expirationDate.after(currentDate)
                    } else {
                        false
                    }
                }

                Log.d("ChallengesActivity", "Found ${validChallenges.size} active challenges")

                withContext(Dispatchers.Main) {
                    if (validChallenges.isEmpty()) {
                        emptyView.visibility = View.VISIBLE
                        emptyView.text = "No active challenges found"
                    } else {
                        for (doc in validChallenges) {
                            // Explicitly check for "challangeName" first (the spelling in your database)
                            val challengeName = doc.getString("challangeName") ?:
                            doc.getString("challengeName") ?:
                            "Unknown Challenge"

                            val constraint = doc.getLong("constraints")?.toInt() ?: 0

                            // Explicitly check for "process" first (the spelling in your database)
                            val progress = doc.getLong("process")?.toInt() ?:
                            doc.getLong("progress")?.toInt() ?: 0

                            val points = doc.getLong("points")?.toInt() ?: 0
                            val type = doc.getString("type") ?: ""

                            // Handle expiration properly
                            val expirationValue = doc.get("expiration")
                            val expirationStr = when (expirationValue) {
                                is Timestamp -> formatDate(expirationValue.toDate())
                                is String -> expirationValue
                                is Long -> formatDate(Date(expirationValue))
                                else -> "Unknown"
                            }

                            val challengeCard = createChallengeCard(
                                challengeName,
                                constraint,
                                progress,
                                points,
                                type,
                                expirationStr,
                                doc.id // Pass document ID for potential updates
                            )
                            challengesContainer.addView(challengeCard)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChallengesActivity", "Error loading challenges", e)
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
        expiration: String,
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
        val statusIcon = cardView.findViewById<ImageView>(R.id.statusIcon)

        // Set challenge information
        challengeNameTextView.text = challengeName
        pointsTextView.text = "$points pts"

        // Format expiration date for display
        val formattedExpiration = formatExpirationDate(expiration)
        expirationTextView.text = "Expires: $formattedExpiration"

        // Set progress
        val progressPercentage = if (constraint > 0) (progress * 100) / constraint else 0
        progressBar.progress = progressPercentage
        progressTextView.text = "$progress/$constraint"

        // Set status icon
        if (progress >= constraint) {
            statusIcon.setImageResource(R.drawable.ic_check_circle)
            statusIcon.visibility = View.VISIBLE
        } else {
            statusIcon.visibility = View.INVISIBLE
        }

        // Set card background tint based on type
        when (type.lowercase()) {
            "like" -> {
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.like_challenge_bg))
            }
            "followers" -> {
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.follower_challenge_bg))
            }
            "comments" -> {
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.comment_challenge_bg))
            }
            "following" -> {
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.following_challenge_bg))
            }
            else -> {
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.default_challenge_bg))
            }
        }

        return cardView
    }

    // Check if user has completed any challenges and update points accordingly
    private suspend fun updateUserPointsForCompletedChallenges() {
        val currentUser = auth.currentUser ?: return

        try {
            val challengesQuery = firestore.collection("UserChallanges")
                .whereEqualTo("userid", currentUser.uid)
                .get()
                .await()

            var totalPointsEarned = 0

            // Find newly completed challenges (where progress >= constraint)
            for (doc in challengesQuery.documents) {
                val constraint = doc.getLong("constraints")?.toInt() ?: 0

                val progress = doc.getLong("process")?.toInt() ?:
                doc.getLong("progress")?.toInt() ?: 0

                val points = doc.getLong("points")?.toInt() ?: 0
                val completed = doc.getBoolean("completed") ?: false

                // If challenge is complete but not marked as completed yet
                if (progress >= constraint && !completed) {
                    Log.d("ChallengesActivity", "Challenge completed: ${doc.id}, points: $points")

                    // Mark challenge as completed
                    firestore.collection("UserChallanges")
                        .document(doc.id)
                        .update("completed", true)
                        .await()

                    // Create achievement in UserAchievements collection
                    val challengeData = doc.data
                    if (challengeData != null) {
                        // Create new achievement document
                        val achievementData = hashMapOf(
                            "userid" to currentUser.uid,
                            "challengeId" to doc.id,
                            "challengeName" to (challengeData["challangeName"] ?: challengeData["challengeName"] ?: "Unknown Challenge"),
                            "type" to (challengeData["type"] ?: ""),
                            "points" to points,
                            "completedAt" to Timestamp.now(),
                            "constraints" to constraint,
                            "progress" to progress
                        )

                        // Add to UserAchievements collection
                        firestore.collection("UserAchievements")
                            .add(achievementData)
                            .await()

                        Log.d("ChallengesActivity", "Achievement created in UserAchievements collection")
                    }

                    // Add points to total
                    totalPointsEarned += points
                }
            }

            // If points were earned, update user's points
            if (totalPointsEarned > 0) {
                val userDoc = firestore.collection("Users")
                    .document(currentUser.uid)
                    .get()
                    .await()

                val currentPoints = userDoc.getLong("points")?.toInt() ?: 0
                val newPoints = currentPoints + totalPointsEarned

                Log.d("ChallengesActivity", "Updating user points: $currentPoints → $newPoints")

                firestore.collection("Users")
                    .document(currentUser.uid)
                    .update("points", newPoints)
                    .await()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ChallengesPageActivity,
                        "Congratulations! You earned $totalPointsEarned points!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e("ChallengesActivity", "Error updating user points", e)
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
                    Log.e("ChallengesActivity", "Error parsing date: $dateString", e)
                    null
                }
            }
        }
    }

    private fun formatDate(date: Date): String {
        val outputFormat = SimpleDateFormat("MMMM d, yyyy 'at' hh:mm:ss a z", Locale.US)
        outputFormat.timeZone = TimeZone.getTimeZone("UTC")
        return outputFormat.format(date)
    }

    private fun formatExpirationDate(dateString: String): String {
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
            Log.e("ChallengesActivity", "Error formatting date: $dateString", e)
            dateString // Return original string on error
        }
    }

    override fun onResume() {
        super.onResume()

        // Check for updates when returning to this activity
        lifecycleScope.launch(Dispatchers.IO) {
            updateUserPointsForCompletedChallenges()
        }

        // Reload challenges in case data changed while away
        loadChallenges()
    }

    // Override the back button behavior
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}