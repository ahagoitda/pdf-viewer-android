package com.pdfutility.domain.usecase

import android.net.Uri
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.domain.repository.SplitRepository
import javax.inject.Inject

class SplitPdfUseCase @Inject constructor(
    private val splitRepository: SplitRepository,
) {
    suspend operator fun invoke(
        sourceUri: Uri,
        pageIndices: List<Int>,
        outputFileName: String,
    ): ConversionResult {
        return splitRepository.splitPdf(sourceUri, pageIndices, outputFileName)
    }

    suspend fun getPageCount(sourceUri: Uri): Int {
        return splitRepository.getPageCount(sourceUri)
    }
}