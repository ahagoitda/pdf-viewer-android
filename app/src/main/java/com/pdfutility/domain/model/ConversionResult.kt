package com.pdfutility.domain.model

sealed class ConversionResult {
    data class Success(
        val outputPath: String,
        val outputName: String,
        val pageCount: Int,
        val totalSize: Long,
    ) : ConversionResult()

    data class Error(
        val message: String,
        val throwable: Throwable? = null,
    ) : ConversionResult()
}
