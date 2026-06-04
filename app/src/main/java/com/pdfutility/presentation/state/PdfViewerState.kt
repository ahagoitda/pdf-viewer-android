package com.pdfutility.presentation.state

data class SearchResult(val pageIndex: Int, val snippet: String)

data class PdfViewerState(
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val zoomLevel: Float = 1f,
    val isRendering: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val exportState: ExportState = ExportState.Idle,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val currentSearchIndex: Int = -1,
    val isSearchVisible: Boolean = false,
    val isSearching: Boolean = false,
    val bookmarkedPages: List<Int> = emptyList(),
    val isCurrentPageBookmarked: Boolean = false,
)

sealed interface ExportState {
    data object Idle : ExportState
    data object Exporting : ExportState
    data class Success(val message: String) : ExportState
    data class Error(val error: String) : ExportState
}
