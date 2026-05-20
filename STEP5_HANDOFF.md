당신은 2026년 기준 시니어 안드로이드 아키텍트입니다. 아래 프로젝트의 **Step 5: OOM 방어형 이미지→PDF 변환**을 구현하세요.

---

## 프로젝트 경로
```
C:\Users\ahago\pdf-viewer-app
```

---

## 현재 상태 (Step 1~4까지 완료)

- Navigation: `Screen.ImageToPdf` 라우트 = `"image_to_pdf"`, `PdfUtilityNavHost.kt` 에서 placeholder
- `ConversionFileDataSource.convertImagesToPdf()` — 현재 스켈레톤, `ConversionResult.Error("not yet implemented")` 반환
- `ConvertImagesToPdfUseCase` → `ConversionRepository` → `ConversionRepositoryImpl` → `ConversionFileDataSource` 로 연결된 파이프라인 완비

---

## Step 5 구현할 파일 (6개 + 1개 수정)

| # | 파일 | 설명 |
|---|------|------|
| 1 | `presentation/state/ImageToPdfState.kt` | 변환 진행 상태 (selectedImages, progress, convertedCount) |
| 2 | `presentation/intent/ImageToPdfIntent.kt` | Intent (SelectImages, StartConversion, SetOutputName, Reset) |
| 3 | `presentation/viewmodel/ImageToPdfViewModel.kt` | 이미지 선택, 변환 트리거, 진행률 방출 |
| 4 | `presentation/ui/imagetopdf/ImagePickerScreen.kt` | 이미지 다중 선택 UI (Grid + 확인) |
| 5 | `presentation/ui/imagetopdf/ConversionProgressScreen.kt` | 변환 진행률 Progress + 완료 다이얼로그 |
| 6 | `presentation/ui/imagetopdf/ImageToPdfScreen.kt` | 전체 화면 조합 + 상태에 따른 분기 |
| ✎ | `presentation/ui/navigation/PdfUtilityNavHost.kt` | placeholder → `ImageToPdfScreen` 연결 |

---

## 🚨 핵심: OOM 방어형 이미지→PDF 변환 로직 (ConversionFileDataSource)

### ❌ 절대 금지
```kotlin
// 절대 이렇게 하지 말 것
val bitmaps = images.map { MediaStore.Images.Media.getBitmap(contentResolver, it) }
// 모든 이미지를 Bitmap 리스트로 메모리에 올리면 OOM 발생
```

### ✅ 요구 구현 방식: 스트리밍(Streaming) 단일 패스 변환
```kotlin
suspend fun convertImagesToPdf(images: List<Uri>, outputFileName: String): ConversionResult {
    val outputFile = File(context.filesDir, "pdf_output", "$outputFileName.pdf")
    outputFile.parentFile?.mkdirs()

    val pdfDocument = PdfDocument()  // android.graphics.pdf.PdfDocument

    try {
        for ((index, imageUri) in images.withIndex()) {
            // 1. InputStream으로 이미지 열기 (Bitmap 없이 크기만 얻기)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(imageUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val imageWidth = options.outWidth
            val imageHeight = options.outHeight

            // 2. PDF 페이지 생성 (A4 기준으로 축소/확대)
            val pageInfo = PdfDocument.PageInfo.Builder(imageWidth, imageHeight, index + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // 3. 한 장씩 Bitmap 로드 → Canvas에 그리기 → 즉시 recycle
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // API 28+: ImageDecoder로 메모리 효율적 디코딩
                ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.contentResolver, imageUri),
                    { decoder, _, _ -> decoder.setTargetSampleSize(2) }
                )
            } else {
                contentResolver.openInputStream(imageUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }

            bitmap?.let {
                canvas.drawBitmap(it, 0f, 0f, null)
                it.recycle()  // ← 즉시 recycle! 메모리 누적 방지
            }

            // 4. 페이지 마무리
            pdfDocument.finishPage(page)
        }

        // 5. 파일로 저장 (스트림 출력)
        FileOutputStream(outputFile).use { fos ->
            pdfDocument.writeTo(fos)
        }

        return ConversionResult.Success(
            outputPath = outputFile.absolutePath,
            outputName = outputFileName,
            pageCount = images.size,
            totalSize = outputFile.length(),
        )
    } finally {
        pdfDocument.close()  // ← 반드시 close
    }
}
```

