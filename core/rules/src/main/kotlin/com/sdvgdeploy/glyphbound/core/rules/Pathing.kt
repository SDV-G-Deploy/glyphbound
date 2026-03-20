package com.sdvgdeploy.glyphbound.core.rules

import com.sdvgdeploy.glyphbound.core.model.Level
import com.sdvgdeploy.glyphbound.core.model.Pos
import java.util.ArrayDeque

private val deltas = listOf(Pos(1, 0), Pos(-1, 0), Pos(0, 1), Pos(0, -1))

fun neighbors(level: Level, pos: Pos): List<Pos> = deltas.map { Pos(pos.x + it.x, pos.y + it.y) }
    .filter { level.isWalkable(it) }

fun isConnected(level: Level): Boolean {
    val visited = mutableSetOf<Pos>()
    val q = ArrayDeque<Pos>()
    q.add(level.entry)
    visited.add(level.entry)
    while (q.isNotEmpty()) {
        val p = q.removeFirst()
        if (p == level.exit) return true
        for (n in neighbors(level, p)) {
            if (visited.add(n)) q.add(n)
        }
    }
    return false
}

fun shortestPathCount(level: Level): Int {
    val dist = mutableMapOf<Pos, Int>()
    val ways = mutableMapOf<Pos, Int>()
    val q = ArrayDeque<Pos>()

    dist[level.entry] = 0
    ways[level.entry] = 1
    q.add(level.entry)

    while (q.isNotEmpty()) {
        val p = q.removeFirst()
        val pd = dist.getValue(p)
        for (n in neighbors(level, p)) {
            val nd = dist[n]
            when {
                nd == null -> {
                    dist[n] = pd + 1
                    ways[n] = ways.getValue(p)
                    q.add(n)
                }
                nd == pd + 1 -> {
                    ways[n] = (ways[n] ?: 0) + ways.getValue(p)
                }
            }
        }
    }

    return ways[level.exit] ?: 0
}
