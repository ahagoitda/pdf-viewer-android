package com.pdfutility

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class PdfUtilityApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            PDFBoxResourceLoader.init(this)

            if (BuildConfig.ADMOB_APP_ID.isNotBlank()) {
                MobileAds.initialize(this) { status ->
                    Log.d("PdfUtilityApp", "AdMob Initialized: $status")
                }
            }

            cleanOldOutputFiles()
        } catch (e: Exception) {
            Log.e("PdfUtilityApp", "Failed to initialize app systems", e)
        }
    }

    private fun cleanOldOutputFiles() {
        try {
            val outputDir = File(filesDir, "pdf_output")
            if (!outputDir.exists()) return
            val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
            outputDir.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < sevenDaysAgo) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }
}