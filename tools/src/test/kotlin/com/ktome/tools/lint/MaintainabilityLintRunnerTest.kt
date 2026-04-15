package com.ktome.tools.lint

import com.ktome.tools.verification.BaselineMode
import com.ktome.tools.verification.EvaluationVerdict
import com.ktome.tools.verification.VERIFICATION_BASELINE_SCHEMA_VERSION
import com.ktome.tools.verification.VerificationBaseline
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.SAME_THREAD)
class MaintainabilityLintRunnerTest {
    @TempDir
    lateinit var tempDir: Path

    private val json: Json =
        Json {
            prettyPrint = true
            explicitNulls = false
        }

    @Test
    @Tag("maintainabilityLint")
    fun `repo maintainability baseline stays green`() {
        val repoRoot = repoRoot()
        val reportDir =
            System.getProperty("ktome.maintainability.reportDir")
                ?.let(Path::of)
                ?: tempDir.resolve("maintainability-report")
        val baselinePath = repoRoot.resolve("maintainability-baseline.json")

        val run =
            withRunnerProperties(
                repoRoot = repoRoot,
                baselinePath = baselinePath,
                reportDir = reportDir,
                blockingMode =
                    System.getProperty("ktome.maintainability.blockingMode")
                        ?.toBooleanStrictOrNull()
                        ?: true,
            ) {
                MaintainabilityLintRunner.run()
            }

        assertEquals(
            if ((System.getProperty("ktome.maintainability.blockingMode")?.toBooleanStrictOrNull() ?: true)) "BLOCKING" else "REPORT_ONLY",
            run.summary.gateMode,
        )
        assertTrue(run.summaryPath.exists())
        assertTrue(run.findingsPath.exists())
        assertTrue(run.reportPath.exists())
    }

    @Test
    fun `runner reports helper sprawl option sprawl and temp path without expiry`() {
        val repoRoot = createTempRepo(
            sourceFiles =
                mapOf(
                    "core/src/main/kotlin/com/ktome/core/sample/DemoRuntime.kt" to
                        """
                        package com.ktome.core.sample

                        class DemoHelper

                        class RuntimeApi {
                            fun resolveState(
                                actorId: String,
                                zoneId: String,
                                floorId: String,
                                includeFallback: Boolean,
                                retryCount: Int,
                            ): String {
                                // TODO remove when P5-W5 lands
                                return "${'$'}actorId-${'$'}zoneId-${'$'}floorId-${'$'}includeFallback-${'$'}retryCount"
                            }
                        }
                        """.trimIndent(),
                ),
            baseline = emptyBaseline(),
        )

        val run =
            withRunnerProperties(
                repoRoot = repoRoot,
                baselinePath = repoRoot.resolve("maintainability-baseline.json"),
                reportDir = tempDir.resolve("report"),
                blockingMode = false,
            ) {
                MaintainabilityLintRunner.run()
            }

        val findingsJson =
            Json.parseToJsonElement(run.findingsPath.readText())
                .jsonArray
                .map { element -> element.jsonObject }
        val taxonomies = findingsJson.map { finding -> finding.getValue("taxonomy").jsonPrimitive.content }.toSet()

        assertEquals(EvaluationVerdict.FAIL.name, run.summary.verdict)
        assertEquals("REPORT_ONLY", run.summary.gateMode)
        assertTrue(taxonomies.contains("helper-sprawl"))
        assertTrue(taxonomies.contains("option-sprawl"))
        assertTrue(taxonomies.contains("temp-path-without-expiry"))
        assertTrue(run.summary.unexpectedRegressionCount >= 4)
    }

