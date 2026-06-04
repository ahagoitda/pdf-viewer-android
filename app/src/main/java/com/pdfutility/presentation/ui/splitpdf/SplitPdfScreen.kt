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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdfutility.R
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.presentation.intent.SplitPdfIntent
import com.pdfutility.presentation.viewmodel.SplitPdfViewModel
import com.pdfutility.util.formatFileSize
import com.pdfutility.util.openPdfFile
import com.pdfutility.util.sharePdfFile

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
                title = { Text(stringResource(R.string.pdf_split), modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                title = { Text(stringResource(R.string.error)) },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { viewModel.onIntent(SplitPdfIntent.Reset) }) { Text(stringResource(R.string.confirm)) }
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
            Text(stringResource(R.string.select_pdf))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.select_pdf_hint),
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
        TextButton(onClick = onSelectPdf) { Text(stringResource(R.string.another_file)) }
    }

    Text(
        text = stringResource(R.string.pages_selected, state.pageCount, state.selectedPages.size),
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
            label = { Text(stringResource(R.string.output_file_name)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = state.pageRangeInput,
            onValueChange = { onIntent(SplitPdfIntent.SetPageRange(it)) },
            label = { Text(stringResource(R.string.page_range)) },
            placeholder = { Text(stringResource(R.string.page_range_hint)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        Button(onClick = { onIntent(SplitPdfIntent.ApplyPageRange) }) {
            Text(stringResource(R.string.apply_range))
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = { onIntent(SplitPdfIntent.SelectAll) }) {
            Icon(Icons.Default.SelectAll, contentDescription = stringResource(R.string.select_all))
        }
        IconButton(onClick = { onIntent(SplitPdfIntent.DeselectAll) }) {
            Icon(Icons.Default.Deselect, contentDescription = stringResource(R.string.deselect_all))
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
        Text(stringResource(R.string.extract_pages, state.selectedPages.size))
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
    val selectionDescription = stringResource(
        R.string.page_selection_description,
        pageIndex + 1,
        stringResource(if (isSelected) R.string.selected else R.string.not_selected)
    )

    Box(
        modifier = Modifier
            .shadow(1.dp, MaterialTheme.shapes.small)
            .border(borderWidth, borderColor, MaterialTheme.shapes.small)
            .semantics {
                role = Role.Button
                selected = isSelected
                contentDescription = selectionDescription
            }
            .clickable(onClick = onClick),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (bitmap != null && !bitmap.isRecycled) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.page_label, pageIndex + 1),
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
    val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (result is ConversionResult.Success) R.string.extract_success else R.string.extract_fail)) },
        text = {
            when (result) {
                is ConversionResult.Success -> {
                    Column {
                        Text(stringResource(R.string.file_name_label, result.outputName))
                        Text(stringResource(R.string.extracted_pages, result.pageCount))
                        Text(stringResource(R.string.size_label, formatFileSize(result.totalSize)))
                    }
                }
                is ConversionResult.Error -> Text(result.message)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.confirm)) } },
        dismissButton = {
            if (result is ConversionResult.Success) {
                Row {
                    TextButton(onClick = { openPdfFile(context, result.outputPath) }) {
                        Text(stringResource(R.string.open))
                    }
                    TextButton(onClick = { sharePdfFile(context, result.outputPath) }) {
                        Text(stringResource(R.string.share))
                    }
                }
            }
        },
    )
}
