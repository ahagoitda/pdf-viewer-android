package com.pdfutility.presentation.intent

import android.net.Uri

sealed interface ReorderPagesIntent {
    data class LoadPdf(val uri: Uri) : ReorderPagesIntent
    data class MovePage(val fromIndex: Int, val toIndex: Int) : ReorderPagesIntent
    data class SetOutputName(val name: String) : ReorderPagesIntent
    data object StartReorder : ReorderPagesIntent
    data object Reset : ReorderPagesIntent
    data object DismissResult : ReorderPagesIntent
}