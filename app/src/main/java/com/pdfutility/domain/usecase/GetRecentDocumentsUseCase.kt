package com.pdfutility.domain.usecase

import com.pdfutility.domain.model.PdfDocument
import com.pdfutility.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentDocumentsUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
) {
    operator fun invoke(): Flow<List<PdfDocument>> {
        return documentRepository.getRecentDocuments()
    }
}