    @Test
    fun `approved baseline converts existing debt into green approved debt report`() {
        val repoRoot = createTempRepo(
            sourceFiles =
                mapOf(
                    "core/src/main/kotlin/com/ktome/core/sample/DemoRuntime.kt" to
                        """
                        package com.ktome.core.sample

                        object RollbackManager

                        class RuntimeApi {
                            fun resolveState(
                                actorId: String,
                                zoneId: String,
                                floorId: String,
                                includeFallback: Boolean,
                                retryCount: Int,
                            ): String {
                                // TODO remove after tactical-ai lands
                                return "${'$'}actorId-${'$'}zoneId-${'$'}floorId-${'$'}includeFallback-${'$'}retryCount"
                            }
                        }
                        """.trimIndent(),
                ),
            baseline = emptyBaseline(),
        )
        val approvedDebtKeys = MaintainabilityLintRunner.collectFindings(repoRoot).map { finding -> finding.findingId }
        writeBaseline(
            path = repoRoot.resolve("maintainability-baseline.json"),
            baseline = baselineWithDebt(approvedDebtKeys),
        )

        val run =
            withRunnerProperties(
                repoRoot = repoRoot,
                baselinePath = repoRoot.resolve("maintainability-baseline.json"),
                reportDir = tempDir.resolve("approved-report"),
                blockingMode = true,
            ) {
                MaintainabilityLintRunner.run()
            }

        assertEquals(EvaluationVerdict.PASS.name, run.summary.verdict)
        assertEquals("BLOCKING", run.summary.gateMode)
        assertEquals(0, run.summary.unexpectedRegressionCount)
        assertEquals(run.findings.size, run.summary.approvedDebtCount)
    }

    @Test
    fun `typed config object passes without maintainability findings`() {
        val repoRoot = createTempRepo(
            sourceFiles =
                mapOf(
                    "game/src/main/kotlin/com/ktome/game/sample/RuntimeConfig.kt" to
                        """
                        package com.ktome.game.sample

                        data class RuntimeConfig(
                            val includeFallback: Boolean,
                            val localeLock: Boolean,
                        )

                        class RuntimeFacade {
                            fun resolve(config: RuntimeConfig): String = config.toString()
                        }
                        """.trimIndent(),
                ),
            baseline = emptyBaseline(),
        )

        val run =
            withRunnerProperties(
                repoRoot = repoRoot,
                baselinePath = repoRoot.resolve("maintainability-baseline.json"),
                reportDir = tempDir.resolve("typed-config-report"),
                blockingMode = true,
            ) {
                MaintainabilityLintRunner.run()
            }

        assertEquals(EvaluationVerdict.PASS.name, run.summary.verdict)
        assertEquals("BLOCKING", run.summary.gateMode)
        assertTrue(run.findings.isEmpty())
    }

    @Test
    fun `finding ids and report schema stay stable across repeated scans`() {
        val repoRoot = createTempRepo(
            sourceFiles =
                mapOf(
                    "core/src/main/kotlin/com/ktome/core/sample/DemoRuntime.kt" to
                        """
                        package com.ktome.core.sample

                        class DemoHelper

                        class RuntimeApi {
                            fun resolve(actorId: String, zoneId: String, includeFallback: Boolean, floorId: String, retryCount: Int): String {
                                // TODO remove when short-run governance lands
                                return "${'$'}actorId-${'$'}zoneId-${'$'}includeFallback-${'$'}floorId-${'$'}retryCount"
                            }
                        }
                        """.trimIndent(),
                ),
            baseline = emptyBaseline(),
        )

        val firstRun =
            withRunnerProperties(
                repoRoot = repoRoot,
                baselinePath = repoRoot.resolve("maintainability-baseline.json"),
                reportDir = tempDir.resolve("stability-first"),
                blockingMode = false,
            ) {
                MaintainabilityLintRunner.run()
            }
        val secondRun =
            withRunnerProperties(
                repoRoot = repoRoot,
                baselinePath = repoRoot.resolve("maintainability-baseline.json"),
                reportDir = tempDir.resolve("stability-second"),
                blockingMode = false,
            ) {
                MaintainabilityLintRunner.run()
            }

        assertEquals(firstRun.findings.map(MaintainabilityFinding::findingId), secondRun.findings.map(MaintainabilityFinding::findingId))

        val summary = json.decodeFromString<MaintainabilityLintSummary>(firstRun.summaryPath.readText())
        val findingsJson = Json.parseToJsonElement(firstRun.findingsPath.readText()).jsonArray
        val firstFinding = findingsJson.first().jsonObject
        assertEquals(EvaluationVerdict.FAIL.name, summary.verdict)
        assertEquals("REPORT_ONLY", summary.gateMode)
        assertTrue(summary.findingCount > 0)
        assertEquals(summary.findingCount, findingsJson.size)
        assertTrue(
            summary.taxonomyCounts.keys.all { taxonomy ->
                taxonomy in
                    setOf(
                        "branch-patch",
                        "helper-sprawl",
                        "option-sprawl",
                        "second-authority",
                        "stringly-contract",
                        "temp-path-without-expiry",
                        "test-gap",
                    )
            },
        )
        assertTrue(firstFinding.getValue("findingId").jsonPrimitive.content.isNotBlank())
        assertTrue(firstFinding.getValue("taxonomy").jsonPrimitive.content.isNotBlank())
        assertTrue(firstFinding.getValue("ruleId").jsonPrimitive.content.isNotBlank())
        assertTrue(firstFinding.getValue("status").jsonPrimitive.content.isNotBlank())
    }

