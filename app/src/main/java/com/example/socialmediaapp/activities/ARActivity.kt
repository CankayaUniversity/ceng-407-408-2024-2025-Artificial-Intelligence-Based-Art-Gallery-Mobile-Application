package com.example.socialmediaapp.activities

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
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
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import java.util.concurrent.CompletableFuture

class ARActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private var imageUrl: String? = null
    private var caption: String? = null
    private var postId: String? = null
    private var isImageLoaded = false

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val TAG = "ARActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Intent'ten verileri al ve debug için log'la
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

        // Kamera izni kontrolü
        if (checkCameraPermission()) {
            setupARFragment()
        } else {
            requestCameraPermission()
        }

        // Test için resmi önceden yükle
        preloadImage()
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

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupARFragment()
                } else {
                    Toast.makeText(this, "Camera permission is required for AR", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun setupARFragment() {
        try {
            arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment

            // Plane discovery indicator'ı göster (daha iyi kullanıcı deneyimi için)
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

        // ViewRenderable kullanarak resmi AR'da göster
        createImageRenderable { renderable ->
            if (renderable != null) {
                Log.d(TAG, "Renderable created successfully")
                addNodeToScene(anchor, renderable)
            } else {
                Log.e(TAG, "Failed to create renderable")
                Toast.makeText(this, "Failed to create AR object", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createImageRenderable(callback: (ViewRenderable?) -> Unit) {
        try {
            Log.d(TAG, "Creating image renderable...")

            // Layout inflate et
            val view = LayoutInflater.from(this).inflate(R.layout.ar_image_layout, null)
            val imageView = view.findViewById<ImageView>(R.id.ar_image)
            val captionText = view.findViewById<TextView>(R.id.ar_caption)

            // Caption'ı ayarla
            captionText.text = caption ?: "AI Generated Art"

            if (imageUrl.isNullOrEmpty()) {
                Log.e(TAG, "Image URL is null or empty in createImageRenderable!")
                callback(null)
                return
            }

            Log.d(TAG, "Loading image for renderable: $imageUrl")

            // Resmi yükle
            Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .centerCrop()
                .into(object : CustomTarget<Bitmap>(400, 400) { // Fixed size
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        Log.d(TAG, "Bitmap ready for renderable: ${resource.width}x${resource.height}")
                        imageView.setImageBitmap(resource)

                        // ViewRenderable oluştur
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

                // Node'un boyutunu ayarla
                localScale = Vector3(0.3f, 0.3f, 0.3f) // Daha küçük başlangıç boyutu
            }

            // Sahneye ekle
            arFragment.arSceneView.scene.addChild(anchorNode)

            // Node'u seç
            transformableNode.select()

            Log.d(TAG, "AR node added successfully")
            Toast.makeText(this, "Image placed! Pinch to resize, drag to move", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Log.e(TAG, "Error adding node to scene: ${e.message}")
            Toast.makeText(this, "Error placing AR object: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()

        // AR Core session'ının aktif olup olmadığını kontrol et
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