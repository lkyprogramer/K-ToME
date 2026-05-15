#!/usr/bin/env python3
"""Shared contract code for the dark-v1 sprite sheet pipeline."""

from __future__ import annotations

import hashlib
import json
import pathlib
import re
from dataclasses import dataclass
from typing import Any

from asset_pipeline_common import ALLOWED_CATEGORIES, load_json, load_yaml


STYLE_TAG = "ktome-dark-fantasy-sprite-ui-v1"
SHEET_PLAN_SCHEMA_VERSION = "dark-sprite-sheet-plan-v1"
KEY_REGISTRY_SCHEMA_VERSION = "dark-key-registry-v1"
OWNER_CONTRACT_SCHEMA_VERSION = "dark-owner-contract-v1"
OWNER_PR_PATTERN = re.compile(r"^PR-\d{2}(?:-\d+)?$")
OWNER_PR_PATTERN_TEXT = r"^PR-\d{2}(?:-\d+)?$"
DARK_RUNTIME_PREFIX = "dark-v1/"
DARK_RAW_SHEET_DIR = "assets-src/image/raw/sheets/dark-v1"
DARK_CONTACT_SHEET_DIR = "assets-src/image/contact-sheets/dark-v1"
PENDING_RAW_OUTPUT = "debug/missing_visual.png"
SHEET_TYPE_POLICIES = {
    "icon-sheet": {"columns": 8, "rows": 8, "cellWidth": 128, "cellHeight": 128},
    "large-sheet": {"columns": 4, "rows": 4, "cellWidth": 256, "cellHeight": 256},
    "tile-sheet": {"columns": 8, "rows": 8, "cellWidth": 128, "cellHeight": 128},
}
CELL_INPUT_FIELDS = {
    "row",
    "col",
    "targetKey",
    "category",
    "outputName",
    "subject",
    "reserved",
    "aliasOf",
    "note",
}
QA_OUTPUT_FIELDS = {
    "qaStatus",
    "rawSheetHash",
    "cellRect",
    "cellHash",
    "outputHash",
    "reviewer",
    "reviewedAt",
    "rejectionReason",
}


@dataclass(frozen=True)
class SheetPlan:
    sheet_id: str
    round_id: int
    sheet_type: str
    raw_sheet_path: str
    output_root: str
    prompt_base: str
    grid: dict[str, int]

    @property
    def capacity(self) -> int:
        return self.grid["columns"] * self.grid["rows"]

    @property
    def canvas_size(self) -> tuple[int, int]:
        return (
            self.grid["columns"] * self.grid["cellWidth"],
            self.grid["rows"] * self.grid["cellHeight"],
        )


@dataclass(frozen=True)
class PlanCell:
    sheet_id: str
    row: int
    col: int
    target_key: str
    category: str
    output_name: str
    subject: str
    reserved: bool
    alias_of: str | None

    @property
    def id(self) -> str:
        return f"{self.sheet_id}:{self.row}:{self.col}"


@dataclass(frozen=True)
class OwnerRequiredCell:
    target_key: str
    sheet_id: str
    category: str


@dataclass(frozen=True)
class OwnerSheetCellCounts:
    direct: int
    alias: int
    reserved: int
    total: int


@dataclass(frozen=True)
class OwnerContract:
    owner_pr: str
    required_sheet_ids: list[str]
    required_cells: list[OwnerRequiredCell]
    required_counts_by_sheet: dict[str, OwnerSheetCellCounts]


def repo_relative_error(value: str, field_name: str, owner: str) -> str | None:
    candidate = pathlib.PurePosixPath(value.replace("\\", "/"))
    if not value.strip():
        return f"{owner} {field_name} is required."
    if pathlib.PurePath(value).is_absolute() or candidate.is_absolute():
        return f"{owner} {field_name} must be repo-relative: {value}."
    if ".." in candidate.parts:
        return f"{owner} {field_name} must not escape the repo: {value}."
    if value.startswith("/Users/") or value.startswith("/tmp/") or ":\\" in value:
        return f"{owner} {field_name} must not contain a machine absolute path: {value}."
    return None


