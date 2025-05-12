package com.example.socialmediaapp.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.example.socialmediaapp.R
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit


class ImageGenerationPageActivity : BaseActivity() {
    override fun getContentLayoutId(): Int {
        return R.layout.activity_image_generation_page
    }

    private lateinit var promptEditText: EditText
    private lateinit var negativePromptEditText: EditText
    private lateinit var generateButton: Button
    private lateinit var storySwitch: SwitchMaterial
    private lateinit var imageGenerationModeSwitch: SwitchMaterial
    private lateinit var progressBar: ProgressBar
    private lateinit var promptCounter: TextView
    private lateinit var negativePromptCounter: TextView
    private lateinit var promptClearButton: ImageButton
    private lateinit var negativePromptClearButton: ImageButton
    private lateinit var styleCardCyberpunk: CardView
    private lateinit var styleCardCartoon: CardView
    private lateinit var styleCardAnime: CardView
    private lateinit var styleCardHyperrealistic: CardView
    private var selectedStyle: String = "none"
    private val MAX_PROMPT_LENGTH = 300
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_NAME = "ThemePrefs"
    // Your Azure OpenAI API credentials - replace with actual keys in production
    private val AZURE_OPENAI_DALLE_ENDPOINT = "https://mindsart-storygeneration.openai.azure.com/openai/deployments/MindsArt-dall-e-3/images/generations?api-version=2024-02-01"
    private val AZURE_OPENAI_DALLE_API_KEY = "Bd5xyYWvSIZMUzaaBOhBMM8mVpoP9Ldk4EWBa4REpuM4MZ3HLIFIJQQJ99BCACfhMk5XJ3w3AAABACOG34N4"
    private val AZURE_ENDPOINT = "https://mindsart-storygeneration.openai.azure.com/"
    private val AZURE_OPENAI_GPT_ENDPOINT = "https://mindsart-storygeneration.openai.azure.com/openai/deployments/MindsArt-GPT4/chat/completions?api-version=2024-10-21"
    private val AZURE_VISION_CAPTION_ENDPOINT = "https://mindsartapi-imagecaption-2.cognitiveservices.azure.com/"
    private val AZURE_VISION_API_KEY = "AdQAfeZyRq0km3uH1KMxdBXAgC76e4i0VulxxmntLbw55fZdPL82JQQJ99BCACfhMk5XJ3w3AAAFACOGkC8v" // Replace with your actual Azure Vision API key

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set title for this activity
        setToolbarTitle("Image Generation")

        initializeViews()
        setupListeners()
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val mainLayout = findViewById<ScrollView>(R.id.image_generation)

