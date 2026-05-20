package com.pdfutility.domain.repository

import com.pdfutility.domain.model.PdfDocument
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    suspend fun getPdfDocuments(): Result<List<PdfDocument>>
    suspend fun deleteDocument(uri: String): Result<Unit>
    fun getRecentDocuments(): Flow<List<PdfDocument>>
    suspend fun markDocumentOpened(document: PdfDocument)
    suspend fun resolveDocumentDetails(uri: String): PdfDocument?
}
