package com.pdfutility.presentation.ui.reorderpages

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.presentation.intent.ReorderPagesIntent
import com.pdfutility.presentation.viewmodel.ReorderPagesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderPagesScreen(
    onBackClick: () -> Unit,
    onReorderSuccess: (String) -> Unit,
    viewModel: ReorderPagesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.onIntent(ReorderPagesIntent.LoadPdf(it)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("페이지 재배열") },
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
            if (state.sourceUri == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Button(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }) {
                        Text("PDF 파일 선택")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "페이지 순서를 변경할 PDF를 선택하세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (state.isProcessing && state.pageCount == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                PdfLoadedView(
                    state = state,
                    onIntent = viewModel::onIntent,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        state.result?.let { result ->
            ReorderResultDialog(
                result = result,
                onDismiss = {
                    viewModel.onIntent(ReorderPagesIntent.DismissResult)
                    if (result is ConversionResult.Success) {
                        onReorderSuccess(result.outputPath)
                    }
                },
            )
        }

        state.error?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.onIntent(ReorderPagesIntent.Reset) },
                title = { Text("오류") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { viewModel.onIntent(ReorderPagesIntent.Reset) }) { Text("확인") }
                },
            )
        }
    }
}

@Composable
private fun ColumnScope.PdfLoadedView(
    state: com.pdfutility.presentation.state.ReorderPagesState,
    onIntent: (ReorderPagesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = state.sourceName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${state.pageCount}페이지",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = state.outputFileName,
        onValueChange = { onIntent(ReorderPagesIntent.SetOutputName(it)) },
        label = { Text("출력 파일 이름") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )

    Spacer(modifier = Modifier.height(8.dp))

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(state.pageOrder, key = { _, pageIndex -> pageIndex }) { listIndex, pageIndex ->
            PageReorderItem(
                displayIndex = listIndex + 1,
                originalPageIndex = pageIndex,
                bitmap = state.thumbnailBitmaps[pageIndex],
                canMoveUp = listIndex > 0,
                canMoveDown = listIndex < state.pageOrder.lastIndex,
                onMoveUp = { onIntent(ReorderPagesIntent.MovePage(listIndex, listIndex - 1)) },
                onMoveDown = { onIntent(ReorderPagesIntent.MovePage(listIndex, listIndex + 1)) },
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = { onIntent(ReorderPagesIntent.StartReorder) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.isProcessing,
    ) {
        if (state.isProcessing) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("재배열된 PDF 저장")
    }
}

@Composable
private fun PageReorderItem(
    displayIndex: Int,
    originalPageIndex: Int,
    bitmap: Bitmap?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$displayIndex",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(28.dp),
            )

            if (bitmap != null && !bitmap.isRecycled) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 60.dp, height = 80.dp)
                        .shadow(1.dp, MaterialTheme.shapes.extraSmall),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Surface(
                    modifier = Modifier.size(width = 60.dp, height = 80.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.extraSmall,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${originalPageIndex + 1}페이지",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )

            Column {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "위로", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "아래로", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ReorderResultDialog(
    result: ConversionResult,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (result is ConversionResult.Success) "재배열 완료" else "재배열 실패") },
        text = {
            when (result) {
                is ConversionResult.Success -> {
                    Column {
                        Text("파일명: ${result.outputName}.pdf")
                        Text("페이지 수: ${result.pageCount}장")
                        Text("크기: ${formatFileSize(result.totalSize)}")
                    }
                }
                is ConversionResult.Error -> Text(result.message)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("확인") } },
    )
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}