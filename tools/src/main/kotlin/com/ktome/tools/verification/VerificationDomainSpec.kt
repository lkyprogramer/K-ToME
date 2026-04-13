package com.ktome.tools.verification

data class VerificationDomainSpec(
    val domainId: String,
    val phaseIds: Set<String>,
    val workloadClass: VerificationWorkloadClass,
    val defaultTier: VerificationTier,
    val nodeSpecs: List<VerificationNodeSpec>,
    val inputScopes: List<InputScope> = emptyList(),
    val preflightTaskPaths: List<String> = emptyList(),
    val ownerTaskPaths: List<String> = emptyList(),
    val baselinePolicy: BaselinePolicySpec? = null,
    val cachePolicy: VerificationCachePolicy,
    val artifactPolicy: VerificationArtifactPolicy,
) {
    init {
        require(domainId.isNotBlank()) { "VerificationDomainSpec.domainId must not be blank." }
        require(phaseIds.isNotEmpty()) { "VerificationDomainSpec($domainId) must declare at least one phase id." }
        require(nodeSpecs.isNotEmpty()) { "VerificationDomainSpec($domainId) must declare at least one verification node." }

        val distinctNodeIds = nodeSpecs.map { it.nodeId }.toSet()
        require(distinctNodeIds.size == nodeSpecs.size) {
            "VerificationDomainSpec($domainId) must not contain duplicate node ids: ${nodeSpecs.map { it.nodeId }}."
        }
        require(nodeSpecs.any { it.tier == defaultTier }) {
            "VerificationDomainSpec($domainId) must declare a node for default tier $defaultTier."
        }
        val distinctScopeIds = inputScopes.map(InputScope::scopeId).toSet()
        require(distinctScopeIds.size == inputScopes.size) {
            "VerificationDomainSpec($domainId) must not contain duplicate input scope ids: ${inputScopes.map(InputScope::scopeId)}."
        }
        require(preflightTaskPaths.none(String::isBlank)) {
            "VerificationDomainSpec($domainId).preflightTaskPaths must not contain blank task paths."
        }
        require(ownerTaskPaths.none(String::isBlank)) {
            "VerificationDomainSpec($domainId).ownerTaskPaths must not contain blank task paths."
        }
        val missingDependencies =
            nodeSpecs.flatMap { node ->
                node.dependsOn
                    .filterNot(distinctNodeIds::contains)
                    .map { dependencyId -> "${node.nodeId} -> $dependencyId" }
            }
        require(missingDependencies.isEmpty()) {
            "VerificationDomainSpec($domainId) contains unknown node dependencies: $missingDependencies."
        }
    }

    fun node(nodeId: String): VerificationNodeSpec =
        nodeSpecs.firstOrNull { it.nodeId == nodeId }
            ?: error("VerificationDomainSpec($domainId) has no node registered for nodeId $nodeId.")

    fun nodesFor(tier: VerificationTier): List<VerificationNodeSpec> =
        nodeSpecs.filter { it.tier == tier }.sortedBy(VerificationNodeSpec::nodeId)

    fun declaredWorkloadClasses(): Set<VerificationWorkloadClass> =
        nodeSpecs.map(VerificationNodeSpec::workloadClass).toSet()

    fun resolveNode(
        tier: VerificationTier,
        nodeId: String? = null,
    ): VerificationNodeSpec {
        if (nodeId != null) {
            val resolvedNode = node(nodeId)
            require(resolvedNode.tier == tier) {
                "VerificationDomainSpec($domainId) node $nodeId is registered for tier ${resolvedNode.tier}, not $tier."
            }
            return resolvedNode
        }

        val tierNodes = nodesFor(tier)
        return when (tierNodes.size) {
            0 -> error("VerificationDomainSpec($domainId) has no node registered for tier $tier.")
            1 -> tierNodes.single()
            else ->
                error(
                    "VerificationDomainSpec($domainId) has multiple nodes for tier $tier: ${tierNodes.map { it.nodeId }}. " +
                        "Select one explicitly by nodeId.",
                )
        }
    }
}
