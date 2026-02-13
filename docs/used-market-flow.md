# 중고거래 판매 자동화: 구현 플로우와 계획

## 목표
판매자가 상품 사진을 올리면 이미지 분석 → 설명 자동 작성 → 판매글 게시까지 일괄 처리되는 파이프라인 구축.

## 전체 플로우(요약)
1. 판매자 사진 업로드 (구현됨)
2. AI 이미지 분석 - AI
3. 분석내용 기반 카테고리 분류 - AI
4. 설명 생성 - AI
5. 판매글 검수/수정 - USER
6. 플랫폼에 게시 (구현됨) - USER
7. 성과/피드백 수집 및 모델 개선

## 시스템 구성요소
- 업로드/수집
  - 이미지 업로드 API (구현됨), 메타데이터 입력 UI(필수: 카테고리/브랜드/모델/구매시기 등)
- AI 이미지 분석
  - 사실 추출: 객체/브랜드/모델/상태/OCR, 품질 체크
- 카테고리 분류
  - 분석내용을 카테고리로 매핑(후보 + 신뢰도)
- 설명 생성
  - 템플릿 + LLM 결합, 사실 기반
- 설명 생성
  - 템플릿 + LLM 결합
  - 사실 기반, 과장 금지, 누락 필수 항목 체크
- 게시/자동화
  - 플랫폼 API (구현됨) 또는 브라우저 자동화
- 모니터링
  - 성과 지표(조회/문의/판매완료), 내용 품질 피드백

## 구현 단계별 계획

### 1단계: 요구사항/데이터 수집
- 플랫폼 정책 확인
- 카테고리별 필수 필드 정의
- 설명 작성에 필요한 표준 용어/문구 수집

### 2단계: AI 이미지 분석 MVP
- 최소 기능: 브랜드/모델 OCR + 상태/구성품 추출
- 이미지 품질 필터링(저품질 시 재촬영 유도)

### 3단계: 카테고리 분류 MVP
- 분석내용 기반 후보 카테고리 산출
- 신뢰도 낮으면 사용자 확인 요청

### 4단계: 설명 생성 MVP
- 템플릿 기반 자동 작성
- 필수 항목 누락 체크
- 사용자 수정 가능

### 5단계: 게시 자동화
- 플랫폼 API 연동 또는 브라우저 자동화
- 게시 실패/검수 대응 로직

### 6단계: 고도화
- 멀티모달 모델로 상태/하자 판단
- 설명 생성 품질 향상(카테고리별 룰)
- 성과 피드백을 통한 자동 개선

## 리스크 및 대응
- 허위/과장 설명: LLM 출력 검수 룰 적용
- 플랫폼 정책 변경: API/자동화 모듈 분리
- 이미지 품질 미흡: 업로드 단계에서 재촬영 유도

## 필요 기술 스택(예시)
- 백엔드: Java/Spring 또는 Node
- 이미지 분석: OpenCV + OCR(Tesseract)
- 설명 생성: LLM API
- 자동화: 플랫폼 API, 없으면 Selenium/Playwright

## 파이썬 AI 서비스 분리(권장)
- 목적: 이미지 분석/카테고리 분류/설명 생성을 파이썬 서비스로 분리하여 모델 변경과 실험을 빠르게 반영
- 통신 방식: Spring 백엔드 → Python AI Service HTTP 호출
- 배포: 단일 EC2에서 컨테이너/프로세스로 분리 운영

### 서비스 인터페이스(예시)
- `POST /ai/analyze-image`
  - 입력: 이미지 URL, 선택 메타데이터
  - 출력: 1단계 분석 JSON
- `POST /ai/classify-category`
  - 입력: 1단계 분석 JSON
  - 출력: 2단계 카테고리 분류 JSON
- `POST /ai/generate-description`
  - 입력: 카테고리 확정 + 분석 결과
  - 출력: 3단계 설명 생성 JSON + 게시용 payload

### OpenAI API 사용(요약)
- 인증: `OPENAI_API_KEY` 환경변수 사용
- 이미지 입력: URL 또는 Base64 데이터 URL을 입력으로 전달
- 호출 방식: Python SDK의 Responses API 사용

