package com.sdvgdeploy.glyphbound.core.procgen

import com.sdvgdeploy.glyphbound.core.model.Level
import com.sdvgdeploy.glyphbound.core.model.Pos
import com.sdvgdeploy.glyphbound.core.model.Tile
import com.sdvgdeploy.glyphbound.core.rules.isConnected
import com.sdvgdeploy.glyphbound.core.rules.shortestPathCount
import kotlin.random.Random

object LevelGenerator {
    data class Config(
        val width: Int = 24,
        val height: Int = 16,
        val wallChance: Double = 0.30,
        val riskChance: Double = 0.08,
        val maxAttempts: Int = 200
    )

    fun generate(seed: Long, config: Config = Config()): Level {
        repeat(config.maxAttempts) { attempt ->
            val level = build(seed + attempt, config)
            if (isConnected(level) && shortestPathCount(level) >= 2) return level
        }
        return buildCorridorFallback(seed, config)
    }

    private fun build(seed: Long, config: Config): Level {
        val rng = Random(seed)
        val tiles = MutableList(config.height) { MutableList(config.width) { Tile.FLOOR } }

        for (y in 0 until config.height) {
            for (x in 0 until config.width) {
                val border = x == 0 || y == 0 || x == config.width - 1 || y == config.height - 1
                tiles[y][x] = when {
                    border -> Tile.WALL
                    rng.nextDouble() < config.wallChance -> Tile.WALL
                    rng.nextDouble() < config.riskChance -> Tile.RISK
                    else -> Tile.FLOOR
                }
            }
        }

        val entry = Pos(1, 1)
        val exit = Pos(config.width - 2, config.height - 2)
        tiles[entry.y][entry.x] = Tile.ENTRY
        tiles[exit.y][exit.x] = Tile.EXIT

        return Level(config.width, config.height, seed, tiles, entry, exit)
    }

    private fun buildCorridorFallback(seed: Long, config: Config): Level {
        val tiles = MutableList(config.height) { MutableList(config.width) { Tile.WALL } }
        val entry = Pos(1, 1)
        val exit = Pos(config.width - 2, config.height - 2)

        for (x in 1 until config.width - 1) {
            tiles[1][x] = Tile.FLOOR
            tiles[config.height - 2][x] = Tile.FLOOR
        }
        for (y in 1 until config.height - 1) {
            tiles[y][1] = Tile.FLOOR
            tiles[y][config.width - 2] = Tile.FLOOR
        }
        for (x in 2 until config.width - 2 step 4) {
            for (y in 2 until config.height - 2) {
                if ((x + y) % 7 == 0) tiles[y][x] = Tile.RISK
            }
        }

        tiles[entry.y][entry.x] = Tile.ENTRY
        tiles[exit.y][exit.x] = Tile.EXIT
        return Level(config.width, config.height, seed, tiles, entry, exit)
    }
}
