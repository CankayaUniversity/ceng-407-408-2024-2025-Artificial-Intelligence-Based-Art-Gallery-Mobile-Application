package com.example.socialmediaapp.activities

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.socialmediaapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ChallengesActivity : BaseActivity() {
    private lateinit var challengesTab: TextView
    private lateinit var achievementsTab: TextView
    private lateinit var challengeOfTheDayTitle: TextView
    private lateinit var challengesOfTheWeekTitle: TextView

    private lateinit var dailyChallenge: ConstraintLayout
    private lateinit var weeklyChallenge1: ConstraintLayout
    private lateinit var weeklyChallenge2: ConstraintLayout
    private lateinit var weeklyChallenge3: ConstraintLayout

    private lateinit var dailyChallengeCheckmark: ImageView
    private lateinit var weeklyChallenge1Checkmark: ImageView
    private lateinit var weeklyChallenge2Checkmark: ImageView
    private lateinit var weeklyChallenge3Checkmark: ImageView

    private lateinit var dailyChallengeText: TextView
    private lateinit var weeklyChallenge1Text: TextView
    private lateinit var weeklyChallenge2Text: TextView
    private lateinit var weeklyChallenge3Text: TextView

    // Store challenge IDs for click handling
    private var dailyChallengeId: String = ""
    private var weeklyChallenge1Id: String = ""
    private var weeklyChallenge2Id: String = ""
    private var weeklyChallenge3Id: String = ""

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun getContentLayoutId(): Int {
        return R.layout.activity_challenges
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set toolbar title
        setToolbarTitle("Challenges")

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        initializeViews()

        // Set up tab navigation
        setupTabNavigation()

        // Set up challenge click listeners
        setupChallengeClickListeners()

        // Load challenges from Firebase
        loadChallenges()
    }

    private fun initializeViews() {
        // Tab views
        challengesTab = findViewById(R.id.challenges_tab)
        achievementsTab = findViewById(R.id.achievements_tab)

        // Title views
        challengeOfTheDayTitle = findViewById(R.id.challenge_of_the_day_title)
        challengesOfTheWeekTitle = findViewById(R.id.challenges_of_the_week_title)

        // Challenge containers
        dailyChallenge = findViewById(R.id.daily_challenge_container)
        weeklyChallenge1 = findViewById(R.id.weekly_challenge_1_container)
        weeklyChallenge2 = findViewById(R.id.weekly_challenge_2_container)
        weeklyChallenge3 = findViewById(R.id.weekly_challenge_3_container)

        // Checkmark images
        dailyChallengeCheckmark = findViewById(R.id.daily_challenge_checkmark)
        weeklyChallenge1Checkmark = findViewById(R.id.weekly_challenge_1_checkmark)
        weeklyChallenge2Checkmark = findViewById(R.id.weekly_challenge_2_checkmark)
        weeklyChallenge3Checkmark = findViewById(R.id.weekly_challenge_3_checkmark)

        // Challenge text views
        dailyChallengeText = findViewById(R.id.daily_challenge_text)
        weeklyChallenge1Text = findViewById(R.id.weekly_challenge_1_text)
        weeklyChallenge2Text = findViewById(R.id.weekly_challenge_2_text)
        weeklyChallenge3Text = findViewById(R.id.weekly_challenge_3_text)
    }

    private fun setupTabNavigation() {
        // Set challenges tab as active initially
        challengesTab.setBackgroundResource(R.drawable.tab_selected_background)
        achievementsTab.setBackgroundResource(android.R.color.transparent)

        // Set up tab click listeners
        challengesTab.setOnClickListener {
            // Already on challenges tab, do nothing
        }

        achievementsTab.setOnClickListener {
            // Navigate to achievements activity
            startActivity(android.content.Intent(this, AchievementsActivity::class.java))
            finish()
        }
    }

    private fun setupChallengeClickListeners() {
        // Set click listeners for each challenge container to mark challenges as completed
        dailyChallenge.setOnClickListener {
            if (dailyChallengeId.isNotEmpty()) {
                markChallengeAsCompleted(dailyChallengeId)
            }
        }

        weeklyChallenge1.setOnClickListener {
            if (weeklyChallenge1Id.isNotEmpty()) {
                markChallengeAsCompleted(weeklyChallenge1Id)
            }
        }

        weeklyChallenge2.setOnClickListener {
            if (weeklyChallenge2Id.isNotEmpty()) {
                markChallengeAsCompleted(weeklyChallenge2Id)
            }
        }

        weeklyChallenge3.setOnClickListener {
            if (weeklyChallenge3Id.isNotEmpty()) {
                markChallengeAsCompleted(weeklyChallenge3Id)
            }
        }
    }

    private fun loadChallenges() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            // Handle not logged in state
            Toast.makeText(this, "Please log in to view challenges", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading state
        setLoadingState(true)

        // Get current date for daily challenge
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dailyId = "daily_$currentDate"
        dailyChallengeId = dailyId

        // Get current week number and year for weekly challenges
        val calendar = Calendar.getInstance()
        val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)

        // Load daily challenge
        loadDailyChallenge(userId, dailyId)

        // Load weekly challenges
        loadWeeklyChallenges(userId, currentWeek, currentYear)
    }

    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            // Show loading text in challenge text views
            dailyChallengeText.text = "Loading daily challenge..."
            weeklyChallenge1Text.text = "Loading weekly challenge 1..."
            weeklyChallenge2Text.text = "Loading weekly challenge 2..."
            weeklyChallenge3Text.text = "Loading weekly challenge 3..."

            // Hide checkmarks during loading
            dailyChallengeCheckmark.visibility = View.INVISIBLE
            weeklyChallenge1Checkmark.visibility = View.INVISIBLE
            weeklyChallenge2Checkmark.visibility = View.INVISIBLE
            weeklyChallenge3Checkmark.visibility = View.INVISIBLE
        } else {
            // Make checkmarks visible after loading
            dailyChallengeCheckmark.visibility = View.VISIBLE
            weeklyChallenge1Checkmark.visibility = View.VISIBLE
            weeklyChallenge2Checkmark.visibility = View.VISIBLE
            weeklyChallenge3Checkmark.visibility = View.VISIBLE
        }
    }

    private fun loadDailyChallenge(userId: String, dailyId: String) {
        firestore.collection("Challenges")
            .document(dailyId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val challenge = document.getString("challenge")
                        ?: "Write a prompt with 'Harry Potter' in it and share artwork"
                    val points = document.getLong("points") ?: 50

                    // Update UI with challenge details
                    dailyChallengeText.text = challenge

                    // Check if user completed this challenge
                    checkChallengeCompletion(userId, dailyId, dailyChallengeCheckmark)
                } else {
                    // Create default daily challenge if not found
                    createDefaultDailyChallenge(dailyId)
                    dailyChallengeText.text =
                        "Write a prompt with 'Harry Potter' in it and share artwork"
                    dailyChallengeCheckmark.setImageResource(R.drawable.ic_unchecked_circle)
                    dailyChallengeCheckmark.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                // Handle error
                dailyChallengeText.text = "Failed to load daily challenge"
                dailyChallengeCheckmark.setImageResource(R.drawable.ic_unchecked_circle)
                dailyChallengeCheckmark.visibility = View.VISIBLE
            }
    }

    private fun createDefaultDailyChallenge(dailyId: String) {
        val challengeData = hashMapOf(
            "challenge" to "Write a prompt with 'Harry Potter' in it and share artwork",
            "points" to 50,
            "type" to "daily",
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("Challenges")
            .document(dailyId)
            .set(challengeData)
    }

    private fun loadWeeklyChallenges(userId: String, week: Int, year: Int) {
        // Construct weekly challenge IDs
        weeklyChallenge1Id = "weekly_${year}_${week}_1"
        weeklyChallenge2Id = "weekly_${year}_${week}_2"
        weeklyChallenge3Id = "weekly_${year}_${week}_3"

        // Query for weekly challenges
        firestore.collection("Challenges")
            .whereEqualTo("type", "weekly")
            .whereEqualTo("week", week)
            .whereEqualTo("year", year)
            .get()
            .addOnSuccessListener { documents ->
                val challenges = mutableListOf<Map<String, Any>>()

                for (document in documents) {
                    val data = document.data
                    data["id"] = document.id
                    challenges.add(data)
                }

                // If no weekly challenges found, create default ones
                if (challenges.isEmpty()) {
                    createDefaultWeeklyChallenges(week, year)

                    // Set default UI
                    weeklyChallenge1Text.text = "Write 5 prompts with 'dragons' in them"
                    weeklyChallenge2Text.text = "Follow 3 users"
                    weeklyChallenge3Text.text = "Like 5 artworks"

                    // Check completions for default challenges
                    checkChallengeCompletion(userId, weeklyChallenge1Id, weeklyChallenge1Checkmark)
                    checkChallengeCompletion(userId, weeklyChallenge2Id, weeklyChallenge2Checkmark)
                    checkChallengeCompletion(userId, weeklyChallenge3Id, weeklyChallenge3Checkmark)
                } else {
                    // Sort challenges by challenge_number
                    challenges.sortBy { it["challenge_number"] as? Long ?: 0 }

                    // Display up to 3 weekly challenges
                    displayWeeklyChallenge(
                        challenges,
                        0,
                        userId,
                        weeklyChallenge1Text,
                        weeklyChallenge1Checkmark,
                        weeklyChallenge1Id
                    )
                    displayWeeklyChallenge(
                        challenges,
                        1,
                        userId,
                        weeklyChallenge2Text,
                        weeklyChallenge2Checkmark,
                        weeklyChallenge2Id
                    )
                    displayWeeklyChallenge(
                        challenges,
                        2,
                        userId,
                        weeklyChallenge3Text,
                        weeklyChallenge3Checkmark,
                        weeklyChallenge3Id
                    )
                }

                // Done loading
                setLoadingState(false)
            }
            .addOnFailureListener {
                // Handle error - load default challenges
                createDefaultWeeklyChallenges(week, year)

                // Set default UI
                weeklyChallenge1Text.text = "Write 5 prompts with 'dragons' in them"
                weeklyChallenge2Text.text = "Follow 3 users"
                weeklyChallenge3Text.text = "Like 5 artworks"

                // Check completions
                checkChallengeCompletion(userId, weeklyChallenge1Id, weeklyChallenge1Checkmark)
                checkChallengeCompletion(userId, weeklyChallenge2Id, weeklyChallenge2Checkmark)
                checkChallengeCompletion(userId, weeklyChallenge3Id, weeklyChallenge3Checkmark)

                // Done loading
                setLoadingState(false)
            }
    }

    private fun displayWeeklyChallenge(
        challenges: List<Map<String, Any>>,
        index: Int,
        userId: String,
        textView: TextView,
        checkmarkView: ImageView,
        defaultChallengeId: String
    ) {
        if (index < challenges.size) {
            val challenge = challenges[index]
            textView.text = challenge["challenge"] as? String ?: "Challenge not available"
            val challengeId = challenge["id"] as String

            // Store the challenge ID in appropriate variable for click handling
            when (index) {
                0 -> weeklyChallenge1Id = challengeId
                1 -> weeklyChallenge2Id = challengeId
                2 -> weeklyChallenge3Id = challengeId
            }

            checkChallengeCompletion(userId, challengeId, checkmarkView)
        } else {
            // If challenge not found, use default text
            when (index) {
                0 -> textView.text = "Write 5 prompts with 'dragons' in them"
                1 -> textView.text = "Follow 3 users"
                2 -> textView.text = "Like 5 artworks"
            }
            checkChallengeCompletion(userId, defaultChallengeId, checkmarkView)
        }
    }

    private fun createDefaultWeeklyChallenges(week: Int, year: Int) {
        // Create challenge 1
        val challenge1Data = hashMapOf(
            "challenge" to "Write 5 prompts with 'dragons' in them",
            "points" to 100,
            "type" to "weekly",
            "week" to week,
            "year" to year,
            "challenge_number" to 1,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("Challenges")
            .document(weeklyChallenge1Id)
            .set(challenge1Data)

        // Create challenge 2
        val challenge2Data = hashMapOf(
            "challenge" to "Follow 3 users",
            "points" to 75,
            "type" to "weekly",
            "week" to week,
            "year" to year,
            "challenge_number" to 2,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("Challenges")
            .document(weeklyChallenge2Id)
            .set(challenge2Data)

        // Create challenge 3
        val challenge3Data = hashMapOf(
            "challenge" to "Like 5 artworks",
            "points" to 50,
            "type" to "weekly",
            "week" to week,
            "year" to year,
            "challenge_number" to 3,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("Challenges")
            .document(weeklyChallenge3Id)
            .set(challenge3Data)
    }

    private fun checkChallengeCompletion(
        userId: String,
        challengeId: String,
        checkmarkView: ImageView
    ) {
        firestore.collection("Users")
            .document(userId)
            .collection("CompletedChallenges")
            .document(challengeId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Challenge completed
                    checkmarkView.setImageResource(R.drawable.ic_checked_circle)
                    checkmarkView.visibility = View.VISIBLE
                } else {
                    // Challenge not completed
                    checkmarkView.setImageResource(R.drawable.ic_unchecked_circle)
                    checkmarkView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                // Handle error
                checkmarkView.setImageResource(R.drawable.ic_unchecked_circle)
                checkmarkView.visibility = View.VISIBLE
            }
    }

    // Function to mark a challenge as completed
    fun markChallengeAsCompleted(challengeId: String) {
        val userId = auth.currentUser?.uid ?: return

        // First check if already completed
        firestore.collection("Users")
            .document(userId)
            .collection("CompletedChallenges")
            .document(challengeId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Challenge already completed
                    Toast.makeText(this, "Challenge already completed!", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Get the challenge to record points
                firestore.collection("Challenges")
                    .document(challengeId)
                    .get()
                    .addOnSuccessListener { challengeDoc ->
                        if (challengeDoc != null && challengeDoc.exists()) {
                            val points = challengeDoc.getLong("points") ?: 0
                            val challengeText = challengeDoc.getString("challenge") ?: "Challenge"

                            // Record completed challenge
                            val challengeData = hashMapOf(
                                "completedAt" to com.google.firebase.Timestamp.now(),
                                "challengeId" to challengeId,
                                "points" to points
                            )

                            firestore.collection("Users")
                                .document(userId)
                                .collection("CompletedChallenges")
                                .document(challengeId)
                                .set(challengeData)
                                .addOnSuccessListener {
                                    // Update UI to show completion
                                    updateCompletionUI(challengeId)

                                    // Update user's total points
                                    updateUserPoints(userId, points)

                                    // Check for achievements based on challenges completed
                                    checkChallengesAchievement(userId)

                                    // Show completion toast
                                    Toast.makeText(
                                        this,
                                        "Challenge completed! +$points points",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        this,
                                        "Failed to mark challenge as completed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            Toast.makeText(this, "Challenge not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to find challenge details", Toast.LENGTH_SHORT)
                            .show()
                    }
            }
    }

    private fun updateCompletionUI(challengeId: String) {
        // Update the checkmark icon based on which challenge was completed
        when (challengeId) {
            dailyChallengeId ->
                dailyChallengeCheckmark.setImageResource(R.drawable.ic_checked_circle)

            weeklyChallenge1Id ->
                weeklyChallenge1Checkmark.setImageResource(R.drawable.ic_checked_circle)

            weeklyChallenge2Id ->
                weeklyChallenge2Checkmark.setImageResource(R.drawable.ic_checked_circle)

            weeklyChallenge3Id ->
                weeklyChallenge3Checkmark.setImageResource(R.drawable.ic_checked_circle)
        }
    }

    private fun updateUserPoints(userId: String, points: Long) {
        // Update user's points in Firestore
        firestore.collection("Users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val currentPoints = document.getLong("points") ?: 0
                    val newPoints = currentPoints + points

                    // Update the points field
                    firestore.collection("Users")
                        .document(userId)
                        .update("points", newPoints)
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to update points", Toast.LENGTH_SHORT)
                                .show()
                        }
                } else {
                    // If user document doesn't exist yet, create it
                    val userData = hashMapOf(
                        "points" to points,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )

                    firestore.collection("Users")
                        .document(userId)
                        .set(userData)
                        .addOnFailureListener {
                            Toast.makeText(
                                this,
                                "Failed to create user profile",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to retrieve user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkChallengesAchievement(userId: String) {
        // Query to count completed challenges
        firestore.collection("Users")
            .document(userId)
            .collection("CompletedChallenges")
            .get()
            .addOnSuccessListener { documents ->
                val completedCount = documents.size()

                // Check for achievements based on number of completed challenges
                when {
                    completedCount >= 50 -> {
                        unlockAchievement(
                            userId,
                            "challenge_master",
                            "Challenge Master",
                            "Complete 50 challenges",
                            500
                        )
                    }

                    completedCount >= 25 -> {
                        unlockAchievement(
                            userId,
                            "challenge_expert",
                            "Challenge Expert",
                            "Complete 25 challenges",
                            250
                        )
                    }

                    completedCount >= 10 -> {
                        unlockAchievement(
                            userId,
                            "challenge_pro",
                            "Challenge Pro",
                            "Complete 10 challenges",
                            100
                        )
                    }

                    completedCount >= 5 -> {
                        unlockAchievement(
                            userId,
                            "challenge_enthusiast",
                            "Challenge Enthusiast",
                            "Complete 5 challenges",
                            50
                        )
                    }

                    completedCount >= 1 -> {
                        unlockAchievement(
                            userId,
                            "first_challenge",
                            "First Challenge",
                            "Complete your first challenge",
                            10
                        )
                    }
                }

                // Check for daily streak achievements
                checkDailyStreakAchievements(userId)
            }
            .addOnFailureListener {
                // Handle error
                Toast.makeText(this, "Failed to check for achievements", Toast.LENGTH_SHORT).show()
            }
    }

    private fun unlockAchievement(
        userId: String,
        achievementId: String,
        title: String,
        description: String,
        points: Long
    ) {
        // Check if achievement already unlocked
        firestore.collection("Users")
            .document(userId)
            .collection("Achievements")
            .document(achievementId)
            .get()
            .addOnSuccessListener { document ->
                if (document == null || !document.exists()) {
                    // Achievement not yet unlocked, so unlock it
                    val achievementData = hashMapOf(
                        "id" to achievementId,
                        "title" to title,
                        "description" to description,
                        "points" to points,
                        "unlockedAt" to com.google.firebase.Timestamp.now()
                    )

                    firestore.collection("Users")
                        .document(userId)
                        .collection("Achievements")
                        .document(achievementId)
                        .set(achievementData)
                        .addOnSuccessListener {
                            // Update user points for achievement
                            updateUserPoints(userId, points)

                            // Show achievement unlocked toast
                            Toast.makeText(
                                this,
                                "Achievement Unlocked: $title (+$points points)",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
    }

    private fun checkDailyStreakAchievements(userId: String) {
        // Get all daily challenges completed by the user, sorted by completion date
        firestore.collection("Users")
            .document(userId)
            .collection("CompletedChallenges")
            .whereGreaterThan("challengeId", "daily_")
            .orderBy("challengeId")
            .get()
            .addOnSuccessListener { documents ->
                // Count consecutive daily challenges
                val completedDates = mutableListOf<Date>()

                for (document in documents) {
                    val timestamp = document.getTimestamp("completedAt")
                    if (timestamp != null) {
                        completedDates.add(timestamp.toDate())
                    }
                }

                // If we have at least one completed daily challenge
                if (completedDates.isNotEmpty()) {
                    // Sort dates
                    completedDates.sortBy { it.time }

                    // Calculate max streak
                    var currentStreak = 1
                    var maxStreak = 1

                    // Convert dates to local date strings for comparison
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dateStrings = completedDates.map { dateFormat.format(it) }.distinct()

                    // Calculate streak by looking for consecutive dates
                    for (i in 1 until dateStrings.size) {
                        val currentDate = SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                        ).parse(dateStrings[i])
                        val previousDate = SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                        ).parse(dateStrings[i - 1])

                        // Check if dates are consecutive
                        val cal1 = Calendar.getInstance()
                        val cal2 = Calendar.getInstance()
                        cal1.time = previousDate
                        cal2.time = currentDate

                        // Add one day to previous date and see if it equals current date
                        cal1.add(Calendar.DAY_OF_YEAR, 1)

                        if (dateFormat.format(cal1.time) == dateFormat.format(cal2.time)) {
                            currentStreak++
                            if (currentStreak > maxStreak) {
                                maxStreak = currentStreak
                            }
                        } else {
                            currentStreak = 1
                        }
                    }

                    // Check streak achievements
                    when {
                        maxStreak >= 30 -> {
                            unlockAchievement(
                                userId,
                                "monthly_streak",
                                "Monthly Streak",
                                "Complete daily challenges for 30 consecutive days",
                                1000
                            )
                        }

                        maxStreak >= 7 -> {
                            unlockAchievement(
                                userId,
                                "weekly_streak",
                                "Weekly Streak",
                                "Complete daily challenges for 7 consecutive days",
                                200
                            )
                        }

                        maxStreak >= 3 -> {
                            unlockAchievement(
                                userId,
                                "three_day_streak",
                                "Three Day Streak",
                                "Complete daily challenges for 3 consecutive days",
                                50
                            )
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to check for streak achievements", Toast.LENGTH_SHORT)
                    .show()
            }
    }
}