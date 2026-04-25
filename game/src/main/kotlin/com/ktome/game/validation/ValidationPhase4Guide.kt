package com.ktome.game.validation

import com.ktome.core.world.solvability.SearchBindingId

data class ValidationPhase4Guide(
    val targetLabelKeys: List<String>,
    val quickPathLabelKeys: List<String>,
    val evidenceLabelKeys: List<String>,
)

private data class ValidationPresetGuideSpec(
    val seedCorpus: List<Long>,
    val phase4Guide: ValidationPhase4Guide,
    val hiddenBindingId: SearchBindingId? = null,
)

private fun singleSeedGuideSpec(
    preset: ValidationPreset,
    phase4Guide: ValidationPhase4Guide,
    hiddenBindingId: SearchBindingId? = null,
): ValidationPresetGuideSpec =
    ValidationPresetGuideSpec(
        seedCorpus = listOf(validationFoundationConfig(preset).seed),
        phase4Guide = phase4Guide,
        hiddenBindingId = hiddenBindingId,
    )

fun validationSeedCorpus(preset: ValidationPreset): List<Long> =
    validationPresetGuideSpec(preset).seedCorpus

fun validationPhase4Guide(preset: ValidationPreset): ValidationPhase4Guide =
    validationPresetGuideSpec(preset).phase4Guide

fun validationPhase4Guide(summary: ValidationSummarySnapshot): ValidationPhase4Guide =
    summary.scenarioId
        ?.let(ValidationScenarioRegistry::find)
        ?.let { scenario ->
            ValidationPhase4Guide(
                targetLabelKeys =
                    listOf(
                        "validation.phase4.v4.${scenario.id.value}.target",
                    ),
                quickPathLabelKeys =
                    listOf(
                        "validation.phase4.v4.${scenario.id.value}.quick.prepare",
                        "validation.phase4.v4.${scenario.id.value}.quick.evidence",
                    ),
                evidenceLabelKeys =
                    listOf(
                        "validation.phase4.v4.${scenario.id.value}.evidence.bootstrap",
                        "validation.phase4.v4.${scenario.id.value}.evidence.primary",
                        "validation.phase4.v4.${scenario.id.value}.evidence.secondary",
                        "validation.phase4.v4.${scenario.id.value}.evidence.summary",
                    ),
            )
        }
        ?: validationPhase4Guide(summary.preset)

fun validationHiddenBindingForPreset(preset: ValidationPreset): SearchBindingId? =
    validationPresetGuideSpec(preset).hiddenBindingId

