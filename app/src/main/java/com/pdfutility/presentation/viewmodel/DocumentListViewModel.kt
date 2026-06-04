package com.pdfutility.presentation.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfutility.R
import com.pdfutility.domain.model.PdfDocument
import com.pdfutility.domain.usecase.DeleteDocumentUseCase
import com.pdfutility.domain.usecase.GetBookmarkedDocumentsUseCase
import com.pdfutility.domain.usecase.GetPdfDocumentsUseCase
import com.pdfutility.domain.usecase.GetRecentDocumentsUseCase
import com.pdfutility.domain.usecase.MarkDocumentOpenedUseCase
import com.pdfutility.domain.usecase.ToggleBookmarkUseCase
import com.pdfutility.presentation.intent.DocumentListIntent
import com.pdfutility.presentation.state.DocumentListState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getPdfDocumentsUseCase: GetPdfDocumentsUseCase,
    private val getRecentDocumentsUseCase: GetRecentDocumentsUseCase,
    private val getBookmarkedDocumentsUseCase: GetBookmarkedDocumentsUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase,
    private val markDocumentOpenedUseCase: MarkDocumentOpenedUseCase,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DocumentListState())
    val state: StateFlow<DocumentListState> = _state.asStateFlow()

    init {
        checkPermission()
        observeRecentDocuments()
        observeBookmarks()
        if (_state.value.permissionGranted) {
            loadDocuments()
        }
    }

    fun onIntent(intent: DocumentListIntent) {
        when (intent) {
            is DocumentListIntent.LoadDocuments -> loadDocuments()
            is DocumentListIntent.DeleteDocument -> deleteDocument(intent.uri)
            is DocumentListIntent.OpenDocument -> openDocument(intent.document)
            is DocumentListIntent.RequestPermission -> checkPermission()
            is DocumentListIntent.ToggleBookmark -> toggleBookmark(intent.document)
            is DocumentListIntent.ToggleShowBookmarks -> toggleShowBookmarks()
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            _state.update { it.copy(permissionGranted = true) }
            return
        }

        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        _state.update { it.copy(permissionGranted = isGranted) }
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = getPdfDocumentsUseCase()
            result.onSuccess { docs ->
                _state.update { it.copy(documents = docs, isLoading = false) }
            }.onFailure { e ->
                _state.update { it.copy(error = e.message ?: context.getString(R.string.document_load_error), isLoading = false) }
            }
        }
    }

    private fun observeRecentDocuments() {
        viewModelScope.launch {
            getRecentDocumentsUseCase().collect { recentDocs ->
                _state.update { it.copy(recentDocuments = recentDocs) }
            }
        }
    }

    private fun observeBookmarks() {
        viewModelScope.launch {
            getBookmarkedDocumentsUseCase().collect { bookmarks ->
                _state.update { it.copy(bookmarkedDocuments = bookmarks) }
            }
        }
    }

    private fun deleteDocument(uri: String) {
        viewModelScope.launch {
            val result = deleteDocumentUseCase(uri)
            result.onSuccess {
                loadDocuments()
            }.onFailure { e ->
                _state.update { it.copy(error = e.message ?: context.getString(R.string.document_delete_error)) }
            }
        }
    }

    private fun openDocument(document: PdfDocument) {
        viewModelScope.launch {
            markDocumentOpenedUseCase(document)
        }
    }

    private fun toggleBookmark(document: PdfDocument) {
        viewModelScope.launch {
            toggleBookmarkUseCase(document)
        }
    }

    private fun toggleShowBookmarks() {
        _state.update { it.copy(showBookmarks = !it.showBookmarks) }
    }
}
