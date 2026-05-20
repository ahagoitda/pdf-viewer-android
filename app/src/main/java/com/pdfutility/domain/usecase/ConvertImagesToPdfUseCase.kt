package com.pdfutility.domain.usecase

import android.net.Uri
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.domain.repository.ConversionRepository
import javax.inject.Inject

class ConvertImagesToPdfUseCase @Inject constructor(
    private val conversionRepository: ConversionRepository,
) {
    suspend operator fun invoke(
        images: List<Uri>,
        outputFileName: String,
    ): ConversionResult {
        return conversionRepository.convertImagesToPdf(images, outputFileName)
    }
}
