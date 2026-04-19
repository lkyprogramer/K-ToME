package com.ktome.tools.verification

import com.ktome.tools.phase4.Phase4OwnerBaselineRegistry

object VerificationTaskRegistry {
    private val phase4CanonicalReportSharedPaths: List<String> =
        listOf(
            "tools/src/main/kotlin/com/ktome/tools/phase4/Phase4AggregationInputRunner.kt",
            "tools/src/main/kotlin/com/ktome/tools/phase4/Phase4CriticalPathPacing.kt",
            "tools/src/main/kotlin/com/ktome/tools/phase4/Phase4DomainArtifactRegistry.kt",
            "tools/src/main/kotlin/com/ktome/tools/phase4/Phase4MetricCatalog.kt",
            "tools/src/main/kotlin/com/ktome/tools/phase4/Phase4OwnerBaselineRegistry.kt",
            "tools/src/main/kotlin/com/ktome/tools/phase4/Phase4OwnerMetricTargets.kt",
            "tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt",
            "tools/src/main/kotlin/com/ktome/tools/phase4/ReportPhase4Runner.kt",
        )

    private val contractLintDomain =
        VerificationDomainSpec(
            domainId = "contractLint",
            phaseIds = setOf("phase4", "phase5"),
            workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
            defaultTier = VerificationTier.PREFLIGHT,
            nodeSpecs =
                listOf(
                    VerificationNodeSpec(
                        nodeId = "contractLint.staticGraph",
                        description = "Runs the schema, cross-reference, and key-namespace lint suite as a static graph demo domain.",
                        workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                        tier = VerificationTier.PREFLIGHT,
                        nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                        selectedClasses =
                            listOf(
                                "com.ktome.tools.lint.ContractLintTest",
                                "com.ktome.tools.lint.Phase4MapgenContractLintTest",
                                "com.ktome.tools.lint.Phase2ContentCoverageTest",
                            ),
                    ),
                ),
            baselinePolicy = BaselinePolicySpec(mode = BaselineMode.STRICT_ZERO_FAILURE),
            inputScopes =
                listOf(
                    InputScope(
                        scopeId = "schema-i18n.locale",
                        pathPrefixes = listOf("game/src/main/resources/i18n/"),
                    ),
                    InputScope(
                        scopeId = "schema-i18n.schema",
                        pathPrefixes = listOf("game/src/main/kotlin/com/ktome/game/data/schema/"),
                        ownerRequired = true,
                    ),
                ),
            preflightTaskPaths = listOf(":tools:verifyContractLintPreflight"),
            ownerTaskPaths = listOf(":tools:contractLint"),
            cachePolicy =
                VerificationCachePolicy(
                    buildCacheEnabled = true,
                    configurationCacheCompatible = true,
                    reuseExistingArtifacts = true,
                ),
            artifactPolicy = VerificationArtifactPolicy(),
        )

    private val maintainabilityDomain =
        VerificationDomainSpec(
            domainId = "maintainability",
            phaseIds = setOf("phase4", "phase5"),
            workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
            defaultTier = VerificationTier.PREFLIGHT,
            nodeSpecs =
                listOf(
                    VerificationNodeSpec(
                        nodeId = "maintainability.preflight",
                        description = "Runs the anti-bloat maintainability lint against the versioned debt baseline.",
                        workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                        tier = VerificationTier.PREFLIGHT,
                        nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                        selectedClasses = listOf("com.ktome.tools.lint.MaintainabilityLintRunnerTest"),
                    ),
                ),
            inputScopes =
                listOf(
                    InputScope(
                        scopeId = "maintainability.runtime.core",
                        pathPrefixes = listOf("core/src/main/kotlin/"),
                    ),
                    InputScope(
                        scopeId = "maintainability.runtime.game",
                        pathPrefixes = listOf("game/src/main/kotlin/"),
                    ),
                    InputScope(
                        scopeId = "maintainability.runtime.client",
                        pathPrefixes = listOf("client/src/main/kotlin/"),
                    ),
                    InputScope(
                        scopeId = "maintainability.runtime.tools",
                        pathPrefixes = listOf("tools/src/main/kotlin/"),
                    ),
                    InputScope(
                        scopeId = "maintainability.runtime.build-logic",
                        pathPrefixes = listOf("build-logic/src/main/kotlin/"),
                    ),
                    InputScope(
                        scopeId = "maintainability.governance",
                        pathPrefixes =
                            listOf(
                                "docs/rule/ai-change-governance.md",
                                "maintainability-baseline.json",
                                "build.gradle.kts",
                                "tools/build.gradle.kts",
                            ),
                    ),
                ),
            preflightTaskPaths = listOf(":tools:maintainabilityLint"),
            baselinePolicy =
                BaselinePolicySpec(
                    mode = BaselineMode.APPROVED_DEBT_SET,
                    baselinePath = "maintainability-baseline.json",
                ),
            cachePolicy =
                VerificationCachePolicy(
                    buildCacheEnabled = true,
                    configurationCacheCompatible = true,
                    reuseExistingArtifacts = true,
                ),
            artifactPolicy = VerificationArtifactPolicy(),
        )

