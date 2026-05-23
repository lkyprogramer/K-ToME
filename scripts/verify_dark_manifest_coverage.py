#!/usr/bin/env python3
"""Write and validate dark-v1 manifest coverage artifacts."""

from __future__ import annotations

import argparse
import pathlib
import re
from datetime import datetime, timezone
from typing import Any

from asset_pipeline_common import load_json
from dark_sprite_sheet_contract import (
    DARK_RUNTIME_PREFIX,
    PENDING_RAW_OUTPUT,
    STYLE_TAG,
    load_key_registry,
    load_manifest_entries,
    load_owner_contract,
    load_sheet_plan,
    print_errors,
    repo_relative_error,
    sha256_file,
    write_json,
)

FINAL_FULL_INVENTORY_SCHEMA_VERSION = "dark-v1-final-full-inventory-v1"
OWNER_PR_PATTERN = re.compile(r"^PR-\d{2}(?:-\d+)?$")
ART_RANDOM_QA_SCHEMA_VERSION = "dark-art-random-qa-v1"
ART_RANDOM_QA_ACCEPTED_DECISIONS = {"PASS", "ACCEPTED", "CODEX_VISUAL_CHECKED", "MANUAL_PASS"}


def raw_output_path(entry: dict[str, Any] | None) -> str:
    if entry is None:
        return ""
    return str(entry.get("rawOutputPath", "")).strip()


