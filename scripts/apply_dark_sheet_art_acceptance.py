#!/usr/bin/env python3
"""Promote hash-matched PR06 dark-v1 sheet QA acceptance into sprite map JSONL."""

from __future__ import annotations

import argparse
import json
import pathlib
import subprocess
import sys
from typing import Any


ACCEPTED_DECISIONS = {"PASS", "ACCEPTED", "MANUAL_PASS", "CODEX_VISUAL_CHECKED"}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input-report", type=pathlib.Path, required=True)
    parser.add_argument("--acceptance", type=pathlib.Path, required=True)
    parser.add_argument("--out", type=pathlib.Path, required=True)
    return parser.parse_args()


def print_errors(errors: list[str]) -> int:
    print("dark-sheet-art-acceptance FAILED:", file=sys.stderr)
    for error in errors:
        print(f"- {error}", file=sys.stderr)
    return 1


def load_json(path: pathlib.Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def load_jsonl(path: pathlib.Path) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        if not line.strip():
            continue
        try:
            row = json.loads(line)
        except json.JSONDecodeError as exc:
            raise ValueError(f"{path.as_posix()}:{line_number} is not valid JSON: {exc}") from exc
        if not isinstance(row, dict):
            raise ValueError(f"{path.as_posix()}:{line_number} must be a JSON object.")
        records.append(row)
    return records


def git_hash_object(path: pathlib.Path) -> str:
    return subprocess.check_output(["git", "hash-object", path.as_posix()], text=True).strip()


def acceptance_by_sheet(payload: dict[str, Any]) -> tuple[dict[str, dict[str, Any]], list[str]]:
    errors: list[str] = []
    if payload.get("schemaVersion") != "dark-uiux-pr06-sheet-art-acceptance-v1":
        errors.append("acceptance schemaVersion must be dark-uiux-pr06-sheet-art-acceptance-v1.")
    reviewer = str(payload.get("reviewer", "")).strip()
    reviewed_at = str(payload.get("reviewedAt", "")).strip()
    if not reviewer:
        errors.append("acceptance reviewer is required.")
    if not reviewed_at:
        errors.append("acceptance reviewedAt is required.")
    sheets = payload.get("sheets")
    if not isinstance(sheets, list) or not sheets:
        errors.append("acceptance sheets must be a non-empty list.")
        return {}, errors
    accepted: dict[str, dict[str, Any]] = {}
    for index, sheet in enumerate(sheets):
        if not isinstance(sheet, dict):
            errors.append(f"acceptance sheets[{index}] must be an object.")
            continue
        sheet_id = str(sheet.get("sheetId", "")).strip()
        decision = str(sheet.get("decision", "")).strip()
        raw_sheet_path = str(sheet.get("rawSheetPath", "")).strip()
        raw_sheet_git_hash = str(sheet.get("rawSheetGitHash", "")).strip()
        if not sheet_id:
            errors.append(f"acceptance sheets[{index}] sheetId is required.")
            continue
        if sheet_id in accepted:
            errors.append(f"acceptance sheetId '{sheet_id}' is duplicated.")
        if decision not in ACCEPTED_DECISIONS:
            errors.append(f"acceptance sheet '{sheet_id}' decision must be accepted, got '{decision or '<missing>'}'.")
        if not raw_sheet_path:
            errors.append(f"acceptance sheet '{sheet_id}' rawSheetPath is required.")
        if not raw_sheet_git_hash:
            errors.append(f"acceptance sheet '{sheet_id}' rawSheetGitHash is required.")
        accepted[sheet_id] = sheet
    return accepted, errors


def validate_hashes(accepted: dict[str, dict[str, Any]]) -> list[str]:
    errors: list[str] = []
    for sheet_id, sheet in sorted(accepted.items()):
        raw_sheet_path = pathlib.Path(str(sheet.get("rawSheetPath", "")).strip())
        expected_hash = str(sheet.get("rawSheetGitHash", "")).strip()
        if not raw_sheet_path.is_file():
            errors.append(f"acceptance sheet '{sheet_id}' rawSheetPath is missing: {raw_sheet_path.as_posix()}.")
            continue
        actual_hash = git_hash_object(raw_sheet_path)
        if actual_hash != expected_hash:
            errors.append(
                f"acceptance sheet '{sheet_id}' rawSheetGitHash is stale: "
                f"expected {expected_hash}, actual {actual_hash}."
            )
    return errors


def promote(records: list[dict[str, Any]], accepted: dict[str, dict[str, Any]], payload: dict[str, Any]) -> tuple[list[dict[str, Any]], list[str]]:
    errors: list[str] = []
    reviewer = str(payload["reviewer"]).strip()
    reviewed_at = str(payload["reviewedAt"]).strip()
    promoted: list[dict[str, Any]] = []
    seen_sheets: set[str] = set()
    for index, record in enumerate(records):
        sheet_id = str(record.get("sheetId", "")).strip()
        target_key = str(record.get("targetKey", "")).strip()
        if not sheet_id:
            errors.append(f"input report record {index} is missing sheetId.")
            continue
        seen_sheets.add(sheet_id)
        sheet = accepted.get(sheet_id)
        if sheet is None:
            errors.append(f"input report sheet '{sheet_id}' has no accepted sheet record.")
            continue
        if str(record.get("rawSheetPath", "")).strip() != str(sheet.get("rawSheetPath", "")).strip():
            errors.append(
                f"{target_key or sheet_id} rawSheetPath mismatch: "
                f"report={record.get('rawSheetPath')} acceptance={sheet.get('rawSheetPath')}."
            )
            continue
        row = dict(record)
        row["qaStatus"] = "ACCEPTED"
        row["reviewer"] = reviewer
        row["reviewedAt"] = reviewed_at
        row["rejectionReason"] = None
        promoted.append(row)
    return promoted, errors


def main() -> int:
    args = parse_args()
    try:
        records = load_jsonl(args.input_report)
        payload = load_json(args.acceptance)
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        return print_errors([str(exc)])
    if not isinstance(payload, dict):
        return print_errors(["acceptance root must be a JSON object."])

    accepted, errors = acceptance_by_sheet(payload)
    if not errors:
        errors += validate_hashes(accepted)
    promoted: list[dict[str, Any]] = []
    if not errors:
        promoted, errors = promote(records, accepted, payload)
    if errors:
        return print_errors(errors)

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text("".join(json.dumps(record, sort_keys=True) + "\n" for record in promoted), encoding="utf-8")
    print(f"dark-sheet-art-acceptance OK: acceptedSheets={len(accepted)}, records={len(promoted)}, out={args.out.as_posix()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
