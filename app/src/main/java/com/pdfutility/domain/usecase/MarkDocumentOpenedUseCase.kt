package com.pdfutility.domain.usecase

import com.pdfutility.domain.model.PdfDocument
import com.pdfutility.domain.repository.DocumentRepository
import javax.inject.Inject

class MarkDocumentOpenedUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(document: PdfDocument) {
        repository.markDocumentOpened(document)
    }
}
