from fastapi import APIRouter

from app.schemas import (
    AnalyzeImageRequest,
    AnalyzeImageResponse,
    ClassifyCategoryRequest,
    ClassifyCategoryResponse,
    GenerateDescriptionRequest,
    GenerateDescriptionResponse,
)
from app.services.pipeline import analyze_image, classify_category, generate_description


router = APIRouter()


@router.post("/analyze-image", response_model=AnalyzeImageResponse)
def analyze_image_route(request: AnalyzeImageRequest) -> AnalyzeImageResponse:
    return analyze_image(request)


@router.post("/classify-category", response_model=ClassifyCategoryResponse)
def classify_category_route(request: ClassifyCategoryRequest) -> ClassifyCategoryResponse:
    return classify_category(request)


@router.post("/generate-description", response_model=GenerateDescriptionResponse)
def generate_description_route(request: GenerateDescriptionRequest) -> GenerateDescriptionResponse:
    return generate_description(request)
