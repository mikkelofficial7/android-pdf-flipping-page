package com.lib.pdfflipbook.pdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface PdfClass {
    fun readPdfFile(context: Context, pdfUri: Uri): ArrayList<Bitmap>

    fun createDefaultWhiteImage(width: Int, height: Int): Bitmap
}