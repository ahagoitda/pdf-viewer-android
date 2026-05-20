당신은 2026년 기준 시니어 안드로이드 아키텍트입니다. 아래 프로젝트의 **Step 4: PdfRenderer 기반 고성능 Compose PDF 뷰어**를 구현하세요.

---

## 프로젝트 경로
```
C:\Users\ahago\pdf-viewer-app
```

---

## 현재 상태 (Step 3까지 완료)

- Navigation: `Screen.PdfViewer` 라우트 = `"pdf_viewer/{pdfUri}"`, 현재 주석 처리됨
- `PdfUtilityNavHost.kt:43` — `// PdfViewerScreen(pdfUri = pdfUri) // Will be implemented in Step 4`
- 뷰어 진입 시 `pdfUri: String` (URL 인코딩된 content:// URI) 이 인자로 전달됨

---

## Step 4 구현할 파일 (5개 + 1개 수정)

| # | 파일 | 설명 |
|---|------|------|
| 1 | `presentation/state/PdfViewerState.kt` | 뷰어 상태 (pageCount, currentPage, zoomLevel, isRendering) |
| 2 | `presentation/intent/PdfViewerIntent.kt` | 뷰어 Intent (LoadDocument, SetZoom, GoToPage) |
| 3 | `presentation/viewmodel/PdfViewerViewModel.kt` | **핵심** — PdfRenderer 생명주기, Bitmap 풀, Lazy 렌더링 제어 |
| 4 | `presentation/ui/pdfviewer/PdfPageItem.kt` | 개별 PDF 페이지 Compose 컴포넌트 (Bitmap → Image) |
| 5 | `presentation/ui/pdfviewer/PdfViewerScreen.kt` | LazyColumn 기반 연속 스크롤 뷰어 + 줌 제스처 |
| ✎ | `presentation/ui/navigation/PdfUtilityNavHost.kt` | 주석 교체 → `PdfViewerScreen` 연결 |

---

## 🚨 핵심 요구사항 (반드시 준수)

### 1. PdfRenderer 사용 (WebView 금지)
```kotlin
val pdfRenderer = PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
```
- `PdfRenderer` 는 `ParcelFileDescriptor` 로 파일을 열어야 함
- `pdfUri` (content:// URI) → `ContentResolver.openFileDescriptor()` → `ParcelFileDescriptor`
- viewModelScope에서 `withContext(Dispatchers.IO)` 로 열기

### 2. LazyColumn + 동적 Bitmap 렌더링 (Lazy Rendering)
- **전체 페이지를 한 번에 렌더링하지 말 것**
- `LazyColumn` 내 각 item에서 `LaunchedEffect` 또는 `produceState` 로 해당 페이지만 Bitmap 생성
- Bitmap 생성 코드:
```kotlin
val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
pdfRenderer.renderPage(pageIndex, bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
```
- `renderWidth`, `renderHeight` 는 디바이스 화면 너비에 비례하여 계산 (화질 ↔ 메모리 균형)

### 3. 빠른 스크롤 디바운싱 (Render Throttling)
- `LazyListState.firstVisibleItemIndex` 로 현재 보이는 페이지 범위만 렌더링
- 스크롤 속도가 빠를 때(`isScrollInProgress`) 는 렌더링 건너뛰기 또는 저해상도 임시 렌더링
- `snapshotFlow` + `debounce(100)` 로 렌더링 요청 조절

### 4. 생명주기 자원 누수 방지 (필수)
```kotlin
override fun onCleared() {
    super.onCleared()
    renderedBitmaps.values.forEach { it.recycle() }
    renderedBitmaps.clear()
    pdfRenderer?.close()
}
```
- ViewModel `onCleared()` 에서 `pdfRenderer.close()` + 모든 `Bitmap.recycle()` 보장
- Compose `DisposableEffect(Unit)` 추가 방어선 — 화면 이탈 시 정리

### 5. 줌(확대/축소) 지원
- `PdfViewerState.zoomLevel: Float` (기본 1.0f, 범위 1.0f ~ 5.0f)
- `Modifier.graphicsLayer { scaleX = zoomLevel; scaleY = zoomLevel }`
- 핀치 줌 제스처: `detectTransformGestures` 또는 `Modifier.transformable`
- 버튼으로 +/- 줌 컨트롤

### 6. 성능 최적화
- Bitmap 재사용 풀: `MutableMap<Int, Bitmap>` (pageIndex → Bitmap)
- 화면 밖으로 3페이지 이상 벗어난 Bitmap은 `recycle()` 후 맵에서 제거
- `RENDER_MODE_FOR_DISPLAY` 사용 (고화질, GPU 가속 렌더링)

---

## PdfViewerState / PdfViewerIntent 명세

```kotlin
data class PdfViewerState(
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val zoomLevel: Float = 1f,
    val isRendering: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)
```

```kotlin
sealed interface PdfViewerIntent {
    data class LoadDocument(val uri: String) : PdfViewerIntent
    data class SetZoom(val level: Float) : PdfViewerIntent
    data class GoToPage(val page: Int) : PdfViewerIntent
    data class RenderPage(val pageIndex: Int, val width: Int, val height: Int) : PdfViewerIntent
    data object ClearRenderedPages : PdfViewerIntent
}
```

---

## PdfViewerViewModel 핵심 로직

```
onIntent(LoadDocument) → ContentResolver.openFileDescriptor(uri) → PdfRenderer(parcelFd)
  → totalPageCount 설정 → State 업데이트 → 첫 페이지만 렌더링

onIntent(RenderPage) → 해당 pageIndex Bitmap 생성 → renderedBitmaps[pageIndex] = bitmap
  → 3페이지 초과 이전 Bitmap recycle → State 업데이트

onCleared() → 모든 Bitmap recycle + pdfRenderer.close()
```

---

## LazyColumn 구조

```kotlin
LazyColumn(state = listState) {
    items(pageCount) { pageIndex ->
        if (isPageVisible(pageIndex, listState)) {
            PdfPageItem(
                pageIndex = pageIndex,
                bitmap = renderedBitmaps[pageIndex],
                zoomLevel = zoomLevel,
                onRenderRequest = { w, h -> onIntent(RenderPage(pageIndex, w, h)) }
            )
        } else {
            // Placeholder: Box with fixed height (spacer)
        }
    }
}
```

---

## 유의사항
- `ContentResolver.openFileDescriptor()` 는 파일이 없을 수 있으므로 try-catch 필수
- PDF 페이지 크기는 `pdfRenderer.getPageWidth(pageIndex)`, `getPageHeight(pageIndex)` 로 확인
- 렌더링 Bitmap 크기는 화면 폭 기준으로 축소 계산: `val scale = screenWidth / pageWidth.toFloat()`
- UTF-8 URL 디코딩해서 content:// URI 원복 필요 (`URLDecoder.decode(uri, "UTF-8")`)
- **모든 UI 문자열은 한글로 작성**
