package com.pdfutility.presentation.state

data class PdfViewerState(
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val zoomLevel: Float = 1f,
    val isRendering: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val exportState: ExportState = ExportState.Idle
)

sealed interface ExportState {
    data object Idle : ExportState
    data object Exporting : ExportState
    data class Success(val message: String) : ExportState
    data class Error(val error: String) : ExportState
}
