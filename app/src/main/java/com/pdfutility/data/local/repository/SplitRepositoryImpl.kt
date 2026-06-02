package com.pdfutility.data.local.repository

import android.net.Uri
import com.pdfutility.data.local.fileio.PdfSplitDataSource
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.domain.repository.SplitRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SplitRepositoryImpl @Inject constructor(
    private val pdfSplitDataSource: PdfSplitDataSource,
) : SplitRepository {

    override suspend fun splitPdf(
        sourceUri: Uri,
        pageIndices: List<Int>,
        outputFileName: String,
    ): ConversionResult {
        return pdfSplitDataSource.splitPdf(sourceUri, pageIndices, outputFileName)
    }

    override suspend fun getPageCount(sourceUri: Uri): Int {
        return pdfSplitDataSource.getPageCount(sourceUri)
    }
}