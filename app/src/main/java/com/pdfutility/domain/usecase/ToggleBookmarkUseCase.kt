package com.pdfutility.domain.usecase

import com.pdfutility.domain.model.PdfDocument
import com.pdfutility.domain.repository.DocumentRepository
import javax.inject.Inject

class ToggleBookmarkUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
) {
    suspend operator fun invoke(document: PdfDocument): Boolean {
        return documentRepository.toggleBookmark(document)
    }
}