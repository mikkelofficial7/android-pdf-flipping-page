package com.view.pdfflipbook

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.lib.flipbookengineandroid.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.pbLoad.isVisible = true

        val uri: Uri? = intent?.data
        if (uri != null) {
            binding.pbLoad.isVisible = false
            binding.flipView.readPdfFile(this, uri)
        } else {
            binding.pbLoad.isVisible = false
        }
    }
}