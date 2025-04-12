# PaperPDF - Simple PDF Reader with paper motion style 📁

<div align="left">
  <img src="https://github.com/mikkelofficial7/android-pdf-flipping-page/blob/main/sample1.jpg" alt="Flipbook Engine" width="300" height="400">
  <img src="https://github.com/mikkelofficial7/android-pdf-flipping-page/blob/main/sample.gif" alt="Flipbook Engine" width="200" height="400">
</div>

Latest stable version: 

[![](https://jitpack.io/v/mikkelofficial7/android-pdf-flipping-page.svg)](https://github.com/mikkelofficial7/android-pdf-flipping-page/releases/tag/v1.3.2)

How to use (Sample demo provided):

1. Add this gradle in ```build.gradle(:app)``` :
```
dependencies {
   implementation 'com.github.mikkelofficial7:android-pdf-flipping-page:v1.3.2'
}
 ```
or gradle.kts:
```
dependencies {
   implementation("com.github.mikkelofficial7:android-pdf-flipping-page:v1.3.2")
}
 ```

2. Add it in your root settings.gradle at the end of repositories:
```
repositories {
  mavenCentral()
  maven { url 'https://jitpack.io' }
}
```

3. Put this XML in your ```activity_main.xml```
```
<com.lib.pdfflipbook.PdfFlipBook
        android:id="@+id/flipView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:showTitleMode="true"/> <!-- true or false -->
```

4. Access Pdf Viewer in you ```Activity``` or ```Fragment``` class
```
binding.flipView.readPdfFile(context, uri)
binding.flipView.setTimerTitleDismiss(2000) // in millisecond, set 0 to undismissable
binding.flipView.showHidePdfMode(false) // show title reader/zoom mode (true or false)
```

5. Add this permission to ```AndroidManifest.xml```

```
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
```

6. Add this also inside ```<activity>``` tag

```
<intent-filter>
    <data android:mimeType="application/pdf" />
    <data android:scheme="content" />
    <data android:scheme="file" />
</intent-filter>
```
