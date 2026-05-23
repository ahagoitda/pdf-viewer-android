package com.pdfutility.domain.usecase

import android.net.Uri
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.domain.repository.ConversionRepository
import javax.inject.Inject

class ConvertFileToPdfUseCase @Inject constructor(
    private val repository: ConversionRepository,
) {
    suspend operator fun invoke(uri: Uri, mimeType: String?): ConversionResult {
        return repository.convertFileToPdf(uri, mimeType)
    }
}
