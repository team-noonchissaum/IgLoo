from __future__ import annotations

from datetime import datetime
from typing import List, Optional, Dict, Any

from pydantic import BaseModel, Field


class ValueConfidence(BaseModel):
    value: str
    confidence: float = Field(ge=0.0, le=1.0)


class ImageQuality(BaseModel):
    is_blurry: bool
    is_overexposed: bool
    resolution_ok: bool


class Defect(BaseModel):
    type: str
    location: Optional[str] = None
    severity: Optional[str] = None
    confidence: Optional[float] = Field(default=None, ge=0.0, le=1.0)


class AnalyzeImageRequest(BaseModel):
    imageUrls: List[str]
    metadata: Optional[Dict[str, Any]] = None


class AnalyzeImageResponse(BaseModel):
    brand: ValueConfidence
    model: ValueConfidence
    condition: ValueConfidence
    defects: List[Defect]
    accessories: List[str]
    text_ocr: List[str]
    image_quality: ImageQuality


class ClassifyCategoryRequest(BaseModel):
    brand: Optional[ValueConfidence] = None
    model: Optional[ValueConfidence] = None
    text_ocr: Optional[List[str]] = None
    categories: Optional[List["CategoryItem"]] = None
    keywords: Optional[List[str]] = None


class CategoryCandidate(BaseModel):
    category_id: int
    category_path: str
    confidence: float = Field(ge=0.0, le=1.0)


class CategoryItem(BaseModel):
    id: int
    name: str
    parentId: Optional[int] = None
    path: Optional[str] = None
    aliases: Optional[List[str]] = None
    isLeaf: Optional[bool] = None


class ClassifyCategoryResponse(BaseModel):
    category_candidates: List[CategoryCandidate]
    selected_category_id: Optional[int] = None
    selection_reason: str
    needs_user_confirmation: bool


class GenerateDescriptionRequest(BaseModel):
    category_id: Optional[int] = None
    brand: Optional[str] = None
    model: Optional[str] = None
    condition: Optional[str] = None
    defects: List[Defect] = Field(default_factory=list)
    accessories: List[str] = Field(default_factory=list)
    image_urls: List[str] = Field(default_factory=list)
    item_type: Optional[str] = None
    start_price: Optional[int] = None
    auction_duration: Optional[int] = None
    start_at: Optional[datetime] = None
    end_at: Optional[datetime] = None


class AuctionRegisterReq(BaseModel):
    title: str
    description: str
    startPrice: Optional[int] = None
    categoryId: int
    auctionDuration: Optional[int] = None
    startAt: Optional[datetime] = None
    endAt: Optional[datetime] = None
    imageUrls: List[str]


class GenerateDescriptionResponse(BaseModel):
    title: str
    summary: str
    body: str
    hashtags: List[str]
    auction_register_req: AuctionRegisterReq
