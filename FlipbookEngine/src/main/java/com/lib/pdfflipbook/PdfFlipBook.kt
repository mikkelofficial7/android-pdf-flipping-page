package com.lib.pdfflipbook

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.lib.flipbookengine.databinding.LayoutFlipperBinding
import com.lib.pdfflipbook.listener.PageRunningListener

class PdfFlipBook @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs), PageRunningListener {
    private var actualContentHeight = 0
    private var actualContentWidth = 0
    private var currentZoomState: ZoomState
    private lateinit var pageRunningListener: PageRunningListener
    private var listData: ArrayList<Bitmap> = arrayListOf()
    private val binding: LayoutFlipperBinding = LayoutFlipperBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        currentZoomState = ZoomState.NO_ZOOM
        getProportionalContentSize()
        getCurrentZoomState()
        hideButtonZoom()
    }

    fun readPdfFile(context: Context, pdfUri: Uri) {
        pageRunningListener = this

        val bitmapList = ArrayList<Bitmap>()
        val contentResolver = context.contentResolver

        try {
            val fileDescriptor = contentResolver.openFileDescriptor(pdfUri, "r") ?: return
            val pdfRenderer = PdfRenderer(fileDescriptor)

            for (i in 0 until pdfRenderer.pageCount) {
                val page = pdfRenderer.openPage(i)

                val bitmap = Bitmap.createBitmap(
                    page.width, page.height, Bitmap.Config.ARGB_8888
                )

                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE) // Set background to white
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                bitmapList.add(bitmap)
                page.close()
            }

            pdfRenderer.close()
            fileDescriptor.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        setDataToReader(bitmapList)
    }

    private fun setDataToReader(data: List<Bitmap>) {
        listData.clear()
        listData.addAll(data)

        if (listData.isEmpty()) return
        if (listData.size == 1) listData.add(createDefaultWhiteImage(600, 600))

        getCurrentZoomState()
        showButtonZoom()
        handleZoomAction()

        binding.paperFoldView.setData(listData, pageRunningListener)
    }

    private fun createDefaultWhiteImage(width: Int = 10, height: Int = 10): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE) // Fill with white color
        return bitmap
    }

    private fun showButtonZoom() {
        binding.layoutZoom.isVisible = true
    }

    private fun hideButtonZoom() {
        binding.layoutZoom.isVisible = false
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
                binding.paperFoldView.visibility = View.VISIBLE
            }
            ZoomState.ZOOM_IN -> {
                showHideZoomButton(false)
                showHideResetZoomButton(true)
                binding.paperFoldView.visibility = View.INVISIBLE
            }
            ZoomState.ZOOM_OUT -> {
                showHideZoomButton(false)
                showHideResetZoomButton(true)
                binding.paperFoldView.visibility = View.INVISIBLE
            }
        }
    }

    private fun getProportionalContentSize() {
        binding.layoutActivePage.viewTreeObserver.addOnGlobalLayoutListener {
            actualContentHeight = binding.layoutActivePage.height
            actualContentWidth = binding.layoutActivePage.width

            binding.paperFoldView.setActualContentSize(actualContentHeight, actualContentWidth)
        }
    }

    private fun showHideResetZoomButton(isShow: Boolean) {
        binding.btnResetZoom.isVisible = isShow
    }

    private fun showHideZoomButton(isShow: Boolean) {
        binding.btnZoomIn.isVisible = isShow
        binding.btnZoomOut.isVisible = isShow
        binding.dividerZoom.isVisible = isShow
    }

    override fun onCurrentPageRunning(activePage: Bitmap?) {
        binding.layoutActivePage.setImageBitmap(activePage)
    }
}

enum class ZoomState {
    NO_ZOOM,
    ZOOM_IN,
    ZOOM_OUT
}