package com.pdfutility.domain.repository

import android.net.Uri
import com.pdfutility.domain.model.ConversionResult

interface SplitRepository {
    suspend fun splitPdf(
        sourceUri: Uri,
        pageIndices: List<Int>,
        outputFileName: String,
    ): ConversionResult

    suspend fun getPageCount(sourceUri: Uri): Int
}
