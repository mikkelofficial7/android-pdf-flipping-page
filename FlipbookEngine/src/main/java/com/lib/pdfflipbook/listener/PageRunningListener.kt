package com.lib.pdfflipbook.listener

import android.graphics.Bitmap
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)

interface PageRunningListener {
    fun onCurrentPageRunning(activePage: Bitmap?)
}