    @Test
    fun `option debt finding ids stay stable when declarations move between lines`() {
        val compactRepo =
            createTempRepo(
                sourceFiles =
                    mapOf(
                        "core/src/main/kotlin/com/ktome/core/sample/StableIds.kt" to
                            """
                            package com.ktome.core.sample

                            class RuntimeApi {
                                fun resolve(actorId: String, zoneId: String, floorId: String, includeFallback: Boolean, retryCount: Int): String =
                                    "${'$'}actorId-${'$'}zoneId-${'$'}floorId-${'$'}includeFallback-${'$'}retryCount"
                            }
                            """.trimIndent(),
                    ),
                baseline = emptyBaseline(),
            )
        val shiftedRepo =
            createTempRepo(
                sourceFiles =
                    mapOf(
                        "core/src/main/kotlin/com/ktome/core/sample/StableIds.kt" to
                            """
                            package com.ktome.core.sample



                            class RuntimeApi {
                                fun resolve(actorId: String, zoneId: String, floorId: String, includeFallback: Boolean, retryCount: Int): String =
                                    "${'$'}actorId-${'$'}zoneId-${'$'}floorId-${'$'}includeFallback-${'$'}retryCount"
                            }
                            """.trimIndent(),
                    ),
                baseline = emptyBaseline(),
            )

        val compactIds = MaintainabilityLintRunner.collectFindings(compactRepo).map { finding -> finding.findingId }
        val shiftedIds = MaintainabilityLintRunner.collectFindings(shiftedRepo).map { finding -> finding.findingId }

        assertEquals(compactIds, shiftedIds)
        assertTrue(compactIds.none { findingId -> Regex("""option-sprawl:(?:boolean|parameter-matrix):.+:\d+:""").containsMatchIn(findingId) })
    }

