package com.pdfutility.domain.repository

import android.net.Uri
import com.pdfutility.domain.model.ConversionResult

interface ReorderRepository {
    suspend fun reorderPages(
        sourceUri: Uri,
        newOrder: List<Int>,
        outputFileName: String,
    ): ConversionResult

    suspend fun getPageCount(sourceUri: Uri): Int
}