    private val lootDomain =
        VerificationDomainSpec(
            domainId = "loot",
            phaseIds = setOf("phase4"),
            workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
            defaultTier = VerificationTier.PREFLIGHT,
            nodeSpecs =
                listOf(
                    VerificationNodeSpec(
                        nodeId = "loot.preflight",
                        description = "Runs the Phase 4 loot static preflight without entering the statistical lab.",
                        workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                        tier = VerificationTier.PREFLIGHT,
                        nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                        selectedClasses = listOf("com.ktome.tools.loot.LootPreflightRunnerTest"),
                    ),
                ),
            inputScopes =
                listOf(
                    InputScope(
                        scopeId = "loot.data.items",
                        pathPrefixes = listOf("game/src/main/resources/data/items/"),
                    ),
                    InputScope(
                        scopeId = "loot.data.loot",
                        pathPrefixes = listOf("game/src/main/resources/data/loot/"),
                    ),
                    InputScope(
                        scopeId = "loot.data.world",
                        pathPrefixes = listOf("game/src/main/resources/data/world/"),
                    ),
                    InputScope(
                        scopeId = "loot.runtime",
                        pathPrefixes =
                            listOf(
                                "game/src/main/kotlin/com/ktome/game/loot/",
                                "tools/src/main/kotlin/com/ktome/tools/loot/",
                            ),
                        ownerRequired = true,
                        requestedTaskPaths =
                            listOf(
                                ":tools:verifyLootPreflight",
                                ":tools:lootBalanceLab",
                                ":tools:whiteBoxLoot",
                            ),
                        requestedPreflightTaskPaths = listOf(":tools:verifyLootPreflight"),
                    ),
                    InputScope(
                        scopeId = "loot.owner-evaluation",
                        pathPrefixes =
                            listOf(
                                "tools/src/main/kotlin/com/ktome/tools/loot/WhiteBoxLootRunner.kt",
                                Phase4OwnerBaselineRegistry.LOOT_LOCAL_REWARD_BASELINE_RELATIVE_PATH,
                            ),
                        requestedTaskPaths =
                            listOf(
                                ":tools:verifyLootPreflight",
                                ":tools:whiteBoxLoot",
                            ),
                        requestedPreflightTaskPaths = listOf(":tools:verifyLootPreflight"),
                    ),
                    InputScope(
                        scopeId = "loot.phase4-report",
                        pathPrefixes =
                            phase4CanonicalReportSharedPaths,
                        requestedTaskPaths = listOf(":tools:reportPhase4Only"),
                    ),
                ),
            preflightTaskPaths = listOf(":tools:verifyLootPreflight"),
            ownerTaskPaths = listOf(":tools:lootBalanceLab", ":tools:whiteBoxLoot"),
            baselinePolicy = BaselinePolicySpec(mode = BaselineMode.STRICT_ZERO_FAILURE),
            cachePolicy =
                VerificationCachePolicy(
                    buildCacheEnabled = true,
                    configurationCacheCompatible = true,
                    reuseExistingArtifacts = true,
                ),
            artifactPolicy = VerificationArtifactPolicy(),
        )

