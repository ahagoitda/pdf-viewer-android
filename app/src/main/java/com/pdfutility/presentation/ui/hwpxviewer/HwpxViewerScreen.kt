package com.pdfutility.presentation.ui.hwpxviewer

import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfutility.R
import com.pdfutility.data.local.fileio.HwpxTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ViewerTheme(val displayNameRes: Int, val backgroundColor: Color, val textColor: Color) {
    LIGHT(R.string.theme_light, Color(0xFFFFFFFF), Color(0xFF1C1B1F)),
    SEPIA(R.string.theme_sepia, Color(0xFFF4ECD8), Color(0xFF5B4636)),
    DARK(R.string.theme_dark, Color(0xFF1E1E1E), Color(0xFFE0E0E0))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HwpxViewerScreen(
    hwpxUri: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uri = remember(hwpxUri) { Uri.parse(hwpxUri) }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var isLoading by remember { mutableStateOf(true) }
    var paragraphs by remember { mutableStateOf<List<String>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 뷰어 제어 상태
    var fontSize by remember { mutableStateOf(16f) }
    var viewerTheme by remember { mutableStateOf(ViewerTheme.LIGHT) }

    // 검색 관련 상태
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Pair<Int, IntRange>>>(emptyList()) }
    var currentSearchIndex by remember { mutableStateOf(0) }

    // 바텀시트 / 다이얼로그 제어
    var showSettings by remember { mutableStateOf(false) }
    var showDocInfo by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()

    // HWPX 텍스트 파싱
    LaunchedEffect(hwpxUri) {
        isLoading = true
        errorMessage = null
        val displayName = getDisplayName(context, uri)
        if (displayName.endsWith(".hwp", ignoreCase = true)) {
            errorMessage = context.getString(R.string.legacy_hwp_error)
            isLoading = false
        } else {
            withContext(Dispatchers.IO) {
                runCatching { HwpxTextExtractor.extractParagraphs(context, uri) }
            }.onSuccess { result ->
                paragraphs = result
                isLoading = false
            }.onFailure { error ->
                errorMessage = context.getString(R.string.hwpx_open_error, error.message)
                isLoading = false
            }
        }
    }

    // 검색 변경 시 매칭 포인트 검색
    LaunchedEffect(searchQuery, paragraphs) {
        if (searchQuery.isEmpty()) {
            searchResults = emptyList()
            currentSearchIndex = 0
        } else {
            val results = mutableListOf<Pair<Int, IntRange>>()
            paragraphs.forEachIndexed { pIndex, text ->
                var start = 0
                while (true) {
                    val index = text.indexOf(searchQuery, start, ignoreCase = true)
                    if (index == -1) break
                    results.add(pIndex to index..(index + searchQuery.length - 1))
                    start = index + searchQuery.length
                }
            }
            searchResults = results
            currentSearchIndex = 0
        }
    }

    // 검색 매칭 위치로 이동
    val scrollToMatch = { index: Int ->
        if (searchResults.isNotEmpty() && index in searchResults.indices) {
            val targetParagraph = searchResults[index].first
            coroutineScope.launch {
                lazyListState.animateScrollToItem(targetParagraph)
            }
        }
    }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_query_placeholder)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                        }
                    },
                    actions = {
                        if (searchResults.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    R.string.search_results_count,
                                    currentSearchIndex + 1,
                                    searchResults.size
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(onClick = {
                                if (searchResults.isNotEmpty()) {
                                    currentSearchIndex = (currentSearchIndex - 1 + searchResults.size) % searchResults.size
                                    scrollToMatch(currentSearchIndex)
                                }
                            }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.previous))
                            }
                            IconButton(onClick = {
                                if (searchResults.isNotEmpty()) {
                                    currentSearchIndex = (currentSearchIndex + 1) % searchResults.size
                                    scrollToMatch(currentSearchIndex)
                                }
                            }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.next))
                            }
                        } else if (searchQuery.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.no_results),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = viewerTheme.backgroundColor,
                        titleContentColor = viewerTheme.textColor,
                        navigationIconContentColor = viewerTheme.textColor,
                        actionIconContentColor = viewerTheme.textColor
                    )
                )
            } else {
                TopAppBar(
                    title = { Text(getDisplayName(context, uri), modifier = Modifier.semantics { heading() }) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        if (errorMessage == null && !isLoading) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                            }
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.reader_settings_title))
                            }
                            IconButton(onClick = { showDocInfo = true }) {
                                Icon(Icons.Default.Info, contentDescription = stringResource(R.string.document_info_title))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = viewerTheme.backgroundColor,
                        titleContentColor = viewerTheme.textColor,
                        navigationIconContentColor = viewerTheme.textColor,
                        actionIconContentColor = viewerTheme.textColor
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(viewerTheme.backgroundColor)
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = if (viewerTheme == ViewerTheme.DARK) Color.White else MaterialTheme.colorScheme.primary
                    )
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (viewerTheme == ViewerTheme.DARK) Color(0xFFFF8A80) else MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                paragraphs.isEmpty() -> {
                    Text(
                        text = "문서 텍스트가 비어 있습니다.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = viewerTheme.textColor.copy(alpha = 0.6f)
                    )
                }
                else -> {
                    SelectionContainer {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(18.dp)
                        ) {
                            itemsIndexed(paragraphs) { pIndex, text ->
                                val annotatedText = remember(text, searchQuery, searchResults, currentSearchIndex) {
                                    buildAnnotatedStringWithHighlights(
                                        text = text,
                                        query = searchQuery,
                                        paragraphIndex = pIndex,
                                        results = searchResults,
                                        currentIndex = currentSearchIndex
                                    )
                                }

                                Text(
                                    text = annotatedText,
                                    fontSize = fontSize.sp,
                                    lineHeight = (fontSize * 1.55f).sp,
                                    color = viewerTheme.textColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 읽기 설정 바텀시트
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            containerColor = viewerTheme.backgroundColor,
            contentColor = viewerTheme.textColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.reader_settings_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = viewerTheme.textColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 폰트 크기 슬라이더
                Text(
                    text = stringResource(R.string.font_size_label, fontSize),
                    style = MaterialTheme.typography.bodyMedium,
                    color = viewerTheme.textColor
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text("A", fontSize = 12.sp, color = viewerTheme.textColor.copy(alpha = 0.6f))
                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 12f..32f,
                        steps = 9,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = if (viewerTheme == ViewerTheme.DARK) Color.White else MaterialTheme.colorScheme.primary,
                            activeTrackColor = if (viewerTheme == ViewerTheme.DARK) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                        )
                    )
                    Text("A", fontSize = 24.sp, color = viewerTheme.textColor.copy(alpha = 0.8f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 배경 테마 선택
                Text(
                    text = stringResource(R.string.theme_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = viewerTheme.textColor,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    ViewerTheme.values().forEach { theme ->
                        val isSelected = viewerTheme == theme
                        val borderCol = if (isSelected) {
                            if (theme == ViewerTheme.DARK) Color.White else MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.backgroundColor)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) borderCol else theme.textColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewerTheme = theme }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(theme.displayNameRes),
                                color = theme.textColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    // 문서 정보 다이얼로그
    if (showDocInfo) {
        val totalChars = remember(paragraphs) { paragraphs.sumOf { it.length } }
        val charsNoSpaces = remember(paragraphs) { paragraphs.sumOf { it.replace(" ", "").replace("\n", "").length } }
        val fileSizeBytesStr = remember(uri) { getFileSizeString(context, uri) }

        AlertDialog(
            onDismissRequest = { showDocInfo = false },
            title = { Text(stringResource(R.string.document_info_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.doc_info_file_size, fileSizeBytesStr))
                    Text(stringResource(R.string.doc_info_paragraph_count, paragraphs.size))
                    Text(stringResource(R.string.doc_info_char_count, totalChars))
                    Text(stringResource(R.string.doc_info_char_no_space, charsNoSpaces))
                }
            },
            confirmButton = {
                TextButton(onClick = { showDocInfo = false }) {
                    Text(stringResource(R.string.confirm))
                }
            }
        )
    }
}

// 검색 단어 노란색/주황색 하이라이팅 처리 Helper
private fun buildAnnotatedStringWithHighlights(
    text: String,
    query: String,
    paragraphIndex: Int,
    results: List<Pair<Int, IntRange>>,
    currentIndex: Int
): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        if (query.isNotEmpty()) {
            val matches = results.filter { it.first == paragraphIndex }
            matches.forEach { match ->
                val overallIndex = results.indexOf(match)
                val isCurrent = overallIndex == currentIndex
                val backgroundCol = if (isCurrent) {
                    Color(0xFFFF9800) // 주황색 (현재 검색 대상)
                } else {
                    Color(0xFFFFEB3B).copy(alpha = 0.6f) // 노란색 (일반 검색어)
                }
                val range = match.second
                addStyle(
                    style = SpanStyle(background = backgroundCol, color = Color.Black),
                    start = range.first,
                    end = range.last + 1
                )
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
    return uri.lastPathSegment ?: context.getString(R.string.hwpx_doc)
}

private fun getFileSizeString(context: android.content.Context, uri: Uri): String {
    var bytes = 0L
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0) bytes = cursor.getLong(index)
            }
        }
    }
    if (bytes <= 0) {
        runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                bytes = it.length
            }
        }
    }
    if (bytes <= 0) return "알 수 없음"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format("%.2f MB", mb)
    } else {
        String.format("%.1f KB", kb)
    }
}
