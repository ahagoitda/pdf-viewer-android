package com.pdfutility.presentation.ui.documentlist

import android.Manifest
import android.os.Build
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdfutility.domain.model.PdfDocument
import com.pdfutility.presentation.intent.DocumentListIntent
import com.pdfutility.presentation.ui.common.PdfUtilityScaffold
import com.pdfutility.presentation.viewmodel.DocumentListViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DocumentListScreen(
    onDocumentClick: (PdfDocument) -> Unit,
    onImageToPdfClick: () -> Unit,
    viewModel: DocumentListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var documentToDelete by remember { mutableStateOf<PdfDocument?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onIntent(DocumentListIntent.LoadDocuments)
        }
    }

    LaunchedEffect(Unit) {
        if (!state.permissionGranted) {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            permissionLauncher.launch(permission)
        }
    }

    PdfUtilityScaffold(
        title = "PDF 목록",
        floatingActionButton = {
            FloatingActionButton(onClick = onImageToPdfClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "이미지를 PDF로 변환")
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!state.permissionGranted) {
                PermissionRequiredView {
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    permissionLauncher.launch(permission)
                }
            } else if (state.isLoading) {
                LoadingView()
            } else if (state.error != null) {
                ErrorView(state.error!!) {
                    viewModel.onIntent(DocumentListIntent.LoadDocuments)
                }
            } else if (state.documents.isEmpty() && state.recentDocuments.isEmpty()) {
                EmptyStateView()
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (state.recentDocuments.isNotEmpty()) {
                        item {
                            SectionHeader("최근 열람 문서")
                        }
                        items(state.recentDocuments) { doc ->
                            DocumentItem(
                                document = doc,
                                onClick = {
                                    viewModel.onIntent(DocumentListIntent.OpenDocument(doc))
                                    onDocumentClick(doc)
                                },
                                onDelete = { documentToDelete = doc }
                            )
                        }
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }

                    item {
                        SectionHeader("모든 문서")
                    }

                    if (state.documents.isEmpty()) {
                        item {
                            Text(
                                text = "문서가 없습니다.",
                                modifier = Modifier.padding(16.dp),
                                color = Color.Gray
                            )
                        }
                    } else {
                        items(state.documents) { doc ->
                            DocumentItem(
                                document = doc,
                                onClick = {
                                    viewModel.onIntent(DocumentListIntent.OpenDocument(doc))
                                    onDocumentClick(doc)
                                },
                                onDelete = { documentToDelete = doc }
                            )
                        }
                    }
                }
            }
        }
    }

    documentToDelete?.let { doc ->
        DeleteConfirmDialog(
            documentName = doc.name,
            onConfirm = {
                viewModel.onIntent(DocumentListIntent.DeleteDocument(doc.uri))
                documentToDelete = null
            },
            onDismiss = { documentToDelete = null }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
    )
}

@Composable
fun DocumentItem(
    document: PdfDocument,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val fileSize = Formatter.formatShortFileSize(context, document.size)
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(document.lastModified))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = document.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            Text(
                text = "$fileSize | $date",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "삭제",
                tint = Color.Red
            )
        }
    }
}

@Composable
fun PermissionRequiredView(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "파일 읽기 권한이 필요합니다.")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text(text = "권한 요청")
        }
    }
}

@Composable
fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = Color.Red)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(text = "다시 시도")
        }
    }
}

@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.LightGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "찾은 PDF 문서가 없습니다.", color = Color.Gray, fontSize = 18.sp)
    }
}

@Composable
fun DeleteConfirmDialog(
    documentName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "문서 삭제") },
        text = { Text(text = "'$documentName' 문서를 삭제하시겠습니까?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "삭제", color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "취소")
            }
        }
    )
}
