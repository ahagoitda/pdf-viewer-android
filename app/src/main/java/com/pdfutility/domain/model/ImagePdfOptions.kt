package com.pdfutility.domain.model

enum class PdfPageSize {
    A4,
    Original,
}

enum class PdfPageOrientation {
    Portrait,
    Landscape,
}

data class ImagePdfOptions(
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val orientation: PdfPageOrientation = PdfPageOrientation.Portrait,
    val marginPt: Int = 0,
    val quality: Int = 90,
)
