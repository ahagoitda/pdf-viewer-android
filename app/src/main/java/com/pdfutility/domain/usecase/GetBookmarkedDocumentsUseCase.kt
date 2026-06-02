package com.pdfutility.domain.usecase

import com.pdfutility.domain.model.PdfDocument
import com.pdfutility.domain.repository.DocumentRepository
import javax.inject.Inject

class GetBookmarkedDocumentsUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
) {
    operator fun invoke() = documentRepository.getBookmarkedDocuments()
}