    private val hiddenDomain =
        VerificationDomainSpec(
            domainId = "hidden",
            phaseIds = setOf("phase4"),
            workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
            defaultTier = VerificationTier.OWNER,
            nodeSpecs =
                listOf(
                    VerificationNodeSpec(
                        nodeId = "hidden.preflight",
                        description = "Runs the Phase 4 hidden-content static preflight without organic or session-driven probes.",
                        workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                        tier = VerificationTier.PREFLIGHT,
                        nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                        selectedClasses = listOf("com.ktome.tools.hidden.HiddenPreflightRunnerTest"),
                    ),
                    VerificationNodeSpec(
                        nodeId = "hidden.owner",
                        description = "Runs the deterministic hidden-content owner harness through the unified verification contract.",
                        workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
                        tier = VerificationTier.OWNER,
                        nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                        selectedClasses = listOf("com.ktome.tools.hidden.HiddenContentHarnessRunnerTest"),
                    ),
                ),
            inputScopes =
                listOf(
                    InputScope(
                        scopeId = "hidden.data.events",
                        pathPrefixes = listOf("game/src/main/resources/data/events/"),
                    ),
                    InputScope(
                        scopeId = "hidden.data.secret-zones",
                        pathPrefixes = listOf("game/src/main/resources/data/secret-zones/"),
                    ),
                    // Search bindings live under mapgen data, so the first version stays conservative here.
                    InputScope(
                        scopeId = "hidden.data.mapgen",
                        pathPrefixes = listOf("game/src/main/resources/data/mapgen/"),
                    ),
                    InputScope(
                        scopeId = "hidden.preflight-runner",
                        pathPrefixes = listOf("tools/src/main/kotlin/com/ktome/tools/hidden/HiddenPreflightRunner.kt"),
                        requestedTaskPaths = listOf(":tools:verifyHiddenPreflight"),
                        requestedPreflightTaskPaths = listOf(":tools:verifyHiddenPreflight"),
                    ),
                    InputScope(
                        scopeId = "hidden.runtime",
                        pathPrefixes =
                            listOf(
                                "tools/src/main/kotlin/com/ktome/tools/hidden/HiddenContentHarnessRunner.kt",
                                "tools/src/main/kotlin/com/ktome/tools/hidden/WhiteBoxHiddenContentRunner.kt",
                                "game/src/main/kotlin/com/ktome/game/hidden/",
                                "game/src/main/kotlin/com/ktome/game/Phase4StaticContentValidator.kt",
                        ),
                        ownerRequired = true,
                        requestedPreflightTaskPaths = listOf(":tools:verifyHiddenPreflight"),
                    ),
                    InputScope(
                        scopeId = "hidden.owner-evaluation",
                        pathPrefixes =
                            listOf(Phase4OwnerBaselineRegistry.SCRIPTED_HIDDEN_BASELINE_RELATIVE_PATH) +
                                phase4CanonicalReportSharedPaths,
                        requestedTaskPaths = listOf(":tools:reportPhase4Only"),
                    ),
                ),
            preflightTaskPaths = listOf(":tools:verifyHiddenPreflight"),
            ownerTaskPaths = listOf(":tools:hiddenContentHarness"),
            baselinePolicy = BaselinePolicySpec(mode = BaselineMode.STRICT_ZERO_FAILURE),
            cachePolicy =
                VerificationCachePolicy(
                    buildCacheEnabled = true,
                    configurationCacheCompatible = true,
                    reuseExistingArtifacts = true,
                ),
            artifactPolicy = VerificationArtifactPolicy(),
        )

