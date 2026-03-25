package com.ktome.game.data

import com.ktome.game.data.schema.ProfessionSchemaV2
import com.ktome.game.data.schema.TalentSchemaV2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

internal object ProfessionTreeAssertions {
    fun assertFixedTreeLayout(
        professionId: String,
        expectedTreeIds: List<String>,
    ) {
        val catalog = DataLoader().loadSchemaCatalog()
        val profession = requireNotNull(catalog.professions.firstOrNull { schema -> schema.id == professionId }) {
            "Missing profession '$professionId'."
        }
        val talentsById = catalog.talents.associateBy(TalentSchemaV2::id)

        assertEquals(expectedTreeIds, profession.talentTrees)
        profession.talentTrees.forEach { treeId ->
            val tree = requireNotNull(catalog.talentTrees.firstOrNull { schema -> schema.id == treeId }) {
                "Missing tree '$treeId'."
            }
            val treeTalents =
                tree.nodes.map { talentId ->
                    requireNotNull(talentsById[talentId]) { "Tree '$treeId' references unknown talent '$talentId'." }
                }
            val treeTalentIds = treeTalents.map(TalentSchemaV2::id).toSet()

            assertEquals(tree.nodes.distinct(), tree.nodes, "Tree '$treeId' must not repeat node ids.")
            assertEquals(tree.nodes, treeTalents.map(TalentSchemaV2::id), "Tree '$treeId' node order must match the tree definition.")
            assertTrue(treeTalents.size >= 2, "Tree '$treeId' should expose at least two talents.")
            assertTrue(treeTalents.any { talent -> talent.unlockLevel <= 2 }, "Tree '$treeId' should expose an early-game talent.")
            assertTraversablePrerequisiteGraph(treeId, treeTalents, treeTalentIds, tree.nodes)
            treeTalents.forEach { talent ->
                assertEquals(treeId, talent.treeId, "Tree '$treeId' contains talent '${talent.id}' with mismatched treeId '${talent.treeId}'.")
            }
        }
        assertTrue(
            profession.startingTalents.all { starterId -> starterId in talentsById.keys },
            "Profession '$professionId' contains an unknown starter talent.",
        )
    }

    fun assertSoloContractLint() {
        val catalog = DataLoader().loadSchemaCatalog()
        catalog.professions.forEach { profession ->
            assertNonEmpty(profession, "offenseTags", profession.soloContract.offenseTags)
            assertNonEmpty(profession, "defenseTags", profession.soloContract.defenseTags)
            assertNonEmpty(profession, "mobilityTags", profession.soloContract.mobilityTags)
            assertNonEmpty(profession, "aoeAnswerTags", profession.soloContract.aoeAnswerTags)
            assertNonEmpty(profession, "bossAnswerTags", profession.soloContract.bossAnswerTags)
            assertNonEmpty(profession, "panicAnswerTags", profession.soloContract.panicAnswerTags)
        }
    }

    private fun assertNonEmpty(
        profession: ProfessionSchemaV2,
        label: String,
        values: List<String>,
    ) {
        assertTrue(values.isNotEmpty(), "Profession '${profession.id}' must define non-empty soloContract.$label.")
    }

    private fun assertTraversablePrerequisiteGraph(
        treeId: String,
        treeTalents: List<TalentSchemaV2>,
        treeTalentIds: Set<String>,
        treeNodes: List<String>,
    ) {
        val childrenByPrereq = mutableMapOf<String, MutableList<String>>()
        val inDegreeByTalentId = treeTalents.associate { talent -> talent.id to 0 }.toMutableMap()
        val nodeIndexByTalentId = treeNodes.withIndex().associate { indexed -> indexed.value to indexed.index }

        treeTalents.forEach { talent ->
            talent.requirements.talentPrereqs.forEach { prereq ->
                assertTrue(
                    prereq.talentId in treeTalentIds,
                    "Tree '$treeId' talent '${talent.id}' depends on out-of-tree prerequisite '${prereq.talentId}'.",
                )
                assertTrue(
                    prereq.talentId != talent.id,
                    "Tree '$treeId' talent '${talent.id}' cannot depend on itself.",
                )
                assertTrue(
                    nodeIndexByTalentId.getValue(prereq.talentId) < nodeIndexByTalentId.getValue(talent.id),
                    "Tree '$treeId' prerequisite '${prereq.talentId}' must appear before '${talent.id}' in the node layout.",
                )
                childrenByPrereq.getOrPut(prereq.talentId) { mutableListOf() }.add(talent.id)
                inDegreeByTalentId[talent.id] = inDegreeByTalentId.getValue(talent.id) + 1
            }
        }

        val ready = ArrayDeque(treeTalents.filter { talent -> inDegreeByTalentId.getValue(talent.id) == 0 }.map(TalentSchemaV2::id))
        assertTrue(ready.isNotEmpty(), "Tree '$treeId' has no root talent and cannot be traversed.")

        val visited = linkedSetOf<String>()
        while (ready.isNotEmpty()) {
            val current = ready.removeFirst()
            if (!visited.add(current)) {
                continue
            }
            childrenByPrereq[current].orEmpty().sorted().forEach { childId ->
                val nextInDegree = inDegreeByTalentId.getValue(childId) - 1
                inDegreeByTalentId[childId] = nextInDegree
                if (nextInDegree == 0) {
                    ready.addLast(childId)
                }
            }
        }

        val unreachableTalents = treeTalents.map(TalentSchemaV2::id).filterNot(visited::contains)
        assertTrue(
            unreachableTalents.isEmpty(),
            "Tree '$treeId' prerequisite graph is not traversable; unreachable talents: $unreachableTalents.",
        )
        val unresolvedTalents = inDegreeByTalentId.filterValues { degree -> degree > 0 }.keys.sorted()
        assertTrue(
            unresolvedTalents.isEmpty(),
            "Tree '$treeId' prerequisite graph contains a cycle or unresolved dependency among: $unresolvedTalents.",
        )
    }
}
