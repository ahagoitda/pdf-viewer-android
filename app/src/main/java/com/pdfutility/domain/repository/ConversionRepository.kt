package com.pdfutility.domain.repository

import android.net.Uri
import com.pdfutility.domain.model.ImagePdfOptions
import com.pdfutility.domain.model.ConversionResult

interface ConversionRepository {
    suspend fun convertImagesToPdf(
        images: List<Uri>,
        outputFileName: String,
        options: ImagePdfOptions,
    ): ConversionResult
}
