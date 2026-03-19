#!/usr/bin/env python3
"""Generate K-ToME image assets with Gemini only.

This script intentionally fails fast when no Gemini key is provided.
It is designed for the Phase 2 image pipeline described in docs/phase2
and consumes a YAML asset plan under assets-src/image/specs/.
"""

from __future__ import annotations

import argparse
import base64
import concurrent.futures
import hashlib
import json
import os
import pathlib
import sys
import time
import threading
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any

try:
    import yaml  # type: ignore
except Exception as exc:  # pragma: no cover
    raise RuntimeError("PyYAML is required to read the asset plan YAML") from exc


DEFAULT_MODEL = "gemini-3.1-flash-image-preview"
DEFAULT_ASPECT_RATIO = "1:1"
DEFAULT_IMAGE_SIZE = "1K"
REPORT_LOCK = threading.Lock()
RETRYABLE_HTTP_STATUS = {429, 503, 504}

ALLOWED_CATEGORIES = {
    "tile_ground",
    "tile_wall",
    "tile_decal",
    "prop_interactable",
    "prop_environment",
    "actor_sprite",
    "portrait",
    "icon_skill",
    "icon_status",
    "icon_item",
    "icon_quest",
    "ui_frame",
    "vfx_plate",
}


@dataclass(frozen=True)
class AssetJob:
    gate_id: str
    gate_description: str
    asset_id: str
    category: str
    output_name: str
    visual_key: str
    subject: str
    biome: str | None
    profession: str | None
    faction: str | None
    material_tags: list[str]
    mood_tags: list[str]
    constraints: list[str]
    negative_constraints: list[str]
    style_tag: str
    style_prompt: str
    art_style_bible: str
    category_template: str
    biome_atmosphere: str
    faction_style: str
    gemini_key_required: bool


def fail(message: str) -> int:
    print(message, file=sys.stderr)
    return 1


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate K-ToME image assets with Gemini")
    parser.add_argument(
        "--plan",
        default="assets-src/image/specs/phase2-asset-plan.yaml",
        help="Path to the YAML asset plan",
    )
    parser.add_argument(
        "--out-dir",
        default="assets-src/image/raw/generated",
        help="Directory for generated raw images",
    )
    parser.add_argument(
        "--report",
        default="assets-src/image/manifests/phase2-generation-report.jsonl",
        help="JSONL report path for generated assets",
    )
    parser.add_argument(
        "--gemini-api-key",
        default="",
        help="Gemini API key. If omitted, GEMINI_API_KEY environment variable is used.",
    )
    parser.add_argument("--model", default=os.getenv("IMAGE_MODEL", DEFAULT_MODEL))
    parser.add_argument(
        "--image-aspect-ratio",
        default=os.getenv("GEMINI_IMAGE_ASPECT_RATIO", DEFAULT_ASPECT_RATIO),
    )
    parser.add_argument(
        "--image-size",
        default=os.getenv("GEMINI_IMAGE_SIZE", DEFAULT_IMAGE_SIZE),
    )
    parser.add_argument(
        "--timeout-s",
        type=int,
        default=int(os.getenv("GEMINI_REQUEST_TIMEOUT_S", "45")),
    )
    parser.add_argument(
        "--request-retries",
        type=int,
        default=int(os.getenv("GEMINI_REQUEST_RETRIES", "1")),
    )
    parser.add_argument(
        "--delay-ms",
        type=int,
        default=int(os.getenv("GEMINI_REQUEST_DELAY_MS", "1500")),
    )
    parser.add_argument(
        "--skip-existing",
        action="store_true",
        help="Skip assets whose output file already exists.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print compiled prompts without calling the Gemini API.",
    )
    parser.add_argument(
        "--concurrency",
        type=int,
        default=int(os.getenv("GEMINI_CONCURRENCY", "1")),
        help="Maximum number of concurrent Gemini generation requests.",
    )
    return parser.parse_args()


def require_api_key(arg_value: str) -> str:
    api_key = (arg_value or os.getenv("GEMINI_API_KEY", "")).strip()
    if not api_key:
        raise RuntimeError(
            "Gemini API key is required. Provide --gemini-api-key or set GEMINI_API_KEY before generation."
        )
    return api_key


