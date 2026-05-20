package com.pdfutility.presentation.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfutility.presentation.intent.PdfViewerIntent
import com.pdfutility.presentation.state.PdfViewerState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import com.pdfutility.domain.model.PdfDocument
import com.pdfutility.domain.usecase.MarkDocumentOpenedUseCase
import com.pdfutility.domain.usecase.ResolveDocumentDetailsUseCase

@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val resolveDocumentDetailsUseCase: ResolveDocumentDetailsUseCase,
    private val markDocumentOpenedUseCase: MarkDocumentOpenedUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(PdfViewerState())
    val state: StateFlow<PdfViewerState> = _state.asStateFlow()

    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    
    private val _renderedBitmaps = MutableStateFlow<Map<Int, Bitmap>>(emptyMap())
    val renderedBitmaps: StateFlow<Map<Int, Bitmap>> = _renderedBitmaps.asStateFlow()

    private val rendererMutex = Mutex()

    fun onIntent(intent: PdfViewerIntent) {
        when (intent) {
            is PdfViewerIntent.LoadDocument -> loadDocument(intent.uri)
            is PdfViewerIntent.SetZoom -> {
                _state.update { it.copy(zoomLevel = intent.level.coerceIn(1f, 5f)) }
            }
            is PdfViewerIntent.GoToPage -> {
                _state.update { it.copy(currentPage = intent.page) }
            }
            is PdfViewerIntent.RenderPage -> {
                renderPage(intent.pageIndex, intent.width, intent.height)
            }
            is PdfViewerIntent.ClearRenderedPages -> clearAllBitmaps()
        }
    }

    private fun loadDocument(encodedUri: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val decodedUri = URLDecoder.decode(encodedUri, StandardCharsets.UTF_8.toString())
                val uri = Uri.parse(decodedUri)
                
                withContext(Dispatchers.IO) {
                    parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                    parcelFileDescriptor?.let { pfd ->
                        pdfRenderer = PdfRenderer(pfd)
                        val pageCount = pdfRenderer?.pageCount ?: 0
                        _state.update { it.copy(pageCount = pageCount, isLoading = false) }
                        
                        // Resolve document details and record in recent history
                        val docDetails = resolveDocumentDetailsUseCase(decodedUri) ?: PdfDocument(
                            uri = decodedUri,
                            name = uri.lastPathSegment ?: "Document.pdf",
                            size = 0L,
                            lastModified = System.currentTimeMillis()
                        )
                        markDocumentOpenedUseCase(docDetails)
                    } ?: throw Exception("파일을 열 수 없습니다.")
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "PDF를 불러오는 중 오류가 발생했습니다.", isLoading = false) }
            }
        }
    }

    private fun renderPage(pageIndex: Int, width: Int, height: Int) {
        if (pageIndex < 0 || pageIndex >= (_state.value.pageCount)) return
        if (_renderedBitmaps.value.containsKey(pageIndex)) return

        viewModelScope.launch {
            rendererMutex.withLock {
                if (_renderedBitmaps.value.containsKey(pageIndex)) return@withLock
                
                val bitmap = withContext(Dispatchers.Default) {
                    try {
                        pdfRenderer?.let { renderer ->
                            val page = renderer.openPage(pageIndex)
                            // Calculate height based on aspect ratio if not provided or to maintain quality
                            val scale = width.toFloat() / page.width
                            val targetHeight = (page.height * scale).toInt()
                            
                            val bmp = Bitmap.createBitmap(width, targetHeight, Bitmap.Config.ARGB_8888)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                            bmp
                        }
                    } catch (e: Exception) {
                        null
                    }
                }

                bitmap?.let { bmp ->
                    val currentMap = _renderedBitmaps.value.toMutableMap()
                    
                    // Keep only 5 pages around the current one to save memory
                    val keysToRemove = currentMap.keys.filter { it < pageIndex - 2 || it > pageIndex + 2 }
                    keysToRemove.forEach { key ->
                        currentMap[key]?.recycle()
                        currentMap.remove(key)
                    }
                    
                    currentMap[pageIndex] = bmp
                    _renderedBitmaps.value = currentMap
                    _state.update { it.copy(currentPage = pageIndex) }
                }
            }
        }
    }

    private fun clearAllBitmaps() {
        val currentMap = _renderedBitmaps.value
        currentMap.values.forEach { it.recycle() }
        _renderedBitmaps.value = emptyMap()
    }

    override fun onCleared() {
        super.onCleared()
        clearAllBitmaps()
        try {
            pdfRenderer?.close()
            parcelFileDescriptor?.close()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
