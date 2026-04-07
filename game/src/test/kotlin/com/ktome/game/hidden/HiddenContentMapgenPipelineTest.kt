package com.ktome.game.hidden

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.mapgen.GeneratedEntrance
import com.ktome.core.mapgen.GeneratedFloor
import com.ktome.core.mapgen.MapgenPipeline
import com.ktome.core.mapgen.MapgenRequest
import com.ktome.core.mapgen.NodeId
import com.ktome.core.mapgen.PathClass
import com.ktome.core.mapgen.RoomInstance
import com.ktome.core.mapgen.RoomShape
import com.ktome.core.mapgen.TopologyEdge
import com.ktome.core.mapgen.TopologyGraph
import com.ktome.core.mapgen.TopologyNode
import com.ktome.core.world.solvability.ContentRef
import com.ktome.core.world.solvability.DiscoveryPredicate
import com.ktome.core.world.solvability.DiscoveryPredicateType
import com.ktome.core.world.solvability.DiscoveryRule
import com.ktome.core.world.solvability.NodeAnchorId
import com.ktome.core.world.solvability.RegistryId
import com.ktome.core.world.solvability.SearchBindingId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test

class HiddenContentMapgenPipelineTest {
    @Test
    fun `nearest optional anchor resolves nearest optional node`() {
        val pipeline = pipeline(secretZone(returnBridgePolicy = ReturnBridgePolicy.NEAREST_OPTIONAL_ANCHOR))

        val resolvedFloor = pipeline.run(request())

        assertEquals(OPTIONAL_NODE_ID, resolvedFloor.entrances.single().resolvedReturnBridgeNodeId)
    }

    @Test
    fun `last mainline branch resolves nearest primary path node`() {
        val pipeline = pipeline(secretZone(returnBridgePolicy = ReturnBridgePolicy.LAST_MAINLINE_BRANCH))

        val resolvedFloor = pipeline.run(request())

        assertEquals(HUB_NODE_ID, resolvedFloor.entrances.single().resolvedReturnBridgeNodeId)
    }

    @Test
    fun `nearest optional anchor fails when resolved optional node cannot reach exit`() {
        val pipeline =
            pipeline(
                secretZone(returnBridgePolicy = ReturnBridgePolicy.NEAREST_OPTIONAL_ANCHOR),
                floor =
                    testFloor(
                        edges =
                            listOf(
                                TopologyEdge(from = START_NODE_ID, to = HUB_NODE_ID),
                                TopologyEdge(from = HUB_NODE_ID, to = EXIT_NODE_ID),
                                TopologyEdge(from = OPTIONAL_NODE_ID, to = SECRET_NODE_ID),
                            ),
                    ),
            )

        assertThrows<IllegalArgumentException> {
            pipeline.run(request())
        }
    }

    @Test
    fun `last mainline branch fails when entrance branch is disconnected from mainline`() {
        val pipeline =
            pipeline(
                secretZone(returnBridgePolicy = ReturnBridgePolicy.LAST_MAINLINE_BRANCH),
                floor =
                    testFloor(
                        edges =
                            listOf(
                                TopologyEdge(from = START_NODE_ID, to = HUB_NODE_ID),
                                TopologyEdge(from = HUB_NODE_ID, to = EXIT_NODE_ID),
                                TopologyEdge(from = OPTIONAL_NODE_ID, to = SECRET_NODE_ID),
                            ),
                    ),
            )

        assertThrows<IllegalArgumentException> {
            pipeline.run(request())
        }
    }

    @Test
    fun `explicit anchor resolves tagged node`() {
        val pipeline =
            pipeline(
                secretZone(
                    returnBridgePolicy = ReturnBridgePolicy.EXPLICIT_ANCHOR,
                    returnBridgeAnchorTag = "hub",
                ),
            )

        val resolvedFloor = pipeline.run(request())

        assertEquals(HUB_NODE_ID, resolvedFloor.entrances.single().resolvedReturnBridgeNodeId)
    }

    @Test
    fun `pipeline fails when secret zone entrance anchor drifts from generated entrance`() {
        val pipeline =
            pipeline(
                secretZone(
                    returnBridgePolicy = ReturnBridgePolicy.LAST_MAINLINE_BRANCH,
                    entranceBindingId = NodeAnchorId("optional.branch.missing"),
                ),
            )

        assertThrows<IllegalArgumentException> {
            pipeline.run(request())
        }
    }

    @Test
    fun `explicit anchor fails when tag cannot resolve a node`() {
        val pipeline =
            pipeline(
                secretZone(
                    returnBridgePolicy = ReturnBridgePolicy.EXPLICIT_ANCHOR,
                    returnBridgeAnchorTag = "missing",
                ),
            )

        assertThrows<IllegalArgumentException> {
            pipeline.run(request())
        }
    }

