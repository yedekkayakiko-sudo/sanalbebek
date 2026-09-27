package com.ortakyasam.spike

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Uygulamanın ana ekranı: Pou tarzı odalar. Ortada büyük bebek; mutfakta yemeği ağzına sürükle,
 * yatak odasında ışığı kapat, banyoda sabunla köpürt ve duş yaptır, oyun odasında topla oyna.
 * Hepsi kodla çiziliyor; bağımlılık yok.
 */
@SuppressLint("ViewConstructor")
class RoomView(ctx: Context, var pet: Pet, private val onSettings: () -> Unit) : View(ctx) {
    var topInset = 0f
    var bottomInset = 0f
    var needsSetup = false

    private val d = resources.displayMetrics.density
    private var chibi = ChibiRenderer(pet.look)
    private val rnd = Random(SystemClock.uptimeMillis())
    private var room = 0
    private val ROOMS = listOf("🍽️" to "Mutfak", "🛏️" to "Yatak", "🛁" to "Banyo", "⚽" to "Oyun")
    private val FOODS = listOf("🍎", "🍌", "🥛", "🍪", "🥕")

    // Yerleşim
    private var barTop = 0f
    private var trayY = 0f
    private var floorY = 0f
    private var cx = 0f
    private var u = 4f

    // Durum
    private var lastT = 0L
    private var dragItem: String? = null
    private var dragX = 0f
    private var dragY = 0f
    private var petting = false
    private var lastPetHeart = 0L
    private var tickleUntil = 0L
    private var eatUntil = 0L
    private var refuseUntil = 0L
    private var waveUntil = 0L
    private var say: String? = null
    private var sayUntil = 0L
    private var hatchTaps = 0
    private var showerUntil = 0L
    private val foam = ArrayList<FloatArray>()                 // köpük: bebeğe göre x, y, yarıçap
    private var ball: FloatArray? = null                        // x, y, vx, vy
    private val parts = ArrayList<Part>()
    private var nextIdle = 0L

    private class Part(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val max: Float, val s: String, val size: Float, val grav: Float)

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val rect = RectF()
    private val path = Path()

    fun reloadPet(p: Pet) {
        pet = p
        if (chibi.look != p.look) chibi = ChibiRenderer(p.look)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) = layoutRoom()

    private fun layoutRoom() {
        val w = width.toFloat(); val h = height.toFloat()
        if (w == 0f) return
        barTop = h - bottomInset - 76 * d
        trayY = barTop - 52 * d
        floorY = trayY - 58 * d
        cx = w / 2
        val avail = floorY - (topInset + 130 * d)
        u = min(w / 58f, avail / 52f)
    }

    fun setInsets(top: Float, bottom: Float) { topInset = top; bottomInset = bottom; layoutRoom(); invalidate() }

    // ---------- Çizim ----------

    override fun onDraw(c: Canvas) {
        val now = SystemClock.uptimeMillis()
        val dt = if (lastT == 0L) 0f else min(0.05f, (now - lastT) / 1000f)
        lastT = now
        update(now, dt)
        drawRoom(c, now)
        drawBaby(c, now)
        drawParts(c)
        if (room == 1 && pet.asleep) { fill.color = 0x99000018.toInt(); c.drawRect(0f, 0f, width.toFloat(), trayY - 40 * d, fill); drawMoon(c) }
        drawHud(c)
        drawTray(c, now)
        drawBar(c)
        dragItem?.let { text.textSize = 44 * d; text.alpha = 255; c.drawText(it, dragX, dragY + 15 * d, text) }
        postInvalidateOnAnimation()
    }