def load_plan(plan_path: pathlib.Path) -> dict[str, Any]:
    if not plan_path.is_file():
        raise FileNotFoundError(f"Asset plan not found: {plan_path}")
    raw = yaml.safe_load(plan_path.read_text(encoding="utf-8"))
    if not isinstance(raw, dict):
        raise ValueError("Asset plan root must be a mapping")
    return raw


def normalize_list(raw: Any) -> list[str]:
    if raw is None:
        return []
    if isinstance(raw, list):
        return [str(item).strip() for item in raw if str(item).strip()]
    if isinstance(raw, str) and raw.strip():
        return [raw.strip()]
    return []


def parse_jobs(plan: dict[str, Any]) -> list[AssetJob]:
    style_tag = str(plan.get("styleTag", "")).strip()
    style_prompt = str(plan.get("stylePrompt", "")).strip()
    art_style_bible = str(plan.get("artStyleBible", "")).strip()
    defaults = plan.get("defaults", {})
    if not style_tag or not style_prompt or not art_style_bible:
        raise ValueError("Asset plan must define styleTag, stylePrompt and artStyleBible")
    if not isinstance(defaults, dict):
        raise ValueError("defaults must be a mapping")

    gate_map = plan.get("phase2AssetGates")
    if not isinstance(gate_map, dict) or not gate_map:
        raise ValueError("phase2AssetGates must be a non-empty mapping")

    default_negative = normalize_list(defaults.get("negativeConstraints"))
    category_templates: dict[str, str] = {}
    raw_templates = defaults.get("categoryPromptTemplates")
    if isinstance(raw_templates, dict):
        for k, v in raw_templates.items():
            category_templates[str(k).strip()] = str(v).strip()

    biome_atmosphere_map: dict[str, str] = {}
    raw_biomes = defaults.get("biomeAtmosphere")
    if isinstance(raw_biomes, dict):
        for k, v in raw_biomes.items():
            biome_atmosphere_map[str(k).strip()] = str(v).strip()

    faction_style_map: dict[str, str] = {}
    raw_factions = defaults.get("factionStyle")
    if isinstance(raw_factions, dict):
        for k, v in raw_factions.items():
            faction_style_map[str(k).strip()] = str(v).strip()

    jobs: list[AssetJob] = []
    for gate_id, gate_payload in gate_map.items():
        if not isinstance(gate_payload, dict):
            raise ValueError(f"Gate {gate_id} must be a mapping")
        description = str(gate_payload.get("description", "")).strip()
        assets = gate_payload.get("assets")
        if not isinstance(assets, list) or not assets:
            raise ValueError(f"Gate {gate_id} must define a non-empty assets list")

        for asset in assets:
            if not isinstance(asset, dict):
                raise ValueError(f"Gate {gate_id} has a non-mapping asset entry")
            category = str(asset.get("category", "")).strip()
            if category not in ALLOWED_CATEGORIES:
                raise ValueError(f"Unsupported asset category '{category}' in gate {gate_id}")

            asset_id = str(asset.get("id", "")).strip()
            output_name = str(asset.get("outputName", "")).strip()
            visual_key = str(asset.get("visualKey", "")).strip()
            subject = str(asset.get("subject", "")).strip()
            if not asset_id or not output_name or not visual_key or not subject:
                raise ValueError(f"Gate {gate_id} asset is missing id/outputName/visualKey/subject")

            biome = str(asset.get("biome", "")).strip() or None
            faction = str(asset.get("faction", "")).strip() or None

            jobs.append(
                AssetJob(
                    gate_id=str(gate_id),
                    gate_description=description,
                    asset_id=asset_id,
                    category=category,
                    output_name=output_name,
                    visual_key=visual_key,
                    subject=subject,
                    biome=biome,
                    profession=str(asset.get("profession", "")).strip() or None,
                    faction=faction,
                    material_tags=normalize_list(asset.get("materialTags")),
                    mood_tags=normalize_list(asset.get("moodTags")),
                    constraints=normalize_list(asset.get("constraints")),
                    negative_constraints=default_negative + normalize_list(asset.get("negativeConstraints")),
                    style_tag=style_tag,
                    style_prompt=style_prompt,
                    art_style_bible=art_style_bible,
                    category_template=category_templates.get(category, ""),
                    biome_atmosphere=biome_atmosphere_map.get(biome, "") if biome else "",
                    faction_style=faction_style_map.get(faction, "") if faction else "",
                    gemini_key_required=bool(asset.get("geminiKeyRequired", True)),
                )
            )
    return jobs


