package com.lib.pdfflipbook

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.lib.flipbookengine.R
import com.lib.flipbookengine.databinding.LayoutPdfFlipperBookBinding

@SuppressLint("Recycle", "CustomViewStyleable")
class PdfFlipBook @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {
    private var showTitleMode: Boolean = false
    private var titleDismissTime: Int = 0
    private val binding: LayoutPdfFlipperBookBinding = LayoutPdfFlipperBookBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        val attr = context.obtainStyledAttributes(attrs, R.styleable.PdfView)
        showTitleMode = attr.getBoolean(R.styleable.PdfView_showTitleMode, false)
        titleDismissTime = attr.getInt(R.styleable.PdfView_titleDismissMilliSecond, 0)

        binding.flipBook.setTimerTitleDismiss(titleDismissTime)
        binding.flipBook.showHidePdfTitleMode(showTitleMode)
    }

    fun readPdfFile(context: Context, pdfUri: Uri) {
        binding.flipBook.readPdfFile(context, pdfUri)
    }

    fun setTimerTitleDismiss(titleDismissMilliSecond: Int) {
        binding.flipBook.setTimerTitleDismiss(titleDismissMilliSecond)
    }

    fun showHidePdfTitleMode(isShow: Boolean) {
        binding.flipBook.showHidePdfTitleMode(isShow)
    }
}