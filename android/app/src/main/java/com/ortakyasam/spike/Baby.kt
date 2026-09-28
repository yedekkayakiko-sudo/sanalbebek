package com.ortakyasam.spike

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.SystemClock
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Tek bir bebek: duvar kağıdı (kilit ekranı) ve ikonların üstündeki pencere (ana ekran) aynı durumu çizer.
 * Konumlar ekran pikseli cinsindendir. Bebek ekranın her yerinde iki boyutta dolaşır.
 */
object Baby {
    enum class Mode { WALK, EAT, KICK, CLIMB, HANG, FALL, CARRIED, SHY, DANCE, SIT, PEEK }
    private enum class Goal { NONE, EAT, SLEEP, KICK, CLIMB, PEEK }

    const val BOWL_X = 0.16f
    const val BED_X = 0.40f
    const val HATCH_MS = 3500L
    private val SPEED = floatArrayOf(34f, 46f, 62f, 80f, 96f, 112f)

    lateinit var pet: Pet; private set
    lateinit var chibi: ChibiRenderer; private set
    private var app: Context? = null
    private val rnd = Random(SystemClock.uptimeMillis())

    var w = 0; private set
    var h = 0; private set
    var sb = 60f; private set
    var S = 1f; private set
    var u = 2f; private set
    var locked = false; private set
    var charging = false

    var mode = Mode.WALK; private set
    private var modeStart = 0L
    private var modeUntil = 0L
    private var goal = Goal.NONE
    var bx = -1f; private set
    var by = -1f; private set
    var gy = 0f; private set          // tutma noktası: parmak ya da pile tutunan eller
    private var tx = -1f
    private var ty = -1f
    private var climbFrom = 0f
    private var facing = 1
    private var moving = false
    private var tickleUntil = 0L
    private var waveUntil = 0L
    private var nextWander = 0L
    private var bubble: String? = null
    private var bubbleUntil = 0L
    var hatchStart = 0L
    var ballX = 0.66f; private set
    private var ballV = 0f
    private var lastStep = 0L
    private var lastCatchUp = 0L
    private var peekSide = 1
    private var shownFedAt = 0L

    fun init(ctx: Context) {
        if (app != null) return
        app = ctx.applicationContext
        pet = Pet(ctx.applicationContext)
        chibi = ChibiRenderer(pet.look)
        shownFedAt = pet.fedAt
    }

    /** Ekran açılınca: ayarlar uygulamada değişmiş olabilir, durumu yeniden oku ve geçen süreyi uygula. */
    fun reload(ctx: Context) {
        init(ctx)
        pet = Pet(ctx.applicationContext)
        if (pet.look != chibi.look) chibi = ChibiRenderer(pet.look)
        pet.catchUp(); lastCatchUp = SystemClock.uptimeMillis()
        lastStep = 0L
        if (pet.hatched) pet.takeMilestone()?.let { say(it, 4500); waveUntil = SystemClock.uptimeMillis() + 3000 }
        // Uygulamadan yeni beslendiyse ana ekranda yerken görünsün.
        if (pet.fedAt > shownFedAt && System.currentTimeMillis() - pet.fedAt < 5 * 60_000 && pet.hatched && !pet.asleep) {
            shownFedAt = pet.fedAt
            setMode(Mode.EAT, 3500); say("ham-ham!", 2500)
        }
    }

    fun setScreen(width: Int, height: Int, statusBar: Float) {
        if (width <= 0 || height <= 0) return
        if (width == w && height == h) { sb = statusBar; return }
        if (w > 0 && bx >= 0f) {
            val fx = width.toFloat() / w; val fy = height.toFloat() / h
            bx *= fx; tx *= fx; by *= fy; ty *= fy; gy *= fy
        }
        w = width; h = height; sb = statusBar
        S = w / 400f; u = 2f * S
        if (bx < 0f) { bx = w * 0.5f; by = h * 0.8f; tx = bx; ty = by }
    }

    fun setLocked(v: Boolean) {
        if (v == locked) return
        locked = v
        // Kilit ↔ ana ekran: bebeği o ekranın alanına koy.
        if (mode == Mode.HANG || mode == Mode.CLIMB || mode == Mode.CARRIED || mode == Mode.FALL || mode == Mode.PEEK) mode = Mode.WALK
        val (lo, hi) = yRange()
        by = by.coerceIn(lo, hi); bx = bx.coerceIn(14 * S, w - 14 * S)
        tx = bx; ty = by; goal = Goal.NONE; moving = false
    }