    @Test
    fun `explicit anchor fails when tagged node is secret`() {
        val pipeline =
            pipeline(
                secretZone(
                    returnBridgePolicy = ReturnBridgePolicy.EXPLICIT_ANCHOR,
                    returnBridgeAnchorTag = "hub",
                ),
                floor = testFloor(hubTags = emptySet(), secretTags = setOf("hub")),
            )

        assertThrows<IllegalArgumentException> {
            pipeline.run(request())
        }
    }

    @Test
    fun `explicit anchor fails when tagged node cannot reach exit`() {
        val pipeline =
            pipeline(
                secretZone(
                    returnBridgePolicy = ReturnBridgePolicy.EXPLICIT_ANCHOR,
                    returnBridgeAnchorTag = "isolated",
                ),
                floor = testFloor(isolatedTags = setOf("isolated")),
            )

        assertThrows<IllegalArgumentException> {
            pipeline.run(request())
        }
    }

    private fun pipeline(
        secretZone: SecretZoneDef,
        floor: GeneratedFloor = testFloor(),
    ): HiddenContentMapgenPipeline =
        HiddenContentMapgenPipeline(
            delegate =
                object : MapgenPipeline {
                    override fun run(request: MapgenRequest): GeneratedFloor = floor
                },
            secretZoneRegistry = SecretZoneRegistry(mapOf(secretZone.id.id to secretZone)),
        )

    private fun secretZone(
        returnBridgePolicy: ReturnBridgePolicy,
        entranceBindingId: NodeAnchorId = OPTIONAL_ANCHOR_ID,
        returnBridgeAnchorTag: String? = null,
    ): SecretZoneDef =
        SecretZoneDef(
            id = SECRET_ZONE_REF,
            nameKey = "zone.secret.test.name",
            descKey = "zone.secret.test.desc",
            visualKey = "zone.secret.test.visual",
            iconKey = "zone.secret.test.icon",
            audioProfile = "audio.secret_zone.test",
            schemaVersion = 1,
            tags = listOf("secret_zone", "test"),
            entryRule = DISCOVERY_RULE,
            pathClass = PathClass.SECRET,
            rewardProfileId = ContentRef(registry = RegistryId(LOOT_PROFILE_REGISTRY_ID), id = "loot.test.secret"),
            guaranteedContent = listOf(ContentRef(registry = RegistryId(HIDDEN_EVENT_REGISTRY_ID), id = "hidden.event.test.reward")),
            entranceBindingId = entranceBindingId,
            returnBridgePolicy = returnBridgePolicy,
            returnBridgeAnchorTag = returnBridgeAnchorTag,
        )

    private fun request(): MapgenRequest =
        MapgenRequest(
            zoneId = "greenwood_fringe",
            floorIndex = 1,
            seed = 42L,
            targetWidth = 32,
            targetHeight = 8,
        )

    private fun testFloor(
        hubTags: Set<String> = setOf("hub"),
        secretTags: Set<String> = setOf("secret"),
        isolatedTags: Set<String> = emptySet(),
        edges: List<TopologyEdge> =
            listOf(
                TopologyEdge(from = START_NODE_ID, to = HUB_NODE_ID),
                TopologyEdge(from = HUB_NODE_ID, to = OPTIONAL_NODE_ID),
                TopologyEdge(from = HUB_NODE_ID, to = OPTIONAL_TWO_NODE_ID),
                TopologyEdge(from = HUB_NODE_ID, to = EXIT_NODE_ID),
                TopologyEdge(from = OPTIONAL_NODE_ID, to = SECRET_NODE_ID),
            ),
    ): GeneratedFloor {
        val map = GameMap.fromAscii(rows = List(8) { ".".repeat(32) }, playerStart = Point(1, 1))
        val topology =
            TopologyGraph(
                nodes =
                    listOf(
                        topologyNode(id = START_NODE_ID, anchorId = START_ANCHOR_ID, pathClass = PathClass.CRITICAL_PATH, tags = setOf("start")),
                        topologyNode(id = HUB_NODE_ID, anchorId = HUB_ANCHOR_ID, pathClass = PathClass.CRITICAL_PATH, tags = hubTags),
                        topologyNode(id = OPTIONAL_NODE_ID, anchorId = OPTIONAL_ANCHOR_ID, pathClass = PathClass.OPTIONAL, tags = setOf("optional")),
                        topologyNode(id = OPTIONAL_TWO_NODE_ID, anchorId = OPTIONAL_TWO_ANCHOR_ID, pathClass = PathClass.OPTIONAL, tags = setOf("optional")),
                        topologyNode(id = SECRET_NODE_ID, anchorId = SECRET_ANCHOR_ID, pathClass = PathClass.SECRET, tags = secretTags),
                        topologyNode(id = EXIT_NODE_ID, anchorId = EXIT_ANCHOR_ID, pathClass = PathClass.CRITICAL_PATH, tags = setOf("exit")),
                        topologyNode(id = ISOLATED_NODE_ID, anchorId = ISOLATED_ANCHOR_ID, pathClass = PathClass.OPTIONAL, tags = isolatedTags),
                    ),
                edges = edges,
                primaryPathNodeIds = listOf(START_NODE_ID, HUB_NODE_ID, EXIT_NODE_ID),
                optionalLoopCount = 1,
            )
        return GeneratedFloor.compatibility(
            zoneId = "greenwood_fringe",
            floorIndex = 1,
            seed = 42L,
            map = map,
            topology = topology,
            rooms =
                listOf(
                    roomInstance(nodeId = START_NODE_ID, anchorId = START_ANCHOR_ID, x = 1),
                    roomInstance(nodeId = HUB_NODE_ID, anchorId = HUB_ANCHOR_ID, x = 5),
                    roomInstance(nodeId = OPTIONAL_NODE_ID, anchorId = OPTIONAL_ANCHOR_ID, x = 9, pathClass = PathClass.OPTIONAL),
                    roomInstance(nodeId = OPTIONAL_TWO_NODE_ID, anchorId = OPTIONAL_TWO_ANCHOR_ID, x = 13, pathClass = PathClass.OPTIONAL),
                    roomInstance(nodeId = SECRET_NODE_ID, anchorId = SECRET_ANCHOR_ID, x = 17, pathClass = PathClass.SECRET),
                    roomInstance(nodeId = EXIT_NODE_ID, anchorId = EXIT_ANCHOR_ID, x = 21),
                    roomInstance(nodeId = ISOLATED_NODE_ID, anchorId = ISOLATED_ANCHOR_ID, x = 25, pathClass = PathClass.OPTIONAL),
                ),
            entrances =
                listOf(
                    GeneratedEntrance(
                        bindingId = SEARCH_BINDING_ID,
                        fromNodeId = OPTIONAL_NODE_ID,
                        targetNodeId = SECRET_NODE_ID,
                        entranceAnchorId = OPTIONAL_ANCHOR_ID,
                        targetAnchorId = SECRET_ANCHOR_ID,
                        pathClass = PathClass.SECRET,
                        discoveryRule = DISCOVERY_RULE,
                        targetSecretZoneId = SECRET_ZONE_REF,
                    ),
                ),
        )
    }

