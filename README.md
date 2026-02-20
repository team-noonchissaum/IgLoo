# IgLoo

> 기존의 당근마켓, 번개장터, 중고나라와 같은 중고거래 서비스 모델에 **경매 낙찰 시스템**을 도입하여 거래의 재미와 효율성을 극대화한 서비스

![Backend](https://img.shields.io/badge/Backend-Spring%20Boot%203.4.1-6DB33F?logo=springboot&logoColor=white)
![AI Service](https://img.shields.io/badge/AI%20Service-FastAPI-009688?logo=fastapi&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)

---

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [핵심 차별화 포인트](#핵심-차별화-포인트)
- [핵심 기능](#핵심-기능)
- [시스템 아키텍처](#시스템-아키텍처)
- [DB/ERD](#dberd)
- [리포지토리 구조](#리포지토리-구조)
- [기술 스택](#기술-스택)
- [주요 도메인](#주요-도메인)
- [사전 요구사항](#사전-요구사항)
- [실행 방법](#실행-방법)
- [테스트](#테스트)
- [서비스 접속 및 API 문서](#서비스-접속-및-api-문서)
- [환경 변수](#환경-변수)

---

## 프로젝트 소개

- IgLoo는 중고거래 모델에 경매 낙찰 시스템을 결합한 서비스

- 기본 거래 흐름 :
  - `경매 등록 -> 입찰 -> 낙찰/유찰 처리 -> 주문/배송 -> 정산`

- 기존 중고거래의 **기다리는 판매**를 줄이고, 짧은 시간 안에 거래 성사를 목표로 함

---

## 핵심 차별화 포인트

| 비교 항목 | IgLoo (로컬 기반 실시간 경매)                         | 일반 장기 경매 서비스 |
| --------- | ----------------------------------------------------- | --------------------- |
| 경매 기간 | 초단기 운영 (1시간 / 3시간 / 6시간 / 12시간 / 24시간) | 수일 단위 경매 중심   |
| 거래 방식 | 직거래 + 택배 하이브리드                              | 택배 중심             |
| 편의사항  | 경매 게시글 AI 보조 등록                              | 직접 등록             |
| 안전 장치 | 결제/충전/지갑 기반 자금 보호 흐름                    | 플랫폼 정책 중심      |

---

## 핵심 기능

- 초단기 경매 운영 및 실시간 입찰 상태 동기화 (STOMP/WebSocket)
- 경매 마감 임박 처리, 마감/결과 전환 스케줄링
- 낙찰 후 주문 생성 및 거래 방식 선택(직거래/택배)
- 결제 승인, 충전금 관리, 환불/취소 처리
- 지갑/락 잔액 기반 입찰 자금 보호 및 정산 연계
- 사용자 신고/차단 및 운영자 처리 플로우
- AI 서비스 연동(이미지 분석, 카테고리 분류, 설명 생성)

---

## 시스템 아키텍처

![System Architecture](https://github.com/user-attachments/assets/39bf4fa3-0e1d-4dc4-9250-7d54d3f5bf74)

- 사용자는 Vercel에 배포된 React SPA로 접속, API 요청은 Nginx Reverse Proxy를 통해 Spring Boot 서버로 라우팅됨.
- 실시간 경매 흐름은 STOMP/SockJS 기반 WSS 채널로 처리, 입찰 상태와 이벤트가 즉시 동기화됨.
- 백엔드는 핵심 거래 데이터를 AWS RDS(MySQL)에 저장, Redis/Redisson으로 캐시와 분산 락을 관리함.
- 이미지 업로드/조회는 S3 Object Storage 사용, AI 분석 요청은 FastAPI AI Service를 거쳐 OpenAI API로 전달됨.
- 배포는 GitHub Actions가 Docker 이미지를 빌드해 Docker Hub에 반영, AWS 환경에 백엔드/AI 서비스로 배포됨.

---

## DB/ERD

![ERD](https://github.com/user-attachments/assets/55b004c2-b4e1-4cbc-a816-6640b867c674)

- DB 명세: [Notion DB 명세서](https://www.notion.so/DB-2ef4a9f8e43980c8a656fb2eb2f82daa)

---

## 리포지토리 구조

```text
IgLoo/
├── backend/                 # Spring Boot API 서버
├── ai-service/              # FastAPI 기반 AI 보조 서비스
├── nginx/                   # Nginx 설정
├── redis/                   # Redis 설정
├── docs/                    # 프로젝트 문서
├── prometheus.yml           # 모니터링 수집 설정
├── docker-compose.yml       # 서버/배포용 compose (이미지 기반)
├── local-docker-compose.yml # 로컬 인프라용 compose (redis/mysql/nginx)
└── README.md
```

프론트엔드는 별도 저장소로 운영

- 저장소: [IgLooFE](https://github.com/team-noonchissaum/IgLooFE)
- 배포: Vercel 파이프라인 기반 별도 관리

---

## 기술 스택

- Backend
  - Java 21
  - Spring Boot 3.4.1
  - Spring Data JPA
  - Spring Security, OAuth2 Client, JWT
  - Spring WebSocket (STOMP)
  - Spring Batch
  - OpenFeign
  - Redis, Redisson
  - MySQL 8.0 (AWS RDS)
- AI Service
  - FastAPI
  - Pydantic
  - Uvicorn
  - OpenAI API 연동
- Infra / Cloud
  - Docker / Docker Compose
  - Nginx
  - AWS S3

---

## 주요 도메인

- `auction`: 경매 등록/조회, 입찰, 실시간 상태 동기화, 스케줄링
- `order`: 낙찰 이후 주문 생성, 거래 방식(직거래/택배), 배송 처리
- `wallet`: 잔액/락 잔액, 거래별 지갑 기록, 출금 처리
- `payment`: 결제 승인/검증, 취소/환불, 거래 금액 정합성 처리
- `auth`: JWT 기반 인증/인가, OAuth2 로그인 연동
- `notification`: 경매 상태/입찰/주문 이벤트 알림 전송
- `task`: 비동기 작업 상태 저장/복구, 대기 작업 검증
- `report`: 신고 접수/처리 및 운영자 처리 흐름
- `ai`: 이미지 분석, 카테고리 분류, 상품 설명 생성, 백엔드 연동 API 제공

---

## 사전 요구사항

- Docker / Docker Compose
- Java 21 (백엔드 로컬 실행 시)
- Python 3.10+ (AI 서비스 로컬 실행 시)
- 사용 포트
  - `3000` (Frontend)
  - `8080` (Backend)
  - `8001` (AI Service)
  - `3306` (MySQL)
  - `6379` (Redis)

포트 충돌 시 실행 중인 프로세스 종료 또는 포트 변경 후 실행

---

## 실행 방법

### 1) 서버/배포용 Compose 실행

루트 `.env` 파일 기준 실행

```bash
docker compose up -d
```

### 2) 로컬 개발 실행

1. 로컬 인프라 실행

```bash
docker compose -f local-docker-compose.yml up -d
```

2. AI 서비스 실행

```bash
cd ai-service
uvicorn app.main:app --reload --host 0.0.0.0 --port 8001
```

3. 백엔드 실행

Mac/Linux:

```bash
cd backend
./gradlew bootRun
```

Windows:

```powershell
cd backend
.\gradlew.bat bootRun
```

---

## 테스트

`test`는 전체 테스트, `unitTest`와 `integrationTest`는 유형별 분리 실행용

Mac/Linux:

```bash
cd backend
./gradlew test
./gradlew unitTest
./gradlew integrationTest
```

Windows:

```powershell
cd backend
.\gradlew.bat test
.\gradlew.bat unitTest
.\gradlew.bat integrationTest
```

---

## 서비스 접속 및 API 문서

- 프론트엔드 (로컬): `http://localhost:3000`
- 프론트엔드 (배포): `https://ig-loo-fe-89f2.vercel.app/`
- API 서버 (로컬): `http://localhost:8080`
- 인증: JWT 기반 인증/인가 (Spring Security)
- 실시간 통신: STOMP(WebSocket) 기반 경매/알림 메시징
- API 명세: [Notion API 명세서](https://www.notion.so/API-2f04a9f8e4398062bd14c52c4864f5ea)
- 보조 문서: [거래 흐름](docs/used-market-flow.md), [구현 로그](docs/used-market-implementation-log.md)

---

## 환경 변수

기준 파일:

- `backend/src/main/resources/application.yml`
- `backend/src/test/resources/application-test.yml`

로컬 개발에서는 `.env.dev`(IntelliJ 실행용) 또는 `.env` 사용

아래 키를 환경별로 설정

```env
# DB
DB_URL=
DB_USERNAME=
DB_PASSWORD=
MYSQL_ROOT_PASSWORD=

# Redis
REDIS_HOST=
REDIS_PORT=
REDIS_PASSWORD=

# Auth / OAuth
JWT_SECRET=
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
NAVER_CLIENT_ID=
NAVER_CLIENT_SECRET=

# Payment / External
TOSS_CLIENT_KEY=
TOSS_SECRET_KEY=
SWEETTRACKER_API_KEY=

# AWS
AWS_ACCESS_KEY=
AWS_SECRET_KEY=
AWS_S3_BUCKET=

# Mail
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_HOST=
MAIL_PORT=

# App
FRONT_BASE_URL=
KAKAO_MAP_REST_API_KEY=
AI_SERVICE_BASE_URL=

# AI Service
OPENAI_API_KEY=
OPENAI_MODEL=
```
