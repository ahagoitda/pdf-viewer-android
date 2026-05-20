package com.pdfutility.domain.model

data class PdfDocument(
    val uri: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val pageCount: Int? = null,
)
