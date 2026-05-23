package com.pdfutility.presentation.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.domain.model.PdfDocument
import com.pdfutility.domain.usecase.ConvertFileToPdfUseCase
import com.pdfutility.domain.usecase.DeleteDocumentUseCase
import com.pdfutility.domain.usecase.GetPdfDocumentsUseCase
import com.pdfutility.domain.usecase.GetRecentDocumentsUseCase
import com.pdfutility.domain.usecase.MarkDocumentOpenedUseCase
import com.pdfutility.presentation.intent.DocumentListIntent
import com.pdfutility.presentation.state.DocumentListState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getPdfDocumentsUseCase: GetPdfDocumentsUseCase,
    private val getRecentDocumentsUseCase: GetRecentDocumentsUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase,
    private val markDocumentOpenedUseCase: MarkDocumentOpenedUseCase,
    private val convertFileToPdfUseCase: ConvertFileToPdfUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DocumentListState())
    val state: StateFlow<DocumentListState> = _state.asStateFlow()

    private val _conversionNavEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val conversionNavEvent: SharedFlow<String> = _conversionNavEvent.asSharedFlow()

    init {
        checkPermission()
        observeRecentDocuments()
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
            is DocumentListIntent.ConvertAndOpenFile -> convertAndOpenFile(intent)
        }
    }

    private fun checkPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val isGranted = ContextCompat.checkSelfPermission(
            context,
            permission
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
                _state.update { it.copy(error = e.message ?: "문서를 불러오는 중 오류가 발생했습니다.", isLoading = false) }
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

    private fun deleteDocument(uri: String) {
        viewModelScope.launch {
            val result = deleteDocumentUseCase(uri)
            result.onSuccess {
                loadDocuments()
            }.onFailure { e ->
                _state.update { it.copy(error = e.message ?: "문서 삭제 중 오류가 발생했습니다.") }
            }
        }
    }

    private fun openDocument(document: PdfDocument) {
        viewModelScope.launch {
            markDocumentOpenedUseCase(document)
        }
    }

    private fun convertAndOpenFile(intent: DocumentListIntent.ConvertAndOpenFile) {
        viewModelScope.launch {
            _state.update { it.copy(isConverting = true, conversionError = null) }
            when (val result = convertFileToPdfUseCase(intent.uri, intent.mimeType)) {
                is ConversionResult.Success -> {
                    _state.update { it.copy(isConverting = false) }
                    _conversionNavEvent.emit(result.outputPath)
                }
                is ConversionResult.Error -> {
                    _state.update { it.copy(isConverting = false, conversionError = result.message) }
                }
            }
        }
    }

    fun clearConversionError() {
        _state.update { it.copy(conversionError = null) }
    }
}
