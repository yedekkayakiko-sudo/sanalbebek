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
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Uygulamanın ana ekranı: Pou tarzı odalar. Ortada büyük bebek; mutfakta buzdolabındaki yemeği ağzına
 * sürükle (market ve reklam hediyesi), yatak odasında ışığı kapat, banyoda sabun, duş ve bez, revirde
 * ateş ölç ve ilaç ver, oyun odasında top, balon ve mini oyun. Bebek gerçek zamanla büyür; boyu ve
 * oranları Pet.scale / Pet.maturity ile değişir. Hepsi kodla çiziliyor; bağımlılık yok.
 */
@SuppressLint("ViewConstructor")
class RoomView(ctx: Context, var pet: Pet, private val onSettings: () -> Unit) : View(ctx) {
    var onChat: () -> Unit = {}
    var topInset = 0f
    var bottomInset = 0f
    var needsSetup = false

    private val d = resources.displayMetrics.density
    private var chibi = ChibiRenderer(pet.look)
    private val rnd = Random(SystemClock.uptimeMillis())
    private var room = 0
    private val ROOMS = listOf("🍽️" to "Mutfak", "🛏️" to "Yatak", "🛁" to "Banyo", "🩺" to "Revir", "⚽" to "Oyun")

    // Yerleşim
    private var barTop = 0f
    private var trayY = 0f
    private var floorY = 0f
    private var cx = 0f
    private var u = 4f
    private val k get() = u * pet.scale()               // bebeğin gerçek birimi (büyüdükçe artar)

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
    private val foam = ArrayList<FloatArray>()                 // köpük: bebeğe göre x, y, yarıçap (k biriminde)
    private var ball: FloatArray? = null                        // x, y, vx, vy
    private val parts = ArrayList<Part>()
    private var nextIdle = 0L

    // Açılır paneller
    private enum class Panel { NONE, MARKET, AD, CELEBRATE, GAME, GAME_OVER }
    private var panel = Panel.NONE
    private var panelText = ""
    private var adUntil = 0L
    private val buyRects = ArrayList<Pair<RectF, String>>()
    private val adRect = RectF()
    private val closeRect = RectF()
    private val okRect = RectF()

    // Mini oyun: yukarıdan düşen yiyecekleri yakala
    private var gameX = 0f
    private var gameScore = 0
    private var gameLives = 3
    private var gameUntil = 0L
    private var gameNext = 0L
    private val drops = ArrayList<FloatArray>()                // x, y, hız, tür (0 iyi, 1 taş)
    private val GOOD = listOf("🍎", "🍓", "🍪", "🍌", "⭐")

    private class Part(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val max: Float, val s: String, val size: Float, val grav: Float)

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val rect = RectF()
    private val path = Path()

    fun reloadPet(p: Pet) {
        pet = p
        if (chibi.look != p.look) chibi = ChibiRenderer(p.look)
        layoutRoom()
        val bonus = p.dailyBonus()
        val m = p.takeMilestone()
        when {
            m != null -> celebrate(m + if (bonus > 0) "\n\nGünlük hediye: +$bonus 🪙" else "")
            bonus > 0 -> celebrate("Günlük hediye!\n+$bonus 🪙")
        }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) = layoutRoom()

    private fun layoutRoom() {
        val w = width.toFloat(); val h = height.toFloat()
        if (w == 0f) return
        barTop = h - bottomInset - 72 * d
        trayY = barTop - 50 * d
        floorY = trayY - 64 * d
        cx = w / 2
        val avail = floorY - (topInset + 190 * d)
        // En büyük hali (boy çarpanı ~1.6) de ekrana sığsın; küçükken gerçekten küçük görünür.
        u = min(w / 70f, avail / 82f)
    }

    fun setInsets(top: Float, bottom: Float) { topInset = top; bottomInset = bottom; layoutRoom(); invalidate() }

    // ---------- Çizim ----------

    override fun onDraw(c: Canvas) {
        val now = SystemClock.uptimeMillis()
        val dt = if (lastT == 0L) 0f else min(0.05f, (now - lastT) / 1000f)
        lastT = now
        if (panel == Panel.GAME || panel == Panel.GAME_OVER) {
            updateGame(now, dt); drawGame(c, now)
        } else {
            update(now, dt)
            drawRoom(c)
            drawBaby(c, now)
            drawParts(c)
            if (room == 1 && pet.asleep) { fill.color = 0x99000018.toInt(); c.drawRect(0f, 0f, width.toFloat(), trayY - 40 * d, fill); drawMoon(c) }
            drawHud(c)
            drawChatButton(c, now)
            drawTray(c, now)
            drawBar(c)
            dragItem?.let { text.textSize = 44 * d; text.alpha = 255; c.drawText(it, dragX, dragY + 15 * d, text) }
            drawPanel(c, now)
        }
        postInvalidateOnAnimation()
    }

