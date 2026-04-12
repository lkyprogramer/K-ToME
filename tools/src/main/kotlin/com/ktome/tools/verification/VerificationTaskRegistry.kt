package com.ktome.tools.verification

object VerificationTaskRegistry {
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
            cachePolicy =
                VerificationCachePolicy(
                    buildCacheEnabled = true,
                    configurationCacheCompatible = true,
                    reuseExistingArtifacts = true,
                ),
            artifactPolicy = VerificationArtifactPolicy(),
        )

    private val domainsById: Map<String, VerificationDomainSpec> =
        listOf(contractLintDomain).associateBy { it.domainId }

    fun spec(domainId: String): VerificationDomainSpec =
        domainsById[domainId] ?: error("No verification domain registered for $domainId.")

    fun registeredDomainIds(): Set<String> = domainsById.keys
}
