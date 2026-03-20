package com.sdvgdeploy.glyphbound

import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.sdvgdeploy.glyphbound.core.model.DifficultyProfile
import com.sdvgdeploy.glyphbound.core.model.Direction
import com.sdvgdeploy.glyphbound.core.model.GameState
import com.sdvgdeploy.glyphbound.core.procgen.LevelGenerator
import com.sdvgdeploy.glyphbound.core.rules.step
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private lateinit var state: GameState
    private lateinit var mapView: GlyphMapView
    private lateinit var hudText: TextView
    private lateinit var messageText: TextView
    private lateinit var profileButton: Button

    private var highContrast = false
    private var baseSeed = 1337L
    private var profile = DifficultyProfile.NORMAL

    private var touchStartX = 0f
    private var touchStartY = 0f
    private val swipeThresholdPx = 48f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        hudText = findViewById(R.id.hudText)
        messageText = findViewById(R.id.messageText)
        profileButton = findViewById(R.id.profileButton)

        baseSeed = intent?.getLongExtra("seed", 1337L) ?: 1337L
        profile = DifficultyProfile.fromRaw(intent?.getStringExtra("profile"))

        findViewById<Button>(R.id.upButton).setOnClickListener { move(Direction.UP) }
        findViewById<Button>(R.id.downButton).setOnClickListener { move(Direction.DOWN) }
        findViewById<Button>(R.id.leftButton).setOnClickListener { move(Direction.LEFT) }
        findViewById<Button>(R.id.rightButton).setOnClickListener { move(Direction.RIGHT) }

        findViewById<Switch>(R.id.highContrastSwitch).setOnCheckedChangeListener { _, checked ->
            highContrast = checked
            render()
        }

        profileButton.setOnClickListener {
            profile = DifficultyProfile.entries[(profile.ordinal + 1) % DifficultyProfile.entries.size]
            restartLevel()
        }

        mapView.setOnTouchListener { _, event -> handleSwipe(event) }

        restartLevel()
    }

    private fun restartLevel() {
        val level = LevelGenerator.generate(baseSeed, profile)
        state = GameState(level = level, player = level.entry, profile = profile)
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
        val reproKey = "${baseSeed}:${profile.name}"
        hudText.text = "HP ${state.hp}   Seed $reproKey   Steps ${state.moves}"
        profileButton.text = profile.name
        messageText.text = state.message
        mapView.render(
            buffer = com.sdvgdeploy.glyphbound.core.model.GlyphRender.buildBuffer(state.level, state.player),
            highContrast = highContrast
        )
    }
}
