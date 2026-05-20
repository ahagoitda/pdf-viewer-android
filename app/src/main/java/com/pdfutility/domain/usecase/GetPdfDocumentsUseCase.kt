package com.pdfutility.domain.usecase

import com.pdfutility.domain.model.PdfDocument
import com.pdfutility.domain.repository.DocumentRepository
import javax.inject.Inject

class GetPdfDocumentsUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
) {
    suspend operator fun invoke(): Result<List<PdfDocument>> {
        return documentRepository.getPdfDocuments()
    }
}
