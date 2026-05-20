package com.pdfutility.presentation.intent

import android.net.Uri
import com.pdfutility.presentation.state.ImageItem

sealed interface ImageToPdfIntent {
    data class SelectImages(val images: List<ImageItem>) : ImageToPdfIntent
    data class SetOutputName(val name: String) : ImageToPdfIntent
    data object StartConversion : ImageToPdfIntent
    data object Reset : ImageToPdfIntent
    data object DismissResult : ImageToPdfIntent
}
