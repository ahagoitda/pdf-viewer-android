package com.pdfutility.presentation.state

import android.graphics.Bitmap
import android.net.Uri
import com.pdfutility.domain.model.ConversionResult

data class SplitPdfState(
    val sourceUri: Uri? = null,
    val sourceName: String = "",
    val pageCount: Int = 0,
    val selectedPages: Set<Int> = emptySet(),
    val pageRangeInput: String = "",
    val thumbnailBitmaps: Map<Int, Bitmap> = emptyMap(),
    val isProcessing: Boolean = false,
    val outputFileName: String = "",
    val result: ConversionResult? = null,
    val error: String? = null,
)
