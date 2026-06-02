package com.pdfutility.presentation.ui.hwpxviewer

import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pdfutility.data.local.fileio.HwpxTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HwpxViewerScreen(
    hwpxUri: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uri = remember(hwpxUri) { Uri.parse(hwpxUri) }
    var isLoading by remember { mutableStateOf(true) }
    var bodyText by remember { mutableStateOf("") }

    LaunchedEffect(hwpxUri) {
        isLoading = true
        bodyText = withContext(Dispatchers.IO) {
            val displayName = getDisplayName(context, uri)
            if (displayName.endsWith(".hwp", ignoreCase = true)) {
                "구형 HWP 바이너리 문서는 아직 지원하지 않습니다.\n\nHWPX로 저장한 파일을 열어 주세요."
            } else {
                runCatching { HwpxTextExtractor.extract(context, uri) }
                    .getOrElse { error -> "문서를 열 수 없습니다.\n\n원인: ${error.message}" }
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(getDisplayName(context, uri)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                SelectionContainer {
                    Text(
                        text = bodyText,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

private fun getDisplayName(context: android.content.Context, uri: Uri): String {
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor: Cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index)
            }
        }
    }
    return uri.lastPathSegment ?: "HWPX 문서"
}
