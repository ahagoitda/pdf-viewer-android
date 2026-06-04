package com.pdfutility.presentation.state

import android.net.Uri
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.domain.model.ImagePdfOptions

data class ImageToPdfState(
    val selectedImages: List<ImageItem> = emptyList(),
    val outputFileName: String = "",
    val options: ImagePdfOptions = ImagePdfOptions(),
    val isConverting: Boolean = false,
    val conversionProgress: Float = 0f,
    val conversionResult: ConversionResult? = null,
    val error: String? = null,
)

data class ImageItem(
    val uri: Uri,
    val displayName: String,
    val size: Long,
)
