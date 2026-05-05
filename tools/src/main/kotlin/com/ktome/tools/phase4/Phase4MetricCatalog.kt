package com.ktome.tools.phase4

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal data class Phase4MetricSpec(
    val id: String,
    val ownerTaskId: String,
    val outputSection: String,
    val formula: String,
    val failSemantics: String,
    val decisionNotes: String,
)

@Serializable
internal data class Phase4MetricCatalogEntry(
    @SerialName("metricId")
    val id: String,
    val ownerTaskId: String,
    val sourcePath: String,
    val outputSection: String,
    val formula: String,
    @SerialName("target")
    val targetText: String,
    val failSemantics: String,
    val decisionNotes: String,
)

internal object Phase4MetricCatalog {
    val specs: List<Phase4MetricSpec> =
        listOf(
            Phase4MetricSpec(
                id = "scriptedHiddenVerificationRate",
                ownerTaskId = "hiddenContentHarness",
                outputSection = "scripted-vs-organic-hidden",
                formula = "(totalCases - failureCount) / totalCases",
                failSemantics = "FAIL means scripted correctness, reveal, reward, or return-bridge verification is broken.",
                decisionNotes = "This is a scripted correctness owner metric and must never be interpreted as organic experience evidence.",
            ),
            Phase4MetricSpec(
                id = "leadDiscoveryRate",
                ownerTaskId = "organicHiddenProbe",
                outputSection = "scripted-vs-organic-hidden",
                formula = "leadDiscoveryCount / totalCases",
                failSemantics = "FAIL is allowed in the first owner-metric pass; it exposes missing lead discovery rather than scripted correctness breakage.",
                decisionNotes = "Lead discovery only counts revealed entrances or actual secret-zone entry; firstHiddenDiscoveryTurn and discovery distributions are supporting evidence, not substitute headline success signals.",
            ),
            Phase4MetricSpec(
                id = "secretConversionRate",
                ownerTaskId = "organicHiddenProbe",
                outputSection = "scripted-vs-organic-hidden",
                formula = "secretConversionCount / leadDiscoveryCount",
                failSemantics = "FAIL means the organic probe can see hidden leads but still does not convert them into real secret-zone entry often enough.",
                decisionNotes = "The report must also surface per-zone secret-entry failures and supporting timing evidence so zones like abyssal_temple cannot hide behind aggregate discovery.",
            ),
            Phase4MetricSpec(
                id = "topZoneLeadShare",
                ownerTaskId = "organicHiddenProbe",
                outputSection = "scripted-vs-organic-hidden",
                formula = "max(non-greenwood per-zone leadDiscoveryCount) / sum(non-greenwood per-zone leadDiscoveryCount)",
                failSemantics = "FAIL means organic lead discovery is collapsing into one non-onramp zone instead of being distributed across mandatory zones.",
                decisionNotes = "greenwood_fringe is intentionally excluded from the top-share denominator but remains required to prove a Search-driven onramp path.",
            ),
            Phase4MetricSpec(
                id = "zoneSearchPromptVisibility",
                ownerTaskId = "hiddenContentHarness",
                outputSection = "scripted-vs-organic-hidden",
                formula = "secret-bearing zones with at least one scripted non-flavor Search prompt case / secret-bearing zone count",
                failSemantics = "FAIL means at least one mandatory secret-bearing zone can ship without a concrete Search affordance case.",
                decisionNotes = "This is scripted per-zone prompt coverage from hiddenContentHarness; organic prompt visibility remains supporting evidence only.",
            ),
            Phase4MetricSpec(
                id = "perZoneSecretConversionFloor.reportOnly",
                ownerTaskId = "organicHiddenProbe",
                outputSection = "scripted-vs-organic-hidden",
                formula = "per-zone secretZoneEntryCount / per-zone leadDiscoveryCount",
                failSemantics = "Report-only in PR04; it must be visible but must not be used to loosen blocking owner gates.",
                decisionNotes = "This field preserves per-zone conversion evidence for the follow-up floor without hiding low-sample zones behind aggregate conversion.",
            ),
            Phase4MetricSpec(
                id = "secretZoneSearchConversionFloor.reportOnly",
                ownerTaskId = "organicHiddenProbe",
                outputSection = "scripted-vs-organic-hidden",
                formula = "per-zone secretZoneEntryCount / per-zone searchRevealCount",
                failSemantics = "Report-only in PR04; low values identify zones where explicit Search reveals do not convert into entry.",
                decisionNotes = "This is anchored to Search reveals, not all lead discovery, so it catches entrance interaction and pathing breakage.",
            ),
            Phase4MetricSpec(
                id = "perZoneSearchUseFloor.reportOnly",
                ownerTaskId = "organicHiddenProbe",
                outputSection = "scripted-vs-organic-hidden",
                formula = "per-zone organic runs with accepted Search / per-zone organic runs",
                failSemantics = "Report-only in PR04; low values expose missing or badly placed Search prompts.",
                decisionNotes = "This remains supporting evidence until the organic bot and prompt tuning have enough coverage history.",
            ),
            Phase4MetricSpec(
                id = "slagCueDensityPerEligibleRoom.reportOnly",
                ownerTaskId = "organicHiddenProbe",
                outputSection = "scripted-vs-organic-hidden",
                formula = "deep_iron_pit slag cue density per eligible room",
                failSemantics = "Report-only in PR04; it must surface slag cue availability without gating the owner report.",
                decisionNotes = "The floor guards against deep_iron_pit losing ore/slag prompt support while Search adoption is still being tuned.",
            ),
            Phase4MetricSpec(
                id = "sameZoneSecretVsCadenceMaxOverlap",
                ownerTaskId = "whiteBoxLoot",
                outputSection = "local-reward-identity",
                formula = "max(overlap(secretProfile, sameZoneCadenceProfile))",
                failSemantics = "FAIL means same-zone secret rewards are too similar to cadence rewards and local identity is being erased.",
                decisionNotes = "Only same-zone secret vs cadence pairs count; cross-zone averages are not valid evidence.",
            ),
            Phase4MetricSpec(
                id = "sameZoneSecretVsRewardMaxOverlap",
                ownerTaskId = "whiteBoxLoot",
                outputSection = "local-reward-identity",
                formula = "max(overlap(secretProfile, sameZoneRewardProfile))",
                failSemantics = "FAIL means same-zone secret rewards collapse into normal reward channels despite corpus-average separation.",
                decisionNotes = "This guardrail exists specifically to catch high-value local reward collisions hidden by corpus averages.",
            ),
            Phase4MetricSpec(
                id = "dynamicPoolCoverage",
                ownerTaskId = "whiteBoxLoot",
                outputSection = "local-reward-identity",
                formula = "dynamicPoolTargetProfileCount / 10",
                failSemantics = "FAIL means at least one PR-02 target profile still ships as a fixed list instead of a dynamic pool.",
                decisionNotes = "The denominator is frozen to the ten PR-02 target profiles; partial coverage is not acceptable evidence.",
            ),
            Phase4MetricSpec(
                id = "specialTierPassiveFamilyDuplicateCount",
                ownerTaskId = "whiteBoxLoot",
                outputSection = "local-reward-identity",
                formula = "count(same-zone duplicate passive families across unique/artifact templates)",
                failSemantics = "FAIL means same-zone special-tier rewards still collapse into overlapping passive families.",
                decisionNotes = "This metric is derived from the official special-template corpus and should remain at zero once PR-02 identity cleanup lands.",
            ),
            Phase4MetricSpec(
                id = "secretZoneRewardAuthorityViolations",
                ownerTaskId = "whiteBoxLoot",
                outputSection = "local-reward-identity",
                formula = "count(shared secret reward authority scan violations for secret-zone bindings)",
                failSemantics = "FAIL means the shared SecretRewardAuthority contract no longer resolves secret-zone rewards solely from SecretZoneDef.rewardProfileId.",
                decisionNotes = "This is the single-source-of-truth guardrail for the landed authority helper; non-secret-zone LOOT_PROFILE remains legal, but secret-zone fallback drift must stay at zero.",
            ),
            Phase4MetricSpec(
                id = "topFiveAffixExposureShare",
                ownerTaskId = "whiteBoxLoot",
                outputSection = "local-reward-identity",
                formula = "sum(top five affix ids by full observed whiteBoxLoot affix exposure) / total observed affix exposures",
                failSemantics = "FAIL means reward affix exposure has collapsed into a few generic affixes instead of build-specific payoff.",
                decisionNotes = "The metric is emitted by whiteBoxLoot from the shared loot kernel; max single-affix share, focused high-frequency share, and full affix distribution diff are supporting evidence under the same metric.",
            ),
            Phase4MetricSpec(
                id = "frontstageHighPriorityCueRetainedRate",
                ownerTaskId = "hiddenContentHarness",
                outputSection = "frontstage-action-cue",
                formula = "cases retaining CRITICAL/HIGH frontstage action cue / cases expected to surface high-priority search or secret cue",
                failSemantics = "FAIL means high-value search/secret cues can be displaced before the player-facing snapshot exposes them.",
                decisionNotes = "This metric must consume typed FrontstageActionCueSnapshot priority/category fields, not localized text or client-side ordering.",
            ),
            Phase4MetricSpec(
                id = "frontstageCueDedupAppliedCount",
                ownerTaskId = "hiddenContentHarness",
                outputSection = "frontstage-action-cue",
                formula = "count(focused repeated-stableKey probes where duplicate log entries collapse to one frontstage cue)",
                failSemantics = "FAIL means stableKey replacement is not active and repeated triggers can grow the frontstage queue.",
                decisionNotes = "Dedup is verified with the runtime session contract so the report does not infer queue behavior from display text.",
            ),
            Phase4MetricSpec(
                id = "frontstageCueExpiryParity",
                ownerTaskId = "hiddenContentHarness",
                outputSection = "frontstage-action-cue",
                formula = "focused TTL probe pass count / focused TTL probe count",
                failSemantics = "FAIL means priority-specific TTL semantics drifted from the formal action cue contract.",
                decisionNotes = "The probe uses the same session snapshot path as player rendering and locks CRITICAL/HIGH/MEDIUM/LOW expiry boundaries.",
            ),
            Phase4MetricSpec(
                id = "frontstageSecretCueVisibilityRate",
                ownerTaskId = "hiddenContentHarness",
                outputSection = "frontstage-action-cue",
                formula = "entered secret-zone cases with SECRET frontstage cue / entered secret-zone case count",
                failSemantics = "FAIL means secret-zone action cues are logged or white-box visible but not retained in player-facing frontstage snapshot.",
                decisionNotes = "Secret cue visibility is anchored in the hidden-content corpus and typed category field, not a client tone heuristic.",
            ),
            Phase4MetricSpec(
                id = "frontstageSearchCueVisibilityRate",
                ownerTaskId = "hiddenContentHarness",
                outputSection = "frontstage-action-cue",
                formula = "scripted cases with visible Search prompt cue / scripted cases expected to expose Search availability",
                failSemantics = "FAIL means player-facing Search affordance cues are missing before hidden-zone entry attempts.",
                decisionNotes = "This metric consumes SEARCH_AVAILABLE cueType evidence and covers the non-combat Search prompt path required by PR04.",
            ),
            Phase4MetricSpec(
                id = "zoneHookCoverage",
                ownerTaskId = "hiddenContentHarness",
                outputSection = "frontstage-action-cue",
                formula = "triggered mandatory zone runtime hooks / mandatory zone runtime hooks",
                failSemantics = "FAIL means at least one mandatory zone has flavor text without a runtime hook path.",
                decisionNotes = "The denominator is the five PR04 mandatory zone hooks: trail_pressure, slag_alert, ritual_pressure, ferry_crossing, void_pressure.",
            ),
            Phase4MetricSpec(
                id = "professionCapstoneSourceCoverage.reportOnly",
                ownerTaskId = "whiteBoxLoot",
                outputSection = "local-reward-identity",
                formula = "covered preferred profession/source pairs / total preferred profession/source pairs",
                failSemantics = "FAIL means at least one preferred profession/source pair still has no legal capstone path on the official main-path sources.",
                decisionNotes = "Unlike the staged adoption and non-weapon floors, preferred source coverage is part of the current hardening contract and must fail before long-run verification.",
            ),
            Phase4MetricSpec(
                id = "zoneRouteHashDiversity.topHashShare",
                ownerTaskId = "longRunLab",
                outputSection = "route-diversity",
                formula = "max(zoneRouteHash count) / terminal route diversity run count",
                failSemantics = "FAIL means long-run route evidence is still collapsing into one route hash instead of proving route diversity.",
                decisionNotes = "This metric is read from long-run-full.json route diversity evidence; route probes are reported separately and do not enter the denominator.",
            ),
            Phase4MetricSpec(
                id = "branchInclusiveCount",
                ownerTaskId = "longRunLab",
                outputSection = "route-diversity",
                formula = "count(branch_inclusive scenarios)",
                failSemantics = "FAIL means the branch-inclusive corpus no longer covers the four PR06 mandatory/secret combinations.",
                decisionNotes = "The branch count is owner evidence from the long-run corpus, not a markdown-derived summary.",
            ),
            Phase4MetricSpec(
                id = "fullRouteCount",
                ownerTaskId = "longRunLab",
                outputSection = "route-diversity",
                formula = "count(full_route scenarios)",
                failSemantics = "FAIL means the formal full-route corpus fell below the fixed PR06 12-run matrix.",
                decisionNotes = "The fixed 12 full-route seeds are the route diversity owner corpus and must remain visible in reportPhase4.",
            ),
            Phase4MetricSpec(
                id = "topologyCategoryDiversityPerSmokeRun.reportOnly",
                ownerTaskId = "longRunLab",
                outputSection = "route-diversity",
                formula = "distinct terminal route hashes / terminal route diversity run count",
                failSemantics = "Report-only warning in PR06; it must be visible but must not fail the owner gate.",
                decisionNotes = "This supporting ratio keeps topology diversity visible without redefining the blocking top-hash-share gate.",
            ),
            Phase4MetricSpec(
                id = "fullRouteIntentDistinctCount",
                ownerTaskId = "longRunLab",
                outputSection = "route-diversity",
                formula = "distinct(route intent token) over full_route scenarios",
                failSemantics = "display only",
                decisionNotes = "Supporting evidence for the 12 documented PR06 route intents; the blocking coverage is enforced by corpus tests and counts.",
            ),
            Phase4MetricSpec(
                id = "avgObjectiveAcquireTurn",
                ownerTaskId = "longRunLab",
                outputSection = "critical-path-pacing",
                formula = "min(critical-path zone avgObjectiveAcquireTurn)",
                failSemantics = "FAIL means at least one critical-path zone still allows objective acquisition too close to zone entry.",
                decisionNotes = "The report must keep per-zone breakdown and failing-zone ids; this metric is the minimum across critical-path zones, not a corpus average.",
            ),
            Phase4MetricSpec(
                id = "avgVisibleHostileTurnCount",
                ownerTaskId = "longRunLab",
                outputSection = "critical-path-pacing",
                formula = "min(critical-path zone avgVisibleHostileTurnCount)",
                failSemantics = "FAIL means at least one critical-path zone is still effectively combat-empty in visible hostile turns.",
                decisionNotes = "This is a pacing floor guardrail, not a balance tuning target; the minimum critical-path zone owns the verdict.",
            ),
            Phase4MetricSpec(
                id = "avgEnemyTurns",
                ownerTaskId = "longRunLab",
                outputSection = "critical-path-pacing",
                formula = "min(critical-path zone avgEnemyTurns)",
                failSemantics = "FAIL means at least one critical-path zone still collapses to zero or near-zero enemy activity.",
                decisionNotes = "Use longRunLab zone traversal diagnostics directly; do not reconstruct this from markdown.",
            ),
            Phase4MetricSpec(
                id = "criticalPathCombatFloorSatisfied",
                ownerTaskId = "longRunLab",
                outputSection = "critical-path-pacing",
                formula = "satisfiedCriticalPathZoneCount / criticalPathZoneCount",
                failSemantics = "FAIL means at least one critical-path zone misses the formal objective/combat pacing floors.",
                decisionNotes = "This is the aggregate pacing owner gate; the section must still print which zones failed and why.",
            ),
            Phase4MetricSpec(
                id = "terminalWeaponBaseDiversity",
                ownerTaskId = "longRunLab",
                outputSection = "terminal-build-identity",
                formula = "distinct(fullRoute terminal weapon base ids)",
                failSemantics = "FAIL means long-run terminal builds are collapsing toward too few weapon bases.",
                decisionNotes = "The metric is read from long-run-full.json and must not be reverse-engineered from markdown.",
            ),
            Phase4MetricSpec(
                id = "crossProfessionTopWeaponDominance",
                ownerTaskId = "longRunLab",
                outputSection = "terminal-build-identity",
                formula = "count(most common fullRoute terminal weapon) / fullRouteCount",
                failSemantics = "FAIL means one weapon base is dominating terminal builds across professions.",
                decisionNotes = "This metric is the explicit owner gate for the current battle_axe convergence risk.",
            ),
            Phase4MetricSpec(
                id = "professionAlignedWeaponAdoptionRate",
                ownerTaskId = "longRunLab",
                outputSection = "terminal-build-identity",
                formula = "alignedFullRouteSampleCount / fullRouteCount",
                failSemantics = "FAIL means profession identity is being swallowed by generic terminal weapon choices.",
                decisionNotes = "Allowed archetypes are frozen in V2OPT-PR-01 and should only change with explicit document updates.",
            ),
            Phase4MetricSpec(
                id = "professionCapstoneSeenRate",
                ownerTaskId = "longRunLab",
                outputSection = "terminal-build-identity",
                formula = "fullRoute runs with at least one profession capstone seen / fullRouteCount",
                failSemantics = "FAIL means main-path capstone chase targets still are not materially visible across full-route runs.",
                decisionNotes = "This metric must be derived from milestone reward summaries and printed with per-profession capstone breakdown.",
            ),
            Phase4MetricSpec(
                id = "professionCapstoneAdoptionRate",
                ownerTaskId = "longRunLab",
                outputSection = "terminal-build-identity",
                formula = "fullRoute runs adopting at least one profession capstone / fullRouteCount",
                failSemantics = "FAIL means profession capstones are being seen but still are not converting into terminal-build decisions often enough.",
                decisionNotes = "Use the same profession-capstone milestone summary as professionCapstoneSeenRate; the report must keep the shared per-profession breakdown visible.",
            ),
            Phase4MetricSpec(
                id = "nonWeaponBuildPayoffRate",
                ownerTaskId = "longRunLab",
                outputSection = "terminal-build-identity",
                formula = "fullRoute runs adopting a non-weapon profession capstone / fullRouteCount",
                failSemantics = "FAIL means OFF_HAND / ARMOR build-defining payoff still is not landing often enough in terminal builds.",
                decisionNotes = "Use the same profession-capstone evidence chain as professionCapstoneSeenRate; do not invent a second build summary source.",
            ),
            Phase4MetricSpec(
                id = "professionCapstoneAdoptionFloor",
                ownerTaskId = "longRunLab",
                outputSection = "terminal-build-identity",
                formula = "count(professions meeting build-identity adoption floor) / foundationProfessionCount",
                failSemantics = "FAIL means at least one base profession still does not adopt a build-identity capstone in terminal full-route runs.",
                decisionNotes = "PR-03 cuts this from report-only to blocking; the floor is derived from build-identity schema and printed with per-profession breakdown.",
            ),
            Phase4MetricSpec(
                id = "nonWeaponBuildPayoffFloor",
                ownerTaskId = "longRunLab",
                outputSection = "terminal-build-identity",
                formula = "count(professions meeting build-identity non-weapon floor) / foundationProfessionCount",
                failSemantics = "FAIL means at least one base profession still lacks a non-weapon build payoff in terminal full-route runs.",
                decisionNotes = "PR-03 cuts this from report-only to blocking and keeps capstoneAdoptionBySlot as supporting evidence.",
            ),
            Phase4MetricSpec(
                id = "milestoneRewardAdoptionDelta",
                ownerTaskId = "longRunLab",
                outputSection = "milestone-reward-slot-balance",
                formula = "adopted milestone reward count - non-adopted milestone reward count",
                failSemantics = "FAIL means milestone rewards are being generated but not becoming terminal-build decisions.",
                decisionNotes = "This is the explicit adopted > notAdopted PR-03 gate.",
            ),
            Phase4MetricSpec(
                id = "milestoneRewardSlotBalance.maxSlotShare",
                ownerTaskId = "longRunLab",
                outputSection = "milestone-reward-slot-balance",
                formula = "max(slotFamilyShare) across milestone reward slot families",
                failSemantics = "FAIL means milestone rewards are still collapsing into a single slot family.",
                decisionNotes = "Raw slot and normalized slot-family distributions remain supporting evidence.",
            ),
            Phase4MetricSpec(
                id = "milestoneRewardSlotBalance.WEAPON",
                ownerTaskId = "longRunLab",
                outputSection = "milestone-reward-slot-balance",
                formula = "weapon slot-family milestone reward share",
                failSemantics = "FAIL means weapon rewards no longer remain a material milestone family.",
                decisionNotes = "Part of the PR-03 slot-balance cutover.",
            ),
            Phase4MetricSpec(
                id = "milestoneRewardSlotBalance.OFF_HAND",
                ownerTaskId = "longRunLab",
                outputSection = "milestone-reward-slot-balance",
                formula = "off-hand slot-family milestone reward share",
                failSemantics = "FAIL means off-hand rewards no longer remain a material milestone family.",
                decisionNotes = "Part of the PR-03 slot-balance cutover.",
            ),
            Phase4MetricSpec(
                id = "milestoneRewardSlotBalance.ARMOR",
                ownerTaskId = "longRunLab",
                outputSection = "milestone-reward-slot-balance",
                formula = "armor slot-family milestone reward share",
                failSemantics = "FAIL means armor rewards no longer remain a material milestone family.",
                decisionNotes = "Part of the PR-03 slot-balance cutover.",
            ),
            Phase4MetricSpec(
                id = "milestoneRewardSlotBalance.ACCESSORY",
                ownerTaskId = "longRunLab",
                outputSection = "milestone-reward-slot-balance",
                formula = "accessory slot-family milestone reward share",
                failSemantics = "FAIL means accessory rewards no longer remain a material milestone family.",
                decisionNotes = "Part of the PR-03 slot-balance cutover.",
            ),
            Phase4MetricSpec(
                id = "milestoneRewardSlotBalance.CONSUMABLE_OR_UTILITY",
                ownerTaskId = "longRunLab",
                outputSection = "milestone-reward-slot-balance",
                formula = "utility slot-family milestone reward share",
                failSemantics = "FAIL means utility-style milestone rewards disappear from the corpus.",
                decisionNotes = "This family covers consumable or support/sustain utility milestone rewards without changing the core equipment slot enum.",
            ),
            Phase4MetricSpec(
                id = "starterProfessionTalentMaxCount",
                ownerTaskId = "longRunLab",
                outputSection = "profession-tree-run-choice",
                formula = "max(starterProfessionTalentCount) over release-facing terminal profession runs",
                failSemantics = "FAIL means at least one release-facing profession starts with more than three learned profession talents.",
                decisionNotes = "Only vanguard/arcanist/rogue/templar enter the blocking denominator; advanced and frozen professions are listed separately.",
            ),
            Phase4MetricSpec(
                id = "learnedTalentChoiceEventRate",
                ownerTaskId = "longRunLab",
                outputSection = "profession-tree-run-choice",
                formula = "terminal runs with at least one confirmed non-starter rank 0 -> 1 learned event / terminal run count",
                failSemantics = "FAIL means long-run automation is not proving that upgrade points become real learn choices.",
                decisionNotes = "Starter ranks and rank-up events must not count toward this numerator.",
            ),
            Phase4MetricSpec(
                id = "multiTreeInvestmentAboveThresholdRate",
                ownerTaskId = "longRunLab",
                outputSection = "profession-tree-run-choice",
                formula = "terminal runs with at least two profession trees each invested >= 3 points / terminal run count",
                failSemantics = "FAIL means profession tree progression collapses into single-tree investment before terminal runs.",
                decisionNotes = "This metric reads tree investment from runtime run summaries, not from UI labels or markdown.",
            ),
            Phase4MetricSpec(
                id = "breakpointChoiceEventRate",
                ownerTaskId = "longRunLab",
                outputSection = "profession-tree-run-choice",
                formula = "terminal runs confirming at least one breakpoint event / terminal runs with at least one breakpoint preview",
                failSemantics = "FAIL means breakpoint previews are visible but not being converted into confirmed choices.",
                decisionNotes = "Hover and preview-only state do not count; only log.talent.breakpoint_chosen events count.",
            ),
            Phase4MetricSpec(
                id = "talentTreePrimaryInvestmentDistribution",
                ownerTaskId = "longRunLab",
                outputSection = "profession-tree-run-choice",
                formula = "count(primaryInvestmentTreeId) over release-facing terminal profession runs",
                failSemantics = "display only",
                decisionNotes = "Supporting distribution used to inspect single-tree dominance; it is not a blocking target.",
            ),
            Phase4MetricSpec(
                id = "talentReserveSwapCount",
                ownerTaskId = "longRunLab",
                outputSection = "profession-tree-run-choice",
                formula = "sum(reserve active-slot choices) over release-facing terminal profession runs",
                failSemantics = "display only",
                decisionNotes = "Supporting pressure signal for active-slot choice; it must not become a second loadout authority.",
            ),
            Phase4MetricSpec(
                id = "rankBreakpointAdoptionByTalent",
                ownerTaskId = "longRunLab",
                outputSection = "profession-tree-run-choice",
                formula = "count(log.talent.breakpoint_chosen) grouped by talentId@breakpointRank",
                failSemantics = "display only",
                decisionNotes = "Supporting adoption breakdown used for balancing after the blocking event rate is green.",
            ),
            Phase4MetricSpec(
                id = "autoLearnedNonStarterTalentCount",
                ownerTaskId = "longRunLab",
                outputSection = "profession-tree-run-choice",
                formula = "count(learned non-starter talents without a matching learned event)",
                failSemantics = "display only",
                decisionNotes = "Schema invariant audit: formal new runs must stay at 0; this field must not enter blocking owner targets.",
            ),
            Phase4MetricSpec(
                id = "starterInscriptionMaxCount",
                ownerTaskId = "longRunLab",
                outputSection = "inscription-shop-replacement",
                formula = "max(startingInscriptionCount) over terminal full-route runs",
                failSemantics = "FAIL means at least one profession still starts with more than two inscriptions.",
                decisionNotes = "This is the owner guard for Phase4 v4 PR-02 two-starter inscription contract.",
            ),
            Phase4MetricSpec(
                id = "fullSlotInscriptionPurchaseBlockedWithoutReplacementCount",
                ownerTaskId = "longRunLab",
                outputSection = "inscription-shop-replacement",
                formula = "sum(full-slot inscription purchases rejected without replacement prompt)",
                failSemantics = "FAIL means full-slot inscription purchase regressed to the old hard-reject flow.",
                decisionNotes = "Full-slot purchases must enter the replacement flow and must not deduct shards before a target is chosen.",
            ),
            Phase4MetricSpec(
                id = "inscriptionInstallOrReplaceRate",
                ownerTaskId = "longRunLab",
                outputSection = "inscription-shop-replacement",
                formula = "terminal full-route runs with at least one inscription install or replacement / terminal full-route runs",
                failSemantics = "FAIL means long-run automation is not proving the inscription shop path is reachable.",
                decisionNotes = "The rate is backed by runtime install/replace telemetry, not localized shop text.",
            ),
            Phase4MetricSpec(
                id = "inscriptionReplacementProbeSuccessCount",
                ownerTaskId = "longRunLab",
                outputSection = "inscription-shop-replacement",
                formula = "count(deterministic full-slot replacement probes completed through RunObservation + SmokeBot)",
                failSemantics = "FAIL means owner evidence no longer proves that a full-slot replacement prompt can be resolved by automation.",
                decisionNotes = "This is separate from installOrReplaceRate so install-only terminal runs cannot satisfy the replacement contract.",
            ),
            Phase4MetricSpec(
                id = "terminalInscriptionLoadoutDiversity",
                ownerTaskId = "longRunLab",
                outputSection = "inscription-shop-replacement",
                formula = "distinct terminal inscription loadout ids over terminal full-route runs",
                failSemantics = "display only",
                decisionNotes = "Supporting diversity signal used for balance tuning after the blocking reachability gates are green.",
            ),
            Phase4MetricSpec(
                id = "inscriptionCategoryCountDistribution",
                ownerTaskId = "longRunLab",
                outputSection = "inscription-shop-replacement",
                formula = "terminal inscription category count vectors grouped by count",
                failSemantics = "display only",
                decisionNotes = "Supporting evidence for category-limit pressure and replacement choices.",
            ),
            Phase4MetricSpec(
                id = "shopInscriptionOfferConversionRate",
                ownerTaskId = "longRunLab",
                outputSection = "inscription-shop-replacement",
                formula = "inscription shop purchases / inscription shop offers seen",
                failSemantics = "display only",
                decisionNotes = "Supporting conversion signal; it is not a substitute for installOrReplaceRate.",
            ),
            Phase4MetricSpec(
                id = "inscriptionReplaceReasonDistribution",
                ownerTaskId = "longRunLab",
                outputSection = "inscription-shop-replacement",
                formula = "replacement rejections grouped by InscriptionEquipFailure",
                failSemantics = "display only",
                decisionNotes = "Supporting rejection taxonomy for replacement modal tuning.",
            ),
            Phase4MetricSpec(
                id = "phaseTransitionObservedRatio",
                ownerTaskId = "bossHarness",
                outputSection = "boss-phase-identity",
                formula = "boss samples with observed formal phase transition / boss sample count",
                failSemantics = "FAIL means at least one boss sample no longer exercises the formal phase transition path required by PR-04.",
                decisionNotes = "This is bossHarness owner evidence, not a supporting input metric; it must remain visible in reportPhase4 owner metrics.",
            ),
            Phase4MetricSpec(
                id = "variantTraceDivergenceRatio",
                ownerTaskId = "bossHarness",
                outputSection = "boss-phase-identity",
                formula = "variant/base boss pairs with divergent AI/action trace / variant/base boss pair count",
                failSemantics = "FAIL means at least one boss variant still behaves like its base encounter despite distinct variant identity data.",
                decisionNotes = "Trace divergence is the headline variant identity metric; pair-level trace evidence remains supporting detail.",
            ),
            Phase4MetricSpec(
                id = "minVariantActionTraceDivergenceScore",
                ownerTaskId = "bossHarness",
                outputSection = "boss-phase-identity",
                formula = "min(selected-action divergence score across variant/base boss pairs)",
                failSemantics = "FAIL means the weakest boss variant/base pair falls below the frozen action-divergence floor.",
                decisionNotes = "The minimum owns the verdict so aggregate trace diversity cannot hide a near-identical variant.",
            ),
            Phase4MetricSpec(
                id = "bossVariantBasePhaseCountMin",
                ownerTaskId = "bossHarness",
                outputSection = "boss-phase-identity",
                formula = "min(base encounter phase count across formal boss variants)",
                failSemantics = "FAIL means a formal boss variant is bound to a base encounter without enough phase structure.",
                decisionNotes = "This keeps variant identity tied to BossEncounter phase contract rather than generic mutation metadata.",
            ),
            Phase4MetricSpec(
                id = "bossVariantPhaseOverrideSchemaCoverage",
                ownerTaskId = "bossHarness",
                outputSection = "boss-phase-identity",
                formula = "boss variants declaring phaseOverrides / formal boss variant count",
                failSemantics = "FAIL means at least one formal boss variant still has no data-level phase override language.",
                decisionNotes = "This is schema owner evidence from boss-variants/index.yaml, not inferred from base encounter phase count.",
            ),
            Phase4MetricSpec(
                id = "bossVariantPhaseOverrideRuntimeTriggerCoverage",
                ownerTaskId = "bossHarness",
                outputSection = "boss-phase-identity",
                formula = "boss variants whose phase override trigger fired in bossHarness / formal boss variant count",
                failSemantics = "FAIL means at least one declared phase override cannot be observed through runtime trigger facts.",
                decisionNotes = "The runtime trigger consumes semantic trigger ids in BossPhaseManager; tools must not reimplement trigger evaluation.",
            ),
            Phase4MetricSpec(
                id = "bossVariantPhaseOverrideTelegraphCoverage",
                ownerTaskId = "bossHarness",
                outputSection = "boss-phase-identity",
                formula = "boss variant override telegraph specs observed in runtime boss traces / formal boss variant telegraph specs",
                failSemantics = "FAIL means at least one variant phase override does not surface its formal telegraph warning.",
                decisionNotes = "The denominator is fixed to the three PR-05 variant telegraph specs.",
            ),
            Phase4MetricSpec(
                id = "bossVariantPhaseOverrideActionDistinctCount.reportOnly",
                ownerTaskId = "bossHarness",
                outputSection = "boss-phase-identity",
                formula = "minimum per-variant distinct action ids strengthened by variant phase overrides in runtime samples",
                failSemantics = "display only",
                decisionNotes = "Report-only supporting signal; details include per-variant counts so one variant cannot be hidden by another variant's action emphasis.",
            ),
            Phase4MetricSpec(
                id = "terrainInteractionEncounterRate.aggregate",
                ownerTaskId = "terrainInteractionBatch",
                outputSection = "terrain-combat-sample-contract",
                formula = "triggeredInteractionCombatCount / taggedCombatCount",
                failSemantics = "FAIL means aggregate terrain interaction encounter rate regressed below the frozen baseline uplift target.",
                decisionNotes = "Aggregate success is not sufficient; per-zone lower-bound failures must still be surfaced separately.",
            ),
            Phase4MetricSpec(
                id = "terrainInteractionEncounterRate.per_zone_lower_bound",
                ownerTaskId = "terrainInteractionBatch",
                outputSection = "terrain-combat-sample-contract",
                formula = "for each combat-sampled zone: terrainInteractionEncounterRate >= perZoneEncounterLowerBoundTarget",
                failSemantics = "FAIL means at least one combat-sampled zone is falling below the minimum encounter language floor even if the aggregate stays green.",
                decisionNotes = "The combat-sampled zone list and exclusion reasons are part of the contract and must be printed beside the metric.",
            ),
        )

