package com.pdfutility.domain.usecase

import android.net.Uri
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.domain.repository.ReorderRepository
import javax.inject.Inject

class ReorderPagesUseCase @Inject constructor(
    private val reorderRepository: ReorderRepository,
) {
    suspend operator fun invoke(
        sourceUri: Uri,
        newOrder: List<Int>,
        outputFileName: String,
    ): ConversionResult {
        return reorderRepository.reorderPages(sourceUri, newOrder, outputFileName)
    }

    suspend fun getPageCount(sourceUri: Uri): Int {
        return reorderRepository.getPageCount(sourceUri)
    }
}