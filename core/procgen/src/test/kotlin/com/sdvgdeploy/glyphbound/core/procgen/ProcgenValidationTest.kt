package com.sdvgdeploy.glyphbound.core.procgen

import com.sdvgdeploy.glyphbound.core.model.DifficultyProfile
import com.sdvgdeploy.glyphbound.core.model.Level
import com.sdvgdeploy.glyphbound.core.model.Pos
import com.sdvgdeploy.glyphbound.core.model.Tile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProcgenValidationTest {
    @Test
    fun reproducibility_sameSeed_sameMap() {
        val seed = 424242L
        val a = LevelGenerator.generate(seed)
        val b = LevelGenerator.generate(seed)

        assertEquals(a.width, b.width)
        assertEquals(a.height, b.height)
        assertEquals(a.entry, b.entry)
        assertEquals(a.exit, b.exit)
        assertEquals(
            a.tiles.map { row -> row.map { it.glyph } },
            b.tiles.map { row -> row.map { it.glyph } }
        )
    }

    @Test
    fun connectivity_entryAlwaysReachesExit() {
        val level = LevelGenerator.generate(123456L)
        val result = PathValidator.validate(level)
        assertTrue(result.connected)
    }

    @Test
    fun edgeMode_policyPassAndFail() {
        val fail = fromAscii(
            "#####",
            "#S..#",
            "###.#",
            "#..E#",
            "#####"
        )
        val pass = fromAscii(
            "#####",
            "#S..#",
            "#.#.#",
            "#..E#",
            "#####"
        )

        val edge = PathValidationConfig(minDisjointPaths = 2, mode = DisjointMode.EDGE)
        assertFalse(PathValidator.validate(fail, edge).isValid)
        assertTrue(PathValidator.validate(pass, edge).isValid)
    }

    @Test
    fun nodeMode_isStricterThanEdge_onControlledMap() {
        val edgeConfig = PathValidationConfig(minDisjointPaths = 2, mode = DisjointMode.EDGE)
        val nodeConfig = PathValidationConfig(minDisjointPaths = 2, mode = DisjointMode.NODE)

        val candidate = (1L..5_000L)
            .map { LevelGenerator.generate(it, LevelGenerator.Config(width = 9, height = 7, wallChance = 0.28, riskChance = 0.0, validator = edgeConfig)) }
            .firstOrNull { level ->
                val edge = PathValidator.validate(level, edgeConfig)
                val node = PathValidator.validate(level, nodeConfig)
                edge.isValid && !node.isValid
            }

        assertTrue(candidate != null, "Expected at least one map where EDGE passes but NODE fails")

        val edge = PathValidator.validate(candidate!!, edgeConfig)
        val node = PathValidator.validate(candidate, nodeConfig)
        assertTrue(edge.disjointPathCount >= node.disjointPathCount)
        assertTrue(edge.isValid)
        assertFalse(node.isValid)
    }

    @Test
    fun deterministicRetry_withSameSeedAndProfile_staysReproducible() {
        val seed = 8888L
        val profile = DifficultyProfile.HARD

        val a = LevelGenerator.generate(seed, profile)
        val b = LevelGenerator.generate(seed, profile)

        assertEquals(
            a.tiles.map { row -> row.map { it.glyph } },
            b.tiles.map { row -> row.map { it.glyph } }
        )
        assertEquals(a.seed, b.seed)

        val validation = PathValidator.validate(
            a,
            LevelGenerator.configFor(profile).validator
        )
        assertTrue(validation.isValid)
        assertEquals(DisjointMode.NODE, validation.mode)
    }

    private fun fromAscii(vararg rows: String): Level {
        val height = rows.size
        val width = rows.first().length
        val tiles = MutableList(height) { y ->
            MutableList(width) { x ->
                when (rows[y][x]) {
                    '#' -> Tile.WALL
                    '.' -> Tile.FLOOR
                    'S' -> Tile.ENTRY
                    'E' -> Tile.EXIT
                    '~' -> Tile.RISK
                    else -> Tile.FLOOR
                }
            }
        }

        var entry = Pos(0, 0)
        var exit = Pos(0, 0)
        for (y in rows.indices) {
            for (x in rows[y].indices) {
                if (rows[y][x] == 'S') entry = Pos(x, y)
                if (rows[y][x] == 'E') exit = Pos(x, y)
            }
        }

        return Level(width, height, 0L, tiles, entry, exit)
    }
}
