package com.pdfutility.presentation.ui.mergepdf

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.presentation.intent.MergePdfIntent
import com.pdfutility.presentation.state.SelectedPdf
import com.pdfutility.presentation.viewmodel.MergePdfViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergePdfScreen(
    onBackClick: () -> Unit,
    onMergeSuccess: (String) -> Unit,
    viewModel: MergePdfViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            val pdfs = uris.map { uri ->
                val meta = queryPdfMetadata(context, uri)
                SelectedPdf(uri = uri, name = meta.first, size = meta.second)
            }
            viewModel.onIntent(MergePdfIntent.AddPdfs(pdfs))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF 병합") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = state.outputFileName,
                onValueChange = { viewModel.onIntent(MergePdfIntent.SetOutputName(it)) },
                label = { Text("출력 파일 이름") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("예: 병합문서") },
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "선택된 PDF (${state.selectedPdfs.size}개)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = {
                    pdfPicker.launch(arrayOf("application/pdf"))
                }) {
                    Icon(Icons.Default.Add, contentDescription = "PDF 추가")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.selectedPdfs.isEmpty()) {
                EmptyMergeView()
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(state.selectedPdfs, key = { _, item -> item.uri }) { index, pdf ->
                        PdfListItem(
                            index = index,
                            pdf = pdf,
                            totalCount = state.selectedPdfs.size,
                            onMoveUp = { viewModel.onIntent(MergePdfIntent.MovePdf(index, index - 1)) },
                            onMoveDown = { viewModel.onIntent(MergePdfIntent.MovePdf(index, index + 1)) },
                            onRemove = { viewModel.onIntent(MergePdfIntent.RemovePdf(pdf.uri)) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.onIntent(MergePdfIntent.StartMerge) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.selectedPdfs.size >= 2 && !state.isMerging,
            ) {
                Icon(Icons.Default.Description, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("PDF 병합하기")
            }
        }

        if (state.isMerging) {
            MergeProgressDialog(progress = state.mergeProgress)
        }

        state.mergeResult?.let { result ->
            MergeResultDialog(
                result = result,
                onDismiss = {
                    viewModel.onIntent(MergePdfIntent.DismissResult)
                    if (result is ConversionResult.Success) {
                        onMergeSuccess(result.outputPath)
                    }
                },
            )
        }

        state.error?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.onIntent(MergePdfIntent.Reset) },
                title = { Text("오류") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.onIntent(MergePdfIntent.Reset)
                    }) { Text("확인") }
                },
            )
        }
    }
}

@Composable
private fun PdfListItem(
    index: Int,
    pdf: SelectedPdf,
    totalCount: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(28.dp),
            )
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pdf.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatFileSize(pdf.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onMoveUp, enabled = index > 0) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "위로", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onMoveDown, enabled = index < totalCount - 1) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "아래로", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "제거", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EmptyMergeView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "+ 버튼으로 PDF를 추가하세요",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "최소 2개의 PDF가 필요합니다",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun MergeProgressDialog(progress: Float) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("PDF 병합 중...", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun MergeResultDialog(
    result: ConversionResult,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (result is ConversionResult.Success) "병합 완료" else "병합 실패") },
        text = {
            when (result) {
                is ConversionResult.Success -> {
                    Column {
                        Text("파일명: ${result.outputName}.pdf")
                        Text("총 페이지: ${result.pageCount}장")
                        Text("크기: ${formatFileSize(result.totalSize)}")
                    }
                }
                is ConversionResult.Error -> Text(result.message)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("확인") } },
    )
}

private fun queryPdfMetadata(context: android.content.Context, uri: Uri): Pair<String, Long> {
    var name = "Unknown.pdf"
    var size = 0L
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) name = cursor.getString(nameIdx)
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
    }
    return name to size
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
