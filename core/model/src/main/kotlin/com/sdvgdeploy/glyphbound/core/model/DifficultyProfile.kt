package com.sdvgdeploy.glyphbound.core.model

enum class DifficultyProfile(
    val wallChance: Double,
    val riskChance: Double,
    val minDisjointPaths: Int,
    val useNodeDisjoint: Boolean,
    val startingHp: Int,
    val riskDamageMultiplier: Int
) {
    EASY(
        wallChance = 0.24,
        riskChance = 0.05,
        minDisjointPaths = 2,
        useNodeDisjoint = false,
        startingHp = 14,
        riskDamageMultiplier = 1
    ),
    NORMAL(
        wallChance = 0.30,
        riskChance = 0.08,
        minDisjointPaths = 2,
        useNodeDisjoint = false,
        startingHp = 10,
        riskDamageMultiplier = 1
    ),
    HARD(
        wallChance = 0.36,
        riskChance = 0.12,
        minDisjointPaths = 2,
        useNodeDisjoint = true,
        startingHp = 8,
        riskDamageMultiplier = 2
    );

    companion object {
        fun fromRaw(value: String?): DifficultyProfile =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NORMAL
    }
}
