package com.pdfutility

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PdfUtilityApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Defensive initialization for AdMob
            MobileAds.initialize(this) { status ->
                Log.d("PdfUtilityApp", "AdMob Initialized: $status")
            }
        } catch (e: Exception) {
            Log.e("PdfUtilityApp", "Failed to initialize AdMob", e)
        }
    }
}
