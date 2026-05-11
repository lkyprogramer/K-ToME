#!/usr/bin/env python3
"""Validate dark-v1 sheet-plan schema or raw sheet mapping artifacts."""

from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
from typing import Any

from dark_sprite_sheet_contract import (
    DARK_CONTACT_SHEET_DIR,
    DARK_RUNTIME_PREFIX,
    STYLE_TAG,
    load_manifest_entries,
    load_owner_contract,
    load_sheet_plan,
    load_yaml,
    print_errors,
    repo_relative_error,
    safe_int,
    sha256_file,
)


def load_pillow_image():
    try:
        from PIL import Image
    except ModuleNotFoundError as exc:
        if exc.name == "PIL":
            raise RuntimeError(
                "Pillow is required for --check map image inspection; install the PIL package or run --check sheet-plan."
            ) from exc
        raise
    return Image


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate dark-v1 sprite sheet plan and map artifacts.")
    parser.add_argument("--plan", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/sheet-plan.yaml"))
    parser.add_argument("--manifest", type=pathlib.Path, default=pathlib.Path("assets-src/image/manifests/phase2-visual-manifest.json"))
    parser.add_argument("--check", choices=("sheet-plan", "map"), default="map")
    parser.add_argument("--raw-root", type=pathlib.Path, default=None)
    parser.add_argument("--contact-root", type=pathlib.Path, default=pathlib.Path(DARK_CONTACT_SHEET_DIR))
    parser.add_argument("--runtime-root", type=pathlib.Path, default=pathlib.Path("client/src/main/resources"))
    parser.add_argument("--report", type=pathlib.Path, default=pathlib.Path("assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl"))
    parser.add_argument("--require-full-grid", action="store_true")
    parser.add_argument("--owner-contract", type=pathlib.Path, default=None)
    return parser.parse_args()


def cell_rect(sheet, cell) -> tuple[int, int, int, int]:
    left = cell.col * sheet.grid["cellWidth"]
    upper = cell.row * sheet.grid["cellHeight"]
    return (left, upper, left + sheet.grid["cellWidth"], upper + sheet.grid["cellHeight"])


def raw_sheet_path(sheet, raw_root: pathlib.Path | None) -> pathlib.Path:
    if raw_root is None:
        return pathlib.Path(sheet.raw_sheet_path)
    return raw_root / f"{sheet.sheet_id}.png"


def load_existing_qa_records(report_path: pathlib.Path) -> dict[tuple[str, str], dict[str, Any]]:
    if not report_path.is_file():
        return {}
    records: dict[tuple[str, str], dict[str, Any]] = {}
    for line in report_path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        try:
            row = json.loads(line)
        except json.JSONDecodeError:
            continue
        sheet_id = row.get("sheetId")
        target_key = row.get("targetKey")
        if isinstance(sheet_id, str) and isinstance(target_key, str):
            records[(sheet_id, target_key)] = row
    return records


def preserve_existing_qa_fields(
    record: dict[str, Any],
    existing_records: dict[tuple[str, str], dict[str, Any]],
) -> dict[str, Any]:
    previous = existing_records.get((record["sheetId"], record["targetKey"]))
    if previous is None or previous.get("qaStatus") in (None, "DRY_RUN"):
        return record
    stable_fields = ("rawSheetHash", "cellRect", "cellHash", "outputName", "outputHash")
    if any(previous.get(field) != record.get(field) for field in stable_fields):
        return record
    for field in ("qaStatus", "reviewer", "reviewedAt", "rejectionReason"):
        record[field] = previous.get(field)
    return record


def validate_manifest_mapping(cells, manifest_path: pathlib.Path) -> list[str]:
    errors: list[str] = []
    manifest_by_key = load_manifest_entries(manifest_path)
    for cell in cells:
        entry = manifest_by_key.get(cell.target_key)
        if entry is None:
            errors.append(f"{cell.target_key} is missing from canonical visual manifest.")
            continue
        if str(entry.get("category", "")).strip() != cell.category:
            errors.append(
                f"{cell.target_key} manifest category mismatch: manifest={entry.get('category')} sheet-plan={cell.category}."
            )
        if str(entry.get("rawOutputPath", "")).strip() != cell.output_name:
            errors.append(
                f"{cell.target_key} manifest rawOutputPath mismatch: manifest={entry.get('rawOutputPath')} sheet-plan={cell.output_name}."
            )
    return errors


def validate_full_grid_occupancy(plan_path: pathlib.Path, sheets) -> list[str]:
    payload = load_yaml(plan_path)
    sheet_payloads = payload.get("sheets")
    if not isinstance(sheet_payloads, list):
        return []

    sheet_by_id = {sheet.sheet_id: sheet for sheet in sheets}
    errors: list[str] = []
    for sheet_payload in sheet_payloads:
        if not isinstance(sheet_payload, dict):
            continue
        sheet_id = str(sheet_payload.get("sheetId", "")).strip()
        sheet = sheet_by_id.get(sheet_id)
        if sheet is None:
            continue
        cells = sheet_payload.get("cells")
        if not isinstance(cells, list):
            continue
        listed_slots = {
            (safe_int(cell.get("row"), -1), safe_int(cell.get("col"), -1))
            for cell in cells
            if isinstance(cell, dict)
        }
        expected_slots = {
            (row, col)
            for row in range(sheet.grid["rows"])
            for col in range(sheet.grid["columns"])
        }
        missing_slots = sorted(expected_slots - listed_slots)
        if missing_slots:
            preview = ", ".join(f"{row}:{col}" for row, col in missing_slots[:12])
            suffix = "" if len(missing_slots) <= 12 else f", ... total={len(missing_slots)}"
            errors.append(
                f"{sheet_id} must list every grid slot when --require-full-grid is set; "
                f"missing slots={preview}{suffix}."
            )
    return errors


def count_cells_by_sheet(plan_path: pathlib.Path) -> dict[str, dict[str, int]]:
    payload = load_yaml(plan_path)
    sheet_payloads = payload.get("sheets")
    if not isinstance(sheet_payloads, list):
        return {}

    counts_by_sheet: dict[str, dict[str, int]] = {}
    for sheet_payload in sheet_payloads:
        if not isinstance(sheet_payload, dict):
            continue
        sheet_id = str(sheet_payload.get("sheetId", "")).strip()
        cells = sheet_payload.get("cells")
        if not sheet_id or not isinstance(cells, list):
            continue
        counts = {"direct": 0, "alias": 0, "reserved": 0, "total": 0}
        for cell in cells:
            if not isinstance(cell, dict):
                continue
            counts["total"] += 1
            if cell.get("reserved") is True:
                counts["reserved"] += 1
            elif str(cell.get("aliasOf", "")).strip():
                counts["alias"] += 1
            else:
                counts["direct"] += 1
        counts_by_sheet[sheet_id] = counts
    return counts_by_sheet


def validate_owner_contract(plan_path: pathlib.Path, cells, owner_contract_path: pathlib.Path | None) -> list[str]:
    if owner_contract_path is None:
        return []

    owner_contract, errors = load_owner_contract(owner_contract_path)
    if owner_contract is None:
        return errors

    cells_by_key = {cell.target_key: cell for cell in cells}
    for required_cell in owner_contract.required_cells:
        plan_cell = cells_by_key.get(required_cell.target_key)
        if plan_cell is None:
            errors.append(
                f"{owner_contract.owner_pr} owner contract required key is missing from sheet-plan: "
                f"{required_cell.target_key}."
            )
            continue
        if plan_cell.sheet_id != required_cell.sheet_id:
            errors.append(
                f"{required_cell.target_key} sheet mismatch: "
                f"contract={required_cell.sheet_id} sheet-plan={plan_cell.sheet_id}."
            )
        if plan_cell.category != required_cell.category:
            errors.append(
                f"{required_cell.target_key} category mismatch: "
                f"contract={required_cell.category} sheet-plan={plan_cell.category}."
            )
        if plan_cell.alias_of:
            errors.append(f"{required_cell.target_key} must be a direct cell for {owner_contract.owner_pr}, not aliasOf={plan_cell.alias_of}.")

    actual_counts = count_cells_by_sheet(plan_path)
    for sheet_id, required_counts in owner_contract.required_counts_by_sheet.items():
        actual = actual_counts.get(sheet_id, {"direct": 0, "alias": 0, "reserved": 0, "total": 0})
        expected = {
            "direct": required_counts.direct,
            "alias": required_counts.alias,
            "reserved": required_counts.reserved,
            "total": required_counts.total,
        }
        if actual != expected:
            errors.append(
                f"{sheet_id} cell counts mismatch for {owner_contract.owner_pr}: "
                f"expected={expected} actual={actual}."
            )
    return errors


def validate_sheet_plan_only(
    plan_path: pathlib.Path,
    require_full_grid: bool,
    owner_contract_path: pathlib.Path | None,
) -> list[str]:
    sheets, cells, errors = load_sheet_plan(plan_path)
    if len([cell for cell in cells if not cell.reserved]) < 3:
        errors.append("sheet-plan must contain at least 3 non-reserved dry-run cells.")
    for sheet in sheets:
        for field_name, value in (("rawSheetPath", sheet.raw_sheet_path), ("outputRoot", sheet.output_root)):
            error = repo_relative_error(value, field_name, sheet.sheet_id)
            if error:
                errors.append(error)
    if require_full_grid:
        errors += validate_full_grid_occupancy(plan_path, sheets)
    errors += validate_owner_contract(plan_path, cells, owner_contract_path)
    return errors


def validate_map(
    plan_path: pathlib.Path,
    manifest_path: pathlib.Path,
    raw_root: pathlib.Path | None,
    contact_root: pathlib.Path,
    runtime_root: pathlib.Path,
    report_path: pathlib.Path,
) -> list[str]:
    sheets, cells, errors = load_sheet_plan(plan_path)
    if errors:
        return errors
    errors += validate_manifest_mapping(cells, manifest_path)
    cells_by_sheet = {sheet.sheet_id: [cell for cell in cells if cell.sheet_id == sheet.sheet_id] for sheet in sheets}
    records: list[dict[str, Any]] = []
    existing_qa_records = load_existing_qa_records(report_path)
    image_module = None

    for sheet in sheets:
        raw_path = raw_sheet_path(sheet, raw_root)
        if not raw_path.is_file():
            errors.append(f"missingRawSheet sheetId={sheet.sheet_id} expected={raw_path.as_posix()}.")
            continue
        if raw_path.name != f"{sheet.sheet_id}.png":
            errors.append(f"{sheet.sheet_id} raw sheet file must be named {sheet.sheet_id}.png, got {raw_path.name}.")
        raw_hash = sha256_file(raw_path)
        if image_module is None:
            try:
                image_module = load_pillow_image()
            except RuntimeError as exc:
                errors.append(str(exc))
                break
        with image_module.open(raw_path) as image:
            expected_size = sheet.canvas_size
            if image.size != expected_size:
                errors.append(f"{sheet.sheet_id} raw sheet size must be {expected_size}, got {image.size}.")
            rgba = image.convert("RGBA")
            for cell in cells_by_sheet[sheet.sheet_id]:
                rect = cell_rect(sheet, cell)
                crop = rgba.crop(rect)
                if crop.getbbox() is None:
                    errors.append(f"{cell.id} alpha bbox is empty.")
                output_hash = None
                sliced_output_required = cell.output_name.startswith(DARK_RUNTIME_PREFIX)
                if sliced_output_required:
                    output_path = runtime_root / cell.output_name
                    if not output_path.is_file():
                        errors.append(f"{cell.id} sliced output is missing: {output_path.as_posix()}.")
                    else:
                        output_hash = sha256_file(output_path)
                cell_bytes = crop.tobytes()
                records.append(
                    preserve_existing_qa_fields(
                        {
                            "schemaVersion": "dark-sprite-map-report-v1",
                            "styleTag": STYLE_TAG,
                            "qaStatus": "DRY_RUN",
                            "sheetId": sheet.sheet_id,
                            "targetKey": cell.target_key,
                            "rawSheetPath": sheet.raw_sheet_path,
                            "rawSheetHash": raw_hash,
                            "cellRect": {"left": rect[0], "top": rect[1], "right": rect[2], "bottom": rect[3]},
                            "cellHash": hashlib.sha256(cell_bytes).hexdigest(),
                            "outputName": cell.output_name,
                            "slicedOutputRequired": sliced_output_required,
                            "outputHash": output_hash,
                            "reviewer": None,
                            "reviewedAt": None,
                            "rejectionReason": None,
                        },
                        existing_qa_records,
                    )
                )
        contact_path = contact_root / f"{sheet.sheet_id}-contact.png"
        if not contact_path.is_file():
            errors.append(f"{sheet.sheet_id} contact sheet is missing: {contact_path.as_posix()}.")

    if not errors:
        report_path.parent.mkdir(parents=True, exist_ok=True)
        report_path.write_text(
            "".join(json.dumps(record, sort_keys=True) + "\n" for record in records),
            encoding="utf-8",
        )
    return errors


def main() -> int:
    args = parse_args()
    if args.check == "sheet-plan":
        errors = validate_sheet_plan_only(args.plan, args.require_full_grid, args.owner_contract)
        label = "dark-sprite-sheet-lint"
    else:
        if args.owner_contract is not None:
            return print_errors("sprite-sheet-map-lint", ["--owner-contract is only supported with --check sheet-plan."])
        errors = validate_map(args.plan, args.manifest, args.raw_root, args.contact_root, args.runtime_root, args.report)
        label = "sprite-sheet-map-lint"
    if errors:
        return print_errors(label, errors)
    print(f"{label} OK: plan={args.plan.as_posix()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
