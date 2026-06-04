package com.pdfutility.presentation.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfutility.R
import com.pdfutility.data.local.fileio.PdfSplitDataSource
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.domain.usecase.ReorderPagesUseCase
import com.pdfutility.presentation.intent.ReorderPagesIntent
import com.pdfutility.presentation.state.ReorderPagesState
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
class ReorderPagesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reorderPagesUseCase: ReorderPagesUseCase,
    private val pdfSplitDataSource: PdfSplitDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(ReorderPagesState())
    val state: StateFlow<ReorderPagesState> = _state.asStateFlow()

    private val thumbnailCache = ConcurrentHashMap<Int, Bitmap>()

    private companion object {
        const val MAX_THUMBNAILS = 20
    }

    fun onIntent(intent: ReorderPagesIntent) {
        when (intent) {
            is ReorderPagesIntent.LoadPdf -> loadPdf(intent.uri)
            is ReorderPagesIntent.MovePage -> movePage(intent.fromIndex, intent.toIndex)
            is ReorderPagesIntent.SetOutputName -> setOutputName(intent.name)
            is ReorderPagesIntent.StartReorder -> startReorder()
            is ReorderPagesIntent.Reset -> reset()
            is ReorderPagesIntent.DismissResult -> dismissResult()
        }
    }

    private fun loadPdf(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(sourceUri = uri, isProcessing = true, error = null) }
            val pageCount = reorderPagesUseCase.getPageCount(uri)
            if (pageCount <= 0) {
                _state.update { it.copy(isProcessing = false, error = context.getString(R.string.pdf_page_read_error)) }
                return@launch
            }
            val name = queryDisplayName(uri) ?: "document"
            _state.update {
                it.copy(
                    pageCount = pageCount,
                    sourceName = name,
                    outputFileName = name.removeSuffix(".pdf") + "_reordered",
                    pageOrder = (0 until pageCount).toList(),
                    isProcessing = false,
                )
            }
            for (i in 0 until pageCount) {
                ensureActive()
                val thumbnail = pdfSplitDataSource.renderPageThumbnail(uri, i, 120)
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

    private fun movePage(fromIndex: Int, toIndex: Int) {
        val current = _state.value.pageOrder.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _state.update { it.copy(pageOrder = current) }
    }

    private fun setOutputName(name: String) {
        _state.update { it.copy(outputFileName = name) }
    }

    private fun startReorder() {
        val uri = _state.value.sourceUri ?: return
        val order = _state.value.pageOrder
        val name = _state.value.outputFileName.ifBlank { "reordered_${System.currentTimeMillis()}" }

        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isProcessing = true, error = null) }
            val result = reorderPagesUseCase(uri, order, name)
            _state.update { it.copy(isProcessing = false, result = result) }
        }
    }

    private fun reset() {
        thumbnailCache.values.forEach { it.recycle() }
        thumbnailCache.clear()
        _state.value = ReorderPagesState()
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
