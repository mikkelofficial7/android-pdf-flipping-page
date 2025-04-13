package com.lib.pdfflipbook.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class PdfClassImpl: PdfClass {
    override fun readPdfFile(context: Context, pdfUri: Uri): ArrayList<Bitmap> {
        val bitmapList = ArrayList<Bitmap>()
        val contentResolver = context.contentResolver

        try {
            val fileDescriptor = contentResolver.openFileDescriptor(pdfUri, "r") ?: return arrayListOf()
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

        return bitmapList
    }

    override fun createDefaultWhiteImage(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE) // Fill with white color
        return bitmap
    }
}