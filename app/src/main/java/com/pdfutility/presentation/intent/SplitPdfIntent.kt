package com.pdfutility.presentation.intent

import android.net.Uri

sealed interface SplitPdfIntent {
    data class LoadPdf(val uri: Uri) : SplitPdfIntent
    data class TogglePage(val pageIndex: Int) : SplitPdfIntent
    data object SelectAll : SplitPdfIntent
    data object DeselectAll : SplitPdfIntent
    data class SetPageRange(val range: String) : SplitPdfIntent
    data object ApplyPageRange : SplitPdfIntent
    data class SetOutputName(val name: String) : SplitPdfIntent
    data object StartSplit : SplitPdfIntent
    data object Reset : SplitPdfIntent
    data object DismissResult : SplitPdfIntent
}
