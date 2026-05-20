package com.pdfutility.presentation.ui.pdfviewer

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdfutility.presentation.intent.PdfViewerIntent
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
