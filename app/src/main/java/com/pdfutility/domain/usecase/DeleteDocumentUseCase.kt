package com.pdfutility.domain.usecase

import com.pdfutility.domain.repository.DocumentRepository
import javax.inject.Inject

class DeleteDocumentUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
) {
    suspend operator fun invoke(uri: String): Result<Unit> {
        return documentRepository.deleteDocument(uri)
    }
}