### 파이썬 서비스 스켈레톤(요약)
- 프레임워크: FastAPI
- 모듈 구조(예시)
  - `app/main.py` (앱 엔트리포인트)
  - `app/routes/ai.py` (API 라우팅)
  - `app/services/openai_client.py` (OpenAI 호출 래퍼)
  - `app/services/pipeline.py` (3단계 파이프라인)
  - `app/schemas.py` (요청/응답 스키마)

### 요청/응답 스키마 매핑(요약)
- `AnalyzeImageRequest` → 1단계 분석 JSON
- `ClassifyCategoryRequest` → 2단계 분류 JSON
- `GenerateDescriptionRequest` → 3단계 설명 JSON + `auction_register_req`

## 백엔드 연동 요청/응답(현 구현 기준)

### 1) 이미지 업로드(S3) - 단일
**Request** `POST /api/images?type=item`
```http
Content-Type: multipart/form-data
```
`file` 필드로 업로드

**Response**
```json
{
  "message": "이미지 업로드 성공",
  "data": "https://cdn.example.com/items/abc.jpg"
}
```

### 2) 이미지 업로드(S3) - 다중
**Request** `POST /api/images/multiple?type=item`
```http
Content-Type: multipart/form-data
```
`files` 필드로 다중 업로드

**Response**
```json
{
  "message": "이미지 업로드 성공",
  "data": [
    "https://cdn.example.com/items/abc.jpg",
    "https://cdn.example.com/items/def.jpg"
  ]
}
```

### 3) 경매(판매글) 등록
**Request** `POST /api/auctions`
```json
{
  "title": "아이폰 13 128GB 블랙 판매합니다",
  "description": "아이폰 13 128GB 블랙입니다. 사용감은 있으나 큰 하자는 없습니다. 구성품은 본체, 박스, 충전기 포함입니다. 직거래/택배 가능해요.",
  "startPrice": 10000,
  "categoryId": 1,
  "auctionDuration": 1440,
  "startAt": "2026-02-11T10:00:00",
  "endAt": "2026-02-12T10:00:00",
  "imageUrls": [
    "https://cdn.example.com/items/abc.jpg",
    "https://cdn.example.com/items/def.jpg"
  ]
}
```

**Response**
```json
{
  "message": "Auction registered successfully",
  "data": 12345
}
```

### 4) 내부 호출: Python AI Service
**Request** `POST /ai/analyze-image`
```json
{
  "imageUrls": [
    "https://cdn.example.com/items/abc.jpg",
    "https://cdn.example.com/items/def.jpg"
  ],
  "metadata": {
    "seller_note": "사용감 조금 있음",
    "purchase_year": 2023
  }
}
```

**Response** (1단계 분석 JSON)
```json
{
  "brand": { "value": "Apple", "confidence": 0.81 },
  "model": { "value": "iPhone 13", "confidence": 0.74 },
  "condition": { "value": "USED_GOOD", "confidence": 0.62 },
  "defects": [
    { "type": "SCRATCH", "location": "back", "severity": "LOW", "confidence": 0.58 }
  ],
  "accessories": ["charger", "box"],
  "text_ocr": ["iPhone", "128GB"],
  "image_quality": {
    "is_blurry": false,
    "is_overexposed": false,
    "resolution_ok": true
  }
}
```

**Request** `POST /ai/classify-category`
```json
{
  "brand": { "value": "Apple", "confidence": 0.81 },
  "model": { "value": "iPhone 13", "confidence": 0.74 },
  "text_ocr": ["iPhone", "128GB"]
}
```

**Response** (2단계 분류 JSON)
```json
{
  "category_candidates": [
    { "category_id": 1, "category_path": "전자기기/IT", "confidence": 0.72 },
    { "category_id": 2, "category_path": "전자기기/IT > 모바일", "confidence": 0.18 }
  ],
  "selected_category_id": 1,
  "selection_reason": "brand/model match + OCR keywords",
  "needs_user_confirmation": false
}
```

