package com.pdfutility

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PdfUtilityApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Initialize PDFBox
            PDFBoxResourceLoader.init(this)
            
            // Defensive initialization for AdMob
            MobileAds.initialize(this) { status ->
                Log.d("PdfUtilityApp", "AdMob Initialized: $status")
            }
        } catch (e: Exception) {
            Log.e("PdfUtilityApp", "Failed to initialize app systems", e)
        }
    }
}
