package com.sdvgdeploy.glyphbound.core.rules

import com.sdvgdeploy.glyphbound.core.model.Direction
import com.sdvgdeploy.glyphbound.core.model.GameState
import com.sdvgdeploy.glyphbound.core.model.HazardType
import com.sdvgdeploy.glyphbound.core.model.HazardZone
import com.sdvgdeploy.glyphbound.core.model.Level
import com.sdvgdeploy.glyphbound.core.model.Pos
import com.sdvgdeploy.glyphbound.core.model.SpreadProfile
import com.sdvgdeploy.glyphbound.core.model.Tile
import kotlin.math.absoluteValue

data class ReduceResult(
    val state: GameState,
    val effects: List<GameEffect>,
    val events: List<String>
)

data class PipelineState(
    val state: GameState,
    val levelTiles: List<MutableList<Tile>>,
    val hazardZones: List<HazardZone>,
    val events: List<String>,
    val persistentHazardDamage: Int = 0
)

sealed interface GameEffect {
    data class IgniteOil(val positions: List<Pos>) : GameEffect
    data class ShockWater(val positions: List<Pos>) : GameEffect
}

fun step(state: GameState, direction: Direction): GameState {
    val reduced = reduce(state, direction)
    val reacted = applyReactions(reduced)
    val ticked = tickHazards(reacted)
    val resolved = resolveDamage(ticked)
    return buildCombatLog(resolved)
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
        (neighborTiles.filterValues { it == Tile.OIL }.keys + if (tile == Tile.OIL) listOf(target) else emptyList())
            .distinct()
            .sortedWith(compareBy<Pos> { it.y }.thenBy { it.x })
    } else {
        emptyList()
    }

    val ignited = boundedSpread(
        state = state,
        start = target,
        candidates = ignitionCandidates,
        profile = profileEnv.fireSpreadProfile,
        spreadKind = "fire"
    )

    val shockCandidates = if (hasSparkSource) {
        (neighborTiles.filterValues { it == Tile.WATER }.keys + if (tile == Tile.WATER) listOf(target) else emptyList())
            .distinct()
            .sortedWith(compareBy<Pos> { it.y }.thenBy { it.x })
    } else {
        emptyList()
    }

    val shocked = boundedSpread(
        state = state,
        start = target,
        candidates = shockCandidates,
        profile = profileEnv.shockSpreadProfile,
        spreadKind = "shock"
    )

    val moved = state.copy(
        player = target,
        moves = state.moves + 1,
        hp = state.hp - tileDamage
    )

    val events = buildList {
        if (tileDamage > 0) add("Hazard tile ${tile.name.lowercase()}: -$tileDamage HP")
        if (ignited.isNotEmpty()) add("Ignition: ${ignited.size} oil tile(s) caught fire")
        if (shocked.isNotEmpty()) add("Shock: ${shocked.size} water tile(s) electrified")
    }

    val effects = buildList {
        if (ignited.isNotEmpty()) add(GameEffect.IgniteOil(ignited))
        if (shocked.isNotEmpty()) add(GameEffect.ShockWater(shocked))
    }

    return ReduceResult(moved, effects, events)
}

fun applyReactions(result: ReduceResult): PipelineState {
    val state = result.state
    val env = state.profile.env
    val levelTiles = copyTiles(state.level)
    val newZones = mutableListOf<HazardZone>()

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

    return PipelineState(
        state = state,
        levelTiles = levelTiles,
        hazardZones = refreshedZones,
        events = result.events
    )
}

