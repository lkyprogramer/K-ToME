package com.ktome.tools.verification

import kotlinx.serialization.Serializable

@Serializable
data class VerificationImpactReason(
    val reasonId: String,
    val scopeId: String? = null,
    val matchedFiles: List<String> = emptyList(),
    val ownerRequired: Boolean = false,
) {
    init {
        require(reasonId.isNotBlank()) { "VerificationImpactReason.reasonId must not be blank." }
        require(scopeId == null || scopeId.isNotBlank()) { "VerificationImpactReason.scopeId must not be blank when present." }
    }
}

@Serializable
data class VerificationDomainImpact(
    val domainId: String,
    val requestedTaskPaths: List<String>,
    val reasons: List<VerificationImpactReason>,
)

@Serializable
data class VerificationImpactPlan(
    val changedFiles: List<String>,
    val requestedTaskPaths: List<String>,
    val impactedDomains: List<VerificationDomainImpact>,
    val collectionNotes: List<String> = emptyList(),
) {
    fun renderConsoleSummary(): String =
        buildString {
            appendLine("verifyChanged impact analysis")
            appendLine("changedFiles=${changedFiles.size}")
            collectionNotes.forEach { note -> appendLine("- note: $note") }
            if (changedFiles.isEmpty()) {
                appendLine("- no repo changes detected; falling back to scopeCoverageLint only")
            } else {
                changedFiles.forEach { changedFile -> appendLine("- changed: $changedFile") }
            }
            impactedDomains.forEach { impact ->
                appendLine("- domain: ${impact.domainId}")
                impact.reasons.forEach { reason ->
                    val scopeSuffix =
                        reason.scopeId?.let { scopeId -> " scope=$scopeId" }
                            ?: ""
                    val mode = if (reason.ownerRequired) "owner" else "preflight"
                    appendLine("  reason=${reason.reasonId}$scopeSuffix mode=$mode files=${reason.matchedFiles.joinToString()}")
                }
            }
            appendLine("tasks=${requestedTaskPaths.size}")
            requestedTaskPaths.forEach { taskPath -> appendLine("- task: $taskPath") }
        }.trimEnd()
}

data class VerificationFallbackRule(
    val ruleId: String,
    val pathPrefix: String,
    val domainIds: Set<String>,
    val ownerRequired: Boolean,
) {
    init {
        require(ruleId.isNotBlank()) { "VerificationFallbackRule.ruleId must not be blank." }
        require(pathPrefix.isNotBlank()) { "VerificationFallbackRule($ruleId).pathPrefix must not be blank." }
        require(domainIds.isNotEmpty()) { "VerificationFallbackRule($ruleId).domainIds must not be empty." }
        require(domainIds.none(String::isBlank)) { "VerificationFallbackRule($ruleId).domainIds must not contain blanks." }
    }

    fun matches(path: String): Boolean = InputScope.normalizePath(path).startsWith(InputScope.normalizePath(pathPrefix))
}

object VerificationImpactAnalyzer {
    private val phase4OwnerDomains: Set<String> =
        linkedSetOf(
            "mapgen",
            "solvability",
            "loot",
            "hidden",
            "content-pack",
            "terrain",
            "boss",
            "longrun",
        )

    private val phase4GameHarnessProducerDomains: Set<String> =
        linkedSetOf(
            "terrain",
            "boss",
            "longrun",
        )

    private val fallbackRules: List<VerificationFallbackRule> =
        listOf(
            VerificationFallbackRule(
                ruleId = "false-negative.core-phase4-owner",
                pathPrefix = "core/src/main/kotlin/com/ktome/core/",
                domainIds = phase4OwnerDomains,
                ownerRequired = true,
            ),
            VerificationFallbackRule(
                ruleId = "false-negative.data-loader",
                pathPrefix = "game/src/main/kotlin/com/ktome/game/data/DataLoader.kt",
                domainIds = linkedSetOf("loot", "hidden", "content-pack"),
                ownerRequired = true,
            ),
            VerificationFallbackRule(
                ruleId = "false-negative.foundation-session",
                pathPrefix = "game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt",
                domainIds = linkedSetOf("loot", "hidden", "boss", "longrun"),
                ownerRequired = true,
            ),
            VerificationFallbackRule(
                ruleId = "false-negative.headless-harness",
                pathPrefix = "game/src/main/kotlin/com/ktome/game/harness/HeadlessRunHarness.kt",
                domainIds = phase4GameHarnessProducerDomains,
                ownerRequired = true,
            ),
        )

