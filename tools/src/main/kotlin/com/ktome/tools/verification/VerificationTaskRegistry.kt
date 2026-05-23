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

    private val keywordRegistryDomain =
        VerificationDomainSpec(
            domainId = "keywordRegistry",
            phaseIds = setOf("phase4"),
            workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
            defaultTier = VerificationTier.OWNER,
            nodeSpecs =
                listOf(
                    VerificationNodeSpec(
                        nodeId = "keywordRegistry.lint",
                        description = "Validates DescriptionPresenter keyword consumers against the core KeywordRegistry authority.",
                        workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                        tier = VerificationTier.OWNER,
                        nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                        selectedClasses = listOf("com.ktome.tools.lint.KeywordRegistryLintTest"),
                    ),
                ),
            inputScopes =
                listOf(
                    InputScope(
                        scopeId = "keyword-registry.authority",
                        pathPrefixes =
                            listOf(
                                "core/src/main/kotlin/com/ktome/core/talent/KeywordRegistry.kt",
                                "core/src/main/kotlin/com/ktome/core/talent/DescriptionModel.kt",
                                "client/src/main/kotlin/com/ktome/client/ui/talent/DescriptionPresenter.kt",
                                "client/src/main/kotlin/com/ktome/client/ui/inspect/ExplainPaneModel.kt",
                                "client/src/main/kotlin/com/ktome/client/ui/status/StatusPresentationModel.kt",
                                "game/src/main/resources/data/talents/",
                                "game/src/main/resources/i18n/",
                            ),
                        ownerRequired = true,
                    ),
                ),
            ownerTaskPaths = listOf(":tools:keywordRegistryLint"),
            baselinePolicy = BaselinePolicySpec(mode = BaselineMode.STRICT_ZERO_FAILURE),
            cachePolicy =
                VerificationCachePolicy(
                    buildCacheEnabled = true,
                    configurationCacheCompatible = true,
                    reuseExistingArtifacts = true,
                ),
            artifactPolicy = VerificationArtifactPolicy(),
        )

    private val clientUiEvidenceDomain =
        VerificationDomainSpec(
            domainId = "client-ui-evidence",
            phaseIds = setOf("dark-uiux"),
            workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
            defaultTier = VerificationTier.OWNER,
            nodeSpecs =
                listOf(
                    VerificationNodeSpec(
                        nodeId = "client-ui-evidence.golden-smoke",
                        description = "Runs client smoke and screenshot golden evidence for player-visible UI and renderer changes.",
                        workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
                        tier = VerificationTier.OWNER,
                        nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                        selectedTags = listOf("clientSmoke", "goldenScreenshot"),
                    ),
                ),
            inputScopes =
                listOf(
                    InputScope(
                        scopeId = "client-ui-evidence.runtime",
                        pathPrefixes =
                            listOf(
                                "client/src/main/kotlin/com/ktome/client/render/",
                                "client/src/main/kotlin/com/ktome/client/screen/",
                                "client/src/main/kotlin/com/ktome/client/ui/",
                            ),
                        ownerRequired = true,
                    ),
                    InputScope(
                        scopeId = "client-ui-evidence.tests",
                        pathPrefixes =
                            listOf(
                                "client/src/test/kotlin/com/ktome/client/golden/",
                                "client/src/test/kotlin/com/ktome/client/render/",
                                "client/src/test/kotlin/com/ktome/client/screen/",
                                "client/src/test/kotlin/com/ktome/client/ui/",
                            ),
                        ownerRequired = true,
                    ),
                ),
            ownerTaskPaths = listOf(":client:clientSmoke", ":client:goldenScreenshot"),
            baselinePolicy = BaselinePolicySpec(mode = BaselineMode.STRICT_ZERO_FAILURE),
            cachePolicy =
                VerificationCachePolicy(
                    buildCacheEnabled = true,
                    configurationCacheCompatible = true,
                    reuseExistingArtifacts = true,
                ),
            artifactPolicy = VerificationArtifactPolicy(),
        )

    private val darkUiuxPipelineDomain =
        VerificationDomainSpec(
            domainId = "dark-uiux-pipeline",
            phaseIds = setOf("dark-uiux"),
            workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
            defaultTier = VerificationTier.OWNER,
            nodeSpecs =
                listOf(
                    VerificationNodeSpec(
                        nodeId = "dark-uiux.pipeline",
                        description = "Runs the dark-v1 sprite sheet, key registry, map, and PR-00 dry-run coverage gates.",
                        workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                        tier = VerificationTier.OWNER,
                        nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                        selectedTags = listOf("darkUiuxPipeline"),
                    ),
                ),
            inputScopes =
                listOf(
                    InputScope(
                        scopeId = "dark-uiux.assets",
                        pathPrefixes =
                            listOf(
                                "UI/sprite-sheets/",
                                "assets-src/image/raw/sheets/dark-v1/",
                                "assets-src/image/contact-sheets/dark-v1/",
                                "client/src/main/resources/dark-v1/",
                            ),
                        ownerRequired = true,
                    ),
                    InputScope(
                        scopeId = "dark-uiux.manifest",
                        pathPrefixes =
                            listOf(
                                "assets-src/image/manifests/phase2-visual-manifest.json",
                                "client/src/main/resources/manifests/visual-manifest.json",
                                "assets-src/image/manifests/dark-v1-",
                            ),
                        ownerRequired = true,
                    ),
                    InputScope(
                        scopeId = "dark-uiux.pipeline-scripts",
                        pathPrefixes =
                            listOf(
                                "scripts/dark_sprite_sheet_contract.py",
                                "scripts/asset_pipeline_common.py",
                                "scripts/generate_sheet_prompt.py",
                                "scripts/codex-generate-image.py",
                                "scripts/verify_dark_key_registry.py",
                                "scripts/verify_sprite_sheet_map.py",
                                "scripts/verify_dark_manifest_coverage.py",
                                "scripts/slice_spritesheet.py",
                                "scripts/render_contact_sheet.py",
                                "scripts/manifest-lint.py",
                            ),
                        ownerRequired = true,
                    ),
                ),
            ownerTaskPaths =
                listOf(
                    ":tools:darkKeyRegistryLint",
                    ":tools:darkSpriteSheetLint",
                    ":tools:spriteSheetMapLint",
                    ":tools:darkArtRandomQa",
                    ":tools:darkManifestCoveragePr02OwnerScope",
                    ":tools:darkManifestCoveragePr02_1OwnerScope",
                    ":tools:darkManifestCoveragePr02_2OwnerScope",
                ),
            baselinePolicy = BaselinePolicySpec(mode = BaselineMode.STRICT_ZERO_FAILURE),
            cachePolicy =
                VerificationCachePolicy(
                    buildCacheEnabled = true,
                    configurationCacheCompatible = true,
                    reuseExistingArtifacts = true,
                ),
            artifactPolicy = VerificationArtifactPolicy(),
        )

    private val resourcePipelineDomain =
        VerificationDomainSpec(
            domainId = "resource-pipeline",
            phaseIds = setOf("phase4", "phase5", "dark-uiux"),
            workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
            defaultTier = VerificationTier.PREFLIGHT,
            nodeSpecs =
                listOf(
                    VerificationNodeSpec(
                        nodeId = "resource-pipeline.authority",
                        description = "Validates project-wide image, sprite-sheet, audio, manifest, and resource inventory authority.",
                        workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                        tier = VerificationTier.PREFLIGHT,
                        nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                        selectedTags = listOf("resourcePipelineLint"),
                    ),
                ),
            inputScopes =
                listOf(
                    InputScope(
                        scopeId = "resource-pipeline.image",
                        pathPrefixes =
                            listOf(
                                "assets-src/image/specs/",
                                "assets-src/image/raw/",
                                "assets-src/image/processed/",
                                "assets-src/image/contact-sheets/",
                                "assets-src/image/manifests/",
                                "client/src/main/resources/dark-v1/",
                                "client/src/main/resources/phase2/",
                                "client/src/main/resources/phase3/",
                                "client/src/main/resources/phase4/",
                            ),
                    ),
                    InputScope(
                        scopeId = "resource-pipeline.dark-sheet",
                        pathPrefixes =
                            listOf(
                                "UI/sprite-sheets/",
                                "scripts/dark_sprite_sheet_contract.py",
                                "scripts/generate_sheet_prompt.py",
                                "scripts/slice_spritesheet.py",
                                "scripts/render_contact_sheet.py",
                                "scripts/verify_dark_key_registry.py",
                                "scripts/verify_dark_manifest_coverage.py",
                                "scripts/verify_sprite_sheet_map.py",
                            ),
                    ),
                    InputScope(
                        scopeId = "resource-pipeline.audio",
                        pathPrefixes =
                            listOf(
                                "assets-src/audio/specs/",
                                "assets-src/audio/raw/",
                                "assets-src/audio/cleaned/",
                                "assets-src/audio/manifests/",
                                "client/src/main/resources/audio/",
                                "scripts/audio-lint.py",
                                "scripts/process_audio.py",
                            ),
                    ),
                    InputScope(
                        scopeId = "resource-pipeline.manifest-sync",
                        pathPrefixes =
                            listOf(
                                "client/src/main/resources/manifests/",
                                "scripts/asset-lint.py",
                                "scripts/asset_pipeline_common.py",
                                "scripts/codex-generate-image.py",
                                "scripts/manifest-lint.py",
                                "scripts/resource_pipeline_authority_lint.py",
                                "scripts/style-lint.py",
                                "scripts/sync_phase2_manifests.py",
                            ),
                    ),
                    InputScope(
                        scopeId = "resource-pipeline.production-inventory",
                        pathPrefixes =
                            listOf(
                                "client/src/main/kotlin/com/ktome/client/assets/",
                            ),
                    ),
                ),
            preflightTaskPaths = listOf(":tools:resourcePipelineLint"),
            baselinePolicy = BaselinePolicySpec(mode = BaselineMode.STRICT_ZERO_FAILURE),
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
                    InputScope(
                        scopeId = "boss.owner-evaluation",
                        pathPrefixes =
                            listOf(Phase4OwnerBaselineRegistry.BOSS_PHASE_IDENTITY_BASELINE_RELATIVE_PATH) +
                                phase4CanonicalReportSharedPaths,
                        requestedTaskPaths = listOf(":tools:reportPhase4Only"),
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
                                "game/src/main/kotlin/com/ktome/game/RouteHash.kt",
                                "game/src/main/kotlin/com/ktome/game/harness/ScenarioModels.kt",
                                "game/src/main/kotlin/com/ktome/game/harness/HeadlessRunHarness.kt",
                                "game/src/main/kotlin/com/ktome/game/harness/HarnessMetadata.kt",
                                "game/src/main/kotlin/com/ktome/game/harness/BuildIdentityAdoptionPolicy.kt",
                                "game/src/main/kotlin/com/ktome/game/loot/MilestoneRewardSelector.kt",
                                "game/src/main/kotlin/com/ktome/game/AffixBuildTags.kt",
                                "game/src/main/resources/data/build-identity/index.yaml",
                                "game/src/main/resources/data/items/index.yaml",
                                "game/src/main/resources/data/loot/index.yaml",
                                "game/src/main/resources/data/secret-zones/index.yaml",
                                "game/src/main/resources/data/events/index.yaml",
                                "game/src/main/resources/data/mapgen/zones/index.yaml",
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
            keywordRegistryDomain,
            clientUiEvidenceDomain,
            darkUiuxPipelineDomain,
            resourcePipelineDomain,
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
