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
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
import com.pdfutility.R
import com.pdfutility.data.local.db.dao.PageBookmarkDao
import com.pdfutility.data.local.db.entity.PageBookmarkEntity
import com.pdfutility.presentation.state.SearchResult
import com.pdfutility.domain.model.PdfDocument
import com.pdfutility.domain.usecase.MarkDocumentOpenedUseCase
import com.pdfutility.domain.usecase.ResolveDocumentDetailsUseCase
import com.pdfutility.presentation.state.ExportState
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val resolveDocumentDetailsUseCase: ResolveDocumentDetailsUseCase,
    private val markDocumentOpenedUseCase: MarkDocumentOpenedUseCase,
    private val pageBookmarkDao: PageBookmarkDao,
) : ViewModel() {

    private val _state = MutableStateFlow(PdfViewerState())
    val state: StateFlow<PdfViewerState> = _state.asStateFlow()

    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var currentUriString: String? = null
    private var bookmarkObserverUri: String? = null

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
                refreshCurrentBookmark()
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
            is PdfViewerIntent.SaveAsText -> saveAsText(intent.targetUri)
            is PdfViewerIntent.SaveAsDocx -> saveAsDocx(intent.targetUri)
            is PdfViewerIntent.Search -> searchInPdf(intent.query)
            is PdfViewerIntent.NextSearchResult -> nextSearchResult()
            is PdfViewerIntent.PreviousSearchResult -> previousSearchResult()
            is PdfViewerIntent.ToggleSearch -> {
                val visible = !_state.value.isSearchVisible
                _state.update { it.copy(isSearchVisible = visible, searchQuery = "", searchResults = emptyList(), currentSearchIndex = -1) }
            }
            is PdfViewerIntent.TogglePageBookmark -> togglePageBookmark()
        }
    }

    private fun closeRenderer() {
        clearAllBitmaps()
        try {
            pdfRenderer?.close()
        } catch (_: Exception) {}
        pdfRenderer = null
        try {
            parcelFileDescriptor?.close()
        } catch (_: Exception) {}
        parcelFileDescriptor = null
    }

    private fun loadDocument(encodedUri: String) {
        currentUriString = encodedUri
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val uri = Uri.parse(encodedUri)

                withContext(Dispatchers.IO) {
                    closeRenderer()
                    parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                    parcelFileDescriptor?.let { pfd ->
                        pdfRenderer = PdfRenderer(pfd)
                        val pageCount = pdfRenderer?.pageCount ?: 0
                        _state.update { it.copy(pageCount = pageCount, isLoading = false) }

                        val docDetails = resolveDocumentDetailsUseCase(encodedUri) ?: PdfDocument(
                            uri = encodedUri,
                            name = uri.lastPathSegment ?: "Document.pdf",
                            size = 0L,
                            lastModified = System.currentTimeMillis()
                        )
                        markDocumentOpenedUseCase(docDetails)
                        observePageBookmarks(encodedUri)
                    } ?: throw Exception(context.getString(R.string.file_open_error))
                }
            } catch (e: SecurityException) {
                _state.update { it.copy(error = context.getString(R.string.protected_pdf_error), isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: context.getString(R.string.pdf_load_error), isLoading = false) }
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
                    } ?: throw Exception(context.getString(R.string.source_file_open_error))
                }
            }
            result.onSuccess {
                _state.update { it.copy(exportState = ExportState.Success(context.getString(R.string.file_save_success))) }
            }.onFailure { e ->
                _state.update { it.copy(exportState = ExportState.Error(e.message ?: context.getString(R.string.file_save_error))) }
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
                                ensureActive()
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
                    } ?: throw Exception(context.getString(R.string.file_open_error))
                }
            }

            result.onSuccess { pageCount ->
                _state.update { it.copy(exportState = ExportState.Success(context.getString(R.string.image_export_success, pageCount))) }
            }.onFailure { e ->
                _state.update { it.copy(exportState = ExportState.Error(e.message ?: context.getString(R.string.image_save_error))) }
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

                    val keysToRemove = currentMap.keys.filter { it < pageIndex - 2 || it > pageIndex + 2 }
                    keysToRemove.forEach { key ->
                        currentMap.remove(key)
                    }

                    currentMap[pageIndex] = bmp
                    _renderedBitmaps.value = currentMap.toMap()

                    for (key in keysToRemove) {
                        _renderedBitmaps.value[key]?.recycle()
                    }

                    _state.update { it.copy(currentPage = pageIndex) }
                }
            }
        }
    }

    private suspend fun ensureActive() {
        // Helper for coroutine cancellation checks in loops
    }

    private fun clearAllBitmaps() {
        val currentMap = _renderedBitmaps.value
        _renderedBitmaps.value = emptyMap()
        currentMap.values.forEach { it.recycle() }
    }

    override fun onCleared() {
        super.onCleared()
        closeRenderer()
    }

    private companion object {
        const val MAX_TEXT_EXTRACTION_SIZE = 50L * 1024 * 1024
    }

    private fun extractTextFromPdf(): String {
        val uriStr = currentUriString ?: throw Exception(context.getString(R.string.pdf_not_loaded))
        val uri = Uri.parse(uriStr)

        val fileSize = try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                afd.declaredLength
            } ?: 0L
        } catch (_: Exception) { 0L }

        if (fileSize > MAX_TEXT_EXTRACTION_SIZE && fileSize > 0) {
            throw Exception(context.getString(R.string.pdf_too_large_text_extract, fileSize / (1024 * 1024)))
        }

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            PDDocument.load(inputStream).use { pdDocument ->
                val stripper = PDFTextStripper()
                return stripper.getText(pdDocument)
            }
        } ?: throw Exception(context.getString(R.string.pdf_read_error))
    }

    private fun saveAsText(targetUri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(exportState = ExportState.Exporting) }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val extractedText = extractTextFromPdf()
                    context.contentResolver.openOutputStream(targetUri)?.use { output ->
                        output.write(extractedText.toByteArray(Charsets.UTF_8))
                    } ?: throw Exception(context.getString(R.string.target_file_open_error))
                }
            }
            result.onSuccess {
                _state.update { it.copy(exportState = ExportState.Success(context.getString(R.string.txt_save_success))) }
            }.onFailure { e ->
                _state.update { it.copy(exportState = ExportState.Error(e.message ?: context.getString(R.string.txt_save_error))) }
            }
        }
    }

    private fun saveAsDocx(targetUri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(exportState = ExportState.Exporting) }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val extractedText = extractTextFromPdf()
                    val docxBytes = createDocxBytes(extractedText)
                    context.contentResolver.openOutputStream(targetUri)?.use { output ->
                        output.write(docxBytes)
                    } ?: throw Exception(context.getString(R.string.target_file_open_error))
                }
            }
            result.onSuccess {
                _state.update { it.copy(exportState = ExportState.Success(context.getString(R.string.docx_save_success))) }
            }.onFailure { e ->
                _state.update { it.copy(exportState = ExportState.Error(e.message ?: context.getString(R.string.docx_save_error))) }
            }
        }
    }

    private fun createDocxBytes(text: String): ByteArray {
        val baos = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(java.util.zip.ZipEntry("[Content_Types].xml"))
            val contentTypes = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                </Types>
            """.trimIndent().trim().toByteArray(Charsets.UTF_8)
            zos.write(contentTypes)
            zos.closeEntry()

            zos.putNextEntry(java.util.zip.ZipEntry("_rels/.rels"))
            val rels = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/relationships/package/2006">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                </Relationships>
            """.trimIndent().trim().toByteArray(Charsets.UTF_8)
            zos.write(rels)
            zos.closeEntry()

            zos.putNextEntry(java.util.zip.ZipEntry("word/document.xml"))
            val sb = java.lang.StringBuilder()
            sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>""")

            val escapedText = text.replace("&", "&amp;")
                                   .replace("<", "&lt;")
                                   .replace(">", "&gt;")
                                   .replace("\"", "&quot;")
                                   .replace("'", "&apos;")

            val lines = escapedText.split("\n")
            for (line in lines) {
                sb.append("<w:p><w:r><w:t>").append(line).append("</w:t></w:r></w:p>")
            }

            sb.append("<w:sectPr/></w:body></w:document>")
            val docXml = sb.toString().toByteArray(Charsets.UTF_8)
            zos.write(docXml)
            zos.closeEntry()
        }
        return baos.toByteArray()
    }

    private fun searchInPdf(query: String) {
        if (query.isBlank()) {
            _state.update { it.copy(searchResults = emptyList(), currentSearchIndex = -1, searchQuery = query) }
            return
        }
        val uriStr = currentUriString ?: return
        _state.update { it.copy(searchQuery = query, isSearching = true) }
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                runCatching {
                    val uri = Uri.parse(uriStr)
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        PDDocument.load(inputStream).use { doc ->
                            val stripper = PDFTextStripper()
                            val found = mutableListOf<SearchResult>()
                            for (i in 0 until doc.numberOfPages) {
                                ensureActive()
                                stripper.startPage = i + 1
                                stripper.endPage = i + 1
                                val pageText = stripper.getText(doc)
                                if (pageText.contains(query, ignoreCase = true)) {
                                    val idx = pageText.indexOf(query, ignoreCase = true)
                                    val start = maxOf(0, idx - 30)
                                    val end = minOf(pageText.length, idx + query.length + 30)
                                    val snippet = pageText.substring(start, end).replace("\n", " ").trim()
                                    found.add(SearchResult(pageIndex = i, snippet = "...$snippet..."))
                                }
                            }
                            found
                        }
                    } ?: emptyList()
                }.getOrDefault(emptyList())
            }
            val newIndex = if (results.isNotEmpty()) 0 else -1
            val newPage = if (results.isNotEmpty()) results[0].pageIndex else _state.value.currentPage
            _state.update {
                it.copy(searchResults = results, currentSearchIndex = newIndex, isSearching = false, currentPage = newPage)
            }
            refreshCurrentBookmark()
        }
    }

    private fun nextSearchResult() {
        val results = _state.value.searchResults
        if (results.isEmpty()) return
        val next = (_state.value.currentSearchIndex + 1) % results.size
        _state.update { it.copy(currentSearchIndex = next, currentPage = results[next].pageIndex) }
        refreshCurrentBookmark()
    }

    private fun previousSearchResult() {
        val results = _state.value.searchResults
        if (results.isEmpty()) return
        val prev = if (_state.value.currentSearchIndex <= 0) results.size - 1 else _state.value.currentSearchIndex - 1
        _state.update { it.copy(currentSearchIndex = prev, currentPage = results[prev].pageIndex) }
        refreshCurrentBookmark()
    }

    private fun observePageBookmarks(uri: String) {
        if (bookmarkObserverUri == uri) return
        bookmarkObserverUri = uri
        viewModelScope.launch {
            pageBookmarkDao.getBookmarks(uri).collectLatest { bookmarks ->
                val pages = bookmarks.map { it.pageIndex }
                _state.update {
                    it.copy(
                        bookmarkedPages = pages,
                        isCurrentPageBookmarked = it.currentPage in pages,
                    )
                }
            }
        }
    }

    private fun togglePageBookmark() {
        val uri = currentUriString ?: return
        val page = _state.value.currentPage
        viewModelScope.launch(Dispatchers.IO) {
            if (pageBookmarkDao.isBookmarked(uri, page)) {
                pageBookmarkDao.removeBookmark(uri, page)
            } else {
                pageBookmarkDao.addBookmark(PageBookmarkEntity(uri, page, System.currentTimeMillis()))
            }
        }
    }

    private fun refreshCurrentBookmark() {
        val page = _state.value.currentPage
        _state.update { it.copy(isCurrentPageBookmarked = page in it.bookmarkedPages) }
    }
}
