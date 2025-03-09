package com.example.artminds_ai


import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
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
    private lateinit var resultImageView: ImageView
    private lateinit var progressBar: ProgressBar

    // Your Azure OpenAI API credentials
    private val AZURE_OPENAI_DALLE_ENDPOINT = "https://mindsart-storygeneration.openai.azure.com/openai/deployments/mindsArt-ImageGeneration/images/generations?api-version=2024-02-01"
    private val AZURE_OPENAI_DALLE_API_KEY = "lGsXrBI6BLLSaVNeTk6ILqt24GSpT0koiqRP7iINuB5aqKqzf5XDJQQJ99BCACfhMk5XJ3w3AAABACOGWSmU"
    private val AZURE_ENDPOINT = "https://mindsart-storygeneration.openai.azure.com/"

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
        resultImageView = findViewById(R.id.resultImageView)
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
                    // Other parameters can be added as needed
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
                    // The response format might be different based on your Azure setup
                    val jsonResponse = JSONObject(responseData)

                    // Check if the response has direct image data
                    if (jsonResponse.has("data")) {
                        val data = jsonResponse.getJSONArray("data")
                        val imageObj = data.getJSONObject(0)

                        // Extract image URL or base64 data depending on response format
                        val imageUrl = when {
                            imageObj.has("url") -> imageObj.getString("url")
                            imageObj.has("b64_json") -> {
                                // If it's base64, we'd need to convert it to displayable format
                                // For simplicity, we're assuming URL-based response here
                                withContext(Dispatchers.Main) {
                                    showLoading(false)
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Received base64 image data, handling not implemented in this example",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                return@launch
                            }
                            else -> {
                                withContext(Dispatchers.Main) {
                                    showLoading(false)
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Unexpected response format",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                return@launch
                            }
                        }

                        withContext(Dispatchers.Main) {
                            loadImage(imageUrl)
                        }
                    }
                    // Check if this is an asynchronous operation that requires polling
                    else if (jsonResponse.has("id") || jsonResponse.has("operation")) {
                        val operationId = jsonResponse.optString("id", jsonResponse.optString("operation"))
                        // You would call a polling method here if needed
                        pollForResult(operationId)
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

    private suspend fun pollForResult(operationId: String) {
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

                        withContext(Dispatchers.Main) {
                            loadImage(imageUrl)
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
                        withContext(Dispatchers.Main) {
                            // Optional: Update UI to show waiting status
                        }
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

    private fun loadImage(imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .into(resultImageView)

        showLoading(false)
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