    @Test
    fun `alias imports still count as cross file production references for internal apis`() {
        val repoRoot =
            createTempRepo(
                sourceFiles =
                    mapOf(
                        "core/src/main/kotlin/com/ktome/core/sample/InternalApis.kt" to
                            """
                            package com.ktome.core.sample

                            internal fun resolveViaAlias(
                                actorId: String,
                                zoneId: String,
                                floorId: String,
                                includeFallback: Boolean,
                                retryCount: Int,
                            ): String = "${'$'}actorId-${'$'}zoneId-${'$'}floorId-${'$'}includeFallback-${'$'}retryCount"

                            internal object RuntimeGate {
                                internal fun resolveState(
                                    actorId: String,
                                    zoneId: String,
                                    floorId: String,
                                    includeFallback: Boolean,
                                    retryCount: Int,
                                ): String = "${'$'}actorId-${'$'}zoneId-${'$'}floorId-${'$'}includeFallback-${'$'}retryCount"
                            }
                            """.trimIndent(),
                        "game/src/main/kotlin/com/ktome/game/sample/InternalApiConsumers.kt" to
                            """
                            package com.ktome.game.sample

                            import com.ktome.core.sample.RuntimeGate as AliasedRuntimeGate
                            import com.ktome.core.sample.resolveViaAlias as aliasedResolve

                            internal fun useAliases(actorId: String, zoneId: String, floorId: String) {
                                aliasedResolve(actorId, zoneId, floorId, true, 1)
                                AliasedRuntimeGate.resolveState(actorId, zoneId, floorId, true, 1)
                            }
                            """.trimIndent(),
                    ),
                baseline = emptyBaseline(),
            )

        val optionFindings =
            MaintainabilityLintRunner
                .collectFindings(repoRoot)
                .filter { finding -> finding.taxonomy == "option-sprawl" }

        assertTrue(
            optionFindings.any { finding -> finding.symbol == "resolveViaAlias(5)" && finding.ruleId == "boolean-parameter" },
            optionFindings.joinToString(separator = "\n") { finding -> "${finding.symbol}:${finding.ruleId}:${finding.findingId}" },
        )
        assertTrue(optionFindings.any { finding -> finding.symbol == "resolveViaAlias(5)" && finding.ruleId == "parameter-count" })
        assertTrue(optionFindings.any { finding -> finding.symbol == "RuntimeGate.resolveState(5)" && finding.ruleId == "boolean-parameter" })
        assertTrue(optionFindings.any { finding -> finding.symbol == "RuntimeGate.resolveState(5)" && finding.ruleId == "parameter-count" })
    }

    @Test
    fun `api lint ignores private top level containers`() {
        val repoRoot =
            createTempRepo(
                sourceFiles =
                    mapOf(
                        "tools/src/main/kotlin/com/ktome/tools/sample/PrivateContainer.kt" to
                            """
                            package com.ktome.tools.sample

                            private class PrivateRuntimeApi {
                                fun privateResolve(
                                    actorId: String,
                                    zoneId: String,
                                    floorId: String,
                                    includeFallback: Boolean,
                                    retryCount: Int,
                                ): String = "${'$'}actorId-${'$'}zoneId-${'$'}floorId-${'$'}includeFallback-${'$'}retryCount"
                            }

                            class PublicRuntimeApi {
                                fun publicResolve(
                                    actorId: String,
                                    zoneId: String,
                                    floorId: String,
                                    includeFallback: Boolean,
                                    retryCount: Int,
                                ): String = "${'$'}actorId-${'$'}zoneId-${'$'}floorId-${'$'}includeFallback-${'$'}retryCount"
                            }
                            """.trimIndent(),
                    ),
                baseline = emptyBaseline(),
            )

        val findings = MaintainabilityLintRunner.collectFindings(repoRoot)
        val functionNames =
            findings
                .mapNotNull { finding -> finding.details["functionName"]?.jsonPrimitive?.content }
                .toSet()

        assertTrue("publicResolve" in functionNames)
        assertTrue("privateResolve" !in functionNames)
    }

    @Test
    fun `internal top level runtime helper stays out when not used cross file in production`() {
        val repoRoot =
            createTempRepo(
                sourceFiles =
                    mapOf(
                        "client/src/main/kotlin/com/ktome/client/sample/SameFileOnly.kt" to
                            """
                            package com.ktome.client.sample

                            internal fun appendAndPersistProfileRun(
                                profileId: String,
                                persistenceEnabled: Boolean,
                            ): String = if (persistenceEnabled) profileId else "memory"

                            class ProfileScreen {
                                fun persist(profileId: String): String =
                                    appendAndPersistProfileRun(profileId, persistenceEnabled = true)
                            }
                            """.trimIndent(),
                    ),
                baseline = emptyBaseline(),
            )

        val findingIds = MaintainabilityLintRunner.collectFindings(repoRoot).map { finding -> finding.findingId }

        assertTrue(findingIds.none { findingId -> "appendAndPersistProfileRun" in findingId })
    }

