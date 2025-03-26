package com.lib.pdfflipbook

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.lib.flipbookengine.databinding.LayoutFlipperBinding

class PdfFlipBook @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {
    private var listData: ArrayList<Bitmap> = arrayListOf()
    private val binding: LayoutFlipperBinding = LayoutFlipperBinding.inflate(LayoutInflater.from(context), this, true)

    private fun setDataToReader(data: List<Bitmap>) {
        listData.clear()
        listData.addAll(data)

        if (listData.isEmpty()) return
        if (listData.size == 1) listData.add(createWhiteBitmap(600, 600))

        binding.paperFoldView.setData(listData)
    }

    fun readPdfFile(context: Context, pdfUri: Uri) {
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

    private fun createWhiteBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE) // Fill with white color
        return bitmap
    }
}