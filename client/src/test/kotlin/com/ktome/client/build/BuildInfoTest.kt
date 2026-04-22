package com.ktome.client.build

import java.util.Properties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BuildInfoTest {
    @AfterEach
    fun resetBuildInfo() {
        BuildInfo.resetForTest()
    }

    @Test
    fun `resolved short hash is cached without fallback warning`() {
        val warnings = mutableListOf<String>()
        val snapshot =
            BuildInfo.initializeForTest(
                properties =
                    buildProperties(
                        shortHash = "abcdef1",
                        shortHashResolved = "true",
                    ),
                warningSink = warnings::add,
            )

        assertEquals("abcdef1", snapshot.shortHash)
        assertTrue(snapshot.hashResolved)
        assertEquals("abcdef1", BuildInfo.shortHash)
        assertTrue(BuildInfo.shortHash.matches(Regex("unknown|[0-9a-f]{7,40}")))
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `unresolved short hash falls back and warns during initialize`() {
        val warnings = mutableListOf<String>()
        val snapshot =
            BuildInfo.initializeForTest(
                properties =
                    buildProperties(
                        shortHash = "unknown",
                        shortHashResolved = "false",
                    ),
                warningSink = warnings::add,
            )

        assertEquals("unknown", snapshot.shortHash)
        assertFalse(snapshot.hashResolved)
        assertTrue(BuildInfo.shortHash.matches(Regex("unknown|[0-9a-f]{7,40}")))
        assertEquals(1, warnings.size)
        assertTrue(warnings.single().contains("BuildInfo.shortHash resolution failed"))
        assertTrue(warnings.single().contains("fell back to 'unknown'"))
        assertTrue(warnings.single().contains("not resolved"))
    }

    @Test
    fun `invalid short hash falls back and warns during initialize`() {
        val warnings = mutableListOf<String>()
        val snapshot =
            BuildInfo.initializeForTest(
                properties =
                    buildProperties(
                        shortHash = "not-a-hash",
                        shortHashResolved = "true",
                    ),
                warningSink = warnings::add,
            )

        assertEquals("unknown", snapshot.shortHash)
        assertFalse(snapshot.hashResolved)
        assertEquals(1, warnings.size)
        assertTrue(warnings.single().contains("invalid build hash"))
    }

    @Test
    fun `missing build info resource falls back and warns during initialize`() {
        val warnings = mutableListOf<String>()
        val snapshot =
            BuildInfo.initializeForTest(
                properties = null,
                warningSink = warnings::add,
            )

        assertEquals("unknown", snapshot.shortHash)
        assertFalse(snapshot.hashResolved)
        assertEquals(1, warnings.size)
        assertTrue(warnings.single().contains("missing resource"))
    }

    private fun buildProperties(
        shortHash: String,
        shortHashResolved: String,
    ): Properties =
        Properties().apply {
            setProperty("shortHash", shortHash)
            setProperty("shortHashResolved", shortHashResolved)
        }
}
