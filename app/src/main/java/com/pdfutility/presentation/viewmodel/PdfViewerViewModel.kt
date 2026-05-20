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
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import com.pdfutility.domain.model.PdfDocument
import com.pdfutility.domain.usecase.MarkDocumentOpenedUseCase
import com.pdfutility.domain.usecase.ResolveDocumentDetailsUseCase
import com.pdfutility.presentation.state.ExportState

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
    private var currentUriString: String? = null
    
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
            is PdfViewerIntent.SaveAsFile -> saveAsFile(intent.targetUri)
            is PdfViewerIntent.ExportAsImages -> exportAsImages()
            is PdfViewerIntent.DismissExportState -> {
                _state.update { it.copy(exportState = ExportState.Idle) }
            }
        }
    }

    private fun loadDocument(encodedUri: String) {
        currentUriString = encodedUri
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val uri = Uri.parse(encodedUri)
                
                withContext(Dispatchers.IO) {
                    parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                    parcelFileDescriptor?.let { pfd ->
                        pdfRenderer = PdfRenderer(pfd)
                        val pageCount = pdfRenderer?.pageCount ?: 0
                        _state.update { it.copy(pageCount = pageCount, isLoading = false) }
                        
                        // Resolve document details and record in recent history
                        val docDetails = resolveDocumentDetailsUseCase(encodedUri) ?: PdfDocument(
                            uri = encodedUri,
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

    private fun saveAsFile(targetUri: Uri) {
        val uriStr = currentUriString ?: return
        viewModelScope.launch {
            _state.update { it.copy(exportState = ExportState.Exporting) }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val sourceUri = Uri.parse(uriStr)
                    context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        context.contentResolver.openOutputStream(targetUri)?.use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw Exception("원본 파일을 열 수 없습니다.")
                }
            }
            result.onSuccess {
                _state.update { it.copy(exportState = ExportState.Success("파일이 성공적으로 저장되었습니다.")) }
            }.onFailure { e ->
                _state.update { it.copy(exportState = ExportState.Error(e.message ?: "파일 저장 중 오류가 발생했습니다.")) }
            }
        }
    }

    private fun exportAsImages() {
        val uriStr = currentUriString ?: return
        viewModelScope.launch {
            _state.update { it.copy(exportState = ExportState.Exporting) }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val uri = Uri.parse(uriStr)
                    val contentResolver = context.contentResolver
                    
                    val baseName = resolveDocumentDetailsUseCase(uriStr)?.name?.removeSuffix(".pdf")
                        ?: "document_${System.currentTimeMillis()}"
                        
                    contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            val pageCount = renderer.pageCount
                            
                            for (i in 0 until pageCount) {
                                val page = renderer.openPage(i)
                                val targetWidth = 1500
                                val targetHeight = (page.height * (targetWidth.toFloat() / page.width)).toInt()
                                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                page.close()
                                
                                val displayName = "${baseName}_page_${i + 1}.jpg"
                                val values = ContentValues().apply {
                                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PdfUtility/$baseName")
                                        put(MediaStore.Images.Media.IS_PENDING, 1)
                                    }
                                }
                                
                                val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                                if (imageUri != null) {
                                    contentResolver.openOutputStream(imageUri)?.use { out ->
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                    }
                                    
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        values.clear()
                                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                                        contentResolver.update(imageUri, values, null, null)
                                    }
                                }
                                bitmap.recycle()
                            }
                            pageCount
                        }
                    } ?: throw Exception("파일을 열 수 없습니다.")
                }
            }
            
            result.onSuccess { pageCount ->
                _state.update { it.copy(exportState = ExportState.Success("${pageCount}장의 이미지가 갤러리(Pictures/PdfUtility)에 저장되었습니다.")) }
            }.onFailure { e ->
                _state.update { it.copy(exportState = ExportState.Error(e.message ?: "이미지 저장 중 오류가 발생했습니다.")) }
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