def compile_prompt(job: AssetJob) -> str:
    sections: list[str] = []

    # 1. World & rendering style directive
    sections.append(f"[Art Direction]\n{job.style_prompt}")

    # 2. Category-specific template
    if job.category_template:
        sections.append(f"[Asset Type Rules]\n{job.category_template}")

    # 3. Biome atmosphere
    if job.biome_atmosphere:
        sections.append(f"[Environment Atmosphere]\n{job.biome_atmosphere}")

    # 4. Faction visual language
    if job.faction_style:
        sections.append(f"[Faction Visual Language]\n{job.faction_style}")

    # 5. Subject — the specific asset description
    sections.append(f"[Subject]\n{job.subject}")

    # 6. Supporting tags
    tag_parts: list[str] = []
    if job.material_tags:
        tag_parts.append(f"Materials: {', '.join(job.material_tags)}")
    if job.mood_tags:
        tag_parts.append(f"Mood: {', '.join(job.mood_tags)}")
    if tag_parts:
        sections.append("[Tags]\n" + "\n".join(tag_parts))

    # 7. Constraints (already translated to visual language in YAML)
    if job.constraints:
        constraint_lines = "\n".join(f"- {c}" for c in job.constraints)
        sections.append(f"[Requirements]\n{constraint_lines}")

    # 8. Negative constraints
    if job.negative_constraints:
        neg_lines = "\n".join(f"- {c}" for c in job.negative_constraints)
        sections.append(f"[Forbidden]\n{neg_lines}")

    return "\n\n".join(sections)


def call_gemini(
    *,
    prompt: str,
    api_key: str,
    model: str,
    timeout_s: int,
    image_aspect_ratio: str,
    image_size: str,
    request_retries: int,
) -> bytes:
    url = (
        "https://generativelanguage.googleapis.com/v1beta/models/"
        f"{urllib.parse.quote(model, safe='')}:generateContent?key={urllib.parse.quote(api_key, safe='')}"
    )
    payload = {
        "contents": [{"role": "user", "parts": [{"text": prompt}]}],
        "generationConfig": {
            "responseModalities": ["IMAGE", "TEXT"],
            "imageConfig": {
                "aspectRatio": image_aspect_ratio,
                "imageSize": image_size,
            },
        },
    }
    encoded = json.dumps(payload).encode("utf-8")
    headers = {
        "Content-Type": "application/json",
    }

    last_error: Exception | None = None
    for attempt in range(request_retries + 1):
        request = urllib.request.Request(url, data=encoded, headers=headers, method="POST")
        try:
            with urllib.request.urlopen(request, timeout=timeout_s) as response:
                body = json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as exc:
            payload_text = exc.read().decode("utf-8", errors="replace")
            if exc.code in RETRYABLE_HTTP_STATUS and attempt < request_retries:
                time.sleep(max(2.0, 2.5 * (attempt + 1)))
                continue
            raise RuntimeError(f"Gemini API HTTP {exc.code}: {payload_text}") from exc
        except Exception as exc:  # pragma: no cover
            last_error = exc
            if attempt >= request_retries:
                break
            time.sleep(max(1.0, 1.5 * (attempt + 1)))
            continue

        candidates = body.get("candidates", [])
        for candidate in candidates:
            content = candidate.get("content", {})
            for part in content.get("parts", []):
                inline = part.get("inlineData") or part.get("inline_data")
                if not inline:
                    continue
                data = inline.get("data")
                if data:
                    return base64.b64decode(data)
        raise RuntimeError("Gemini response did not contain inline image data")

    if last_error is not None:
        raise RuntimeError(f"Gemini request failed: {last_error}") from last_error
    raise RuntimeError("Gemini request failed without a detailed error")


