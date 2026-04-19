package com.ktome.tools.verification

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object VerificationCli {
    private val prettyJson: Json =
        Json {
            prettyPrint = true
            explicitNulls = false
        }

    @JvmStatic
    fun main(args: Array<String>) {
        val parsed = ParsedCommand.parse(args.toList())
        when (parsed.command) {
            "run" -> runVerification(parsed)
            "report" -> rebuildReport(parsed)
            "legacy-adapter" -> runLegacyAdapter(parsed)
            "plan-changed" -> writeChangedPlan(parsed)
            else -> error("Unsupported verification command ${parsed.command}.")
        }
    }

    private fun runVerification(parsed: ParsedCommand) {
        val domain = VerificationTaskRegistry.spec(parsed.requireDomainId())
        val tier = VerificationTier.valueOf(parsed.requireTier())
        val node = domain.resolveNode(tier = tier, nodeId = parsed.nodeId)
        val outputDir = parsed.outputDir.also { it.createDirectories() }
        val rawResult =
            when (node.nodeKind) {
                VerificationNodeKind.LEGACY_JUNIT_CLASS_SET -> LegacyJUnitClassSetExecutor.execute(domain.domainId, tier, node)
                VerificationNodeKind.REPORT_ONLY ->
                    error("VerificationTask cannot execute report-only node ${node.nodeId}; use VerificationReportTask instead.")
            }

        writeArtifacts(
            domain = domain,
            tier = tier,
            nodeId = node.nodeId,
            snapshotHash = parsed.requireSnapshotHash(),
            cacheStatus = parsed.cacheStatus,
            outputDir = outputDir,
            rawResult = rawResult,
            sourceArtifactDir = null,
            reportOnly = false,
        )
    }

    private fun rebuildReport(parsed: ParsedCommand) {
        val domain = VerificationTaskRegistry.spec(parsed.requireDomainId())
        val tier = VerificationTier.valueOf(parsed.requireTier())
        val sourceArtifactDir =
            parsed.artifactInputs.singleOrNull()
                ?: error("VerificationReportTask requires exactly one --artifact-input directory.")
        require(sourceArtifactDir.exists()) {
            "Verification report source artifact directory does not exist: $sourceArtifactDir"
        }
        val rawResultPath = sourceArtifactDir.resolve(domain.artifactPolicy.rawResultFileName)
        require(rawResultPath.exists()) {
            "Verification report source raw result is missing: $rawResultPath"
        }
        val summaryPath = sourceArtifactDir.resolve(domain.artifactPolicy.summaryFileName)
        require(summaryPath.exists()) {
            "Verification report source summary is missing: $summaryPath"
        }
        val rawResult = prettyJson.decodeFromString<LegacyJUnitRawResult>(rawResultPath.readText())
        val sourceSummary = prettyJson.decodeFromString<VerificationSummary>(summaryPath.readText())
        require(sourceSummary.domainId == domain.domainId) {
            "Verification report source summary domain mismatch: expected ${domain.domainId}, found ${sourceSummary.domainId}"
        }
        require(sourceSummary.tier == tier.name) {
            "Verification report source summary tier mismatch: expected ${tier.name}, found ${sourceSummary.tier}"
        }
        require(sourceSummary.nodeId == rawResult.nodeId) {
            "Verification report source summary node mismatch: expected ${rawResult.nodeId}, found ${sourceSummary.nodeId}"
        }
        parsed.nodeId?.let { requestedNodeId ->
            require(requestedNodeId == rawResult.nodeId) {
                "Verification report requested node mismatch: expected $requestedNodeId, found ${rawResult.nodeId}"
            }
        }
        writeArtifacts(
            domain = domain,
            tier = tier,
            nodeId = rawResult.nodeId,
            snapshotHash = sourceSummary.snapshotHash,
            cacheStatus = parsed.cacheStatus,
            outputDir = parsed.outputDir.also { it.createDirectories() },
            rawResult = rawResult,
            sourceArtifactDir = sourceArtifactDir.toString(),
            reportOnly = true,
        )
    }

    private fun runLegacyAdapter(parsed: ParsedCommand) {
        val tier = VerificationTier.valueOf(parsed.requireTier())
        val node =
            VerificationNodeSpec(
                nodeId = parsed.nodeId ?: "${parsed.requireDomainId()}.legacyAdapter",
                description = "Adapts an existing JUnit class/tag selection into verification artifacts.",
                workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                tier = tier,
                nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                selectedClasses = parsed.selectedClasses,
                selectedTags = parsed.selectedTags,
            )
        val domain =
            VerificationDomainSpec(
                domainId = parsed.requireDomainId(),
                phaseIds = setOf("migration"),
                workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                defaultTier = tier,
                nodeSpecs = listOf(node),
                cachePolicy =
                    VerificationCachePolicy(
                        buildCacheEnabled = true,
                        configurationCacheCompatible = true,
                        reuseExistingArtifacts = true,
                    ),
                artifactPolicy = VerificationArtifactPolicy(),
            )
        val rawResult = LegacyJUnitClassSetExecutor.execute(parsed.requireDomainId(), tier, node)
        writeArtifacts(
            domain = domain,
            tier = tier,
            nodeId = node.nodeId,
            snapshotHash = parsed.requireSnapshotHash(),
            cacheStatus = parsed.cacheStatus,
            outputDir = parsed.outputDir.also { it.createDirectories() },
            rawResult = rawResult,
            sourceArtifactDir = null,
            reportOnly = false,
        )
    }

    private fun writeChangedPlan(parsed: ParsedCommand) {
        val outputDir = parsed.outputDir.also { it.createDirectories() }
        val collected =
            if (parsed.changedFiles.isNotEmpty()) {
                GitChangedFileCollection(
                    changedFiles = parsed.changedFiles,
                    notes = emptyList(),
                )
            } else {
                GitChangedFileCollector.collect(
                    repoRoot = repoRoot(),
                    preferredBaseRef = parsed.baseRef,
                )
            }
        val plan =
            VerificationImpactAnalyzer
                .analyze(collected.changedFiles)
                .copy(collectionNotes = collected.notes)
        outputDir.resolve(CHANGED_PLAN_FILE_NAME).writeText(prettyJson.encodeToString(plan))
        outputDir.resolve(CHANGED_TASKS_FILE_NAME).writeText(plan.requestedTaskPaths.joinToString(separator = System.lineSeparator()))
        outputDir.resolve(CHANGED_PREFLIGHT_TASKS_FILE_NAME).writeText(
            plan.requestedPreflightTaskPaths.joinToString(separator = System.lineSeparator()),
        )
        outputDir.resolve(CHANGED_PLAN_MARKDOWN_FILE_NAME).writeText(renderChangedPlanMarkdown(plan))
        println(plan.renderConsoleSummary())
    }

    private fun writeArtifacts(
        domain: VerificationDomainSpec,
        tier: VerificationTier,
        nodeId: String,
        snapshotHash: String,
        cacheStatus: String,
        outputDir: Path,
        rawResult: LegacyJUnitRawResult,
        sourceArtifactDir: String?,
        reportOnly: Boolean,
    ) {
        val outputPaths = outputPaths(outputDir, domain.artifactPolicy, reportOnly)
        if (!reportOnly) {
            outputDir.resolve(domain.artifactPolicy.rawResultFileName).writeText(prettyJson.encodeToString(rawResult))
        }
        outputDir.resolve(domain.artifactPolicy.summaryFileName).writeText(
            prettyJson.encodeToString(
                VerificationSummary(
                    domainId = domain.domainId,
                    tier = tier.name,
                    verdict = if (rawResult.failedTests == 0) "PASS" else "FAIL",
                    snapshotHash = snapshotHash,
                    cacheStatus = cacheStatus,
                    outputPaths = outputPaths,
                    nodeId = nodeId,
                    totalTests = rawResult.totalTests,
                    failedTests = rawResult.failedTests,
                    durationMillis = rawResult.durationMillis,
                    reportOnly = reportOnly,
                ),
            ),
        )
        outputDir.resolve(domain.artifactPolicy.metadataFileName).writeText(
            prettyJson.encodeToString(
                    VerificationMetadata(
                        domainId = domain.domainId,
                        phaseIds = domain.phaseIds.sorted(),
                        workloadClass = domain.workloadClass.name,
                        declaredWorkloadClasses = domain.declaredWorkloadClasses().map(Enum<*>::name).sorted(),
                        defaultTier = domain.defaultTier.name,
                        selectedTier = tier.name,
                        nodeId = nodeId,
                        selectedNodeWorkloadClass = domain.node(nodeId).workloadClass.name,
                        baselineMode = domain.baselinePolicy?.mode?.name,
                        cachePolicy =
                            VerificationCachePolicyDescriptor(
                            buildCacheEnabled = domain.cachePolicy.buildCacheEnabled,
                            configurationCacheCompatible = domain.cachePolicy.configurationCacheCompatible,
                            reuseExistingArtifacts = domain.cachePolicy.reuseExistingArtifacts,
                        ),
                    artifactPolicy =
                        VerificationArtifactPolicyDescriptor(
                            rawResultFileName = domain.artifactPolicy.rawResultFileName,
                            summaryFileName = domain.artifactPolicy.summaryFileName,
                            metadataFileName = domain.artifactPolicy.metadataFileName,
                        ),
                    sourceArtifactDir = sourceArtifactDir,
                ),
            ),
        )
        require(rawResult.failedTests == 0) {
            "Verification node $nodeId recorded ${rawResult.failedTests} failing tests; inspect ${outputDir.resolve(domain.artifactPolicy.summaryFileName)}."
        }
    }

    private fun outputPaths(
        outputDir: Path,
        artifactPolicy: VerificationArtifactPolicy,
        reportOnly: Boolean,
    ): Map<String, String> {
        val paths = linkedMapOf<String, String>()
        if (!reportOnly) {
            paths["rawResult"] = outputDir.resolve(artifactPolicy.rawResultFileName).toString()
        }
        paths["summary"] = outputDir.resolve(artifactPolicy.summaryFileName).toString()
        paths["metadata"] = outputDir.resolve(artifactPolicy.metadataFileName).toString()
        return paths
    }

    private fun renderChangedPlanMarkdown(plan: VerificationImpactPlan): String =
        buildString {
            appendLine("# verifyChanged Plan")
            appendLine()
            if (plan.collectionNotes.isNotEmpty()) {
                appendLine("## Collection Notes")
                plan.collectionNotes.forEach { note -> appendLine("- $note") }
                appendLine()
            }
            appendLine("## Changed Files")
            if (plan.changedFiles.isEmpty()) {
                appendLine("- none")
            } else {
                plan.changedFiles.forEach { changedFile -> appendLine("- `$changedFile`") }
            }
            appendLine()
            appendLine("## Impacted Domains")
            if (plan.impactedDomains.isEmpty()) {
                appendLine("- none")
            } else {
                plan.impactedDomains.forEach { impact ->
                    appendLine("- `${impact.domainId}`")
                    impact.reasons.forEach { reason ->
                        val scopeSuffix =
                            reason.scopeId?.let { scopeId -> " (`$scopeId`)" }
                                ?: ""
                        val mode = if (reason.ownerRequired) "owner" else "preflight"
                        appendLine("  - `$mode` via `${reason.reasonId}`$scopeSuffix: ${reason.matchedFiles.joinToString()}")
                    }
                }
            }
            appendLine()
            appendLine("## Requested Tasks")
            plan.requestedTaskPaths.forEach { taskPath -> appendLine("- `$taskPath`") }
            appendLine()
            appendLine("## Requested Preflight Tasks")
            plan.requestedPreflightTaskPaths.forEach { taskPath -> appendLine("- `$taskPath`") }
        }

    private data class ParsedCommand(
        val command: String,
        val domainId: String?,
        val tier: String?,
        val nodeId: String?,
        val snapshotHash: String?,
        val outputDir: Path,
        val cacheStatus: String,
        val artifactInputs: List<Path>,
        val selectedClasses: List<String>,
        val selectedTags: List<String>,
        val changedFiles: List<String>,
        val baseRef: String?,
    ) {
        companion object {
            private const val DEFAULT_CACHE_STATUS: String = "LOCAL_EXECUTION"

            fun parse(arguments: List<String>): ParsedCommand {
                require(arguments.isNotEmpty()) { "VerificationCli requires a command." }
                val command = arguments.first()
                val values = mutableMapOf<String, MutableList<String>>()
                var index = 1
                while (index < arguments.size) {
                    val key = arguments[index]
                    require(key.startsWith("--")) { "Unexpected CLI token $key." }
                    val value = arguments.getOrNull(index + 1) ?: error("Missing value for $key.")
                    values.getOrPut(key) { mutableListOf() }.add(value)
                    index += 2
                }
                return ParsedCommand(
                    command = command,
                    domainId =
                        when (command) {
                            "plan-changed" -> values["--domain"]?.singleOrNull()
                            else -> values.singleValue("--domain")
                        },
                    tier =
                        when (command) {
                            "plan-changed" -> values["--tier"]?.singleOrNull()
                            else -> values.singleValue("--tier")
                        },
                    nodeId = values["--node-id"]?.singleOrNull(),
                    snapshotHash = values["--snapshot"]?.singleOrNull(),
                    outputDir = Path.of(values.singleValue("--output-dir")),
                    cacheStatus = values["--cache-status"]?.singleOrNull() ?: DEFAULT_CACHE_STATUS,
                    artifactInputs = values["--artifact-input"].orEmpty().map(Path::of),
                    selectedClasses = values["--select-class"].orEmpty().sorted(),
                    selectedTags = values["--select-tag"].orEmpty().sorted(),
                    changedFiles = values["--changed-file"].orEmpty().sorted(),
                    baseRef = values["--base-ref"]?.singleOrNull(),
                )
            }

            private fun Map<String, MutableList<String>>.singleValue(key: String): String =
                get(key)?.singleOrNull() ?: error("Expected exactly one value for $key.")
        }

        fun requireSnapshotHash(): String =
            snapshotHash ?: error("Expected exactly one value for --snapshot.")

        fun requireDomainId(): String =
            domainId ?: error("Expected exactly one value for --domain.")

        fun requireTier(): String =
            tier ?: error("Expected exactly one value for --tier.")
    }

    private const val CHANGED_PLAN_FILE_NAME: String = "verify-changed-plan.json"
    private const val CHANGED_TASKS_FILE_NAME: String = "task-paths.txt"
    private const val CHANGED_PREFLIGHT_TASKS_FILE_NAME: String = "preflight-task-paths.txt"
    private const val CHANGED_PLAN_MARKDOWN_FILE_NAME: String = "verify-changed-plan.md"

    private fun repoRoot(): Path =
        System.getProperty("ktome.repo.root")
            ?.let(Path::of)
            ?: Path.of("").toAbsolutePath().normalize()
}
