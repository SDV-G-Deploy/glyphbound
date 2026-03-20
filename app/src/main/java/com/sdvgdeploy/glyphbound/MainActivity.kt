package com.sdvgdeploy.glyphbound

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.sdvgdeploy.glyphbound.core.model.Direction
import com.sdvgdeploy.glyphbound.core.model.GameState
import com.sdvgdeploy.glyphbound.core.model.Pos
import com.sdvgdeploy.glyphbound.core.procgen.LevelGenerator
import com.sdvgdeploy.glyphbound.core.rules.step

class MainActivity : AppCompatActivity() {
    private lateinit var state: GameState
    private lateinit var mapText: TextView
    private lateinit var metaText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapText = findViewById(R.id.mapText)
        metaText = findViewById(R.id.metaText)

        val seed = intent?.getLongExtra("seed", 1337L) ?: 1337L
        val level = LevelGenerator.generate(seed)
        state = GameState(level = level, player = level.entry)

        findViewById<Button>(R.id.upButton).setOnClickListener { move(Direction.UP) }
        findViewById<Button>(R.id.downButton).setOnClickListener { move(Direction.DOWN) }
        findViewById<Button>(R.id.leftButton).setOnClickListener { move(Direction.LEFT) }
        findViewById<Button>(R.id.rightButton).setOnClickListener { move(Direction.RIGHT) }

        render()
    }

    private fun move(direction: Direction) {
        state = step(state, direction)
        render()
    }

    private fun render() {
        val rows = state.level.tiles.mapIndexed { y, row ->
            row.mapIndexed { x, tile ->
                if (state.player == Pos(x, y)) '@' else tile.glyph
            }.joinToString(separator = "")
        }
        mapText.text = rows.joinToString("\n")
        metaText.text = "seed=${state.level.seed}  hp=${state.hp}  moves=${state.moves}  ${state.message}"
    }
}