    private fun topologyNode(
        id: NodeId,
        anchorId: NodeAnchorId,
        pathClass: PathClass,
        tags: Set<String>,
    ): TopologyNode =
        TopologyNode(
            id = id,
            anchorId = anchorId,
            roomDefId = "room.${id.value}",
            pathClass = pathClass,
            tags = tags,
        )

    private fun roomInstance(
        nodeId: NodeId,
        anchorId: NodeAnchorId,
        x: Int,
        pathClass: PathClass = PathClass.CRITICAL_PATH,
    ): RoomInstance =
        RoomInstance(
            nodeId = nodeId,
            anchorId = anchorId,
            roomDefId = "room.${nodeId.value}",
            x = x,
            y = 1,
            width = 3,
            height = 3,
            shape = RoomShape.RECT,
            pathClass = pathClass,
            tags = emptySet(),
        )

    private companion object {
        val START_NODE_ID: NodeId = NodeId("start")
        val HUB_NODE_ID: NodeId = NodeId("hub")
        val OPTIONAL_NODE_ID: NodeId = NodeId("optional")
        val OPTIONAL_TWO_NODE_ID: NodeId = NodeId("optional-two")
        val SECRET_NODE_ID: NodeId = NodeId("secret")
        val EXIT_NODE_ID: NodeId = NodeId("exit")
        val ISOLATED_NODE_ID: NodeId = NodeId("isolated")

        val START_ANCHOR_ID: NodeAnchorId = NodeAnchorId("critical.start")
        val HUB_ANCHOR_ID: NodeAnchorId = NodeAnchorId("critical.hub")
        val OPTIONAL_ANCHOR_ID: NodeAnchorId = NodeAnchorId("optional.branch.1")
        val OPTIONAL_TWO_ANCHOR_ID: NodeAnchorId = NodeAnchorId("optional.branch.2")
        val SECRET_ANCHOR_ID: NodeAnchorId = NodeAnchorId("secret.cache.1")
        val EXIT_ANCHOR_ID: NodeAnchorId = NodeAnchorId("critical.exit")
        val ISOLATED_ANCHOR_ID: NodeAnchorId = NodeAnchorId("optional.isolated")

        val SEARCH_BINDING_ID: SearchBindingId = SearchBindingId("search.test.secret")
        val SECRET_ZONE_REF: ContentRef = ContentRef(registry = RegistryId(SECRET_ZONE_REGISTRY_ID), id = "test_secret_zone")
        val DISCOVERY_RULE: DiscoveryRule =
            DiscoveryRule(
                predicates =
                    listOf(
                        DiscoveryPredicate(
                            type = DiscoveryPredicateType.PERCEPTION_CHECK,
                            difficulty = 8,
                        ),
                    ),
            )
    }
}
