package com.pdfutility.presentation.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfutility.R
import com.pdfutility.data.local.fileio.PdfSplitDataSource
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.domain.usecase.SplitPdfUseCase
import com.pdfutility.presentation.intent.SplitPdfIntent
import com.pdfutility.presentation.state.SplitPdfState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class SplitPdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val splitPdfUseCase: SplitPdfUseCase,
    private val pdfSplitDataSource: PdfSplitDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(SplitPdfState())
    val state: StateFlow<SplitPdfState> = _state.asStateFlow()

    private val thumbnailCache = ConcurrentHashMap<Int, Bitmap>()

    private companion object {
        const val MAX_THUMBNAILS = 20
    }

    fun onIntent(intent: SplitPdfIntent) {
        when (intent) {
            is SplitPdfIntent.LoadPdf -> loadPdf(intent.uri)
            is SplitPdfIntent.TogglePage -> togglePage(intent.pageIndex)
            is SplitPdfIntent.SelectAll -> selectAll()
            is SplitPdfIntent.DeselectAll -> deselectAll()
            is SplitPdfIntent.SetOutputName -> setOutputName(intent.name)
            is SplitPdfIntent.StartSplit -> startSplit()
            is SplitPdfIntent.Reset -> reset()
            is SplitPdfIntent.DismissResult -> dismissResult()
        }
    }

    private fun loadPdf(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(sourceUri = uri, isProcessing = true, error = null) }
            val pageCount = splitPdfUseCase.getPageCount(uri)
            if (pageCount <= 0) {
                _state.update { it.copy(isProcessing = false, error = context.getString(R.string.pdf_page_read_error)) }
                return@launch
            }

            val name = queryDisplayName(uri) ?: "document"
            _state.update {
                it.copy(
                    pageCount = pageCount,
                    sourceName = name,
                    outputFileName = name.removeSuffix(".pdf"),
                    isProcessing = false,
                )
            }

            for (i in 0 until pageCount) {
                ensureActive()
                val thumbnail = pdfSplitDataSource.renderPageThumbnail(uri, i, 150)
                thumbnail?.let { bmp ->
                    if (thumbnailCache.size >= MAX_THUMBNAILS) {
                        val oldest = thumbnailCache.keys.minOrNull()
                        oldest?.let { thumbnailCache.remove(it)?.recycle() }
                    }
                    thumbnailCache[i] = bmp
                    _state.update { it.copy(thumbnailBitmaps = thumbnailCache.toMap()) }
                }
            }
        }
    }

    private fun togglePage(pageIndex: Int) {
        _state.update { s ->
            val current = s.selectedPages
            val updated = if (pageIndex in current) current - pageIndex else current + pageIndex
            s.copy(selectedPages = updated)
        }
    }

    private fun selectAll() {
        _state.update { s ->
            s.copy(selectedPages = (0 until s.pageCount).toSet())
        }
    }

    private fun deselectAll() {
        _state.update { it.copy(selectedPages = emptySet()) }
    }

    private fun setOutputName(name: String) {
        _state.update { it.copy(outputFileName = name) }
    }

    private fun startSplit() {
        val uri = _state.value.sourceUri ?: return
        val pages = _state.value.selectedPages.sorted()
        if (pages.isEmpty()) {
            _state.update { it.copy(error = context.getString(R.string.page_min_required)) }
            return
        }
        val name = _state.value.outputFileName.ifBlank { "split_${System.currentTimeMillis()}" }

        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isProcessing = true, error = null) }
            val result = splitPdfUseCase(uri, pages, name)
            _state.update { it.copy(isProcessing = false, result = result) }
        }
    }

    private fun reset() {
        thumbnailCache.values.forEach { it.recycle() }
        thumbnailCache.clear()
        _state.value = SplitPdfState()
    }

    private fun dismissResult() {
        _state.update { it.copy(result = null) }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) { null }
    }

    override fun onCleared() {
        super.onCleared()
        thumbnailCache.values.forEach { it.recycle() }
        thumbnailCache.clear()
    }
}
