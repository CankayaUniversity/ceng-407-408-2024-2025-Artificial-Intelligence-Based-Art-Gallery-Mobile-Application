package com.example.socialmediaapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.socialmediaapp.R

class PostPreviewBottomSheet(private val imageUrl: String) : BottomSheetDialogFragment() {

    private var isExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_post_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageView = view.findViewById<ImageView>(R.id.post_image)
        val overlay = view.findViewById<View>(R.id.overlay)

        // Load the image with Glide
        Glide.with(requireContext()).load(imageUrl).into(imageView)

        // Initially, the image is not expanded
        if (isExpanded) {
            imageView.scaleX = 1f
            imageView.scaleY = 1f
        } else {
            // Expand the image and blur the background
            imageView.scaleX = 2f
            imageView.scaleY = 2f
            overlay.visibility = View.VISIBLE
        }

        // Toggle between expanding and shrinking the image when clicked
        imageView.setOnClickListener {
            isExpanded = !isExpanded
            if (isExpanded) {
                // Expand the image
                imageView.scaleX = 2f
                imageView.scaleY = 2f
                overlay.visibility = View.VISIBLE
            } else {
                // Shrink the image back to original size
                imageView.scaleX = 1f
                imageView.scaleY = 1f
                overlay.visibility = View.GONE
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                bottomSheet.setBackgroundResource(android.R.color.transparent)
            }
        }
    }
}