    fun homeFloor() = h * 0.80f
    fun batteryX() = pet.batteryX * w
    private fun yRange(): Pair<Float, Float> =
        if (locked) Pair(h * 0.72f, h * 0.955f) else Pair(min(sb + 50 * u, h * 0.5f), h * 0.9f)

    fun isGrip() = mode == Mode.CARRIED || mode == Mode.CLIMB || mode == Mode.HANG

    // ---------- Güncelleme ----------

    fun step(now: Long) {
        if (w == 0 || app == null) return
        if (lastStep != 0L && now - lastStep < 8) return        // iki yüzey aynı karede çağırırsa bir kez say
        val dt = if (lastStep == 0L) 0f else min(0.1f, (now - lastStep) / 1000f)
        lastStep = now
        update(now, dt)
    }

    private fun update(now: Long, dt: Float) {
        if (now - lastCatchUp > 60_000) { pet.catchUp(); lastCatchUp = now }
        if (!pet.hatched) {
            if (hatchStart in 1..now && now - hatchStart >= HATCH_MS) {
                pet.hatch(); hatchStart = 0L
                say(if (pet.stage >= 4) "merhaba!" else "ba!", 2500); waveUntil = now + 2500
                log("yumurtadan çıktı")
            }
            return
        }

        if (ballV != 0f) {
            ballX += ballV * dt
            ballV *= 0.985f.pow(dt * 60)
            if (ballX < 0.06f) { ballX = 0.06f; ballV = abs(ballV) * 0.6f }
            if (ballX > 0.94f) { ballX = 0.94f; ballV = -abs(ballV) * 0.6f }
            if (abs(ballV) < 0.01f) ballV = 0f
        }

        when (mode) {
            Mode.CARRIED -> return
            Mode.FALL -> {
                val sp = 900 * S * dt
                if (ty - by <= sp) { by = ty; mode = Mode.WALK; say("hop!", 900); afterLanding() } else by += sp
                return
            }
            Mode.CLIMB -> {
                bx = batteryX()
                val p = ((now - modeStart) / 900f).coerceIn(0f, 1f)
                val top = sb + 2 * S
                gy = climbFrom + (top - climbFrom) * (1 - (1 - p) * (1 - p))
                if (p >= 1f) {
                    setMode(Mode.HANG, if (charging) 20_000 else 9000)
                    say(if (pet.stage >= 4) "pil benim!" else "iii!", 1800)
                    log("pile asıldı")
                }
                return
            }
            Mode.HANG -> {
                bx = batteryX(); gy = sb + 2 * S
                if (now > modeUntil) dropFromHang()
                return
            }
            Mode.PEEK -> if (now > modeUntil) { mode = Mode.WALK; goTo(if (peekSide < 0) w * 0.2f else w * 0.8f, by, Goal.NONE) } else return
            Mode.EAT, Mode.KICK, Mode.SHY, Mode.DANCE, Mode.SIT -> if (now > modeUntil) mode = Mode.WALK else return
            Mode.WALK -> {}
        }

        if (pet.asleep) { moving = false; return }

        val dx = tx - bx; val dy = ty - by
        val dist = hypot(dx, dy)
        if (dist > 2f) {
            moving = true
            if (abs(dx) > 1f) facing = if (dx > 0) 1 else -1
            val stepPx = SPEED[pet.stage.coerceIn(0, 5)] * S * dt
            if (dist <= stepPx) { bx = tx; by = ty; arrive() }
            else { bx += dx / dist * stepPx; by += dy / dist * stepPx }
        } else if (moving) {
            arrive()
        } else if (now > nextWander) {
            wander(now)
        }
    }

