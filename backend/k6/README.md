# Auction Bid Load Test (k6)

동시 입찰 상황에서 서버 내구성을 보기 위한 k6 스크립트입니다.

## 1) 사전 준비

- `users.json`에 실제 테스트 계정(여러 명) 입력
- 테스트할 경매가 `RUNNING` 또는 `DEADLINE` 상태인지 확인
- 입찰 계정들의 지갑 잔액이 충분한지 확인

## 2) 기본 실행

프로젝트 루트(`IgLoo`)에서 실행:

```bash
k6 run -e BASE_URL=igloo-auction.duckdns.org -e AUCTION_ID=30 backend/k6/auction-bid-load.js
```

## 3) 부하 강도 조절

```bash
k6 run \
  -e BASE_URL=igloo-auction.duckdns.org \
  -e AUCTION_ID=1 \
  -e START_RATE=30 \
  -e STAGE1_TARGET=120 \
  -e STAGE2_TARGET=300 \
  -e STAGE3_TARGET=300 \
  -e PRE_ALLOCATED_VUS=300 \
  -e MAX_VUS=3000 \
  backend/k6/auction-bid-load.js
```

## 4) 주요 환경변수

- `BASE_URL`: API 서버 주소 (기본 `http://localhost:8080`)
- `AUCTION_ID`: 테스트 대상 경매 ID
- `START_RATE`: 시작 입찰 도착률(req/s)
- `STAGE1_TARGET`, `STAGE2_TARGET`, `STAGE3_TARGET`: 단계별 도착률(req/s)
- `STAGE1_DURATION`, `STAGE2_DURATION`, `STAGE3_DURATION`, `STAGE4_DURATION`: 단계 시간
- `PRE_ALLOCATED_VUS`, `MAX_VUS`: k6 VU 풀 크기
- `BID_MULTIPLIER`: 최소 입찰 배수 (기본 `1.1`)
- `BID_STEP`: 입찰 금액 반올림 단위 (기본 `10`)
- `EXTRA_STEPS_MAX`: 최소금액 대비 랜덤 추가 step 수 (기본 `5`)

## 5) 지표 해석

- `http_req_duration{name:bid_place}`: 입찰 API 지연시간
- `bid_created_rate`: 실제 입찰 성공(201) 비율
- `bid_business_reject_rate`: 비즈니스 규칙에 의한 거절 비율(4xx)
- `bid_system_error_rate`: 서버 오류(5xx) 비율
- `bid_unexpected_response_rate`: 분류되지 않은 응답 비율
