from __future__ import annotations

import json
from datetime import datetime, timedelta
from typing import List

from app.schemas import (
    AnalyzeImageRequest,
    AnalyzeImageResponse,
    ClassifyCategoryRequest,
    ClassifyCategoryResponse,
    CategoryCandidate,
    Defect,
    GenerateDescriptionRequest,
    GenerateDescriptionResponse,
    ImageQuality,
    ValueConfidence,
    AuctionRegisterReq,
)
from app.services.openai_client import OpenAIClient


_client = OpenAIClient()


def analyze_image(request: AnalyzeImageRequest) -> AnalyzeImageResponse:
    if _client.is_configured():
        try:
            payload = _client.analyze_image(request.imageUrls, request.metadata)
            payload = _normalize_analyze_payload(payload)
            _print_analyze_summary(payload)
            return AnalyzeImageResponse(**payload)
        except Exception:
            pass
    response = _fallback_analyze_image(request)
    _print_analyze_summary(response.model_dump())
    return response


def classify_category(request: ClassifyCategoryRequest) -> ClassifyCategoryResponse:
    if _client.is_configured():
        try:
            payload = _client.classify_category(
                request.brand.value if request.brand else None,
                request.model.value if request.model else None,
                request.text_ocr or [],
                [c.model_dump() for c in (request.categories or [])],
                request.keywords or [],
            )
            payload = _normalize_classify_payload(payload)
            _print_classify_summary(payload)
            return ClassifyCategoryResponse(**payload)
        except Exception:
            pass
    response = _fallback_classify_category(request)
    _print_classify_summary(response.model_dump())
    return response


def generate_description(request: GenerateDescriptionRequest) -> GenerateDescriptionResponse:
    if _client.is_configured():
        try:
            payload = _generate_description_with_openai(request)
            print(f"description_generated model=openai title={payload.get('title')}")
            return GenerateDescriptionResponse(**payload)
        except Exception as exc:
            print(f"description_generated model=openai_error error={exc}")
    response = _fallback_generate_description(request)
    print(f"description_generated model=fallback title={response.title}")
    return response


def _fallback_analyze_image(request: AnalyzeImageRequest) -> AnalyzeImageResponse:
    brand = ValueConfidence(value="UNKNOWN", confidence=0.1)
    model = ValueConfidence(value="UNKNOWN", confidence=0.1)
    condition = ValueConfidence(value="USED", confidence=0.4)

    metadata = request.metadata or {}
    note = str(metadata.get("seller_note", "")).strip()
    if note:
        if "미개봉" in note or "새상품" in note:
            condition = ValueConfidence(value="NEW", confidence=0.7)
        if "사용감" in note or "스크래치" in note:
            condition = ValueConfidence(value="USED", confidence=0.6)

    image_quality = ImageQuality(is_blurry=False, is_overexposed=False, resolution_ok=True)
    return AnalyzeImageResponse(
        brand=brand,
        model=model,
        condition=condition,
        defects=[],
        accessories=[],
        text_ocr=[],
        image_quality=image_quality,
    )


def _fallback_classify_category(request: ClassifyCategoryRequest) -> ClassifyCategoryResponse:
    candidates: List[CategoryCandidate] = []
    best = _select_category_by_keywords(request)
    if best:
        candidates = [best]
    elif request.categories:
        misc = _find_misc_leaf(request.categories)
        if misc:
            candidates = [misc]
        else:
            primary = request.categories[0]
            candidates = [
                CategoryCandidate(
                    category_id=primary.id,
                    category_path=primary.name,
                    confidence=0.1,
                )
            ]
    else:
        candidates = [
            CategoryCandidate(category_id=80, category_path="기타", confidence=0.1),
        ]
    selected = candidates[0].category_id
    needs_confirmation = True
    return ClassifyCategoryResponse(
        category_candidates=candidates,
        selected_category_id=selected,
        selection_reason="keyword fallback (model not connected)",
        needs_user_confirmation=needs_confirmation,
    )


def _generate_description_with_openai(request: GenerateDescriptionRequest) -> dict:
    payload = _client.generate_description(
        request.category_id,
        request.brand,
        request.model,
        request.condition,
        [d.model_dump() for d in request.defects],
        request.accessories,
        request.image_urls,
        request.item_type,
        request.start_price,
        request.auction_duration,
        request.start_at.isoformat() if request.start_at else None,
        request.end_at.isoformat() if request.end_at else None,
    )
    return _ensure_auction_payload(payload, request)