fun tickHazards(state: PipelineState): PipelineState {
    val zoneDamage = state.hazardZones.filter { it.pos == state.state.player }.sumOf { it.damage }

    val decayedZones = state.hazardZones.mapNotNull { zone ->
        val ttl = zone.ttl - 1
        if (ttl <= 0) {
            when (zone.type) {
                HazardType.FIRE_ZONE -> if (state.levelTiles[zone.pos.y][zone.pos.x] == Tile.FIRE) state.levelTiles[zone.pos.y][zone.pos.x] = Tile.ASH
                HazardType.SHOCK_ZONE -> if (state.levelTiles[zone.pos.y][zone.pos.x] == Tile.SHOCKED_WATER) state.levelTiles[zone.pos.y][zone.pos.x] = Tile.WATER
            }
            null
        } else {
            zone.copy(ttl = ttl)
        }
    }

    return state.copy(
        hazardZones = decayedZones,
        persistentHazardDamage = zoneDamage
    )
}

fun resolveDamage(state: PipelineState): PipelineState {
    val level = state.state.level.copy(tiles = state.levelTiles)
    val hpAfter = state.state.hp - state.persistentHazardDamage
    val atExit = state.state.player == level.exit
    val died = hpAfter <= 0

    return state.copy(
        state = state.state.copy(
            level = level,
            hp = hpAfter,
            finished = atExit || died,
            won = atExit && !died,
            hazardZones = state.hazardZones
        )
    )
}

fun buildCombatLog(state: PipelineState): GameState {
    val died = state.state.hp <= 0
    val atExit = state.state.player == state.state.level.exit

    val messageParts = buildList {
        addAll(state.events)
        if (state.persistentHazardDamage > 0) add("Persistent hazard: -${state.persistentHazardDamage} HP")
        if (died) add("You collapsed on the path")
        if (atExit && !died) add("Escaped")
    }

    val message = messageParts.joinToString(" ").ifBlank { "Move" }

    return state.state.copy(
        message = message,
        messageLog = (state.state.messageLog + message).takeLast(8)
    )
}

private fun boundedSpread(
    state: GameState,
    start: Pos,
    candidates: List<Pos>,
    profile: SpreadProfile,
    spreadKind: String
): List<Pos> {
    if (candidates.isEmpty() || profile.maxTargets <= 0 || profile.maxChainDepth <= 0) return emptyList()

    val candidateSet = candidates.toSet()
    val queue = ArrayDeque<Pair<Pos, Int>>()
    val visited = linkedSetOf<Pos>()
    val accepted = mutableListOf<Pos>()

    neighbors4(start)
        .filter(candidateSet::contains)
        .sortedWith(compareBy<Pos> { it.y }.thenBy { it.x })
        .forEach { queue.add(it to 1) }

    while (queue.isNotEmpty() && accepted.size < profile.maxTargets) {
        val (pos, depth) = queue.removeFirst()
        if (!visited.add(pos)) continue

        val roll = chainRoll(state, pos, spreadKind, depth)
        if (roll <= profile.spreadChance) {
            accepted += pos
            if (depth < profile.maxChainDepth) {
                neighbors4(pos)
                    .filter(candidateSet::contains)
                    .sortedWith(compareBy<Pos> { it.y }.thenBy { it.x })
                    .forEach { if (it !in visited) queue.add(it to depth + 1) }
            }
        }
    }

    return accepted.sortedWith(compareBy<Pos> { it.y }.thenBy { it.x })
}

private fun neighbors4(pos: Pos): List<Pos> = listOf(
    Pos(pos.x + 1, pos.y),
    Pos(pos.x - 1, pos.y),
    Pos(pos.x, pos.y + 1),
    Pos(pos.x, pos.y - 1)
)

private fun copyTiles(level: Level): List<MutableList<Tile>> =
    level.tiles.map { it.toMutableList() }

private fun chainRoll(state: GameState, pos: Pos, spreadKind: String, depth: Int): Double {
    val kindSalt = if (spreadKind == "fire") 0xF1E3 else 0x5A0C
    val raw = (state.level.seed xor (state.moves.toLong() shl 8) xor (pos.x.toLong() shl 16) xor (pos.y.toLong() shl 24) xor (depth.toLong() shl 32) xor kindSalt.toLong()).absoluteValue
    return (raw % 1000).toDouble() / 1000.0
}
