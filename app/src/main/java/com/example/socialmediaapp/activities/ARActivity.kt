package com.example.socialmediaapp.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
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
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode

class ARActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private var imageUrl: String? = null
    private var caption: String? = null
    private var postId: String? = null
    private var cachedBitmap: Bitmap? = null
    private var isImageReady = false

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val TAG = "ARActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Intent'ten verileri al
        imageUrl = intent.getStringExtra("imageUrl")
        caption = intent.getStringExtra("caption")
        postId = intent.getStringExtra("postId")

        Log.d(TAG, "=== AR Activity Started ===")
        Log.d(TAG, "Image URL: $imageUrl")
        Log.d(TAG, "Caption: $caption")

        // AR desteği kontrolü
        if (!isARSupported()) {
            showARNotSupportedDialog()
            return
        }

        setContentView(R.layout.activity_aractivity)

        // Kamera izni kontrolü
        if (checkCameraPermission()) {
            setupARFragment()
            preloadImage()
        } else {
            requestCameraPermission()
        }
    }

    private fun preloadImage() {
        if (imageUrl.isNullOrEmpty()) {
            Log.e(TAG, "❌ Image URL is empty!")
            createTextOnlyObject()
            return
        }

        Log.d(TAG, "🔄 Starting image preload...")

        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .override(512, 512) // Sabit boyut
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    Log.d(TAG, "✅ Image loaded successfully! ${resource.width}x${resource.height}")
                    cachedBitmap = resource
                    isImageReady = true
                    runOnUiThread {
                        Toast.makeText(this@ARActivity, "✅ Image ready! Tap on white dots to place", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    Log.w(TAG, "⚠️ Image load cleared")
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    Log.e(TAG, "❌ Image load failed for: $imageUrl")
                    runOnUiThread {
                        Toast.makeText(this@ARActivity, "❌ Image load failed, showing text", Toast.LENGTH_LONG).show()
                    }
                    createTextOnlyObject()
                }
            })
    }

    private fun createTextOnlyObject() {
        isImageReady = true // Text olarak hazır
        runOnUiThread {
            Toast.makeText(this, "📝 Text mode ready! Tap on white dots", Toast.LENGTH_LONG).show()
        }
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
            .setMessage("This device doesn't support AR functionality.")
            .setPositiveButton("OK") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupARFragment()
                    preloadImage()
                } else {
                    Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun setupARFragment() {
        try {
            arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment

            // Plane discovery'yi göster
            arFragment.planeDiscoveryController.show()

            Log.d(TAG, "🎯 Setting up AR tap listener...")

            // AR tap listener
            arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
                Log.d(TAG, "🖱️ AR plane tapped!")
                Log.d(TAG, "Plane type: ${plane.type}")
                Log.d(TAG, "Plane tracking state: ${plane.trackingState}")
                Log.d(TAG, "Image ready: $isImageReady")

                if (!isImageReady) {
                    Toast.makeText(this, "⏳ Please wait, loading...", Toast.LENGTH_SHORT).show()
                    return@setOnTapArPlaneListener
                }

                if (plane.trackingState == com.google.ar.core.TrackingState.TRACKING) {
                    Log.d(TAG, "🎯 Placing object...")
                    placeObjectInAR(hitResult)
                } else {
                    Toast.makeText(this, "⚠️ Surface not ready, try again", Toast.LENGTH_SHORT).show()
                }
            }

            // Alternative tap handler - herhangi bir yere tap için
            arFragment.arSceneView.setOnTouchListener { _, motionEvent ->
                if (motionEvent.action == android.view.MotionEvent.ACTION_DOWN) {
                    Log.d(TAG, "🖱️ Screen tapped at: ${motionEvent.x}, ${motionEvent.y}")

                    if (!isImageReady) {
                        Toast.makeText(this, "⏳ Still loading...", Toast.LENGTH_SHORT).show()
                        return@setOnTouchListener true
                    }

                    // Emergency placement - screen center'a koy
                    val frame = arFragment.arSceneView.arFrame
                    if (frame != null) {
                        val hits = frame.hitTest(motionEvent)
                        if (hits.isNotEmpty()) {
                            Log.d(TAG, "🎯 Emergency hit test successful")
                            placeObjectInAR(hits[0])
                        } else {
                            Log.d(TAG, "⚠️ No hits found, creating anchor at camera")
                            createAnchorAtCamera()
                        }
                    }
                }
                false
            }

            Toast.makeText(this, "👀 Look for white dots on flat surfaces", Toast.LENGTH_LONG).show()
            Log.d(TAG, "✅ AR Fragment setup completed")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting up AR Fragment: ${e.message}")
            Toast.makeText(this, "AR setup error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createAnchorAtCamera() {
        try {
            Log.d(TAG, "🎥 Creating anchor at camera position...")
            val session = arFragment.arSceneView.session
            val frame = arFragment.arSceneView.arFrame

            if (session != null && frame != null) {
                val camera = frame.camera
                val pose = camera.pose.compose(com.google.ar.core.Pose.makeTranslation(0f, 0f, -1f))
                val anchor = session.createAnchor(pose)
                Log.d(TAG, "🎯 Camera anchor created")
                addObjectToScene(anchor)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create camera anchor: ${e.message}")
        }
    }

    private fun placeObjectInAR(hitResult: HitResult) {
        try {
            Log.d(TAG, "🎯 Creating anchor from hit result...")
            val anchor = hitResult.createAnchor()
            addObjectToScene(anchor)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to place object: ${e.message}")
            Toast.makeText(this, "Failed to place object: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addObjectToScene(anchor: Anchor) {
        Log.d(TAG, "🏗️ Adding object to scene...")

        if (cachedBitmap != null) {
            Log.d(TAG, "🖼️ Creating image object...")
            createImageObject(anchor)
        } else {
            Log.d(TAG, "📝 Creating text object...")
            createTextObject(anchor)
        }
    }

    private fun createImageObject(anchor: Anchor) {
        try {
            Log.d(TAG, "🖼️ Building image renderable...")

            val imageView = ImageView(this).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(600, 600)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageBitmap(cachedBitmap)
                setBackgroundColor(Color.WHITE)
                setPadding(20, 20, 20, 20)
            }

            ViewRenderable.builder()
                .setView(this, imageView)
                .build()
                .thenAccept { renderable ->
                    Log.d(TAG, "✅ Image renderable created!")
                    runOnUiThread {
                        addNodeToScene(anchor, renderable, "🖼️ Image placed!")
                    }
                }
                .exceptionally { throwable ->
                    Log.e(TAG, "❌ Image renderable failed: ${throwable.message}")
                    runOnUiThread {
                        createTextObject(anchor) // Fallback
                    }
                    null
                }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception creating image object: ${e.message}")
            createTextObject(anchor) // Fallback
        }
    }

    private fun createTextObject(anchor: Anchor) {
        try {
            Log.d(TAG, "📝 Building text renderable...")

            val textView = TextView(this).apply {
                text = "🎨 AI ART 🎨\n\n${caption ?: "Generated Image"}\n\n(Image failed to load)"
                textSize = 20f
                setTextColor(Color.BLACK)
                setBackgroundColor(Color.WHITE)
                setPadding(40, 40, 40, 40)
                gravity = android.view.Gravity.CENTER
            }

            ViewRenderable.builder()
                .setView(this, textView)
                .build()
                .thenAccept { renderable ->
                    Log.d(TAG, "✅ Text renderable created!")
                    runOnUiThread {
                        addNodeToScene(anchor, renderable, "📝 Text placed!")
                    }
                }
                .exceptionally { throwable ->
                    Log.e(TAG, "❌ Text renderable failed: ${throwable.message}")
                    runOnUiThread {
                        Toast.makeText(this@ARActivity, "❌ AR object creation failed completely", Toast.LENGTH_LONG).show()
                    }
                    null
                }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception creating text object: ${e.message}")
            Toast.makeText(this, "❌ Complete failure: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun addNodeToScene(anchor: Anchor, renderable: ViewRenderable, successMessage: String) {
        try {
            Log.d(TAG, "🔗 Adding node to scene...")

            val anchorNode = AnchorNode(anchor).apply {
                setParent(arFragment.arSceneView.scene)
            }

            val transformableNode = TransformableNode(arFragment.transformationSystem).apply {
                this.renderable = renderable
                setParent(anchorNode)
                localScale = Vector3(1.0f, 1.0f, 1.0f) // Normal boyut
                localPosition = Vector3(0f, 0.2f, 0f) // Biraz yukarı
            }

            transformableNode.select()

            Log.d(TAG, "✅ Node added successfully!")
            Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show()

            // Plane discovery'yi gizle
            arFragment.planeDiscoveryController.hide()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to add node: ${e.message}")
            Toast.makeText(this, "❌ Failed to add AR object: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "📱 Activity resumed")
    }

    override fun onPause() {
        super.onPause()
        arFragment.arSceneView.pause()
        Log.d(TAG, "⏸️ Activity paused")
    }

    override fun onDestroy() {
        super.onDestroy()
        arFragment.arSceneView.destroy()
        cachedBitmap?.recycle()
        Log.d(TAG, "🗑️ Activity destroyed")
    }
}