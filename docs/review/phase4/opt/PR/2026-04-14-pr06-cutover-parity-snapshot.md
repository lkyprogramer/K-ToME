# 2026-04-14 PR-06 Cutover Parity Snapshot

- target PR doc: `docs/opt/pr/2026-04-12-unified-verification-pr-06-phase4-gate-cutover-alias-stabilization-and-legacy-fallback-hardening.md`
- snapshot date: `2026-04-14`
- purpose: provide a concrete parity evidence anchor for the PR-06 cutover decision, so later regressions can be distinguished from the original cutover state

## Executed Commands

```bash
./gradlew :tools:phase4LegacyReport
./gradlew :tools:reportPhase4
```

## Canonical Aggregate Snapshot

- artifact: `tools/build/reports/verification/phase4/report-phase4-summary.json`
- `phaseVerdict`: `PASS`
- `inputCount`: `14`
- `failedTaskCount`: `0`
- `ownerMetricCount`: `9`
- `unexpectedRegressionCount`: `0`
- `approvedDebtCount`: `0`
- `expectedFailureCount`: `0`
- `improvedDebtCount`: `0`
- `domainCacheHitRate`: `1.0`
- `artifactReuseRate`: `1.0`
- `slowestDomain`: `mapgenSmoke`

## Legacy Parity Snapshot

- artifact: `tools/build/reports/verification/phase4/report-phase4-legacy-comparison.json`
- `legacySummaryPath`: `tools/build/reports/phase4/phase4-summary.json`
- `metricCount`: `9`
- `mismatchCount`: `0`
- `mismatches`: `[]`

## Owner Metric Set

1. `scriptedHiddenVerificationRate`
2. `organicHiddenDiscoveryRate`
3. `sameZoneSecretVsCadenceMaxOverlap`
4. `sameZoneSecretVsRewardMaxOverlap`
5. `terminalWeaponBaseDiversity`
6. `crossProfessionTopWeaponDominance`
7. `professionAlignedWeaponAdoptionRate`
8. `terrainInteractionEncounterRate.aggregate`
9. `terrainInteractionEncounterRate.per_zone_lower_bound`

## Interpretation

1. PR-06 cutover state already had `0` parity mismatches across the `9` frozen Phase 4 owner metrics.
2. The canonical aggregate was green at the time of the snapshot; this document can be used as the baseline evidence anchor if a later follow-up PR causes `reportPhase4` parity to fail.
3. This snapshot does not replace the formal gate; it records the evidence that the gate was green when the cutover state was reviewed.
