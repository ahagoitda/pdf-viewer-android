package com.pdfutility.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfutility.R
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.domain.usecase.MergePdfsUseCase
import com.pdfutility.presentation.intent.MergePdfIntent
import com.pdfutility.presentation.state.MergePdfState
import com.pdfutility.presentation.state.SelectedPdf
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MergePdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mergePdfsUseCase: MergePdfsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(MergePdfState())
    val state: StateFlow<MergePdfState> = _state.asStateFlow()

    fun onIntent(intent: MergePdfIntent) {
        when (intent) {
            is MergePdfIntent.AddPdfs -> addPdfs(intent.pdfs)
            is MergePdfIntent.RemovePdf -> removePdf(intent.uri)
            is MergePdfIntent.MovePdf -> movePdf(intent.fromIndex, intent.toIndex)
            is MergePdfIntent.SetOutputName -> setOutputName(intent.name)
            is MergePdfIntent.StartMerge -> startMerge()
            is MergePdfIntent.Reset -> reset()
            is MergePdfIntent.DismissResult -> dismissResult()
        }
    }

    private fun addPdfs(pdfs: List<SelectedPdf>) {
        val existingUris = _state.value.selectedPdfs.map { it.uri }.toSet()
        val newPdfs = pdfs.filter { it.uri !in existingUris }
        _state.update { it.copy(selectedPdfs = it.selectedPdfs + newPdfs, error = null) }
    }

    private fun removePdf(uri: android.net.Uri) {
        _state.update { it.copy(selectedPdfs = it.selectedPdfs.filter { pdf -> pdf.uri != uri }) }
    }

    private fun movePdf(fromIndex: Int, toIndex: Int) {
        val current = _state.value.selectedPdfs.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _state.update { it.copy(selectedPdfs = current) }
    }

    private fun setOutputName(name: String) {
        _state.update { it.copy(outputFileName = name) }
    }

    private fun startMerge() {
        val pdfs = _state.value.selectedPdfs
        if (pdfs.size < 2) {
            _state.update { it.copy(error = context.getString(R.string.pdf_min_required)) }
            return
        }

        val name = _state.value.outputFileName.ifBlank { "merged_${System.currentTimeMillis()}" }
        val uris = pdfs.map { it.uri }

        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isMerging = true, mergeProgress = 0f, error = null) }

            val result = mergePdfsUseCase(uris, name) { progress ->
                _state.update { it.copy(mergeProgress = progress) }
            }

            _state.update {
                it.copy(isMerging = false, mergeResult = result, mergeProgress = 1f)
            }
        }
    }

    private fun reset() {
        _state.value = MergePdfState()
    }

    private fun dismissResult() {
        _state.update { it.copy(mergeResult = null) }
    }
}