    private val organicHiddenDomain =
        VerificationDomainSpec(
            domainId = "organic-hidden",
            phaseIds = setOf("phase4"),
            workloadClass = VerificationWorkloadClass.STATISTICAL_BATCH,
            defaultTier = VerificationTier.OWNER,
            nodeSpecs =
                listOf(
                    VerificationNodeSpec(
                        nodeId = "organic-hidden.owner",
                        description = "Runs the Phase 4 organic hidden-content probe through the unified verification contract.",
                        workloadClass = VerificationWorkloadClass.STATISTICAL_BATCH,
                        tier = VerificationTier.OWNER,
                        nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                        selectedClasses = listOf("com.ktome.tools.hidden.OrganicHiddenProbeRunnerTest"),
                    ),
                ),
            inputScopes =
                listOf(
                    InputScope(
                        scopeId = "organic-hidden.data.events",
                        pathPrefixes = listOf("game/src/main/resources/data/events/"),
                    ),
                    InputScope(
                        scopeId = "organic-hidden.data.secret-zones",
                        pathPrefixes = listOf("game/src/main/resources/data/secret-zones/"),
                    ),
                    InputScope(
                        scopeId = "organic-hidden.data.mapgen",
                        pathPrefixes = listOf("game/src/main/resources/data/mapgen/"),
                    ),
                    InputScope(
                        scopeId = "organic-hidden.runtime",
                        pathPrefixes =
                            listOf(
                                "tools/src/main/kotlin/com/ktome/tools/hidden/OrganicHiddenProbeRunner.kt",
                                "game/src/main/kotlin/com/ktome/game/harness/RunBot.kt",
                                "game/src/main/kotlin/com/ktome/game/harness/ScenarioModels.kt",
                                "game/src/main/kotlin/com/ktome/game/harness/ScenarioUtil.kt",
                                "game/src/main/kotlin/com/ktome/game/harness/SmokeBot.kt",
                                "game/src/main/kotlin/com/ktome/game/hidden/",
                            ),
                        ownerRequired = true,
                    ),
                    InputScope(
                        scopeId = "organic-hidden.owner-evaluation",
                        pathPrefixes =
                            listOf(Phase4OwnerBaselineRegistry.ORGANIC_HIDDEN_BASELINE_RELATIVE_PATH) +
                                phase4CanonicalReportSharedPaths,
                        requestedTaskPaths = listOf(":tools:reportPhase4Only"),
                    ),
                ),
            ownerTaskPaths = listOf(":tools:organicHiddenProbe"),
            baselinePolicy = BaselinePolicySpec(mode = BaselineMode.STRICT_ZERO_FAILURE),
            cachePolicy =
                VerificationCachePolicy(
                    buildCacheEnabled = true,
                    configurationCacheCompatible = true,
                    reuseExistingArtifacts = true,
                ),
            artifactPolicy = VerificationArtifactPolicy(),
        )

    private val contentPackDomain =
        VerificationDomainSpec(
            domainId = "content-pack",
            phaseIds = setOf("phase4"),
            workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
            defaultTier = VerificationTier.OWNER,
            nodeSpecs =
                listOf(
                    VerificationNodeSpec(
                        nodeId = "content-pack.preflight",
                        description = "Runs the Phase 4 content-pack static preflight without headless runtime execution.",
                        workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                        tier = VerificationTier.PREFLIGHT,
                        nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                        selectedClasses = listOf("com.ktome.tools.contentpack.ContentPackPreflightRunnerTest"),
                    ),
                    VerificationNodeSpec(
                        nodeId = "content-pack.owner",
                        description = "Runs the content-pack owner harness through the unified verification contract.",
                        workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
                        tier = VerificationTier.OWNER,
                        nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                        selectedClasses = listOf("com.ktome.tools.contentpack.ContentPackHarnessRunnerTest"),
                    ),
                ),
            inputScopes =
                listOf(
                    InputScope(
                        scopeId = "content-pack.sample-pack",
                        pathPrefixes = listOf("examples/content-packs/"),
                    ),
                    InputScope(
                        scopeId = "content-pack.fixtures",
                        pathPrefixes = listOf("tools/src/main/resources/fixtures/content-packs/"),
                    ),
                    InputScope(
                        scopeId = "content-pack.runtime",
                        pathPrefixes =
                            listOf(
                                "game/src/main/kotlin/com/ktome/game/contentpack/",
                                "tools/src/main/kotlin/com/ktome/tools/contentpack/",
                        ),
                        ownerRequired = true,
                        requestedPreflightTaskPaths = listOf(":tools:verifyContentPackPreflight"),
                    ),
                ),
            preflightTaskPaths = listOf(":tools:verifyContentPackPreflight"),
            ownerTaskPaths = listOf(":tools:contentPackHarness", ":tools:whiteBoxContentPack"),
            baselinePolicy = BaselinePolicySpec(mode = BaselineMode.STRICT_ZERO_FAILURE),
            cachePolicy =
                VerificationCachePolicy(
                    buildCacheEnabled = true,
                    configurationCacheCompatible = true,
                    reuseExistingArtifacts = true,
                ),
            artifactPolicy = VerificationArtifactPolicy(),
        )

