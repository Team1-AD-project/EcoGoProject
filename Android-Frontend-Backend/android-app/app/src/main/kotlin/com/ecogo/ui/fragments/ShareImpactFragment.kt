package com.ecogo.ui.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecogo.R
import com.ecogo.data.MascotSize
import com.ecogo.databinding.FragmentShareImpactBinding
import java.io.File
import java.io.FileOutputStream

/**
 * Share impact page
 * Generates share cards and shares to social platforms
 */
class ShareImpactFragment : Fragment() {

    companion object {
        private const val PERIOD_TODAY = "today"
        private const val PERIOD_WEEK = "week"
        private const val PERIOD_MONTH = "month"
        private const val COLOR_PRIMARY = "#059669"
        private const val COLOR_SECONDARY = "#6B7280"
        private const val COLOR_BG = "#ECFDF5"
    }

    private var _binding: FragmentShareImpactBinding? = null
    private val binding get() = _binding!!

    private var selectedPeriod = PERIOD_TODAY

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShareImpactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        loadStatistics()
        setupAnimations()
    }
    
    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Period selection
        binding.chipToday.setOnClickListener { selectPeriod(PERIOD_TODAY) }
        binding.chipWeek.setOnClickListener { selectPeriod(PERIOD_WEEK) }
        binding.chipMonth.setOnClickListener { selectPeriod(PERIOD_MONTH) }
        
        // Share button
        binding.btnShare.setOnClickListener {
            shareImpact()
        }
        
        // Save image button
        binding.btnSave.setOnClickListener {
            saveToGallery()
        }
    }
    
    private fun selectPeriod(period: String) {
        selectedPeriod = period
        
        // Update chip selection state
        binding.chipToday.isChecked = period == PERIOD_TODAY
        binding.chipWeek.isChecked = period == PERIOD_WEEK
        binding.chipMonth.isChecked = period == PERIOD_MONTH
        
        loadStatistics()
    }
    
    private fun loadStatistics() {
        // Load different statistics based on selected period (mock)
        when (selectedPeriod) {
            PERIOD_TODAY -> {
                binding.textPeriod.text = "Today's Impact"
                binding.textTrips.text = "3"
                binding.textDistance.text = "5.2"
                binding.textCarbonSaved.text = "580"
                binding.textPoints.text = "290"
            }
            PERIOD_WEEK -> {
                binding.textPeriod.text = "This Week's Impact"
                binding.textTrips.text = "15"
                binding.textDistance.text = "24.5"
                binding.textCarbonSaved.text = "2,750"
                binding.textPoints.text = "1,375"
            }
            PERIOD_MONTH -> {
                binding.textPeriod.text = "This Month's Impact"
                binding.textTrips.text = "52"
                binding.textDistance.text = "98.3"
                binding.textCarbonSaved.text = "11,200"
                binding.textPoints.text = "5,600"
            }
        }
        
        // Set mascot outfit (should be fetched from user data)
        binding.mascotShare.apply {
            mascotSize = MascotSize.LARGE
            setEmotion(com.ecogo.data.MascotEmotion.CELEBRATING)
            celebrateAnimation()
        }
    }
    
    private fun shareImpact() {
        // Generate share image
        val bitmap = generateShareCard()
        
        // Save to temp file
        val file = File(requireContext().cacheDir, "ecogo_impact_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        
        // Create share Intent
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "My green travel achievements on EcoGo! #EcoGo #GreenTravel")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        startActivity(Intent.createChooser(shareIntent, "Share to"))
    }
    
    private fun saveToGallery() {
        // TODO: Implement save to gallery functionality
        android.widget.Toast.makeText(
            requireContext(),
            "Image saved to gallery",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun generateShareCard(): Bitmap {
        // Create share card Bitmap (800x600)
        val width = 800
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val primaryColor = Color.parseColor(COLOR_PRIMARY)
        val secondaryColor = Color.parseColor(COLOR_SECONDARY)

        // Background
        val bgPaint = Paint().apply {
            color = Color.parseColor(COLOR_BG)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Title
        val titlePaint = Paint().apply {
            color = primaryColor
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("My Green Travel", width / 2f, 80f, titlePaint)

        // Period
        val periodPaint = Paint().apply {
            color = secondaryColor
            textSize = 32f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(binding.textPeriod.text.toString(), width / 2f, 130f, periodPaint)

        // Statistics
        val labelPaint = Paint().apply {
            color = secondaryColor
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        val valuePaint = Paint().apply {
            color = primaryColor
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        
        // Trip count
        canvas.drawText("Trips", width / 4f, 250f, labelPaint)
        canvas.drawText(binding.textTrips.text.toString(), width / 4f, 300f, valuePaint)

        // Distance
        canvas.drawText("Distance (km)", width * 3 / 4f, 250f, labelPaint)
        canvas.drawText(binding.textDistance.text.toString(), width * 3 / 4f, 300f, valuePaint)

        // Carbon reduction
        canvas.drawText("CO2 Saved (g)", width / 4f, 400f, labelPaint)
        canvas.drawText(binding.textCarbonSaved.text.toString(), width / 4f, 450f, valuePaint)

        // Points
        canvas.drawText("Points Earned", width * 3 / 4f, 400f, labelPaint)
        canvas.drawText(binding.textPoints.text.toString(), width * 3 / 4f, 450f, valuePaint)
        
        // Footer
        val footerPaint = Paint().apply {
            color = primaryColor
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("EcoGo - Green Travel Together", width / 2f, 550f, footerPaint)
        
        return bitmap
    }
    
    private fun setupAnimations() {
        val popIn = AnimationUtils.loadAnimation(requireContext(), R.anim.pop_in)
        binding.cardPreview.startAnimation(popIn)
        
        val slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        binding.cardStats.startAnimation(slideUp)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
