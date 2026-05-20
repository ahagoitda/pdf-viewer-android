당신은 2026년 기준 시니어 안드로이드 아키텍트입니다. 아래 프로젝트의 **Step 3 (Presentation Layer + MVI ViewModel)** 을 구현하세요.

---

## 프로젝트 경로

```
C:\pdf viewer android app
```

## 현재까지 완료된 작업

### Step 1: 프로젝트 구조 및 의존성 (완료)
- Kotlin 2.1.0 + Compose BOM 2025.01.00
- Dagger Hilt (KSP), Room (KSP), Coroutines, Navigation Compose, AdMob, Coil
- BuildConfig로 AdMob ID 주입 (local.properties → build.gradle.kts → AndroidManifest)
- Gradle 8.10.2, AGP 8.7.2, minSdk 26, targetSdk 35

### Step 2: Domain + Data Layer (완료)

**Domain 모델:**
- `PdfDocument(uri, name, size, lastModified, pageCount?)` — PDF 파일 엔티티
- `ConversionResult` — sealed class (Success/Error)

**Repository 인터페이스:**
- `DocumentRepository` — getPdfDocuments(), deleteDocument(), getRecentDocuments(): Flow, markDocumentOpened()
- `ConversionRepository` — convertImagesToPdf(images: List<Uri>, outputFileName: String)

**UseCase (모두 @Inject 생성자):**
- `GetPdfDocumentsUseCase` — suspend () -> Result<List<PdfDocument>>
- `GetRecentDocumentsUseCase` — () -> Flow<List<PdfDocument>>
- `DeleteDocumentUseCase` — suspend (uri: String) -> Result<Unit>
- `ConvertImagesToPdfUseCase` — suspend (images, outputFileName) -> ConversionResult

**Data 구현체:**
- `DocumentFileDataSource` — MediaStore로 PDF 파일 쿼리 (VOLUME_EXTERNAL, API Q+), URI 기반 삭제
- `ConversionFileDataSource` — 현재 스켈레톤 (Step 5에서 구현)
- `DocumentRepositoryImpl` — FileDataSource + Room 통합
- `ConversionRepositoryImpl` — ConversionFileDataSource 위임
- Room DB: `RecentDocumentEntity` (uri PK), `RecentDocumentDao` (Flow, REPLACE upsert)

**Hilt DI:**
- `DatabaseModule` — Room DB + DAO 제공
- `RepositoryModule` — @Binds로 인터페이스 바인딩
- `AppModule` — 빈 모듈 (추가 가능)

---

## Step 3 구현할 내용: Presentation Layer + MVI ViewModel

### 3-1. MVI State / Intent 정의

`presentation/state/` 에 각 화면별 State data class 작성:

```kotlin
// DocumentListState.kt
data class DocumentListState(
    val documents: List<PdfDocument> = emptyList(),
    val recentDocuments: List<PdfDocument> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val permissionGranted: Boolean = false,
)

// DocumentListIntent.kt
sealed interface DocumentListIntent {
    data object LoadDocuments : DocumentListIntent
    data class DeleteDocument(val uri: String) : DocumentListIntent
    data class OpenDocument(val document: PdfDocument) : DocumentListIntent
    data object RequestPermission : DocumentListIntent
}
```

### 3-2. ViewModel

`presentation/viewmodel/DocumentListViewModel.kt` — MVI 패턴:
- `@HiltViewModel`, `@Inject` 생성자로 UseCase 주입
- `StateFlow<DocumentListState>` 상태 방출
- `onIntent(DocumentListIntent)` 함수로 Intent 처리
- 권한 체크 로직 포함 (API 33+: `READ_MEDIA_IMAGES`, 이하: `READ_EXTERNAL_STORAGE`)
- ViewModel init 시점에 Room의 `getRecentDocuments()` Flow 구독

### 3-3. Navigation

`presentation/ui/navigation/PdfUtilityNavHost.kt`:
- `NavHost` 정의 (3개 화면: DocumentList, PdfViewer, ImageToPdf)
- 인자: `pdfUri: String` (PDF 뷰어), 선택적
- Bottom Navigation Bar 또는 Top App Bar로 화면 전환

### 3-4. DocumentList Screen

`presentation/ui/documentlist/DocumentListScreen.kt`:
- 권한 요청 다이얼로그 (내장 Compose `rememberLauncherForActivityResult`)
- PDF 목록 LazyColumn (이름, 크기, 날짜 표시)
- 최근 열람 섹션 구분
- 삭제 확인 다이얼로그 + 스와이프 삭제
- AdMob 배너 광고 (하단 고정)
- 빈 상태 UI (Empty state)

### 3-5. AppBar / Scaffold 공통 컴포넌트

`presentation/ui/common/`:
- `PdfUtilityScaffold.kt` — 공통 Scaffold + TopAppBar + BottomAdBanner
- 광고 컴포넌트 `AdMobBanner.kt` — AndroidView로 AdView 래핑

### 3-6. MainActivity 연동

`MainActivity.kt` 수정 — Surface 내부에 `PdfUtilityNavHost` 추가

---

## 🚨 Step 3 반드시 준수할 사항

1. **MVI 패턴 엄수**: ViewModel은 `onIntent()` 단일 진입점, State는 단방향 Flow (StateFlow)
2. **권한**: `rememberLauncherForActivityResult` + `RequestPermission` contract 사용, API 레벨 분기
3. **광고**: AdMob 배너는 `AndroidView`로 Compose에 임베드, `BuildConfig.ADMOB_BANNER_AD_UNIT_ID` 사용
4. **Hilt**: `@HiltViewModel` + `hiltViewModel()` 사용
5. **네비게이션**: `hiltNavigationCompose`의 `hiltViewModel()` 스코프드 사용
6. **빈 화면**: Empty State UI (아이콘 + 안내 문구)
7. **한글 UI**: 앱 내 모든 문자열은 한글로 작성

---

## 기술 스택 참고 (이미 설정됨)

- `libs.navigation.compose` → `androidx.navigation:navigation-compose`
- `libs.hilt.navigation.compose` → `androidx.hilt:hilt-navigation-compose`
- `libs.admob` → `com.google.android.gms:play-services-ads`
- `libs.lifecycle.viewmodel.compose` → `androidx.lifecycle:lifecycle-viewmodel-compose`
- `libs.lifecycle.runtime.compose` → `androidx.lifecycle:lifecycle-runtime-compose`

## Step 3 결과물로 생성해야 할 파일

```
presentation/
├── intent/DocumentListIntent.kt
├── state/DocumentListState.kt
├── viewmodel/DocumentListViewModel.kt
└── ui/
    ├── navigation/PdfUtilityNavHost.kt
    ├── documentlist/DocumentListScreen.kt
    ├── common/PdfUtilityScaffold.kt
    └── common/AdMobBanner.kt
```

그리고 `MainActivity.kt` 수정 (NavHost 추가).