### 핵심 포인트
1. `BitmapFactory.Options.inJustDecodeBounds = true` — 메타데이터만 읽고 메모리 사용 0
2. `ImageDecoder.decodeBitmap` (API 28+) 또는 `BitmapFactory.decodeStream` 사용
3. 한 페이지 그리고 나서 **즉시 `bitmap.recycle()`**
4. `PdfDocument` 도 `finally` 블록에서 `close()` 보장
5. `FileOutputStream` 으로 스트리밍 출력 (전체 PDF를 메모리에 올리지 않음)
6. `withContext(Dispatchers.IO)` 로 백그라운드 스레드에서 실행

---

## State / Intent 명세

```kotlin
data class ImageToPdfState(
    val selectedImages: List<ImageItem> = emptyList(),
    val outputFileName: String = "",
    val isConverting: Boolean = false,
    val conversionProgress: Float = 0f,          // 0.0 ~ 1.0
    val conversionResult: ConversionResult? = null,
    val error: String? = null,
)

data class ImageItem(
    val uri: Uri,
    val displayName: String,
    val size: Long,
)
```

```kotlin
sealed interface ImageToPdfIntent {
    data class SelectImages(val images: List<ImageItem>) : ImageToPdfIntent
    data class SetOutputName(val name: String) : ImageToPdfIntent
    data object StartConversion : ImageToPdfIntent
    data object Reset : ImageToPdfIntent
    data object DismissResult : ImageToPdfIntent
}
```

---

## ViewModel 핵심 로직

```kotlin
@HiltViewModel
class ImageToPdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val convertImagesToPdfUseCase: ConvertImagesToPdfUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ImageToPdfState())
    val state: StateFlow<ImageToPdfState> = _state.asStateFlow()

    fun onIntent(intent: ImageToPdfIntent) {
        when (intent) {
            is ImageToPdfIntent.SelectImages -> selectImages(intent.images)
            is ImageToPdfIntent.SetOutputName -> setOutputName(intent.name)
            is ImageToPdfIntent.StartConversion -> startConversion()
            is ImageToPdfIntent.Reset -> reset()
            is ImageToPdfIntent.DismissResult -> dismissResult()
        }
    }

    private fun startConversion() {
        val images = _state.value.selectedImages.map { it.uri }
        val name = _state.value.outputFileName.ifBlank { "pdf_${System.currentTimeMillis()}" }

        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isConverting = true, conversionProgress = 0f) }
            
            val imageCount = images.size
            // progressFlow로 진행률 emit...
            
            val result = convertImagesToPdfUseCase(images, name)
            
            _state.update {
                it.copy(
                    isConverting = false,
                    conversionResult = result,
                    conversionProgress = 1f,
                )
            }
        }
    }
}
```

---

## UI 요구사항

### ImageToPdfScreen
- 상단: 출력 파일명 TextField (기본값: 날짜 기반 자동 생성)
- 중앙: 선택된 이미지 Grid (LazyVerticalGrid, cols=3, Coil로 썸네일)
- 하단: "이미지 선택" 버튼 → `ActivityResultContracts.PickMultipleVisualMedia()` 또는 `GetMultipleContents`
- 하단: "PDF 변환 시작" 버튼 (이미지 1장 이상일 때 활성화)
- 변환 중: `ConversionProgressScreen` (ProgressBar + 진행률 %)
- 변환 완료: AlertDialog (성공 → "파일 저장됨" / 실패 → 에러 메시지)
- AdMob 전면 광고: 변환 완료 시점에 `InterstitialAd` 표시 (BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID 사용)

### ImagePickerScreen
- `rememberLauncherForActivityResult(PickVisualMedia())` 또는 `GetMultipleContents("image/*")`
- 선택된 이미지 URI 리스트를 `ImageItem` 으로 변환
- 이미지 메타데이터(이름, 크기)는 ContentResolver로 조회

---

## 유의사항
- **모든 UI 문자열은 한글로 작성**
- PDF 출력 경로: `context.filesDir/pdf_output/` (앱 전용 디렉토리, 권한 불필요)
- 이미지 선택: API 33+ → `PickVisualMedia`, 이하 → `GetMultipleContents` 분기
- `ConversionFileDataSource` 는 `@Singleton`, `Dispatchers.IO` 사용
- 변환 중 화면 회전 대응: ViewModel에서 상태 유지 (기본)
- 끝나면 "내 문서" 화면으로 복귀하거나 출력 폴더로 안내하는 Snackbar
