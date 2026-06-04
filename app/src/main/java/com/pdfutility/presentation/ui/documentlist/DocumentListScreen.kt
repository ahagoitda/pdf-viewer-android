package com.pdfutility.presentation.ui.documentlist

import android.Manifest
import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdfutility.R
import com.pdfutility.domain.model.PdfDocument
import com.pdfutility.presentation.intent.DocumentListIntent
import com.pdfutility.presentation.state.DocumentListState
import com.pdfutility.presentation.ui.common.PdfUtilityScaffold
import com.pdfutility.presentation.viewmodel.DocumentListViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DocumentListScreen(
    onDocumentClick: (PdfDocument) -> Unit,
    onHwpxDocumentClick: (String) -> Unit,
    onImageToPdfClick: () -> Unit,
    onMergePdfClick: () -> Unit,
    onSplitPdfClick: () -> Unit,
    onReorderPagesClick: () -> Unit,
    viewModel: DocumentListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var documentToDelete by remember { mutableStateOf<PdfDocument?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onIntent(DocumentListIntent.LoadDocuments)
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                try {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(it, takeFlags)
                } catch (e: Exception) {
                    // Ignore
                }
                onDocumentClick(
                    PdfDocument(
                        uri = it.toString(),
                        name = "",
                        size = 0L,
                        lastModified = 0L
                    )
                )
            }
        }
    )

    val openHwpxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                try {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(it, takeFlags)
                } catch (e: Exception) {
                    // Ignore
                }
                onHwpxDocumentClick(it.toString())
            }
        }
    )

    LaunchedEffect(Unit) {
        if (!state.permissionGranted && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    PdfUtilityScaffold(
        title = stringResource(R.string.document_list),
        actions = {
            IconButton(onClick = { openHwpxLauncher.launch(arrayOf("application/vnd.hancom.hwpx", "application/haansofthwp", "application/x-hwp", "application/octet-stream")) }) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = stringResource(R.string.open_hwpx),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onMergePdfClick) {
                Icon(
                    imageVector = Icons.Default.Merge,
                    contentDescription = stringResource(R.string.pdf_merge),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onSplitPdfClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.CallSplit,
                    contentDescription = stringResource(R.string.pdf_split),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onReorderPagesClick) {
                Icon(
                    imageVector = Icons.Default.Reorder,
                    contentDescription = stringResource(R.string.reorder_pages),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { openDocumentLauncher.launch(arrayOf("application/pdf")) }) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = stringResource(R.string.open_device_pdf),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onImageToPdfClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.image_to_pdf))
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!state.permissionGranted) {
                PermissionRequiredView {
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            } else {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.tab_all)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.tab_bookmarks)) }
                    )
                }

                when (selectedTab) {
                    0 -> AllDocumentsView(state, viewModel, onDocumentClick)
                    1 -> BookmarkedDocumentsView(state, viewModel, onDocumentClick)
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
private fun AllDocumentsView(
    state: DocumentListState,
    viewModel: DocumentListViewModel,
    onDocumentClick: (PdfDocument) -> Unit,
) {
    if (state.isLoading) {
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
                    SectionHeader(stringResource(R.string.recent_docs))
                }
                items(state.recentDocuments) { doc ->
                    DocumentItem(
                        document = doc,
                        isBookmarked = state.bookmarkedDocuments.any { it.uri == doc.uri },
                        onClick = {
                            viewModel.onIntent(DocumentListIntent.OpenDocument(doc))
                            onDocumentClick(doc)
                        },
                        onDelete = { /* handled by long press */ },
                        onBookmark = { viewModel.onIntent(DocumentListIntent.ToggleBookmark(doc)) },
                    )
                }
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            item {
                SectionHeader(stringResource(R.string.all_docs))
            }

            if (state.documents.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_documents),
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray
                    )
                }
            } else {
                items(state.documents) { doc ->
                    DocumentItem(
                        document = doc,
                        isBookmarked = state.bookmarkedDocuments.any { it.uri == doc.uri },
                        onClick = {
                            viewModel.onIntent(DocumentListIntent.OpenDocument(doc))
                            onDocumentClick(doc)
                        },
                        onDelete = {},
                        onBookmark = { viewModel.onIntent(DocumentListIntent.ToggleBookmark(doc)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarkedDocumentsView(
    state: DocumentListState,
    viewModel: DocumentListViewModel,
    onDocumentClick: (PdfDocument) -> Unit,
) {
    if (state.bookmarkedDocuments.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.StarOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = stringResource(R.string.no_bookmarks), color = Color.Gray, fontSize = 18.sp)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.bookmarkedDocuments) { doc ->
                DocumentItem(
                    document = doc,
                    isBookmarked = true,
                    onClick = {
                        viewModel.onIntent(DocumentListIntent.OpenDocument(doc))
                        onDocumentClick(doc)
                    },
                    onDelete = {},
                    onBookmark = { viewModel.onIntent(DocumentListIntent.ToggleBookmark(doc)) },
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(16.dp, 16.dp, 16.dp, 8.dp)
            .semantics { heading() }
    )
}

@Composable
fun DocumentItem(
    document: PdfDocument,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onBookmark: () -> Unit,
) {
    val context = LocalContext.current
    val fileSize = Formatter.formatShortFileSize(context, document.size)
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(document.lastModified))
    val openDescription = stringResource(R.string.open_document, document.name)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = openDescription
            }
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
        IconButton(onClick = onBookmark) {
            Icon(
                imageVector = if (isBookmarked) Icons.Default.Star else Icons.Outlined.StarOutline,
                contentDescription = stringResource(if (isBookmarked) R.string.unbookmark else R.string.bookmark),
                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.Gray
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
        Text(text = stringResource(R.string.permission_required))
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text(text = stringResource(R.string.request_permission))
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
            Text(text = stringResource(R.string.retry))
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
        Text(text = stringResource(R.string.no_pdfs_found), color = Color.Gray, fontSize = 18.sp)
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
        title = { Text(text = stringResource(R.string.delete_document)) },
        text = { Text(text = stringResource(R.string.delete_confirm, documentName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.delete), color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}
