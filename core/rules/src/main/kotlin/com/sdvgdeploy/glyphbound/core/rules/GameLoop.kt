package com.sdvgdeploy.glyphbound.core.rules

import com.sdvgdeploy.glyphbound.core.model.Direction
import com.sdvgdeploy.glyphbound.core.model.GameState
import com.sdvgdeploy.glyphbound.core.model.HazardType
import com.sdvgdeploy.glyphbound.core.model.HazardZone
import com.sdvgdeploy.glyphbound.core.model.Level
import com.sdvgdeploy.glyphbound.core.model.Pos
import com.sdvgdeploy.glyphbound.core.model.Tile
import kotlin.math.absoluteValue

data class ReduceResult(
    val state: GameState,
    val effects: List<GameEffect>,
    val events: List<String>
)

sealed interface GameEffect {
    data class IgniteOil(val positions: List<Pos>) : GameEffect
    data class ShockWater(val positions: List<Pos>) : GameEffect
}

fun step(state: GameState, direction: Direction): GameState {
    val reduced = reduce(state, direction)
    return processEffects(reduced)
}

fun reduce(state: GameState, direction: Direction): ReduceResult {
    if (state.finished) return ReduceResult(state, emptyList(), emptyList())

    val delta = when (direction) {
        Direction.UP -> Pos(0, -1)
        Direction.DOWN -> Pos(0, 1)
        Direction.LEFT -> Pos(-1, 0)
        Direction.RIGHT -> Pos(1, 0)
    }

    val target = Pos(state.player.x + delta.x, state.player.y + delta.y)
    if (!state.level.isWalkable(target)) {
        val blockedState = state.copy(
            moves = state.moves + 1,
            message = "Blocked"
        )
        return ReduceResult(blockedState, emptyList(), listOf("Blocked"))
    }

    val tile = state.level.tileAt(target)
    val profileEnv = state.profile.env
    val tileDamage = tile.risk * profileEnv.hazardDamageMultiplier

    val neighborPositions = neighbors4(target).filter(state.level::inBounds)
    val neighborTiles = neighborPositions.associateWith { state.level.tileAt(it) }

    val hasSparkSource = tile == Tile.SPARK || tile == Tile.FIRE || neighborTiles.values.any { it == Tile.SPARK || it == Tile.FIRE }

    val ignitionCandidates = if (hasSparkSource) {
        neighborTiles.filterValues { it == Tile.OIL }.keys + if (tile == Tile.OIL) listOf(target) else emptyList()
    } else {
        emptyList()
    }

    val ignited = ignitionCandidates
        .distinct()
        .sortedWith(compareBy<Pos> { it.y }.thenBy { it.x })
        .filter { chainRoll(state, it) <= profileEnv.chainReactionChance }
        .take(profileEnv.chainReactionMaxTargets)

    val shockCandidates = if (hasSparkSource) {
        neighborTiles.filterValues { it == Tile.WATER }.keys + if (tile == Tile.WATER) listOf(target) else emptyList()
    } else {
        emptyList()
    }

    val shocked = shockCandidates.distinct().sortedWith(compareBy<Pos> { it.y }.thenBy { it.x }).take(1)

    val moved = state.copy(
        player = target,
        moves = state.moves + 1,
        hp = state.hp - tileDamage
    )

    val events = buildList {
        if (tileDamage > 0) add("Hazard tile ${tile.name.lowercase()}: -$tileDamage HP")
        if (ignited.isNotEmpty()) add("Ignition: ${ignited.size} oil tile(s) caught fire")
        if (shocked.isNotEmpty()) add("Shock: water electrified")
    }

    val effects = buildList {
        if (ignited.isNotEmpty()) add(GameEffect.IgniteOil(ignited))
        if (shocked.isNotEmpty()) add(GameEffect.ShockWater(shocked))
    }

    return ReduceResult(moved, effects, events)
}

fun processEffects(result: ReduceResult): GameState {
    var state = result.state
    val env = state.profile.env
    val newZones = mutableListOf<HazardZone>()
    val levelTiles = copyTiles(state.level)

    result.effects.forEach { effect ->
        when (effect) {
            is GameEffect.IgniteOil -> effect.positions.forEach { pos ->
                if (state.level.inBounds(pos)) {
                    levelTiles[pos.y][pos.x] = Tile.FIRE
                    newZones += HazardZone(pos, HazardType.FIRE_ZONE, env.fireZoneTtl, env.ignitionTickDamage * env.hazardDamageMultiplier, "oil ignition")
                }
            }

            is GameEffect.ShockWater -> effect.positions.forEach { pos ->
                if (state.level.inBounds(pos)) {
                    levelTiles[pos.y][pos.x] = Tile.SHOCKED_WATER
                    newZones += HazardZone(pos, HazardType.SHOCK_ZONE, env.shockZoneTtl, env.shockTickDamage * env.hazardDamageMultiplier, "water spark")
                }
            }
        }
    }

    val refreshedZones = (state.hazardZones + newZones)
        .groupBy { it.pos to it.type }
        .map { (_, zones) -> zones.maxBy { it.ttl } }

    val zoneDamage = refreshedZones.filter { it.pos == state.player }.sumOf { it.damage }
    val decayedZones = refreshedZones.mapNotNull { zone ->
        val ttl = zone.ttl - 1
        if (ttl <= 0) {
            when (zone.type) {
                HazardType.FIRE_ZONE -> if (levelTiles[zone.pos.y][zone.pos.x] == Tile.FIRE) levelTiles[zone.pos.y][zone.pos.x] = Tile.ASH
                HazardType.SHOCK_ZONE -> if (levelTiles[zone.pos.y][zone.pos.x] == Tile.SHOCKED_WATER) levelTiles[zone.pos.y][zone.pos.x] = Tile.WATER
            }
            null
        } else {
            zone.copy(ttl = ttl)
        }
    }

    val level = state.level.copy(tiles = levelTiles)
    val hpAfter = state.hp - zoneDamage
    val atExit = state.player == level.exit
    val died = hpAfter <= 0

    val messageParts = buildList {
        addAll(result.events)
        if (zoneDamage > 0) add("Persistent hazard: -$zoneDamage HP")
        if (died) add("You collapsed on the path")
        if (atExit && !died) add("Escaped")
    }

    val message = messageParts.joinToString(" ").ifBlank { "Move" }

    state = state.copy(
        level = level,
        hp = hpAfter,
        finished = atExit || died,
        won = atExit && !died,
        message = message,
        hazardZones = decayedZones,
        messageLog = (state.messageLog + message).takeLast(8)
    )

    return state
}

private fun neighbors4(pos: Pos): List<Pos> = listOf(
    Pos(pos.x + 1, pos.y),
    Pos(pos.x - 1, pos.y),
    Pos(pos.x, pos.y + 1),
    Pos(pos.x, pos.y - 1)
)

private fun copyTiles(level: Level): List<MutableList<Tile>> =
    level.tiles.map { it.toMutableList() }

private fun chainRoll(state: GameState, pos: Pos): Double {
    val raw = (state.level.seed xor (state.moves.toLong() shl 8) xor (pos.x.toLong() shl 16) xor (pos.y.toLong() shl 24)).absoluteValue
    return (raw % 1000).toDouble() / 1000.0
}
