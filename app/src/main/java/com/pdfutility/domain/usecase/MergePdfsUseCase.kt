package com.pdfutility.domain.usecase

import android.net.Uri
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.domain.repository.MergeRepository
import javax.inject.Inject

class MergePdfsUseCase @Inject constructor(
    private val mergeRepository: MergeRepository,
) {
    suspend operator fun invoke(
        pdfUris: List<Uri>,
        outputFileName: String,
        onProgress: (Float) -> Unit = {},
    ): ConversionResult {
        return mergeRepository.mergePdfs(pdfUris, outputFileName, onProgress)
    }
}
