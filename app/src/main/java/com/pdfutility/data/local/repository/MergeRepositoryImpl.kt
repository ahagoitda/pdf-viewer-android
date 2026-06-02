package com.pdfutility.data.local.repository

import android.net.Uri
import com.pdfutility.data.local.fileio.PdfMergeDataSource
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.domain.repository.MergeRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MergeRepositoryImpl @Inject constructor(
    private val pdfMergeDataSource: PdfMergeDataSource,
) : MergeRepository {

    override suspend fun mergePdfs(
        pdfUris: List<Uri>,
        outputFileName: String,
        onProgress: (Float) -> Unit,
    ): ConversionResult {
        return pdfMergeDataSource.mergePdfs(pdfUris, outputFileName, onProgress)
    }
}