def _fallback_generate_description(request: GenerateDescriptionRequest) -> GenerateDescriptionResponse:
    defects_text = _format_defects(request.defects)
    accessories_text = _format_accessories(request.accessories)
    condition_text = _condition_korean(request.condition or "USED")

    brand = request.brand or ""
    model = request.model or ""
    name_part = " ".join([part for part in [brand, model] if part and part.upper() != "UNKNOWN"]).strip()
    item_type = (request.item_type or "").strip()

    if name_part:
        title = f"{name_part} 판매합니다".strip()
    elif item_type:
        title = f"{item_type} 팝니다"
    else:
        title = "상품 판매합니다"
    summary = f"{condition_text}, 구성품 {accessories_text}"
    body_parts = []
    if name_part:
        body_parts.append(f"{name_part} {condition_text}입니다.")
    elif item_type:
        body_parts.append(f"{item_type} 판매합니다. 상태는 {condition_text}입니다.")
    else:
        body_parts.append(f"상품 상태는 {condition_text}입니다.")
    if defects_text:
        body_parts.append(f"하자: {defects_text}.")
    if accessories_text:
        body_parts.append(f"구성품: {accessories_text}.")
    body = " ".join(body_parts)

    start_at = request.start_at
    end_at = request.end_at
    duration_minutes = request.auction_duration

    if start_at or end_at or duration_minutes:
        if not start_at:
            start_at = datetime.utcnow()
        if end_at:
            duration_minutes = int((end_at - start_at).total_seconds() / 60)
        else:
            duration_minutes = duration_minutes or 1440
            end_at = start_at + timedelta(minutes=duration_minutes)

    auction_register_req = _build_auction_register_req(
        title=title,
        body=body,
        request=request,
        auction_duration=duration_minutes,
        start_at=start_at,
        end_at=end_at,
    )

    hashtags = _build_hashtags(request.brand, request.model)
    return GenerateDescriptionResponse(
        title=title,
        summary=summary,
        body=body,
        hashtags=hashtags,
        auction_register_req=auction_register_req,
    )


def _ensure_auction_payload(payload: dict, request: GenerateDescriptionRequest) -> dict:
    if not isinstance(payload, dict):
        return payload
    auction = payload.get("auction_register_req")
    if isinstance(auction, dict):
        normalized = _normalize_auction_dict(auction, payload, request)
        payload["auction_register_req"] = AuctionRegisterReq(**normalized)
        return payload
    payload["auction_register_req"] = _build_auction_register_req(
        title=payload.get("title") or "상품 판매합니다",
        body=payload.get("body") or "상품 설명이 없습니다.",
        request=request,
        auction_duration=request.auction_duration,
        start_at=request.start_at,
        end_at=request.end_at,
    )
    return payload


def _normalize_auction_dict(
    auction: dict,
    payload: dict,
    request: GenerateDescriptionRequest
) -> dict:
    # 모델이 snake_case로 반환하는 경우를 보정
    return {
        "title": auction.get("title") or payload.get("title") or "상품 판매합니다",
        "description": auction.get("description") or payload.get("body") or "상품 설명이 없습니다.",
        "startPrice": auction.get("startPrice") or auction.get("start_price") or request.start_price,
        "categoryId": auction.get("categoryId") or auction.get("category_id") or request.category_id or 0,
        "auctionDuration": auction.get("auctionDuration") or auction.get("auction_duration") or request.auction_duration,
        "startAt": auction.get("startAt") or auction.get("start_at") or request.start_at,
        "endAt": auction.get("endAt") or auction.get("end_at") or request.end_at,
        "imageUrls": auction.get("imageUrls") or auction.get("image_urls") or request.image_urls,
    }


def _build_auction_register_req(
    title: str,
    body: str,
    request: GenerateDescriptionRequest,
    auction_duration: int | None,
    start_at: datetime | None,
    end_at: datetime | None,
) -> AuctionRegisterReq:
    return AuctionRegisterReq(
        title=title,
        description=body,
        startPrice=request.start_price,
        categoryId=request.category_id,
        auctionDuration=auction_duration,
        startAt=start_at,
        endAt=end_at,
        imageUrls=request.image_urls,
    )


def _format_defects(defects: List[Defect]) -> str:
    if not defects:
        return ""
    items = []
    for defect in defects:
        parts = [defect.type]
        if defect.location:
            parts.append(defect.location)
        if defect.severity:
            parts.append(defect.severity)
        items.append("/".join(parts))
    return ", ".join(items)


def _format_accessories(accessories: List[str]) -> str:
    if not accessories:
        return "없음"
    return ", ".join(accessories)


def _condition_korean(condition: str) -> str:
    mapping = {
        "NEW": "새상품 수준",
        "USED_GOOD": "상태 양호",
        "USED": "사용감 있음",
    }
    return mapping.get(condition, "사용감 있음")


