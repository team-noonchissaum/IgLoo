from dotenv import load_dotenv
from fastapi import FastAPI

load_dotenv()

from app.routes.ai import router as ai_router


app = FastAPI(title="Used Market AI Service", version="0.1.0")

app.include_router(ai_router, prefix="/ai", tags=["ai"])


@app.get("/health")
def health_check():
    return {"status": "ok"}
