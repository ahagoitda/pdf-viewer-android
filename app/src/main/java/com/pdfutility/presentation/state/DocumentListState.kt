package com.pdfutility.presentation.state

import com.pdfutility.domain.model.PdfDocument

data class DocumentListState(
    val documents: List<PdfDocument> = emptyList(),
    val recentDocuments: List<PdfDocument> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val permissionGranted: Boolean = false,
    val isConverting: Boolean = false,
    val conversionError: String? = null,
)
