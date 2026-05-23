package com.pdfutility.presentation.intent

import android.net.Uri
import com.pdfutility.domain.model.PdfDocument

sealed interface DocumentListIntent {
    data object LoadDocuments : DocumentListIntent
    data class DeleteDocument(val uri: String) : DocumentListIntent
    data class OpenDocument(val document: PdfDocument) : DocumentListIntent
    data object RequestPermission : DocumentListIntent
    data class ConvertAndOpenFile(val uri: Uri, val mimeType: String?) : DocumentListIntent
}