    private fun update(now: Long, dt: Float) {
        stepParts(dt)
        if (now < showerUntil) {
            repeat(2) { parts.add(Part(cx + (rnd.nextFloat() - 0.5f) * 40 * k, floorY - 60 * k, 0f, 500 * d, 0.9f, 0.9f, "💧", 16 * d, 0f)) }
            if (foam.isNotEmpty() && rnd.nextFloat() < 0.3f) foam.removeAt(0)
        }
        ball?.let { b ->
            b[3] += 1400 * d * dt; b[0] += b[2] * dt; b[1] += b[3] * dt
            val r = 22 * d
            if (b[1] > floorY - r) { b[1] = floorY - r; b[3] = -abs(b[3]) * 0.62f; b[2] *= 0.9f; if (abs(b[3]) < 60 * d) b[3] = 0f }
            if (b[0] < r) { b[0] = r; b[2] = abs(b[2]) }
            if (b[0] > width - r) { b[0] = width - r; b[2] = -abs(b[2]) }
        }
        if (panel == Panel.AD && now >= adUntil) {
            val gift = listOf("🍼", "🍓", "🍪", "🥛")[rnd.nextInt(4)]
            pet.gift(gift)
            celebrate("Teşekkürler!\nHediye: 1 $gift\nBuzdolabına kondu.")
        }
        if (now > nextIdle) {
            nextIdle = now + 5000 + rnd.nextLong(5000)
            if (pet.hatched && !pet.asleep && dragItem == null && panel == Panel.NONE) {
                when {
                    pet.sick -> talk(if (pet.stage >= 4) "🤒 başım ağrıyor" else "🤒 ıhh")
                    pet.diaper > 70 -> talk(if (pet.stage >= 4) "tuvalete gitmem lazım!" else "💩 ıı!")
                    pet.hunger > 70 -> talk(if (pet.stage >= 4) "acıktım!" else "mama?")
                    pet.clean < 30 -> talk(if (pet.stage >= 4) "banyo?" else "ııh")
                    pet.joy < 30 -> talk(if (pet.stage >= 4) "oynayalım mı?" else "ba?")
                    pet.energy < 25 -> talk("hıı…")
                    rnd.nextFloat() < 0.5f -> { waveUntil = now + 1500; talk(hello()) }
                }
            }
        }
    }

