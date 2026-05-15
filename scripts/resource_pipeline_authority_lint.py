#!/usr/bin/env python3
"""Validate the repository-wide resource generation authority inventory."""

from __future__ import annotations

import argparse
import json
import pathlib
import re
import sys
from typing import Any

from asset_pipeline_common import load_json, load_yaml
from dark_sprite_sheet_contract import load_key_registry, load_sheet_plan, repo_relative_error


SCHEMA_VERSION = "resource-pipeline-authority-v1"
REPO_ROOT = pathlib.Path(__file__).resolve().parents[1]
DEFAULT_PRODUCTION_KOTLIN_ROOTS = (
    "core/src/main/kotlin",
    "game/src/main/kotlin",
    "client/src/main/kotlin",
    "tools/src/main/kotlin",
)
RESOURCE_MIRROR_DECLARATION_PATTERN = re.compile(
    r"\bval[ \t]+(?P<name>[A-Za-z0-9_]*(?:OwnerKeys|ExpectedKeys|RequiredKeys|InventoryKeys)|itemIconKeys)\b[ \t]*(?::|=|by\b)",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate project resource generation authority inputs.")
    parser.add_argument("--image-spec-dir", default="assets-src/image/specs")
    parser.add_argument("--audio-spec-dir", default="assets-src/audio/specs")
    parser.add_argument("--dark-sheet-plan", default="UI/sprite-sheets/sheet-plan.yaml")
    parser.add_argument("--dark-key-registry", default="UI/sprite-sheets/key-registry.yaml")
    parser.add_argument("--visual-manifest", default="assets-src/image/manifests/phase2-visual-manifest.json")
    parser.add_argument("--runtime-visual-manifest", default="client/src/main/resources/manifests/visual-manifest.json")
    parser.add_argument("--audio-manifest", default="assets-src/audio/manifests/phase2-audio-manifest.json")
    parser.add_argument("--runtime-audio-manifest", default="client/src/main/resources/manifests/audio-manifest.json")
    parser.add_argument(
        "--production-kotlin-root",
        action="append",
        dest="production_kotlin_roots",
        default=None,
        help="Production Kotlin root to scan for resource inventory mirrors. May be repeated.",
    )
    parser.add_argument("--report", default="build/reports/resource-pipeline/resource-pipeline-authority.json")
    return parser.parse_args()


def rel(path: pathlib.Path) -> str:
    try:
        return path.resolve().relative_to(REPO_ROOT).as_posix()
    except ValueError:
        return path.as_posix()


def load_entries_by_key(path: pathlib.Path, entry_name: str) -> dict[str, dict[str, Any]]:
    payload = load_json(path)
    entries = payload.get("entries")
    if not isinstance(entries, list):
        raise ValueError(f"{entry_name} entries must be a list: {rel(path)}")
    by_key: dict[str, dict[str, Any]] = {}
    for entry in entries:
        if not isinstance(entry, dict):
            raise ValueError(f"{entry_name} entry must be a mapping: {rel(path)}")
        key = str(entry.get("key", "")).strip()
        if not key:
            raise ValueError(f"{entry_name} entry key is required: {rel(path)}")
        by_key[key] = entry
    return by_key


def validate_manifest_sync(
    canonical_path: pathlib.Path,
    runtime_path: pathlib.Path,
    label: str,
    errors: list[str],
) -> None:
    canonical = load_json(canonical_path)
    runtime = load_json(runtime_path)
    if canonical != runtime:
        errors.append(
            f"{label} canonical/runtime manifests differ; run syncPhase2Manifests before treating runtime manifest as current."
        )


def validate_repo_relative(value: str, field_name: str, owner: str, errors: list[str]) -> None:
    error = repo_relative_error(value, field_name, owner)
    if error:
        errors.append(error)


def image_plan_records(
    image_spec_dir: pathlib.Path,
    visual_by_key: dict[str, dict[str, Any]],
    errors: list[str],
) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    for plan_path in sorted(image_spec_dir.glob("*.yaml")):
        payload = load_yaml(plan_path)
        if "phase2AssetGates" not in payload:
            continue
        style_tag = str(payload.get("styleTag", "")).strip()
        gates = payload.get("phase2AssetGates")
        if not isinstance(gates, dict):
            errors.append(f"{rel(plan_path)} phase2AssetGates must be a mapping.")
            continue
        for gate_id, gate_payload in gates.items():
            if not isinstance(gate_payload, dict):
                errors.append(f"{rel(plan_path)} gate {gate_id} must be a mapping.")
                continue
            assets = gate_payload.get("assets")
            if not isinstance(assets, list):
                errors.append(f"{rel(plan_path)} gate {gate_id} assets must be a list.")
                continue
            for index, asset in enumerate(assets):
                owner = f"{rel(plan_path)} gate {gate_id} asset[{index}]"
                if not isinstance(asset, dict):
                    errors.append(f"{owner} must be a mapping.")
                    continue
                asset_id = str(asset.get("id", "")).strip()
                visual_key = str(asset.get("visualKey", "")).strip()
                category = str(asset.get("category", "")).strip()
                output_name = str(asset.get("outputName", "")).strip()
                if not asset_id:
                    errors.append(f"{owner} id is required.")
                if not visual_key:
                    errors.append(f"{owner} visualKey is required.")
                if not category:
                    errors.append(f"{owner} category is required.")
                validate_repo_relative(output_name, "outputName", owner, errors)

                source_type = "app-icon" if category == "app_icon" or output_name.startswith("packaging/macos/") else "single-image"
                manifest_status = "not-required"
                manifest_path: str | None = None
                if source_type != "app-icon" and visual_key:
                    manifest_entry = visual_by_key.get(visual_key)
                    if manifest_entry is None:
                        manifest_status = "pack-local" if visual_key.startswith("sample_flooded_relics.") else "missing"
                        if manifest_status == "missing":
                            errors.append(f"{owner} visualKey '{visual_key}' is absent from canonical visual manifest.")
                    else:
                        manifest_path = str(manifest_entry.get("rawOutputPath", "")).strip()
                        if manifest_path != output_name:
                            manifest_status = "superseded-by-current-manifest"
                        else:
                            manifest_status = "current"
                records.append(
                    {
                        "sourceType": source_type,
                        "assetId": asset_id,
                        "visualKey": visual_key,
                        "styleTag": style_tag,
                        "sourcePlan": rel(plan_path),
                        "outputPath": output_name,
                        "manifestKey": None if source_type == "app-icon" else visual_key,
                        "manifestPath": manifest_path,
                        "manifestStatus": manifest_status,
                        "ownerScope": str(gate_id),
                        "consumerTest": None,
                    },
                )
    return records


def dark_sheet_records(
    sheet_plan_path: pathlib.Path,
    key_registry_path: pathlib.Path,
    visual_by_key: dict[str, dict[str, Any]],
    errors: list[str],
) -> list[dict[str, Any]]:
    sheets, cells, sheet_errors = load_sheet_plan(sheet_plan_path)
    registry_by_key, registry_errors = load_key_registry(key_registry_path)
    errors.extend(sheet_errors)
    errors.extend(registry_errors)
    sheet_by_id = {sheet.sheet_id: sheet for sheet in sheets}
    records: list[dict[str, Any]] = []
    for cell in cells:
        if cell.reserved:
            continue
        registry_entry = registry_by_key.get(cell.target_key)
        if registry_entry is None:
            errors.append(f"{cell.id} targetKey '{cell.target_key}' is absent from dark key registry.")
        manifest_entry = visual_by_key.get(cell.target_key)
        if manifest_entry is None:
            errors.append(f"{cell.id} targetKey '{cell.target_key}' is absent from canonical visual manifest.")
        else:
            manifest_path = str(manifest_entry.get("rawOutputPath", "")).strip()
            if manifest_path != cell.output_name:
                errors.append(
                    f"{cell.id} outputName must match canonical rawOutputPath for '{cell.target_key}': "
                    f"sheet='{cell.output_name}' manifest='{manifest_path}'."
                )
        sheet = sheet_by_id.get(cell.sheet_id)
        records.append(
            {
                "sourceType": "sprite-sheet",
                "assetId": f"{cell.sheet_id}:{cell.row}:{cell.col}",
                "visualKey": cell.target_key,
                "styleTag": "ktome-dark-fantasy-sprite-ui-v1",
                "sourcePlan": rel(sheet_plan_path),
                "rawPath": sheet.raw_sheet_path if sheet else None,
                "outputPath": cell.output_name,
                "manifestKey": cell.target_key,
                "ownerScope": str(registry_entry.get("ownerPr", "")) if registry_entry else None,
                "consumerTest": str(registry_entry.get("consumerTest", "")) if registry_entry else None,
            },
        )
    return records


def audio_plan_records(
    audio_spec_dir: pathlib.Path,
    audio_by_key: dict[str, dict[str, Any]],
    errors: list[str],
) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    for plan_path in sorted(audio_spec_dir.glob("*.yaml")):
        payload = load_yaml(plan_path)
        entries = payload.get("entries")
        if isinstance(entries, list):
            for index, entry in enumerate(entries):
                owner = f"{rel(plan_path)} entry[{index}]"
                if not isinstance(entry, dict):
                    errors.append(f"{owner} must be a mapping.")
                    continue
                key = str(entry.get("key", "")).strip()
                cue_family = str(entry.get("cueFamily", "")).strip()
                event_id = str(entry.get("eventId", "")).strip()
                source_path = str(entry.get("sourcePath", "")).strip()
                if not key or not cue_family or not event_id or not source_path:
                    errors.append(f"{owner} must define key/cueFamily/eventId/sourcePath.")
                    continue
                validate_repo_relative(source_path, "sourcePath", owner, errors)
                manifest_entry = audio_by_key.get(key)
                manifest_status = "current"
                if manifest_entry is None:
                    manifest_status = "pack-local" if key.startswith("sample_flooded_relics.") else "missing"
                    if manifest_status == "missing":
                        errors.append(f"{owner} key '{key}' is absent from canonical audio manifest.")
                else:
                    for field_name in ("cueFamily", "eventId", "sourcePath"):
                        if str(entry.get(field_name, "")).strip() != str(manifest_entry.get(field_name, "")).strip():
                            errors.append(
                                f"{owner} {field_name} must match canonical audio manifest for '{key}': "
                                f"plan='{entry.get(field_name, '')}' manifest='{manifest_entry.get(field_name, '')}'."
                            )
                records.append(
                    {
                        "sourceType": "audio",
                        "assetId": key,
                        "audioKey": key,
                        "sourcePlan": rel(plan_path),
                        "sourcePath": source_path,
                        "manifestKey": key,
                        "manifestStatus": manifest_status,
                        "cueFamily": cue_family,
                        "eventId": event_id,
                        "ownerScope": next((str(tag) for tag in entry.get("tags", []) if str(tag).startswith("pr")), None)
                        if isinstance(entry.get("tags"), list)
                        else None,
                    },
                )
        targets = payload.get("targets")
        if isinstance(targets, list):
            for index, target in enumerate(targets):
                owner = f"{rel(plan_path)} target[{index}]"
                if not isinstance(target, dict):
                    errors.append(f"{owner} must be a mapping.")
                    continue
                key = str(target.get("key", "")).strip()
                source_path = str(target.get("sourcePath", "")).strip()
                profile = str(target.get("profile", "")).strip()
                if not key or not source_path or not profile:
                    errors.append(f"{owner} must define key/sourcePath/profile.")
                    continue
                validate_repo_relative(source_path, "sourcePath", owner, errors)
                manifest_entry = audio_by_key.get(key)
                if manifest_entry is not None and str(manifest_entry.get("sourcePath", "")).strip() != source_path:
                    errors.append(
                        f"{owner} sourcePath must match canonical audio manifest for '{key}': "
                        f"target='{source_path}' manifest='{manifest_entry.get('sourcePath', '')}'."
                    )
                records.append(
                    {
                        "sourceType": "audio-generation-target",
                        "assetId": str(target.get("id", key)).strip(),
                        "audioKey": key,
                        "sourcePlan": rel(plan_path),
                        "sourcePath": source_path,
                        "manifestKey": key if manifest_entry is not None else None,
                        "profile": profile,
                    },
                )
    return records


def validate_no_resource_inventory_mirrors(
    production_kotlin_roots: list[str],
    errors: list[str],
) -> None:
    for root in production_kotlin_roots:
        root_path = REPO_ROOT / root
        if not root_path.is_dir():
            continue
        for path in sorted(root_path.rglob("*.kt")):
            text = path.read_text(encoding="utf-8")
            for match in RESOURCE_MIRROR_DECLARATION_PATTERN.finditer(text):
                line_number = text.count("\n", 0, match.start()) + 1
                name = match.group("name")
                errors.append(
                    f"{rel(path)}:{line_number} declares resource inventory mirror '{name}'. "
                    "Use the canonical resource plan, key registry, or manifest-derived lint report instead."
                )


def write_report(path: pathlib.Path, records: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(records, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def main() -> int:
    args = parse_args()
    image_spec_dir = REPO_ROOT / args.image_spec_dir
    audio_spec_dir = REPO_ROOT / args.audio_spec_dir
    visual_manifest_path = REPO_ROOT / args.visual_manifest
    runtime_visual_manifest_path = REPO_ROOT / args.runtime_visual_manifest
    audio_manifest_path = REPO_ROOT / args.audio_manifest
    runtime_audio_manifest_path = REPO_ROOT / args.runtime_audio_manifest
    production_kotlin_roots = args.production_kotlin_roots or list(DEFAULT_PRODUCTION_KOTLIN_ROOTS)
    errors: list[str] = []

    visual_by_key = load_entries_by_key(visual_manifest_path, "canonical visual manifest")
    audio_by_key = load_entries_by_key(audio_manifest_path, "canonical audio manifest")
    validate_manifest_sync(visual_manifest_path, runtime_visual_manifest_path, "visual", errors)
    validate_manifest_sync(audio_manifest_path, runtime_audio_manifest_path, "audio", errors)

    visual_records = image_plan_records(image_spec_dir, visual_by_key, errors)
    visual_records.extend(
        dark_sheet_records(
            REPO_ROOT / args.dark_sheet_plan,
            REPO_ROOT / args.dark_key_registry,
            visual_by_key,
            errors,
        ),
    )
    audio_records = audio_plan_records(audio_spec_dir, audio_by_key, errors)
    validate_no_resource_inventory_mirrors(production_kotlin_roots, errors)

    report = {
        "schemaVersion": SCHEMA_VERSION,
        "authoritySources": {
            "imageSpecDir": args.image_spec_dir,
            "audioSpecDir": args.audio_spec_dir,
            "darkSheetPlan": args.dark_sheet_plan,
            "darkKeyRegistry": args.dark_key_registry,
            "visualManifest": args.visual_manifest,
            "audioManifest": args.audio_manifest,
            "productionKotlinRoots": production_kotlin_roots,
        },
        "totals": {
            "visualAssets": len(visual_records),
            "audioAssets": len(audio_records),
            "singleImageAssets": sum(1 for record in visual_records if record["sourceType"] == "single-image"),
            "spriteSheetAssets": sum(1 for record in visual_records if record["sourceType"] == "sprite-sheet"),
            "appIconAssets": sum(1 for record in visual_records if record["sourceType"] == "app-icon"),
            "audioGenerationTargets": sum(1 for record in audio_records if record["sourceType"] == "audio-generation-target"),
        },
        "visualAssets": visual_records,
        "audioAssets": audio_records,
    }
    write_report(REPO_ROOT / args.report, report)

    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1

    print(
        "resource-pipeline-authority OK: "
        f"visualAssets={len(visual_records)}, audioAssets={len(audio_records)}, report={args.report}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