    /** Boşken kendi kendine yaptıkları: ekranın her yerine gider, saklanır, oturur, dans eder, pile tırmanır. */
    private fun wander(now: Long) {
        nextWander = now + 3500 + rnd.nextLong(5500)
        val (lo, hi) = yRange()
        val r = rnd.nextFloat()
        when {
            pet.sick && r < 0.3f -> say(if (pet.stage >= 4) "🤒 hastayım…" else "🤒 ıhh", 2000)
            pet.diaper > 70 && r < 0.25f -> say(if (pet.stage >= 4) "tuvalet!" else "💩 ıı!", 1800)
            pet.hunger > 70 && r < 0.2f -> say(if (pet.stage >= 4) "acıktım…" else "mama?", 1800)
            !locked && pet.stage >= 2 && (r < 0.12f || (charging && r < 0.4f)) -> goTo(batteryX(), lo, Goal.CLIMB)
            !locked && pet.stage >= 2 && r < 0.22f -> {
                peekSide = if (rnd.nextBoolean()) -1 else 1
                goTo(if (peekSide < 0) -7 * u else w + 7 * u, lo + rnd.nextFloat() * (hi - lo), Goal.PEEK)
            }
            pet.stage >= 3 && r < 0.3f -> setMode(Mode.SIT, 3500)
            pet.stage >= 1 && r < 0.36f -> { setMode(Mode.DANCE, 2500); say("♪", 1500) }
            r < 0.42f -> { waveUntil = now + 1500; say(arriveWord(), 1200) }
            else -> goTo(14 * S + rnd.nextFloat() * (w - 28 * S), lo + rnd.nextFloat() * (hi - lo), Goal.NONE)
        }
    }

    private fun goTo(x: Float, y: Float, g: Goal) {
        if (mode != Mode.WALK && mode != Mode.DANCE && mode != Mode.SHY && mode != Mode.SIT && mode != Mode.PEEK) return
        mode = Mode.WALK
        val (lo, hi) = yRange()
        tx = x.coerceIn(-8 * u, w + 8 * u); ty = y.coerceIn(lo, hi); goal = g
        nextWander = SystemClock.uptimeMillis() + 6000
    }

    private fun arrive() {
        val now = SystemClock.uptimeMillis()
        moving = false
        val g = goal; goal = Goal.NONE
        when (g) {
            Goal.EAT -> if (pet.feed()) { shownFedAt = pet.fedAt; facing = -1; setMode(Mode.EAT, 3000); say("ham-ham!", 2500) }
                        else { facing = 1; setMode(Mode.SHY, 1500); say(if (pet.stage >= 4) "tokum!" else "ı-ıh", 1500) }
            Goal.SLEEP -> if (pet.sleep()) { say("iyi geceler…", 1800); log("yatağa yattı") }
                          else say(if (pet.stage >= 4) "uykum yok!" else "ı-ıh", 1500)
            Goal.KICK -> {
                facing = if (ballX * w > bx) 1 else -1
                setMode(Mode.KICK, 450)
                ballV = facing * (0.5f + rnd.nextFloat() * 0.4f)
                pet.play(); say("hop!", 900)
            }
            Goal.CLIMB -> {
                if (locked) return
                bx = batteryX(); climbFrom = by - 40 * u; gy = climbFrom; setMode(Mode.CLIMB, 900)
            }
            Goal.PEEK -> { facing = -peekSide; setMode(Mode.PEEK, 9000) }
            Goal.NONE -> { waveUntil = now + 1400; if (bubble == null || now > bubbleUntil) say(arriveWord(), 1200) }
        }
    }

    private fun afterLanding() {
        if (locked) return
        val fy = homeFloor()
        if (abs(bx - BOWL_X * w) < 60 * S && abs(by - fy) < 70 * S) { goal = Goal.EAT; arrive() }
        else if (abs(bx - BED_X * w) < 60 * S && abs(by - fy) < 70 * S) { goal = Goal.SLEEP; arrive() }
    }

    private fun dropFromHang() {
        by = gy + 42 * u
        ty = min(by + 60 * S, yRange().second)
        setMode(Mode.FALL, Long.MAX_VALUE)
    }

    // ---------- Dokunma ----------

    fun hit(x: Float, y: Float): Boolean {
        val cy = if (isGrip()) gy + 26 * u else by - 18 * u
        return abs(x - bx) < 20 * u && abs(y - cy) < 24 * u
    }

