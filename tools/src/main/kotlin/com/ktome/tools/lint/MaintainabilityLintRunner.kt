package com.ktome.tools.lint

import com.ktome.tools.verification.BaselineMode
import com.ktome.tools.verification.EvaluationEntryStatus
import com.ktome.tools.verification.EvaluationResult
import com.ktome.tools.verification.EvaluationVerdict
import com.ktome.tools.verification.VerificationBaseline
import com.ktome.tools.verification.VerificationBaselineComparator
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val MAINTAINABILITY_DOMAIN_ID: String = "maintainability"
private const val BASELINE_FILE_NAME: String = "maintainability-baseline.json"
private const val SUMMARY_FILE_NAME: String = "summary.json"
private const val FINDINGS_FILE_NAME: String = "findings.json"
private const val REPORT_FILE_NAME: String = "report.md"
private val packageDeclarationRegex: Regex = Regex("""(?m)^[ \t]*package\s+([A-Za-z_][A-Za-z0-9_.]*)\s*$""")
private val importDeclarationRegex: Regex =
    Regex("""(?m)^[ \t]*import\s+([A-Za-z_][A-Za-z0-9_.*`]+)(?:\s+as\s+([A-Za-z_][A-Za-z0-9_]*))?\s*$""")
private const val DECLARATION_ANNOTATION_PREFIX: String =
    """(?:@[A-Za-z_][A-Za-z0-9_.]*(?:\([^)\n]*\))?[ \t]*)*"""
private const val DECLARATION_MODIFIER_PATTERN: String =
    """(?:public|internal|private|protected|sealed|data|value|annotation|abstract|open|final|enum|expect|actual|companion|[ \t])*"""

private val topLevelContainerDeclarationRegex: Regex =
    Regex(
        pattern =
            """(?m)^[ \t]*$DECLARATION_ANNOTATION_PREFIX(?<modifiers>$DECLARATION_MODIFIER_PATTERN)\b(?:class|interface|object)\s+(?<name>[A-Za-z_][A-Za-z0-9_]*)\b""",
    )

private val maintainabilityJson: Json =
    Json {
        prettyPrint = true
        explicitNulls = false
    }

