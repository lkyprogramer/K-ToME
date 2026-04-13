package com.ktome.tools.verification

object VerificationTaskRegistry {
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
                    ),
                ),
            preflightTaskPaths = listOf(":tools:verifyLootPreflight"),
            ownerTaskPaths = listOf(":tools:lootBalanceLab"),
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
            workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
            defaultTier = VerificationTier.PREFLIGHT,
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
                        scopeId = "hidden.runtime",
                        pathPrefixes =
                            listOf(
                                "tools/src/main/kotlin/com/ktome/tools/hidden/",
                                "game/src/main/kotlin/com/ktome/game/hidden/",
                                "game/src/main/kotlin/com/ktome/game/Phase4StaticContentValidator.kt",
                            ),
                        ownerRequired = true,
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

    private val contentPackDomain =
        VerificationDomainSpec(
            domainId = "content-pack",
            phaseIds = setOf("phase4"),
            workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
            defaultTier = VerificationTier.PREFLIGHT,
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
                    ),
                ),
            preflightTaskPaths = listOf(":tools:verifyContentPackPreflight"),
            ownerTaskPaths = listOf(":tools:contentPackHarness"),
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
                        description = "Tracks Phase 4 mapgen owner verification routing.",
                        workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
                        tier = VerificationTier.OWNER,
                        nodeKind = VerificationNodeKind.REPORT_ONLY,
                    ),
                ),
            inputScopes =
                listOf(
                    InputScope(
                        scopeId = "mapgen.runtime",
                        pathPrefixes =
                            listOf(
                                "tools/src/main/kotlin/com/ktome/tools/mapgen/MapgenSmokeRunner.kt",
                                "tools/src/test/kotlin/com/ktome/tools/mapgen/MapgenSmokeHarnessTest.kt",
                            ),
                        ownerRequired = true,
                    ),
                ),
            ownerTaskPaths = listOf(":tools:mapgenSmoke"),
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
                        description = "Tracks Phase 4 solvability owner verification routing.",
                        workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
                        tier = VerificationTier.OWNER,
                        nodeKind = VerificationNodeKind.REPORT_ONLY,
                    ),
                ),
            inputScopes =
                listOf(
                    InputScope(
                        scopeId = "solvability.runtime",
                        pathPrefixes =
                            listOf(
                                "tools/src/main/kotlin/com/ktome/tools/mapgen/SolvabilityHarnessRunner.kt",
                                "tools/src/test/kotlin/com/ktome/tools/mapgen/SolvabilityHarnessRunnerTest.kt",
                                "tools/src/test/kotlin/com/ktome/tools/mapgen/SolvabilityGoldenContractTest.kt",
                            ),
                        ownerRequired = true,
                    ),
                ),
            ownerTaskPaths = listOf(":tools:solvabilityHarness"),
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
                        description = "Tracks Phase 4 terrain interaction owner verification routing.",
                        workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
                        tier = VerificationTier.OWNER,
                        nodeKind = VerificationNodeKind.REPORT_ONLY,
                    ),
                ),
            inputScopes =
                listOf(
                    InputScope(
                        scopeId = "terrain.runtime",
                        pathPrefixes = listOf("game/src/test/kotlin/com/ktome/game/harness/TerrainInteractionBatchTest.kt"),
                        ownerRequired = true,
                    ),
                ),
            ownerTaskPaths = listOf(":game:terrainInteractionBatch"),
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
                        description = "Tracks Phase 4 boss harness owner verification routing.",
                        workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
                        tier = VerificationTier.OWNER,
                        nodeKind = VerificationNodeKind.REPORT_ONLY,
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
                            ),
                        ownerRequired = true,
                    ),
                ),
            ownerTaskPaths = listOf(":game:bossHarness"),
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
                        description = "Tracks Phase 4 long-run owner verification routing.",
                        workloadClass = VerificationWorkloadClass.LONG_RUNNING_SYSTEM,
                        tier = VerificationTier.OWNER,
                        nodeKind = VerificationNodeKind.REPORT_ONLY,
                    ),
                ),
            inputScopes =
                listOf(
                    InputScope(
                        scopeId = "longrun.runtime",
                        pathPrefixes =
                            listOf(
                                "game/src/test/kotlin/com/ktome/game/harness/LongRunLabSeedBank.kt",
                                "game/src/test/kotlin/com/ktome/game/harness/LongRunLabTest.kt",
                                "game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt",
                                "game/src/test/kotlin/com/ktome/game/harness/TemplarHumanCaptainRegressionTest.kt",
                                "game/src/test/kotlin/com/ktome/game/harness/RogueHumanCaptainRegressionTest.kt",
                                "game/src/test/kotlin/com/ktome/game/harness/OfficialSliceStabilityTest.kt",
                            ),
                        ownerRequired = true,
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
            lootDomain,
            hiddenDomain,
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

    fun registeredImpactSpecs(): List<VerificationDomainSpec> =
        domainsById.values.filter { spec -> spec.inputScopes.isNotEmpty() || spec.preflightTaskPaths.isNotEmpty() || spec.ownerTaskPaths.isNotEmpty() }
}
