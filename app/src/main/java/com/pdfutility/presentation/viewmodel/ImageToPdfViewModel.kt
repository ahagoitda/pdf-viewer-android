package com.pdfutility.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.pdfutility.BuildConfig
import com.pdfutility.domain.usecase.ConvertImagesToPdfUseCase
import com.pdfutility.presentation.intent.ImageToPdfIntent
import com.pdfutility.presentation.state.ImageToPdfState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageToPdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val convertImagesToPdfUseCase: ConvertImagesToPdfUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ImageToPdfState())
    val state: StateFlow<ImageToPdfState> = _state.asStateFlow()

    private var interstitialAd: InterstitialAd? = null

    init {
        loadInterstitialAd()
    }

    fun onIntent(intent: ImageToPdfIntent) {
        when (intent) {
            is ImageToPdfIntent.SelectImages -> {
                _state.update { it.copy(selectedImages = intent.images) }
            }
            is ImageToPdfIntent.SetOutputName -> {
                _state.update { it.copy(outputFileName = intent.name) }
            }
            is ImageToPdfIntent.StartConversion -> startConversion()
            is ImageToPdfIntent.Reset -> {
                _state.value = ImageToPdfState()
                loadInterstitialAd()
            }
            is ImageToPdfIntent.DismissResult -> {
                _state.update { it.copy(conversionResult = null) }
            }
        }
    }

    private fun startConversion() {
        val images = _state.value.selectedImages.map { it.uri }
        val name = _state.value.outputFileName.ifBlank { "pdf_${System.currentTimeMillis()}" }

        if (images.isEmpty()) {
            _state.update { it.copy(error = "이미지를 최소 1장 이상 선택해주세요.") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isConverting = true, conversionProgress = 0f, error = null) }
            
            // Note: For true granular progress, the UseCase/Repository would need to support a progress callback.
            // For now, we update to 0.5 when starting and 1.0 when finished as a simple indicator.
            _state.update { it.copy(conversionProgress = 0.5f) }
            
            val result = convertImagesToPdfUseCase(images, name)
            
            _state.update {
                it.copy(
                    isConverting = false,
                    conversionResult = result,
                    conversionProgress = 1f
                )
            }
            
            // Show ad if conversion is successful
            if (result is com.pdfutility.domain.model.ConversionResult.Success) {
                // Interstitial ad should be shown on the UI thread or handled by the activity
                // Here we just make sure it's loaded. The UI can check and show it.
            }
        }
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
            }
        )
    }
    
    fun getInterstitialAd(): InterstitialAd? = interstitialAd
}