    private fun update(now: Long, dt: Float) {
        // parçacıklar
        val it = parts.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.vy += p.grav * dt; p.x += p.vx * dt; p.y += p.vy * dt; p.life -= dt
            if (p.life <= 0f) it.remove()
        }
        // duş
        if (now < showerUntil) {
            repeat(2) { parts.add(Part(cx + (rnd.nextFloat() - 0.5f) * 40 * u, floorY - 60 * u, 0f, 500 * d, 0.9f, 0.9f, "💧", 16 * d, 0f)) }
            if (foam.isNotEmpty() && rnd.nextFloat() < 0.3f) foam.removeAt(0)
        }
        // top
        ball?.let { b ->
            b[3] += 1400 * d * dt; b[0] += b[2] * dt; b[1] += b[3] * dt
            val r = 22 * d
            if (b[1] > floorY - r) { b[1] = floorY - r; b[3] = -abs(b[3]) * 0.62f; b[2] *= 0.9f; if (abs(b[3]) < 60 * d) b[3] = 0f }
            if (b[0] < r) { b[0] = r; b[2] = abs(b[2]) }
            if (b[0] > width - r) { b[0] = width - r; b[2] = -abs(b[2]) }
        }
        // boştayken ara sıra el sallar, konuşur
        if (now > nextIdle) {
            nextIdle = now + 5000 + rnd.nextLong(5000)
            if (pet.hatched && !pet.asleep && dragItem == null) {
                when {
                    pet.hunger > 70 -> talk(if (pet.stage >= 4) "acıktım!" else "mama?")
                    pet.clean < 30 -> talk(if (pet.stage >= 4) "banyo?" else "ııh")
                    pet.joy < 30 -> talk(if (pet.stage >= 4) "oynayalım mı?" else "ba?")
                    pet.energy < 25 -> talk("hıı…")
                    rnd.nextFloat() < 0.5f -> { waveUntil = now + 1500; talk(hello()) }
                }
            }
        }
    }

    private fun drawRoom(c: Canvas, now: Long) {
        val w = width.toFloat(); val h = height.toFloat()
        val wallBottom = floorY - 8 * u
        val (wall, wall2, floor) = when (room) {
            0 -> Triple(0xFFFFE9D2.toInt(), 0xFFFFDDBB.toInt(), 0xFFD9A77A.toInt())
            1 -> Triple(0xFFB9B3EC.toInt(), 0xFF9C95DB.toInt(), 0xFF8C6E9E.toInt())
            2 -> Triple(0xFFD3F1F6.toInt(), 0xFFB6E4EE.toInt(), 0xFF8FC7D3.toInt())
            else -> Triple(0xFFD9F2CB.toInt(), 0xFFBFE6AA.toInt(), 0xFF86BF62.toInt())
        }
        fill.shader = LinearGradient(0f, 0f, 0f, wallBottom, wall, wall2, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, wallBottom, fill); fill.shader = null
        // duvar deseni
        fill.color = 0x14FFFFFF
        when (room) {
            0, 2 -> { val s = 34 * d; var y = topInset + 110 * d; while (y < wallBottom) { var x = if (((y / s).toInt()) % 2 == 0) 0f else s / 2; while (x < w) { rect.set(x + 2, y + 2, x + s - 2, y + s - 2); c.drawRoundRect(rect, 5 * d, 5 * d, fill); x += s }; y += s } }
            else -> { var x = 0f; while (x < w) { c.drawRect(x, 0f, x + 18 * d, wallBottom, fill); x += 36 * d } }
        }
        // zemin
        fill.color = floor; c.drawRect(0f, wallBottom, w, h, fill)
        fill.color = 0x22000000; c.drawRect(0f, wallBottom, w, wallBottom + 5 * d, fill)
        stroke.color = 0x18000000; stroke.strokeWidth = 2 * d
        var fy = wallBottom + 22 * d; while (fy < h) { c.drawLine(0f, fy, w, fy, stroke); fy += 26 * d }
        // odaya özel eşyalar
        when (room) {
            0 -> { // raf ve kavanozlar
                fill.color = 0xFFB9855A.toInt(); rect.set(w * 0.08f, topInset + 150 * d, w * 0.42f, topInset + 160 * d); c.drawRoundRect(rect, 4 * d, 4 * d, fill)
                text.textSize = 30 * d; text.alpha = 255
                c.drawText("🫙", w * 0.15f, topInset + 146 * d, text); c.drawText("🧂", w * 0.25f, topInset + 146 * d, text); c.drawText("🍯", w * 0.35f, topInset + 146 * d, text)
                window(c, w * 0.62f, topInset + 118 * d, w * 0.3f, 90 * d)
            }
            1 -> { // pencere ve yatak
                window(c, w * 0.08f, topInset + 118 * d, w * 0.3f, 100 * d)
                fill.color = 0xFF6C5A9E.toInt(); rect.set(cx - 30 * u, floorY - 14 * u, cx + 30 * u, floorY + 2 * u); c.drawRoundRect(rect, 5 * u, 5 * u, fill)
                fill.color = 0xFFF7F3FF.toInt(); rect.set(cx - 26 * u, floorY - 13 * u, cx + 26 * u, floorY - 5 * u); c.drawRoundRect(rect, 4 * u, 4 * u, fill)
                fill.color = 0xFFFFFFFF.toInt(); rect.set(cx - 25 * u, floorY - 18 * u, cx - 11 * u, floorY - 10 * u); c.drawRoundRect(rect, 4 * u, 4 * u, fill)
            }
            2 -> { // küvet
                fill.color = 0xFFFFFFFF.toInt(); rect.set(cx - 28 * u, floorY - 12 * u, cx + 28 * u, floorY + 3 * u); c.drawRoundRect(rect, 7 * u, 7 * u, fill)
                fill.color = 0xFF9ADCEB.toInt(); rect.set(cx - 25 * u, floorY - 10 * u, cx + 25 * u, floorY - 6 * u); c.drawRoundRect(rect, 3 * u, 3 * u, fill)
                text.textSize = 34 * d; text.alpha = 255; c.drawText("🦆", w * 0.82f, floorY - 2 * d, text)
            }
            else -> {
                window(c, w * 0.6f, topInset + 118 * d, w * 0.32f, 96 * d)
                text.textSize = 38 * d; text.alpha = 255; c.drawText("🧸", w * 0.14f, floorY + 6 * d, text)
            }
        }
    }

    private fun window(c: Canvas, x: Float, y: Float, ww: Float, hh: Float) {
        val night = pet.asleep && room == 1
        fill.color = 0xFFFFFFFF.toInt(); rect.set(x - 5 * d, y - 5 * d, x + ww + 5 * d, y + hh + 5 * d); c.drawRoundRect(rect, 10 * d, 10 * d, fill)
        fill.shader = LinearGradient(0f, y, 0f, y + hh, if (night) 0xFF10163A.toInt() else 0xFF7EC3F0.toInt(), if (night) 0xFF2B2A5E.toInt() else 0xFFCDEBFF.toInt(), Shader.TileMode.CLAMP)
        rect.set(x, y, x + ww, y + hh); c.drawRoundRect(rect, 7 * d, 7 * d, fill); fill.shader = null
        if (!night) { fill.color = 0xDDFFFFFF.toInt(); c.drawCircle(x + ww * 0.3f, y + hh * 0.45f, 12 * d, fill); c.drawCircle(x + ww * 0.42f, y + hh * 0.4f, 15 * d, fill); c.drawCircle(x + ww * 0.55f, y + hh * 0.47f, 11 * d, fill) }
        fill.color = 0xFFFFFFFF.toInt(); c.drawRect(x + ww / 2 - 2 * d, y, x + ww / 2 + 2 * d, y + hh, fill)
    }

    private fun drawMoon(c: Canvas) {
        text.textSize = 30 * d; text.alpha = 255
        c.drawText("🌙", width * 0.23f, topInset + 175 * d, text)
    }

    private fun mouth(): Pair<Float, Float> {
        val sz = floatArrayOf(0.8f, 0.86f, 0.92f, 1f, 1.06f, 1.14f)[pet.stage.coerceIn(0, 5)]
        val up = if (pet.stage >= 3) 15.5f else 17f
        return Pair(cx, floorY - up * u * sz)
    }

    private fun drawBaby(c: Canvas, now: Long) {
        val st = pet.stage.coerceIn(0, 5)
        val (mx, my) = mouth()
        val nearFood = dragItem != null && room == 0 && hypot(dragX - mx, dragY - my) < 60 * d
        val mood = when {
            pet.asleep -> Mood.SLEEP
            now < tickleUntil || now < showerUntil -> Mood.LAUGH
            now < refuseUntil -> Mood.SHY
            nearFood -> Mood.SURPRISE
            pet.hunger > 70 -> Mood.HUNGRY
            pet.energy < 25 -> Mood.SLEEPY
            else -> Mood.HAPPY
        }
        val lookX: Float; val lookY: Float
        val target = when { dragItem != null -> Pair(dragX, dragY); ball != null -> Pair(ball!![0], ball!![1]); else -> null }
        if (target != null && !pet.asleep) {
            val dx = target.first - cx; val dy = target.second - my
            val l = hypot(dx, dy).coerceAtLeast(1f)
            lookX = (dx / l).coerceIn(-1f, 1f); lookY = (dy / l).coerceIn(-1f, 1f)
        } else { lookX = if (now < refuseUntil) 1f else 0f; lookY = 0f }
        val pose = when {
            !pet.hatched || st == 0 -> Pose.EGG
            pet.asleep -> Pose.LIE
            now < eatUntil -> Pose.EAT
            st <= 2 -> Pose.SIT
            else -> Pose.STAND
        }
        val arms = when {
            now < waveUntil -> Arms.WAVE
            now < showerUntil || (ball != null && abs(ball!![3]) > 0f) -> Arms.UP
            else -> Arms.NONE
        }
        val hatch = if (pet.hatched) 1f else (hatchTaps / 4f).coerceAtMost(0.95f)
        val bob = if (pose == Pose.STAND || pose == Pose.SIT) sin(now / 600.0).toFloat() * 0.6f * u else 0f
        val x = if (pose == Pose.LIE) cx + 4 * u else cx
        val y = if (room == 1 && pose == Pose.LIE) floorY - 11 * u else floorY
        val p = chibi.draw(c, Frame(x, y + bob, u, st, pose, mood, arms, lookX = lookX, lookY = lookY, hatch = hatch), now)

        // köpük
        fill.color = 0xF0FFFFFF.toInt(); stroke.color = 0x66B6E4EE; stroke.strokeWidth = 1.2f * d
        for (f in foam) { c.drawCircle(cx + f[0] * u, floorY + f[1] * u, f[2] * u, fill); c.drawCircle(cx + f[0] * u, floorY + f[1] * u, f[2] * u, stroke) }
        // kirliyken koku çizgileri
        if (pet.clean < 35 && pet.hatched && foam.isEmpty()) {
            stroke.color = 0x9977A85A.toInt(); stroke.strokeWidth = 2.5f * d
            for (k in 0..2) {
                val sx = cx + (k - 1) * 14 * u; val base = floorY - 44 * u
                path.reset(); path.moveTo(sx, base)
                for (i in 1..6) path.lineTo(sx + sin(now / 300.0 + i + k).toFloat() * 3 * d, base - i * 5 * d)
                c.drawPath(path, stroke)
            }
        }
        // konuşma balonu
        val s = say
        if (s != null && now < sayUntil) bubble(c, p.x, p.y, s) else say = null
        if (!pet.hatched) bubble(c, cx, floorY - 34 * u, "dokun! 🥚")
    }

    private fun bubble(c: Canvas, px: Float, py: Float, s: String) {
        text.textSize = 20 * d; text.alpha = 255; text.color = 0xFF3A2A20.toInt(); text.isFakeBoldText = true
        val tw = text.measureText(s); val pad = 14 * d; val bh = 42 * d
        val left = (px - tw / 2 - pad).coerceIn(8 * d, width - tw - 2 * pad - 8 * d)
        val bottom = py.coerceAtLeast(topInset + 130 * d + bh)
        fill.color = 0xF7FFFFFF.toInt(); rect.set(left, bottom - bh, left + tw + 2 * pad, bottom); c.drawRoundRect(rect, bh / 2, bh / 2, fill)
        path.reset(); path.moveTo(px - 8 * d, bottom - 1); path.lineTo(px + 8 * d, bottom - 1); path.lineTo(px, bottom + 10 * d); path.close(); c.drawPath(path, fill)
        c.drawText(s, left + pad + tw / 2, bottom - bh / 2 + 7 * d, text)
        text.isFakeBoldText = false
    }

    private fun drawParts(c: Canvas) {
        for (p in parts) {
            text.textSize = p.size; text.alpha = ((p.life / p.max).coerceIn(0f, 1f) * 255).toInt()
            c.drawText(p.s, p.x, p.y, text)
        }
        text.alpha = 255
    }

    private fun gaugeX(i: Int) = width * (0.12f + i * 0.19f)
    private fun hudY() = topInset + 40 * d

    private fun drawHud(c: Canvas) {
        val stats = listOf(Triple("🍗", 100 - pet.hunger, "Tokluk"), Triple("⚡", pet.energy, "Enerji"), Triple("😊", pet.joy, "Neşe"), Triple("🧼", pet.clean, "Temizlik"))
        val r = 25 * d; val y = hudY()
        stats.forEachIndexed { i, (emo, v, label) ->
            val x = gaugeX(i)
            fill.color = 0xF2FFFFFF.toInt(); c.drawCircle(x, y, r + 5 * d, fill)
            stroke.strokeWidth = 5 * d; stroke.color = 0xFFEDE4DA.toInt(); c.drawCircle(x, y, r, stroke)
            stroke.color = when { v > 60 -> 0xFF4CB86A.toInt(); v > 30 -> 0xFFF2B33D.toInt(); else -> 0xFFE8574A.toInt() }
            rect.set(x - r, y - r, x + r, y + r); c.drawArc(rect, -90f, 360f * (v / 100f), false, stroke)
            text.textSize = 22 * d; text.alpha = 255; c.drawText(emo, x, y + 8 * d, text)
            text.textSize = 11 * d; text.color = 0xFF3A2A20.toInt(); c.drawText(label, x, y + r + 20 * d, text)
        }
        // ayarlar
        val gx = width * 0.9f
        fill.color = 0xF2FFFFFF.toInt(); c.drawCircle(gx, y, r, fill)
        text.textSize = 22 * d; c.drawText("⚙️", gx, y + 8 * d, text)
        text.textSize = 11 * d; c.drawText("Ayarlar", gx, y + r + 20 * d, text)
        // isim
        val name = "${pet.name} · ${Pet.STAGE_NAMES[pet.stage]}"
        text.textSize = 16 * d; text.isFakeBoldText = true
        val tw = text.measureText(name)
        fill.color = 0xCCFFFFFF.toInt(); rect.set(cx - tw / 2 - 14 * d, y + r + 30 * d, cx + tw / 2 + 14 * d, y + r + 60 * d); c.drawRoundRect(rect, 15 * d, 15 * d, fill)
        c.drawText(name, cx, y + r + 51 * d, text); text.isFakeBoldText = false
    }

    private fun trayItems(): List<String> = when (room) {
        0 -> FOODS
        1 -> listOf(if (pet.asleep) "☀️" else "💡")
        2 -> listOf("🧼", "🚿")
        else -> listOf("⚽", "🎈")
    }

    private fun trayX(i: Int, n: Int) = width * (i + 0.5f) / n.coerceAtLeast(1) * (if (n <= 2) 0.6f else 1f) + (if (n <= 2) width * 0.2f else 0f)

    private fun drawTray(c: Canvas, now: Long) {
        val items = trayItems()
        fill.color = 0x66FFFFFF; rect.set(10 * d, trayY - 38 * d, width - 10 * d, trayY + 38 * d); c.drawRoundRect(rect, 26 * d, 26 * d, fill)
        items.forEachIndexed { i, s ->
            val x = trayX(i, items.size)
            fill.color = 0xFFFFFFFF.toInt(); c.drawCircle(x, trayY, 30 * d, fill)
            text.textSize = 34 * d; text.alpha = if (dragItem == s) 90 else 255; c.drawText(s, x, trayY + 12 * d, text)
        }
        text.alpha = 255
        val hint = when (room) {
            0 -> "Yemeği sürükleyip ağzına götür"
            1 -> if (pet.asleep) "Uyandırmak için güneşe dokun" else "Uyutmak için ışığı kapat"
            2 -> "Sabunu bebeğin üstünde gezdir, sonra duş"
            else -> "Topu at, sonra topa dokun"
        }
        text.textSize = 13 * d; text.color = 0xFF3A2A20.toInt(); c.drawText(hint, cx, trayY - 44 * d, text)
        if (needsSetup) {
            val y = topInset + 118 * d + 14 * d
            val pulse = 0.85f + 0.15f * sin(now / 300.0).toFloat()
            fill.color = 0xFFEC6E4C.toInt(); rect.set(cx - 150 * d * pulse, y - 18 * d, cx + 150 * d * pulse, y + 18 * d); c.drawRoundRect(rect, 18 * d, 18 * d, fill)
            text.color = 0xFFFFFFFF.toInt(); text.textSize = 14 * d; text.isFakeBoldText = true
            c.drawText("📱 Bebeği telefon ekranına koy  →", cx, y + 5 * d, text)
            text.isFakeBoldText = false; text.color = 0xFF3A2A20.toInt()
        }
    }

    private fun drawBar(c: Canvas) {
        fill.color = 0xFFFFFFFF.toInt(); rect.set(0f, barTop, width.toFloat(), height.toFloat() + 30 * d); c.drawRoundRect(rect, 26 * d, 26 * d, fill)
        val tw = width / ROOMS.size.toFloat()
        ROOMS.forEachIndexed { i, (emo, label) ->
            val x = tw * (i + 0.5f)
            if (i == room) { fill.color = 0xFFFFE3D6.toInt(); rect.set(x - tw / 2 + 8 * d, barTop + 8 * d, x + tw / 2 - 8 * d, barTop + 68 * d); c.drawRoundRect(rect, 18 * d, 18 * d, fill) }
            text.textSize = 26 * d; text.alpha = 255; c.drawText(emo, x, barTop + 38 * d, text)
            text.textSize = 12 * d; text.color = if (i == room) 0xFFD4533A.toInt() else 0xFF7A6B70.toInt(); text.isFakeBoldText = i == room
            c.drawText(label, x, barTop + 60 * d, text)
            text.color = 0xFF3A2A20.toInt(); text.isFakeBoldText = false
        }
    }

    // ---------- Dokunma ----------

    private fun onBaby(x: Float, y: Float) = abs(x - cx) < 22 * u && y > floorY - 46 * u && y < floorY + 4 * u

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.x; val y = e.y; val now = SystemClock.uptimeMillis()
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                petting = false
                if (y > barTop) { room = (x / (width / ROOMS.size.toFloat())).toInt().coerceIn(0, ROOMS.size - 1); foam.clear(); if (room != 3) ball = null; return true }
                if (hypot(x - width * 0.9f, y - hudY()) < 34 * d) { onSettings(); return true }
                if (needsSetup && abs(y - (topInset + 132 * d)) < 22 * d && abs(x - cx) < 160 * d) { onSettings(); return true }
                val items = trayItems()
                items.forEachIndexed { i, s -> if (hypot(x - trayX(i, items.size), y - trayY) < 34 * d) { tapItem(s, x, y); return true } }
                ball?.let { b -> if (hypot(x - b[0], y - b[1]) < 40 * d) { kickBall(b, x); return true } }
                if (onBaby(x, y)) { tapBaby(now); petting = pet.hatched && !pet.asleep; return true }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragItem != null) {
                    dragX = x; dragY = y
                    if (dragItem == "🧼" && onBaby(x, y) && foam.size < 40 && rnd.nextFloat() < 0.35f) {
                        foam.add(floatArrayOf((x - cx) / u + (rnd.nextFloat() - 0.5f) * 6, (y - floorY) / u + (rnd.nextFloat() - 0.5f) * 6, 2.2f + rnd.nextFloat() * 2.5f))
                        pet.bathe(0.8f)
                        if (foam.size % 10 == 0) talk("hihi!")
                    }
                } else if (petting && onBaby(x, y) && now - lastPetHeart > 250) {
                    lastPetHeart = now; tickleUntil = now + 700; pet.cheer(0.5f)
                    parts.add(Part(x, y, (rnd.nextFloat() - 0.5f) * 60 * d, -120 * d, 1f, 1f, "💗", 22 * d, 0f))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val item = dragItem
                if (item != null && room == 0) dropFood(item, x, y)
                if (item == "🧼") pet.save()
                if (petting) pet.save()
                dragItem = null; petting = false
            }
        }
        return true
    }

    private fun tapItem(s: String, x: Float, y: Float) {
        val now = SystemClock.uptimeMillis()
        when (s) {
            in FOODS, "🧼" -> { if (!pet.asleep) { dragItem = s; dragX = x; dragY = y } else talk("zzz") }
            "💡" -> {
                if (pet.energy > 90) talk(if (pet.stage >= 4) "uykum yok!" else "ı-ıh")
                else { pet.asleep = true; pet.save(); talk("iyi geceler…") }
            }
            "☀️" -> { pet.wake(); waveUntil = now + 2000; talk("günaydın!") }
            "🚿" -> {
                if (pet.asleep) { talk("zzz"); return }
                showerUntil = now + 2200
                pet.bathe(if (foam.isNotEmpty()) 25f else 6f); pet.save()
                talk(if (foam.isNotEmpty()) "✨ tertemiz!" else "brrr!")
                parts.add(Part(cx, floorY - 50 * u, 0f, -40 * d, 1.5f, 1.5f, "✨", 30 * d, 0f))
            }
            "⚽" -> { if (ball == null) ball = floatArrayOf(width * 0.25f, floorY - 80 * u, 300 * d, -500 * d); talk("top!") }
            "🎈" -> {
                parts.add(Part(x, y, (rnd.nextFloat() - 0.5f) * 40 * d, -160 * d, 4f, 4f, "🎈", 44 * d, 0f))
                pet.cheer(4f); pet.save(); tickleUntil = now + 1500; talk("vaay!")
            }
        }
    }

    private fun dropFood(item: String, x: Float, y: Float) {
        val now = SystemClock.uptimeMillis()
        val (mx, my) = mouth()
        if (hypot(x - mx, y - my) > 70 * d) return
        if (pet.eatBite(14f)) {
            eatUntil = now + 900
            talk("ham!")
            repeat(5) { parts.add(Part(mx + (rnd.nextFloat() - 0.5f) * 30 * d, my, (rnd.nextFloat() - 0.5f) * 160 * d, -80 * d, 0.7f, 0.7f, "•", 16 * d, 600 * d)) }
            if (pet.hunger < 8) parts.add(Part(mx, my - 40 * d, 0f, -60 * d, 1.4f, 1.4f, "💗", 28 * d, 0f))
        } else {
            refuseUntil = now + 1200
            talk(if (pet.stage >= 4) "tokum!" else "ı-ıh")
        }
    }

    private fun kickBall(b: FloatArray, x: Float) {
        b[2] = (b[0] - x) * 8 + (rnd.nextFloat() - 0.5f) * 300 * d
        b[3] = -900 * d
        pet.cheer(4f); pet.save()
        tickleUntil = SystemClock.uptimeMillis() + 900
        parts.add(Part(b[0], b[1], 0f, -80 * d, 0.8f, 0.8f, "⭐", 24 * d, 0f))
        if (rnd.nextFloat() < 0.4f) talk(if (pet.stage >= 4) "bir daha!" else "hihi!")
    }

    private fun tapBaby(now: Long) {
        if (!pet.hatched) {
            hatchTaps++
            talk("tık!")
            if (hatchTaps >= 4) {
                pet.hatch(); hatchTaps = 0; waveUntil = now + 2500; talk(if (pet.stage >= 4) "merhaba!" else "ba!")
                repeat(14) { parts.add(Part(cx, floorY - 20 * u, (rnd.nextFloat() - 0.5f) * 600 * d, -rnd.nextFloat() * 700 * d, 1.6f, 1.6f, listOf("✨", "🎉", "💗")[rnd.nextInt(3)], 24 * d, 900 * d)) }
            }
            return
        }
        if (pet.asleep) { talk("şşş… zzz"); return }
        tickleUntil = now + 1400
        pet.cheer(2f); pet.save()
        talk(listOf("hihi!", "kıkır!", "ahaha!")[rnd.nextInt(3)])
        repeat(3) { parts.add(Part(cx + (rnd.nextFloat() - 0.5f) * 30 * u, floorY - 40 * u, (rnd.nextFloat() - 0.5f) * 80 * d, -140 * d, 1.1f, 1.1f, "💗", 24 * d, 0f)) }
    }

    private fun talk(s: String) { say = s; sayUntil = SystemClock.uptimeMillis() + 1600 }
    private fun hello() = when (pet.stage) { 0, 1 -> "agu!"; 2, 3 -> "ba-ba!"; else -> "merhaba!" }
}