    fun analyze(changedFiles: Collection<String>): VerificationImpactPlan {
        val normalizedChangedFiles =
            changedFiles
                .asSequence()
                .map(InputScope.Companion::normalizePath)
                .filter(String::isNotBlank)
                .distinct()
                .sorted()
                .toList()
        val impactsByDomainId = linkedMapOf<String, MutableDomainImpact>()
        val registeredDomains = VerificationTaskRegistry.registeredDomainIds().sorted()

        VerificationTaskRegistry.registeredImpactSpecs().forEach { spec ->
            spec.inputScopes.forEach { scope ->
                val matchedFiles = normalizedChangedFiles.filter(scope::matches)
                if (matchedFiles.isNotEmpty()) {
                    impactsByDomainId
                        .getOrPut(spec.domainId) { MutableDomainImpact(spec.domainId) }
                        .recordScope(scope = scope, matchedFiles = matchedFiles)
                }
            }
        }

        normalizedChangedFiles.forEach { changedFile ->
            fallbackRules
                .filter { rule -> rule.matches(changedFile) }
                .forEach { rule ->
                    rule.domainIds
                        .filter(registeredDomains::contains)
                        .forEach { domainId ->
                            impactsByDomainId
                                .getOrPut(domainId) { MutableDomainImpact(domainId) }
                                .recordFallback(
                                    reasonId = rule.ruleId,
                                    matchedFile = changedFile,
                                    ownerRequired = rule.ownerRequired,
                                )
                        }
                }
        }

        val impactedDomains =
            impactsByDomainId.values
                .sortedBy(MutableDomainImpact::domainId)
                .map { impact -> impact.freeze() }
        val requestedTaskPaths =
            buildList {
                add(":tools:scopeCoverageLint")
                impactedDomains.forEach { impact -> addAll(impact.requestedTaskPaths) }
            }.distinct()

        return VerificationImpactPlan(
            changedFiles = normalizedChangedFiles,
            requestedTaskPaths = requestedTaskPaths,
            impactedDomains = impactedDomains,
            collectionNotes = emptyList(),
        )
    }

    private data class MutableDomainImpact(
        val domainId: String,
    ) {
        private val reasons = linkedMapOf<String, MutableReason>()
        private var ownerRequired: Boolean = false

        fun recordScope(
            scope: InputScope,
            matchedFiles: List<String>,
        ) {
            ownerRequired = ownerRequired || scope.ownerRequired
            val reason =
                reasons.getOrPut("scope:${scope.scopeId}") {
                    MutableReason(
                        reasonId = "scope-match",
                        scopeId = scope.scopeId,
                        ownerRequired = scope.ownerRequired,
                    )
                }
            reason.ownerRequired = reason.ownerRequired || scope.ownerRequired
            reason.matchedFiles += matchedFiles
        }

        fun recordFallback(
            reasonId: String,
            matchedFile: String,
            ownerRequired: Boolean,
        ) {
            this.ownerRequired = this.ownerRequired || ownerRequired
            val reason =
                reasons.getOrPut("fallback:$reasonId") {
                    MutableReason(
                        reasonId = reasonId,
                        scopeId = null,
                        ownerRequired = ownerRequired,
                    )
                }
            reason.ownerRequired = reason.ownerRequired || ownerRequired
            reason.matchedFiles += matchedFile
        }

        fun freeze(): VerificationDomainImpact {
            val spec = VerificationTaskRegistry.spec(domainId)
            val requestedTaskPaths =
                buildList {
                    addAll(spec.preflightTaskPaths)
                    if (ownerRequired || spec.preflightTaskPaths.isEmpty()) {
                        addAll(spec.ownerTaskPaths)
                    }
                }.distinct()
            return VerificationDomainImpact(
                domainId = domainId,
                requestedTaskPaths = requestedTaskPaths,
                reasons =
                    reasons.values
                        .map { reason -> reason.freeze() }
                        .sortedWith(compareBy(VerificationImpactReason::reasonId, VerificationImpactReason::scopeId)),
            )
        }
    }

    private data class MutableReason(
        val reasonId: String,
        val scopeId: String?,
        var ownerRequired: Boolean,
        val matchedFiles: LinkedHashSet<String> = linkedSetOf(),
    ) {
        fun freeze(): VerificationImpactReason =
            VerificationImpactReason(
                reasonId = reasonId,
                scopeId = scopeId,
                matchedFiles = matchedFiles.toList(),
                ownerRequired = ownerRequired,
            )
    }
}
