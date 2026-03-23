package com.ktome.core.combat

import com.ktome.core.ecs.Stats
import com.ktome.core.random.RandomSource
import kotlinx.serialization.Serializable

@Serializable
enum class PowerType {
    PHYSICAL,
    SPELL,
    MENTAL,
}

@Serializable
enum class SaveDimension {
    PHYSICAL,
    MENTAL,
    SPELL,
}

@Serializable
data class PowerSaveStats(
    val physicalPower: Int = 10,
    val physicalSave: Int = 10,
    val mentalPower: Int = 10,
    val mentalSave: Int = 10,
    val spellPower: Int = 10,
    val spellSave: Int = 10,
)

data class PowerSaveBonus(
    val physicalPower: Int = 0,
    val physicalSave: Int = 0,
    val mentalPower: Int = 0,
    val mentalSave: Int = 0,
    val spellPower: Int = 0,
    val spellSave: Int = 0,
)

data class PowerSaveResolution(
    val power: Int,
    val save: Int,
    val applyChance: Double,
    val roll: Double,
    val applied: Boolean,
)

object PowerSaveFormula {
    const val SAVE_SIGMOID_K: Double = 0.05
    const val MIN_APPLY_CHANCE: Double = 0.10
    const val MAX_APPLY_CHANCE: Double = 0.90

    fun applyChance(
        power: Int,
        save: Int,
    ): Double {
        val delta = (power - save).toDouble()
        val sigmoid = 1.0 / (1.0 + kotlin.math.exp(-SAVE_SIGMOID_K * delta))
        return (MIN_APPLY_CHANCE + (MAX_APPLY_CHANCE - MIN_APPLY_CHANCE) * sigmoid)
            .coerceIn(MIN_APPLY_CHANCE, MAX_APPLY_CHANCE)
    }

    fun resolve(
        power: Int,
        save: Int,
        random: RandomSource,
    ): PowerSaveResolution {
        val chance = applyChance(power, save)
        val roll = random.nextDouble()
        return PowerSaveResolution(
            power = power,
            save = save,
            applyChance = chance,
            roll = roll,
            applied = roll < chance,
        )
    }

    fun calculate(
        stats: Stats,
        level: Int,
        bonus: PowerSaveBonus = PowerSaveBonus(),
    ): PowerSaveStats =
        PowerSaveStats(
            physicalPower = calculatePhysicalPower(stats.str, level, bonus.physicalPower),
            physicalSave = calculatePhysicalSave(stats.con, level, bonus.physicalSave),
            mentalPower = calculateMentalPower(stats.wil, level, bonus.mentalPower),
            mentalSave = calculateMentalSave(stats.wil, stats.con, level, bonus.mentalSave),
            spellPower = calculateSpellPower(stats.wil, stats.dex, level, bonus.spellPower),
            spellSave = calculateSpellSave(stats.wil, stats.con, level, bonus.spellSave),
        )

    fun calculatePhysicalPower(
        str: Int,
        level: Int,
        bonus: Int = 0,
    ): Int = 10 + str * 3 / 2 + level / 2 + bonus

    fun calculatePhysicalSave(
        con: Int,
        level: Int,
        bonus: Int = 0,
    ): Int = 10 + con * 3 / 2 + level / 2 + bonus

    fun calculateMentalPower(
        wil: Int,
        level: Int,
        bonus: Int = 0,
    ): Int = 10 + wil * 3 / 2 + level / 2 + bonus

    fun calculateMentalSave(
        wil: Int,
        con: Int,
        level: Int,
        bonus: Int = 0,
    ): Int = 10 + wil + con / 2 + level / 2 + bonus

    fun calculateSpellPower(
        wil: Int,
        dex: Int,
        level: Int,
        bonus: Int = 0,
    ): Int = 10 + wil + dex / 2 + level / 2 + bonus

    fun calculateSpellSave(
        wil: Int,
        con: Int,
        level: Int,
        bonus: Int = 0,
    ): Int = 10 + wil / 2 + con + level / 2 + bonus

    fun powerFor(
        stats: PowerSaveStats,
        dimension: SaveDimension,
    ): Int =
        when (dimension) {
            SaveDimension.PHYSICAL -> stats.physicalPower
            SaveDimension.MENTAL -> stats.mentalPower
            SaveDimension.SPELL -> stats.spellPower
        }

    fun saveFor(
        stats: PowerSaveStats,
        dimension: SaveDimension,
    ): Int =
        when (dimension) {
            SaveDimension.PHYSICAL -> stats.physicalSave
            SaveDimension.MENTAL -> stats.mentalSave
            SaveDimension.SPELL -> stats.spellSave
        }
}