    /** Ana ekranda boş yere dokunma (duvar kağıdına COMMAND_TAP olarak gelir). */
    fun tapEmpty(x: Float, y: Float) {
        if (!pet.hatched) { tapBaby(); return }
        if (mode == Mode.CARRIED || mode == Mode.FALL || mode == Mode.CLIMB) return
        if (hit(x, y)) { tapBaby(); return }
        if (pet.asleep) { say("zzz", 1200); return }
        if (mode == Mode.HANG) { dropFromHang(); return }
        val fy = homeFloor()
        val nearFloor = abs(y - fy) < 45 * S
        val what = when {
            !locked && abs(x - batteryX()) < 80 * S && y < sb + 110 * S -> {
                if (pet.stage >= 2) goTo(batteryX(), 0f, Goal.CLIMB) else { goTo(batteryX(), 0f, Goal.NONE); say("?", 1200) }
                "PİL"
            }
            !locked && nearFloor && abs(x - BOWL_X * w) < 32 * S -> { goTo(BOWL_X * w + 24 * S, fy, Goal.EAT); "KAP" }
            !locked && nearFloor && abs(x - BED_X * w) < 40 * S -> { goTo(BED_X * w, fy, Goal.SLEEP); "YATAK" }
            !locked && abs(y - (fy - 9 * S)) < 30 * S && abs(x - ballX * w) < 26 * S -> {
                val side = if (bx < ballX * w) -1 else 1
                goTo(ballX * w + side * 20 * S, fy, Goal.KICK); "TOP"
            }
            else -> { goTo(x, y, Goal.NONE); "boş alana" }
        }
        log("dokunma · x=${x.toInt()} y=${y.toInt()} · $what")
    }

    /** Bebeğin kendisine dokunma. */
    fun tapBaby() {
        val now = SystemClock.uptimeMillis()
        if (!pet.hatched) { say("tık tık!", 1200); if (hatchStart == 0L) hatchStart = now; return }
        if (pet.asleep) { say("şşş… uyuyor", 1500); return }
        tickleUntil = now + 1600
        if (mode == Mode.PEEK) {
            say("ce-e!", 1500); mode = Mode.WALK
            goTo(if (peekSide < 0) w * 0.25f else w * 0.75f, by, Goal.NONE)
            log("saklandığı yerde bulundu (ce-e)")
            return
        }
        say(listOf("hihi!", "kıkır!", "ahaha!")[rnd.nextInt(3)], 1500)
        if (mode == Mode.HANG) modeUntil = max(modeUntil, now + 2500)
        log("bebeğe dokunuldu · gıdıklandı")
    }

    fun canCarry() = pet.hatched && !pet.asleep && mode != Mode.CLIMB && mode != Mode.FALL

    fun startCarry() {
        setMode(Mode.CARRIED, Long.MAX_VALUE); goal = Goal.NONE; moving = false
        gy = by - 42 * u
        say("vii!", 1200)
        log("parmakla taşındı")
    }

    fun carryTo(x: Float, y: Float) {
        if (x > bx + 1) facing = 1 else if (x < bx - 1) facing = -1
        bx = x.coerceIn(0f, w.toFloat())
        gy = y.coerceIn(sb + 2 * S, h * 0.9f - 42 * u)
    }

    fun drop() {
        if (mode != Mode.CARRIED) return
        // Pilin yanına bırakılırsa ona tutunur.
        if (!locked && pet.stage >= 2 && abs(bx - batteryX()) < 90 * S && gy < sb + 120 * S) {
            bx = batteryX(); gy = sb + 2 * S
            setMode(Mode.HANG, if (charging) 20_000 else 9000)
            say(if (pet.stage >= 4) "pil benim!" else "iii!", 1800)
            log("pile bırakıldı, asıldı")
            return
        }
        val (lo, hi) = yRange()
        by = (gy + 42 * u).coerceIn(lo - 60 * S, hi)
        ty = min(max(by + 14 * S, lo), hi)
        setMode(Mode.FALL, Long.MAX_VALUE)
    }

    fun onPower(connected: Boolean) {
        charging = connected
        if (app == null || !pet.hatched || pet.asleep) return
        if (connected) {
            say(if (pet.stage >= 4) "⚡ yaşasın!" else "⚡ iii!", 2200)
            if (!locked && pet.stage >= 2) goTo(batteryX(), 0f, Goal.CLIMB) else waveUntil = SystemClock.uptimeMillis() + 1500
        } else if (mode == Mode.HANG) dropFromHang()
    }