    private val mapgenDomain =
        VerificationDomainSpec(
            domainId = "mapgen",
            phaseIds = setOf("phase4"),
            workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
            defaultTier = VerificationTier.OWNER,
            nodeSpecs =
                listOf(
                    VerificationNodeSpec(
                        nodeId = "mapgen.owner",
                        description = "Runs the white-box Phase 4 mapgen owner domain through the unified verification contract.",
                        workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
                        tier = VerificationTier.OWNER,
                        nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                        selectedClasses = listOf("com.ktome.tools.mapgen.WhiteBoxMapgenHarnessTest"),
                    ),
                    VerificationNodeSpec(
                        nodeId = "mapgen.full",
                        description = "Runs the full Phase 4 mapgen smoke corpus as the upstream kernel for owner/report consumers.",
                        workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
                        tier = VerificationTier.FULL,
                        nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                        selectedClasses = listOf("com.ktome.tools.mapgen.MapgenSmokeHarnessTest"),
                    ),
                ),
            inputScopes =
                listOf(
                    InputScope(
                        scopeId = "mapgen.runtime",
                        pathPrefixes =
                            listOf(
                                "tools/src/main/kotlin/com/ktome/tools/mapgen/MapgenSmokeRunner.kt",
                                "tools/src/main/kotlin/com/ktome/tools/mapgen/WhiteBoxMapgenRunner.kt",
                                "tools/src/test/kotlin/com/ktome/tools/mapgen/WhiteBoxMapgenHarnessTest.kt",
                            ),
                        ownerRequired = true,
                    ),
                ),
            ownerTaskPaths = listOf(":tools:whiteBoxMapgen"),
            cachePolicy =
                VerificationCachePolicy(
                    buildCacheEnabled = true,
                    configurationCacheCompatible = true,
                    reuseExistingArtifacts = true,
                ),
            artifactPolicy = VerificationArtifactPolicy(),
        )

    private val solvabilityDomain =
        VerificationDomainSpec(
            domainId = "solvability",
            phaseIds = setOf("phase4"),
            workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
            defaultTier = VerificationTier.OWNER,
            nodeSpecs =
                listOf(
                    VerificationNodeSpec(
                        nodeId = "solvability.owner",
                        description = "Runs the white-box Phase 4 solvability owner domain through the unified verification contract.",
                        workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
                        tier = VerificationTier.OWNER,
                        nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                        selectedClasses = listOf("com.ktome.tools.mapgen.WhiteBoxSolvabilityHarnessTest"),
                    ),
                    VerificationNodeSpec(
                        nodeId = "solvability.full",
                        description = "Runs the full Phase 4 solvability proof corpus as the upstream kernel for owner/report consumers.",
                        workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
                        tier = VerificationTier.FULL,
                        nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                        selectedClasses = listOf("com.ktome.tools.mapgen.SolvabilityHarnessRunnerTest"),
                    ),
                ),
            inputScopes =
                listOf(
                    InputScope(
                        scopeId = "solvability.runtime",
                        pathPrefixes =
                            listOf(
                                "tools/src/main/kotlin/com/ktome/tools/mapgen/SolvabilityHarnessRunner.kt",
                                "tools/src/main/kotlin/com/ktome/tools/mapgen/WhiteBoxSolvabilityRunner.kt",
                                "tools/src/test/kotlin/com/ktome/tools/mapgen/WhiteBoxSolvabilityHarnessTest.kt",
                            ),
                        ownerRequired = true,
                    ),
                ),
            ownerTaskPaths = listOf(":tools:whiteBoxSolvability"),
            cachePolicy =
                VerificationCachePolicy(
                    buildCacheEnabled = true,
                    configurationCacheCompatible = true,
                    reuseExistingArtifacts = true,
                ),
            artifactPolicy = VerificationArtifactPolicy(),
        )

