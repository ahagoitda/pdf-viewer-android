package com.pdfutility.presentation.ui.imagetopdf

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pdfutility.R
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.domain.model.PdfPageOrientation
import com.pdfutility.domain.model.PdfPageSize
import com.pdfutility.presentation.intent.ImageToPdfIntent
import com.pdfutility.presentation.state.ImageItem
import com.pdfutility.presentation.state.ImageToPdfState
import com.pdfutility.presentation.viewmodel.ImageToPdfViewModel
import com.pdfutility.util.openPdfFile
import com.pdfutility.util.sharePdfFile
import com.pdfutility.util.formatFileSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(
    onBackClick: () -> Unit,
    onConversionSuccess: (String) -> Unit,
    viewModel: ImageToPdfViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val imagePicker = rememberImagePicker { items ->
        viewModel.onIntent(ImageToPdfIntent.SelectImages(items))
    }

    LaunchedEffect(state.conversionResult) {
        state.conversionResult?.let { result ->
            if (result is ConversionResult.Success) {
                val activity = context as? android.app.Activity ?: return@let
                viewModel.getInterstitialAd()?.show(activity)
            }
        }
    }

    if (state.isSortingActive) {
        ImageSortScreen(
            state = state,
            onIntent = viewModel::onIntent,
            onBackClick = { viewModel.onIntent(ImageToPdfIntent.HideSorting) }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.image_to_pdf), modifier = Modifier.semantics { heading() }) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = state.outputFileName,
                    onValueChange = { viewModel.onIntent(ImageToPdfIntent.SetOutputName(it)) },
                    label = { Text(stringResource(R.string.output_file_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.output_file_hint)) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                ImagePdfOptionsView(
                    options = state.options,
                    onOptionsChange = { viewModel.onIntent(ImageToPdfIntent.SetOptions(it)) },
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.selected_images, state.selectedImages.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(state.selectedImages, key = { _, item -> item.uri }) { index, item ->
                        ImageItemRow(
                            item = item,
                            index = index,
                            totalCount = state.selectedImages.size,
                            onRemove = { viewModel.onIntent(ImageToPdfIntent.RemoveImage(item.uri)) },
                            onMoveUp = { viewModel.onIntent(ImageToPdfIntent.MoveImage(index, index - 1)) },
                            onMoveDown = { viewModel.onIntent(ImageToPdfIntent.MoveImage(index, index + 1)) },
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(onClick = {
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(stringResource(R.string.add_image))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.onIntent(ImageToPdfIntent.ShowSorting) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.selectedImages.isNotEmpty() && !state.isConverting
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = stringResource(R.string.convert_to_pdf))
                }
            }
        }
    }

    if (state.isConverting) {
        ConversionProgressScreen(progress = state.conversionProgress)
    }

    state.conversionResult?.let { result ->
        ConversionResultDialog(
            result = result,
            onDismiss = {
                viewModel.onIntent(ImageToPdfIntent.DismissResult)
                if (result is ConversionResult.Success) {
                    onConversionSuccess(result.outputPath)
                }
            }
        )
    }

    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(ImageToPdfIntent.Reset) },
            title = { Text(stringResource(R.string.error)) },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(ImageToPdfIntent.Reset) }) {
                    Text(stringResource(R.string.confirm))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageSortScreen(
    state: ImageToPdfState,
    onIntent: (ImageToPdfIntent) -> Unit,
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "순서 고르기", modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "이미지를 터치하여 병합할 순서를 정하세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "선택됨: ${state.orderedImages.size} / ${state.selectedImages.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onIntent(ImageToPdfIntent.ClearOrder) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("전체 해제")
                }
                Button(
                    onClick = { onIntent(ImageToPdfIntent.ResetOrder) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("처음 선택한 순서")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.selectedImages, key = { it.uri }) { item ->
                    val orderIndex = state.orderedImages.indexOf(item)
                    val isSelected = orderIndex >= 0

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .shadow(1.dp, MaterialTheme.shapes.small)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.small
                            )
                            .clickable { onIntent(ImageToPdfIntent.ToggleImageOrder(item)) }
                    ) {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = item.displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = if (isSelected) 1f else 0.4f
                        )

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(24.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = MaterialTheme.shapes.extraSmall
                                    )
                                    .align(Alignment.TopEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${orderIndex + 1}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onIntent(ImageToPdfIntent.StartConversion) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.orderedImages.isNotEmpty() && !state.isConverting
            ) {
                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "PDF 만들기")
            }
        }
    }
}

@Composable
private fun ImageItemRow(
    item: ImageItem,
    index: Int,
    totalCount: Int,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = item.uri,
                contentDescription = item.displayName,
                modifier = Modifier
                    .size(width = 80.dp, height = 80.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${index + 1} / $totalCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column {
                IconButton(onClick = onMoveUp, enabled = index > 0) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = stringResource(R.string.move_item_up, item.displayName),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onMoveDown, enabled = index < totalCount - 1) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = stringResource(R.string.move_item_down, item.displayName),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.remove_item, item.displayName),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ConversionResultDialog(
    result: ConversionResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(if (result is ConversionResult.Success) R.string.convert_success else R.string.convert_fail))
        },
        text = {
            when (result) {
                is ConversionResult.Success -> {
                    Column {
                        Text(stringResource(R.string.file_name_label, result.outputName))
                        Text(stringResource(R.string.page_count_label, result.pageCount))
                        Text(stringResource(R.string.size_label, formatFileSize(result.totalSize)))
                    }
                }
                is ConversionResult.Error -> {
                    Text(text = result.message)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.confirm))
            }
        },
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

@Composable
private fun ImagePdfOptionsView(
    options: com.pdfutility.domain.model.ImagePdfOptions,
    onOptionsChange: (com.pdfutility.domain.model.ImagePdfOptions) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.pdf_options),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.page_size), modifier = Modifier.width(88.dp))
            RadioButton(
                selected = options.pageSize == PdfPageSize.A4,
                onClick = { onOptionsChange(options.copy(pageSize = PdfPageSize.A4)) },
            )
            Text(stringResource(R.string.page_size_a4))
            RadioButton(
                selected = options.pageSize == PdfPageSize.Original,
                onClick = { onOptionsChange(options.copy(pageSize = PdfPageSize.Original)) },
            )
            Text(stringResource(R.string.page_size_original))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.orientation), modifier = Modifier.width(88.dp))
            RadioButton(
                selected = options.orientation == PdfPageOrientation.Portrait,
                onClick = { onOptionsChange(options.copy(orientation = PdfPageOrientation.Portrait)) },
            )
            Text(stringResource(R.string.portrait))
            RadioButton(
                selected = options.orientation == PdfPageOrientation.Landscape,
                onClick = { onOptionsChange(options.copy(orientation = PdfPageOrientation.Landscape)) },
            )
            Text(stringResource(R.string.landscape))
        }
        Text(stringResource(R.string.margin, options.marginPt))
        Slider(
            value = options.marginPt.toFloat(),
            onValueChange = { onOptionsChange(options.copy(marginPt = it.toInt())) },
            valueRange = 0f..72f,
            steps = 5,
        )
        Text(stringResource(R.string.quality, options.quality))
        Slider(
            value = options.quality.toFloat(),
            onValueChange = { onOptionsChange(options.copy(quality = it.toInt())) },
            valueRange = 50f..100f,
            steps = 4,
        )
    }
}
