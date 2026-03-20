package com.sdvgdeploy.glyphbound.core.procgen

import com.sdvgdeploy.glyphbound.core.model.Level
import com.sdvgdeploy.glyphbound.core.model.Pos
import java.util.ArrayDeque

enum class DisjointMode { EDGE, NODE }

data class PathValidationConfig(
    val minDisjointPaths: Int = 2,
    val mode: DisjointMode = DisjointMode.EDGE
)

data class PathValidationResult(
    val connected: Boolean,
    val disjointPathCount: Int,
    val requiredDisjointPaths: Int,
    val mode: DisjointMode
) {
    val isValid: Boolean get() = connected && disjointPathCount >= requiredDisjointPaths
}

object PathValidator {
    private val deltas = listOf(Pos(1, 0), Pos(-1, 0), Pos(0, 1), Pos(0, -1))

    fun validate(level: Level, config: PathValidationConfig = PathValidationConfig()): PathValidationResult {
        val connected = isConnected(level)
        if (!connected) return PathValidationResult(false, 0, config.minDisjointPaths, config.mode)

        val count = when (config.mode) {
            DisjointMode.EDGE -> maxEdgeDisjointPaths(level)
            DisjointMode.NODE -> maxNodeDisjointPaths(level)
        }

        return PathValidationResult(connected, count, config.minDisjointPaths, config.mode)
    }

    private fun walkableNodes(level: Level): List<Pos> {
        val nodes = mutableListOf<Pos>()
        for (y in 0 until level.height) {
            for (x in 0 until level.width) {
                val p = Pos(x, y)
                if (level.isWalkable(p)) nodes += p
            }
        }
        return nodes
    }

    private fun neighbors(level: Level, pos: Pos): List<Pos> =
        deltas.map { Pos(pos.x + it.x, pos.y + it.y) }.filter { level.isWalkable(it) }

    private fun isConnected(level: Level): Boolean {
        val q = ArrayDeque<Pos>()
        val visited = mutableSetOf<Pos>()
        q += level.entry
        visited += level.entry

        while (q.isNotEmpty()) {
            val current = q.removeFirst()
            if (current == level.exit) return true
            for (n in neighbors(level, current)) {
                if (visited.add(n)) q += n
            }
        }

        return false
    }

    private fun maxEdgeDisjointPaths(level: Level): Int {
        val nodes = walkableNodes(level)
        val index = nodes.withIndex().associate { it.value to it.index }
        val n = nodes.size
        val cap = Array(n) { IntArray(n) }

        for (p in nodes) {
            val i = index.getValue(p)
            for (nPos in neighbors(level, p)) {
                val j = index.getValue(nPos)
                cap[i][j] = 1
            }
        }

        return maxFlow(cap, index.getValue(level.entry), index.getValue(level.exit))
    }

    private fun maxNodeDisjointPaths(level: Level): Int {
        val nodes = walkableNodes(level)
        val index = nodes.withIndex().associate { it.value to it.index }
        val n = nodes.size
        val size = n * 2
        val cap = Array(size) { IntArray(size) }

        fun inId(i: Int) = i * 2
        fun outId(i: Int) = i * 2 + 1

        val startIdx = index.getValue(level.entry)
        val endIdx = index.getValue(level.exit)

        for (p in nodes) {
            val i = index.getValue(p)
            val internalCapacity = if (i == startIdx || i == endIdx) 2 else 1
            cap[inId(i)][outId(i)] = internalCapacity
        }

        for (p in nodes) {
            val i = index.getValue(p)
            for (nPos in neighbors(level, p)) {
                val j = index.getValue(nPos)
                cap[outId(i)][inId(j)] = 1
            }
        }

        return maxFlow(cap, outId(startIdx), inId(endIdx))
    }

    private fun maxFlow(capacity: Array<IntArray>, source: Int, sink: Int): Int {
        val residual = capacity.map { it.clone() }.toTypedArray()
        val parent = IntArray(residual.size)
        var flow = 0

        while (bfs(residual, source, sink, parent)) {
            var bottleneck = Int.MAX_VALUE
            var v = sink
            while (v != source) {
                val u = parent[v]
                bottleneck = minOf(bottleneck, residual[u][v])
                v = u
            }

            v = sink
            while (v != source) {
                val u = parent[v]
                residual[u][v] -= bottleneck
                residual[v][u] += bottleneck
                v = u
            }

            flow += bottleneck
            if (flow >= 2) return flow
        }

        return flow
    }

    private fun bfs(residual: Array<IntArray>, source: Int, sink: Int, parent: IntArray): Boolean {
        parent.fill(-1)
        parent[source] = source
        val q = ArrayDeque<Int>()
        q += source

        while (q.isNotEmpty()) {
            val u = q.removeFirst()
            for (v in residual.indices) {
                if (parent[v] == -1 && residual[u][v] > 0) {
                    parent[v] = u
                    if (v == sink) return true
                    q += v
                }
            }
        }

        return false
    }
}
