package com.view.pdfflipbook

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.view.pdfflipbook.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val pdfPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            hideSelector()
            binding.flipView.readPdfFile(this, uri)
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        changeStatusBarColor(window, R.color.white)

        val uri: Uri? = intent?.data
        if (uri != null) {
            hideSelector()
            binding.flipView.readPdfFile(this, uri)
        }

        binding.flipView.showHidePdfTitleMode(true)
        binding.flipView.setTimerTitleDismiss(6000)

        binding.btnUploadPdf.setOnClickListener {
            pdfPickerLauncher.launch(arrayOf("application/pdf"))
        }
    }

    private fun changeStatusBarColor(window: Window, colorResId: Int) {
        val color = ContextCompat.getColor(window.context, colorResId)
        window.statusBarColor = color

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                if (colorResId == android.R.color.white)
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                else
                    0
        }
    }

    private fun hideSelector() {
        binding.btnUploadPdf.isVisible = false
        binding.tvTitle.isVisible = false
    }
}