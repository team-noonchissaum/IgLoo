import json
import os
from typing import Any

from openai import OpenAI


class OpenAIClient:
    def __init__(self) -> None:
        self.api_key = os.getenv("OPENAI_API_KEY")
        self.model = os.getenv("OPENAI_MODEL", "gpt-4.1-mini")
        self.client = OpenAI(api_key=self.api_key) if self.api_key else None

    def is_configured(self) -> bool:
        return bool(self.api_key)

    def analyze_image(self, image_urls: list[str], metadata: dict | None = None) -> dict:
        prompt = (
            "You analyze used item photos and extract facts. "
            "Return JSON following the schema. "
            "Be conservative and use UNKNOWN when unsure."
        )
        schema = {
            "type": "object",
            "properties": {
                "brand": _value_confidence_schema(),
                "model": _value_confidence_schema(),
                "condition": _value_confidence_schema(),
                "defects": {
                    "type": "array",
                    "items": _defect_schema(),
                },
                "accessories": {"type": "array", "items": {"type": "string"}},
                "text_ocr": {"type": "array", "items": {"type": "string"}},
                "image_quality": _image_quality_schema(),
            },
            "required": [
                "brand",
                "model",
                "condition",
                "defects",
                "accessories",
                "text_ocr",
                "image_quality",
            ],
            "additionalProperties": False,
        }
        content = [
            {"type": "input_text", "text": prompt},
        ]
        if metadata:
            content.append(
                {
                    "type": "input_text",
                    "text": f"metadata: {json.dumps(metadata, ensure_ascii=False)}",
                }
            )
        for url in image_urls:
            content.append({"type": "input_image", "image_url": url})
        return self._call_json_schema(content, schema)

    def classify_category(
        self,
        brand: str | None,
        model: str | None,
        text_ocr: list[str] | None,
        categories: list[dict] | None,
        keywords: list[str] | None,
    ) -> dict:
        prompt = (
            "Classify category candidates for a used item. "
            "Use only the provided category list and match by semantic similarity. "
            "Use category name, path, aliases, and keywords for matching. "
            "Select leaf categories only (isLeaf=true). "
            "If no reasonable match, select the category named '기타'. "
            "Return JSON with candidates and a selected_category_id."
        )
        schema = {
            "type": "object",
            "properties": {
                "category_candidates": {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "properties": {
                            "category_id": {"type": "integer"},
                            "category_path": {"type": "string"},
                            "confidence": {"type": "number"},
                        },
                        "required": ["category_id", "category_path", "confidence"],
                        "additionalProperties": False,
                    },
                },
                "selected_category_id": {"type": ["integer", "null"]},
                "selection_reason": {"type": "string"},
                "needs_user_confirmation": {"type": "boolean"},
            },
            "required": [
                "category_candidates",
                "selected_category_id",
                "selection_reason",
                "needs_user_confirmation",
            ],
            "additionalProperties": False,
        }
        content = [
            {"type": "input_text", "text": prompt},
            {
                "type": "input_text",
                "text": json.dumps(
                    {
                        "brand": brand,
                        "model": model,
                        "text_ocr": text_ocr or [],
                        "categories": categories or [],
                        "keywords": keywords or [],
                    },
                    ensure_ascii=False,
                ),
            },
        ]
        return self._call_json_schema(content, schema)

    def generate_description(
        self,
        category_id: int | None,
        brand: str | None,
        model: str | None,
        condition: str | None,
        defects: list[dict],
        accessories: list[str],
        image_urls: list[str],
        item_type: str | None,
        start_price: int | None,
        auction_duration: int | None,
        start_at: str | None,
        end_at: str | None,
    ) -> dict:
        prompt = (
            "Write a concise, factual listing in Korean (2-3 sentences, <= 320 chars). "
            "No exaggeration. Use given facts only. "
            "If brand/model is unknown, use item_type (e.g., '카메라', '신발') in title/body. "
            "Return JSON with title, summary, body, hashtags, and auction_register_req."
        )
        schema = {
            "type": "object",
            "properties": {
                "title": {"type": "string"},
                "summary": {"type": "string"},
                "body": {"type": "string"},
                "hashtags": {"type": "array", "items": {"type": "string"}},
                "auction_register_req": {
                    "type": "object",
                    "properties": {
                        "title": {"type": "string"},
                        "description": {"type": "string"},
                        "startPrice": {"type": ["integer", "null"]},
                        "categoryId": {"type": ["integer", "null"]},
                        "auctionDuration": {"type": ["integer", "null"]},
                        "startAt": {"type": ["string", "null"]},
                        "endAt": {"type": ["string", "null"]},
                        "imageUrls": {"type": "array", "items": {"type": "string"}},
                    },
                    "required": [
                        "title",
                        "description",
                        "startPrice",
                        "categoryId",
                        "auctionDuration",
                        "startAt",
                        "endAt",
                        "imageUrls",
                    ],
                    "additionalProperties": False,
                },
            },
            "required": ["title", "summary", "body", "hashtags", "auction_register_req"],
            "additionalProperties": False,
        }
        content = [
            {"type": "input_text", "text": prompt},
            {
                "type": "input_text",
                "text": json.dumps(
                    {
                        "category_id": category_id,
                        "brand": brand,
                        "model": model,
                        "condition": condition,
                        "defects": defects or [],
                        "accessories": accessories or [],
                        "image_urls": image_urls or [],
                        "item_type": item_type,
                        "start_price": start_price,
                        "auction_duration": auction_duration,
                        "start_at": start_at,
                        "end_at": end_at,
                    },
                    ensure_ascii=False,
                ),
            },
        ]
        return self._call_json_schema(content, schema)

    def _call_json_schema(self, content: list[dict[str, Any]], schema: dict[str, Any]) -> dict:
        if not self.client:
            raise RuntimeError("OpenAI client is not configured.")
        try:
            response = self.client.responses.create(
                model=self.model,
                input=[
                    {
                        "role": "user",
                        "content": content,
                    }
                ],
                response_format={
                    "type": "json_schema",
                    "json_schema": {
                        "name": "response",
                        "schema": schema,
                        "strict": True,
                    },
                },
            )
            return json.loads(response.output_text)
        except TypeError:
            # Older OpenAI SDKs don't support response_format for Responses API.
            response = self.client.responses.create(
                model=self.model,
                input=[
                    {
                        "role": "user",
                        "content": content,
                    }
                ],
            )
            return _safe_json_loads(response.output_text)


def _value_confidence_schema() -> dict:
    return {
        "type": "object",
        "properties": {
            "value": {"type": "string"},
            "confidence": {"type": "number"},
        },
        "required": ["value", "confidence"],
        "additionalProperties": False,
    }


def _defect_schema() -> dict:
    return {
        "type": "object",
        "properties": {
            "type": {"type": "string"},
            "location": {"type": ["string", "null"]},
            "severity": {"type": ["string", "null"]},
            "confidence": {"type": ["number", "null"]},
        },
        "required": ["type", "location", "severity", "confidence"],
        "additionalProperties": False,
    }


def _image_quality_schema() -> dict:
    return {
        "type": "object",
        "properties": {
            "is_blurry": {"type": "boolean"},
            "is_overexposed": {"type": "boolean"},
            "resolution_ok": {"type": "boolean"},
        },
        "required": ["is_blurry", "is_overexposed", "resolution_ok"],
        "additionalProperties": False,
    }


def _safe_json_loads(text: str) -> dict:
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        if not text:
            raise
        start = text.find("{")
        end = text.rfind("}")
        if start == -1 or end == -1 or end <= start:
            raise
        return json.loads(text[start:end + 1])