def _build_hashtags(brand: str, model: str) -> List[str]:
    tags = []
    if brand:
        tags.append(f"#{brand}")
    if model:
        tags.append(f"#{model}".replace(" ", ""))
    tags.append("#중고거래")
    return tags


def _normalize_analyze_payload(payload: dict) -> dict:
    if not isinstance(payload, dict):
        return payload

    def normalize_value_conf(value) -> dict:
        if isinstance(value, dict) and "value" in value and "confidence" in value:
            return value
        if isinstance(value, str):
            return {"value": value, "confidence": 0.3}
        return {"value": "UNKNOWN", "confidence": 0.1}

    return {
        "brand": normalize_value_conf(payload.get("brand")),
        "model": normalize_value_conf(payload.get("model")),
        "condition": normalize_value_conf(payload.get("condition")),
        "defects": payload.get("defects") or [],
        "accessories": payload.get("accessories") or [],
        "text_ocr": payload.get("text_ocr") or [],
        "image_quality": payload.get("image_quality") or {
            "is_blurry": False,
            "is_overexposed": False,
            "resolution_ok": True,
        },
    }


def _normalize_classify_payload(payload: dict) -> dict:
    if not isinstance(payload, dict):
        return payload
    candidates = payload.get("category_candidates") or []
    if not isinstance(candidates, list):
        candidates = []
    normalized_candidates = []
    for candidate in candidates:
        if not isinstance(candidate, dict):
            continue
        if "category_id" not in candidate or "category_path" not in candidate:
            continue
        normalized_candidates.append(
            {
                "category_id": candidate.get("category_id"),
                "category_path": candidate.get("category_path"),
                "confidence": candidate.get("confidence", 0.0),
            }
        )

    selected_id = payload.get("selected_category_id")
    if selected_id is None and normalized_candidates:
        selected_id = normalized_candidates[0]["category_id"]

    return {
        "category_candidates": normalized_candidates,
        "selected_category_id": selected_id,
        "selection_reason": payload.get("selection_reason") or "normalized",
        "needs_user_confirmation": bool(payload.get("needs_user_confirmation", True)),
    }


def _select_category_by_keywords(request: ClassifyCategoryRequest) -> CategoryCandidate | None:
    if not request.categories or not request.keywords:
        return None
    keywords = _normalize_tokens(request.keywords)
    if not keywords:
        return None
    best = None
    best_score = 0
    for category in request.categories:
        if category.isLeaf is False:
            continue
        score = _score_category(category, keywords)
        if score > best_score:
            best_score = score
            best = category
    if not best:
        return None
    return CategoryCandidate(
        category_id=best.id,
        category_path=best.path or best.name,
        confidence=min(0.8, 0.2 + 0.1 * best_score),
    )


def _find_misc_leaf(categories: List["CategoryItem"]) -> CategoryCandidate | None:
    for category in categories:
        if category.isLeaf and category.name in ("기타물품", "기타"):
            return CategoryCandidate(
                category_id=category.id,
                category_path=category.path or category.name,
                confidence=0.1,
            )
    return None


def _score_category(category: "CategoryItem", keywords: set[str]) -> int:
    score = 0
    targets = []
    if category.name:
        targets.append(category.name)
    if category.path:
        targets.append(category.path)
    if category.aliases:
        targets.extend(category.aliases)
    for target in targets:
        for token in _split_tokens(target):
            if token in keywords:
                score += 1
    return score


def _normalize_tokens(tokens: List[str]) -> set[str]:
    normalized = set()
    for token in tokens:
        for part in _split_tokens(token):
            if part:
                normalized.add(part)
    return normalized


def _split_tokens(text: str) -> List[str]:
    if not text:
        return []
    cleaned = text.replace(">", " ")
    parts = []
    for raw in cleaned.split():
        token = raw.strip().lower()
        if token:
            parts.append(token)
    return parts


def _print_analyze_summary(payload: dict) -> None:
    summary = {
        "brand": payload.get("brand"),
        "model": payload.get("model"),
        "condition": payload.get("condition"),
        "text_ocr": payload.get("text_ocr"),
    }
    print(f"analyze_result {json.dumps(summary, ensure_ascii=False)}")


def _print_classify_summary(payload: dict) -> None:
    summary = {
        "selected_category_id": payload.get("selected_category_id"),
        "needs_user_confirmation": payload.get("needs_user_confirmation"),
        "category_candidates": payload.get("category_candidates"),
    }
    print(f"classify_result {json.dumps(summary, ensure_ascii=False)}")
