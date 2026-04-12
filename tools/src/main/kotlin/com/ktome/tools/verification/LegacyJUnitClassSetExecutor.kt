package com.ktome.tools.verification

import kotlin.system.measureTimeMillis
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import org.junit.platform.launcher.TagFilter

internal object LegacyJUnitClassSetExecutor {
    fun execute(
        domainId: String,
        tier: VerificationTier,
        node: VerificationNodeSpec,
    ): LegacyJUnitRawResult {
        require(node.selectedClasses.isNotEmpty() || node.selectedTags.isNotEmpty()) {
            "Verification node ${node.nodeId} must declare selectedClasses or selectedTags for legacy JUnit execution."
        }

        val request = buildRequest(node)
        val summaryListener = SummaryGeneratingListener()
        val collectingListener = CollectingListener()
        val launcher = LauncherFactory.create()
        val durationMillis =
            measureTimeMillis {
                launcher.execute(request, summaryListener, collectingListener)
            }

        val summary = summaryListener.summary
        val tests = collectingListener.completedTests()
        val failedTests = summary.totalFailureCount.toInt()

        return LegacyJUnitRawResult(
            domainId = domainId,
            tier = tier.name,
            nodeId = node.nodeId,
            selectedClasses = node.selectedClasses.sorted(),
            selectedTags = node.selectedTags.sorted(),
            totalTests = tests.size,
            failedTests = failedTests,
            durationMillis = durationMillis,
            tests = tests,
        )
    }

    private fun buildRequest(node: VerificationNodeSpec): LauncherDiscoveryRequest {
        val builder = LauncherDiscoveryRequestBuilder.request()
        node.selectedClasses.sorted().forEach { className ->
            builder.selectors(selectClass(className))
        }
        if (node.selectedTags.isNotEmpty()) {
            builder.filters(TagFilter.includeTags(*node.selectedTags.sorted().toTypedArray()))
        }
        return builder.build()
    }

    private class CollectingListener : TestExecutionListener {
        private val results = linkedMapOf<String, VerificationTestCaseResult>()

        override fun testPlanExecutionStarted(testPlan: TestPlan) {
            results.clear()
        }

        override fun executionFinished(
            testIdentifier: TestIdentifier,
            testExecutionResult: org.junit.platform.engine.TestExecutionResult,
        ) {
            if (!testIdentifier.isTest) {
                return
            }
            results[testIdentifier.uniqueId] =
                VerificationTestCaseResult(
                    uniqueId = testIdentifier.uniqueId,
                    displayName = testIdentifier.displayName,
                    status = testExecutionResult.status.name,
                    errorMessage = testExecutionResult.throwable.map(Throwable::message).orElse(null),
                )
        }

        fun completedTests(): List<VerificationTestCaseResult> =
            results.values.sortedWith(compareBy({ it.status != "SUCCESSFUL" }, { it.uniqueId }))
    }
}