    private val terrainDomain =
        VerificationDomainSpec(
            domainId = "terrain",
            phaseIds = setOf("phase4"),
            workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
            defaultTier = VerificationTier.OWNER,
            nodeSpecs =
                listOf(
                    VerificationNodeSpec(
                        nodeId = "terrain.owner",
                        description = "Runs the Phase 4 terrain owner harness through the unified verification contract.",
                        workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
                        tier = VerificationTier.OWNER,
                        nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                        selectedClasses = listOf("com.ktome.game.harness.TerrainInteractionBatchTest"),
                    ),
                ),
            inputScopes =
                listOf(
                    InputScope(
                        scopeId = "terrain.runtime",
                        pathPrefixes =
                            listOf(
                                "game/src/test/kotlin/com/ktome/game/harness/TerrainInteractionBatchTest.kt",
                                "tools/src/main/kotlin/com/ktome/tools/phase4/Phase4DomainArtifactRegistry.kt",
                            ),
                        ownerRequired = true,
                    ),
                    InputScope(
                        scopeId = "terrain.owner-evaluation",
                        pathPrefixes =
                            listOf(
                                Phase4OwnerBaselineRegistry.TERRAIN_UNIFIED_BASELINE_RELATIVE_PATH,
                                Phase4OwnerBaselineRegistry.TERRAIN_PER_ZONE_BASELINE_RELATIVE_PATH,
                            ) + phase4CanonicalReportSharedPaths,
                        requestedTaskPaths = listOf(":tools:reportPhase4Only"),
                    ),
                ),
            ownerTaskPaths = listOf(":tools:terrainInteractionBatch"),
            baselinePolicy =
                BaselinePolicySpec(
                    mode = BaselineMode.RELATIVE_BASELINE,
                    baselinePath = "docs/review/phase4/opt/baselines/2026-04-09-opt-pr01-terrain-metrics-baseline-unified.json",
                ),
            cachePolicy =
                VerificationCachePolicy(
                    buildCacheEnabled = true,
                    configurationCacheCompatible = true,
                    reuseExistingArtifacts = true,
                ),
            artifactPolicy = VerificationArtifactPolicy(),
        )

    private val bossDomain =
        VerificationDomainSpec(
            domainId = "boss",
            phaseIds = setOf("phase4"),
            workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
            defaultTier = VerificationTier.OWNER,
            nodeSpecs =
                listOf(
                    VerificationNodeSpec(
                        nodeId = "boss.owner",
                        description = "Runs the Phase 4 boss owner harness through the unified verification contract.",
                        workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
                        tier = VerificationTier.OWNER,
                        nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                        selectedClasses =
                            listOf(
                                "com.ktome.game.harness.BossHarnessTest",
                                "com.ktome.game.harness.OfficialSliceStabilityTest",
                            ),
                    ),
                ),
            inputScopes =
                listOf(
                    InputScope(
                        scopeId = "boss.runtime",
                        pathPrefixes =
                            listOf(
                                "game/src/test/kotlin/com/ktome/game/harness/BossHarnessTest.kt",
                                "game/src/test/kotlin/com/ktome/game/harness/OfficialSliceStabilityTest.kt",
                                "tools/src/main/kotlin/com/ktome/tools/phase4/Phase4DomainArtifactRegistry.kt",
                            ),
                        ownerRequired = true,
                    ),
                ),
            ownerTaskPaths = listOf(":tools:bossHarness"),
            cachePolicy =
                VerificationCachePolicy(
                    buildCacheEnabled = true,
                    configurationCacheCompatible = true,
                    reuseExistingArtifacts = true,
                ),
            artifactPolicy = VerificationArtifactPolicy(),
        )