private fun validationPresetGuideSpec(preset: ValidationPreset): ValidationPresetGuideSpec =
    when (preset) {
        ValidationPreset.MAPGEN_DIFF ->
            ValidationPresetGuideSpec(
                seedCorpus =
                    listOf(
                        20260401L,
                        20260402L,
                        20260403L,
                        20260404L,
                        20260405L,
                    ),
                phase4Guide =
                    ValidationPhase4Guide(
                        targetLabelKeys = listOf("ui.validation.phase4.target.mapgen_diff"),
                        quickPathLabelKeys =
                            listOf(
                                "ui.validation.phase4.quick.mapgen.restart_corpus",
                                "ui.validation.phase4.quick.mapgen.compare_five_seed",
                            ),
                        evidenceLabelKeys =
                            listOf(
                                "ui.validation.phase4.evidence.mapgen.seed_zone",
                                "ui.validation.phase4.evidence.mapgen.terrain_hidden",
                            ),
                    ),
            )

        ValidationPreset.HIDDEN_CONTENT ->
            singleSeedGuideSpec(
                preset = ValidationPreset.HIDDEN_CONTENT,
                phase4Guide =
                    ValidationPhase4Guide(
                        targetLabelKeys =
                            listOf(
                                "ui.validation.phase4.target.hidden_search",
                                "ui.validation.phase4.target.hidden_secret",
                            ),
                        quickPathLabelKeys =
                            listOf(
                                "ui.validation.phase4.quick.hidden.search_anchor",
                                "ui.validation.phase4.quick.hidden.secret_loop",
                            ),
                        evidenceLabelKeys =
                            listOf(
                                "ui.validation.phase4.evidence.hidden.search_feedback",
                                "ui.validation.phase4.evidence.hidden.return_bridge",
                            ),
                    ),
                hiddenBindingId = SearchBindingId("search.underground_river.crystal_rift"),
            )

        ValidationPreset.TERRAIN_INTERACTION ->
            singleSeedGuideSpec(
                preset = ValidationPreset.TERRAIN_INTERACTION,
                phase4Guide =
                    ValidationPhase4Guide(
                        targetLabelKeys = listOf("ui.validation.phase4.target.terrain_boss"),
                        quickPathLabelKeys =
                            listOf(
                                "ui.validation.phase4.quick.terrain.override_cycle",
                                "ui.validation.phase4.quick.terrain.observe_rules",
                            ),
                        evidenceLabelKeys =
                            listOf(
                                "ui.validation.phase4.evidence.terrain.rule",
                                "ui.validation.phase4.evidence.terrain.inspect",
                            ),
                    ),
            )

        ValidationPreset.ELITE_MUTATION ->
            singleSeedGuideSpec(
                preset = ValidationPreset.ELITE_MUTATION,
                phase4Guide =
                    ValidationPhase4Guide(
                        targetLabelKeys = listOf("ui.validation.phase4.target.terrain_boss"),
                        quickPathLabelKeys =
                            listOf(
                                "ui.validation.phase4.quick.elite.spawn_and_focus",
                            ),
                        evidenceLabelKeys =
                            listOf(
                                "ui.validation.phase4.evidence.elite.mutation_readability",
                            ),
                    ),
            )

        ValidationPreset.BOSS_VARIANT ->
            singleSeedGuideSpec(
                preset = ValidationPreset.BOSS_VARIANT,
                phase4Guide =
                    ValidationPhase4Guide(
                        targetLabelKeys = listOf("ui.validation.phase4.target.terrain_boss"),
                        quickPathLabelKeys =
                            listOf(
                                "ui.validation.phase4.quick.boss.force_variant_start",
                                "ui.validation.phase4.quick.boss.travel_and_finish",
                            ),
                        evidenceLabelKeys =
                            listOf(
                                "ui.validation.phase4.evidence.boss.variant_readability",
                                "ui.validation.phase4.evidence.boss.phase_graph",
                            ),
                    ),
            )

        ValidationPreset.LOOT_LAB ->
            singleSeedGuideSpec(
                preset = ValidationPreset.LOOT_LAB,
                phase4Guide =
                    ValidationPhase4Guide(
                        targetLabelKeys =
                            listOf(
                                "ui.validation.phase4.target.loot",
                                "ui.validation.phase4.target.pr03_item_resources",
                            ),
                        quickPathLabelKeys =
                            listOf(
                                "ui.validation.phase4.quick.loot.pr03_showcase",
                                "ui.validation.phase4.quick.loot.present_reward",
                                "ui.validation.phase4.quick.loot.spawn_item",
                                "ui.validation.phase4.quick.loot.shop_anchor",
                            ),
                        evidenceLabelKeys =
                            listOf(
                                "ui.validation.phase4.evidence.loot.pr03_inventory_matrix",
                                "ui.validation.phase4.evidence.loot.pr03_high_value_marker",
                                "ui.validation.phase4.evidence.loot.pr03_audio_cues",
                                "ui.validation.phase4.evidence.loot.inspect",
                                "ui.validation.phase4.evidence.loot.log_source",
                            ),
                    ),
            )

        ValidationPreset.CONTENT_PACK ->
            singleSeedGuideSpec(
                preset = ValidationPreset.CONTENT_PACK,
                phase4Guide =
                    ValidationPhase4Guide(
                        targetLabelKeys = listOf("ui.validation.phase4.target.content_pack"),
                        quickPathLabelKeys =
                            listOf(
                                "ui.validation.phase4.quick.pack.sample_secret_route",
                                "ui.validation.phase4.quick.pack.restart_enabled",
                            ),
                        evidenceLabelKeys =
                            listOf(
                                "ui.validation.phase4.evidence.pack.active_ids",
                                "ui.validation.phase4.evidence.pack.visible_namespace",
                            ),
                    ),
                hiddenBindingId = SearchBindingId("search.underground_river.crystal_rift"),
            )

        ValidationPreset.CUSTOM ->
            singleSeedGuideSpec(
                preset = ValidationPreset.CUSTOM,
                phase4Guide =
                    ValidationPhase4Guide(
                        targetLabelKeys = listOf("ui.validation.phase4.target.custom"),
                        quickPathLabelKeys = listOf("ui.validation.phase4.quick.custom.manual"),
                        evidenceLabelKeys = listOf("ui.validation.phase4.evidence.custom.manual"),
                    ),
            )
    }
