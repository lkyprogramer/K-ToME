package com.ktome.tools.verification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LegacyJUnitClassSetExecutorTest {
    @Test
    fun `selected classes execute without relying on tags`() {
        val node =
            VerificationNodeSpec(
                nodeId = "demo.staticGraph",
                description = "demo",
                workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                tier = VerificationTier.PREFLIGHT,
                nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                selectedClasses = listOf(VerificationDemoProbeTest::class.java.name),
            )

        val rawResult = LegacyJUnitClassSetExecutor.execute("demo", VerificationTier.PREFLIGHT, node)

        assertEquals(2, rawResult.totalTests)
        assertEquals(0, rawResult.failedTests)
        assertTrue(rawResult.tests.all { result -> result.status == "SUCCESSFUL" })
    }

    @Test
    fun `executor reports failing test cases and captures error messages`() {
        val node =
            VerificationNodeSpec(
                nodeId = "demo.failingFixture",
                description = "failing fixture",
                workloadClass = VerificationWorkloadClass.STATIC_GRAPH,
                tier = VerificationTier.PREFLIGHT,
                nodeKind = VerificationNodeKind.LEGACY_JUNIT_CLASS_SET,
                selectedClasses = listOf(VerificationFailingProbeFixture::class.java.name),
            )

        val rawResult = LegacyJUnitClassSetExecutor.execute("demo", VerificationTier.PREFLIGHT, node)

        assertEquals(2, rawResult.totalTests)
        assertEquals(1, rawResult.failedTests)
        assertEquals(1, rawResult.tests.count { result -> result.status == "FAILED" })
        assertTrue(
            rawResult.tests
                .first { result -> result.status == "FAILED" }
                .errorMessage
                ?.contains("expected") == true,
        )
    }
}