def output_state(entry: dict[str, Any] | None) -> str:
    if entry is None:
        return "missing"
    raw_path = raw_output_path(entry)
    if raw_path in ("", PENDING_RAW_OUTPUT):
        return "pending"
    if raw_path.startswith(DARK_RUNTIME_PREFIX):
        return "dark"
    return "old-style"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate dark-v1 manifest coverage by mode.")
    parser.add_argument("--coverage-mode", choices=("pr00-dry-run", "owner-scope", "final-full"), default="final-full")
    parser.add_argument("--owner-pr", default="")
    parser.add_argument("--required-owner-sheet-ids", default="")
    parser.add_argument("--owner-contract", type=pathlib.Path, default=None)
    parser.add_argument("--plan", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/sheet-plan.yaml"))
    parser.add_argument("--registry", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/key-registry.yaml"))
    parser.add_argument("--manifest", type=pathlib.Path, default=pathlib.Path("assets-src/image/manifests/phase2-visual-manifest.json"))
    parser.add_argument("--runtime-manifest", type=pathlib.Path, default=pathlib.Path("client/src/main/resources/manifests/visual-manifest.json"))
    parser.add_argument("--expected-inventory", type=pathlib.Path, default=None)
    parser.add_argument("--art-random-qa-record", type=pathlib.Path, default=None)
    parser.add_argument("--packaged-sentinel-evidence", default="")
    parser.add_argument("--report", type=pathlib.Path, default=pathlib.Path("build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json"))
    return parser.parse_args()


def parse_required_sheet_ids(raw_value: str) -> list[str]:
    return sorted(
        sheet_id
        for sheet_id in (part.strip() for part in raw_value.split(","))
        if sheet_id
    )


def is_dark_output(entry: dict[str, Any] | None) -> bool:
    return output_state(entry) == "dark"


def is_pending_output(entry: dict[str, Any] | None) -> bool:
    return output_state(entry) == "pending"


def is_old_style_output(entry: dict[str, Any] | None) -> bool:
    return output_state(entry) == "old-style"


def key_counts_by_sheet(keys: list[str], registry_by_key: dict[str, dict[str, Any]]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for key in keys:
        sheet_id = str(registry_by_key.get(key, {}).get("sheetId", "")).strip()
        if not sheet_id:
            continue
        counts[sheet_id] = counts.get(sheet_id, 0) + 1
    return dict(sorted(counts.items()))


def registry_only_alias_keys(
    keys: list[str],
    registry_by_key: dict[str, dict[str, Any]],
    cells_by_key: dict[str, Any],
) -> list[str]:
    return sorted(
        key
        for key in keys
        if key not in cells_by_key and str(registry_by_key.get(key, {}).get("aliasOf", "")).strip()
    )


def repo_relative_path(path: pathlib.Path) -> str | None:
    try:
        return path.resolve().relative_to(pathlib.Path.cwd().resolve()).as_posix()
    except ValueError:
        if not path.is_absolute():
            return path.as_posix()
        return None


def stable_source_digest(path: pathlib.Path) -> str:
    return sha256_file(path)


def inventory_required_source_paths(args: argparse.Namespace) -> list[str]:
    paths = [
        args.manifest,
        args.runtime_manifest,
        args.registry,
        args.plan,
        pathlib.Path("UI/pr/screen-coverage-matrix.md"),
        pathlib.Path("UI/sprite-sheets/dark-v1-pr06-handoff-inventory.json"),
    ]
    return sorted({relative for path in paths if (relative := repo_relative_path(path)) is not None})


def validate_coverage_exclusion(
    value: Any,
    key: str,
    family: str,
    registry_by_key: dict[str, dict[str, Any]],
) -> tuple[dict[str, str] | None, list[str]]:
    errors: list[str] = []
    if value is None:
        return None, errors
    if not isinstance(value, dict):
        return None, [f"inventory coverageExclusion for {key} must be an object or null."]
    required_fields = ("key", "family", "reason", "visibleFallbackKey", "evidencePath", "removalOwner", "expiresAfterPr")
    normalized: dict[str, str] = {}
    for field in required_fields:
        raw = value.get(field)
        if not isinstance(raw, str) or not raw.strip():
            errors.append(f"inventory coverageExclusion for {key} must define non-empty {field}.")
        else:
            normalized[field] = raw.strip()
    if normalized.get("key") != key:
        errors.append(f"inventory coverageExclusion key mismatch for {key}: {normalized.get('key', '<missing>')}.")
    if normalized.get("family") != family:
        errors.append(f"inventory coverageExclusion family mismatch for {key}: {normalized.get('family', '<missing>')}.")
    visible_fallback_key = normalized.get("visibleFallbackKey")
    if visible_fallback_key and visible_fallback_key not in registry_by_key:
        errors.append(f"inventory coverageExclusion for {key} references unknown visibleFallbackKey {visible_fallback_key}.")
    evidence_path = normalized.get("evidencePath")
    if evidence_path:
        error = repo_relative_error(evidence_path, "coverageExclusion.evidencePath", f"inventory key {key}")
        if error:
            errors.append(error)
    expires_after_pr = normalized.get("expiresAfterPr")
    if expires_after_pr and not OWNER_PR_PATTERN.match(expires_after_pr):
        errors.append(f"inventory coverageExclusion for {key} expiresAfterPr must match PR-##, got {expires_after_pr}.")
    return normalized if not errors else None, errors


def load_expected_inventory(
    path: pathlib.Path | None,
    args: argparse.Namespace,
    registry_by_key: dict[str, dict[str, Any]],
) -> tuple[list[str] | None, list[dict[str, str]], dict[str, Any], list[str]]:
    if path is None:
        return None, [], {}, []

    payload = load_json(path)
    errors: list[str] = []
    if payload.get("schemaVersion") != FINAL_FULL_INVENTORY_SCHEMA_VERSION:
        errors.append(
            "final-full inventory schemaVersion must be "
            f"{FINAL_FULL_INVENTORY_SCHEMA_VERSION}, got {payload.get('schemaVersion', '<missing>')}."
        )
    if payload.get("schemaOwner") != "PR-06":
        errors.append("final-full inventory schemaOwner must be PR-06.")
    generated_from = payload.get("generatedFrom")
    if not isinstance(generated_from, list) or not generated_from:
        errors.append("final-full inventory generatedFrom must be a non-empty list.")
    else:
        for source_path in generated_from:
            if not isinstance(source_path, str):
                errors.append("final-full inventory generatedFrom entries must be strings.")
                continue
            error = repo_relative_error(source_path, "generatedFrom", "final-full inventory")
            if error:
                errors.append(error)

    source_digests = payload.get("sourceDigests")
    if not isinstance(source_digests, dict) or not source_digests:
        errors.append("final-full inventory sourceDigests must be a non-empty object.")
        source_digests = {}
    else:
        for source_path, expected_digest in sorted(source_digests.items()):
            if not isinstance(source_path, str) or not isinstance(expected_digest, str) or not expected_digest:
                errors.append("final-full inventory sourceDigests entries must be non-empty string pairs.")
                continue
            error = repo_relative_error(source_path, "sourceDigests", "final-full inventory")
            if error:
                errors.append(error)
                continue
            actual_path = pathlib.Path(source_path)
            if not actual_path.exists():
                errors.append(f"final-full inventory source digest path does not exist: {source_path}.")
                continue
            actual_digest = stable_source_digest(actual_path)
            if actual_digest != expected_digest:
                errors.append(
                    f"final-full inventory source digest is stale for {source_path}: "
                    f"expected={expected_digest} actual={actual_digest}."
                )
    for required_path in inventory_required_source_paths(args):
        if pathlib.Path(required_path).exists() and required_path not in source_digests:
            errors.append(f"final-full inventory sourceDigests missing required source {required_path}.")

    expected_keys: list[str] = []
    seen_keys: set[str] = set()
    exclusions: list[dict[str, str]] = []
    families_payload = payload.get("families")
    if not isinstance(families_payload, list) or not families_payload:
        errors.append("final-full inventory families must be a non-empty list.")
    else:
        for family_index, family_payload in enumerate(families_payload):
            if not isinstance(family_payload, dict):
                errors.append(f"final-full inventory families[{family_index}] must be an object.")
                continue
            family = str(family_payload.get("family", "")).strip()
            if not family:
                errors.append(f"final-full inventory families[{family_index}].family is required.")
            expected_count = family_payload.get("expectedCount")
            keys_payload = family_payload.get("keys")
            if not isinstance(keys_payload, list):
                errors.append(f"final-full inventory family {family or family_index} keys must be a list.")
                continue
            if expected_count != len(keys_payload):
                errors.append(
                    f"final-full inventory family {family or family_index} expectedCount "
                    f"{expected_count} does not match key count {len(keys_payload)}."
                )
            historical_sheet_ids = family_payload.get("historicalSheetIds", [])
            if historical_sheet_ids is not None and (
                not isinstance(historical_sheet_ids, list)
                or any(not isinstance(sheet_id, str) or not sheet_id.strip() for sheet_id in historical_sheet_ids)
            ):
                errors.append(f"final-full inventory family {family or family_index} historicalSheetIds must be a string list.")
            for key_index, key_payload in enumerate(keys_payload):
                if not isinstance(key_payload, dict):
                    errors.append(f"final-full inventory family {family or family_index} key[{key_index}] must be an object.")
                    continue
                key = str(key_payload.get("key", "")).strip()
                if not key:
                    errors.append(f"final-full inventory family {family or family_index} key[{key_index}].key is required.")
                    continue
                if key in seen_keys:
                    errors.append(f"final-full inventory contains duplicate key {key}.")
                    continue
                seen_keys.add(key)
                expected_keys.append(key)
                for field_name in ("consumer", "consumerTest"):
                    if not str(key_payload.get(field_name, "")).strip():
                        errors.append(f"final-full inventory key {key} must define {field_name}.")
                historical_key_sheet_ids = key_payload.get("historicalSheetIds", [])
                if historical_key_sheet_ids is not None and (
                    not isinstance(historical_key_sheet_ids, list)
                    or any(not isinstance(sheet_id, str) or not sheet_id.strip() for sheet_id in historical_key_sheet_ids)
                ):
                    errors.append(f"final-full inventory key {key} historicalSheetIds must be a string list.")
                manual_override = key_payload.get("manualOverrideReason")
                if manual_override is not None and (not isinstance(manual_override, str) or not manual_override.strip()):
                    errors.append(f"final-full inventory key {key} manualOverrideReason must be a non-empty string when present.")
                exclusion, exclusion_errors = validate_coverage_exclusion(
                    key_payload.get("coverageExclusion"),
                    key,
                    family,
                    registry_by_key,
                )
                errors += exclusion_errors
                if exclusion is not None:
                    exclusions.append(exclusion)

    missing_registry_keys = sorted(key for key in expected_keys if key not in registry_by_key)
    if missing_registry_keys:
        errors.append(f"final-full inventory keys missing from key registry: {', '.join(missing_registry_keys)}.")
    return sorted(expected_keys), sorted(exclusions, key=lambda item: item["key"]), payload, errors


def parse_packaged_sentinel_evidence(raw_value: str) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    paths = sorted({part.strip() for part in raw_value.split(",") if part.strip()})
    for path in paths:
        error = repo_relative_error(path, "packagedSentinelEvidence", "dark-manifest-coverage-lint")
        if error:
            errors.append(error)
        elif not pathlib.Path(path).exists():
            errors.append(f"packaged sentinel evidence path does not exist: {path}.")
    return paths, errors


def audit_packaged_sentinel(paths: list[str]) -> tuple[str, list[dict[str, Any]], list[str]]:
    if not paths:
        return "deferred-to-pr07", [], []
    hits: list[dict[str, Any]] = []
    errors: list[str] = []
    for path in paths:
        try:
            text = pathlib.Path(path).read_text(encoding="utf-8")
        except UnicodeDecodeError:
            errors.append(f"packaged sentinel evidence must be UTF-8 text: {path}.")
            continue
        for line_number, line in enumerate(text.splitlines(), start=1):
            if "missing_visual" in line:
                hits.append({"path": path, "line": line_number})
    if hits:
        errors.append("packaged sentinel evidence references missing_visual.")
        return "fail", hits, errors
    if errors:
        return "fail", hits, errors
    return "pass", hits, []


def validate_art_random_qa_record(path: pathlib.Path | None) -> tuple[list[dict[str, Any]], list[str]]:
    if path is None:
        return [], []
    errors: list[str] = []
    error = repo_relative_error(path.as_posix(), "artRandomQaRecord", "dark-manifest-coverage-lint")
    if error:
        errors.append(error)
    if not path.is_file():
        return [], errors + [f"art random QA record path does not exist: {path.as_posix()}."]
    payload = load_json(path)
    if payload.get("schemaVersion") != ART_RANDOM_QA_SCHEMA_VERSION:
        errors.append(
            "art random QA record schemaVersion must be "
            f"{ART_RANDOM_QA_SCHEMA_VERSION}, got {payload.get('schemaVersion', '<missing>')}."
        )
    sheets = payload.get("sheets")
    if not isinstance(sheets, list):
        errors.append("art random QA record must define a sheets list.")
        return [], errors
    pending_samples: list[dict[str, Any]] = []
    for sheet in sheets:
        if not isinstance(sheet, dict):
            errors.append("art random QA record sheets entries must be objects.")
            continue
        sheet_id = str(sheet.get("sheetId", "")).strip()
        samples = sheet.get("samples")
        if not isinstance(samples, list):
            errors.append(f"art random QA record sheet {sheet_id or '<missing>'} must define samples.")
            continue
        for sample in samples:
            if not isinstance(sample, dict):
                errors.append(f"art random QA record sheet {sheet_id or '<missing>'} sample must be an object.")
                continue
            decision = str(sample.get("qaDecision", "")).strip()
            reject_reason = sample.get("rejectReason")
            if decision not in ART_RANDOM_QA_ACCEPTED_DECISIONS or reject_reason:
                pending_samples.append(
                    {
                        "sheetId": sheet_id,
                        "targetKey": str(sample.get("targetKey", "")).strip(),
                        "selection": str(sample.get("selection", "")).strip(),
                        "qaDecision": decision or "<missing>",
                        "rejectReason": reject_reason,
                    }
                )
    return pending_samples, errors


def build_coverage(args: argparse.Namespace) -> tuple[dict[str, Any], list[str]]:
    _, cells, plan_errors = load_sheet_plan(args.plan)
    registry_by_key, registry_errors = load_key_registry(args.registry)
    manifest_by_key = load_manifest_entries(args.manifest)
    runtime_by_key = load_manifest_entries(args.runtime_manifest)
    errors = plan_errors + registry_errors
    cells_by_key = {cell.target_key: cell for cell in cells}
    packaged_sentinel_paths, packaged_sentinel_path_errors = parse_packaged_sentinel_evidence(
        args.packaged_sentinel_evidence
    )
    errors += packaged_sentinel_path_errors
    packaged_sentinel_status, packaged_missing_visual_hits, packaged_sentinel_errors = audit_packaged_sentinel(
        packaged_sentinel_paths
    )
    errors += packaged_sentinel_errors
    art_random_qa_pending_samples, art_random_qa_errors = validate_art_random_qa_record(args.art_random_qa_record)
    errors += art_random_qa_errors

    if args.coverage_mode == "owner-scope" and not args.owner_pr:
        errors.append("owner-scope coverage requires --owner-pr.")
    if args.coverage_mode != "owner-scope" and args.owner_pr:
        errors.append(f"{args.coverage_mode} coverage must not set --owner-pr.")
    required_owner_sheet_ids = parse_required_sheet_ids(args.required_owner_sheet_ids)
    if args.coverage_mode != "owner-scope" and required_owner_sheet_ids:
        errors.append(f"{args.coverage_mode} coverage must not set --required-owner-sheet-ids.")
    owner_contract = None
    if args.owner_contract is not None:
        if args.coverage_mode != "owner-scope":
            errors.append(f"{args.coverage_mode} coverage must not set --owner-contract.")
        owner_contract, contract_errors = load_owner_contract(args.owner_contract)
        errors += contract_errors
        if owner_contract is not None:
            if args.owner_pr and owner_contract.owner_pr != args.owner_pr:
                errors.append(
                    f"owner contract ownerPr mismatch: contract={owner_contract.owner_pr} requested={args.owner_pr}."
                )
            contract_sheet_ids = owner_contract.required_sheet_ids
            if required_owner_sheet_ids and required_owner_sheet_ids != contract_sheet_ids:
                errors.append(
                    "required owner sheet ids mismatch owner contract: "
                    f"argument={', '.join(required_owner_sheet_ids)} contract={', '.join(contract_sheet_ids)}."
                )
            required_owner_sheet_ids = contract_sheet_ids

    registry_keys = sorted(registry_by_key)
    expected_inventory_keys: list[str] | None = None
    allowed_coverage_exclusions: list[dict[str, str]] = []
    inventory_payload: dict[str, Any] = {}
    if args.expected_inventory is not None:
        if args.coverage_mode != "final-full":
            errors.append(f"{args.coverage_mode} coverage must not set --expected-inventory.")
        expected_inventory_keys, allowed_coverage_exclusions, inventory_payload, inventory_errors = load_expected_inventory(
            args.expected_inventory,
            args,
            registry_by_key,
        )
        errors += inventory_errors
    elif args.coverage_mode == "final-full":
        errors.append("final-full coverage requires --expected-inventory.")

    if args.coverage_mode == "owner-scope":
        expected_keys = sorted(
            target_key
            for target_key, entry in registry_by_key.items()
            if str(entry.get("ownerPr", "")).strip() == args.owner_pr
        )
        if args.owner_pr and not expected_keys:
            errors.append(
                "owner-scope coverage found no expected keys for "
                f"{args.owner_pr} from {args.registry.as_posix()}."
            )
    elif args.coverage_mode == "final-full" and expected_inventory_keys is not None:
        expected_keys = expected_inventory_keys
    else:
        expected_keys = registry_keys

    allowed_excluded_keys = sorted({exclusion["key"] for exclusion in allowed_coverage_exclusions})
    linted_expected_keys = sorted(set(expected_keys) - set(allowed_excluded_keys))
    canonical_state_by_key = {key: output_state(manifest_by_key.get(key)) for key in expected_keys}
    runtime_state_by_key = {key: output_state(runtime_by_key.get(key)) for key in expected_keys}
    missing_keys = sorted(
        key
        for key in linted_expected_keys
        if canonical_state_by_key[key] == "missing" or runtime_state_by_key[key] == "missing"
    )
    pending_keys = sorted(
        key
        for key in linted_expected_keys
        if canonical_state_by_key[key] == "pending" or runtime_state_by_key[key] == "pending"
    )
    old_style_keys = sorted(
        key
        for key in linted_expected_keys
        if canonical_state_by_key[key] == "old-style" or runtime_state_by_key[key] == "old-style"
    )
    covered_keys = sorted(
        key
        for key in linted_expected_keys
        if canonical_state_by_key[key] == "dark" and runtime_state_by_key[key] == "dark"
    )

    if args.coverage_mode == "owner-scope" and missing_keys:
        errors.append(f"owner-scope missing keys for {args.owner_pr}: {', '.join(missing_keys)}.")
    if args.coverage_mode == "owner-scope" and pending_keys:
        errors.append(f"owner-scope pending keys for {args.owner_pr}: {', '.join(pending_keys)}.")
    if args.coverage_mode == "owner-scope" and old_style_keys:
        errors.append(f"owner-scope old-style keys for {args.owner_pr}: {', '.join(old_style_keys)}.")

    required_owner_keys: list[str] = []
    owner_missing_required_keys: list[str] = []
    owner_unexpected_keys: list[str] = []
    required_owner_key_count_by_sheet: dict[str, int] = {}
    owner_expected_key_count_by_sheet: dict[str, int] = {}
    alias_only_keys = registry_only_alias_keys(expected_keys, registry_by_key, cells_by_key)
    if args.coverage_mode == "owner-scope" and owner_contract is not None:
        required_owner_keys = sorted(cell.target_key for cell in owner_contract.required_cells)
        owner_missing_required_keys = sorted(set(required_owner_keys) - set(expected_keys))
        owner_unexpected_keys = sorted(set(expected_keys) - set(required_owner_keys) - set(alias_only_keys))
        required_owner_key_count_by_sheet = dict(
            sorted(
                {
                    sheet_id: len([cell for cell in owner_contract.required_cells if cell.sheet_id == sheet_id])
                    for sheet_id in owner_contract.required_sheet_ids
                }.items()
            )
        )
        owner_expected_key_count_by_sheet = key_counts_by_sheet(expected_keys, registry_by_key)
        if owner_missing_required_keys:
            errors.append(
                f"owner-scope missing required owner keys for {args.owner_pr}: "
                f"{', '.join(owner_missing_required_keys)}."
            )
        if owner_unexpected_keys:
            errors.append(
                f"owner-scope unexpected owner keys for {args.owner_pr}: {', '.join(owner_unexpected_keys)}."
            )
    if args.coverage_mode == "final-full":
        if missing_keys:
            errors.append(f"final-full missing keys: {', '.join(missing_keys)}.")
        if pending_keys:
            errors.append(f"final-full pendingOrRejectedPlayerVisibleCells are not empty: {', '.join(pending_keys)}.")
        if old_style_keys:
            errors.append(f"final-full oldStylePlayerVisibleKeys are not empty: {', '.join(old_style_keys)}.")
        if art_random_qa_pending_samples:
            preview = ", ".join(
                f"{sample['sheetId']}:{sample['targetKey']}={sample['qaDecision']}"
                for sample in art_random_qa_pending_samples[:12]
            )
            suffix = "" if len(art_random_qa_pending_samples) <= 12 else f", ... total={len(art_random_qa_pending_samples)}"
            errors.append(f"final-full artRandomQaPendingSamples are not empty: {preview}{suffix}.")

    common: dict[str, Any] = {
        "schemaVersion": "dark-v1-manifest-coverage-v1",
        "styleTag": STYLE_TAG,
        "scopeMode": args.coverage_mode,
        "ownerPr": args.owner_pr or None,
        "expectedKeySetSource": args.expected_inventory.as_posix() if args.coverage_mode == "final-full" and args.expected_inventory else args.registry.as_posix(),
        "strictOldStyleResidue": args.coverage_mode == "final-full",
        "generatedAt": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
        "sourceManifestPath": args.manifest.as_posix(),
        "runtimeManifestPath": args.runtime_manifest.as_posix(),
        "keyRegistryPath": args.registry.as_posix(),
        "sheetPlanPath": args.plan.as_posix(),
        "expectedInventoryPath": args.expected_inventory.as_posix() if args.expected_inventory else None,
        "expectedInventoryGeneratedBy": inventory_payload.get("generatedBy") if inventory_payload else None,
        "packagedSentinelEvidencePaths": packaged_sentinel_paths,
        "packagedMissingVisualHits": packaged_missing_visual_hits,
        "packagedSentinelAuditStatus": packaged_sentinel_status,
        "artRandomQaRecordPath": args.art_random_qa_record.as_posix() if args.art_random_qa_record else None,
        "artRandomQaPendingSamples": art_random_qa_pending_samples,
    }
    if args.coverage_mode == "owner-scope":
        owner_sheet_ids = sorted(
            {
                str(registry_by_key[key].get("sheetId", "")).strip()
                for key in expected_keys
                if key not in alias_only_keys
            }
        )
        missing_required_sheet_ids = sorted(set(required_owner_sheet_ids) - set(owner_sheet_ids))
        if missing_required_sheet_ids:
            errors.append(
                "owner-scope missing required sheet ids for "
                f"{args.owner_pr}: required={', '.join(required_owner_sheet_ids)}, "
                f"actual={', '.join(owner_sheet_ids) if owner_sheet_ids else '<empty>'}, "
                f"missing={', '.join(missing_required_sheet_ids)}."
            )
        common.update(
            {
                "ownerSheetIds": owner_sheet_ids,
                "requiredOwnerSheetIds": required_owner_sheet_ids,
                "requiredOwnerContractPath": args.owner_contract.as_posix() if args.owner_contract else None,
                "requiredOwnerKeys": required_owner_keys,
                "ownerExpectedKeys": expected_keys,
                "ownerCoveredKeys": covered_keys,
                "ownerMissingKeys": missing_keys,
                "ownerMissingRequiredKeys": owner_missing_required_keys,
                "ownerUnexpectedKeys": owner_unexpected_keys,
                "ownerAliasOnlyKeys": alias_only_keys,
                "requiredOwnerKeyCountBySheet": required_owner_key_count_by_sheet,
                "ownerExpectedKeyCountBySheet": owner_expected_key_count_by_sheet,
                "ownerPendingKeys": pending_keys,
                "ownerOldStyleKeys": old_style_keys,
                "scopeExternalPendingKeys": sorted(
                    key
                    for key in set(registry_keys) - set(expected_keys)
                    if is_pending_output(manifest_by_key.get(key)) or is_pending_output(runtime_by_key.get(key))
                ),
                "allowedOwnerFallbackKeys": pending_keys,
            }
        )
    else:
        common.update(
            {
                "expectedKeySet": expected_keys,
                "coveredKeySet": covered_keys,
                "missingKeys": missing_keys,
                "oldStylePlayerVisibleKeys": old_style_keys if args.coverage_mode == "final-full" else [],
                "pendingOrRejectedPlayerVisibleCells": pending_keys,
                "fallbackKeyUsage": {
                    key: str(registry_by_key.get(key, {}).get("fallbackKey", "")).strip()
                    for key in linted_expected_keys
                    if is_pending_output(manifest_by_key.get(key)) or is_pending_output(runtime_by_key.get(key))
                },
                "allowedFallbackKeys": ["missing_visual"] if args.coverage_mode == "pr00-dry-run" else [],
                "allowedCoverageExclusions": allowed_coverage_exclusions,
                "allowedExcludedKeySet": allowed_excluded_keys,
                "sourceSheetIds": sorted({cell.sheet_id for cell in cells}),
            }
        )
    common["status"] = "FAIL" if errors else "PASS"
    common["errors"] = errors
    return common, errors


def main() -> int:
    args = parse_args()
    coverage, errors = build_coverage(args)
    write_json(args.report, coverage)
    if errors:
        return print_errors("dark-manifest-coverage-lint", errors)
    print(
        "dark-manifest-coverage-lint OK: "
        f"mode={args.coverage_mode}, expectedKeys={len(coverage.get('expectedKeySet', coverage.get('ownerExpectedKeys', [])))}, report={args.report.as_posix()}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
