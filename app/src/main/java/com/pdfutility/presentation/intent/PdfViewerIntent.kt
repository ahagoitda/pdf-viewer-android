package com.pdfutility.presentation.intent

import android.net.Uri

sealed interface PdfViewerIntent {
    data class LoadDocument(val uri: String) : PdfViewerIntent
    data class SetZoom(val level: Float) : PdfViewerIntent
    data class GoToPage(val page: Int) : PdfViewerIntent
    data class RenderPage(val pageIndex: Int, val width: Int, val height: Int) : PdfViewerIntent
    data object ClearRenderedPages : PdfViewerIntent
    data class SaveAsFile(val targetUri: Uri) : PdfViewerIntent
    data object ExportAsImages : PdfViewerIntent
    data object DismissExportState : PdfViewerIntent
    data class SaveAsText(val targetUri: Uri) : PdfViewerIntent
    data class SaveAsDocx(val targetUri: Uri) : PdfViewerIntent
}