    @Test
    fun `internal member stays out when another file only contains unrelated same name call`() {
        val repoRoot =
            createTempRepo(
                sourceFiles =
                    mapOf(
                        "client/src/main/kotlin/com/ktome/client/sample/LocalRuntimeApi.kt" to
                            """
                            package com.ktome.client.sample

                            internal class LocalRuntimeApi {
                                fun render(renderEnabled: Boolean): String = renderEnabled.toString()
                            }

                            fun useLocalOnly(): String = LocalRuntimeApi().render(renderEnabled = true)
                            """.trimIndent(),
                        "client/src/main/kotlin/com/ktome/client/sample/UnrelatedRender.kt" to
                            """
                            package com.ktome.client.sample

                            fun render(label: String): String = label
                            """.trimIndent(),
                    ),
                baseline = emptyBaseline(),
            )

        val findingIds = MaintainabilityLintRunner.collectFindings(repoRoot).map { finding -> finding.findingId }

        assertTrue(findingIds.none { findingId -> "LocalRuntimeApi.render" in findingId })
    }

    @Test
    fun `internal member stays in when another file references owner type and calls method`() {
        val repoRoot =
            createTempRepo(
                sourceFiles =
                    mapOf(
                        "client/src/main/kotlin/com/ktome/client/sample/SharedRuntimeApi.kt" to
                            """
                            package com.ktome.client.sample

                            internal class SharedRuntimeApi {
                                fun render(renderEnabled: Boolean): String = renderEnabled.toString()
                            }
                            """.trimIndent(),
                        "client/src/main/kotlin/com/ktome/client/sample/SharedRuntimeApiConsumer.kt" to
                            """
                            package com.ktome.client.sample

                            fun consumeSharedRuntimeApi(): String {
                                val api = SharedRuntimeApi()
                                return api.render(renderEnabled = true)
                            }
                            """.trimIndent(),
                    ),
                baseline = emptyBaseline(),
            )

        val findingIds = MaintainabilityLintRunner.collectFindings(repoRoot).map { finding -> finding.findingId }

        assertTrue(findingIds.any { findingId -> "SharedRuntimeApi.render" in findingId })
    }

    @Test
    fun `top level extension stays out when another file only calls same name without receiver hint`() {
        val repoRoot =
            createTempRepo(
                sourceFiles =
                    mapOf(
                        "game/src/main/kotlin/com/ktome/game/sample/DiagnosticExtensions.kt" to
                            """
                            package com.ktome.game.sample

                            data class DiagnosticEntry(val code: String)

                            internal fun MutableList<DiagnosticEntry>.addDiagnostic(
                                enabled: Boolean,
                                code: String = "demo",
                            ) {
                                if (enabled) add(DiagnosticEntry(code))
                            }
                            """.trimIndent(),
                        "game/src/main/kotlin/com/ktome/game/sample/UnrelatedDiagnostics.kt" to
                            """
                            package com.ktome.game.sample

                            fun addDiagnostic(label: String): String = label

                            fun renderDiagnostic(): String = addDiagnostic("ui")
                            """.trimIndent(),
                    ),
                baseline = emptyBaseline(),
            )

        val findingIds = MaintainabilityLintRunner.collectFindings(repoRoot).map { finding -> finding.findingId }

        assertTrue(findingIds.none { findingId -> "DiagnosticEntry>.addDiagnostic" in findingId })
    }

    @Test
    fun `top level internal function uses invocation arity to avoid unrelated same name overloads`() {
        val repoRoot =
            createTempRepo(
                sourceFiles =
                    mapOf(
                        "tools/src/main/kotlin/com/ktome/tools/sample/TopologySupport.kt" to
                            """
                            package com.ktome.tools.sample

                            internal fun buildReport(
                                scopeId: String,
                                renderEnabled: Boolean,
                            ): String = "${'$'}scopeId:${'$'}renderEnabled"
                            """.trimIndent(),
                        "tools/src/main/kotlin/com/ktome/tools/sample/TopologyUi.kt" to
                            """
                            package com.ktome.tools.sample

                            fun buildReport(scopeId: String): String = scopeId

                            fun renderUi(): String = buildReport("ui")
                            """.trimIndent(),
                    ),
                baseline = emptyBaseline(),
            )

        val findingIds = MaintainabilityLintRunner.collectFindings(repoRoot).map { finding -> finding.findingId }

        assertTrue(findingIds.none { findingId -> "buildReport@" in findingId })
    }

