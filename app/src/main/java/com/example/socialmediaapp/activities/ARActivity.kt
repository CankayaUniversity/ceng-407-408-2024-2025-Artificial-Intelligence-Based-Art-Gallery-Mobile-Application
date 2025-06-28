package com.example.socialmediaapp.activities

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.PixelCopy
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.socialmediaapp.R
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CompletableFuture

class ARActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private var imageUrl: String? = null
    private var caption: String? = null
    private var postId: String? = null
    private var isImageLoaded = false
    private var isImagePlaced = false

    // UI Elements
    private lateinit var captureButton: ImageButton
    private lateinit var switchCameraButton: ImageButton
    private lateinit var galleryButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var infoText: TextView

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val STORAGE_PERMISSION_CODE = 101
        private const val TAG = "ARActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Intent'ten verileri al
        imageUrl = intent.getStringExtra("imageUrl")
        caption = intent.getStringExtra("caption")
        postId = intent.getStringExtra("postId")

        Log.d(TAG, "ARActivity started with:")
        Log.d(TAG, "Image URL: $imageUrl")
        Log.d(TAG, "Caption: $caption")
        Log.d(TAG, "Post ID: $postId")

        // AR desteği kontrolü
        if (!isARSupported()) {
            showARNotSupportedDialog()
            return
        }

        setContentView(R.layout.activity_aractivity)

        // UI elementlerini initialize et
        initializeViews()

        // İzin kontrolü
        if (checkPermissions()) {
            setupARFragment()
        } else {
            requestPermissions()
        }

        // Resmi önceden yükle
        preloadImage()
    }

    private fun initializeViews() {
        captureButton = findViewById(R.id.capture_button)
        switchCameraButton = findViewById(R.id.switch_camera_button)
        galleryButton = findViewById(R.id.gallery_button)
        backButton = findViewById(R.id.back_button)
        infoText = findViewById(R.id.info_text)

        // Başlangıçta kamera butonlarını gizle
        captureButton.visibility = View.GONE
        switchCameraButton.visibility = View.GONE
        galleryButton.visibility = View.GONE

        // Button click listeners
        captureButton.setOnClickListener {
            captureARScene()
        }

        switchCameraButton.setOnClickListener {
            // Bu özellik ARCore'da mevcut değil, alternatif göster
            Toast.makeText(this, "Camera switching not available in AR mode", Toast.LENGTH_SHORT).show()
        }

        galleryButton.setOnClickListener {
            openGallery()
        }

        backButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun checkPermissions(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PackageManager.PERMISSION_GRANTED // Android 10+ için storage permission gerekli değil
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                storagePermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), CAMERA_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    setupARFragment()
                } else {
                    Toast.makeText(this, "Camera and storage permissions are required for AR", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun preloadImage() {
        if (imageUrl.isNullOrEmpty()) {
            Log.e(TAG, "Image URL is null or empty!")
            Toast.makeText(this, "Error: No image URL provided", Toast.LENGTH_LONG).show()
            return
        }

        Log.d(TAG, "Preloading image: $imageUrl")
        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    Log.d(TAG, "Image loaded successfully! Size: ${resource.width}x${resource.height}")
                    isImageLoaded = true
                    runOnUiThread {
                        Toast.makeText(this@ARActivity, "Image loaded! Tap on a surface to place it", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    Log.w(TAG, "Image load cleared")
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    Log.e(TAG, "Failed to load image: $imageUrl")
                    runOnUiThread {
                        Toast.makeText(this@ARActivity, "Failed to load image. Check internet connection.", Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    private fun isARSupported(): Boolean {
        return try {
            val availability = com.google.ar.core.ArCoreApk.getInstance().checkAvailability(this)
            availability.isSupported
        } catch (e: Exception) {
            Log.e(TAG, "AR not supported: ${e.message}")
            false
        }
    }

    private fun showARNotSupportedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("AR Not Supported")
            .setMessage("This device doesn't support AR functionality. AR features work only on physical devices with ARCore support.")
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun setupARFragment() {
        try {
            arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment

            // Plane discovery indicator'ı göster
            arFragment.planeDiscoveryController.show()

            // AR tap listener'ı ayarla
            arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
                Log.d(TAG, "AR surface tapped")
                if (!isImageLoaded) {
                    Toast.makeText(this, "Please wait, image is still loading...", Toast.LENGTH_SHORT).show()
                    return@setOnTapArPlaneListener
                }
                placeImageInAR(hitResult)
            }

            Toast.makeText(this, "Look for flat surfaces and tap to place the image", Toast.LENGTH_LONG).show()
            Log.d(TAG, "AR Fragment setup completed")

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up AR Fragment: ${e.message}")
            Toast.makeText(this, "Error setting up AR: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun placeImageInAR(hitResult: HitResult) {
        Log.d(TAG, "Placing image in AR...")
        val anchor = hitResult.createAnchor()

        createImageRenderable { renderable ->
            if (renderable != null) {
                Log.d(TAG, "Renderable created successfully")
                addNodeToScene(anchor, renderable)

                // Resim yerleştirildi, kamera butonlarını göster
                showCameraControls()
            } else {
                Log.e(TAG, "Failed to create renderable")
                Toast.makeText(this, "Failed to create AR object", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCameraControls() {
        isImagePlaced = true
        captureButton.visibility = View.VISIBLE
        galleryButton.visibility = View.VISIBLE

        // Info text'i güncelle
        infoText.text = "Tap capture button to take photo"

        Toast.makeText(this, "Image placed! You can now take photos", Toast.LENGTH_LONG).show()
    }

    private fun createImageRenderable(callback: (ViewRenderable?) -> Unit) {
        try {
            Log.d(TAG, "Creating image renderable...")

            val view = LayoutInflater.from(this).inflate(R.layout.ar_image_layout, null)
            val imageView = view.findViewById<ImageView>(R.id.ar_image)


            //captionText.text = caption ?: "AI Generated Art"

            if (imageUrl.isNullOrEmpty()) {
                Log.e(TAG, "Image URL is null or empty in createImageRenderable!")
                callback(null)
                return
            }

            Log.d(TAG, "Loading image for renderable: $imageUrl")

            Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .centerCrop()
                .into(object : CustomTarget<Bitmap>(400, 400) {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        Log.d(TAG, "Bitmap ready for renderable: ${resource.width}x${resource.height}")
                        imageView.setImageBitmap(resource)

                        ViewRenderable.builder()
                            .setView(this@ARActivity, view)
                            .build()
                            .thenAccept { renderable ->
                                Log.d(TAG, "ViewRenderable build successful")
                                callback(renderable)
                            }
                            .exceptionally { throwable ->
                                Log.e(TAG, "Unable to build renderable", throwable)
                                runOnUiThread {
                                    Toast.makeText(this@ARActivity, "Error creating AR object: ${throwable.message}", Toast.LENGTH_LONG).show()
                                }
                                callback(null)
                                null
                            }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        Log.w(TAG, "Image load cleared in renderable")
                        callback(null)
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        Log.e(TAG, "Failed to load image for renderable: $imageUrl")
                        runOnUiThread {
                            Toast.makeText(this@ARActivity, "Failed to load image for AR", Toast.LENGTH_LONG).show()
                        }
                        callback(null)
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "Exception in createImageRenderable: ${e.message}")
            callback(null)
        }
    }

    private fun addNodeToScene(anchor: Anchor, renderable: ViewRenderable) {
        try {
            Log.d(TAG, "Adding node to AR scene")

            val anchorNode = AnchorNode(anchor)

            val transformableNode = TransformableNode(arFragment.transformationSystem).apply {
                this.renderable = renderable
                setParent(anchorNode)
                localScale = Vector3(0.3f, 0.3f, 0.3f)
            }

            arFragment.arSceneView.scene.addChild(anchorNode)
            transformableNode.select()

            Log.d(TAG, "AR node added successfully")
            Toast.makeText(this, "Image placed! Pinch to resize, drag to move", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Log.e(TAG, "Error adding node to scene: ${e.message}")
            Toast.makeText(this, "Error placing AR object: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun captureARScene() {
        if (!isImagePlaced) {
            Toast.makeText(this, "Please place an image first", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            Log.d(TAG, "Capturing AR scene...")

            val view = arFragment.arSceneView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

            // Android 7.0+ için PixelCopy kullan
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PixelCopy.request(
                    view,
                    bitmap,
                    { result ->
                        when (result) {
                            PixelCopy.SUCCESS -> {
                                Log.d(TAG, "PixelCopy successful")
                                saveBitmapToGallery(bitmap)
                            }
                            else -> {
                                Log.e(TAG, "PixelCopy failed with result: $result")
                                Toast.makeText(this@ARActivity, "Failed to capture image", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    android.os.Handler(mainLooper)
                )
            } else {
                // Eski Android sürümleri için alternatif yöntem
                val canvas = Canvas(bitmap)
                view.draw(canvas)
                saveBitmapToGallery(bitmap)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error capturing AR scene: ${e.message}")
            Toast.makeText(this, "Error capturing image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "AR_Photo_$timeStamp.jpg"

            var savedUri: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ için MediaStore kullan
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AR Photos")
                }

                val resolver = contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let {
                    val outputStream: OutputStream? = resolver.openOutputStream(it)
                    outputStream?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                        savedUri = it
                    }
                }
            } else {
                // Android 9 ve altı için dosya sistemi kullan
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val arPhotosDir = File(picturesDir, "AR Photos")

                if (!arPhotosDir.exists()) {
                    arPhotosDir.mkdirs()
                }

                val file = File(arPhotosDir, fileName)
                val outputStream = FileOutputStream(file)

                outputStream.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                }

                // MediaStore'a bildirin
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                savedUri = Uri.fromFile(file)
                mediaScanIntent.data = savedUri
                sendBroadcast(mediaScanIntent)
            }

            runOnUiThread {
                if (savedUri != null) {
                    Toast.makeText(this, "AR photo saved to gallery!", Toast.LENGTH_LONG).show()
                    Log.d(TAG, "Photo saved successfully: $savedUri")
                } else {
                    Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Failed to save photo - URI is null")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap to gallery: ${e.message}")
            runOnUiThread {
                Toast.makeText(this, "Error saving photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGallery() {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.type = "image/*"
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening gallery: ${e.message}")
            Toast.makeText(this, "Error opening gallery", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        val session = arFragment.arSceneView.session
        if (session == null) {
            Log.e(TAG, "Session is null")
        } else {
            Log.d(TAG, "AR Session is active")
        }
    }

    override fun onPause() {
        super.onPause()
        arFragment.arSceneView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        arFragment.arSceneView.destroy()
    }
}