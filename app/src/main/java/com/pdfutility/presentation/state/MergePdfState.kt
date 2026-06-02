package com.pdfutility.presentation.state

import android.net.Uri
import com.pdfutility.domain.model.ConversionResult

data class MergePdfState(
    val selectedPdfs: List<SelectedPdf> = emptyList(),
    val outputFileName: String = "",
    val isMerging: Boolean = false,
    val mergeProgress: Float = 0f,
    val mergeResult: ConversionResult? = null,
    val error: String? = null,
)

data class SelectedPdf(
    val uri: Uri,
    val name: String,
    val size: Long,
)
