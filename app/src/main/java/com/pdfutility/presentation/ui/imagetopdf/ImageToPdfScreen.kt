package com.pdfutility.presentation.ui.imagetopdf

import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.presentation.intent.ImageToPdfIntent
import com.pdfutility.presentation.viewmodel.ImageToPdfViewModel

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
                viewModel.getInterstitialAd()?.show(context as android.app.Activity)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "이미지를 PDF로 변환") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = state.outputFileName,
                    onValueChange = { viewModel.onIntent(ImageToPdfIntent.SetOutputName(it)) },
                    label = { Text("출력 파일 이름") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("예: my_document") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "선택된 이미지 (${state.selectedImages.size})",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.selectedImages) { item ->
                        Card {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = item.displayName,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .fillMaxWidth(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    item {
                        Card(
                            onClick = {
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.aspectRatio(1f)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "추가")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.onIntent(ImageToPdfIntent.StartConversion) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.selectedImages.isNotEmpty() && !state.isConverting
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "PDF로 변환하기")
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
                    title = { Text("오류") },
                    text = { Text(error) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.onIntent(ImageToPdfIntent.Reset) }) {
                            Text("확인")
                        }
                    }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (result is ConversionResult.Success) "변환 성공" else "변환 실패")
        },
        text = {
            when (result) {
                is ConversionResult.Success -> {
                    Column {
                        Text("파일명: ${result.outputName}.pdf")
                        Text("페이지 수: ${result.pageCount}장")
                        Text("크기: ${android.text.format.Formatter.formatShortFileSize(LocalContext.current, result.totalSize)}")
                    }
                }
                is ConversionResult.Error -> {
                    Text(text = result.message)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인")
            }
        }
    )
}
