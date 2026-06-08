# PDF Utility (Android)

Android 기기 내에서 PDF/HWPX 문서를 열람하고 다양한 형식으로 변환 및 내보내기를 수행할 수 있는 네이티브 문서 뷰어 및 유틸리티 애플리케이션입니다. 외부 서버와의 통신 없이 모든 변환 로직이 기기 내부(Local-only)에서 안전하게 처리되도록 설계되었습니다.

---

## 🛠️ 주요 기능

### 1. PDF 문서 열람 및 고속 렌더링 (Viewer)
- **초고속 스크롤 및 렌더링 최적화**:
  - **9-Page Cache Buffer**: 사용자가 보고 있는 현재 페이지를 기준으로 앞뒤 4페이지(총 9페이지)의 비트맵 렌더링 결과를 메모리에 캐싱하여, 급격한 스크롤 중에도 화면 깜빡임과 빈 화면(Black Frame) 현상을 완전히 배제합니다.
  - **Fine-grained Recomposition**: 비트맵 캐시 상태를 Compose의 `SnapshotStateMap`으로 매핑하여, 전체 리스트가 아닌 업데이트가 발생한 개별 페이지 아이템만 정밀하게 리컴포지션하도록 제어합니다.
  - **안정적인 메모리 관리**: Jetpack Compose 렌더링 큐 내부의 비트맵 재사용 에러(`Canvas: trying to use a recycled bitmap`)를 근본적으로 방지하기 위해 manual `.recycle()` 호출 방식을 제거하고, API 26+ 가상 머신에 최적화된 JVM GC 기반 메모리 정리 모델을 적용하여 100% 충돌 없는 부드러운 화면 전환을 보장합니다.
- **스마트 제스처 확대/축소 및 이동**:
  - 상단 탑바의 **확대(+) / 축소(-)** 버튼 지원
  - 두 손가락 핀치 줌(Pinch-to-zoom) 지원 (상하/좌우 수직 간섭 없는 최적화 제스처 적용)
  - 더블 탭(Double Tap) 시 `2.5배` 자동 확대 / 원본 비율 복원 토글 지원
  - 확대 상태에서 한 손가락 드래그를 통한 상하좌우 자유 이동(Pan) 지원 (화면 이탈 방지 처리)
- **페이지 표시기**: 현재 읽고 있는 페이지 번호를 화면 내에 플로팅 요소로 선명하게 표시합니다.

### 2. 이미지 결합 및 스마트 순서 재배치 (Image to PDF)
- **인터랙티브 그리드 정렬 (Interactive Grid Sort)**:
  - 다중 이미지를 무작위로 선택하여 추가하더라도, 결합 전에 직관적인 3열 그리드 화면에서 합칠 순서를 직접 커스텀 지정할 수 있습니다.
  - 추가된 이미지 위에 선택한 순서대로 **순서 번호 뱃지**가 동적으로 표시되며, 터치 한 번으로 쉽게 정렬 순서를 토글할 수 있습니다.
  - 파일명 기준 **"이름순 정렬(가나다/ABC)"** 및 선택 상태 전체 초기화(`Reset`) 단축 버튼을 제공합니다.
  - 최종 정렬 완료 후 변환을 누르면 사용자가 정한 완벽한 순서대로 PDF 병합이 완료됩니다.

### 3. 한컴 문서 열람 (Hancom Viewer)
- **HWPX 텍스트 뷰어**: HWPX 내부 XML을 로컬에서 파싱하여 본문 텍스트를 표시합니다.
- **외부 앱 열기 연동**: Android `ACTION_VIEW`로 전달된 HWPX 파일을 바로 열 수 있습니다.
- **HWP 안내 처리**: 구형 HWP 바이너리는 현재 미지원이며 HWPX 저장본 사용을 안내합니다.

### 4. 다양한 저장 및 공유 기능 (Export)
- **외부 공유 (Share)**: `FileProvider`를 통해 열려 있는 PDF를 외부 다른 앱으로 안전하게 공유합니다.
- **이미지 변환 저장**: PDF의 모든 페이지를 고해상도 JPEG 파일로 변환하여 기기의 갤러리(`Pictures/PdfUtility/`) 경로에 자동으로 저장 및 등록합니다.
- **포맷 변환 및 내보내기 (Save As...)**:
  - **PDF 파일 (.pdf)로 저장**: Storage Access Framework (SAF)를 통해 사용자가 지정한 경로에 원본 PDF를 복사하여 저장합니다.
  - **텍스트 파일 (.txt)로 추출**: `pdfbox-android` 라이브러리를 탑재하여 PDF 내부 텍스트를 파싱하고 로컬 메모장 파일(.txt)로 저장합니다.
  - **워드 문서 (.docx)로 변환**: 무거운 외부 오피스 라이브러리(Apache POI 등) 없이, 내부에서 직접 Word 표준 아카이브 스키마를 구성하여 텍스트 포맷 기반의 `.docx` 워드 문서를 가볍게 생성 및 저장합니다.

---

## 🎨 프리미엄 UI/UX 및 디자인 가이드

- **Material Design 3 (M3) 테마**:
  - 세련되고 차분한 색조를 사용하여 오랜 시간 문서를 읽어도 눈이 피로하지 않도록 돕습니다.
  - 메인 화면 및 최근 문서 목록에 고급스러운 카드 레이아웃(`surfaceContainerLow`)을 전면 배치하여 직관성과 완성도를 끌어올렸습니다.
  - 각 카드 왼쪽 영역에 원형 아이콘과 프라이머리 컬러 액센트 바를 더해 시각적인 균형감을 부여했습니다.
- **프리미엄 어댑티브 앱 아이콘**:
  - **배경(Background)**: Slate `#0F172A` 계열의 깊이감 있는 다크 톤 디자인 적용.
  - **전경(Foreground)**: 흰색의 도큐먼트 폴더 형태에 입체적인 폴딩 입체 그라데이션 및 코랄 레드(#FF5A5F) 계열의 액센트 그라데이션 'P' 리본을 배치하여, 디바이스의 아이콘 형태(원형, 사각형, 스쿼클 등)에 관계없이 최고 수준의 브랜드 아이덴티티를 유지합니다.

---

## ⚙️ 아키텍처 및 기술 스택

- **디자인 패턴**: MVI (Model-View-Intent) 아키텍처 패턴을 기반으로 단방향 데이터 흐름(UDF) 보장
- **UI 프레임워크**: Jetpack Compose (100% 네이티브 UI 구성)
- **비동기 처리**: Kotlin Coroutines & Flow (백그라운드 스레드에서 무거운 렌더링 및 변환 작업 병렬 처리)
- **의존성 주입**: Dagger Hilt
- **최적화 핵심 기술**: Compose `SnapshotStateMap`, Sliding-Window Cache System (9-Page cache)
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
    ├── intent/                      # MVI Intent 정의 (PdfViewerIntent, ImageToPdfIntent 등)
    ├── state/                       # UI State 정의 (PdfViewerState, ExportState 등)
    ├── ui/                          # Compose 컴포저블 화면 구성
    │   ├── documentlist/            # M3 디자인 적용된 메인 최근 문서 화면
    │   ├── imagetopdf/              # 이미지 순서 정렬 및 병합 화면
    │   └── pdfviewer/               # 제스처 및 렌더링 최적화 뷰어 화면
    └── viewmodel/                   # ViewModel (PdfViewerViewModel, ImageToPdfViewModel 등)
```
