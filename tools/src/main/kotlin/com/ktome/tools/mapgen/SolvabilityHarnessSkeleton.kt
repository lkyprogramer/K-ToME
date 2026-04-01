package com.ktome.tools.mapgen

import com.ktome.core.harness.toJson
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

data class SolvabilityHarnessSkeletonRun(
    val reportPath: Path,
    val status: String,
)

object SolvabilityHarnessSkeleton {
    const val HARNESS_ID: String = "solvabilityHarness"
    private const val REPORT_FILE: String = "solvability-harness-summary.json"

    fun run(): SolvabilityHarnessSkeletonRun {
        val reportDir = reportDir()
        Files.createDirectories(reportDir)
        val header = phase4HarnessHeader(harnessId = HARNESS_ID, seedList = emptyList())
        val payload =
            buildJsonObject {
                put("header", header.toJson())
                put("status", "PENDING_PR03")
                put("message", "PR-01 only freezes the solvabilityHarness alias and seed+zoneId+floorIndex input boundary.")
                putJsonObject("inputBoundary") {
                    put("joinKey", "seed+zoneId+floorIndex")
                    putJsonArray("pathClasses") {
                        add(JsonPrimitive("CRITICAL_PATH"))
                        add(JsonPrimitive("OPTIONAL"))
                        add(JsonPrimitive("SECRET"))
                    }
                }
            }
        val reportPath = reportDir.resolve(REPORT_FILE)
        Files.writeString(reportPath, Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), payload))
        return SolvabilityHarnessSkeletonRun(reportPath = reportPath, status = "PENDING_PR03")
    }

    private fun reportDir(): Path {
        val configured = System.getProperty("ktome.phase4.solvability.reportDir")
        return if (configured.isNullOrBlank()) {
            Path.of("tools", "build", "reports", "phase4", "solvability")
        } else {
            Path.of(configured)
        }
    }
}
