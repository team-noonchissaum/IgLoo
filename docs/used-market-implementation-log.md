# 중고거래 자동화 구현 로그

## 2026-02-11
- 작업 시작: `docs/used-market-flow.md` 기준 구현 착수.
- 적용 범위 가정: 백엔드(Spring) + Python AI 서비스(동일 레포 내 `ai-service` 디렉터리).
- 진행 계획: AI 서비스 스켈레톤 추가 → 백엔드 연동 클라이언트/컨트롤러 추가 → 설정/문서 정리.
- 완료: FastAPI 스켈레톤 및 3개 엔드포인트(`/ai/analyze-image`, `/ai/classify-category`, `/ai/generate-description`) 추가.
- 완료: 백엔드 `AiServiceClient` + `AiController` 추가, `application.yml`에 AI 서비스 URL 설정 추가.
- 완료: 백엔드 통합 엔드포인트 `/api/ai/pipeline` 추가(분석→분류→설명 생성 순차 실행).
- 변경: 설명 생성 payload의 `startPrice` 기본값 제거(사용자가 직접 입력).
- 완료: `ai-service`에 OpenAI Responses API 연동(설명 생성/분석/분류).
- 변경: 카테고리 분류 시 기존 카테고리 목록을 전달하도록 수정(신규 생성 방지).
- 변경: 설명 생성 프롬프트를 짧은 문장(2문장, 200자 이내) 기준으로 조정.
- 변경: 분류 결과가 유효하지 않으면 `기타` 카테고리로 강제 매핑.
- 변경: 카테고리 목록에 경로(`path`) 포함하여 유사/일치 매핑 정확도 보강.
- 변경: 카테고리 분류 프롬프트 강화(leaf 우선, 미매칭 시 `기타`).
- 변경: 설명 생성 프롬프트 길이 상향 및 UNKNOWN 제거 가이드 추가.
- 추가: 카테고리 별칭 JSON(`category-aliases.json`)로 매핑 보강.
- 변경: 카테고리 분류 시 leaf 카테고리만 선택하도록 `isLeaf` 전달.
- 변경: 설명 생성 시 `item_type` 전달(브랜드/모델 미확정 시 “카메라/신발 팝니다” 형태 지원).
- 추가: 별칭 기반 키워드 매칭으로 카테고리 분류 보정 로직 추가.
- 변경: 분류 요청에 `keywords`(메타데이터/OCR/브랜드/모델) 전달.
- 변경: 대분류 선택 시 leaf 카테고리로 강제 보정 로직 추가.
- 변경: AI 서비스에서 분석/분류 결과 요약 로그 출력 추가.
- 변경: 분류 폴백 시 키워드 기반 카테고리 선택 로직 추가.
- 변경: OpenAI 분석/분류 응답 형식 보정 로직 추가.
- 변경: 분류 후보 비어있을 때 선택된 카테고리 후보 자동 보강.
- 변경: `category-aliases.json` 확장 및 카테고리 ID 재매핑.
- 완료: AI 서비스 Dockerfile 추가 및 docker-compose에 ai-service 포함.
- 완료: GitHub Actions에 ai-service 빌드/배포 추가.
- 변경: S3 이미지 URL을 presigned URL로 변경(OpenAI 접근 가능).
- 변경: AI 설명 생성 시 로그 출력 추가.
- 변경: AI 설명 생성 로그를 `print`로 강제 출력.
- 변경: `ai-service`에서 `.env` 자동 로딩 추가(`python-dotenv`).
- 변경: OpenAI 오류 상세를 콘솔에 출력.
- 변경: 구버전 SDK 호환을 위해 `response_format` 미지원 시 JSON 파싱 폴백.
- 변경: `auction_register_req` 형식 불일치 시 기본 payload로 보정.
- 변경: `auction_register_req` snake_case 키 보정 및 필수값 자동 채움.
- 리팩토링: 설명 생성 로직을 헬퍼로 분리하고 payload 보정 함수 정리.

## 프론트 연동 정리 (AI 설명 생성 버튼 방식)

### 흐름
1) 이미지 업로드 (`/api/images` 또는 `/api/images/multiple`)
2) 이미지 업로드 응답의 `imageUrls` 확보
3) 사용자가 “AI 설명 생성” 버튼 클릭
4) `POST /api/ai/pipeline` 호출
5) 결과를 UI에 보여주고 사용자가 수정/보완
6) 최종 등록은 `POST /api/auctions`로 처리

### AI 파이프라인 호출
**Request** `POST /api/ai/pipeline`
```json
{
  "imageUrls": [
    "https://cdn.example.com/items/abc.jpg",
    "https://cdn.example.com/items/def.jpg"
  ],
  "metadata": {
    "seller_note": "사용감 조금 있음",
    "purchase_year": 2023
  },
  "startPrice": 5000,
  "auctionDuration": 1440,
  "startAt": "2026-02-11T10:00:00",
  "endAt": "2026-02-12T10:00:00"
}
```
- `startPrice`는 사용자가 입력 (없으면 생략 가능)
- `auctionDuration/startAt/endAt`는 필요 시만 전달

**Response**
```json
{
  "message": "AI pipeline completed",
  "data": {
    "analyzeResult": {
      "brand": { "value": "UNKNOWN", "confidence": 0.1 },
      "model": { "value": "UNKNOWN", "confidence": 0.1 },
      "condition": { "value": "USED", "confidence": 0.4 },
      "defects": [],
      "accessories": [],
      "text_ocr": [],
      "image_quality": {
        "is_blurry": false,
        "is_overexposed": false,
        "resolution_ok": true
      }
    },
    "classifyResult": {
      "category_candidates": [
        { "category_id": 1, "category_path": "전자기기/IT", "confidence": 0.3 },
        { "category_id": 2, "category_path": "전자기기/IT > 모바일", "confidence": 0.2 }
      ],
      "selected_category_id": 1,
      "selection_reason": "default fallback (model not connected)",
      "needs_user_confirmation": true
    },
    "descriptionResult": {
      "title": "UNKNOWN UNKNOWN 판매합니다",
      "summary": "사용감 있음, 구성품: 없음",
      "body": "UNKNOWN UNKNOWN입니다. 상태는 사용감 있음입니다. 구성품: 없음 직거래/택배 가능합니다.",
      "hashtags": ["#UNKNOWN", "#UNKNOWN", "#중고거래"],
      "auction_register_req": {
        "title": "UNKNOWN UNKNOWN 판매합니다",
        "description": "UNKNOWN UNKNOWN입니다. 상태는 사용감 있음입니다. 구성품: 없음 직거래/택배 가능합니다.",
        "startPrice": 5000,
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
  }
}
```

### 프론트 처리 포인트
- `descriptionResult`를 사용자에게 노출 후 수정 가능하도록 제공
- `startPrice`는 사용자 입력 필드로 받음
- 카테고리 신뢰도가 낮으면 사용자 확인(옵션)
