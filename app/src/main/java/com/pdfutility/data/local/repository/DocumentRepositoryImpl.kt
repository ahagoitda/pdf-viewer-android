package com.pdfutility.data.local.repository

import com.pdfutility.data.local.db.dao.RecentDocumentDao
import com.pdfutility.data.local.fileio.DocumentFileDataSource
import com.pdfutility.data.mapper.toDomain
import com.pdfutility.data.mapper.toEntity
import com.pdfutility.domain.model.PdfDocument
import com.pdfutility.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val fileDataSource: DocumentFileDataSource,
    private val recentDocumentDao: RecentDocumentDao,
) : DocumentRepository {

    override suspend fun getPdfDocuments(): Result<List<PdfDocument>> {
        return fileDataSource.queryPdfFiles()
    }

    override suspend fun deleteDocument(uri: String): Result<Unit> {
        val result = fileDataSource.deleteDocument(uri)
        if (result.isSuccess) {
            recentDocumentDao.deleteByUri(uri)
        }
        return result
    }

    override fun getRecentDocuments(): Flow<List<PdfDocument>> {
        return recentDocumentDao.getRecentDocuments()
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun markDocumentOpened(document: PdfDocument) {
        recentDocumentDao.insertDocument(document.toEntity())
    }

    override suspend fun resolveDocumentDetails(uri: String): PdfDocument? {
        return fileDataSource.queryDocumentDetails(uri)
    }
}