    @Test
    fun `owner detection skips bodyless top level declarations`() {
        val repoRoot =
            createTempRepo(
                sourceFiles =
                    mapOf(
                        "client/src/main/kotlin/com/ktome/client/sample/OwnerDetection.kt" to
                            """
                            package com.ktome.client.sample

                            data class OwnerMarker(
                                val label: String,
                            )

                            class RuntimeFacade {
                                fun render(renderEnabled: Boolean): String = renderEnabled.toString()
                            }
                            """.trimIndent(),
                    ),
                baseline = emptyBaseline(),
            )

        val findingIds =
            MaintainabilityLintRunner.collectFindings(repoRoot)
                .map { finding -> finding.findingId }
                .filter { findingId -> findingId.startsWith("option-sprawl:boolean:client/src/main/kotlin/com/ktome/client/sample/OwnerDetection.kt:") }

        assertEquals(
            listOf("option-sprawl:boolean:client/src/main/kotlin/com/ktome/client/sample/OwnerDetection.kt:RuntimeFacade.render@e84fa64fc4db:renderEnabled"),
            findingIds,
        )
    }

    @Test
    fun `stable ids include top level owner to avoid duplicate debt keys`() {
        val repoRoot =
            createTempRepo(
                sourceFiles =
                    mapOf(
                        "client/src/main/kotlin/com/ktome/client/sample/DuplicateOwners.kt" to
                            """
                            package com.ktome.client.sample

                            interface MenuAudioContract {
                                fun create(renderEnabled: Boolean): String
                            }

                            class MenuAudioAdapter {
                                fun create(renderEnabled: Boolean): String = renderEnabled.toString()
                            }
                            """.trimIndent(),
                    ),
                baseline = emptyBaseline(),
            )

        val findingIds =
            MaintainabilityLintRunner.collectFindings(repoRoot)
                .map { finding -> finding.findingId }
                .filter { findingId -> findingId.startsWith("option-sprawl:boolean:client/src/main/kotlin/com/ktome/client/sample/DuplicateOwners.kt:") }
                .sorted()

        assertEquals(
            listOf(
                "option-sprawl:boolean:client/src/main/kotlin/com/ktome/client/sample/DuplicateOwners.kt:MenuAudioAdapter.create@e84fa64fc4db:renderEnabled",
                "option-sprawl:boolean:client/src/main/kotlin/com/ktome/client/sample/DuplicateOwners.kt:MenuAudioContract.create@e84fa64fc4db:renderEnabled",
            ),
            findingIds,
        )
    }

    @Test
    fun `inline annotations and extension receivers stay part of maintainability detection`() {
        val repoRoot =
            createTempRepo(
                sourceFiles =
                    mapOf(
                        "client/src/main/kotlin/com/ktome/client/sample/Extensions.kt" to
                            """
                            package com.ktome.client.sample

                            @Deprecated("legacy") fun String.render(renderEnabled: Boolean): String = if (renderEnabled) this else uppercase()

                            fun Int.render(renderEnabled: Boolean): String = if (renderEnabled) toString() else "off"
                            """.trimIndent(),
                    ),
                baseline = emptyBaseline(),
            )

        val findingIds =
            MaintainabilityLintRunner.collectFindings(repoRoot)
                .map { finding -> finding.findingId }
                .filter { findingId -> findingId.startsWith("option-sprawl:boolean:client/src/main/kotlin/com/ktome/client/sample/Extensions.kt:") }
                .sorted()

        assertEquals(
            listOf(
                "option-sprawl:boolean:client/src/main/kotlin/com/ktome/client/sample/Extensions.kt:Int.render@e84fa64fc4db:renderEnabled",
                "option-sprawl:boolean:client/src/main/kotlin/com/ktome/client/sample/Extensions.kt:String.render@e84fa64fc4db:renderEnabled",
            ),
            findingIds,
        )
    }