    fun ownerTaskIds(): Set<String> = specs.mapTo(linkedSetOf(), Phase4MetricSpec::ownerTaskId)

    fun metricIds(
        ownerTaskId: String,
        outputSection: String,
    ): List<String> =
        specs
            .filter { spec -> spec.ownerTaskId == ownerTaskId && spec.outputSection == outputSection }
            .map(Phase4MetricSpec::id)

    fun bossPhaseIdentityMetricIds(): List<String> =
        metricIds(
            ownerTaskId = "bossHarness",
            outputSection = "boss-phase-identity",
        )

    fun entryFor(
        metricId: String,
        sourcePathByTaskId: Map<String, String>,
        targetTextByMetricId: Map<String, String>,
    ): Phase4MetricCatalogEntry {
        val spec = requireSpec(metricId)
        return Phase4MetricCatalogEntry(
            id = spec.id,
            ownerTaskId = spec.ownerTaskId,
            sourcePath = checkNotNull(sourcePathByTaskId[spec.ownerTaskId]) { "Missing sourcePath for phase4 task '${spec.ownerTaskId}'." },
            outputSection = spec.outputSection,
            formula = spec.formula,
            targetText = checkNotNull(targetTextByMetricId[spec.id]) { "Missing phase4 targetText for '${spec.id}'." },
            failSemantics = spec.failSemantics,
            decisionNotes = spec.decisionNotes,
        )
    }

    fun entries(
        sourcePathByTaskId: Map<String, String>,
        targetTextByMetricId: Map<String, String>,
    ): List<Phase4MetricCatalogEntry> =
        specs.map { spec ->
            entryFor(
                metricId = spec.id,
                sourcePathByTaskId = sourcePathByTaskId,
                targetTextByMetricId = targetTextByMetricId,
            )
        }

    fun requireSpec(metricId: String): Phase4MetricSpec =
        checkNotNull(specs.firstOrNull { spec -> spec.id == metricId }) { "Missing Phase 4 metric spec for '$metricId'." }
}

internal fun Phase4MetricCatalogEntry.toJson() =
    buildJsonObject {
        put("metricId", id)
        put("ownerTaskId", ownerTaskId)
        put("sourcePath", sourcePath)
        put("outputSection", outputSection)
        put("formula", formula)
        put("target", targetText)
        put("failSemantics", failSemantics)
        put("decisionNotes", decisionNotes)
    }
