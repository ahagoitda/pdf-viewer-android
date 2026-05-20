# PDF Utility (Android)

Android 기기 내에서 PDF 문서를 열람하고 다양한 형식으로 변환 및 내보내기를 수행할 수 있는 네이티브 PDF 뷰어 및 유틸리티 애플리케이션입니다. 외부 서버와의 통신 없이 모든 변환 로직이 기기 내부(Local-only)에서 처리되도록 설계되었습니다.

---

## 🛠️ 주요 기능

### 1. PDF 문서 열람 및 탐색 (Viewer)
- **최근 열람 기록**: 이전에 열었던 PDF 파일 목록을 메인 화면에서 확인하고 빠르게 다시 열 수 있습니다.
- **고해상도 페이지 렌더링**: Android 내장 `PdfRenderer` API를 활용하여 PDF 페이지를 부드럽게 렌더링합니다.
- **스마트 제스처 확대/축소 및 이동**:
  - 상단 탑바의 **확대(+) / 축소(-)** 버튼 지원
  - 두 손가락 핀치 줌(Pinch-to-zoom) 지원 (상하/좌우 수직 간섭 없는 최적화 제스처 적용)
  - 더블 탭(Double Tap) 시 `2.5배` 자동 확대 / 원본 비율 복원 토글 지원
  - 확대 상태에서 한 손가락 드래그를 통한 상하좌우 자유 이동(Pan) 지원 (화면 이탈 방지 처리)
- **페이지 표시기**: 현재 읽고 있는 페이지 번호를 하단 또는 화면 내에 표시합니다.

### 2. 다양한 저장 및 공유 기능 (Export)
- **외부 공유 (Share)**: `FileProvider`를 통해 열려 있는 PDF를 외부 다른 앱으로 안전하게 공유합니다.
- **이미지 변환 저장**: PDF의 모든 페이지를 고해상도 JPEG 파일로 변환하여 기기의 갤러리(`Pictures/PdfUtility/`) 경로에 자동으로 저장 및 등록합니다.
- **포맷 변환 및 내보내기 (Save As...)**:
  - **PDF 파일 (.pdf)로 저장**: Storage Access Framework (SAF)를 통해 사용자가 지정한 경로에 원본 PDF를 복사하여 저장합니다.
  - **텍스트 파일 (.txt)로 추출**: `pdfbox-android` 라이브러리를 탑재하여 PDF 내부 텍스트를 파싱하고 로컬 메모장 파일(.txt)로 저장합니다.
  - **워드 문서 (.docx)로 변환**: 무거운 외부 오피스 라이브러리(Apache POI 등) 없이, 내부에서 직접 Word 표준 아카이브 스키마를 구성하여 텍스트 포맷 기반의 `.docx` 워드 문서를 가볍게 생성 및 저장합니다.

---

## ⚙️ 아키텍처 및 핵심 라이브러리

- **MVI (Model-View-Intent) 패턴**: UI 상태(`PdfViewerState`)와 행동 정의(`PdfViewerIntent`)를 명확하게 분리하여 데이터 흐름을 안전하게 단방향으로 관리합니다.
- **UI 프레임워크**: Jetpack Compose (Kotlin 100% 네이티브 UI)
- **비동기 처리**: Coroutines & Flow (백그라운드 스레드에서 파일 복사, 이미지 렌더링, 텍스트 파싱 진행)
- **의존성 주입**: Dagger Hilt
- **텍스트 파싱 라이브러리**: `pdfbox-android` (v2.0.27.0)

---

## 📂 프로젝트 구조 (핵심 디렉토리)

```text
app/src/main/java/com/pdfutility/
├── MainActivity.kt                  # 앱 진입점
├── PdfUtilityApp.kt                # Application 클래스 (PDFBox 리소스 로더 초기화)
├── data/                            # 데이터 및 저장소 (최근 기록 데이터베이스 등)
├── domain/                          # 비즈니스 유스케이스 정의
└── presentation/                    # UI 및 상태 제어
    ├── intent/                      # MVI Intent 정의 (PdfViewerIntent)
    ├── state/                       # UI State 정의 (PdfViewerState, ExportState)
    ├── ui/                          # Compose 컴포저블 화면 구성 (PdfViewerScreen 등)
    └── viewmodel/                   # ViewModel 비즈니스 연동 (PdfViewerViewModel)
```
