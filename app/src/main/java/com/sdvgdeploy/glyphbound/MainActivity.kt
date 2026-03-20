package com.sdvgdeploy.glyphbound

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.MotionEvent
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.sdvgdeploy.glyphbound.core.model.Direction
import com.sdvgdeploy.glyphbound.core.model.GameState
import com.sdvgdeploy.glyphbound.core.model.Pos
import com.sdvgdeploy.glyphbound.core.procgen.LevelGenerator
import com.sdvgdeploy.glyphbound.core.rules.step
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private lateinit var state: GameState
    private lateinit var mapText: TextView
    private lateinit var hudText: TextView
    private lateinit var messageText: TextView
    private var highContrast = false

    private var touchStartX = 0f
    private var touchStartY = 0f
    private val swipeThresholdPx = 48f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapText = findViewById(R.id.mapText)
        hudText = findViewById(R.id.hudText)
        messageText = findViewById(R.id.messageText)

        val seed = intent?.getLongExtra("seed", 1337L) ?: 1337L
        val level = LevelGenerator.generate(seed)
        state = GameState(level = level, player = level.entry)

        findViewById<Button>(R.id.upButton).setOnClickListener { move(Direction.UP) }
        findViewById<Button>(R.id.downButton).setOnClickListener { move(Direction.DOWN) }
        findViewById<Button>(R.id.leftButton).setOnClickListener { move(Direction.LEFT) }
        findViewById<Button>(R.id.rightButton).setOnClickListener { move(Direction.RIGHT) }

        findViewById<Switch>(R.id.highContrastSwitch).setOnCheckedChangeListener { _, checked ->
            highContrast = checked
            render()
        }

        mapText.setOnTouchListener { _, event -> handleSwipe(event) }

        render()
    }

    private fun handleSwipe(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - touchStartX
                val dy = event.y - touchStartY
                val absDx = abs(dx)
                val absDy = abs(dy)
                if (absDx < swipeThresholdPx && absDy < swipeThresholdPx) return true

                val direction = if (absDx > absDy) {
                    if (dx > 0) Direction.RIGHT else Direction.LEFT
                } else {
                    if (dy > 0) Direction.DOWN else Direction.UP
                }
                move(direction)
                return true
            }
        }
        return false
    }

    private fun move(direction: Direction) {
        state = step(state, direction)
        render()
    }

    private fun render() {
        hudText.text = "HP ${state.hp}   Seed ${state.level.seed}   Steps ${state.moves}"
        messageText.text = state.message
        mapText.text = renderMap()
    }

    private fun renderMap(): CharSequence {
        val palette = if (highContrast) {
            mapOf(
                '@' to Color.WHITE,
                'S' to Color.CYAN,
                'E' to Color.GREEN,
                '#' to Color.LTGRAY,
                '.' to Color.parseColor("#FFD740"),
                '~' to Color.RED
            )
        } else {
            mapOf(
                '@' to Color.parseColor("#DDEEFF"),
                'S' to Color.parseColor("#90CAF9"),
                'E' to Color.parseColor("#81C784"),
                '#' to Color.parseColor("#9E9E9E"),
                '.' to Color.parseColor("#B0BEC5"),
                '~' to Color.parseColor("#EF9A9A")
            )
        }

        val builder = SpannableStringBuilder()
        state.level.tiles.forEachIndexed { y, row ->
            row.forEachIndexed { x, tile ->
                val c = if (state.player == Pos(x, y)) '@' else tile.glyph
                val start = builder.length
                builder.append(c)
                builder.setSpan(
                    ForegroundColorSpan(palette[c] ?: Color.WHITE),
                    start,
                    start + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            if (y < state.level.height - 1) builder.append('\n')
        }
        return builder
    }
}
