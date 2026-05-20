package com.pdfutility.data.local.fileio

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.pdfutility.domain.model.PdfDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentFileDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val contentResolver: ContentResolver
        get() = context.contentResolver

    fun queryPdfFiles(): Result<List<PdfDocument>> = runCatching {
        val documents = mutableListOf<PdfDocument>()

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE,
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("application/pdf")

        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val size = cursor.getLong(sizeColumn)
                val lastModified = cursor.getLong(dateColumn) * 1000L
                val uri = ContentUris.withAppendedId(collection, id).toString()

                documents.add(
                    PdfDocument(
                        uri = uri,
                        name = name,
                        size = size,
                        lastModified = lastModified,
                    )
                )
            }
        }

        documents
    }

    fun deleteDocument(uriString: String): Result<Unit> = runCatching {
        val uri = Uri.parse(uriString)
        contentResolver.delete(uri, null, null)
    }

    fun resolveFileName(uriString: String): String? {
        val uri = Uri.parse(uriString)
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            } else null
        }
    }

    fun queryDocumentDetails(uriString: String): PdfDocument? {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file") {
            val file = java.io.File(uri.path ?: return null)
            if (file.exists()) {
                return PdfDocument(
                    uri = uriString,
                    name = file.name,
                    size = file.length(),
                    lastModified = file.lastModified()
                )
            }
        }
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                    
                    val name = if (nameColumn >= 0) cursor.getString(nameColumn) else "Document.pdf"
                    val size = if (sizeColumn >= 0) cursor.getLong(sizeColumn) else 0L
                    
                    var lastModified = System.currentTimeMillis()
                    val lastModColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                    if (lastModColumn >= 0) {
                        lastModified = cursor.getLong(lastModColumn) * 1000L
                    }
                    
                    PdfDocument(
                        uri = uriString,
                        name = name,
                        size = size,
                        lastModified = lastModified
                    )
                } else null
            }
        } catch (e: Exception) {
            val name = uri.lastPathSegment ?: "Document.pdf"
            PdfDocument(
                uri = uriString,
                name = name,
                size = 0L,
                lastModified = System.currentTimeMillis()
            )
        }
    }
}
