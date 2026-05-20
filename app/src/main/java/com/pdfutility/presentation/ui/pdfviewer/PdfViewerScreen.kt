package com.pdfutility.presentation.ui.pdfviewer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
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
    var showSaveFormatDialog by remember { mutableStateOf(false) }

    // Zoom and pan gestures state variables
    var isPinching by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

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

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newZoom = (state.zoomLevel * zoomChange).coerceIn(1f, 5f)
        viewModel.onIntent(PdfViewerIntent.SetZoom(newZoom))
        if (newZoom > 1f) {
            offset = getBoundedOffset(offset + panChange, newZoom, containerSize)
        } else {
            offset = Offset.Zero
        }
    }

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            uri?.let {
                viewModel.onIntent(PdfViewerIntent.SaveAsFile(it))
            }
        }
    )

    val saveTextLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
        onResult = { uri ->
            uri?.let {
                viewModel.onIntent(PdfViewerIntent.SaveAsText(it))
            }
        }
    )

    val saveDocxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        onResult = { uri ->
            uri?.let {
                viewModel.onIntent(PdfViewerIntent.SaveAsDocx(it))
            }
        }
    )

    if (showSaveFormatDialog) {
        AlertDialog(
            onDismissRequest = { showSaveFormatDialog = false },
            title = { Text("파일 저장 형식 선택") },
            text = {
                Column {
                    Text("저장할 파일 형식을 선택하세요:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(
                        onClick = {
                            showSaveFormatDialog = false
                            val parsedUri = Uri.parse(pdfUri)
                            val fileName = parsedUri.lastPathSegment ?: "document.pdf"
                            saveDocumentLauncher.launch(fileName)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Text("PDF 파일 (.pdf)로 저장", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = {
                            showSaveFormatDialog = false
                            val parsedUri = Uri.parse(pdfUri)
                            val baseName = parsedUri.lastPathSegment?.removeSuffix(".pdf") ?: "document"
                            saveTextLauncher.launch("${baseName}.txt")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Text("텍스트 파일 (.txt)로 저장", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = {
                            showSaveFormatDialog = false
                            val parsedUri = Uri.parse(pdfUri)
                            val baseName = parsedUri.lastPathSegment?.removeSuffix(".pdf") ?: "document"
                            saveDocxLauncher.launch("${baseName}.docx")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Text("워드 문서 (.docx)로 저장", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSaveFormatDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

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
                    IconButton(onClick = { 
                        val newZoom = (state.zoomLevel - 0.2f).coerceIn(1f, 5f)
                        viewModel.onIntent(PdfViewerIntent.SetZoom(newZoom)) 
                        if (newZoom <= 1f) offset = Offset.Zero
                    }) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "축소")
                    }
                    Text(text = "${(state.zoomLevel * 100).toInt()}%")
                    IconButton(onClick = { 
                        val newZoom = (state.zoomLevel + 0.2f).coerceIn(1f, 5f)
                        viewModel.onIntent(PdfViewerIntent.SetZoom(newZoom)) 
                    }) {
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
                                text = { Text("파일로 저장...") },
                                onClick = {
                                    menuExpanded = false
                                    showSaveFormatDialog = true
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
                        userScrollEnabled = state.zoomLevel == 1f && !isPinching,
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { containerSize = it }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        isPinching = event.changes.size >= 2
                                    }
                                }
                            }
                            .pointerInput(state.zoomLevel) {
                                if (state.zoomLevel > 1f) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        offset = getBoundedOffset(offset + dragAmount, state.zoomLevel, containerSize)
                                    }
                                }
                            }
                            .pointerInput(state.zoomLevel) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (state.zoomLevel > 1f) {
                                            viewModel.onIntent(PdfViewerIntent.SetZoom(1f))
                                            offset = Offset.Zero
                                        } else {
                                            viewModel.onIntent(PdfViewerIntent.SetZoom(2.5f))
                                        }
                                    }
                                )
                            }
                            .graphicsLayer {
                                scaleX = state.zoomLevel
                                scaleY = state.zoomLevel
                                translationX = offset.x
                                translationY = offset.y
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

private fun getBoundedOffset(offset: Offset, scale: Float, size: IntSize): Offset {
    if (scale <= 1f) return Offset.Zero
    val maxX = (size.width * (scale - 1f)) / 2f
    val maxY = (size.height * (scale - 1f)) / 2f
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY)
    )
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