**Request** `POST /ai/generate-description`
```json
{
  "category_id": 1,
  "brand": "Apple",
  "model": "iPhone 13",
  "condition": "USED_GOOD",
  "defects": [
    { "type": "SCRATCH", "location": "back", "severity": "LOW" }
  ],
  "accessories": ["charger", "box"],
  "image_urls": [
    "https://cdn.example.com/items/abc.jpg",
    "https://cdn.example.com/items/def.jpg"
  ]
}
```

**Response** (3단계 설명 + 게시용 payload)
```json
{
  "title": "아이폰 13 128GB 블랙 판매합니다",
  "summary": "상태 양호, 기본 구성품 포함",
  "body": "아이폰 13 128GB 블랙입니다. 사용감은 있으나 큰 하자는 없습니다. 구성품은 본체, 박스, 충전기 포함입니다. 직거래/택배 가능해요.",
  "hashtags": ["#아이폰13", "#128GB", "#블랙", "#중고폰"],
  "auction_register_req": {
    "title": "아이폰 13 128GB 블랙 판매합니다",
    "description": "아이폰 13 128GB 블랙입니다. 사용감은 있으나 큰 하자는 없습니다. 구성품은 본체, 박스, 충전기 포함입니다. 직거래/택배 가능해요.",
    "startPrice": 10000,
    "categoryId": 1,
    "auctionDuration": 1440,
    "startAt": "2026-02-11T10:00:00",
    "endAt": "2026-02-12T10:00:00",
    "imageUrls": [
      "https://cdn.example.com/items/abc.jpg",
      "https://cdn.example.com/items/def.jpg"
    ]
  }
}
```

## 산출물
- 요구사항 정리 문서
- 데이터 스키마
- 설명 생성 템플릿
- 게시 자동화 모듈
- 모니터링 대시보드

## 단계별 JSON 스키마(초안)

### 1단계 출력: AI 이미지 분석(JSON)
```json
{
  "brand": { "value": "Apple", "confidence": 0.81 },
  "model": { "value": "iPhone 13", "confidence": 0.74 },
  "condition": { "value": "USED_GOOD", "confidence": 0.62 },
  "defects": [
    { "type": "SCRATCH", "location": "back", "severity": "LOW", "confidence": 0.58 }
  ],
  "accessories": ["charger", "box"],
  "text_ocr": ["iPhone", "128GB"],
  "image_quality": {
    "is_blurry": false,
    "is_overexposed": false,
    "resolution_ok": true
  }
}
```

### 2단계 출력: 카테고리 분류(JSON)
```json
{
  "category_candidates": [
    { "category_id": 1, "category_path": "전자기기/IT", "confidence": 0.72 },
    { "category_id": 2, "category_path": "전자기기/IT > 모바일", "confidence": 0.18 }
  ],
  "selected_category_id": 1,
  "selection_reason": "brand/model match + OCR keywords",
  "needs_user_confirmation": false
}
```

### 3단계 출력: 설명 생성(JSON) + 게시용 Payload
```json
{
  "title": "아이폰 13 128GB 블랙 판매합니다",
  "summary": "상태 양호, 기본 구성품 포함",
  "body": "아이폰 13 128GB 블랙입니다. 사용감은 있으나 큰 하자는 없습니다. 구성품은 본체, 박스, 충전기 포함입니다. 직거래/택배 가능해요.",
  "hashtags": ["#아이폰13", "#128GB", "#블랙", "#중고폰"],
  "auction_register_req": {
    "title": "아이폰 13 128GB 블랙 판매합니다",
    "description": "아이폰 13 128GB 블랙입니다. 사용감은 있으나 큰 하자는 없습니다. 구성품은 본체, 박스, 충전기 포함입니다. 직거래/택배 가능해요.",
    "startPrice": 10000,
    "categoryId": 1,
    "auctionDuration": 1440,
    "startAt": "2026-02-11T10:00:00",
    "endAt": "2026-02-12T10:00:00",
    "imageUrls": [
      "https://cdn.example.com/items/abc.jpg",
      "https://cdn.example.com/items/def.jpg"
    ]
  }
}
```
