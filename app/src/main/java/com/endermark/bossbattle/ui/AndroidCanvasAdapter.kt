package com.endermark.bossbattle.ui

import com.endermark.bossbattle.graphics.Canvas
import com.endermark.bossbattle.graphics.LinearGradient
import com.endermark.bossbattle.graphics.Paint
import com.endermark.bossbattle.graphics.Path
import com.endermark.bossbattle.graphics.PathOp
import com.endermark.bossbattle.graphics.RectF
import com.endermark.bossbattle.graphics.Shader

class AndroidCanvasAdapter(
    private val native: android.graphics.Canvas
) : Canvas {

    private val nativePaint = android.graphics.Paint()
    private val nativeRect = android.graphics.RectF()
    private val nativePath = android.graphics.Path()

    override val width: Int get() = native.width
    override val height: Int get() = native.height

    override fun save() {
        native.save()
    }

    override fun restore() {
        native.restore()
    }

    override fun translate(dx: Float, dy: Float) {
        native.translate(dx, dy)
    }

    override fun scale(sx: Float, sy: Float) {
        native.scale(sx, sy)
    }

    override fun rotate(degrees: Float) {
        native.rotate(degrees)
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        native.drawRect(left, top, right, bottom, syncPaint(paint))
    }

    override fun drawRoundRect(rect: RectF, rx: Float, ry: Float, paint: Paint) {
        nativeRect.set(rect.left, rect.top, rect.right, rect.bottom)
        native.drawRoundRect(nativeRect, rx, ry, syncPaint(paint))
    }

    override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) {
        native.drawCircle(cx, cy, radius, syncPaint(paint))
    }

    override fun drawOval(oval: RectF, paint: Paint) {
        nativeRect.set(oval.left, oval.top, oval.right, oval.bottom)
        native.drawOval(nativeRect, syncPaint(paint))
    }

    override fun drawArc(oval: RectF, startAngle: Float, sweepAngle: Float, useCenter: Boolean, paint: Paint) {
        nativeRect.set(oval.left, oval.top, oval.right, oval.bottom)
        native.drawArc(nativeRect, startAngle, sweepAngle, useCenter, syncPaint(paint))
    }

    override fun drawLine(startX: Float, startY: Float, stopX: Float, stopY: Float, paint: Paint) {
        native.drawLine(startX, startY, stopX, stopY, syncPaint(paint))
    }

    override fun drawPath(path: Path, paint: Paint) {
        nativePath.reset()
        for (op in path.ops) {
            when (op) {
                is PathOp.MoveTo -> nativePath.moveTo(op.x, op.y)
                is PathOp.LineTo -> nativePath.lineTo(op.x, op.y)
                PathOp.Close -> nativePath.close()
            }
        }
        native.drawPath(nativePath, syncPaint(paint))
    }

    override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
        native.drawText(text, x, y, syncPaint(paint))
    }

    private fun syncPaint(paint: Paint): android.graphics.Paint {
        nativePaint.reset()
        nativePaint.isAntiAlias = (paint.flags and Paint.ANTI_ALIAS_FLAG) != 0
        nativePaint.color = paint.color
        nativePaint.style = when (paint.style) {
            Paint.Style.FILL -> android.graphics.Paint.Style.FILL
            Paint.Style.STROKE -> android.graphics.Paint.Style.STROKE
            Paint.Style.FILL_AND_STROKE -> android.graphics.Paint.Style.FILL_AND_STROKE
        }
        nativePaint.strokeWidth = paint.strokeWidth
        nativePaint.strokeCap = when (paint.strokeCap) {
            Paint.Cap.BUTT -> android.graphics.Paint.Cap.BUTT
            Paint.Cap.ROUND -> android.graphics.Paint.Cap.ROUND
            Paint.Cap.SQUARE -> android.graphics.Paint.Cap.SQUARE
        }
        nativePaint.textSize = paint.textSize
        nativePaint.textAlign = when (paint.textAlign) {
            Paint.Align.LEFT -> android.graphics.Paint.Align.LEFT
            Paint.Align.CENTER -> android.graphics.Paint.Align.CENTER
            Paint.Align.RIGHT -> android.graphics.Paint.Align.RIGHT
        }
        nativePaint.isFakeBoldText = paint.isFakeBoldText
        val sh = paint.shader
        nativePaint.shader = if (sh is LinearGradient) {
            val mode = when (sh.tileMode) {
                Shader.TileMode.CLAMP -> android.graphics.Shader.TileMode.CLAMP
                Shader.TileMode.REPEAT -> android.graphics.Shader.TileMode.REPEAT
                Shader.TileMode.MIRROR -> android.graphics.Shader.TileMode.MIRROR
            }
            android.graphics.LinearGradient(sh.x0, sh.y0, sh.x1, sh.y1, sh.colors, sh.positions, mode)
        } else {
            null
        }
        return nativePaint
    }
}
