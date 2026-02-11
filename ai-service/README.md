# Python AI Service (FastAPI)

## 실행
```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8001
```

## 환경 변수
- `OPENAI_API_KEY`: 실제 모델 연동 시 사용
- `OPENAI_MODEL`: 기본 `gpt-4.1-mini`

## 엔드포인트
- `POST /ai/analyze-image`
- `POST /ai/classify-category`
- `POST /ai/generate-description`
- `GET /health`