    private val longrunDomain =
        VerificationDomainSpec(
            domainId = "longrun",
            phaseIds = setOf("phase4"),
            workloadClass = VerificationWorkloadClass.LONG_RUNNING_SYSTEM,
            defaultTier = VerificationTier.OWNER,
            nodeSpecs =
                listOf(
                    VerificationNodeSpec(
                        nodeId = "longrun.owner",
                        description = "Runs the long-run owner matrix through the shared Phase 4 verification routing contract.",
                        workloadClass = VerificationWorkloadClass.LONG_RUNNING_SYSTEM,
                        tier = VerificationTier.OWNER,
                        nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                        selectedClasses =
                            listOf(
                                "com.ktome.game.harness.LongRunLabTest",
                                "com.ktome.game.harness.LongRunLabFullTest",
                                "com.ktome.game.harness.TemplarHumanCaptainRegressionTest",
                                "com.ktome.game.harness.RogueHumanCaptainRegressionTest",
                                "com.ktome.game.harness.OfficialSliceStabilityTest",
                            ),
                    ),
                ),
            inputScopes =
                listOf(
                    InputScope(
                        scopeId = "longrun.runtime",
                        pathPrefixes =
                            listOf(
                                "game/src/test/kotlin/com/ktome/game/harness/LongRunKernelCache.kt",
                                "game/src/test/kotlin/com/ktome/game/harness/LongRunLabSeedBank.kt",
                                "game/src/test/kotlin/com/ktome/game/harness/LongRunLabTest.kt",
                                "game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt",
                                "game/src/test/kotlin/com/ktome/game/harness/TemplarHumanCaptainRegressionTest.kt",
                                "game/src/test/kotlin/com/ktome/game/harness/RogueHumanCaptainRegressionTest.kt",
                                "game/src/test/kotlin/com/ktome/game/harness/OfficialSliceStabilityTest.kt",
                            ),
                        ownerRequired = true,
                    ),
                    InputScope(
                        scopeId = "longrun.owner-evaluation",
                        pathPrefixes =
                            listOf(
                                Phase4OwnerBaselineRegistry.TERMINAL_BUILD_BASELINE_RELATIVE_PATH,
                                Phase4OwnerBaselineRegistry.CRITICAL_PATH_PACING_BASELINE_RELATIVE_PATH,
                            ) + phase4CanonicalReportSharedPaths,
                        requestedTaskPaths = listOf(":tools:reportPhase4Only"),
                    ),
                ),
            ownerTaskPaths = listOf(":game:longRunLab"),
            cachePolicy =
                VerificationCachePolicy(
                    buildCacheEnabled = true,
                    configurationCacheCompatible = true,
                    reuseExistingArtifacts = true,
                ),
            artifactPolicy = VerificationArtifactPolicy(),
        )

    private val scopeCoverageDomain =
        VerificationDomainSpec(
            domainId = "scopeCoverage",
            phaseIds = setOf("phase4"),
            workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
            defaultTier = VerificationTier.PREFLIGHT,
            nodeSpecs =
                listOf(
                    VerificationNodeSpec(
                        nodeId = "scopeCoverage.lint",
                        description = "Checks that Phase 4 impact scopes and false-negative fallbacks still cover critical shared entry points.",
                        workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                        tier = VerificationTier.PREFLIGHT,
                        nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                        selectedClasses = listOf("com.ktome.tools.verification.ScopeCoverageLintTest"),
                    ),
                ),
            preflightTaskPaths = listOf(":tools:scopeCoverageLint"),
            baselinePolicy = BaselinePolicySpec(mode = BaselineMode.STRICT_ZERO_FAILURE),
            cachePolicy =
                VerificationCachePolicy(
                    buildCacheEnabled = true,
                    configurationCacheCompatible = true,
                    reuseExistingArtifacts = true,
                ),
            artifactPolicy = VerificationArtifactPolicy(),
        )

    private val domainsById: Map<String, VerificationDomainSpec> =
        listOf(
            contractLintDomain,
            maintainabilityDomain,
            lootDomain,
            hiddenDomain,
            organicHiddenDomain,
            contentPackDomain,
            mapgenDomain,
            solvabilityDomain,
            terrainDomain,
            bossDomain,
            longrunDomain,
            scopeCoverageDomain,
        ).associateBy { it.domainId }

    fun spec(domainId: String): VerificationDomainSpec =
        domainsById[domainId] ?: error("No verification domain registered for $domainId.")

    fun registeredDomainIds(): Set<String> = domainsById.keys

    fun phaseOwnerTaskIds(phaseId: String): Set<String> =
        domainsById.values
            .asSequence()
            .filter { spec -> phaseId in spec.phaseIds }
            .flatMap { spec -> spec.ownerTaskPaths.asSequence() }
            .map(::taskIdForPath)
            .toSet()

    fun registeredImpactSpecs(): List<VerificationDomainSpec> =
        domainsById.values.filter { spec -> spec.inputScopes.isNotEmpty() || spec.preflightTaskPaths.isNotEmpty() || spec.ownerTaskPaths.isNotEmpty() }

    private fun taskIdForPath(taskPath: String): String = taskPath.substringAfterLast(':')
}
