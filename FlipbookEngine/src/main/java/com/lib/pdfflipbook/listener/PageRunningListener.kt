package com.lib.pdfflipbook.listener

import android.graphics.Bitmap

interface PageRunningListener {
    fun onCurrentPageRunning(activePage: Bitmap?)
}