        if (sharedPreferences.getBoolean(PREF_NAME, false)) {
            mainLayout.setBackgroundColor("#3F51B5".toColorInt())
        }
    }

    private fun initializeViews() {
        // Initialize your views here
        promptEditText = findViewById(R.id.promptEditText)
        negativePromptEditText = findViewById(R.id.negativePromptEditText)
        generateButton = findViewById(R.id.generateButton)
        storySwitch = findViewById(R.id.storySwitch)
        imageGenerationModeSwitch = findViewById(R.id.imageGenerationModeSwitch)
        progressBar = findViewById(R.id.progressBar)
        promptCounter = findViewById(R.id.promptCounter)
        negativePromptCounter = findViewById(R.id.negativePromptCounter)
        promptClearButton = findViewById(R.id.promptClearButton)
        negativePromptClearButton = findViewById(R.id.negativePromptClearButton)

        // Style cards
        styleCardCyberpunk = findViewById(R.id.styleCardCyberpunk)
        styleCardCartoon = findViewById(R.id.styleCardCartoon)
        styleCardAnime = findViewById(R.id.styleCardAnime)
        styleCardHyperrealistic = findViewById(R.id.styleCardHyperrealistic)

        // Initialize counters
        promptCounter.text = "0/${MAX_PROMPT_LENGTH}"
        negativePromptCounter.text = "0/${MAX_PROMPT_LENGTH}"
    }

    private fun setupListeners() {
        // Generate button click listener
        generateButton.setOnClickListener {
            val prompt = promptEditText.text.toString().trim()
            val negativePrompt = negativePromptEditText.text.toString().trim()

            if (prompt.isNotEmpty()) {
                // Determine generation mode
                when {
                    imageGenerationModeSwitch.isChecked -> generateStoryThenImage(prompt)
                    storySwitch.isChecked -> generateStoryImage(prompt, negativePrompt)
                    else -> generateStandardImage(prompt, negativePrompt)
                }
            } else {
                Toast.makeText(this, "Please enter a prompt", Toast.LENGTH_SHORT).show()
            }
        }

        // Prompt text change listener for counter
        promptEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                promptCounter.text = "$length/$MAX_PROMPT_LENGTH"
                promptClearButton.visibility = if (length > 0) View.VISIBLE else View.INVISIBLE
            }
        })

        // Negative prompt text change listener for counter
        negativePromptEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                negativePromptCounter.text = "$length/$MAX_PROMPT_LENGTH"
                negativePromptClearButton.visibility = if (length > 0) View.VISIBLE else View.INVISIBLE
            }
        })

        // Clear buttons
        promptClearButton.setOnClickListener {
            promptEditText.setText("")
        }

        negativePromptClearButton.setOnClickListener {
            negativePromptEditText.setText("")
        }

        // Style card click listeners
        styleCardCyberpunk.setOnClickListener { selectStyle("cyberpunk") }
        styleCardCartoon.setOnClickListener { selectStyle("cartoon") }
        styleCardAnime.setOnClickListener { selectStyle("anime") }
        styleCardHyperrealistic.setOnClickListener { selectStyle("hyperrealistic") }

        // Changing the color of the switches depending on the on-off status
        storySwitch.setOnClickListener {
            if(!storySwitch.isChecked)
                storySwitch.trackTintList = ColorStateList.valueOf(Color.GRAY)
            else
                storySwitch.trackTintList = ColorStateList.valueOf("#3389FF".toColorInt())
        }

        imageGenerationModeSwitch.setOnClickListener {
            if(!imageGenerationModeSwitch.isChecked)
                imageGenerationModeSwitch.trackTintList = ColorStateList.valueOf(Color.GRAY)
            else
                imageGenerationModeSwitch.trackTintList = ColorStateList.valueOf("#3389FF".toColorInt())
        }
    }

    private fun selectStyle(style: String) {
        // Reset all card backgrounds
        styleCardCyberpunk.setCardBackgroundColor(getColor(R.color.card_background))
        styleCardCartoon.setCardBackgroundColor(getColor(R.color.card_background))
        styleCardAnime.setCardBackgroundColor(getColor(R.color.card_background))
        styleCardHyperrealistic.setCardBackgroundColor(getColor(R.color.card_background))

        // Set selected style
        selectedStyle = if (selectedStyle == style) "none" else style

        // Highlight selected card if any
        when (selectedStyle) {
            "cyberpunk" -> styleCardCyberpunk.setCardBackgroundColor(getColor(R.color.selected_style))
            "cartoon" -> styleCardCartoon.setCardBackgroundColor(getColor(R.color.selected_style))
            "anime" -> styleCardAnime.setCardBackgroundColor(getColor(R.color.selected_style))
            "hyperrealistic" -> styleCardHyperrealistic.setCardBackgroundColor(getColor(R.color.selected_style))
        }
    }

    private fun generateStoryThenImage(prompt: String) {
        showLoading(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // First generate a story
                val story = generateStoryFromPrompt(prompt)

                // Then generate image based on the story
                val finalPrompt = "$story, ${prompt}"
                val finalPromptWithStyle = if (selectedStyle != "none") {
                    "$finalPrompt, in $selectedStyle style"
                } else {
                    finalPrompt
                }

                // Modify generateImage to handle story generation mode
                generateImage(finalPromptWithStyle, "", isStoryMode = true, generatedStory = story)
            } catch (e: Exception) {
                handleError("Error in story-to-image generation: ${e.message}")
            }
        }
    }

    private fun generateImage(
        prompt: String,
        negativePrompt: String = "",
        isStoryMode: Boolean = false,
        generatedStory: String? = null
    ) {
        showLoading(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Create the request body for DALL-E
                val requestBodyObj = JSONObject().apply {
                    put("prompt", prompt)
                    put("n", 1)  // Number of images to generate
                    put("size", "1024x1024")  // Image size

                    // Add negative prompt if provided
                    if (negativePrompt.isNotEmpty()) {
                        put("negative_prompt", negativePrompt)
                    }
                }

                val requestBody = requestBodyObj.toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url(AZURE_OPENAI_DALLE_ENDPOINT)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("api-key", AZURE_OPENAI_DALLE_API_KEY)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseData = response.body?.string()

                if (response.isSuccessful && !responseData.isNullOrEmpty()) {
                    val jsonResponse = JSONObject(responseData)

                    // Handle direct image data response
                    if (jsonResponse.has("data")) {
                        val data = jsonResponse.getJSONArray("data")
                        val imageObj = data.getJSONObject(0)

                        if (imageObj.has("url")) {
                            val imageUrl = imageObj.getString("url")

                            // Determine which flow to follow based on switches or passed parameters
                            when {
                                imageGenerationModeSwitch.isChecked || isStoryMode -> {
                                    withContext(Dispatchers.Main) {
                                        navigateToStoryGenerationPage(imageUrl, generatedStory)
                                        showLoading(false)
                                    }
                                }
                                storySwitch.isChecked -> {
                                    generateStory(prompt, imageUrl)
                                }
                                else -> {
                                    withContext(Dispatchers.Main) {
                                        navigateToStoryGenerationPage(imageUrl, null)
                                        showLoading(false)
                                    }
                                }
                            }
                        } else {
                            handleError("Image URL not found in response")
                        }
                    }
                    // Handle asynchronous response that requires polling
                    else if (jsonResponse.has("id") || jsonResponse.has("operation")) {
                        val operationId = jsonResponse.optString("id", jsonResponse.optString("operation"))
                        pollForResult(operationId, prompt, isStoryMode, generatedStory)
                    } else {
                        handleError("Unexpected response format: $responseData")
                    }
                } else {
                    handleError("Error: ${response.code} - ${responseData ?: "Unknown error"}")
                }
            } catch (e: Exception) {
                handleError("Exception: ${e.message}")
            }
        }
    }

    private suspend fun pollForResult(
        operationId: String,
        prompt: String,
        isStoryMode: Boolean = false,
        generatedStory: String? = null
    ) {
        try {
            // Base polling URL for checking operation status
            val url = "${AZURE_ENDPOINT}openai/operations/images/$operationId?api-version=2024-02-01"

            var isCompleted = false
            var attempts = 0
            val maxAttempts = 30

            while (!isCompleted && attempts < maxAttempts) {
                attempts++

                val request = Request.Builder()
                    .url(url)
                    .addHeader("api-key", AZURE_OPENAI_DALLE_API_KEY)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseData = response.body?.string()

                if (response.isSuccessful && !responseData.isNullOrEmpty()) {
                    val jsonResponse = JSONObject(responseData)
                    val status = jsonResponse.getString("status")

                    when (status) {
                        "succeeded" -> {
                            isCompleted = true
                            val result = jsonResponse.getJSONObject("result")
                            val data = result.getJSONArray("data")
                            val imageUrl = data.getJSONObject(0).getString("url")

                            // Determine which flow to follow based on switches or passed parameters
                            when {
                                imageGenerationModeSwitch.isChecked || isStoryMode -> {
                                    withContext(Dispatchers.Main) {
                                        navigateToStoryGenerationPage(imageUrl, generatedStory)
                                        showLoading(false)
                                    }
                                }
                                storySwitch.isChecked -> {
                                    generateStory(prompt, imageUrl)
                                }
                                else -> {
                                    withContext(Dispatchers.Main) {
                                        navigateToStoryGenerationPage(imageUrl, null)
                                        showLoading(false)
                                    }
                                }
                            }
                        }
                        "failed" -> {
                            isCompleted = true
                            handleError("Image generation failed: ${jsonResponse.optString("error", "Unknown error")}")
                        }
                        else -> {
                            // Still processing, wait and try again
                            kotlinx.coroutines.delay(2000) // 2-second delay
                        }
                    }
                } else {
                    isCompleted = true
                    handleError("Error checking status: ${response.code}")
                }
            }

            if (attempts >= maxAttempts) {
                handleError("Timeout: Image generation is taking too long")
            }
        } catch (e: Exception) {
            handleError("Error polling for result: ${e.message}")
        }
    }

    private fun generateStoryImage(prompt: String, negativePrompt: String) {
        showLoading(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Generate story first
                val story = generateStoryFromPrompt(prompt)

                // Build final prompt
                var finalPrompt = "$story, $prompt"
                if (selectedStyle != "none") {
                    finalPrompt = "$finalPrompt, in $selectedStyle style"
                }

                generateImage(finalPrompt, negativePrompt)
            } catch (e: Exception) {
                handleError("Error in story image generation: ${e.message}")
            }
        }
    }

    private fun generateStandardImage(prompt: String, negativePrompt: String) {
        var finalPrompt = prompt
        if (selectedStyle != "none") {
            finalPrompt = "$prompt, in $selectedStyle style"
        }

        generateImage(finalPrompt, negativePrompt)
    }

    private suspend fun generateStoryFromPrompt(prompt: String): String {
        val storyPrompt = """
            Write a short creative story based on this description: "$prompt". 
            The story should be engaging, between 80-120 words, and suitable for general audiences.
            Make it vivid and descriptive, capturing the essence of the prompt.
        """.trimIndent()

        val requestBody = JSONObject().apply {
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", storyPrompt)
                })
            })
            put("max_tokens", 500)
            put("temperature", 0.7)
        }.toString()

        val request = Request.Builder()
            .url(AZURE_OPENAI_GPT_ENDPOINT)
            .addHeader("Content-Type", "application/json")
            .addHeader("api-key", AZURE_OPENAI_DALLE_API_KEY)
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        val response = client.newCall(request).execute()
        val responseData = response.body?.string()

        return if (response.isSuccessful && !responseData.isNullOrEmpty()) {
            val jsonResponse = JSONObject(responseData)
            if (jsonResponse.has("choices")) {
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    if (firstChoice.has("message")) {
                        val message = firstChoice.getJSONObject("message")
                        message.getString("content")
                    } else {
                        throw Exception("No story message found")
                    }
                } else {
                    throw Exception("No story choices available")
                }
            } else {
                throw Exception("Invalid story generation response")
            }
        } else {
            throw Exception("Story generation failed")
        }
    }

    private fun generateImage(prompt: String, negativePrompt: String = "") {
        showLoading(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Create the request body for DALL-E
                val requestBodyObj = JSONObject().apply {
                    put("prompt", prompt)
                    put("n", 1)  // Number of images to generate
                    put("size", "1024x1024")  // Image size

                    // Add negative prompt if provided
                    if (negativePrompt.isNotEmpty()) {
                        put("negative_prompt", negativePrompt)
                    }
                }

                val requestBody = requestBodyObj.toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url(AZURE_OPENAI_DALLE_ENDPOINT)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("api-key", AZURE_OPENAI_DALLE_API_KEY)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseData = response.body?.string()

                if (response.isSuccessful && !responseData.isNullOrEmpty()) {
                    val jsonResponse = JSONObject(responseData)

                    // Handle direct image data response
                    if (jsonResponse.has("data")) {
                        val data = jsonResponse.getJSONArray("data")
                        val imageObj = data.getJSONObject(0)

                        if (imageObj.has("url")) {
                            val imageUrl = imageObj.getString("url")

                            // Determine which flow to follow based on switches
                            when {
                                imageGenerationModeSwitch.isChecked -> {
                                    // Story already generated in generateStoryThenImage method
                                    withContext(Dispatchers.Main) {
                                        navigateToStoryGenerationPage(imageUrl, null)
                                        showLoading(false)
                                    }
                                }
                                storySwitch.isChecked -> {
                                    generateStory(prompt, imageUrl)
                                }
                                else -> {
                                    withContext(Dispatchers.Main) {
                                        navigateToStoryGenerationPage(imageUrl, null)
                                        showLoading(false)
                                    }
                                }
                            }
                        } else {
                            handleError("Image URL not found in response")
                        }
                    }
                    // Handle asynchronous response that requires polling
                    else if (jsonResponse.has("id") || jsonResponse.has("operation")) {
                        val operationId = jsonResponse.optString("id", jsonResponse.optString("operation"))
                        pollForResult(operationId, prompt)
                    } else {
                        handleError("Unexpected response format: $responseData")
                    }
                } else {
                    handleError("Error: ${response.code} - ${responseData ?: "Unknown error"}")
                }
            } catch (e: Exception) {
                handleError("Exception: ${e.message}")
            }
        }
    }

    private suspend fun pollForResult(operationId: String, prompt: String) {
        try {
            // Base polling URL for checking operation status
            val url = "${AZURE_ENDPOINT}openai/operations/images/$operationId?api-version=2024-02-01"

            var isCompleted = false
            var attempts = 0
            val maxAttempts = 30

            while (!isCompleted && attempts < maxAttempts) {
                attempts++

                val request = Request.Builder()
                    .url(url)
                    .addHeader("api-key", AZURE_OPENAI_DALLE_API_KEY)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseData = response.body?.string()

                if (response.isSuccessful && !responseData.isNullOrEmpty()) {
                    val jsonResponse = JSONObject(responseData)
                    val status = jsonResponse.getString("status")

                    when (status) {
                        "succeeded" -> {
                            isCompleted = true
                            val result = jsonResponse.getJSONObject("result")
                            val data = result.getJSONArray("data")
                            val imageUrl = data.getJSONObject(0).getString("url")

                            // Determine which flow to follow based on switches
                            when {
                                imageGenerationModeSwitch.isChecked -> {
                                    // Story already generated in generateStoryThenImage method
                                    withContext(Dispatchers.Main) {
                                        navigateToStoryGenerationPage(imageUrl, null)
                                        showLoading(false)
                                    }
                                }
                                storySwitch.isChecked -> {
                                    generateStory(prompt, imageUrl)
                                }
                                else -> {
                                    withContext(Dispatchers.Main) {
                                        navigateToStoryGenerationPage(imageUrl, null)
                                        showLoading(false)
                                    }
                                }
                            }
                        }
                        "failed" -> {
                            isCompleted = true
                            handleError("Image generation failed: ${jsonResponse.optString("error", "Unknown error")}")
                        }
                        else -> {
                            // Still processing, wait and try again
                            kotlinx.coroutines.delay(2000) // 2-second delay
                        }
                    }
                } else {
                    isCompleted = true
                    handleError("Error checking status: ${response.code}")
                }
            }

            if (attempts >= maxAttempts) {
                handleError("Timeout: Image generation is taking too long")
            }
        } catch (e: Exception) {
            handleError("Error polling for result: ${e.message}")
        }
    }

    private suspend fun generateStory(prompt: String, imageUrl: String) {
        try {
            val storyPrompt = """
                Write a short creative story based on this image description: "$prompt". 
                The story should be engaging, between 150-250 words, and suitable for general audiences.
                Make it vivid and descriptive, capturing the essence of the image.
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", storyPrompt)
                    })
                })
                put("max_tokens", 500)
                put("temperature", 0.7)
            }.toString()

            val request = Request.Builder()
                .url(AZURE_OPENAI_GPT_ENDPOINT)
                .addHeader("Content-Type", "application/json")
                .addHeader("api-key", AZURE_OPENAI_DALLE_API_KEY)
                .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            val response = client.newCall(request).execute()
            val responseData = response.body?.string()

            if (response.isSuccessful && !responseData.isNullOrEmpty()) {
                val jsonResponse = JSONObject(responseData)

                // Extract the story from the GPT response
                val story = if (jsonResponse.has("choices")) {
                    val choices = jsonResponse.getJSONArray("choices")
                    if (choices.length() > 0) {
                        val firstChoice = choices.getJSONObject(0)
                        if (firstChoice.has("message")) {
                            val message = firstChoice.getJSONObject("message")
                            message.getString("content")
                        } else {
                            "Failed to extract story from response"
                        }
                    } else {
                        "No story was generated"
                    }
                } else {
                    "Failed to generate a story"
                }

                withContext(Dispatchers.Main) {
                    navigateToStoryGenerationPage(imageUrl, story)
                    showLoading(false)
                }
            } else {
                // If story generation fails, still show the image
                withContext(Dispatchers.Main) {
                    navigateToStoryGenerationPage(imageUrl, "Failed to generate a story. Error: ${response.code} - ${responseData ?: "Unknown error"}")
                    showLoading(false)
                }
            }
        } catch (e: Exception) {
            // If story generation fails, still show the image
            withContext(Dispatchers.Main) {
                navigateToStoryGenerationPage(imageUrl, "Failed to generate a story. Error: ${e.message}")
                showLoading(false)
            }
        }
    }

    private suspend fun generateImageCaption(imageUrl: String): String {
        try {
            val requestBodyObj = JSONObject().apply {
                put("url", imageUrl)
            }

            val requestBody = requestBodyObj.toString()
                .toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(AZURE_VISION_CAPTION_ENDPOINT)
                .addHeader("Content-Type", "application/json")
                .addHeader("Ocp-Apim-Subscription-Key", AZURE_VISION_API_KEY)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseData = response.body?.string()

            return if (response.isSuccessful && !responseData.isNullOrEmpty()) {
                val jsonResponse = JSONObject(responseData)
                if (jsonResponse.has("description") &&
                    jsonResponse.getJSONObject("description").has("captions")) {
                    val captions = jsonResponse.getJSONObject("description").getJSONArray("captions")
                    if (captions.length() > 0) {
                        captions.getJSONObject(0).getString("text")
                    } else {
                        "No caption found"
                    }
                } else {
                    "Unable to generate caption"
                }
            } else {
                "Caption generation failed"
            }
        } catch (e: Exception) {
            return "Error in caption generation: ${e.message}"
        }
    }

    private suspend fun handleError(message: String) {
        withContext(Dispatchers.Main) {
            showLoading(false)
            Toast.makeText(this@ImageGenerationPageActivity, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToStoryGenerationPage(imageUrl: String, story: String?) {
        val intent = Intent(this, StoryGenerationPageActivity::class.java).apply {
            putExtra("IMAGE_URL", imageUrl)
            putExtra("STORY", story)
            putExtra("PROMPT", promptEditText.text.toString().trim())
        }
        startActivity(intent)
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            generateButton.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            generateButton.isEnabled = true
        }
    }
}