def write_report(report_path: pathlib.Path, record: dict[str, Any]) -> None:
    report_path.parent.mkdir(parents=True, exist_ok=True)
    with REPORT_LOCK:
        with report_path.open("a", encoding="utf-8") as handle:
            handle.write(json.dumps(record, ensure_ascii=False) + "\n")


def generate_job(
    *,
    job: AssetJob,
    out_dir: pathlib.Path,
    report_path: pathlib.Path,
    api_key: str,
    model: str,
    timeout_s: int,
    image_aspect_ratio: str,
    image_size: str,
    request_retries: int,
    delay_ms: int,
    skip_existing: bool,
) -> tuple[str, str]:
    prompt = compile_prompt(job)
    output_path = out_dir / job.output_name
    output_path.parent.mkdir(parents=True, exist_ok=True)

    if skip_existing and output_path.is_file():
        return ("skip", f"[skip] {job.asset_id} -> {output_path}")

    image_bytes = call_gemini(
        prompt=prompt,
        api_key=api_key,
        model=model,
        timeout_s=timeout_s,
        image_aspect_ratio=image_aspect_ratio,
        image_size=image_size,
        request_retries=request_retries,
    )
    output_path.write_bytes(image_bytes)

    report_record = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "gateId": job.gate_id,
        "gateDescription": job.gate_description,
        "assetId": job.asset_id,
        "category": job.category,
        "visualKey": job.visual_key,
        "outputPath": str(output_path),
        "model": model,
        "promptSha1": hashlib.sha1(prompt.encode("utf-8")).hexdigest(),
        "styleTag": job.style_tag,
        "artStyleBible": job.art_style_bible,
    }
    write_report(report_path, report_record)

    if delay_ms > 0:
        time.sleep(delay_ms / 1000.0)

    return ("generated", f"[gemini] {job.asset_id} -> {output_path}")


def main() -> int:
    args = parse_args()
    plan_path = pathlib.Path(args.plan).resolve()
    out_dir = pathlib.Path(args.out_dir).resolve()
    report_path = pathlib.Path(args.report).resolve()

    try:
        plan = load_plan(plan_path)
        jobs = parse_jobs(plan)
        if args.concurrency <= 0:
            raise ValueError("--concurrency must be >= 1")
    except Exception as exc:
        return fail(str(exc))

    if args.dry_run:
        for job in jobs:
            prompt = compile_prompt(job)
            print(f"{'=' * 72}")
            print(f"Asset: {job.asset_id}  |  Category: {job.category}  |  Gate: {job.gate_id}")
            print(f"Output: {job.output_name}")
            print(f"{'- ' * 36}")
            print(prompt)
            print()
        print(f"{'=' * 72}")
        print(f"Total: {len(jobs)} assets")
        return 0

    try:
        api_key = require_api_key(args.gemini_api_key)
    except Exception as exc:
        return fail(str(exc))

    out_dir.mkdir(parents=True, exist_ok=True)
    for job in jobs:
        if not job.gemini_key_required:
            return fail(f"Asset {job.asset_id} violates policy: geminiKeyRequired must be true")

    errors: list[str] = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.concurrency) as executor:
        future_to_job = {
            executor.submit(
                generate_job,
                job=job,
                out_dir=out_dir,
                report_path=report_path,
                api_key=api_key,
                model=args.model,
                timeout_s=args.timeout_s,
                image_aspect_ratio=args.image_aspect_ratio,
                image_size=args.image_size,
                request_retries=args.request_retries,
                delay_ms=args.delay_ms,
                skip_existing=args.skip_existing,
            ): job
            for job in jobs
        }
        for future in concurrent.futures.as_completed(future_to_job):
            job = future_to_job[future]
            try:
                _, message = future.result()
                print(message, flush=True)
            except Exception as exc:
                errors.append(f"Gemini generation failed for {job.asset_id}: {exc}")

    if errors:
        for message in errors:
            print(message, file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