    fun startDance() { if (pet.hatched && !pet.asleep && pet.stage >= 1) { setMode(Mode.DANCE, 3500); say("♪ la-la", 3000) } }
    fun sayHungryIfNeeded() { if (pet.hatched && !pet.asleep && pet.hunger > 70) say(if (pet.stage >= 4) "acıktım…" else "mama?", 2500) }

    private fun arriveWord() = when (pet.stage) { 0, 1 -> "agu!"; 2, 3 -> "ba-ba!"; else -> "geldim!" }

    private fun setMode(m: Mode, dur: Long) {
        val now = SystemClock.uptimeMillis()
        mode = m; modeStart = now
        modeUntil = if (dur == Long.MAX_VALUE) Long.MAX_VALUE else now + dur
    }

    private fun say(s: String, ms: Long) { bubble = s; bubbleUntil = SystemClock.uptimeMillis() + ms }

    private fun log(s: String) { app?.let { DiagLog.add(it, "$s · ${if (locked) "kilit" else "ana ekran"}") } }

    // ---------- Çizim ----------

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3A2A20.toInt(); isFakeBoldText = true }
    private val rect = RectF()
    private val path = Path()

    /**
     * Bebeği çizer. ox, oy: çizim yüzeyinin ekrandaki sol üst köşesi.
     * overlay: ikonların üstündeki küçük pencere mi (balon pencerenin içinde kalmalı).
     */
    fun draw(c: Canvas, now: Long, ox: Float, oy: Float, overlay: Boolean, winW: Float = 0f, winH: Float = 0f) {
        if (app == null) return
        val st = pet.stage.coerceIn(0, 5)
        val x = bx - ox
        val hatch = when {
            pet.hatched -> 1f
            hatchStart in 1..now -> ((now - hatchStart).toFloat() / HATCH_MS).coerceIn(0f, 0.99f)
            else -> 0f
        }
        val mood = when {
            pet.asleep -> Mood.SLEEP
            now < tickleUntil -> Mood.LAUGH
            mode == Mode.SHY -> Mood.SHY
            mode == Mode.CARRIED && now - modeStart < 500 -> Mood.SURPRISE
            mode == Mode.DANCE || mode == Mode.HANG -> Mood.LAUGH
            pet.energy < 25 || pet.sick -> Mood.SLEEPY
            pet.hunger > 70 -> Mood.HUNGRY
            else -> Mood.HAPPY
        }
        val ground = when {
            st == 0 -> Pose.LIE
            st == 1 -> Pose.SIT
            st == 2 -> if (moving) Pose.CRAWL else Pose.SIT
            mode == Mode.SIT -> Pose.SIT
            else -> Pose.STAND
        }
        val arms = when {
            mode == Mode.DANCE -> if ((now / 400) % 2 == 0L) Arms.UP else Arms.WAVE
            now < waveUntil -> Arms.WAVE
            mode == Mode.PEEK -> Arms.WAVE
            charging && mode == Mode.WALK && !moving -> Arms.UP
            else -> Arms.NONE
        }
        val lookX = when { moving -> facing * 0.7f; mode == Mode.PEEK -> -peekSide * 0.9f; else -> 0f }
        val fy = by - oy
        val frame = when {
            !pet.hatched -> Frame(x, fy, u, st, Pose.EGG, Mood.HAPPY, hatch = hatch)
            pet.asleep && mode == Mode.WALK -> Frame(x, fy, u, st, Pose.LIE, Mood.SLEEP)
            mode == Mode.CARRIED -> Frame(x, gy - oy, u, st, Pose.DANGLE, mood, facing = facing)
            mode == Mode.CLIMB || mode == Mode.HANG -> Frame(x, gy - oy, u, st, Pose.HANG, mood, charging = charging)
            mode == Mode.FALL -> Frame(x, ty - oy, u, st, ground, Mood.SURPRISE, Arms.UP, lift = ty - by)
            mode == Mode.EAT -> Frame(x, fy, u, st, Pose.EAT, Mood.HAPPY, facing = facing)
            mode == Mode.KICK -> Frame(x, fy, u, st, ground, Mood.LAUGH, kick = st >= 3, facing = facing)
            mode == Mode.DANCE -> Frame(x, fy, u, st, ground, mood, arms, moving = true, facing = facing)
            else -> Frame(x, fy, u, st, ground, mood, arms, moving, facing, lookX = lookX, charging = charging)
        }
        val p = chibi.draw(c, frame.copy(scale = pet.scale(), mature = pet.maturity()), now)
        val b = bubble
        if (b == null || now > bubbleUntil) { bubble = null; return }
        when {
            overlay && isGrip() -> drawBubble(c, x, winH - 3 * S, b, 0f, winW, false, 13 * S)
            overlay -> drawBubble(c, p.x, p.y, b, 0f, winW, true, 13 * S)
            isGrip() -> drawBubble(c, x + (if (bx > w / 2) -1 else 1) * 50 * S, gy - oy + 50 * S, b, 0f, c.width.toFloat(), true, 15 * S)
            else -> drawBubble(c, p.x, max(p.y, sb - oy + 34 * S), b, 0f, c.width.toFloat(), true, 15 * S)
        }
    }

    private fun drawBubble(c: Canvas, px: Float, bottom: Float, s: String, minX: Float, maxX: Float, tail: Boolean, size0: Float) {
        textPaint.textSize = size0
        // Uzun yazı (dönüm noktası gibi) dar pencereye sığsın diye küçülür.
        val room = maxX - minX - 4 * S
        val full = textPaint.measureText(s) + size0 * 1.1f
        val size = if (full > room) size0 * room / full else size0
        textPaint.textSize = size
        val tw = textPaint.measureText(s)
        val pad = size * 0.55f
        val bh = size + pad * 1.2f
        val bw = tw + 2 * pad
        val left = if (bw >= maxX - minX) minX else (px - bw / 2).coerceIn(minX + 2 * S, maxX - bw - 2 * S)
        fill.color = 0xF2FFFFFF.toInt()
        rect.set(left, bottom - bh, left + bw, bottom)
        c.drawRoundRect(rect, bh / 2, bh / 2, fill)
        if (tail) {
            path.reset()
            val t = px.coerceIn(left + bh / 2, left + bw - bh / 2)
            path.moveTo(t - 4 * S, bottom - 1); path.lineTo(t + 4 * S, bottom - 1); path.lineTo(t, bottom + 5 * S); path.close()
            c.drawPath(path, fill)
        }
        c.drawText(s, left + pad, bottom - bh / 2 + size * 0.36f, textPaint)
    }

    /** Ana ekrandaki kap, yatak ve top (duvar kağıdında, ikonların arkasında). */
    fun drawProps(c: Canvas, ox: Float) {
        if (app == null) return
        val fy = homeFloor()
        val kx = BOWL_X * w - ox
        fill.color = 0xFFF4A26B.toInt()
        rect.set(kx - 20 * S, fy - 16 * S, kx + 20 * S, fy + 6 * S); c.drawArc(rect, 0f, 180f, true, fill)
        fill.color = if (pet.hunger > 70) 0xFF8A5A3C.toInt() else 0xFFF6D365.toInt()
        rect.set(kx - 17 * S, fy - 9 * S, kx + 17 * S, fy - 3 * S); c.drawOval(rect, fill)
        val yx = BED_X * w - ox
        fill.color = 0xFF9DB4E0.toInt()
        rect.set(yx - 34 * S, fy - 12 * S, yx + 34 * S, fy + 2 * S); c.drawRoundRect(rect, 7 * S, 7 * S, fill)
        fill.color = 0xFFFFFFFF.toInt()
        rect.set(yx - 30 * S, fy - 17 * S, yx - 12 * S, fy - 7 * S); c.drawRoundRect(rect, 5 * S, 5 * S, fill)
        val bxs = ballX * w - ox; val r = 9 * S
        fill.color = 0xFFE85D5D.toInt(); c.drawCircle(bxs, fy - r, r, fill)
        stroke.color = 0xFFFFFFFF.toInt(); stroke.strokeWidth = 2.2f * S
        rect.set(bxs - r * 0.7f, fy - r * 1.7f, bxs + r * 0.7f, fy - r * 0.3f)
        c.drawArc(rect, (ballX * 3000f) % 360f, 120f, false, stroke)
    }
}