    private fun stepParts(dt: Float) {
        val it = parts.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.vy += p.grav * dt; p.x += p.vx * dt; p.y += p.vy * dt; p.life -= dt
            if (p.life <= 0f) it.remove()
        }
    }

    private fun drawRoom(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val wallBottom = floorY - 8 * u
        val (wall, wall2, floor) = when (room) {
            0 -> Triple(0xFFFFE9D2.toInt(), 0xFFFFDDBB.toInt(), 0xFFD9A77A.toInt())
            1 -> Triple(0xFFB9B3EC.toInt(), 0xFF9C95DB.toInt(), 0xFF8C6E9E.toInt())
            2 -> Triple(0xFFD3F1F6.toInt(), 0xFFB6E4EE.toInt(), 0xFF8FC7D3.toInt())
            3 -> Triple(0xFFF4FBF6.toInt(), 0xFFDDF2E4.toInt(), 0xFFA9D6BA.toInt())
            else -> Triple(0xFFD9F2CB.toInt(), 0xFFBFE6AA.toInt(), 0xFF86BF62.toInt())
        }
        fill.shader = LinearGradient(0f, 0f, 0f, wallBottom, wall, wall2, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, wallBottom, fill); fill.shader = null
        fill.color = 0x14FFFFFF
        when (room) {
            0, 2, 3 -> { val s = 34 * d; var y = topInset + 170 * d; while (y < wallBottom) { var x = if (((y / s).toInt()) % 2 == 0) 0f else s / 2; while (x < w) { rect.set(x + 2, y + 2, x + s - 2, y + s - 2); c.drawRoundRect(rect, 5 * d, 5 * d, fill); x += s }; y += s } }
            else -> { var x = 0f; while (x < w) { c.drawRect(x, 0f, x + 18 * d, wallBottom, fill); x += 36 * d } }
        }
        fill.color = floor; c.drawRect(0f, wallBottom, w, h, fill)
        fill.color = 0x22000000; c.drawRect(0f, wallBottom, w, wallBottom + 5 * d, fill)
        stroke.color = 0x18000000; stroke.strokeWidth = 2 * d
        var fy = wallBottom + 22 * d; while (fy < h) { c.drawLine(0f, fy, w, fy, stroke); fy += 26 * d }
        val deco = topInset + 200 * d
        text.alpha = 255
        when (room) {
            0 -> {
                fill.color = 0xFFB9855A.toInt(); rect.set(w * 0.06f, deco + 32 * d, w * 0.4f, deco + 42 * d); c.drawRoundRect(rect, 4 * d, 4 * d, fill)
                text.textSize = 28 * d
                c.drawText("🫙", w * 0.13f, deco + 28 * d, text); c.drawText("🧂", w * 0.23f, deco + 28 * d, text); c.drawText("🍯", w * 0.33f, deco + 28 * d, text)
                text.textSize = 64 * d; c.drawText("🧊", w * 0.86f, floorY - 4 * d, text)
            }
            1 -> {
                window(c, w * 0.08f, deco, w * 0.3f, 90 * d)
                if (pet.stage == 0) { // beşik
                    fill.color = 0xFFE9C9A5.toInt(); rect.set(cx - 30 * u, floorY - 16 * u, cx + 30 * u, floorY + 2 * u); c.drawRoundRect(rect, 6 * u, 6 * u, fill)
                    stroke.color = 0xFFC9A17A.toInt(); stroke.strokeWidth = 1.2f * u
                    var bx = cx - 26 * u; while (bx < cx + 27 * u) { c.drawLine(bx, floorY - 15 * u, bx, floorY - 4 * u, stroke); bx += 4 * u }
                } else {
                    fill.color = 0xFF6C5A9E.toInt(); rect.set(cx - 30 * u, floorY - 14 * u, cx + 30 * u, floorY + 2 * u); c.drawRoundRect(rect, 5 * u, 5 * u, fill)
                    fill.color = 0xFFF7F3FF.toInt(); rect.set(cx - 26 * u, floorY - 13 * u, cx + 26 * u, floorY - 5 * u); c.drawRoundRect(rect, 4 * u, 4 * u, fill)
                }
                fill.color = 0xFFFFFFFF.toInt(); rect.set(cx - 25 * u, floorY - 18 * u, cx - 11 * u, floorY - 10 * u); c.drawRoundRect(rect, 4 * u, 4 * u, fill)
            }
            2 -> {
                fill.color = 0xFFFFFFFF.toInt(); rect.set(cx - 28 * u, floorY - 12 * u, cx + 28 * u, floorY + 3 * u); c.drawRoundRect(rect, 7 * u, 7 * u, fill)
                fill.color = 0xFF9ADCEB.toInt(); rect.set(cx - 25 * u, floorY - 10 * u, cx + 25 * u, floorY - 6 * u); c.drawRoundRect(rect, 3 * u, 3 * u, fill)
                text.textSize = 34 * d; c.drawText("🦆", w * 0.84f, floorY - 2 * d, text)
                if (pet.stage >= 4 && !pet.isPet) { text.textSize = 56 * d; c.drawText("🚽", w * 0.14f, floorY + 4 * d, text) }
            }
            3 -> {
                fill.color = 0xFFE8574A.toInt(); val px = w * 0.84f; val py = deco + 40 * d
                c.drawRect(px - 16 * d, py - 5 * d, px + 16 * d, py + 5 * d, fill); c.drawRect(px - 5 * d, py - 16 * d, px + 5 * d, py + 16 * d, fill)
                text.textSize = 40 * d; c.drawText("🧸", w * 0.12f, floorY + 4 * d, text)
                text.textSize = 30 * d; c.drawText("🩹", w * 0.15f, deco + 50 * d, text)
            }
            else -> {
                window(c, w * 0.6f, deco, w * 0.32f, 86 * d)
                text.textSize = 38 * d; c.drawText("🧸", w * 0.14f, floorY + 6 * d, text)
            }
        }
        // Bezi / tuvaleti gelmişse yerde minik bir işaret
        if (pet.hatched && pet.diaper > 60 && room != 2) { text.textSize = 30 * d; c.drawText("💩", cx + 30 * k, floorY + 2 * d, text) }
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
        c.drawText("🌙", width * 0.23f, topInset + 250 * d, text)
    }

    // Bebeğin ölçüleri (ChibiRenderer ile aynı formüller)
    private fun headR() = 12.5f * k * (1 - 0.12f * pet.maturity())
    private fun legLen() = 4.5f * k * (1 + 0.8f * pet.maturity())
    private fun bodyH() = 8.5f * k * (1 + 0.35f * pet.maturity())
    private fun basePose(): Pose = when {
        !pet.hatched -> Pose.EGG
        pet.asleep || pet.stage == 0 -> Pose.LIE
        pet.stage <= 2 -> Pose.SIT
        else -> Pose.STAND
    }
    private fun babyX() = if (basePose() == Pose.LIE) cx + 4 * k else cx
    private fun babyY() = if (room == 1 && basePose() == Pose.LIE) floorY - 11 * u else floorY

    private fun mouth(): Pair<Float, Float> {
        val r = headR(); val y = babyY(); val x = babyX()
        return when (basePose()) {
            Pose.LIE -> Pair(x - 11 * k, y - 10 * k + 0.64f * r)
            Pose.SIT -> Pair(x, y - 15 * k - 0.18f * r)
            Pose.EGG -> Pair(x, y - 18 * k + 0.64f * r)
            else -> Pair(x, y - legLen() - bodyH() - 0.18f * r)
        }
    }

    private fun babyTop() = babyY() - when (basePose()) { Pose.LIE -> 26 * k; Pose.SIT -> 42 * k; Pose.EGG -> 34 * k; else -> legLen() + bodyH() + headR() * 2.4f }

    private fun drawBaby(c: Canvas, now: Long) {
        val st = pet.stage
        val (mx, my) = mouth()
        val nearMouth = dragItem != null && dragItem != "🧼" && hypot(dragX - mx, dragY - my) < 70 * d
        val mood = when {
            pet.asleep -> Mood.SLEEP
            now < tickleUntil || now < showerUntil -> Mood.LAUGH
            now < refuseUntil -> Mood.SHY
            nearMouth -> Mood.SURPRISE
            pet.sick -> Mood.SLEEPY
            pet.hunger > 70 -> Mood.HUNGRY
            pet.energy < 25 -> Mood.SLEEPY
            else -> Mood.HAPPY
        }
        val target = when { dragItem != null -> Pair(dragX, dragY); ball != null -> Pair(ball!![0], ball!![1]); else -> null }
        var lookX = if (now < refuseUntil) 1f else 0f; var lookY = 0f
        if (target != null && !pet.asleep) {
            val dx = target.first - mx; val dy = target.second - my
            val l = hypot(dx, dy).coerceAtLeast(1f)
            lookX = (dx / l).coerceIn(-1f, 1f); lookY = (dy / l).coerceIn(-1f, 1f)
        }
        val pose = if (now < eatUntil && basePose() != Pose.LIE && basePose() != Pose.EGG) Pose.EAT else basePose()
        val arms = when {
            now < waveUntil -> Arms.WAVE
            now < showerUntil || (ball != null && abs(ball!![3]) > 0f) -> Arms.UP
            else -> Arms.NONE
        }
        val hatch = if (pet.hatched) 1f else (hatchTaps / 4f).coerceAtMost(0.95f)
        val bob = if (pose == Pose.STAND || pose == Pose.SIT) sin(now / 600.0).toFloat() * 0.6f * u else 0f
        val p = chibi.draw(c, Frame(babyX(), babyY() + bob, u, st, pose, mood, arms, lookX = lookX, lookY = lookY, hatch = hatch, scale = pet.scale(), mature = pet.maturity()), now)

        fill.color = 0xF0FFFFFF.toInt(); stroke.color = 0x66B6E4EE; stroke.strokeWidth = 1.2f * d
        for (f in foam) { c.drawCircle(cx + f[0] * k, floorY + f[1] * k, f[2] * k, fill); c.drawCircle(cx + f[0] * k, floorY + f[1] * k, f[2] * k, stroke) }
        if (pet.hatched && (pet.clean < 35 || pet.diaper > 70) && foam.isEmpty()) {
            stroke.color = 0x9977A85A.toInt(); stroke.strokeWidth = 2.5f * d
            for (j in 0..2) {
                val sx = cx + (j - 1) * 14 * k; val base = babyTop() - 4 * d
                path.reset(); path.moveTo(sx, base)
                for (i in 1..6) path.lineTo(sx + sin(now / 300.0 + i + j).toFloat() * 3 * d, base - i * 5 * d)
                c.drawPath(path, stroke)
            }
        }
        if (pet.sick && !pet.asleep) { text.textSize = 26 * d; text.alpha = 255; c.drawText("🤒", p.x + 18 * k, p.y + 8 * k, text) }
        val s = say
        if (s != null && now < sayUntil) bubble(c, p.x, p.y, s) else say = null
        if (!pet.hatched) bubble(c, cx, babyTop() - 6 * d, "dokun! 🥚")
    }

    private fun bubble(c: Canvas, px: Float, py: Float, s: String) {
        text.textSize = 20 * d; text.alpha = 255; text.color = 0xFF3A2A20.toInt(); text.isFakeBoldText = true
        val tw = text.measureText(s); val pad = 14 * d; val bh = 42 * d
        val left = (px - tw / 2 - pad).coerceIn(8 * d, width - tw - 2 * pad - 8 * d)
        val bottom = py.coerceAtLeast(topInset + 190 * d + bh)
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

    // ---------- Üst bilgi ----------

    private fun row1Y() = topInset + 30 * d
    private fun gaugeY() = topInset + 94 * d
    private fun gaugeX(i: Int) = width * (0.14f + i * 0.24f)

    private fun drawHud(c: Canvas) {
        text.color = 0xFF3A2A20.toInt(); text.alpha = 255
        // altın
        val coins = "🪙 ${pet.coins}"
        text.textSize = 16 * d; text.isFakeBoldText = true
        val cw = text.measureText(coins)
        fill.color = 0xF2FFFFFF.toInt(); rect.set(12 * d, row1Y() - 18 * d, 12 * d + cw + 24 * d, row1Y() + 18 * d); c.drawRoundRect(rect, 18 * d, 18 * d, fill)
        c.drawText(coins, 24 * d + cw / 2, row1Y() + 6 * d, text)
        // isim ve yaş
        val days = pet.ageDays().toInt()
        val age = if (days < 60) "$days günlük" else "${(pet.ageDays() / 30.44f).toInt()} aylık"
        val name = "${pet.name} · $age"
        val nw = text.measureText(name)
        fill.color = 0xF2FFFFFF.toInt(); rect.set(cx - nw / 2 - 14 * d, row1Y() - 18 * d, cx + nw / 2 + 14 * d, row1Y() + 18 * d); c.drawRoundRect(rect, 18 * d, 18 * d, fill)
        c.drawText(name, cx, row1Y() + 6 * d, text)
        text.isFakeBoldText = false
        // ayarlar
        val gx = width - 30 * d
        fill.color = 0xF2FFFFFF.toInt(); c.drawCircle(gx, row1Y(), 20 * d, fill)
        text.textSize = 20 * d; c.drawText("⚙️", gx, row1Y() + 7 * d, text)
        // göstergeler
        val stats = listOf(Triple("🍗", 100 - pet.hunger, "Tokluk"), Triple("⚡", pet.energy, "Enerji"), Triple("😊", pet.joy, "Neşe"), Triple("🧼", min(pet.clean, 100 - pet.diaper), "Temizlik"))
        val r = 23 * d; val y = gaugeY()
        stats.forEachIndexed { i, (emo, v, label) ->
            val x = gaugeX(i)
            fill.color = 0xF2FFFFFF.toInt(); c.drawCircle(x, y, r + 5 * d, fill)
            stroke.strokeWidth = 5 * d; stroke.color = 0xFFEDE4DA.toInt(); c.drawCircle(x, y, r, stroke)
            stroke.color = when { v > 60 -> 0xFF4CB86A.toInt(); v > 30 -> 0xFFF2B33D.toInt(); else -> 0xFFE8574A.toInt() }
            rect.set(x - r, y - r, x + r, y + r); c.drawArc(rect, -90f, 360f * (v / 100f), false, stroke)
            text.textSize = 20 * d; c.drawText(emo, x, y + 7 * d, text)
            text.textSize = 11 * d; c.drawText(label, x, y + r + 18 * d, text)
        }
        // büyüme çubuğu
        val st = pet.stage
        val by = gaugeY() + 58 * d
        val label: String; val frac: Float
        if (st < 5) {
            val a = Pet.STAGE_DAYS[st]; val b = Pet.STAGE_DAYS[st + 1]
            frac = ((pet.growth - a) / (b - a)).coerceIn(0f, 1f)
            val left = ceil(b - pet.growth).toInt().coerceAtLeast(1)
            label = "${pet.stageName(st)} → ${pet.stageName(st + 1)} · ~$left gün"
        } else { frac = 1f; label = "${pet.stageName(5)} · her ay biraz daha büyüyor" }
        val bw = width * 0.62f
        fill.color = 0xAAFFFFFF.toInt(); rect.set(cx - bw / 2, by - 5 * d, cx + bw / 2, by + 5 * d); c.drawRoundRect(rect, 5 * d, 5 * d, fill)
        fill.color = 0xFF9B7BE0.toInt(); rect.set(cx - bw / 2, by - 5 * d, cx - bw / 2 + bw * frac, by + 5 * d); c.drawRoundRect(rect, 5 * d, 5 * d, fill)
        text.textSize = 11 * d; c.drawText(label, cx, by + 20 * d, text)
    }

    // Konuş düğmesi: sahnenin sağ üstünde
    private fun chatX() = width - 42 * d
    private fun chatY() = topInset + 205 * d

    private fun drawChatButton(c: Canvas, now: Long) {
        val pulse = 1f + 0.05f * sin(now / 400.0).toFloat()
        fill.color = 0xFFEC6E4C.toInt(); c.drawCircle(chatX(), chatY(), 27 * d * pulse, fill)
        text.alpha = 255; text.textSize = 24 * d; c.drawText("💬", chatX(), chatY() + 8 * d, text)
        text.textSize = 12 * d; text.color = 0xFF3A2A20.toInt(); text.isFakeBoldText = true
        c.drawText("Konuş", chatX(), chatY() + 46 * d, text); text.isFakeBoldText = false
    }

    // ---------- Odanın eşyaları ----------

    private fun trayItems(): List<String> = when (room) {
        0 -> pet.inventory.filter { it.value > 0 }.keys.toList().take(6) + "🛒"
        1 -> listOf(if (pet.asleep) "☀️" else "💡")
        2 -> listOf("🧼", "🚿", when { pet.isPet -> "🧻"; pet.stage >= 4 -> "🚽"; else -> "🧷" })
        3 -> listOf("🌡️", "💊")
        else -> listOf("⚽", "🎈", "🎮")
    }

    private fun trayX(i: Int, n: Int): Float {
        val span = if (n <= 3) width * 0.6f else width - 40 * d
        return (width - span) / 2 + span * (i + 0.5f) / n
    }

    private fun drawTray(c: Canvas, now: Long) {
        val items = trayItems()
        fill.color = 0x66FFFFFF; rect.set(10 * d, trayY - 36 * d, width - 10 * d, trayY + 36 * d); c.drawRoundRect(rect, 26 * d, 26 * d, fill)
        val r = if (items.size > 5) 24 * d else 29 * d
        items.forEachIndexed { i, s ->
            val x = trayX(i, items.size)
            fill.color = if (s == "🛒" || s == "🎮") 0xFFFFE3D6.toInt() else 0xFFFFFFFF.toInt(); c.drawCircle(x, trayY, r, fill)
            text.textSize = r * 1.1f; text.alpha = if (dragItem == s) 90 else 255; c.drawText(s, x, trayY + r * 0.38f, text)
            val n = pet.inventory[s]
            if (room == 0 && n != null) {
                fill.color = 0xFFEC6E4C.toInt(); c.drawCircle(x + r * 0.75f, trayY - r * 0.75f, 10 * d, fill)
                text.color = 0xFFFFFFFF.toInt(); text.textSize = 11 * d; text.isFakeBoldText = true
                c.drawText("$n", x + r * 0.75f, trayY - r * 0.75f + 4 * d, text); text.color = 0xFF3A2A20.toInt(); text.isFakeBoldText = false
            }
        }
        text.alpha = 255
        val hint = when (room) {
            0 -> if (pet.stage <= 1) (if (pet.isPet) "Yavru şimdilik sadece süt içiyor 🍼" else "Bebek şimdilik sadece süt içiyor 🍼") else "Yemeği sürükleyip ağzına götür · 🛒 market"
            1 -> if (pet.asleep) "Uyandırmak için güneşe dokun" else "Uyutmak için ışığı kapat"
            2 -> when { pet.isPet -> "Sabun, duş · 🧻 çişini temizle"; pet.stage >= 4 -> "Sabun, duş · 🚽 tuvalete götür"; else -> "Sabun, duş · 🧷 bezini değiştir" }
            3 -> "Termometreyi ve ilacı ağzına götür"
            else -> "Topa dokun · balon uçur · 🎮 oyna, altın kazan"
        }
        text.textSize = 13 * d; text.color = 0xFF3A2A20.toInt(); c.drawText(hint, cx, trayY - 44 * d, text)
        if (needsSetup) {
            val y = floorY + 30 * d
            val pulse = 0.9f + 0.1f * sin(now / 300.0).toFloat()
            fill.color = 0xFFEC6E4C.toInt(); rect.set(cx - 140 * d * pulse, y - 16 * d, cx + 140 * d * pulse, y + 16 * d); c.drawRoundRect(rect, 16 * d, 16 * d, fill)
            text.color = 0xFFFFFFFF.toInt(); text.textSize = 13 * d; text.isFakeBoldText = true
            c.drawText("📱 Bebeği telefon ekranına koy  →", cx, y + 5 * d, text)
            text.isFakeBoldText = false; text.color = 0xFF3A2A20.toInt()
        }
    }

    private fun drawBar(c: Canvas) {
        fill.color = 0xFFFFFFFF.toInt(); rect.set(0f, barTop, width.toFloat(), height.toFloat() + 30 * d); c.drawRoundRect(rect, 26 * d, 26 * d, fill)
        val tw = width / ROOMS.size.toFloat()
        ROOMS.forEachIndexed { i, (emo, label) ->
            val x = tw * (i + 0.5f)
            if (i == room) { fill.color = 0xFFFFE3D6.toInt(); rect.set(x - tw / 2 + 5 * d, barTop + 7 * d, x + tw / 2 - 5 * d, barTop + 64 * d); c.drawRoundRect(rect, 18 * d, 18 * d, fill) }
            text.textSize = 24 * d; text.alpha = 255; c.drawText(emo, x, barTop + 35 * d, text)
            text.textSize = 12 * d; text.color = if (i == room) 0xFFD4533A.toInt() else 0xFF7A6B70.toInt(); text.isFakeBoldText = i == room
            c.drawText(label, x, barTop + 56 * d, text)
            text.color = 0xFF3A2A20.toInt(); text.isFakeBoldText = false
        }
        // Dikkat isteyen odalarda kırmızı nokta
        val alert = listOf(pet.hunger > 70, pet.energy < 25 && !pet.asleep, pet.clean < 30 || pet.diaper > 70, pet.sick, pet.joy < 30)
        alert.forEachIndexed { i, on -> if (on) { fill.color = 0xFFE8574A.toInt(); c.drawCircle(tw * (i + 0.5f) + 16 * d, barTop + 14 * d, 5 * d, fill) } }
    }

    // ---------- Paneller ----------

    private fun celebrate(t: String) {
        panel = Panel.CELEBRATE; panelText = t
        repeat(24) { parts.add(Part(width / 2f, height * 0.4f, (rnd.nextFloat() - 0.5f) * 900 * d, -rnd.nextFloat() * 900 * d, 2f, 2f, listOf("✨", "🎉", "💗", "⭐")[rnd.nextInt(4)], 24 * d, 900 * d)) }
    }

    private fun card(c: Canvas, top: Float, bottom: Float) {
        fill.color = 0x88000000.toInt(); c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fill)
        fill.color = 0xFFFFFBF6.toInt(); rect.set(20 * d, top, width - 20 * d, bottom); c.drawRoundRect(rect, 28 * d, 28 * d, fill)
    }

    private fun button(c: Canvas, r: RectF, label: String, color: Int, textColor: Int) {
        fill.color = color; c.drawRoundRect(r, 18 * d, 18 * d, fill)
        text.color = textColor; text.textSize = 16 * d; text.isFakeBoldText = true
        c.drawText(label, r.centerX(), r.centerY() + 6 * d, text)
        text.color = 0xFF3A2A20.toInt(); text.isFakeBoldText = false
    }

    private fun multiline(c: Canvas, s: String, x: Float, y: Float, size: Float) {
        text.textSize = size; text.color = 0xFF3A2A20.toInt(); text.isFakeBoldText = true
        s.split("\n").forEachIndexed { i, line -> c.drawText(line, x, y + i * size * 1.35f, text) }
        text.isFakeBoldText = false
    }

    private fun drawPanel(c: Canvas, now: Long) {
        when (panel) {
            Panel.NONE, Panel.GAME, Panel.GAME_OVER -> return
            Panel.CELEBRATE -> {
                val top = height * 0.3f; val bottom = height * 0.62f
                card(c, top, bottom)
                multiline(c, panelText, cx, top + 60 * d, 22 * d)
                okRect.set(cx - 90 * d, bottom - 70 * d, cx + 90 * d, bottom - 22 * d)
                button(c, okRect, "Yaşasın!", 0xFFEC6E4C.toInt(), 0xFFFFFFFF.toInt())
                drawParts(c)
            }
            Panel.AD -> {
                fill.color = 0xFF111111.toInt(); c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fill)
                val left = ceil((adUntil - now) / 1000f).toInt().coerceAtLeast(0)
                text.color = 0xFFFFFFFF.toInt(); text.textSize = 20 * d; text.isFakeBoldText = true
                c.drawText("Reklam (deneme)", cx, height * 0.4f, text)
                text.textSize = 15 * d; text.isFakeBoldText = false
                c.drawText("Gerçek reklam sonra eklenecek · $left sn", cx, height * 0.4f + 32 * d, text)
                text.textSize = 60 * d; c.drawText("📺", cx, height * 0.3f, text)
                text.color = 0xFF3A2A20.toInt()
            }
            Panel.MARKET -> {
                val top = topInset + 60 * d; val bottom = height - bottomInset - 40 * d
                card(c, top, bottom)
                multiline(c, "🛒 Market", cx, top + 42 * d, 22 * d)
                text.textSize = 15 * d; c.drawText("Altının: 🪙 ${pet.coins}", cx, top + 70 * d, text)
                closeRect.set(width - 70 * d, top + 12 * d, width - 30 * d, top + 52 * d)
                text.textSize = 24 * d; c.drawText("✕", closeRect.centerX(), closeRect.centerY() + 8 * d, text)
                buyRects.clear()
                val foods = Pet.FOODS.entries.toList()
                val cols = 3; val cellW = (width - 60 * d) / cols; val cellH = 104 * d
                foods.forEachIndexed { i, (emo, f) ->
                    val x0 = 30 * d + (i % cols) * cellW; val y0 = top + 90 * d + (i / cols) * cellH
                    val r = RectF(x0 + 6 * d, y0, x0 + cellW - 6 * d, y0 + cellH - 10 * d)
                    val babyOk = pet.stage > 1 || f.baby
                    fill.color = if (babyOk) 0xFFFFFFFF.toInt() else 0xFFF1E9E1.toInt(); c.drawRoundRect(r, 16 * d, 16 * d, fill)
                    text.textSize = 34 * d; text.alpha = if (babyOk) 255 else 110; c.drawText(emo, r.centerX(), y0 + 42 * d, text); text.alpha = 255
                    text.textSize = 13 * d; text.isFakeBoldText = true
                    c.drawText("🪙 ${f.price}", r.centerX(), y0 + 66 * d, text); text.isFakeBoldText = false
                    text.textSize = 11 * d; c.drawText("sende: ${pet.inventory[emo] ?: 0}", r.centerX(), y0 + 84 * d, text)
                    buyRects.add(r to emo)
                }
                val rows = ceil(foods.size / cols.toFloat()).toInt()
                val ay = top + 90 * d + rows * cellH + 10 * d
                adRect.set(40 * d, ay, width - 40 * d, ay + 54 * d)
                button(c, adRect, "📺 Reklam izle → bedava yemek", 0xFF4CB86A.toInt(), 0xFFFFFFFF.toInt())
                if (pet.stage <= 1) { text.textSize = 12 * d; c.drawText("Soluk olanları bebek henüz yiyemiyor", cx, ay + 76 * d, text) }
            }
        }
    }

    // ---------- Mini oyun ----------

    private fun startGame() {
        panel = Panel.GAME; gameScore = 0; gameLives = 3; drops.clear()
        gameUntil = SystemClock.uptimeMillis() + 30_000; gameNext = 0L; gameX = width / 2f
    }

    private fun updateGame(now: Long, dt: Float) {
        stepParts(dt)
        if (panel != Panel.GAME) return
        if (now > gameUntil || gameLives <= 0) {
            panel = Panel.GAME_OVER
            val won = gameScore
            if (won > 0) pet.earn(won)
            pet.cheer(10f); pet.energy = max(0f, pet.energy - 5f); pet.save()
            return
        }
        if (now > gameNext) {
            gameNext = now + (550 - min(300L, (30_000 - (gameUntil - now)) / 100)).coerceAtLeast(250)
            val bad = rnd.nextFloat() < 0.22f
            drops.add(floatArrayOf(30 * d + rnd.nextFloat() * (width - 60 * d), topInset + 60 * d, (260 + rnd.nextFloat() * 200) * d, if (bad) 1f else 0f, rnd.nextInt(GOOD.size).toFloat()))
        }
        val catchY = height - bottomInset - 110 * d
        val it = drops.iterator()
        while (it.hasNext()) {
            val o = it.next()
            o[1] += o[2] * dt
            if (abs(o[1] - catchY) < 30 * d && abs(o[0] - gameX) < 46 * d) {
                if (o[3] == 1f) { gameLives--; parts.add(Part(o[0], o[1], 0f, -100 * d, 0.8f, 0.8f, "💥", 30 * d, 0f)) }
                else { gameScore++; parts.add(Part(o[0], o[1], 0f, -120 * d, 0.7f, 0.7f, "+1", 22 * d, 0f)) }
                it.remove()
            } else if (o[1] > height) it.remove()
        }
    }

    private fun drawGame(c: Canvas, now: Long) {
        fill.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), 0xFF7EC3F0.toInt(), 0xFFD9F2CB.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fill); fill.shader = null
        fill.color = 0xFF86BF62.toInt(); c.drawRect(0f, height - bottomInset - 80 * d, width.toFloat(), height.toFloat(), fill)
        val catchY = height - bottomInset - 80 * d
        chibi.draw(c, Frame(gameX, catchY, u * 0.8f, pet.stage, if (pet.stage <= 2) Pose.SIT else Pose.STAND, Mood.LAUGH, Arms.UP, moving = true, scale = pet.scale(), mature = pet.maturity()), now)
        for (o in drops) { text.textSize = 34 * d; text.alpha = 255; c.drawText(if (o[3] == 1f) "🪨" else GOOD[o[4].toInt()], o[0], o[1] + 12 * d, text) }
        drawParts(c)
        text.color = 0xFF3A2A20.toInt(); text.isFakeBoldText = true; text.textSize = 20 * d
        val left = ceil((gameUntil - now) / 1000f).toInt().coerceAtLeast(0)
        c.drawText("🪙 $gameScore   ${"❤️".repeat(gameLives.coerceAtLeast(0))}   ⏱ $left", cx, topInset + 36 * d, text)
        text.isFakeBoldText = false; text.textSize = 13 * d
        c.drawText("Parmağınla sağa sola götür · yiyecekleri yakala, taşlardan kaç", cx, topInset + 62 * d, text)
        if (panel == Panel.GAME_OVER) {
            val top = height * 0.32f; val bottom = height * 0.6f
            card(c, top, bottom)
            multiline(c, "Oyun bitti!\n+$gameScore 🪙 kazandın", cx, top + 60 * d, 22 * d)
            okRect.set(cx - 90 * d, bottom - 70 * d, cx + 90 * d, bottom - 22 * d)
            button(c, okRect, "Tamam", 0xFFEC6E4C.toInt(), 0xFFFFFFFF.toInt())
        }
    }

    // ---------- Dokunma ----------

    private fun onBaby(x: Float, y: Float) = abs(x - cx) < 26 * k && y > babyTop() && y < floorY + 6 * d

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.x; val y = e.y; val now = SystemClock.uptimeMillis()
        // Paneller önce
        when (panel) {
            Panel.GAME -> { gameX = x.coerceIn(30 * d, width - 30 * d); return true }
            Panel.GAME_OVER, Panel.CELEBRATE -> { if (e.actionMasked == MotionEvent.ACTION_UP && okRect.contains(x, y)) { panel = Panel.NONE; parts.clear() }; return true }
            Panel.AD -> return true
            Panel.MARKET -> {
                if (e.actionMasked != MotionEvent.ACTION_UP) return true
                if (closeRect.contains(x, y)) { panel = Panel.NONE; return true }
                if (adRect.contains(x, y)) { panel = Panel.AD; adUntil = now + 5000; return true }
                for ((r, emo) in buyRects) if (r.contains(x, y)) {
                    if (pet.buy(emo)) parts.add(Part(x, y, 0f, -120 * d, 0.8f, 0.8f, "+1 $emo", 20 * d, 0f))
                    else parts.add(Part(x, y, 0f, -80 * d, 1f, 1f, "altın yetmedi", 16 * d, 0f))
                    invalidate(); return true
                }
                return true
            }
            Panel.NONE -> {}
        }
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                petting = false
                if (y > barTop) { room = (x / (width / ROOMS.size.toFloat())).toInt().coerceIn(0, ROOMS.size - 1); foam.clear(); if (room != 4) ball = null; return true }
                if (hypot(x - (width - 30 * d), y - row1Y()) < 28 * d) { onSettings(); return true }
                if (hypot(x - chatX(), y - chatY()) < 34 * d) { onChat(); return true }
                if (needsSetup && abs(y - (floorY + 30 * d)) < 20 * d && abs(x - cx) < 150 * d) { onSettings(); return true }
                val items = trayItems()
                items.forEachIndexed { i, s -> if (hypot(x - trayX(i, items.size), y - trayY) < 32 * d) { tapItem(s, x, y); return true } }
                ball?.let { b -> if (hypot(x - b[0], y - b[1]) < 40 * d) { kickBall(b, x); return true } }
                if (onBaby(x, y)) { tapBaby(now); petting = pet.hatched && !pet.asleep; return true }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragItem != null) {
                    dragX = x; dragY = y
                    if (dragItem == "🧼" && onBaby(x, y) && foam.size < 40 && rnd.nextFloat() < 0.35f) {
                        foam.add(floatArrayOf((x - cx) / k + (rnd.nextFloat() - 0.5f) * 6, (y - floorY) / k + (rnd.nextFloat() - 0.5f) * 6, 2.2f + rnd.nextFloat() * 2.5f))
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
                if (item != null) dropItem(item, x, y)
                if (item == "🧼" || petting) pet.save()
                dragItem = null; petting = false
            }
        }
        return true
    }

    private fun tapItem(s: String, x: Float, y: Float) {
        val now = SystemClock.uptimeMillis()
        when {
            s == "🛒" -> panel = Panel.MARKET
            s == "🎮" -> { if (pet.asleep) talk("zzz") else if (pet.energy < 15) talk("çok yorgunum…") else startGame() }
            s in Pet.FOODS || s == "🧼" || s == "🌡️" || s == "💊" -> { if (!pet.asleep) { dragItem = s; dragX = x; dragY = y } else talk("zzz") }
            s == "💡" -> {
                if (pet.energy > 90) talk(if (pet.stage >= 4) "uykum yok!" else "ı-ıh")
                else { pet.asleep = true; pet.save(); talk("iyi geceler…") }
            }
            s == "☀️" -> { pet.wake(); waveUntil = now + 2000; talk("günaydın!") }
            s == "🚿" -> {
                if (pet.asleep) { talk("zzz"); return }
                showerUntil = now + 2200
                pet.bathe(if (foam.isNotEmpty()) 25f else 6f); pet.save()
                talk(if (foam.isNotEmpty()) "✨ tertemiz!" else "brrr!")
                parts.add(Part(cx, floorY - 50 * k, 0f, -40 * d, 1.5f, 1.5f, "✨", 30 * d, 0f))
            }
            s == "🧷" || s == "🚽" || s == "🧻" -> {
                if (pet.diaper < 20) { talk(if (pet.stage >= 4) "gelmedi ki!" else "ı-ıh"); return }
                pet.changeDiaper(); waveUntil = now + 1500
                talk(if (s == "🚽") "oh be! 🚽" else "✨ oh!")
                repeat(6) { parts.add(Part(cx + (rnd.nextFloat() - 0.5f) * 30 * k, floorY - 20 * k, (rnd.nextFloat() - 0.5f) * 100 * d, -120 * d, 1f, 1f, "✨", 20 * d, 0f)) }
                if (s == "🚽") { pet.earn(2); parts.add(Part(cx, floorY - 40 * k, 0f, -80 * d, 1.2f, 1.2f, "+2 🪙", 20 * d, 0f)) }
            }
            s == "⚽" -> { if (ball == null) ball = floatArrayOf(width * 0.25f, floorY - 80 * u, 300 * d, -500 * d); talk("top!") }
            s == "🎈" -> {
                parts.add(Part(x, y, (rnd.nextFloat() - 0.5f) * 40 * d, -160 * d, 4f, 4f, "🎈", 44 * d, 0f))
                pet.cheer(4f); pet.save(); tickleUntil = now + 1500; talk("vaay!")
            }
        }
    }

    private fun dropItem(item: String, x: Float, y: Float) {
        val now = SystemClock.uptimeMillis()
        val (mx, my) = mouth()
        if (hypot(x - mx, y - my) > 80 * d) return
        when (item) {
            "🌡️" -> {
                val t = pet.temperature()
                talk("%.1f°C %s".format(t, if (pet.sick) "🤒" else "✓"))
                if (pet.sick) parts.add(Part(mx, my - 50 * d, 0f, -30 * d, 2f, 2f, "Ateşi var! İlaç ver 💊", 16 * d, 0f))
            }
            "💊" -> {
                if (!pet.sick) { refuseUntil = now + 1200; talk(if (pet.stage >= 4) "hasta değilim ki!" else "ı-ıh") }
                else {
                    pet.cure(); eatUntil = now + 700; talk(if (pet.stage >= 4) "iyileştim! 💪" else "✨")
                    repeat(10) { parts.add(Part(mx, my, (rnd.nextFloat() - 0.5f) * 300 * d, -rnd.nextFloat() * 300 * d, 1.2f, 1.2f, "✨", 22 * d, 300 * d)) }
                }
            }
            in Pet.FOODS -> {
                val why = pet.eatItem(item)
                if (why == null) {
                    eatUntil = now + 900
                    talk(if (item == "🍼" || item == "🥛") "hınk hınk" else "ham!")
                    repeat(5) { parts.add(Part(mx + (rnd.nextFloat() - 0.5f) * 30 * d, my, (rnd.nextFloat() - 0.5f) * 160 * d, -80 * d, 0.7f, 0.7f, "•", 16 * d, 600 * d)) }
                    if (pet.hunger < 8) parts.add(Part(mx, my - 40 * d, 0f, -60 * d, 1.4f, 1.4f, "💗", 28 * d, 0f))
                } else { refuseUntil = now + 1200; talk(why) }
            }
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
                pet.hatch(); hatchTaps = 0; waveUntil = now + 2500
                repeat(14) { parts.add(Part(cx, floorY - 20 * k, (rnd.nextFloat() - 0.5f) * 600 * d, -rnd.nextFloat() * 700 * d, 1.6f, 1.6f, listOf("✨", "🎉", "💗")[rnd.nextInt(3)], 24 * d, 900 * d)) }
                pet.takeMilestone()?.let { celebrate(it) }
            }
            return
        }
        if (pet.asleep) { talk("şşş… zzz"); return }
        tickleUntil = now + 1400
        pet.cheer(2f); pet.save()
        talk(listOf("hihi!", "kıkır!", "ahaha!")[rnd.nextInt(3)])
        repeat(3) { parts.add(Part(cx + (rnd.nextFloat() - 0.5f) * 30 * k, babyTop(), (rnd.nextFloat() - 0.5f) * 80 * d, -140 * d, 1.1f, 1.1f, "💗", 24 * d, 0f)) }
    }

    private fun talk(s: String) { say = if (s.contains("°C")) s else pet.voice(s); sayUntil = SystemClock.uptimeMillis() + 1800 }
    private fun hello() = when (pet.stage) { 0, 1 -> "agu!"; 2, 3 -> "ba-ba!"; else -> "merhaba!" }
}
