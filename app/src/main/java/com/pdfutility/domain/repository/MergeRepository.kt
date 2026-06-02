package com.pdfutility.domain.repository

import android.net.Uri
import com.pdfutility.domain.model.ConversionResult

interface MergeRepository {
    suspend fun mergePdfs(
        pdfUris: List<Uri>,
        outputFileName: String,
        onProgress: (Float) -> Unit,
    ): ConversionResult
}
