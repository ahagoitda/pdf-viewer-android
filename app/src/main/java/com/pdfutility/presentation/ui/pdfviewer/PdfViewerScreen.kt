package com.pdfutility.presentation.ui.pdfviewer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdfutility.presentation.intent.PdfViewerIntent
import com.pdfutility.presentation.state.ExportState
import com.pdfutility.presentation.viewmodel.PdfViewerViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdfUri: String,
    onBackClick: () -> Unit,
    viewModel: PdfViewerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val renderedBitmaps by viewModel.renderedBitmaps.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(pdfUri) {
        viewModel.onIntent(PdfViewerIntent.LoadDocument(pdfUri))
    }

    // Track current page based on scroll
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                viewModel.onIntent(PdfViewerIntent.GoToPage(index))
            }
    }

    val transformableState = rememberTransformableState { zoomChange, _, _ ->
        viewModel.onIntent(PdfViewerIntent.SetZoom(state.zoomLevel * zoomChange))
    }

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            uri?.let {
                viewModel.onIntent(PdfViewerIntent.SaveAsFile(it))
            }
        }
    )

    // Export Feedback Dialogs
    when (val exportState = state.exportState) {
        is ExportState.Exporting -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("처리 중") },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("잠시만 기다려주세요...")
                    }
                },
                confirmButton = {}
            )
        }
        is ExportState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.onIntent(PdfViewerIntent.DismissExportState) },
                title = { Text("성공") },
                text = { Text(exportState.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.onIntent(PdfViewerIntent.DismissExportState) }) {
                        Text("확인")
                    }
                }
            )
        }
        is ExportState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.onIntent(PdfViewerIntent.DismissExportState) },
                title = { Text("오류") },
                text = { Text(exportState.error) },
                confirmButton = {
                    TextButton(onClick = { viewModel.onIntent(PdfViewerIntent.DismissExportState) }) {
                        Text("확인")
                    }
                }
            )
        }
        is ExportState.Idle -> {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "PDF 뷰어") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onIntent(PdfViewerIntent.SetZoom(state.zoomLevel - 0.2f)) }) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "축소")
                    }
                    Text(text = "${(state.zoomLevel * 100).toInt()}%")
                    IconButton(onClick = { viewModel.onIntent(PdfViewerIntent.SetZoom(state.zoomLevel + 0.2f)) }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "확대")
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "더보기")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("공유하기") },
                                onClick = {
                                    menuExpanded = false
                                    sharePdf(context, pdfUri)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("파일로 저장") },
                                onClick = {
                                    menuExpanded = false
                                    val parsedUri = Uri.parse(pdfUri)
                                    val fileName = parsedUri.lastPathSegment ?: "document.pdf"
                                    saveDocumentLauncher.launch(fileName)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("이미지로 저장") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.onIntent(PdfViewerIntent.ExportAsImages)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                Column {
                    // Page indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${state.currentPage + 1} / ${state.pageCount}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = state.zoomLevel
                                scaleY = state.zoomLevel
                             }
                            .transformable(state = transformableState)
                    ) {
                        items(state.pageCount) { pageIndex ->
                            PdfPageItem(
                                pageIndex = pageIndex,
                                bitmap = renderedBitmaps[pageIndex],
                                onRenderRequest = { width, height ->
                                    viewModel.onIntent(PdfViewerIntent.RenderPage(pageIndex, width, height))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun sharePdf(context: Context, uriString: String) {
    try {
        val uri = Uri.parse(uriString)
        val shareableUri = if (uri.scheme == "file") {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                java.io.File(uri.path ?: "")
            )
        } else {
            uri
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, shareableUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "PDF 공유"))
    } catch (e: Exception) {
        Toast.makeText(context, "공유 실패: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
