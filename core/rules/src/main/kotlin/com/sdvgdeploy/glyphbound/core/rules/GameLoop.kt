package com.sdvgdeploy.glyphbound.core.rules

import com.sdvgdeploy.glyphbound.core.model.Direction
import com.sdvgdeploy.glyphbound.core.model.GameState
import com.sdvgdeploy.glyphbound.core.model.Pos
import com.sdvgdeploy.glyphbound.core.model.Tile

fun step(state: GameState, direction: Direction): GameState {
    if (state.finished) return state
    val delta = when (direction) {
        Direction.UP -> Pos(0, -1)
        Direction.DOWN -> Pos(0, 1)
        Direction.LEFT -> Pos(-1, 0)
        Direction.RIGHT -> Pos(1, 0)
    }
    val target = Pos(state.player.x + delta.x, state.player.y + delta.y)
    if (!state.level.isWalkable(target)) {
        return state.copy(message = "Blocked", moves = state.moves + 1)
    }

    val tile = state.level.tileAt(target)
    val damage = tile.risk * state.profile.riskDamageMultiplier
    val hp = state.hp - damage
    val atExit = target == state.level.exit
    val died = hp <= 0

    return state.copy(
        player = target,
        hp = hp,
        moves = state.moves + 1,
        finished = atExit || died,
        won = atExit && !died,
        message = when {
            atExit -> "Escaped"
            died -> "You collapsed on the path"
            tile == Tile.RISK -> "Risk tile hurt: -${damage} HP"
            else -> "Move"
        }
    )
}
