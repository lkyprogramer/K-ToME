package com.ktome.core.talent

class TalentRegistry {
    private val talents = linkedMapOf<String, TalentDef>()

    fun register(talent: TalentDef) {
        talents[talent.id] = talent
    }

    fun registerAll(definitions: Iterable<TalentDef>) {
        definitions.forEach(::register)
    }

    fun get(id: String): TalentDef? = talents[id]

    fun getAll(): Map<String, TalentDef> = talents.toMap()
}