    @Test
    fun `parameter parser keeps lambda defaults and backtick identifiers intact`() {
        val repoRoot =
            createTempRepo(
                sourceFiles =
                    mapOf(
                        "tools/src/main/kotlin/com/ktome/tools/sample/LambdaDefaults.kt" to
                            """
                            package com.ktome.tools.sample

                            class RuntimeApi {
                                fun evaluate(
                                    `class`: Boolean,
                                    onPair: (String, String) -> String = { left, right -> "${'$'}left,${'$'}right" },
                                    includeFallback: Boolean,
                                    traceEnabled: Boolean,
                                    auditEnabled: Boolean,
                                ): String = onPair("left", "right")
                            }
                            """.trimIndent(),
                    ),
                baseline = emptyBaseline(),
            )

        val findings = MaintainabilityLintRunner.collectFindings(repoRoot)
        val findingPrefix =
            "tools/src/main/kotlin/com/ktome/tools/sample/LambdaDefaults.kt:RuntimeApi.evaluate@"
        val matrixFinding =
            findings.single { finding ->
                finding.findingId.startsWith("option-sprawl:parameter-matrix:$findingPrefix")
            }
        val signatureId =
            matrixFinding.findingId.substringAfter("option-sprawl:parameter-matrix:tools/src/main/kotlin/com/ktome/tools/sample/LambdaDefaults.kt:")
        val booleanFindingIds =
            findings
                .map { finding -> finding.findingId }
                .filter { findingId -> findingId.startsWith("option-sprawl:boolean:tools/src/main/kotlin/com/ktome/tools/sample/LambdaDefaults.kt:$signatureId:") }
                .sorted()

        assertEquals(
            listOf(
                "option-sprawl:boolean:tools/src/main/kotlin/com/ktome/tools/sample/LambdaDefaults.kt:$signatureId:`class`",
                "option-sprawl:boolean:tools/src/main/kotlin/com/ktome/tools/sample/LambdaDefaults.kt:$signatureId:auditEnabled",
                "option-sprawl:boolean:tools/src/main/kotlin/com/ktome/tools/sample/LambdaDefaults.kt:$signatureId:includeFallback",
                "option-sprawl:boolean:tools/src/main/kotlin/com/ktome/tools/sample/LambdaDefaults.kt:$signatureId:traceEnabled",
            ),
            booleanFindingIds,
        )
        assertEquals("evaluate", matrixFinding.details.getValue("functionName").jsonPrimitive.content)
        assertEquals("5", matrixFinding.currentValueText)
    }

    @Test
    fun `report only mode keeps failing summary but does not enforce a green gate`() {
        val repoRoot = createTempRepo(
            sourceFiles =
                mapOf(
                    "core/src/main/kotlin/com/ktome/core/sample/DemoRuntime.kt" to
                        """
                        package com.ktome.core.sample

                        class DemoHelper
                        """.trimIndent(),
                ),
            baseline = emptyBaseline(),
        )

        val run =
            withRunnerProperties(
                repoRoot = repoRoot,
                baselinePath = repoRoot.resolve("maintainability-baseline.json"),
                reportDir = tempDir.resolve("report-only-report"),
                blockingMode = false,
            ) {
                MaintainabilityLintRunner.run()
            }

        assertEquals(EvaluationVerdict.FAIL.name, run.summary.verdict)
        assertEquals("REPORT_ONLY", run.summary.gateMode)
        assertTrue(run.summaryPath.exists())
        assertTrue(run.findingsPath.exists())
    }

