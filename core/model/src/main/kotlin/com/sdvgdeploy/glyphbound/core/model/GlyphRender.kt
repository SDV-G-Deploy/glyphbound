package com.sdvgdeploy.glyphbound.core.model

data class GlyphPalette(
    val player: Int,
    val entry: Int,
    val exit: Int,
    val wall: Int,
    val floor: Int,
    val risk: Int,
    val fallback: Int = 0xFFFFFFFF.toInt()
) {
    fun colorFor(glyph: Char): Int = when (glyph) {
        '@' -> player
        'S' -> entry
        'E' -> exit
        '#' -> wall
        '.' -> floor
        '~' -> risk
        else -> fallback
    }
}

object GlyphRender {
    val defaultPalette = GlyphPalette(
        player = 0xFFDDEEFF.toInt(),
        entry = 0xFF90CAF9.toInt(),
        exit = 0xFF81C784.toInt(),
        wall = 0xFF9E9E9E.toInt(),
        floor = 0xFFB0BEC5.toInt(),
        risk = 0xFFEF9A9A.toInt()
    )

    val highContrastPalette = GlyphPalette(
        player = 0xFFFFFFFF.toInt(),
        entry = 0xFF00FFFF.toInt(),
        exit = 0xFF00FF00.toInt(),
        wall = 0xFFD3D3D3.toInt(),
        floor = 0xFFFFD740.toInt(),
        risk = 0xFFFF0000.toInt()
    )

    fun buildBuffer(level: Level, player: Pos): List<String> {
        return level.tiles.mapIndexed { y, row ->
            buildString(level.width) {
                row.forEachIndexed { x, tile ->
                    append(if (player == Pos(x, y)) '@' else tile.glyph)
                }
            }
        }
    }
}
