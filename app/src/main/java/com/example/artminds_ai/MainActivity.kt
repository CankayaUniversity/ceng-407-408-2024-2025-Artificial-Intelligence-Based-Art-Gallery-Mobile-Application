package com.example.artminds_ai

import org.json.JSONArray
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var promptEditText: EditText
    private lateinit var generateButton: Button
    private lateinit var generateStoryCheckBox: CheckBox
    private lateinit var progressBar: ProgressBar

    // Your Azure OpenAI API credentials
    private val AZURE_OPENAI_DALLE_ENDPOINT = "YOUR_DALLE3_ENDPOINT_KEY"
    private val AZURE_OPENAI_DALLE_API_KEY = "YOUR_DALLE3_API_KEY"
    private val AZURE_ENDPOINT = "YOUR_AZURE_ENDPOINT_KEY"

    // Add the Azure OpenAI GPT endpoint for story generation
    // Change the deployment name and API version as needed for your setup
    private val AZURE_OPENAI_GPT_ENDPOINT = "YOUR_GPT_ENDPOINT_KEY"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        promptEditText = findViewById(R.id.promptEditText)
        generateButton = findViewById(R.id.generateButton)
        generateStoryCheckBox = findViewById(R.id.generateStoryCheckBox)
        progressBar = findViewById(R.id.progressBar)

        generateButton.setOnClickListener {
            val prompt = promptEditText.text.toString().trim()
            if (prompt.isNotEmpty()) {
                generateImage(prompt)
            } else {
                Toast.makeText(this, "Please enter a prompt", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateImage(prompt: String) {
        showLoading(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Using your exact endpoint for DALL-E image generation
                val requestBody = JSONObject().apply {
                    put("prompt", prompt)
                    put("n", 1)  // Number of images to generate
                    put("size", "1024x1024")  // Image size
                }.toString()

                val request = Request.Builder()
                    .url(AZURE_OPENAI_DALLE_ENDPOINT)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("api-key", AZURE_OPENAI_DALLE_API_KEY)
                    .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                val response = client.newCall(request).execute()
                val responseData = response.body?.string()

                if (response.isSuccessful && !responseData.isNullOrEmpty()) {
                    val jsonResponse = JSONObject(responseData)

                    // Check if the response has direct image data
                    if (jsonResponse.has("data")) {
                        val data = jsonResponse.getJSONArray("data")
                        val imageObj = data.getJSONObject(0)

                        // Extract image URL
                        if (imageObj.has("url")) {
                            val imageUrl = imageObj.getString("url")

                            if (generateStoryCheckBox.isChecked) {
                                // Generate a story based on the prompt
                                generateStory(prompt, imageUrl)
                            } else {
                                // Just show the image result
                                withContext(Dispatchers.Main) {
                                    navigateToResultActivity(imageUrl, null)
                                    showLoading(false)
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                showLoading(false)
                                Toast.makeText(
                                    this@MainActivity,
                                    "Image URL not found in response",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    // Check if this is an asynchronous operation that requires polling
                    else if (jsonResponse.has("id") || jsonResponse.has("operation")) {
                        val operationId = jsonResponse.optString("id", jsonResponse.optString("operation"))
                        pollForResult(operationId, prompt)
                    } else {
                        withContext(Dispatchers.Main) {
                            showLoading(false)
                            Toast.makeText(
                                this@MainActivity,
                                "Unexpected response format: $responseData",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    // Handle error
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        Toast.makeText(
                            this@MainActivity,
                            "Error: ${response.code} - ${responseData ?: "Unknown error"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        this@MainActivity,
                        "Exception: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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

                    if (status == "succeeded") {
                        isCompleted = true
                        val result = jsonResponse.getJSONObject("result")
                        val data = result.getJSONArray("data")
                        val imageUrl = data.getJSONObject(0).getString("url")

                        if (generateStoryCheckBox.isChecked) {
                            // Generate a story based on the prompt
                            generateStory(prompt, imageUrl)
                        } else {
                            // Just show the image result
                            withContext(Dispatchers.Main) {
                                navigateToResultActivity(imageUrl, null)
                                showLoading(false)
                            }
                        }
                    } else if (status == "failed") {
                        withContext(Dispatchers.Main) {
                            showLoading(false)
                            Toast.makeText(
                                this@MainActivity,
                                "Image generation failed: ${jsonResponse.optString("error", "Unknown error")}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        isCompleted = true
                    } else {
                        // Still processing, wait and try again
                        kotlinx.coroutines.delay(2000) // 2-second delay before polling again
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        Toast.makeText(
                            this@MainActivity,
                            "Error checking status: ${response.code}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    isCompleted = true
                }
            }

            if (attempts >= maxAttempts) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        this@MainActivity,
                        "Timeout: Image generation is taking too long",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                showLoading(false)
                Toast.makeText(
                    this@MainActivity,
                    "Error polling for result: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun generateStory(prompt: String, imageUrl: String) {
        try {
            val storyPrompt = """
            Write a short creative story based on this image description: "$prompt". 
            The story should be engaging, between 150-250 words, and suitable for general audiences.
            Make it vivid and descriptive, capturing the essence of the image.
        """.trimIndent()

            // The issue is here - messages should be an array, not an object
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
                    navigateToResultActivity(imageUrl, story)
                    showLoading(false)
                }
            } else {
                withContext(Dispatchers.Main) {
                    // If story generation fails, still show the image
                    navigateToResultActivity(imageUrl, "Failed to generate a story. Error: ${response.code} - ${responseData ?: "Unknown error"}")
                    showLoading(false)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                // If story generation fails, still show the image
                navigateToResultActivity(imageUrl, "Failed to generate a story. Error: ${e.message}")
                showLoading(false)
            }
        }
    }

    private fun navigateToResultActivity(imageUrl: String, story: String?) {
        val intent = Intent(this, ResultActivity::class.java).apply {
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