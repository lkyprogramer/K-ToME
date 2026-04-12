package com.ktome.tools.verification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VerificationDomainSpecTest {
    @Test
    fun `default tier must map to a concrete node`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                VerificationDomainSpec(
                    domainId = "demo",
                    phaseIds = setOf("phase4"),
                    workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                    defaultTier = VerificationTier.PREFLIGHT,
                    nodeSpecs =
                        listOf(
                            VerificationNodeSpec(
                                nodeId = "demo.owner",
                                description = "owner only",
                                workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                                tier = VerificationTier.OWNER,
                                nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                                selectedClasses = listOf(VerificationDemoProbeTest::class.java.name),
                            ),
                        ),
                    cachePolicy =
                        VerificationCachePolicy(
                            buildCacheEnabled = true,
                            configurationCacheCompatible = true,
                        ),
                    artifactPolicy = VerificationArtifactPolicy(),
                )
            }

        assertTrue(exception.message!!.contains("default tier"))
    }

    @Test
    fun `registry exposes contractLint static graph foundation`() {
        val spec = VerificationTaskRegistry.spec("contractLint")

        assertEquals(VerificationWorkloadClass.STATIC_GRAPH, spec.workloadClass)
        assertEquals(VerificationTier.PREFLIGHT, spec.defaultTier)
        assertEquals(BaselineMode.STRICT_ZERO_FAILURE, spec.baselinePolicy?.mode)
        assertEquals(1, spec.nodeSpecs.size)
        assertEquals("contractLint.staticGraph", spec.nodeSpecs.single().nodeId)
    }

    @Test
    fun `registry exposes phase4 loot input scopes and task routing`() {
        val spec = VerificationTaskRegistry.spec("loot")

        assertEquals(VerificationTier.PREFLIGHT, spec.defaultTier)
        assertTrue(spec.inputScopes.any { scope -> scope.scopeId == "loot.data.loot" && !scope.ownerRequired })
        assertTrue(spec.inputScopes.any { scope -> scope.scopeId == "loot.runtime" && scope.ownerRequired })
        assertEquals(listOf(":tools:verifyLootPreflight"), spec.preflightTaskPaths)
        assertEquals(listOf(":tools:lootBalanceLab"), spec.ownerTaskPaths)
    }

    @Test
    fun `resolve node fails when tier is ambiguous without node id`() {
        val spec =
            VerificationDomainSpec(
                domainId = "demo",
                phaseIds = setOf("phase4"),
                workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                defaultTier = VerificationTier.PREFLIGHT,
                nodeSpecs =
                    listOf(
                        VerificationNodeSpec(
                            nodeId = "demo.preflight.a",
                            description = "first",
                            workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                            tier = VerificationTier.PREFLIGHT,
                            nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                            selectedClasses = listOf(VerificationDemoProbeTest::class.java.name),
                        ),
                        VerificationNodeSpec(
                            nodeId = "demo.preflight.b",
                            description = "second",
                            workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                            tier = VerificationTier.PREFLIGHT,
                            nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                            selectedClasses = listOf(VerificationDemoProbeTest::class.java.name),
                        ),
                    ),
                cachePolicy =
                    VerificationCachePolicy(
                        buildCacheEnabled = true,
                        configurationCacheCompatible = true,
                    ),
                artifactPolicy = VerificationArtifactPolicy(),
            )

        val exception =
            assertThrows(IllegalStateException::class.java) {
                spec.resolveNode(VerificationTier.PREFLIGHT)
            }

        assertTrue(exception.message!!.contains("multiple nodes"))
    }

    @Test
    fun `resolve node accepts explicit node id when tier has multiple nodes`() {
        val spec =
            VerificationDomainSpec(
                domainId = "demo",
                phaseIds = setOf("phase4"),
                workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                defaultTier = VerificationTier.PREFLIGHT,
                nodeSpecs =
                    listOf(
                        VerificationNodeSpec(
                            nodeId = "demo.preflight.a",
                            description = "first",
                            workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                            tier = VerificationTier.PREFLIGHT,
                            nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                            selectedClasses = listOf(VerificationDemoProbeTest::class.java.name),
                        ),
                        VerificationNodeSpec(
                            nodeId = "demo.preflight.b",
                            description = "second",
                            workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                            tier = VerificationTier.PREFLIGHT,
                            nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                            selectedClasses = listOf(VerificationDemoProbeTest::class.java.name),
                            dependsOn = setOf("demo.preflight.a"),
                        ),
                    ),
                cachePolicy =
                    VerificationCachePolicy(
                        buildCacheEnabled = true,
                        configurationCacheCompatible = true,
                    ),
                artifactPolicy = VerificationArtifactPolicy(),
            )

        val node = spec.resolveNode(VerificationTier.PREFLIGHT, "demo.preflight.b")

        assertEquals("demo.preflight.b", node.nodeId)
        assertEquals(setOf("demo.preflight.a"), node.dependsOn)
    }

    @Test
    fun `unknown node dependency is rejected`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                VerificationDomainSpec(
                    domainId = "demo",
                    phaseIds = setOf("phase4"),
                    workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                    defaultTier = VerificationTier.PREFLIGHT,
                    nodeSpecs =
                        listOf(
                            VerificationNodeSpec(
                                nodeId = "demo.preflight",
                                description = "only",
                                workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                                tier = VerificationTier.PREFLIGHT,
                                nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                                dependsOn = setOf("demo.missing"),
                                selectedClasses = listOf(VerificationDemoProbeTest::class.java.name),
                            ),
                        ),
                    cachePolicy =
                        VerificationCachePolicy(
                            buildCacheEnabled = true,
                            configurationCacheCompatible = true,
                        ),
                    artifactPolicy = VerificationArtifactPolicy(),
                )
            }

        assertTrue(exception.message!!.contains("unknown node dependencies"))
    }

    @Test
    fun `node workload class must stay aligned with domain workload class`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                VerificationDomainSpec(
                    domainId = "demo",
                    phaseIds = setOf("phase4"),
                    workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                    defaultTier = VerificationTier.PREFLIGHT,
                    nodeSpecs =
                        listOf(
                            VerificationNodeSpec(
                                nodeId = "demo.preflight",
                                description = "only",
                                workloadClass = VerificationWorkloadClass.DETERMINISTIC_SCENARIO,
                                tier = VerificationTier.PREFLIGHT,
                                nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                                selectedClasses = listOf(VerificationDemoProbeTest::class.java.name),
                            ),
                        ),
                    cachePolicy =
                        VerificationCachePolicy(
                            buildCacheEnabled = true,
                            configurationCacheCompatible = true,
                        ),
                    artifactPolicy = VerificationArtifactPolicy(),
                )
            }

        assertTrue(exception.message!!.contains("workloadClass diverges"))
    }
}
