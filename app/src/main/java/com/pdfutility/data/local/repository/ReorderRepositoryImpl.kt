package com.pdfutility.data.local.repository

import android.net.Uri
import com.pdfutility.data.local.fileio.PdfReorderDataSource
import com.pdfutility.data.local.fileio.PdfSplitDataSource
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.domain.repository.ReorderRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReorderRepositoryImpl @Inject constructor(
    private val pdfReorderDataSource: PdfReorderDataSource,
    private val pdfSplitDataSource: PdfSplitDataSource,
) : ReorderRepository {

    override suspend fun reorderPages(
        sourceUri: Uri,
        newOrder: List<Int>,
        outputFileName: String,
    ): ConversionResult {
        return pdfReorderDataSource.reorderPages(sourceUri, newOrder, outputFileName)
    }

    override suspend fun getPageCount(sourceUri: Uri): Int {
        return pdfSplitDataSource.getPageCount(sourceUri)
    }
}