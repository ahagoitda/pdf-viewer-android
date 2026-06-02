package com.pdfutility.presentation.intent

import com.pdfutility.presentation.state.SelectedPdf

sealed interface MergePdfIntent {
    data class AddPdfs(val pdfs: List<SelectedPdf>) : MergePdfIntent
    data class RemovePdf(val uri: android.net.Uri) : MergePdfIntent
    data class MovePdf(val fromIndex: Int, val toIndex: Int) : MergePdfIntent
    data class SetOutputName(val name: String) : MergePdfIntent
    data object StartMerge : MergePdfIntent
    data object Reset : MergePdfIntent
    data object DismissResult : MergePdfIntent
}
