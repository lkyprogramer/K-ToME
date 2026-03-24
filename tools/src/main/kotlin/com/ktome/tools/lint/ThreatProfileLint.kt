package com.ktome.tools.lint

import com.ktome.game.data.schema.SchemaCatalog

object ThreatProfileLint {
    fun validate(catalog: SchemaCatalog) {
        val threatProfileIds = catalog.threatProfiles.map { threatProfile -> threatProfile.id }
        require(threatProfileIds.distinct().size == threatProfileIds.size) {
            "Threat profile ids must stay unique."
        }

        catalog.telegraphSpecs.forEach { telegraph ->
            require(telegraph.previewTurns > 0) {
                "Telegraph '${telegraph.id}' must keep previewTurns > 0."
            }
            require(telegraph.threatProfileId in threatProfileIds) {
                "Unknown threat profile '${telegraph.threatProfileId}'."
            }
            require(telegraph.counterplayTags.isNotEmpty()) {
                "Telegraph '${telegraph.id}' must expose counterplay tags."
            }
        }

        catalog.threatProfiles.forEach { threatProfile ->
            require(threatProfile.levelBand.min <= threatProfile.levelBand.max) {
                "Invalid level band for '${threatProfile.id}'."
            }
        }

        catalog.threatProfiles
            .groupBy { threatProfile -> "${threatProfile.defenderArchetype}:${threatProfile.difficultyId}" }
            .forEach { (groupId, profiles) ->
                profiles
                    .sortedBy { threatProfile -> threatProfile.levelBand.min }
                    .zipWithNext()
                    .forEach { (current, next) ->
                        require(current.levelBand.max < next.levelBand.min) {
                            "Threat profile level bands overlap inside group '$groupId': ${current.id} and ${next.id}."
                        }
                    }
            }
    }
}
