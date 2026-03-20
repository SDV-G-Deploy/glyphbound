package com.sdvgdeploy.glyphbound.core.rules

import com.sdvgdeploy.glyphbound.core.model.Direction
import com.sdvgdeploy.glyphbound.core.model.DifficultyProfile
import com.sdvgdeploy.glyphbound.core.model.GameState
import com.sdvgdeploy.glyphbound.core.model.Level
import com.sdvgdeploy.glyphbound.core.model.Pos
import com.sdvgdeploy.glyphbound.core.model.Tile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRulesTest {

    @Test
    fun reducerMove_transitionIsPure() {
        val initial = stateFrom(
            listOf(
                "#####",
                "#SoE#",
                "#####"
            ),
            profile = DifficultyProfile.NORMAL
        )

        val reduced = reduce(initial, Direction.RIGHT)

        assertEquals(Pos(2, 1), reduced.state.player)
        assertTrue(reduced.events.isEmpty())
    }

    @Test
    fun hazardTtlDecay_fireToAsh() {
        val initial = stateFrom(
            listOf(
                "#####",
                "#Sof#",
                "#####"
            ),
            profile = DifficultyProfile.EASY,
            player = Pos(1, 1)
        )

        val reduced = reduce(initial, Direction.RIGHT)
        val afterFirst = processEffects(reduced)
        val afterSecond = step(afterFirst, Direction.LEFT)

        assertTrue(afterFirst.hazardZones.isNotEmpty())
        assertEquals(Tile.ASH, afterSecond.level.tileAt(Pos(2, 1)))
    }

    @Test
    fun tileTransitions_waterToShockedBackToWater() {
        val initial = stateFrom(
            listOf(
                "#####",
                "#S*w#",
                "#####"
            ),
            profile = DifficultyProfile.EASY
        )

        val afterSpark = step(initial, Direction.RIGHT)
        assertEquals(Tile.SHOCKED_WATER, afterSpark.level.tileAt(Pos(3, 1)))

        val cooled = step(afterSpark, Direction.LEFT)
        assertEquals(Tile.WATER, cooled.level.tileAt(Pos(3, 1)))
    }

    @Test
    fun chainReaction_isBoundedByProfile() {
        val initial = stateFrom(
            listOf(
                "#######",
                "#Sooo*#",
                "#######"
            ),
            profile = DifficultyProfile.EASY
        )

        val reduced = reduce(initial, Direction.RIGHT)
        val ignition = reduced.effects.filterIsInstance<GameEffect.IgniteOil>().flatMap { it.positions }

        assertTrue(ignition.size <= DifficultyProfile.EASY.env.chainReactionMaxTargets)
    }

    @Test
    fun deterministicSequence_sameSeedAndIntentsSameResult() {
        val intents = listOf(Direction.RIGHT, Direction.RIGHT, Direction.LEFT, Direction.RIGHT)
        val s1 = intents.fold(deterministicState()) { s, d -> step(s, d) }
        val s2 = intents.fold(deterministicState()) { s, d -> step(s, d) }

        assertEquals(s1.hp, s2.hp)
        assertEquals(s1.player, s2.player)
        assertEquals(s1.messageLog, s2.messageLog)
        assertEquals(s1.hazardZones, s2.hazardZones)
    }

    private fun deterministicState(): GameState = stateFrom(
        listOf(
            "########",
            "#So*wE##",
            "########"
        ),
        profile = DifficultyProfile.NORMAL,
        seed = 42L
    )

    private fun stateFrom(
        rows: List<String>,
        profile: DifficultyProfile,
        seed: Long = 1L,
        player: Pos? = null
    ): GameState {
        val tiles = rows.map { row ->
            row.map { ch ->
                when (ch) {
                    '#' -> Tile.WALL
                    'S' -> Tile.ENTRY
                    'E' -> Tile.EXIT
                    '.' -> Tile.FLOOR
                    '~' -> Tile.RISK
                    'o' -> Tile.OIL
                    'w' -> Tile.WATER
                    '*' -> Tile.SPARK
                    'f' -> Tile.FIRE
                    'a' -> Tile.ASH
                    'z' -> Tile.SHOCKED_WATER
                    else -> Tile.FLOOR
                }
            }.toMutableList()
        }.toMutableList()

        var entry = Pos(1, 1)
        var exit = Pos(rows[0].length - 2, rows.size - 2)
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                if (ch == 'S') entry = Pos(x, y)
                if (ch == 'E') exit = Pos(x, y)
            }
        }

        val level = Level(rows[0].length, rows.size, seed, tiles, entry, exit)
        return GameState(level = level, player = player ?: entry, profile = profile, hp = profile.startingHp)
    }
}
