package com.pdfutility.presentation.intent

import com.pdfutility.presentation.state.ImageItem

sealed interface ImageToPdfIntent {
    data class SelectImages(val images: List<ImageItem>) : ImageToPdfIntent
    data class RemoveImage(val uri: android.net.Uri) : ImageToPdfIntent
    data class MoveImage(val fromIndex: Int, val toIndex: Int) : ImageToPdfIntent
    data class SetOutputName(val name: String) : ImageToPdfIntent
    data object StartConversion : ImageToPdfIntent
    data object Reset : ImageToPdfIntent
    data object DismissResult : ImageToPdfIntent
}