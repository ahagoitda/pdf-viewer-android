package com.pdfutility.data.mapper

import com.pdfutility.data.local.db.entity.BookmarkEntity
import com.pdfutility.data.local.db.entity.RecentDocumentEntity
import com.pdfutility.domain.model.PdfDocument

fun PdfDocument.toEntity(lastOpened: Long = System.currentTimeMillis()): RecentDocumentEntity {
    return RecentDocumentEntity(
        uri = uri,
        name = name,
        size = size,
        lastOpened = lastOpened,
        lastModified = lastModified,
    )
}

fun RecentDocumentEntity.toDomain(): PdfDocument {
    return PdfDocument(
        uri = uri,
        name = name,
        size = size,
        lastModified = lastModified,
    )
}

fun BookmarkEntity.toDomain(): PdfDocument {
    return PdfDocument(
        uri = uri,
        name = name,
        size = size,
        lastModified = bookmarkedAt,
    )
}