@Serializable
data class MaintainabilityFinding(
    val findingId: String,
    val taxonomy: String,
    val ruleId: String,
    val severity: String,
    val status: String,
    val filePath: String,
    val line: Int,
    val symbol: String? = null,
    val summary: String,
    val currentValueText: String,
    val details: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class MaintainabilityLintSummary(
    val domainId: String,
    val baselineId: String,
    val metricDefinitionVersion: String,
    val gateMode: String,
    val verdict: String,
    val findingCount: Int,
    val approvedDebtCount: Int,
    val unexpectedRegressionCount: Int,
    val improvedDebtCount: Int,
    val taxonomyCounts: Map<String, Int>,
    val severityCounts: Map<String, Int>,
    val newFindingIds: List<String>,
    val clearedFindingIds: List<String>,
    val evaluation: EvaluationResult,
)

data class MaintainabilityLintRun(
    val summaryPath: Path,
    val findingsPath: Path,
    val reportPath: Path,
    val summary: MaintainabilityLintSummary,
    val findings: List<MaintainabilityFinding>,
)

internal object MaintainabilityLintRunner {
    private val sourceRoots: List<String> =
        listOf(
            "core/src/main/kotlin",
            "game/src/main/kotlin",
            "client/src/main/kotlin",
            "tools/src/main/kotlin",
            "build-logic/src/main/kotlin",
        )

    private val helperLikeSuffixes: Set<String> = setOf("Helper", "Utils", "Manager")
    private val explicitHelperLikeAllowlist: Set<String> = emptySet()
    private val temporaryKeywordPattern: Regex = Regex("""(?i)\b(?:TODO|TEMP|HACK|compat)\b""")
    private val debtPattern: Regex = Regex("""(?i)\bdebt\([^)]+\)""")
    private val deletionConditionPattern: Regex =
        Regex("""(?i)\b(?:remove|delete|drop)\s+(?:when|after)\b|\bexpires?\b|\bsunset\b""")
    private val helperTypeDeclarationRegex: Regex =
        Regex(
            pattern =
                """(?m)^[ \t]*$DECLARATION_ANNOTATION_PREFIX$DECLARATION_MODIFIER_PATTERN\b(?:class|interface|object)\s+([A-Za-z_][A-Za-z0-9_]*)\b""",
        )
    private val declarationRegex: Regex =
        Regex(
            pattern =
                """(?m)^[ \t]*$DECLARATION_ANNOTATION_PREFIX(?:public|internal|private|protected|override|open|abstract|suspend|operator|infix|inline|tailrec|external|final|actual|expect|sealed|data|value|enum|annotation|companion|[ \t])*\b(fun|class|interface|object)\b""",
        )

    fun run(): MaintainabilityLintRun {
        val repoRoot = repoRoot()
        val outputDir = reportDir()
        outputDir.createDirectories()
        val gateMode = gateMode()

        val baseline = VerificationBaseline.read(baselinePath(repoRoot))
        require(baseline.domainId == MAINTAINABILITY_DOMAIN_ID) {
            "Maintainability baseline domainId must be '$MAINTAINABILITY_DOMAIN_ID', found '${baseline.domainId}'."
        }
        require(baseline.mode == BaselineMode.APPROVED_DEBT_SET) {
            "Maintainability baseline must use APPROVED_DEBT_SET, found ${baseline.mode}."
        }

        val findings = collectFindings(repoRoot)
        val findingIds = findings.map(DetectedMaintainabilityFinding::findingId).toSet()
        val numericValues = findings.mapNotNull(DetectedMaintainabilityFinding::numericValueEntry).toMap()
        val currentValueTexts = findings.associate { finding -> finding.findingId to finding.currentValueText }
        val currentValueElements = findings.associate { finding -> finding.findingId to finding.currentValueElement }
        val detailsByMetricId = findings.associate { finding -> finding.findingId to finding.details }

        val evaluation =
            VerificationBaselineComparator.compareApprovedDebtSet(
                domainId = MAINTAINABILITY_DOMAIN_ID,
                evaluationId = "maintainability.current",
                baseline = baseline,
                actualDebtKeys = findingIds,
                actualDebtValues = numericValues,
                currentValueTexts = currentValueTexts,
                currentValueElements = currentValueElements,
                detailsByMetricId = detailsByMetricId,
            )

        val statusByFindingId =
            evaluation.entries
                .filterNot { entry -> entry.status == EvaluationEntryStatus.IMPROVEMENT }
                .associate { entry -> entry.metricId to entry.status.name }

        val renderedFindings =
            findings.map { finding ->
                MaintainabilityFinding(
                    findingId = finding.findingId,
                    taxonomy = finding.taxonomy,
                    ruleId = finding.ruleId,
                    severity = finding.severity,
                    status = requireNotNull(statusByFindingId[finding.findingId]) {
                        "Missing evaluation status for maintainability finding ${finding.findingId}."
                    },
                    filePath = finding.filePath,
                    line = finding.line,
                    symbol = finding.symbol,
                    summary = finding.summary,
                    currentValueText = finding.currentValueText,
                    details = finding.details,
                )
            }

        val summary =
            MaintainabilityLintSummary(
                domainId = MAINTAINABILITY_DOMAIN_ID,
                baselineId = requireNotNull(evaluation.baselineId) { "Maintainability evaluation baselineId must not be null." },
                metricDefinitionVersion =
                    requireNotNull(evaluation.metricDefinitionVersion) {
                        "Maintainability evaluation metricDefinitionVersion must not be null."
                    },
                gateMode = gateMode.name,
                verdict = evaluation.verdict.name,
                findingCount = renderedFindings.size,
                approvedDebtCount = evaluation.approvedDebtCount,
                unexpectedRegressionCount = evaluation.unexpectedRegressionCount,
                improvedDebtCount = evaluation.improvedDebtCount,
                taxonomyCounts = renderedFindings.groupingBy(MaintainabilityFinding::taxonomy).eachCount().toSortedMap(),
                severityCounts = renderedFindings.groupingBy(MaintainabilityFinding::severity).eachCount().toSortedMap(),
                newFindingIds =
                    evaluation.entries
                        .filter { entry -> entry.status == EvaluationEntryStatus.UNEXPECTED_REGRESSION }
                        .map { entry -> entry.metricId },
                clearedFindingIds =
                    evaluation.entries
                        .filter { entry -> entry.status == EvaluationEntryStatus.IMPROVEMENT }
                        .map { entry -> entry.metricId },
                evaluation = evaluation,
            )

        val summaryPath = outputDir.resolve(SUMMARY_FILE_NAME)
        val findingsPath = outputDir.resolve(FINDINGS_FILE_NAME)
        val reportPath = outputDir.resolve(REPORT_FILE_NAME)
        Files.writeString(summaryPath, maintainabilityJson.encodeToString(summary))
        Files.writeString(findingsPath, maintainabilityJson.encodeToString(renderedFindings))
        Files.writeString(reportPath, renderMarkdown(summary, renderedFindings))

        if (gateMode == MaintainabilityGateMode.BLOCKING && evaluation.verdict == EvaluationVerdict.FAIL) {
            error(
                "Maintainability lint failed in blocking mode with ${evaluation.unexpectedRegressionCount} unexpected regressions. " +
                    "See ${summaryPath.toAbsolutePath()} and ${reportPath.toAbsolutePath()}.",
            )
        }

        return MaintainabilityLintRun(
            summaryPath = summaryPath,
            findingsPath = findingsPath,
            reportPath = reportPath,
            summary = summary,
            findings = renderedFindings,
        )
    }

    internal fun collectFindings(repoRoot: Path = repoRoot()): List<DetectedMaintainabilityFinding> =
        sourceRoots
            .asSequence()
            .map(repoRoot::resolve)
            .filter(Path::isDirectory)
            .flatMap(::collectKotlinFiles)
            .map { file -> SourceFileContext.from(repoRoot, file) }
            .toList()
            .let { sourceFiles ->
                val referenceIndex = CrossFileRuntimeReferenceIndex.build(sourceFiles)
                sourceFiles.asSequence().flatMap { sourceFile -> scanFile(sourceFile, referenceIndex).asSequence() }
            }
            .sortedWith(
                compareBy(
                    DetectedMaintainabilityFinding::taxonomy,
                    DetectedMaintainabilityFinding::filePath,
                    DetectedMaintainabilityFinding::line,
                    DetectedMaintainabilityFinding::findingId,
                ),
            ).toList()

    private fun collectKotlinFiles(root: Path): Sequence<Path> =
        Files.walk(root).use { walked ->
            walked
                .filter(Files::isRegularFile)
                .filter { file -> file.fileName.toString().endsWith(".kt") }
                .map(Path::normalize)
                .toList()
                .asSequence()
        }

    private fun scanFile(
        sourceFile: SourceFileContext,
        referenceIndex: CrossFileRuntimeReferenceIndex,
    ): List<DetectedMaintainabilityFinding> {
        val findings = mutableListOf<DetectedMaintainabilityFinding>()

        findings += detectHelperLikeTypes(sourceFile.relativePath, sourceFile.parsedSource, sourceFile.lineIndex)
        findings += detectFunctionSignatureFindings(sourceFile, referenceIndex)
        findings += detectTemporaryPathFindings(sourceFile.relativePath, sourceFile.parsedSource.comments)

        return findings
    }

    private fun detectHelperLikeTypes(
        relativePath: String,
        parsedSource: ParsedKotlinSource,
        lineIndex: LineIndex,
    ): List<DetectedMaintainabilityFinding> =
        helperTypeDeclarationRegex
            .findAll(parsedSource.sanitizedCode)
            .mapNotNull { match ->
                if (parsedSource.braceDepthAt(match.range.first) > 1) {
                    return@mapNotNull null
                }
                val typeName = match.groupValues[1]
                if (helperLikeSuffixes.none(typeName::endsWith)) {
                    return@mapNotNull null
                }
                val allowlistKey = "$relativePath:$typeName"
                if (allowlistKey in explicitHelperLikeAllowlist) {
                    return@mapNotNull null
                }
                val line = lineIndex.lineNumberAt(match.range.first)
                DetectedMaintainabilityFinding(
                    findingId = "helper-sprawl:$relativePath:$typeName",
                    taxonomy = "helper-sprawl",
                    ruleId = "helper-like-type",
                    severity = "P2",
                    filePath = relativePath,
                    line = line,
                    symbol = typeName,
                    summary = "Type $typeName uses helper-like naming without an explicit infrastructural allowlist.",
                    currentValueText = typeName,
                    currentValueElement = JsonPrimitive(typeName),
                    details =
                        buildJsonObject {
                            put("typeName", typeName)
                        },
                )
            }.toList()

    private fun detectFunctionSignatureFindings(
        sourceFile: SourceFileContext,
        referenceIndex: CrossFileRuntimeReferenceIndex,
    ): List<DetectedMaintainabilityFinding> {
        val findings = mutableListOf<DetectedMaintainabilityFinding>()
        declarationRegex
            .findAll(sourceFile.parsedSource.sanitizedCode)
            .forEach { match ->
                if (match.groupValues[1] != "fun") {
                    return@forEach
                }
                if (sourceFile.parsedSource.braceDepthAt(match.range.first) > 1) {
                    return@forEach
                }
                val signature =
                    parseFunctionSignature(sourceFile.parsedSource, match.range.first)
                        ?: return@forEach
                if (signature.visibility == FunctionVisibility.NON_API) {
                    return@forEach
                }
                if (signature.visibility == FunctionVisibility.INTERNAL &&
                    !referenceIndex.hasCrossFileProductionReference(
                        sourceFile = sourceFile,
                        signature = signature,
                    )
                ) {
                    return@forEach
                }
                val line = sourceFile.lineIndex.lineNumberAt(match.range.first)
                if (signature.booleanParameters.isNotEmpty()) {
                    signature.booleanParameters.forEach { parameter ->
                        findings +=
                            DetectedMaintainabilityFinding(
                                findingId = "option-sprawl:boolean:${sourceFile.relativePath}:${signature.signatureId}:$parameter",
                                taxonomy = "option-sprawl",
                                ruleId = "boolean-parameter",
                                severity = "P1",
                                filePath = sourceFile.relativePath,
                                line = line,
                                symbol = signature.displayName,
                                summary = "Runtime API ${signature.displayName} exposes Boolean parameter '$parameter'.",
                                currentValueText = parameter,
                                currentValueElement = JsonPrimitive(parameter),
                                details =
                                    buildJsonObject {
                                        put("functionName", signature.name)
                                        put("parameterName", parameter)
                                        put("parameterCount", signature.parameters.size)
                                        put("visibility", signature.visibility.name.lowercase())
                                    },
                                numericValue = 1.0,
                            )
                    }
                }
                if (signature.parameters.size > 4) {
                    findings +=
                        DetectedMaintainabilityFinding(
                            findingId = "option-sprawl:parameter-matrix:${sourceFile.relativePath}:${signature.signatureId}",
                            taxonomy = "option-sprawl",
                            ruleId = "parameter-count",
                            severity = "P2",
                            filePath = sourceFile.relativePath,
                            line = line,
                            symbol = signature.displayName,
                            summary = "Runtime API ${signature.displayName} declares ${signature.parameters.size} parameters; prefer a typed request or config model.",
                            currentValueText = signature.parameters.size.toString(),
                            currentValueElement = JsonPrimitive(signature.parameters.size),
                            details =
                                buildJsonObject {
                                    put("functionName", signature.name)
                                    put("parameterCount", signature.parameters.size)
                                    put("visibility", signature.visibility.name.lowercase())
                                    put("parameterNames", signature.parameters.joinToString())
                                },
                            numericValue = signature.parameters.size.toDouble(),
                        )
                }
            }
        return findings
    }

    private fun detectTemporaryPathFindings(
        relativePath: String,
        comments: List<ExtractedComment>,
    ): List<DetectedMaintainabilityFinding> =
        comments.mapNotNull { comment ->
            if (!temporaryKeywordPattern.containsMatchIn(comment.text)) {
                return@mapNotNull null
            }
            if (debtPattern.containsMatchIn(comment.text) && deletionConditionPattern.containsMatchIn(comment.text)) {
                return@mapNotNull null
            }
            val normalizedText = comment.text.replace(Regex("""\s+"""), " ").trim()
            DetectedMaintainabilityFinding(
                findingId = "temp-path-without-expiry:$relativePath:${comment.line}:${shortHash(normalizedText)}",
                taxonomy = "temp-path-without-expiry",
                ruleId = "temporary-path",
                severity = "P1",
                filePath = relativePath,
                line = comment.line,
                summary = "Temporary path comment is missing both debt(id) and an explicit deletion condition.",
                currentValueText = normalizedText,
                currentValueElement = JsonPrimitive(normalizedText),
                details =
                    buildJsonObject {
                        put("commentText", normalizedText)
                    },
                numericValue = 1.0,
            )
        }

    private fun parseFunctionSignature(
        parsedSource: ParsedKotlinSource,
        funStartOffset: Int,
    ): ParsedFunctionSignature? {
        val source = parsedSource.sanitizedCode
        val funIndex = source.indexOf("fun", funStartOffset)
        if (funIndex < 0) {
            return null
        }
        var index = funIndex + "fun".length
        while (index < source.length && source[index].isWhitespace()) {
            index++
        }
        if (index < source.length && source[index] == '<') {
            index = skipBalanced(source, index, '<', '>') + 1
        }
        val parameterStart = findParameterStart(source, index)
        if (parameterStart < 0) {
            return null
        }
        val callablePrefix = source.substring(index, parameterStart).trim()
        val functionName =
            Regex("""(`[^`]+`|[A-Za-z_][A-Za-z0-9_]*)$""")
                .find(callablePrefix)
                ?.groupValues
                ?.get(1)
                ?: return null
        val receiverDescriptor =
            callablePrefix
                .removeSuffix(functionName)
                .trimEnd()
                .removeSuffix(".")
                .trim()
                .takeIf(String::isNotBlank)
                ?.replace(Regex("""\s+"""), "")
        val parameterEnd = skipBalanced(source, parameterStart, '(', ')')
        val rawParameters = splitParameters(source.substring(parameterStart + 1, parameterEnd))
        val parameters =
            rawParameters.map { parameter ->
                extractParameterName(parameter.substringBefore('=').trim()) ?: parameter.substringBefore(':').substringBefore('=').trim()
            }
        val enclosingContainer = parsedSource.enclosingTopLevelContainer(funStartOffset)
        val ownerPrefix =
            buildList {
                enclosingContainer?.name?.let(::add)
                receiverDescriptor?.let(::add)
            }.takeIf { it.isNotEmpty() }
                ?.joinToString(separator = ".")
        val visibility =
            when {
                Regex("""\bprivate\b""").containsMatchIn(source.substring(funStartOffset, funIndex)) -> FunctionVisibility.NON_API
                Regex("""\bprotected\b""").containsMatchIn(source.substring(funStartOffset, funIndex)) -> FunctionVisibility.NON_API
                enclosingContainer?.isPrivate == true -> FunctionVisibility.NON_API
                Regex("""\binternal\b""").containsMatchIn(source.substring(funStartOffset, funIndex)) -> FunctionVisibility.INTERNAL
                enclosingContainer?.isInternal == true -> FunctionVisibility.INTERNAL
                else -> FunctionVisibility.PUBLIC
            }
        val booleanParameters =
            rawParameters.mapNotNull { parameter ->
                val parameterHeader = parameter.substringBefore('=')
                val match =
                    Regex("""(`[^`]+`|[A-Za-z_][A-Za-z0-9_]*)\s*:\s*(?:kotlin\.)?Boolean\??\b""")
                        .find(parameterHeader.trim())
                match?.groupValues?.get(1)
            }
        return ParsedFunctionSignature(
            name = functionName,
            referenceName = functionName.removeSurrounding("`"),
            displayName =
                ownerPrefix
                    ?.let { owner -> "$owner.$functionName(${parameters.size})" }
                    ?: "$functionName(${parameters.size})",
            signatureId =
                ownerPrefix
                    ?.let { owner -> "$owner.$functionName@${shortHash(rawParameters.joinToString(separator = "|"))}" }
                    ?: "$functionName@${shortHash(rawParameters.joinToString(separator = "|"))}",
            parameters = parameters,
            booleanParameters = booleanParameters,
            visibility = visibility,
            topLevelOwnerName = enclosingContainer?.name,
            minimumArgumentCount = rawParameters.count { parameter -> '=' !in parameter },
            maximumArgumentCount = rawParameters.size,
            receiverTypeTokens = receiverDescriptor?.let(::extractTypeReferenceTokens).orEmpty(),
        )
    }

    private fun findParameterStart(
        source: String,
        startIndex: Int,
    ): Int {
        var index = startIndex
        var angleDepth = 0
        while (index < source.length) {
            when (val character = source[index]) {
                '<' -> angleDepth++
                '>' -> if (angleDepth > 0) angleDepth--
                '(' -> if (angleDepth == 0) return index
                '{', '=', '\n' -> if (angleDepth == 0) return -1
                else -> character
            }
            index++
        }
        return -1
    }

    private fun skipBalanced(
        source: String,
        startIndex: Int,
        open: Char,
        close: Char,
    ): Int {
        var index = startIndex
        var depth = 0
        while (index < source.length) {
            when (source[index]) {
                open -> depth++
                close -> {
                    depth--
                    if (depth == 0) {
                        return index
                    }
                }
            }
            index++
        }
        error("Unbalanced '$open' ... '$close' declaration while parsing maintainability lint source.")
    }

    private fun splitParameters(parameterBlock: String): List<String> {
        if (parameterBlock.isBlank()) {
            return emptyList()
        }
        val parameters = mutableListOf<String>()
        var angleDepth = 0
        var parenthesisDepth = 0
        var bracketDepth = 0
        var braceDepth = 0
        var inBacktick = false
        var segmentStart = 0
        parameterBlock.forEachIndexed { index, character ->
            when {
                character == '`' -> inBacktick = !inBacktick
                inBacktick -> Unit
                character == '<' -> angleDepth++
                character == '>' -> if (angleDepth > 0) angleDepth-- else Unit
                character == '(' -> parenthesisDepth++
                character == ')' -> if (parenthesisDepth > 0) parenthesisDepth-- else Unit
                character == '[' -> bracketDepth++
                character == ']' -> if (bracketDepth > 0) bracketDepth-- else Unit
                character == '{' -> braceDepth++
                character == '}' -> if (braceDepth > 0) braceDepth-- else Unit
                character == ',' && angleDepth == 0 && parenthesisDepth == 0 && bracketDepth == 0 && braceDepth == 0 -> {
                    parameters += parameterBlock.substring(segmentStart, index).trim()
                    segmentStart = index + 1
                }
            }
        }
        parameters += parameterBlock.substring(segmentStart).trim()
        return parameters.filter(String::isNotBlank)
    }

    private fun extractParameterName(parameterHeader: String): String? =
        Regex("""(`[^`]+`|[A-Za-z_][A-Za-z0-9_]*)\s*:\s*""")
            .find(parameterHeader)
            ?.groupValues
            ?.get(1)

    private fun extractTypeReferenceTokens(typeReference: String): Set<String> =
        Regex("""[A-Za-z_][A-Za-z0-9_]*""")
            .findAll(typeReference)
            .map { match -> match.value }
            .filterNot { token -> token in setOf("in", "out", "reified", "suspend") }
            .toSet()

    private fun baselinePath(repoRoot: Path): Path =
        System.getProperty("ktome.maintainability.baselinePath")
            ?.let(Path::of)
            ?.normalize()
            ?: repoRoot.resolve(BASELINE_FILE_NAME)

    private fun reportDir(): Path =
        Path.of(
            requireNotNull(System.getProperty("ktome.maintainability.reportDir")) {
                "ktome.maintainability.reportDir system property is required for maintainability lint output."
            },
        )

    private fun gateMode(): MaintainabilityGateMode =
        when (System.getProperty("ktome.maintainability.blockingMode")?.lowercase()) {
            null, "", "true" -> MaintainabilityGateMode.BLOCKING
            "false" -> MaintainabilityGateMode.REPORT_ONLY
            else ->
                error(
                    "ktome.maintainability.blockingMode must be 'true' or 'false', found '${System.getProperty("ktome.maintainability.blockingMode")}'.",
                )
        }

    private fun repoRoot(): Path =
        System.getProperty("ktome.repo.root")
            ?.let(Path::of)
            ?.normalize()
            ?: Path.of("").toAbsolutePath().normalize()

    private fun renderMarkdown(
        summary: MaintainabilityLintSummary,
        findings: List<MaintainabilityFinding>,
    ): String =
        buildString {
            appendLine("# Maintainability Lint")
            appendLine()
            appendLine("## Summary")
            appendLine("- gateMode: `${summary.gateMode}`")
            appendLine("- verdict: `${summary.verdict}`")
            appendLine("- findings: `${summary.findingCount}`")
            appendLine("- approvedDebt: `${summary.approvedDebtCount}`")
            appendLine("- unexpectedRegression: `${summary.unexpectedRegressionCount}`")
            appendLine("- improvedDebt: `${summary.improvedDebtCount}`")
            appendLine("- baseline: `${summary.baselineId}`")
            appendLine("- metricDefinitionVersion: `${summary.metricDefinitionVersion}`")
            appendLine()
            appendLine("## Unexpected Regressions")
            val regressions = findings.filter { finding -> finding.status == EvaluationEntryStatus.UNEXPECTED_REGRESSION.name }
            if (regressions.isEmpty()) {
                appendLine("- none")
            } else {
                regressions.forEach { finding -> appendLine(renderFindingLine(finding)) }
            }
            appendLine()
            appendLine("## Approved Debt")
            val approvedDebt = findings.filter { finding -> finding.status == EvaluationEntryStatus.APPROVED_DEBT.name }
            if (approvedDebt.isEmpty()) {
                appendLine("- none")
            } else {
                approvedDebt.forEach { finding -> appendLine(renderFindingLine(finding)) }
            }
            appendLine()
            appendLine("## Improvements")
            val improvements =
                summary.evaluation.entries
                    .filter { entry -> entry.status == EvaluationEntryStatus.IMPROVEMENT }
                    .map { entry -> entry.metricId }
            if (improvements.isEmpty()) {
                appendLine("- none")
            } else {
                improvements.forEach { metricId -> appendLine("- `$metricId` cleared from current scan.") }
            }
        }.trimEnd()

    private fun renderFindingLine(finding: MaintainabilityFinding): String {
        val symbolSuffix = finding.symbol?.let { symbol -> " symbol=`$symbol`" } ?: ""
        return "- `${finding.taxonomy}` `${finding.filePath}:${finding.line}`$symbolSuffix ${finding.summary}"
    }

    private fun shortHash(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
            .take(12)
}

internal data class DetectedMaintainabilityFinding(
    val findingId: String,
    val taxonomy: String,
    val ruleId: String,
    val severity: String,
    val filePath: String,
    val line: Int,
    val summary: String,
    val currentValueText: String,
    val currentValueElement: JsonPrimitive,
    val details: JsonObject = JsonObject(emptyMap()),
    val symbol: String? = null,
    val numericValue: Double? = null,
) {
    fun numericValueEntry(): Pair<String, Double>? = numericValue?.let { value -> findingId to value }
}

private enum class FunctionVisibility {
    PUBLIC,
    INTERNAL,
    NON_API,
}

private enum class MaintainabilityGateMode {
    BLOCKING,
    REPORT_ONLY,
}

private data class ParsedFunctionSignature(
    val name: String,
    val referenceName: String,
    val displayName: String,
    val signatureId: String,
    val parameters: List<String>,
    val booleanParameters: List<String>,
    val visibility: FunctionVisibility,
    val topLevelOwnerName: String?,
    val minimumArgumentCount: Int,
    val maximumArgumentCount: Int,
    val receiverTypeTokens: Set<String>,
)

private data class ExtractedComment(
    val line: Int,
    val text: String,
)

private class LineIndex(
    source: String,
) {
    private val lineStartOffsets: IntArray =
        buildList {
            add(0)
            source.forEachIndexed { index, character ->
                if (character == '\n') {
                    add(index + 1)
                }
            }
        }.toIntArray()

    fun lineNumberAt(offset: Int): Int {
        var low = 0
        var high = lineStartOffsets.lastIndex
        while (low <= high) {
            val middle = (low + high) ushr 1
            val lineOffset = lineStartOffsets[middle]
            if (lineOffset <= offset) {
                if (middle == lineStartOffsets.lastIndex || lineStartOffsets[middle + 1] > offset) {
                    return middle + 1
                }
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        return 1
    }
}

private class ParsedKotlinSource private constructor(
    val sanitizedCode: String,
    val comments: List<ExtractedComment>,
    private val braceDepthByOffset: IntArray,
) {
    fun braceDepthAt(offset: Int): Int = braceDepthByOffset[offset.coerceIn(0, braceDepthByOffset.lastIndex)]

    fun enclosingTopLevelContainer(offset: Int): TopLevelContainer? {
        if (braceDepthAt(offset) != 1) {
            return null
        }
        val containerOpenBrace = findEnclosingTopLevelContainerOpenBrace(offset) ?: return null
        val declarationMatch =
            topLevelContainerDeclarationRegex
                .findAll(sanitizedCode.substring(0, containerOpenBrace))
                .lastOrNull { match ->
                    braceDepthAt(match.range.first) == 0
                } ?: return null
        val modifiers = declarationMatch.groups["modifiers"]?.value.orEmpty()
        val name = declarationMatch.groups["name"]?.value ?: return null
        return TopLevelContainer(
            name = name,
            isPrivate = Regex("""\bprivate\b""").containsMatchIn(modifiers),
            isInternal = Regex("""\binternal\b""").containsMatchIn(modifiers),
        )
    }

    companion object {
        fun parse(source: String): ParsedKotlinSource {
            val sanitized = StringBuilder(source.length)
            val comments = mutableListOf<ExtractedComment>()
            val braceDepthByOffset = IntArray(source.length.coerceAtLeast(1))
            var depth = 0
            var index = 0
            var line = 1

            fun appendReplacement(character: Char) {
                sanitized.append(if (character == '\n') '\n' else ' ')
            }

            while (index < source.length) {
                braceDepthByOffset[index] = depth
                when {
                    source.startsWith("//", index) -> {
                        val startLine = line
                        val comment = StringBuilder()
                        while (index < source.length && source[index] != '\n') {
                            braceDepthByOffset[index] = depth
                            comment.append(source[index])
                            appendReplacement(source[index])
                            index++
                        }
                        comments += ExtractedComment(line = startLine, text = comment.toString())
                    }

                    source.startsWith("/*", index) -> {
                        val startLine = line
                        val comment = StringBuilder()
                        comment.append("/*")
                        braceDepthByOffset[index] = depth
                        braceDepthByOffset[index + 1] = depth
                        appendReplacement(source[index])
                        appendReplacement(source[index + 1])
                        index += 2
                        while (index < source.length && !source.startsWith("*/", index)) {
                            braceDepthByOffset[index] = depth
                            val character = source[index]
                            comment.append(character)
                            appendReplacement(character)
                            if (character == '\n') {
                                line++
                            }
                            index++
                        }
                        if (index < source.length) {
                            comment.append("*/")
                            braceDepthByOffset[index] = depth
                            braceDepthByOffset[index + 1] = depth
                            appendReplacement(source[index])
                            appendReplacement(source[index + 1])
                            index += 2
                        }
                        comments += ExtractedComment(line = startLine, text = comment.toString())
                    }

                    source.startsWith("\"\"\"", index) -> {
                        repeat(3) {
                            braceDepthByOffset[index] = depth
                            appendReplacement(source[index])
                            index++
                        }
                        while (index < source.length && !source.startsWith("\"\"\"", index)) {
                            braceDepthByOffset[index] = depth
                            val character = source[index]
                            appendReplacement(character)
                            if (character == '\n') {
                                line++
                            }
                            index++
                        }
                        if (index < source.length) {
                            repeat(3) {
                                braceDepthByOffset[index] = depth
                                appendReplacement(source[index])
                                index++
                            }
                        }
                    }

                    source[index] == '"' -> {
                        braceDepthByOffset[index] = depth
                        appendReplacement(source[index])
                        index++
                        while (index < source.length) {
                            braceDepthByOffset[index] = depth
                            val character = source[index]
                            appendReplacement(character)
                            if (character == '\\' && index + 1 < source.length) {
                                index++
                                braceDepthByOffset[index] = depth
                                appendReplacement(source[index])
                            } else if (character == '"') {
                                index++
                                break
                            } else if (character == '\n') {
                                line++
                            }
                            index++
                        }
                    }

                    source[index] == '\'' -> {
                        braceDepthByOffset[index] = depth
                        appendReplacement(source[index])
                        index++
                        while (index < source.length) {
                            braceDepthByOffset[index] = depth
                            val character = source[index]
                            appendReplacement(character)
                            if (character == '\\' && index + 1 < source.length) {
                                index++
                                braceDepthByOffset[index] = depth
                                appendReplacement(source[index])
                            } else if (character == '\'') {
                                index++
                                break
                            } else if (character == '\n') {
                                line++
                            }
                            index++
                        }
                    }

                    else -> {
                        val character = source[index]
                        sanitized.append(character)
                        when (character) {
                            '{' -> depth++
                            '}' -> if (depth > 0) depth--
                            '\n' -> line++
                        }
                        index++
                    }
                }
            }

            if (source.isEmpty()) {
                braceDepthByOffset[0] = 0
            }

            val sanitizedCode = sanitized.toString()

            return ParsedKotlinSource(
                sanitizedCode = sanitizedCode,
                comments = comments,
                braceDepthByOffset = braceDepthByOffset,
            )
        }
    }

    private fun findEnclosingTopLevelContainerOpenBrace(offset: Int): Int? {
        var nestedBraceDepth = 0
        for (index in (offset - 1).coerceAtLeast(0) downTo 0) {
            when (sanitizedCode[index]) {
                '}' -> nestedBraceDepth++
                '{' ->
                    if (nestedBraceDepth == 0) {
                        return index
                    } else {
                        nestedBraceDepth--
                    }
            }
        }
        return null
    }
}

private data class TopLevelContainer(
    val name: String,
    val isPrivate: Boolean,
    val isInternal: Boolean,
)

private data class ImportedSymbol(
    val canonicalPath: String,
    val localName: String,
)

private data class SourceFileContext(
    val relativePath: String,
    val packageName: String,
    val importedSymbols: Set<ImportedSymbol>,
    val wildcardImports: Set<String>,
    val parsedSource: ParsedKotlinSource,
    val lineIndex: LineIndex,
) {
    companion object {
        fun from(
            repoRoot: Path,
            file: Path,
        ): SourceFileContext {
            val text = file.readText()
            val packageName = packageDeclarationRegex.find(text)?.groupValues?.get(1).orEmpty()
            val imports =
                importDeclarationRegex
                    .findAll(text)
                    .map { match ->
                        val canonicalPath = match.groupValues[1].removeSurrounding("`")
                        val alias = match.groupValues[2].takeIf(String::isNotBlank)
                        ImportedSymbol(
                            canonicalPath = canonicalPath,
                            localName = alias ?: canonicalPath.substringAfterLast('.').removeSurrounding("`"),
                        )
                    }
                    .toList()
            return SourceFileContext(
                relativePath = repoRoot.relativize(file).toString().replace('\\', '/'),
                packageName = packageName,
                importedSymbols = imports.filterNot { symbol -> symbol.canonicalPath.endsWith(".*") }.toSet(),
                wildcardImports = imports.filter { symbol -> symbol.canonicalPath.endsWith(".*") }.map { symbol -> symbol.canonicalPath.removeSuffix(".*") }.toSet(),
                parsedSource = ParsedKotlinSource.parse(text),
                lineIndex = LineIndex(text),
            )
        }
    }
}

private class CrossFileRuntimeReferenceIndex private constructor(
    private val sourceFiles: List<SourceFileContext>,
) {
    fun hasCrossFileProductionReference(
        sourceFile: SourceFileContext,
        signature: ParsedFunctionSignature,
    ): Boolean {
        return sourceFiles.any { candidate ->
            if (candidate.relativePath == sourceFile.relativePath) {
                return@any false
            }
            if (!candidate.canReference(sourceFile, signature)) {
                return@any false
            }
            val ownerReferenceNames = candidate.ownerReferenceNamesFor(sourceFile, signature)
            val ownerMatched =
                ownerReferenceNames.any { ownerName ->
                    candidate.parsedSource.sanitizedCode.hasMemberInvocationWithinArity(
                        ownerName = ownerName,
                        memberName = signature.referenceName,
                        minimumArgumentCount = signature.minimumArgumentCount,
                        maximumArgumentCount = signature.maximumArgumentCount,
                    )
                }
            if (ownerMatched) {
                return@any true
            }
            val directReferenceNames = candidate.directReferenceNamesFor(sourceFile, signature)
            val directMatched =
                directReferenceNames.any { referenceName ->
                    candidate.parsedSource.sanitizedCode.hasDirectInvocationWithinArity(
                        referenceName = referenceName,
                        allowMemberReceiver = ownerReferenceNames.isNotEmpty(),
                        minimumArgumentCount = signature.minimumArgumentCount,
                        maximumArgumentCount = signature.maximumArgumentCount,
                    )
                }
            if (!directMatched) {
                return@any false
            }
            when {
                ownerReferenceNames.isNotEmpty() -> ownerReferenceNames.any { ownerName ->
                    Regex("""\b${Regex.escape(ownerName)}\b""").containsMatchIn(candidate.parsedSource.sanitizedCode)
                }
                signature.receiverTypeTokens.isNotEmpty() -> signature.receiverTypeTokens.any { token ->
                    Regex("""\b${Regex.escape(token)}\b""").containsMatchIn(candidate.parsedSource.sanitizedCode)
                }
                else -> true
            }
        }
    }

    companion object {
        fun build(sourceFiles: List<SourceFileContext>): CrossFileRuntimeReferenceIndex =
            CrossFileRuntimeReferenceIndex(
                sourceFiles = sourceFiles,
            )
    }
}

private fun String.hasDirectInvocationWithinArity(
    referenceName: String,
    allowMemberReceiver: Boolean,
    minimumArgumentCount: Int,
    maximumArgumentCount: Int,
): Boolean {
    var searchStart = 0
    while (searchStart < length) {
        val referenceOffset = indexOf(referenceName, startIndex = searchStart)
        if (referenceOffset < 0) {
            return false
        }
        val referenceEnd = referenceOffset + referenceName.length
        val beforeReference = getOrNull(referenceOffset - 1)
        val afterReference = getOrNull(referenceEnd)
        val beforeOk =
            beforeReference == null ||
                (!beforeReference.isIdentifierTokenChar() && (allowMemberReceiver || beforeReference != '.'))
        val afterOk = afterReference == null || !afterReference.isIdentifierTokenChar()
        if (beforeOk && afterOk) {
            val openParenOffset = skipInlineWhitespace(referenceEnd)
            if (openParenOffset < length && this[openParenOffset] == '(') {
                val argumentCount = invocationArgumentCountAt(openParenOffset) ?: return false
                if (argumentCount in minimumArgumentCount..maximumArgumentCount) {
                    return true
                }
            }
        }
        searchStart = referenceOffset + 1
    }
    return false
}

private fun String.hasMemberInvocationWithinArity(
    ownerName: String,
    memberName: String,
    minimumArgumentCount: Int,
    maximumArgumentCount: Int,
): Boolean {
    var searchStart = 0
    while (searchStart < length) {
        val ownerOffset = indexOf(ownerName, startIndex = searchStart)
        if (ownerOffset < 0) {
            return false
        }
        val ownerEnd = ownerOffset + ownerName.length
        val beforeOwner = getOrNull(ownerOffset - 1)
        val afterOwner = getOrNull(ownerEnd)
        val ownerBoundaryOk = beforeOwner == null || !beforeOwner.isIdentifierTokenChar()
        val ownerNameOk = afterOwner == null || !afterOwner.isIdentifierTokenChar()
        if (ownerBoundaryOk && ownerNameOk) {
            val dotOffset = skipInlineWhitespace(ownerEnd)
            if (dotOffset < length && this[dotOffset] == '.') {
                val memberOffset = skipInlineWhitespace(dotOffset + 1)
                if (regionMatches(memberOffset, memberName, 0, memberName.length)) {
                    val memberEnd = memberOffset + memberName.length
                    val afterMember = getOrNull(memberEnd)
                    if (afterMember == null || !afterMember.isIdentifierTokenChar()) {
                        val openParenOffset = skipInlineWhitespace(memberEnd)
                        if (openParenOffset < length && this[openParenOffset] == '(') {
                            val argumentCount = invocationArgumentCountAt(openParenOffset) ?: return false
                            if (argumentCount in minimumArgumentCount..maximumArgumentCount) {
                                return true
                            }
                        }
                    }
                }
            }
        }
        searchStart = ownerOffset + 1
    }
    return false
}

private fun Char.isIdentifierTokenChar(): Boolean = isLetterOrDigit() || this == '_' || this == '`'

private fun String.skipInlineWhitespace(startIndex: Int): Int {
    var index = startIndex
    while (index < length && (this[index] == ' ' || this[index] == '\t')) {
        index++
    }
    return index
}

private fun String.invocationArgumentCountAt(openParenOffset: Int): Int? {
    if (openParenOffset !in indices || this[openParenOffset] != '(') {
        return null
    }
    val argumentBlock = substring(openParenOffset + 1, skipBalanced(openParenOffset, '(', ')'))
    if (argumentBlock.isBlank()) {
        return 0
    }
    return splitTopLevelArguments(argumentBlock).size
}

private fun String.skipBalanced(
    startIndex: Int,
    open: Char,
    close: Char,
): Int {
    var index = startIndex
    var depth = 0
    while (index < length) {
        when (this[index]) {
            open -> depth++
            close -> {
                depth--
                if (depth == 0) {
                    return index
                }
            }
        }
        index++
    }
    return lastIndex
}

private fun splitTopLevelArguments(argumentBlock: String): List<String> {
    val arguments = mutableListOf<String>()
    var angleDepth = 0
    var parenthesisDepth = 0
    var bracketDepth = 0
    var braceDepth = 0
    var inBacktick = false
    var segmentStart = 0
    argumentBlock.forEachIndexed { index, character ->
        when {
            character == '`' -> inBacktick = !inBacktick
            inBacktick -> Unit
            character == '<' -> angleDepth++
            character == '>' -> if (angleDepth > 0) angleDepth-- else Unit
            character == '(' -> parenthesisDepth++
            character == ')' -> if (parenthesisDepth > 0) parenthesisDepth-- else Unit
            character == '[' -> bracketDepth++
            character == ']' -> if (bracketDepth > 0) bracketDepth-- else Unit
            character == '{' -> braceDepth++
            character == '}' -> if (braceDepth > 0) braceDepth-- else Unit
            character == ',' && angleDepth == 0 && parenthesisDepth == 0 && bracketDepth == 0 && braceDepth == 0 -> {
                arguments += argumentBlock.substring(segmentStart, index).trim()
                segmentStart = index + 1
            }
        }
    }
    arguments += argumentBlock.substring(segmentStart).trim()
    return arguments.filter(String::isNotBlank)
}

private fun SourceFileContext.canReference(
    declarationFile: SourceFileContext,
    signature: ParsedFunctionSignature,
): Boolean = directReferenceNamesFor(declarationFile, signature).isNotEmpty() || ownerReferenceNamesFor(declarationFile, signature).isNotEmpty()

private fun SourceFileContext.directReferenceNamesFor(
    declarationFile: SourceFileContext,
    signature: ParsedFunctionSignature,
): Set<String> {
    val referenceNames = linkedSetOf<String>()
    if (packageName == declarationFile.packageName || wildcardImports.contains(declarationFile.packageName)) {
        referenceNames += signature.referenceName
    }
    referenceNames += importedLocalNamesFor("${declarationFile.packageName}.${signature.referenceName}")
    return referenceNames
}

private fun SourceFileContext.ownerReferenceNamesFor(
    declarationFile: SourceFileContext,
    signature: ParsedFunctionSignature,
): Set<String> {
    val ownerName = signature.topLevelOwnerName ?: return emptySet()
    val referenceNames = linkedSetOf<String>()
    if (packageName == declarationFile.packageName || wildcardImports.contains(declarationFile.packageName)) {
        referenceNames += ownerName
    }
    referenceNames += importedLocalNamesFor("${declarationFile.packageName}.$ownerName")
    return referenceNames
}

private fun SourceFileContext.importedLocalNamesFor(canonicalPath: String): Set<String> =
    importedSymbols
        .asSequence()
        .filter { symbol -> symbol.canonicalPath == canonicalPath }
        .mapTo(linkedSetOf()) { symbol -> symbol.localName }
