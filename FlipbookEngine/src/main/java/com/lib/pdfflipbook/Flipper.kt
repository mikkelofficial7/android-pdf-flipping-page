package com.lib.pdfflipbook

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.RestrictTo
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.lib.flipbookengine.R
import com.lib.flipbookengine.databinding.LayoutFlipperBinding
import com.lib.pdfflipbook.enums.ZoomState
import com.lib.pdfflipbook.listener.PageRunningListener
import com.lib.pdfflipbook.pdf.PdfClass
import com.lib.pdfflipbook.pdf.PdfClassImpl

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@SuppressLint("Recycle", "CustomViewStyleable")
class Flipper @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs), PageRunningListener {
    private var showTitleMode: Boolean = false
    private var titleDismissTime: Int = 0
    private var actualContentHeight = 0
    private var actualContentWidth = 0
    private var currentZoomState: ZoomState

    private var pageRunningListener: PageRunningListener = this
    private var listData: ArrayList<Bitmap> = arrayListOf()

    private val binding: LayoutFlipperBinding = LayoutFlipperBinding.inflate(LayoutInflater.from(context), this, true)
    private val pdfEngine: PdfClass = PdfClassImpl()
    init {
        val attr = context.obtainStyledAttributes(attrs, R.styleable.PdfView)
        showTitleMode = attr.getBoolean(R.styleable.PdfView_showTitleMode, false)
        titleDismissTime = attr.getInt(R.styleable.PdfView_titleDismissMilliSecond, 0)

        currentZoomState = ZoomState.NO_ZOOM
        getProportionalContentSize()
        getCurrentZoomState()
        showHideButtonZoom(false)
    }

    fun readPdfFile(context: Context, pdfUri: Uri) {
        val listBitmap = pdfEngine.readPdfFile(context, pdfUri)
        setDataToReader(listBitmap)
    }

    fun setTimerTitleDismiss(titleDismissMilliSecond: Int) {
        titleDismissTime = titleDismissMilliSecond

        if (titleDismissTime <= 0) return
        Handler(Looper.getMainLooper()).postDelayed({
            binding.tvPdfMode.isVisible = false
        }, titleDismissTime.toLong())
    }

    fun showHidePdfTitleMode(isShow: Boolean) {
        showTitleMode = isShow
        binding.tvPdfMode.isVisible = showTitleMode && listData.isNotEmpty()

        if (!showTitleMode) return
        binding.tvPdfMode.text = when(currentZoomState) {
            ZoomState.NO_ZOOM -> context.getString(R.string.enter_read_mode)
            else -> context.getString(R.string.enter_zoom_mode)
        }
    }


    private fun showHideResetZoomButton(isShow: Boolean) {
        binding.btnResetZoom.isVisible = isShow
    }

    private fun setDataToReader(data: List<Bitmap>) {
        listData.clear()
        listData.addAll(data)

        if (listData.isEmpty()) return
        if (listData.size == 1) listData.add(pdfEngine.createDefaultWhiteImage(600, 600))

        currentZoomState = ZoomState.NO_ZOOM
        getCurrentZoomState()
        handleZoomAction()
        showHideButtonZoom(true)

        binding.paperFoldView.setData(listData, pageRunningListener)
    }

    private fun handleZoomAction() {
        binding.btnZoomIn.setOnClickListener {
            currentZoomState = ZoomState.ZOOM_IN
            getCurrentZoomState()
        }
        binding.btnZoomOut.setOnClickListener {
            currentZoomState = ZoomState.ZOOM_OUT
            getCurrentZoomState()
        }
        binding.btnResetZoom.setOnClickListener {
            currentZoomState = ZoomState.NO_ZOOM
            getCurrentZoomState()
        }
    }

    private fun getCurrentZoomState() {
        when (currentZoomState) {
            ZoomState.NO_ZOOM -> {
                showHideZoomButton(true)
                showHideResetZoomButton(false)
                showHidePdfTitleMode(showTitleMode)
                setTimerTitleDismiss(titleDismissTime)
                binding.paperFoldView.visibility = View.VISIBLE
            }
            ZoomState.ZOOM_IN -> {
                showHideZoomButton(false)
                showHideResetZoomButton(true)
                showHidePdfTitleMode(showTitleMode)
                setTimerTitleDismiss(titleDismissTime)
                binding.paperFoldView.visibility = View.INVISIBLE
            }
            ZoomState.ZOOM_OUT -> {
                showHideZoomButton(false)
                showHideResetZoomButton(true)
                showHidePdfTitleMode(showTitleMode)
                setTimerTitleDismiss(titleDismissTime)
                binding.paperFoldView.visibility = View.INVISIBLE
            }
        }
    }

    private fun getProportionalContentSize() {
        binding.layoutShadow.viewTreeObserver.addOnGlobalLayoutListener {
            actualContentHeight = binding.layoutShadow.height
            actualContentWidth = binding.layoutShadow.width

            binding.paperFoldView.setActualContentSize(actualContentHeight, actualContentWidth)
        }
    }

    private fun showHideButtonZoom(isShow: Boolean) {
        binding.layoutZoom.isVisible = isShow
    }

    private fun showHideZoomButton(isShow: Boolean) {
        binding.btnZoomIn.isVisible = isShow
        binding.btnZoomOut.isVisible = isShow
        binding.dividerZoom.isVisible = isShow
    }

    override fun onCurrentPageRunning(activePage: Bitmap?) {
        binding.layoutShadow.setImageBitmap(activePage)
        binding.layoutActivePage.setImageBitmap(activePage)
    }
}