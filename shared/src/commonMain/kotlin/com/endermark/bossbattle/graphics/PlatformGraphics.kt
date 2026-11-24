package com.endermark.bossbattle.graphics

object Color {
    const val WHITE: Int = 0xFFFFFFFF.toInt()
    const val BLACK: Int = 0xFF000000.toInt()
    const val TRANSPARENT: Int = 0x00000000

    fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int {
        return ((alpha and 0xFF) shl 24) or
            ((red and 0xFF) shl 16) or
            ((green and 0xFF) shl 8) or
            (blue and 0xFF)
    }

    fun parseColor(colorString: String): Int {
        val hex = colorString.removePrefix("#")
        return when (hex.length) {
            6 -> (0xFF000000L or hex.toLong(16)).toInt()
            8 -> hex.toLong(16).toInt()
            else -> WHITE
        }
    }
}

class RectF(
    var left: Float = 0f,
    var top: Float = 0f,
    var right: Float = 0f,
    var bottom: Float = 0f
) {
    fun set(left: Float, top: Float, right: Float, bottom: Float) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }

    fun width(): Float = right - left
    fun height(): Float = bottom - top
    fun centerX(): Float = (left + right) * 0.5f
    fun centerY(): Float = (top + bottom) * 0.5f
    fun contains(x: Float, y: Float): Boolean = x >= left && x <= right && y >= top && y <= bottom
}

sealed class PathOp {
    data class MoveTo(val x: Float, val y: Float) : PathOp()
    data class LineTo(val x: Float, val y: Float) : PathOp()
    data object Close : PathOp()
}

class Path {
    val ops: MutableList<PathOp> = mutableListOf()

    fun reset() {
        ops.clear()
    }

    fun moveTo(x: Float, y: Float) {
        ops.add(PathOp.MoveTo(x, y))
    }

    fun lineTo(x: Float, y: Float) {
        ops.add(PathOp.LineTo(x, y))
    }

    fun close() {
        ops.add(PathOp.Close)
    }
}

open class Shader {
    enum class TileMode {
        CLAMP,
        REPEAT,
        MIRROR
    }
}

class LinearGradient(
    val x0: Float,
    val y0: Float,
    val x1: Float,
    val y1: Float,
    val colors: IntArray,
    val positions: FloatArray? = null,
    val tileMode: TileMode = TileMode.CLAMP
) : Shader() {
    constructor(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        color0: Int,
        color1: Int,
        tileMode: TileMode = TileMode.CLAMP
    ) : this(x0, y0, x1, y1, intArrayOf(color0, color1), null, tileMode)
}

class Paint(val flags: Int = 0) {
    var color: Int = Color.BLACK
    var style: Style = Style.FILL
    var strokeWidth: Float = 1f
    var strokeCap: Cap = Cap.BUTT
    var textSize: Float = 16f
    var textAlign: Align = Align.LEFT
    var isFakeBoldText: Boolean = false
    var shader: Shader? = null

    var alpha: Int
        get() = (color ushr 24) and 0xFF
        set(value) {
            color = ((value and 0xFF) shl 24) or (color and 0x00FFFFFF)
        }

    fun measureText(text: String): Float {
        // Consistent cross-platform approximation for bold/regular sans-serif text
        val factor = if (isFakeBoldText) 0.60f else 0.54f
        return text.length * textSize * factor
    }

    enum class Style {
        FILL,
        STROKE,
        FILL_AND_STROKE
    }

    enum class Cap {
        BUTT,
        ROUND,
        SQUARE
    }

    enum class Align {
        LEFT,
        CENTER,
        RIGHT
    }

    companion object {
        const val ANTI_ALIAS_FLAG: Int = 1
    }
}

interface Canvas {
    val width: Int
    val height: Int

    fun save()
    fun restore()
    fun translate(dx: Float, dy: Float)
    fun scale(sx: Float, sy: Float)
    fun rotate(degrees: Float)

    fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint)
    fun drawRect(rect: RectF, paint: Paint) = drawRect(rect.left, rect.top, rect.right, rect.bottom, paint)
    fun drawRoundRect(rect: RectF, rx: Float, ry: Float, paint: Paint)
    fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint)
    fun drawOval(oval: RectF, paint: Paint)
    fun drawArc(oval: RectF, startAngle: Float, sweepAngle: Float, useCenter: Boolean, paint: Paint)
    fun drawLine(startX: Float, startY: Float, stopX: Float, stopY: Float, paint: Paint)
    fun drawPath(path: Path, paint: Paint)
    fun drawText(text: String, x: Float, y: Float, paint: Paint)
}
