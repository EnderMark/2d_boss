package com.endermark.bossbattle.web

import com.endermark.bossbattle.graphics.Canvas
import com.endermark.bossbattle.graphics.LinearGradient
import com.endermark.bossbattle.graphics.Paint
import com.endermark.bossbattle.graphics.Path
import com.endermark.bossbattle.graphics.PathOp
import com.endermark.bossbattle.graphics.RectF
import org.w3c.dom.BUTT
import org.w3c.dom.CENTER
import org.w3c.dom.CanvasLineCap
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.LEFT
import org.w3c.dom.RIGHT
import org.w3c.dom.ROUND
import org.w3c.dom.SQUARE
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min

class HtmlCanvasAdapter(
    private val ctx: CanvasRenderingContext2D,
    override val width: Int,
    override val height: Int
) : Canvas {

    override fun save() = ctx.save()
    override fun restore() = ctx.restore()
    override fun translate(dx: Float, dy: Float) = ctx.translate(dx.toDouble(), dy.toDouble())
    override fun scale(sx: Float, sy: Float) = ctx.scale(sx.toDouble(), sy.toDouble())
    override fun rotate(degrees: Float) = ctx.rotate(degrees.toDouble() * PI / 180.0)

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        val w = (right - left).toDouble()
        val h = (bottom - top).toDouble()
        applyPaint(paint)
        ctx.beginPath()
        ctx.rect(left.toDouble(), top.toDouble(), w, h)
        fillOrStroke(paint)
    }

    override fun drawRoundRect(rect: RectF, rx: Float, ry: Float, paint: Paint) {
        val x = rect.left.toDouble()
        val y = rect.top.toDouble()
        val w = rect.width().toDouble()
        val h = rect.height().toDouble()
        if (w <= 0.0 || h <= 0.0) return
        val r = min(rx.toDouble(), min(w / 2.0, h / 2.0)).coerceAtLeast(0.0)
        applyPaint(paint)
        ctx.beginPath()
        ctx.moveTo(x + r, y)
        ctx.lineTo(x + w - r, y)
        ctx.quadraticCurveTo(x + w, y, x + w, y + r)
        ctx.lineTo(x + w, y + h - r)
        ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h)
        ctx.lineTo(x + r, y + h)
        ctx.quadraticCurveTo(x, y + h, x, y + h - r)
        ctx.lineTo(x, y + r)
        ctx.quadraticCurveTo(x, y, x + r, y)
        ctx.closePath()
        fillOrStroke(paint)
    }

    override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) {
        if (radius <= 0f) return
        applyPaint(paint)
        ctx.beginPath()
        ctx.arc(cx.toDouble(), cy.toDouble(), radius.toDouble(), 0.0, PI * 2.0)
        fillOrStroke(paint)
    }

    override fun drawOval(oval: RectF, paint: Paint) {
        val rx = abs(oval.width() * 0.5).coerceAtLeast(0.01)
        val ry = abs(oval.height() * 0.5).coerceAtLeast(0.01)
        applyPaint(paint)
        ctx.beginPath()
        ctx.ellipse(oval.centerX().toDouble(), oval.centerY().toDouble(), rx, ry, 0.0, 0.0, PI * 2.0)
        fillOrStroke(paint)
    }

    override fun drawArc(oval: RectF, startAngle: Float, sweepAngle: Float, useCenter: Boolean, paint: Paint) {
        val rx = abs(oval.width() * 0.5).coerceAtLeast(0.01)
        val ry = abs(oval.height() * 0.5).coerceAtLeast(0.01)
        val startRad = startAngle.toDouble() * PI / 180.0
        val endRad = (startAngle + sweepAngle).toDouble() * PI / 180.0
        val cx = oval.centerX().toDouble()
        val cy = oval.centerY().toDouble()
        applyPaint(paint)
        ctx.beginPath()
        if (useCenter) {
            ctx.moveTo(cx, cy)
        }
        ctx.ellipse(cx, cy, rx, ry, 0.0, startRad, endRad, sweepAngle < 0f)
        if (useCenter) {
            ctx.closePath()
        }
        fillOrStroke(paint)
    }

    override fun drawLine(startX: Float, startY: Float, stopX: Float, stopY: Float, paint: Paint) {
        applyPaint(paint)
        ctx.beginPath()
        ctx.moveTo(startX.toDouble(), startY.toDouble())
        ctx.lineTo(stopX.toDouble(), stopY.toDouble())
        ctx.stroke()
    }

    override fun drawPath(path: Path, paint: Paint) {
        if (path.ops.isEmpty()) return
        applyPaint(paint)
        ctx.beginPath()
        for (op in path.ops) {
            when (op) {
                is PathOp.MoveTo -> ctx.moveTo(op.x.toDouble(), op.y.toDouble())
                is PathOp.LineTo -> ctx.lineTo(op.x.toDouble(), op.y.toDouble())
                PathOp.Close -> ctx.closePath()
            }
        }
        fillOrStroke(paint)
    }

    override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
        applyPaint(paint)
        val weight = if (paint.isFakeBoldText) "bold " else ""
        ctx.font = "$weight${paint.textSize}px system-ui, -apple-system, sans-serif"
        ctx.textAlign = when (paint.textAlign) {
            Paint.Align.LEFT -> CanvasTextAlign.LEFT
            Paint.Align.CENTER -> CanvasTextAlign.CENTER
            Paint.Align.RIGHT -> CanvasTextAlign.RIGHT
        }
        if (paint.style == Paint.Style.STROKE) {
            ctx.strokeText(text, x.toDouble(), y.toDouble())
        } else {
            ctx.fillText(text, x.toDouble(), y.toDouble())
        }
    }

    private fun applyPaint(paint: Paint) {
        val sh = paint.shader
        val styleVal: dynamic = if (sh is LinearGradient) {
            val g = ctx.createLinearGradient(sh.x0.toDouble(), sh.y0.toDouble(), sh.x1.toDouble(), sh.y1.toDouble())
            val n = sh.colors.size
            for (i in 0 until n) {
                val stop = sh.positions?.getOrNull(i)?.toDouble()
                    ?: if (n <= 1) 0.0 else i.toDouble() / (n - 1).toDouble()
                g.addColorStop(stop.coerceIn(0.0, 1.0), argbToCss(sh.colors[i]))
            }
            g
        } else {
            argbToCss(paint.color)
        }
        ctx.fillStyle = styleVal
        ctx.strokeStyle = styleVal
        ctx.lineWidth = paint.strokeWidth.toDouble()
        ctx.lineCap = when (paint.strokeCap) {
            Paint.Cap.BUTT -> CanvasLineCap.BUTT
            Paint.Cap.ROUND -> CanvasLineCap.ROUND
            Paint.Cap.SQUARE -> CanvasLineCap.SQUARE
        }
    }

    private fun fillOrStroke(paint: Paint) {
        when (paint.style) {
            Paint.Style.FILL -> ctx.fill()
            Paint.Style.STROKE -> ctx.stroke()
            Paint.Style.FILL_AND_STROKE -> {
                ctx.fill()
                ctx.stroke()
            }
        }
    }

    private fun argbToCss(argb: Int): String {
        val a = ((argb ushr 24) and 0xFF) / 255.0
        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF
        return "rgba($r,$g,$b,$a)"
    }
}