def sha256_file(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def sha256_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def safe_int(value: Any, default: int = 0) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def load_sheet_plan(path: pathlib.Path) -> tuple[list[SheetPlan], list[PlanCell], list[str]]:
    payload = load_yaml(path)
    errors: list[str] = []
    schema_version = str(payload.get("schemaVersion", "")).strip()
    if schema_version != SHEET_PLAN_SCHEMA_VERSION:
        errors.append(f"sheet-plan schemaVersion must be {SHEET_PLAN_SCHEMA_VERSION}, got {schema_version or '<missing>'}.")
    sheets_payload = payload.get("sheets")
    if not isinstance(sheets_payload, list) or not sheets_payload:
        errors.append("sheet-plan.yaml must define a non-empty sheets list.")
        return [], [], errors

    root_style_tag = str(payload.get("styleTag", "")).strip()
    if root_style_tag and root_style_tag != STYLE_TAG:
        errors.append(f"sheet-plan root styleTag must be {STYLE_TAG}, got {root_style_tag}.")

    sheets: list[SheetPlan] = []
    cells: list[PlanCell] = []
    seen_sheet_ids: set[str] = set()
    seen_cell_slots: set[str] = set()
    seen_target_keys: set[str] = set()

    for index, sheet in enumerate(sheets_payload):
        owner = f"sheet[{index}]"
        if not isinstance(sheet, dict):
            errors.append(f"{owner} must be a mapping.")
            continue
        qa_fields = sorted(QA_OUTPUT_FIELDS.intersection(sheet.keys()))
        if qa_fields:
            errors.append(f"{owner} must not contain QA output fields: {', '.join(qa_fields)}.")

        sheet_id = str(sheet.get("sheetId", "")).strip()
        sheet_type = str(sheet.get("type", "")).strip()
        style_tag = str(sheet.get("styleTag", "")).strip()
        raw_sheet_path = str(sheet.get("rawSheetPath", "")).strip()
        output_root = str(sheet.get("outputRoot", "")).strip()
        prompt_base = str(sheet.get("promptBase", "")).strip()
        round_value = sheet.get("round")
        grid_payload = sheet.get("grid")

        if not sheet_id:
            errors.append(f"{owner} sheetId is required.")
        elif sheet_id in seen_sheet_ids:
            errors.append(f"Duplicate sheetId '{sheet_id}'.")
        seen_sheet_ids.add(sheet_id)

        if sheet_type not in SHEET_TYPE_POLICIES:
            errors.append(f"{owner} type must be one of {sorted(SHEET_TYPE_POLICIES)}, got '{sheet_type}'.")
        if style_tag != STYLE_TAG:
            errors.append(f"{owner} styleTag must be {STYLE_TAG}, got '{style_tag}'.")
        if not isinstance(round_value, int) or round_value < 1:
            errors.append(f"{owner} round must be a positive integer.")
            round_id = 0
        else:
            round_id = round_value
        for field_name, value in (("rawSheetPath", raw_sheet_path), ("outputRoot", output_root)):
            error = repo_relative_error(value, field_name, owner)
            if error:
                errors.append(error)
        if sheet_id and raw_sheet_path != f"{DARK_RAW_SHEET_DIR}/{sheet_id}.png":
            errors.append(
                f"{owner} rawSheetPath must be {DARK_RAW_SHEET_DIR}/{sheet_id}.png, got {raw_sheet_path}."
            )
        if not prompt_base:
            errors.append(f"{owner} promptBase is required.")
        if not isinstance(grid_payload, dict):
            errors.append(f"{owner} grid must be a mapping.")
            grid = {"columns": 0, "rows": 0, "cellWidth": 0, "cellHeight": 0}
        else:
            grid = {
                "columns": safe_int(grid_payload.get("columns")),
                "rows": safe_int(grid_payload.get("rows")),
                "cellWidth": safe_int(grid_payload.get("cellWidth")),
                "cellHeight": safe_int(grid_payload.get("cellHeight")),
            }
            if sheet_type in SHEET_TYPE_POLICIES and grid != SHEET_TYPE_POLICIES[sheet_type]:
                errors.append(f"{owner} grid must match {sheet_type} policy {SHEET_TYPE_POLICIES[sheet_type]}, got {grid}.")

        sheet_plan = SheetPlan(
            sheet_id=sheet_id,
            round_id=round_id,
            sheet_type=sheet_type,
            raw_sheet_path=raw_sheet_path,
            output_root=output_root,
            prompt_base=prompt_base,
            grid=grid,
        )
        sheets.append(sheet_plan)

        sheet_cells = sheet.get("cells")
        if not isinstance(sheet_cells, list) or not sheet_cells:
            errors.append(f"{owner} cells must be a non-empty list.")
            continue
        if len(sheet_cells) > sheet_plan.capacity:
            errors.append(f"{owner} declares {len(sheet_cells)} cells but capacity is {sheet_plan.capacity}.")

        for cell_index, cell in enumerate(sheet_cells):
            cell_owner = f"{owner}.cell[{cell_index}]"
            if not isinstance(cell, dict):
                errors.append(f"{cell_owner} must be a mapping.")
                continue
            qa_fields = sorted(QA_OUTPUT_FIELDS.intersection(cell.keys()))
            if qa_fields:
                errors.append(f"{cell_owner} must not contain QA output fields: {', '.join(qa_fields)}.")
            unsupported_fields = sorted(set(cell) - CELL_INPUT_FIELDS - QA_OUTPUT_FIELDS)
            if unsupported_fields:
                errors.append(f"{cell_owner} contains unsupported fields: {', '.join(unsupported_fields)}.")
            row = safe_int(cell.get("row"), -1)
            col = safe_int(cell.get("col"), -1)
            if row < 0 or row >= sheet_plan.grid["rows"] or col < 0 or col >= sheet_plan.grid["columns"]:
                errors.append(f"{cell_owner} row/col must be inside grid {sheet_plan.grid}, got row={row} col={col}.")
            slot_id = f"{sheet_id}:{row}:{col}"
            if slot_id in seen_cell_slots:
                errors.append(f"Duplicate sheet cell slot '{slot_id}'.")
            seen_cell_slots.add(slot_id)

            reserved = cell.get("reserved") is True
            target_key = str(cell.get("targetKey", "")).strip()
            category = str(cell.get("category", "")).strip()
            output_name = str(cell.get("outputName", "")).strip()
            subject = str(cell.get("subject", "")).strip()
            alias_of = str(cell.get("aliasOf", "")).strip() or None

            if reserved:
                if target_key or category or output_name:
                    errors.append(f"{cell_owner} reserved cells must not define targetKey/category/outputName.")
            else:
                if not target_key:
                    errors.append(f"{cell_owner} targetKey is required.")
                elif target_key in seen_target_keys:
                    errors.append(f"Duplicate targetKey '{target_key}'.")
                seen_target_keys.add(target_key)
                if category not in ALLOWED_CATEGORIES:
                    errors.append(f"{cell_owner} category '{category}' is not in the asset pipeline allowlist.")
                error = repo_relative_error(output_name, "outputName", cell_owner)
                if error:
                    errors.append(error)
                if not subject:
                    errors.append(f"{cell_owner} subject is required.")
                cells.append(
                    PlanCell(
                        sheet_id=sheet_id,
                        row=row,
                        col=col,
                        target_key=target_key,
                        category=category,
                        output_name=output_name,
                        subject=subject,
                        reserved=False,
                        alias_of=alias_of,
                    )
                )

    target_keys = {cell.target_key for cell in cells}
    cells_by_key = {cell.target_key: cell for cell in cells}
    non_alias_dark_outputs: dict[str, str] = {}
    for cell in cells:
        if cell.alias_of:
            target = cells_by_key.get(cell.alias_of)
            if cell.alias_of not in target_keys:
                errors.append(f"{cell.id} aliasOf target does not exist in sheet plan: {cell.alias_of}.")
            elif target and cell.output_name != target.output_name:
                errors.append(
                    f"{cell.id} alias outputName must match aliasOf target {cell.alias_of}: "
                    f"alias={cell.output_name} target={target.output_name}."
                )
        elif cell.output_name.startswith(DARK_RUNTIME_PREFIX):
            existing_owner = non_alias_dark_outputs.get(cell.output_name)
            if existing_owner:
                errors.append(
                    f"{cell.id} outputName duplicates sliced dark-v1 output from {existing_owner}: {cell.output_name}."
                )
            else:
                non_alias_dark_outputs[cell.output_name] = cell.id
    return sheets, cells, errors


def load_key_registry(path: pathlib.Path) -> tuple[dict[str, dict[str, Any]], list[str]]:
    payload = load_yaml(path)
    errors: list[str] = []
    schema_version = str(payload.get("schemaVersion", "")).strip()
    if schema_version != KEY_REGISTRY_SCHEMA_VERSION:
        errors.append(f"key-registry schemaVersion must be {KEY_REGISTRY_SCHEMA_VERSION}, got {schema_version or '<missing>'}.")
    entries = payload.get("entries")
    if not isinstance(entries, list) or not entries:
        errors.append("key-registry.yaml must define a non-empty entries list.")
        return {}, errors
    style_tag = str(payload.get("styleTag", "")).strip()
    if style_tag and style_tag != STYLE_TAG:
        errors.append(f"key-registry styleTag must be {STYLE_TAG}, got {style_tag}.")
    by_key: dict[str, dict[str, Any]] = {}
    for index, entry in enumerate(entries):
        owner = f"key-registry entry[{index}]"
        if not isinstance(entry, dict):
            errors.append(f"{owner} must be a mapping.")
            continue
        target_key = str(entry.get("targetKey", "")).strip()
        if not target_key:
            errors.append(f"{owner} targetKey is required.")
            continue
        if target_key in by_key:
            errors.append(f"Duplicate key-registry targetKey '{target_key}'.")
        by_key[target_key] = entry
    return by_key, errors


def load_owner_contract(path: pathlib.Path) -> tuple[OwnerContract | None, list[str]]:
    payload = load_yaml(path)
    errors: list[str] = []
    schema_version = str(payload.get("schemaVersion", "")).strip()
    if schema_version != OWNER_CONTRACT_SCHEMA_VERSION:
        errors.append(
            f"owner contract schemaVersion must be {OWNER_CONTRACT_SCHEMA_VERSION}, got {schema_version or '<missing>'}."
        )

    owner_pr = str(payload.get("ownerPr", "")).strip()
    if not OWNER_PR_PATTERN.match(owner_pr):
        errors.append(f"owner contract ownerPr must match {OWNER_PR_PATTERN_TEXT}, got '{owner_pr or '<missing>'}'.")

    raw_sheet_ids = payload.get("requiredSheetIds")
    if not isinstance(raw_sheet_ids, list) or not raw_sheet_ids:
        errors.append("owner contract requiredSheetIds must be a non-empty list.")
        required_sheet_ids: list[str] = []
    else:
        required_sheet_ids = sorted({str(sheet_id).strip() for sheet_id in raw_sheet_ids if str(sheet_id).strip()})
        if len(required_sheet_ids) != len(raw_sheet_ids):
            errors.append("owner contract requiredSheetIds must not contain blank or duplicate sheet ids.")

    required_cells_payload = payload.get("requiredCells")
    required_cells: list[OwnerRequiredCell] = []
    seen_target_keys: set[str] = set()
    if not isinstance(required_cells_payload, list) or not required_cells_payload:
        errors.append("owner contract requiredCells must be a non-empty list.")
    else:
        for index, raw_cell in enumerate(required_cells_payload):
            owner = f"owner contract requiredCells[{index}]"
            if not isinstance(raw_cell, dict):
                errors.append(f"{owner} must be a mapping.")
                continue
            target_key = str(raw_cell.get("targetKey", "")).strip()
            sheet_id = str(raw_cell.get("sheetId", "")).strip()
            category = str(raw_cell.get("category", "")).strip()
            if not target_key:
                errors.append(f"{owner} targetKey is required.")
            elif target_key in seen_target_keys:
                errors.append(f"Duplicate owner contract targetKey '{target_key}'.")
            seen_target_keys.add(target_key)
            if not sheet_id:
                errors.append(f"{owner} sheetId is required.")
            elif required_sheet_ids and sheet_id not in required_sheet_ids:
                errors.append(f"{owner} sheetId {sheet_id} is not listed in requiredSheetIds.")
            if category not in ALLOWED_CATEGORIES:
                errors.append(f"{owner} category '{category}' is not in the asset pipeline allowlist.")
            if target_key and sheet_id and category:
                required_cells.append(OwnerRequiredCell(target_key=target_key, sheet_id=sheet_id, category=category))

    counts_payload = payload.get("requiredCellCountsBySheet")
    required_counts_by_sheet: dict[str, OwnerSheetCellCounts] = {}
    if not isinstance(counts_payload, dict) or not counts_payload:
        errors.append("owner contract requiredCellCountsBySheet must be a non-empty mapping.")
    else:
        for sheet_id, raw_counts in counts_payload.items():
            sheet_id_text = str(sheet_id).strip()
            owner = f"owner contract requiredCellCountsBySheet[{sheet_id_text}]"
            if required_sheet_ids and sheet_id_text not in required_sheet_ids:
                errors.append(f"{owner} sheetId is not listed in requiredSheetIds.")
            if not isinstance(raw_counts, dict):
                errors.append(f"{owner} must be a mapping.")
                continue
            counts = OwnerSheetCellCounts(
                direct=safe_int(raw_counts.get("direct"), -1),
                alias=safe_int(raw_counts.get("alias"), -1),
                reserved=safe_int(raw_counts.get("reserved"), -1),
                total=safe_int(raw_counts.get("total"), -1),
            )
            if min(counts.direct, counts.alias, counts.reserved, counts.total) < 0:
                errors.append(f"{owner} direct/alias/reserved/total must be non-negative integers.")
            if counts.direct + counts.alias + counts.reserved != counts.total:
                errors.append(f"{owner} direct + alias + reserved must equal total.")
            required_counts_by_sheet[sheet_id_text] = counts

    missing_count_sheet_ids = sorted(set(required_sheet_ids) - set(required_counts_by_sheet))
    if missing_count_sheet_ids:
        errors.append(
            "owner contract requiredCellCountsBySheet must include every requiredSheetIds entry: "
            f"missing={', '.join(missing_count_sheet_ids)}."
        )

    required_direct_counts: dict[str, int] = {}
    for cell in required_cells:
        required_direct_counts[cell.sheet_id] = required_direct_counts.get(cell.sheet_id, 0) + 1
    for sheet_id, counts in required_counts_by_sheet.items():
        if required_direct_counts.get(sheet_id, 0) != counts.direct:
            errors.append(
                f"owner contract requiredCells direct count for {sheet_id} must equal requiredCellCountsBySheet.direct: "
                f"requiredCells={required_direct_counts.get(sheet_id, 0)} direct={counts.direct}."
            )

    if errors:
        return None, errors
    return (
        OwnerContract(
            owner_pr=owner_pr,
            required_sheet_ids=required_sheet_ids,
            required_cells=required_cells,
            required_counts_by_sheet=required_counts_by_sheet,
        ),
        [],
    )


def load_manifest_entries(path: pathlib.Path) -> dict[str, dict[str, Any]]:
    payload = load_json(path)
    entries = payload.get("entries")
    if not isinstance(entries, list):
        return {}
    return {
        str(entry.get("key", "")).strip(): entry
        for entry in entries
        if isinstance(entry, dict) and str(entry.get("key", "")).strip()
    }


def write_json(path: pathlib.Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def print_errors(prefix: str, errors: list[str]) -> int:
    if not errors:
        return 0
    print(f"{prefix} FAILED:")
    for error in errors:
        print(f"- {error}")
    return 1
