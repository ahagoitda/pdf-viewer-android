package com.pdfutility.presentation.ui.splitpdf

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.presentation.intent.SplitPdfIntent
import com.pdfutility.presentation.viewmodel.SplitPdfViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitPdfScreen(
    onBackClick: () -> Unit,
    onSplitSuccess: (String) -> Unit,
    viewModel: SplitPdfViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.onIntent(SplitPdfIntent.LoadPdf(it)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF 페이지 추출") },
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
                EmptyPdfSelectView(onSelectPdf = {
                    pdfPicker.launch(arrayOf("application/pdf"))
                })
            } else {
                PdfLoadedView(
                    state = state,
                    onIntent = viewModel::onIntent,
                    onSelectPdf = {
                        pdfPicker.launch(arrayOf("application/pdf"))
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        state.error?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.onIntent(SplitPdfIntent.Reset) },
                title = { Text("오류") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { viewModel.onIntent(SplitPdfIntent.Reset) }) { Text("확인") }
                },
            )
        }

        state.result?.let { result ->
            SplitResultDialog(
                result = result,
                onDismiss = {
                    viewModel.onIntent(SplitPdfIntent.DismissResult)
                    if (result is ConversionResult.Success) {
                        onSplitSuccess(result.outputPath)
                    }
                },
            )
        }
    }
}

@Composable
private fun EmptyPdfSelectView(onSelectPdf: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(onClick = onSelectPdf) {
            Text("PDF 파일 선택")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "분할할 PDF 파일을 선택하세요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ColumnScope.PdfLoadedView(
    state: com.pdfutility.presentation.state.SplitPdfState,
    onIntent: (SplitPdfIntent) -> Unit,
    onSelectPdf: () -> Unit,
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
        )
        TextButton(onClick = onSelectPdf) { Text("다른 파일") }
    }

    Text(
        text = "총 ${state.pageCount}페이지 중 ${state.selectedPages.size}페이지 선택됨",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = state.outputFileName,
            onValueChange = { onIntent(SplitPdfIntent.SetOutputName(it)) },
            label = { Text("출력 파일 이름") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = { onIntent(SplitPdfIntent.SelectAll) }) {
            Icon(Icons.Default.SelectAll, contentDescription = "전체 선택")
        }
        IconButton(onClick = { onIntent(SplitPdfIntent.DeselectAll) }) {
            Icon(Icons.Default.Deselect, contentDescription = "전체 해제")
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (state.isProcessing && state.pageCount == 0) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = modifier,
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items((0 until state.pageCount).toList()) { pageIndex ->
                PageThumbnail(
                    pageIndex = pageIndex,
                    bitmap = state.thumbnailBitmaps[pageIndex],
                    isSelected = pageIndex in state.selectedPages,
                    onClick = { onIntent(SplitPdfIntent.TogglePage(pageIndex)) },
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = { onIntent(SplitPdfIntent.StartSplit) },
        modifier = Modifier.fillMaxWidth(),
        enabled = state.selectedPages.isNotEmpty() && !state.isProcessing,
    ) {
        if (state.isProcessing) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("${state.selectedPages.size}페이지 추출하기")
    }
}

@Composable
private fun PageThumbnail(
    pageIndex: Int,
    bitmap: Bitmap?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .shadow(1.dp, MaterialTheme.shapes.small)
            .border(borderWidth, borderColor, MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (bitmap != null && !bitmap.isRecycled) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "${pageIndex + 1}페이지",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "${pageIndex + 1}",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun SplitResultDialog(
    result: ConversionResult,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (result is ConversionResult.Success) "추출 완료" else "추출 실패") },
        text = {
            when (result) {
                is ConversionResult.Success -> {
                    Column {
                        Text("파일명: ${result.outputName}.pdf")
                        Text("추출된 페이지: ${result.pageCount}장")
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