    @Test
    fun `blocking mode throws after writing failing artifacts`() {
        val repoRoot = createTempRepo(
            sourceFiles =
                mapOf(
                    "core/src/main/kotlin/com/ktome/core/sample/DemoRuntime.kt" to
                        """
                        package com.ktome.core.sample

                        class DemoHelper
                        """.trimIndent(),
                ),
            baseline = emptyBaseline(),
        )
        val reportDir = tempDir.resolve("blocking-failure-report")

        val exception =
            assertThrows(IllegalStateException::class.java) {
                withRunnerProperties(
                    repoRoot = repoRoot,
                    baselinePath = repoRoot.resolve("maintainability-baseline.json"),
                    reportDir = reportDir,
                    blockingMode = true,
                ) {
                    MaintainabilityLintRunner.run()
                }
            }

        assertTrue(exception.message!!.contains("blocking mode"))
        assertTrue(reportDir.resolve("summary.json").exists())
        assertTrue(reportDir.resolve("findings.json").exists())
        val summary = json.decodeFromString<MaintainabilityLintSummary>(reportDir.resolve("summary.json").readText())
        assertEquals(EvaluationVerdict.FAIL.name, summary.verdict)
        assertEquals("BLOCKING", summary.gateMode)
    }

    @Test
    fun `maintainability baseline file uses approved debt schema`() {
        val baseline = VerificationBaseline.read(repoRoot().resolve("maintainability-baseline.json"))

        assertEquals(VERIFICATION_BASELINE_SCHEMA_VERSION, baseline.schemaVersion)
        assertEquals("maintainability", baseline.domainId)
        assertEquals(BaselineMode.APPROVED_DEBT_SET, baseline.mode)
        assertTrue(baseline.approvedDebtKeys.isNotEmpty())
    }

    private fun createTempRepo(
        sourceFiles: Map<String, String>,
        baseline: VerificationBaseline,
    ): Path {
        val repoRoot = tempDir.resolve("repo-${sourceFiles.size}-${baseline.approvedDebtKeys.size}")
        sourceFiles.forEach { (relativePath, content) ->
            val target = repoRoot.resolve(relativePath)
            Files.createDirectories(target.parent)
            target.writeText(content)
        }
        writeBaseline(repoRoot.resolve("maintainability-baseline.json"), baseline)
        return repoRoot
    }

    private fun writeBaseline(
        path: Path,
        baseline: VerificationBaseline,
    ) {
        Files.createDirectories(path.parent)
        path.writeText(json.encodeToString(baseline))
    }

    private fun emptyBaseline(): VerificationBaseline = baselineWithDebt(emptyList())

    private fun baselineWithDebt(approvedDebtKeys: List<String>): VerificationBaseline =
        VerificationBaseline(
            schemaVersion = VERIFICATION_BASELINE_SCHEMA_VERSION,
            baselineId = "maintainability-baseline-v1",
            domainId = "maintainability",
            mode = BaselineMode.APPROVED_DEBT_SET,
            metricDefinitionVersion = "maintainability-lint-v1",
            approvedDebtKeys = approvedDebtKeys.sorted(),
        )

    private fun repoRoot(): Path =
        System.getProperty("ktome.repo.root")
            ?.let(Path::of)
            ?: Path.of("").toAbsolutePath().normalize()

    private fun <T> withRunnerProperties(
        repoRoot: Path,
        baselinePath: Path,
        reportDir: Path,
        blockingMode: Boolean = false,
        action: () -> T,
    ): T {
        val previousRepoRoot = System.getProperty("ktome.repo.root")
        val previousBaselinePath = System.getProperty("ktome.maintainability.baselinePath")
        val previousReportDir = System.getProperty("ktome.maintainability.reportDir")
        val previousBlockingMode = System.getProperty("ktome.maintainability.blockingMode")
        System.setProperty("ktome.repo.root", repoRoot.toString())
        System.setProperty("ktome.maintainability.baselinePath", baselinePath.toString())
        System.setProperty("ktome.maintainability.reportDir", reportDir.toString())
        System.setProperty("ktome.maintainability.blockingMode", blockingMode.toString())
        return try {
            action()
        } finally {
            restoreProperty("ktome.repo.root", previousRepoRoot)
            restoreProperty("ktome.maintainability.baselinePath", previousBaselinePath)
            restoreProperty("ktome.maintainability.reportDir", previousReportDir)
            restoreProperty("ktome.maintainability.blockingMode", previousBlockingMode)
        }
    }

    private fun restoreProperty(
        key: String,
        value: String?,
    ) {
        if (value == null) {
            System.clearProperty(key)
        } else {
            System.setProperty(key, value)
        }
    }
}
