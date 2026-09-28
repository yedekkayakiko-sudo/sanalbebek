package com.ortakyasam.spike

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class Pose { STAND, SIT, CRAWL, LIE, EGG, DANGLE, HANG, EAT }
enum class Mood { HAPPY, LAUGH, SLEEP, SLEEPY, HUNGRY, SURPRISE, SHY }
enum class Arms { NONE, UP, WAVE }

/** Tek karelik çizim girdisi. x, y: ayakların (asılıyken ellerin) piksel konumu. */
data class Frame(
    val x: Float, val y: Float, val u: Float, val stage: Int, val pose: Pose, val mood: Mood,
    val arms: Arms = Arms.NONE, val moving: Boolean = false, val facing: Int = 1, val kick: Boolean = false,
    val lift: Float = 0f, val lookX: Float = 0f, val lookY: Float = 0f, val charging: Boolean = false, val hatch: Float = 1f,
    /** Boy çarpanı (Pet.scale); 0 ise döneme göre. mature 0..1: büyüdükçe kol-bacak uzar, kafa orantıca küçülür. */
    val scale: Float = 0f, val mature: Float = 0f,
)

/** Prototipteki chibi çiziminin Kotlin karşılığı (prototype/index.html → drawChibi, chibiHead). */
class ChibiRenderer(val look: Look) {
    private val INK = 0xFF2B1D14.toInt()
    private val LINE = 0xFFFFF1E0.toInt()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFakeBoldText = true }
    private val path = Path()
    private val rect = RectF()
    private val size = floatArrayOf(0.8f, 0.86f, 0.92f, 1f, 1.06f, 1.14f)

    private fun circle(c: Canvas, x: Float, y: Float, r: Float, color: Int) { fill.shader = null; fill.color = color; c.drawCircle(x, y, r, fill) }
    private fun oval(c: Canvas, cx: Float, cy: Float, rx: Float, ry: Float, color: Int) { fill.shader = null; fill.color = color; rect.set(cx - rx, cy - ry, cx + rx, cy + ry); c.drawOval(rect, fill) }
    private fun line(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int, w: Float) { stroke.color = color; stroke.strokeWidth = w; c.drawLine(x1, y1, x2, y2, stroke) }

    fun draw(c: Canvas, f: Frame, t: Long): PointF {
        val u = f.u * (if (f.scale > 0f) f.scale else size[f.stage.coerceIn(0, 5)])
        val m = f.mature.coerceIn(0f, 1f)
        val r = 12.5f * u * (1 - 0.12f * m); val bw = 12f * u * (1 + 0.1f * m); val bh = 8.5f * u * (1 + 0.35f * m); val lg = 4.5f * u * (1 + 0.8f * m)
        val x = f.x; val y = f.y - f.lift
        val skin = look.skin
        fun limb(x1: Float, y1: Float, x2: Float, y2: Float, w: Float) = line(c, x1, y1, x2, y2, skin, w * u)
        fun foot(fx: Float, fy: Float) = oval(c, fx, fy, 2.8f * u, 1.9f * u, 0xFFFFF4EA.toInt())
        fun shadow(w: Float) = oval(c, x, f.y + 1, w * (1 - min(0.5f, f.lift / (60 * u))), 3 * u, 0x55000000)
        fun onesie(cx: Float, top: Float) {
            path.reset()
            path.moveTo(cx - bw * 0.36f, top); path.lineTo(cx + bw * 0.36f, top)
            path.quadTo(cx + bw * 0.64f, top + bh, cx + bw * 0.5f, top + bh + 1.5f * u)
            path.lineTo(cx - bw * 0.5f, top + bh + 1.5f * u)
            path.quadTo(cx - bw * 0.64f, top + bh, cx - bw * 0.36f, top); path.close()
            fill.shader = null; fill.color = look.outfit; c.drawPath(path, fill)
        }
        val pose = if (f.hatch < 1f) Pose.EGG else f.pose
        val ts = t.toFloat()
        var hx: Float; var hy: Float; var hr = r
        when (pose) {
            Pose.EGG -> {
                shadow(12 * u)
                c.save(); c.translate(x, y - 9 * u)
                val rot = when { f.moving -> sin(ts / 200f) * 20f; f.hatch < 1f -> sin(ts / 60f) * 7f * (1 - f.hatch); else -> 0f }
                c.rotate(rot)
                path.reset(); path.moveTo(-11 * u, -2 * u)
                for (i in 0..6) path.lineTo(-11 * u + i * (22 * u / 6), (if (i % 2 == 1) -5 else -1) * u)
                path.quadTo(12 * u, 10 * u, 0f, 10 * u); path.quadTo(-12 * u, 10 * u, -11 * u, -2 * u); path.close()
                fill.shader = null; fill.color = 0xFFFFF6E6.toInt(); c.drawPath(path, fill)
                stroke.color = LINE; stroke.strokeWidth = 1.5f * u; c.drawPath(path, stroke)
                c.restore()
                hx = x; hy = y - 18 * u; hr = r * 0.88f
            }
            Pose.DANGLE, Pose.HANG -> {
                val hang = pose == Pose.HANG
                val sw = sin(ts / (if (hang) 420f else 240f)) * (if (hang) 6f else 9f)     // derece
                val hyOff = if (hang) 14 * u + r else r * 0.9f
                c.save(); c.translate(x, y); c.rotate(sw)
                if (hang) { limb(-5 * u, hyOff - r * 0.6f, -3 * u, 0f, 3.4f); limb(5 * u, hyOff - r * 0.6f, 3 * u, 0f, 3.4f) }
                val top = hyOff + r * 0.72f; val kick = sin(ts / 130f) * 2.4f * u
                limb(-2.5f * u, top + bh, -3.5f * u + kick, top + bh + lg + 2 * u, 4.2f)
                limb(2.5f * u, top + bh, 3.5f * u - kick, top + bh + lg + 2 * u, 4.2f)
                foot(-3.5f * u + kick, top + bh + lg + 2.6f * u); foot(3.5f * u - kick, top + bh + lg + 2.6f * u)
                onesie(0f, top)
                if (!hang) { limb(-bw * 0.4f, top + 2 * u, -bw * 0.85f, top - 4 * u + kick, 3.4f); limb(bw * 0.4f, top + 2 * u, bw * 0.85f, top - 4 * u - kick, 3.4f) }
                c.restore()
                val rad = Math.toRadians(sw.toDouble())
                hx = x - (sin(rad) * hyOff).toFloat(); hy = y + (cos(rad) * hyOff).toFloat()
            }
            Pose.SIT, Pose.EAT -> {
                val hop = if (f.moving) abs(sin(ts / 130f)) * 3 * u else 0f
                val by = y - hop
                shadow(13 * u)
                limb(x - 3 * u, by - 3 * u, x - 9 * u, y - 1.5f * u, 4.4f); limb(x + 3 * u, by - 3 * u, x + 9 * u, y - 1.5f * u, 4.4f)
                foot(x - 10 * u, y - 1.5f * u); foot(x + 10 * u, y - 1.5f * u)
                oval(c, x, by - 7.5f * u, 8.5f * u, 7.5f * u, look.outfit)
                val sy = by - 11 * u
                if (pose == Pose.EAT) {
                    val k = abs(sin(ts / 300f))
                    limb(x - 6 * u, sy, x - 8 * u, sy + 6 * u, 3.5f)
                    limb(x + 6 * u, sy, x + 3 * u, sy - 7 * u + k * 8 * u, 3.5f)
                    line(c, x + 3 * u, sy - 7 * u + k * 8 * u, x + 1 * u, sy - 10 * u + k * 8 * u, 0xFFD9D9D9.toInt(), 1.4f * u)
                } else arms(c, f, x, sy, u, ts, 9 * u, 5 * u)
                hx = x; hy = by - 15 * u - r * 0.82f
            }
            Pose.CRAWL -> {
                val fc = if (f.facing < 0) -1f else 1f
                val k = if (f.moving) sin(ts / 120f) * 2.5f * u else 0f
                shadow(14 * u)
                limb(x - 7 * u * fc, y - 7 * u, x - 10 * u * fc - k * fc, y - 0.5f * u, 4f)
                limb(x - 4 * u * fc, y - 7 * u, x - 3 * u * fc + k * fc, y - 0.5f * u, 4f)
                oval(c, x - u * fc, y - 9 * u, 10 * u, 6.5f * u, look.outfit)
                val shx = x + 6 * u * fc; val shy = y - 9 * u
                if (f.arms != Arms.NONE) limb(shx, shy, shx + 6 * u * fc, shy - 8 * u, 3.6f) else limb(shx, shy, shx + u * fc + k * fc, y - 0.5f * u, 3.6f)
                hx = x + 11 * u * fc; hy = y - 11 * u - r * 0.8f
            }
            Pose.LIE -> {
                shadow(16 * u)
                oval(c, x + 5 * u, y - 5 * u, 11 * u, 5.5f * u, look.outfit)
                limb(x + 15 * u, y - 4 * u, x + 20 * u, y - 2 * u, 3.5f)
                hx = x - 11 * u; hy = y - 10 * u; hr = r * 0.92f
            }
            Pose.STAND -> {
                val hop = if (f.moving) abs(sin(ts / (if (f.stage == 3) 130f else 105f))) * 3.2f * u else 0f
                val wob = if (f.stage == 3) sin(ts / 420f) * 3f else 0f
                shadow(12 * u)
                c.save(); c.rotate(wob, x, f.y)
                val by = y - hop
                val step = if (f.moving) sin(ts / (if (f.stage == 3) 120f else 95f)) * 2.5f * u else 0f
                val hip = by - lg; val neck = hip - bh
                if (f.kick) {
                    limb(x - 2.5f * u, hip, x - 3 * u, by - 1.5f * u, 4.2f); limb(x + 2.5f * u, hip, x + 9 * u * f.facing, by - 5 * u, 4.2f)
                    foot(x - 3.5f * u, by - u); foot(x + 9.5f * u * f.facing, by - 5 * u)
                } else {
                    limb(x - 2.5f * u, hip, x - 3 * u + step, by - 1.5f * u, 4.2f); limb(x + 2.5f * u, hip, x + 3 * u - step, by - 1.5f * u, 4.2f)
                    foot(x - 3.5f * u + step, by - u); foot(x + 3.5f * u - step, by - u)
                }
                onesie(x, neck)
                arms(c, f, x, neck + 2 * u, u, ts, bw * 0.6f, 6.5f * u, bw * 0.38f)
                c.restore()
                hx = x; hy = neck - r * 0.82f
            }
        }
        head(c, hx, hy, hr, f.mood, f.stage, ts, f.lookX, f.lookY)
        if (f.mood == Mood.SLEEP) {
            text.color = 0xDDFFFFFF.toInt(); text.textSize = 10 * u
            for (i in 0..2) { val p = ((ts / 2400f) + i / 3f) % 1f; text.alpha = ((1 - p) * 220).toInt(); c.drawText("z", hx + 11 * u + p * 14 * u, hy - 10 * u - p * 22 * u, text) }
        }
        if (f.charging) { text.color = 0xFFFFE066.toInt(); text.textSize = 9 * u; text.alpha = 255; c.drawText("⚡", hx + 10 * u, hy - 8 * u, text) }
        if (f.mood == Mood.HUNGRY && pose != Pose.EAT) {
            val bx = hx - 20 * u; val by = hy - 14 * u
            circle(c, hx - 13 * u, hy - 7 * u, 1.5f * u, 0xFFFFFFFF.toInt()); circle(c, bx, by, 7 * u, 0xFFFFFFFF.toInt())
            fill.color = 0xFFCFE3EF.toInt(); rect.set(bx - 2.2f * u, by - 3.5f * u, bx + 2.2f * u, by + 3.5f * u); c.drawRoundRect(rect, 1.4f * u, 1.4f * u, fill)
        }
        val earTop = when (look.ears) { 2 -> 1.9f; 1 -> 1.5f; else -> 1.3f }
        return PointF(hx, hy - hr * earTop - 4 * u)
    }

    private fun arms(c: Canvas, f: Frame, x: Float, sy: Float, u: Float, ts: Float, out: Float, down: Float, from: Float = 6 * u) {
        for (s in intArrayOf(-1, 1)) {
            val sx = x + s * from
            val (ex, ey) = when {
                f.arms == Arms.UP -> Pair(sx + s * 4 * u, sy - 9 * u)
                f.arms == Arms.WAVE && s > 0 -> Pair(sx + 5.5f * u + sin(ts / 110f) * 2.4f * u, sy - 8 * u)
                f.mood == Mood.SLEEPY && s > 0 -> Pair(x + 4 * u, sy - 13 * u)
                else -> Pair(x + s * out, sy + down + s * sin(ts / 420f) * 0.8f * u)
            }
            line(c, sx, sy, ex, ey, look.skin, 3.5f * u)
        }
    }

    private fun head(c: Canvas, x: Float, y: Float, r: Float, mood: Mood, stage: Int, ts: Float, lookX: Float, lookY: Float) {
        val hair = look.hair
        oval(c, x, y + r * 0.08f, r * 1.1f, r * 1.04f, hair)
        if (look.ears == 1) for (s in intArrayOf(-1, 1)) {
            path.reset(); path.moveTo(x + s * r * 0.3f, y - r * 0.8f); path.quadTo(x + s * r * 0.9f, y - r * 1.55f, x + s * r * 0.98f, y - r * 0.42f); path.close()
            fill.shader = null; fill.color = hair; c.drawPath(path, fill)
            path.reset(); path.moveTo(x + s * r * 0.48f, y - r * 0.78f); path.quadTo(x + s * r * 0.86f, y - r * 1.3f, x + s * r * 0.86f, y - r * 0.6f); path.close()
            fill.color = 0xFFF7A9BC.toInt(); c.drawPath(path, fill)
        }
        if (look.ears == 2) for (s in intArrayOf(-1, 1)) {
            c.save(); c.rotate(s * 12f + sin(ts / 700f + s) * 4f, x + s * r * 0.42f, y - r * 1.32f)
            oval(c, x + s * r * 0.42f, y - r * 1.32f, r * 0.22f, r * 0.58f, hair)
            oval(c, x + s * r * 0.42f, y - r * 1.28f, r * 0.1f, r * 0.4f, 0xFFF7A9BC.toInt())
            c.restore()
        }
        oval(c, x, y + r * 0.14f, r * 0.96f, r * 0.86f, look.skin)
        // perçem
        path.reset(); path.moveTo(x - r, y + r * 0.12f)
        path.quadTo(x - r * 1.05f, y - r * 1.02f, x, y - r); path.quadTo(x + r * 1.05f, y - r * 1.02f, x + r, y + r * 0.12f)
        val tips = if (stage < 2) 3 else 5
        for (i in tips downTo 0) path.lineTo(x + r * 0.95f - (i.toFloat() / tips) * r * 1.9f, y - r * (if (i % 2 == 1) 0.34f else 0.12f) - (if (stage < 2) r * 0.22f else 0f))
        path.close(); fill.shader = null; fill.color = hair; c.drawPath(path, fill)
        // tepede tek tel saç
        path.reset(); val wob = sin(ts / 500f) * r * 0.06f
        path.moveTo(x + r * 0.05f, y - r * 0.98f); path.quadTo(x + r * 0.35f + wob, y - r * 1.55f, x + r * 0.55f + wob, y - r * 1.25f)
        stroke.color = hair; stroke.strokeWidth = r * 0.12f; c.drawPath(path, stroke)
        // gözler
        val ex = r * 0.38f; val ey = y + r * 0.24f; val rx = r * 0.235f; val ry = r * 0.3f
        val lx = when (mood) { Mood.HUNGRY -> -0.8f; Mood.SHY -> 0.6f; else -> lookX }
        val ly = if (mood == Mood.SHY) 0.5f else lookY
        stroke.color = INK; stroke.strokeWidth = r * 0.085f
        val blink = (ts % 4100f) < 120f
        for (s in intArrayOf(-1, 1)) {
            val cx = x + s * ex
            when {
                mood == Mood.SLEEP || blink -> { rect.set(cx - rx, ey - rx, cx + rx, ey + rx); c.drawArc(rect, 18f, 144f, false, stroke) }
                mood == Mood.LAUGH -> { path.reset(); path.moveTo(cx - rx, ey + r * 0.04f); path.quadTo(cx, ey - r * 0.2f, cx + rx, ey + r * 0.04f); c.drawPath(path, stroke) }
                mood == Mood.SURPRISE -> { circle(c, cx, ey, rx * 0.95f, 0xFFFFFFFF.toInt()); circle(c, cx, ey, rx * 0.5f, INK); circle(c, cx - rx * 0.2f, ey - rx * 0.25f, rx * 0.2f, 0xFFFFFFFF.toInt()) }
                else -> {
                    val ox = lx * rx * 0.25f; val oy = ly * ry * 0.2f
                    oval(c, cx, ey, rx, ry, 0xFFFFFFFF.toInt())
                    fill.shader = LinearGradient(0f, ey - ry, 0f, ey + ry, intArrayOf(INK, look.eye, look.eye), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
                    rect.set(cx + ox - rx * 0.9f, ey + oy + ry * 0.06f - ry * 0.9f, cx + ox + rx * 0.9f, ey + oy + ry * 0.06f + ry * 0.9f); c.drawOval(rect, fill)
                    fill.shader = null
                    oval(c, cx + ox, ey + oy + ry * 0.1f, rx * 0.42f, ry * 0.46f, INK)
                    circle(c, cx + ox - rx * 0.3f, ey + oy - ry * 0.32f, rx * 0.34f, 0xFFFFFFFF.toInt())
                    circle(c, cx + ox + rx * 0.36f, ey + oy + ry * 0.38f, rx * 0.15f, 0xFFFFFFFF.toInt())
                    rect.set(cx - rx * 1.02f, ey - ry * 1.02f, cx + rx * 1.02f, ey + ry * 1.02f); c.drawArc(rect, 194f, 152f, false, stroke)
                    if (mood == Mood.SLEEPY) { fill.color = look.skin; c.drawRect(cx - rx * 1.2f, ey - ry * 1.25f, cx + rx * 1.2f, ey - ry * 0.05f, fill); c.drawLine(cx - rx, ey, cx + rx, ey, stroke) }
                }
            }
        }
        // yanaklar
        for (s in intArrayOf(-1, 1)) oval(c, x + s * r * 0.62f, y + r * 0.56f, r * 0.2f, r * 0.11f, if (mood == Mood.SHY) 0x9EFF6982.toInt() else 0x73FF7D96)
        // ağız
        val my = y + r * 0.64f
        stroke.color = INK; stroke.strokeWidth = r * 0.07f
        when (mood) {
            Mood.LAUGH, Mood.SURPRISE -> oval(c, x, my + r * 0.04f, r * 0.1f, r * 0.1f, 0xFFC0485A.toInt())
            Mood.SLEEPY -> oval(c, x, my, r * 0.06f, r * 0.08f, 0xFFC0485A.toInt())
            Mood.HUNGRY -> { rect.set(x - r * 0.08f, my - r * 0.02f, x + r * 0.08f, my + r * 0.14f); c.drawArc(rect, 200f, 140f, false, stroke) }
            else -> for (s in intArrayOf(-1, 1)) { rect.set(x + s * r * 0.06f - r * 0.06f, my - r * 0.08f, x + s * r * 0.06f + r * 0.06f, my + r * 0.04f); c.drawArc(rect, 10f, 160f, false, stroke) }   // ω
        }
    }
}
