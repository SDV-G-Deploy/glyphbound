package com.sdvgdeploy.glyphbound

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.sdvgdeploy.glyphbound.core.model.GlyphPalette
import com.sdvgdeploy.glyphbound.core.model.GlyphRender

class GlyphMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13f, resources.displayMetrics)
    }

    private var rows: List<String> = emptyList()
    private var palette: GlyphPalette = GlyphRender.defaultPalette

    fun render(buffer: List<String>, highContrast: Boolean) {
        rows = buffer
        palette = if (highContrast) GlyphRender.highContrastPalette else GlyphRender.defaultPalette
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (rows.isEmpty()) return

        val cellWidth = glyphPaint.measureText("M")
        val fm = glyphPaint.fontMetrics
        val cellHeight = (fm.descent - fm.ascent)
        val baselineShift = -fm.ascent

        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                glyphPaint.color = palette.colorFor(ch)
                val px = x * cellWidth
                val py = y * cellHeight + baselineShift
                canvas.drawText(ch.toString(), px, py, glyphPaint)
            }
        }
    }
}
