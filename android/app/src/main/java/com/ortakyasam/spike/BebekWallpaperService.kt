package com.ortakyasam.spike

import android.app.KeyguardManager
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Telefonda yaşayan chibi. Prototipteki (prototype/index.html) etkileşimlerin cihazdaki karşılığı:
 * dokunulan yere gelir, üstüne dokununca gıdıklanır, kaba/yatağa/topa dokununca onları kullanır,
 * pil ikonunun altına dokununca tırmanıp asılır, şarja takınca pile koşar, parmakla taşınabilir
 * (launcher sürüklemeyi iletirse). Ekran kapalıyken hiçbir şey çizilmez.
 *
 * Cihaz testi soruları için günlüğe (DiagLog) yazmaya devam eder.
 */
class BebekWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = BebekEngine()

    private enum class Mode { WALK, EAT, KICK, CLIMB, HANG, FALL, CARRIED, SHY, DANCE }
    private enum class Goal { NONE, EAT, SLEEP, KICK, CLIMB }

    inner class BebekEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val rnd = Random(SystemClock.uptimeMillis())
        private var visible = false
        private var w = 0
        private var h = 0
        private var S = 1f              // ölçek: 400 px genişliğe göre
        private var u = 2f              // chibi birimi
        private var sb = 60f            // durum çubuğu yüksekliği
        private var xOff = 0f
        private var lastLoggedOffset = -1f
        private var frames = 0
        private var visibleSince = 0L
        private var lastT = 0L
        private var lastCatchUp = 0L
        private var wasLocked: Boolean? = null

        private lateinit var pet: Pet
        private lateinit var chibi: ChibiRenderer

        // Konum: x dünya koordinatında (ekran genişliği oranı, sayfalarla kayar), y piksel (ayak hizası).
        private var bx = 0.5f
        private var by = -1f
        private var tx = 0.5f
        private var ty = -1f
        private var facing = 1
        private var moving = false
        private var mode = Mode.WALK
        private var modeStart = 0L
        private var modeUntil = 0L
        private var goal = Goal.NONE
        private var gy = 0f             // tutma noktası (taşınırken parmak, tırmanırken eller)
        private var climbFrom = 0f
        private var tickleUntil = 0L
        private var waveUntil = 0L
        private var nextWander = 0L
        private var bubble: String? = null
        private var bubbleUntil = 0L
        private var hatchStart = 0L
        private var chargingNow = false
        private var ballX = 0.66f
        private var ballV = 0f

        // Sürükleme
        private var downX = 0f
        private var downY = 0f
        private var downOnBaby = false
        private var touchMoves = 0
        private var lastDragEnd = 0L

        private val bg = Paint()
        private var bgHour = -1
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3A2A20.toInt(); isFakeBoldText = true }
        private val rect = RectF()
        private val path = Path()
        private val stars = List(40) { floatArrayOf(Random.nextFloat() * 1.3f, Random.nextFloat() * 0.6f, 0.5f + Random.nextFloat()) }

        private val drawRunner = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) handler.postDelayed(this, if (pet.asleep && mode == Mode.WALK && !moving) 200L else 33L)
            }
        }

        private val powerReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, i: Intent) = onPower(i.action == Intent.ACTION_POWER_CONNECTED)
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            pet = Pet(applicationContext)
            chibi = ChibiRenderer(pet.look)
            val id = resources.getIdentifier("status_bar_height", "dimen", "android")
            if (id > 0) sb = resources.getDimensionPixelSize(id).toFloat()
            val f = IntentFilter().apply { addAction(Intent.ACTION_POWER_CONNECTED); addAction(Intent.ACTION_POWER_DISCONNECTED) }
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(powerReceiver, f, Context.RECEIVER_EXPORTED) else registerReceiver(powerReceiver, f)
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawRunner)
            try { unregisterReceiver(powerReceiver) } catch (_: Exception) { }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            w = width; h = height
            S = w / 400f
            u = 2f * S
            if (by < 0f) { by = floor(); ty = by }
            bgHour = -1
            drawFrame()
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            val ctx = applicationContext
            if (v) {
                visibleSince = SystemClock.elapsedRealtime()
                frames = 0
                lastT = 0L
                // Ayarlar uygulamada değişmiş olabilir: durumu yeniden oku, geçen süreyi uygula.
                pet = Pet(ctx)
                if (pet.look != chibi.look) chibi = ChibiRenderer(pet.look)
                pet.catchUp(); lastCatchUp = SystemClock.uptimeMillis()
                chargingNow = charging(ctx)
                val hp = headphones(ctx)
                if (!isPreview) DiagLog.add(ctx, "GÖRÜNÜR · ${where()} · şarj: $chargingNow · kulaklık: $hp · ${Pet.STAGE_NAMES[pet.stage]} · açlık ${pet.hunger.toInt()} enerji ${pet.energy.toInt()}${if (pet.asleep) " · uyuyor" else ""}")
                val now = SystemClock.uptimeMillis()
                if (!pet.hatched && !isPreview && hatchStart == 0L) hatchStart = now + 600
                else if (pet.hatched && !pet.asleep && hp && pet.stage >= 1) { setMode(Mode.DANCE, 3500); say("♪ la-la", 3000) }
                else if (pet.hatched && !pet.asleep && pet.hunger > 70) say(if (pet.stage >= 4) "acıktım…" else "mama?", 2500)
                handler.removeCallbacks(drawRunner)
                handler.post(drawRunner)
            } else {
                handler.removeCallbacks(drawRunner)
                if (mode == Mode.CARRIED) { mode = Mode.WALK; by = floor(); ty = by }
                if (!isPreview) {
                    val secs = (SystemClock.elapsedRealtime() - visibleSince) / 1000
                    DiagLog.add(ctx, "gizlendi · $secs sn görünür kaldı · $frames kare çizildi")
                }
            }
        }

        override fun onOffsetsChanged(
            xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, xPixelOffset: Int, yPixelOffset: Int,
        ) {
            xOff = xOffset
            if (!isPreview && xOffsetStep > 0f && abs(xOffset - lastLoggedOffset) >= xOffsetStep * 0.99f) {
                lastLoggedOffset = xOffset
                DiagLog.add(applicationContext, "sayfa · xOffset=${"%.2f".format(xOffset)} · adım=${"%.2f".format(xOffsetStep)}")
            }
            if (!visible) drawFrame()
        }

        // ---------- Dokunma ----------

        override fun onCommand(action: String?, x: Int, y: Int, z: Int, extras: Bundle?, resultRequested: Boolean): Bundle? {
            if (action == WallpaperManager.COMMAND_TAP && w > 0 && SystemClock.uptimeMillis() - lastDragEnd > 400) onTap(x.toFloat(), y.toFloat())
            return super.onCommand(action, x, y, z, extras, resultRequested)
        }

        private fun onTap(x: Float, y: Float) {
            val now = SystemClock.uptimeMillis()
            val ctx = applicationContext
            if (!pet.hatched) {
                say("tık tık!", 1200)
                if (hatchStart == 0L) hatchStart = now
                return
            }
            if (mode == Mode.CARRIED || mode == Mode.FALL || mode == Mode.CLIMB) return
            if (hitBaby(x, y)) {
                if (pet.asleep) { say("şşş… uyuyor", 1500); DiagLog.add(ctx, "dokunma · bebeğe (uyuyor)"); return }
                tickleUntil = now + 1600
                say(listOf("hihi!", "kıkır!", "ahaha!")[rnd.nextInt(3)], 1500)
                if (mode == Mode.HANG) modeUntil = max(modeUntil, now + 2500)
                DiagLog.add(ctx, "dokunma · BEBEĞE · gıdıklandı")
                return
            }
            if (pet.asleep) { say("zzz", 1200); return }
            if (mode == Mode.HANG) { dropDown(); return }
            val wx = x / w + xOff * PARALLAX
            val locked = isLocked()
            val nearFloor = abs(y - floor()) < 40 * S
            val what: String
            when {
                !locked && abs(x - pet.batteryX * w) < 70 * S && y < sb + 90 * S -> {
                    what = "PİL"
                    if (pet.stage >= 2) goTo(pet.batteryX + xOff * PARALLAX, floor(), Goal.CLIMB)
                    else { goTo(pet.batteryX + xOff * PARALLAX, floor(), Goal.NONE); say("?", 1200) }
                }
                !locked && nearFloor && abs(wx - BOWL) < 0.08f -> { what = "KAP"; goTo(BOWL + 0.06f, floor(), Goal.EAT) }
                !locked && nearFloor && abs(wx - BED) < 0.1f -> { what = "YATAK"; goTo(BED, floor(), Goal.SLEEP) }
                !locked && abs(y - (floor() - 9 * S)) < 30 * S && abs(wx - ballX) < 0.06f -> {
                    what = "TOP"
                    val side = if (bx < ballX) -1 else 1
                    goTo(ballX + side * 0.05f, floor(), Goal.KICK)
                }
                else -> {
                    what = "boş alana"
                    val top = if (locked) floor() else h * 0.2f
                    goTo(wx, y.coerceIn(top, floor()), Goal.NONE)
                }
            }
            DiagLog.add(ctx, "dokunma · x=${x.toInt()} y=${y.toInt()} · $what · ${where()}")
        }

        override fun onTouchEvent(event: MotionEvent) {
            // Birçok launcher dokunmayı sadece COMMAND_TAP olarak iletir; sürükleme gelirse bebek taşınır.
            val now = SystemClock.uptimeMillis()
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchMoves = 0; downX = event.x; downY = event.y
                    downOnBaby = pet.hatched && !pet.asleep && hitBaby(event.x, event.y) && mode != Mode.CLIMB && mode != Mode.FALL
                }
                MotionEvent.ACTION_MOVE -> {
                    touchMoves++
                    if (downOnBaby && mode != Mode.CARRIED && hypot(event.x - downX, event.y - downY) > 14 * S) {
                        setMode(Mode.CARRIED, Long.MAX_VALUE); goal = Goal.NONE; moving = false
                        say("vii!", 1200)
                        DiagLog.add(applicationContext, "SÜRÜKLEME geldi · bebek taşınıyor")
                    }
                    if (mode == Mode.CARRIED) {
                        bx = event.x / w + xOff * PARALLAX
                        gy = event.y.coerceIn(sb + 10 * S, floor() - 40 * u)
                        facing = if (event.x > downX) 1 else -1
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (mode == Mode.CARRIED) {
                        lastDragEnd = now
                        by = gy + 42 * u
                        ty = floor(); tx = bx
                        setMode(Mode.FALL, Long.MAX_VALUE)
                    } else if (touchMoves > 3 && !isPreview) {
                        DiagLog.add(applicationContext, "sürükleme olayı geldi · $touchMoves hareket (bebeğin üstünde değildi)")
                    }
                    downOnBaby = false
                }
            }
            super.onTouchEvent(event)
        }

        // ---------- Olaylar ----------

        private fun onPower(connected: Boolean) {
            chargingNow = connected
            if (!isPreview) DiagLog.add(applicationContext, if (connected) "ŞARJA TAKILDI" else "şarjdan çıkarıldı")
            if (!visible || !pet.hatched || pet.asleep) return
            if (connected) {
                say(if (pet.stage >= 4) "⚡ yaşasın!" else "⚡ iii!", 2200)
                if (!isLocked() && pet.stage >= 2 && mode == Mode.WALK) goTo(pet.batteryX + xOff * PARALLAX, floor(), Goal.CLIMB)
                else waveUntil = SystemClock.uptimeMillis() + 1500
            } else if (mode == Mode.HANG) dropDown()
        }

        private fun goTo(wx: Float, y: Float, g: Goal) {
            if (mode != Mode.WALK && mode != Mode.DANCE && mode != Mode.SHY) return
            mode = Mode.WALK
            tx = wx.coerceIn(0.04f, 1f + PARALLAX - 0.04f); ty = y; goal = g
            nextWander = SystemClock.uptimeMillis() + 7000
        }

        private fun arrive() {
            val now = SystemClock.uptimeMillis()
            moving = false
            val g = goal; goal = Goal.NONE
            when (g) {
                Goal.EAT -> if (pet.feed()) { facing = -1; setMode(Mode.EAT, 3000); say("ham-ham!", 2500) }
                            else { facing = 1; setMode(Mode.SHY, 1500); say(if (pet.stage >= 4) "tokum!" else "ı-ıh", 1500) }
                Goal.SLEEP -> if (pet.sleep()) { say("iyi geceler…", 1800); DiagLog.add(applicationContext, "yatağa yattı") }
                              else say(if (pet.stage >= 4) "uykum yok!" else "ı-ıh", 1500)
                Goal.KICK -> {
                    facing = if (ballX > bx) 1 else -1
                    setMode(Mode.KICK, 450)
                    ballV = facing * (0.5f + rnd.nextFloat() * 0.4f)
                    pet.play(); say("hop!", 900)
                }
                Goal.CLIMB -> { climbFrom = floor() - 40 * u; gy = climbFrom; setMode(Mode.CLIMB, 900) }
                Goal.NONE -> { waveUntil = now + 1400; if (bubble == null || now > bubbleUntil) say(arriveWord(), 1200) }
            }
        }

        private fun dropDown() {
            by = gy + 42 * u; ty = floor()
            setMode(Mode.FALL, Long.MAX_VALUE)
        }

        private fun arriveWord() = when (pet.stage) { 0, 1 -> "agu!"; 2, 3 -> "ba-ba!"; else -> "geldim!" }

        private fun setMode(m: Mode, dur: Long) {
            val now = SystemClock.uptimeMillis()
            mode = m; modeStart = now
            modeUntil = if (dur == Long.MAX_VALUE) Long.MAX_VALUE else now + dur
        }

        private fun say(s: String, ms: Long) { bubble = s; bubbleUntil = SystemClock.uptimeMillis() + ms }

        // ---------- Güncelleme ----------

        private fun update(now: Long, dt: Float) {
            val locked = isLocked()
            if (wasLocked != locked) {
                // Kilit ↔ ana ekran geçişi: bebeği o ekranın zeminine koy.
                wasLocked = locked
                if (mode == Mode.HANG || mode == Mode.CLIMB || mode == Mode.CARRIED || mode == Mode.FALL) mode = Mode.WALK
                by = floor(); ty = by; goal = Goal.NONE
            }
            if (now - lastCatchUp > 60_000) { pet.catchUp(); lastCatchUp = now }

            // Yumurtadan çıkış
            if (!pet.hatched && hatchStart in 1..now && now - hatchStart >= HATCH_MS) {
                pet.hatch(); hatchStart = 0L
                say(if (pet.stage >= 4) "merhaba!" else "ba!", 2500); waveUntil = now + 2500
                DiagLog.add(applicationContext, "yumurtadan çıktı")
            }
            if (!pet.hatched) return

            // Top
            if (ballV != 0f) {
                ballX += ballV * dt
                ballV *= 0.985f.pow(dt * 60)
                val lo = xOff * PARALLAX + 0.06f; val hi = xOff * PARALLAX + 0.94f
                if (ballX < lo) { ballX = lo; ballV = abs(ballV) * 0.6f }
                if (ballX > hi) { ballX = hi; ballV = -abs(ballV) * 0.6f }
                if (abs(ballV) < 0.01f) ballV = 0f
            }

            when (mode) {
                Mode.CARRIED -> return
                Mode.FALL -> {
                    val sp = 1400 * S * dt
                    if (ty - by <= sp) {
                        by = ty; mode = Mode.WALK; say("hop!", 900)
                        // Kabın ya da yatağın yanına bırakıldıysa onu kullanır.
                        if (!locked && abs(bx - BOWL) < 0.1f) { goal = Goal.EAT; arrive() }
                        else if (!locked && abs(bx - BED) < 0.1f) { goal = Goal.SLEEP; arrive() }
                    } else by += sp
                    return
                }
                Mode.CLIMB -> {
                    bx = pet.batteryX + xOff * PARALLAX
                    val p = ((now - modeStart) / 900f).coerceIn(0f, 1f)
                    val top = sb + 2 * S
                    gy = climbFrom + (top - climbFrom) * (1 - (1 - p) * (1 - p))
                    if (p >= 1f) {
                        setMode(Mode.HANG, if (chargingNow) 20_000 else 9000)
                        say(if (pet.stage >= 4) "pil benim!" else "iii!", 1800)
                        DiagLog.add(applicationContext, "pile asıldı")
                    }
                    return
                }
                Mode.HANG -> {
                    bx = pet.batteryX + xOff * PARALLAX
                    gy = sb + 2 * S
                    if (now > modeUntil) dropDown()
                    return
                }
                Mode.EAT, Mode.KICK, Mode.SHY, Mode.DANCE -> { if (now > modeUntil) mode = Mode.WALK else return }
                Mode.WALK -> {}
            }

            if (pet.asleep) { moving = false; return }

            // Yürüme (dönem hızları prototiple aynı)
            val dxPx = (tx - bx) * w
            val dyPx = ty - by
            val dist = hypot(dxPx, dyPx)
            if (dist > 2f) {
                moving = true
                if (abs(dxPx) > 1f) facing = if (dxPx > 0) 1 else -1
                val step = SPEED[pet.stage.coerceIn(0, 5)] * S * dt
                if (dist <= step) { bx = tx; by = ty; arrive() }
                else { bx += dxPx / dist * step / w; by += dyPx / dist * step }
            } else if (moving) {
                arrive()
            } else if (now > nextWander) {
                // Boşta gezinme: görünen alanda, zemine yakın bir yere.
                nextWander = now + 4000 + rnd.nextLong(6000)
                val lo = xOff * PARALLAX + 0.12f
                tx = lo + rnd.nextFloat() * 0.76f
                ty = floor()
                if (!locked && chargingNow && pet.stage >= 2 && rnd.nextFloat() < 0.3f) { tx = pet.batteryX + xOff * PARALLAX; goal = Goal.CLIMB }
                else if (pet.hunger > 70 && rnd.nextFloat() < 0.3f) say(if (pet.stage >= 4) "acıktım…" else "mama?", 1800)
            }
        }

        // ---------- Çizim ----------

        private fun drawFrame() {
            if (w == 0 || h == 0) return
            val holder = surfaceHolder
            val c: Canvas = try { holder.lockCanvas() } catch (e: Exception) { null } ?: return
            try {
                frames++
                val now = SystemClock.uptimeMillis()
                val dt = if (lastT == 0L) 0f else min(0.1f, (now - lastT) / 1000f)
                lastT = now
                update(now, dt)
                val locked = isLocked()
                drawBackground(c)
                if (!locked) drawProps(c, now)
                drawBaby(c, now)
            } finally {
                holder.unlockCanvasAndPost(c)
            }
        }

        private fun drawBackground(c: Canvas) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val night = hour < 7 || hour >= 20
            if (hour != bgHour) {
                bgHour = hour
                val (top, bottom) = when {
                    night -> 0xFF0E1330.toInt() to 0xFF2A2350.toInt()
                    hour >= 17 -> 0xFF3B2F5C.toInt() to 0xFFC98276.toInt()
                    else -> 0xFF4E7FAE.toInt() to 0xFFE7B899.toInt()
                }
                bg.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), top, bottom, Shader.TileMode.CLAMP)
            }
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)
            val shift = xOff * PARALLAX * 0.5f
            if (night) for (s in stars) {
                fill.color = 0x99FFFFFF.toInt()
                c.drawCircle((s[0] - shift) * w, s[1] * h, s[2] * 1.4f * S, fill)
            }
            // Yumuşak tepe (zemin hissi)
            fill.color = if (night) 0x33000000 else 0x22FFFFFF
            rect.set((-0.3f - shift) * w, h * 0.83f, (1.6f - shift) * w, h * 1.4f)
            c.drawOval(rect, fill)
        }

        private fun sxOf(wx: Float) = (wx - xOff * PARALLAX) * w

        private fun drawProps(c: Canvas, now: Long) {
            val fy = h * FLOOR_HOME
            // Yemek kabı
            val kx = sxOf(BOWL)
            fill.color = 0xFFF4A26B.toInt()
            rect.set(kx - 20 * S, fy - 16 * S, kx + 20 * S, fy + 6 * S)
            c.drawArc(rect, 0f, 180f, true, fill)
            fill.color = if (pet.hunger > 70) 0xFF8A5A3C.toInt() else 0xFFF6D365.toInt()
            rect.set(kx - 17 * S, fy - 9 * S, kx + 17 * S, fy - 3 * S); c.drawOval(rect, fill)
            // Yatak
            val yx = sxOf(BED)
            fill.color = 0xFF9DB4E0.toInt()
            rect.set(yx - 34 * S, fy - 12 * S, yx + 34 * S, fy + 2 * S); c.drawRoundRect(rect, 7 * S, 7 * S, fill)
            fill.color = 0xFFFFFFFF.toInt()
            rect.set(yx - 30 * S, fy - 17 * S, yx - 12 * S, fy - 7 * S); c.drawRoundRect(rect, 5 * S, 5 * S, fill)
            // Top
            val bxs = sxOf(ballX); val r = 9 * S
            fill.color = 0xFFE85D5D.toInt(); c.drawCircle(bxs, fy - r, r, fill)
            stroke.color = 0xFFFFFFFF.toInt(); stroke.strokeWidth = 2.2f * S
            val rot = (ballX * 900f) % 360f
            rect.set(bxs - r * 0.7f, fy - r * 1.7f, bxs + r * 0.7f, fy - r * 0.3f)
            c.drawArc(rect, rot, 120f, false, stroke)
        }

        private fun drawBaby(c: Canvas, now: Long) {
            val st = pet.stage.coerceIn(0, 5)
            val sx = sxOf(bx)
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
                pet.energy < 25 -> Mood.SLEEPY
                pet.hunger > 70 -> Mood.HUNGRY
                else -> Mood.HAPPY
            }
            val ground = when { st == 1 -> Pose.SIT; st == 2 -> if (moving) Pose.CRAWL else Pose.SIT; else -> Pose.STAND }
            val arms = when {
                mode == Mode.DANCE -> if ((now / 400) % 2 == 0L) Arms.UP else Arms.WAVE
                now < waveUntil -> Arms.WAVE
                chargingNow && mode == Mode.WALK && !moving -> Arms.UP
                else -> Arms.NONE
            }
            val look = if (moving) facing * 0.7f else 0f
            val frame = when {
                !pet.hatched -> Frame(sx, by, u, st, Pose.EGG, Mood.HAPPY, hatch = hatch)
                pet.asleep && mode == Mode.WALK -> Frame(sx, by, u, st, Pose.LIE, Mood.SLEEP)
                mode == Mode.CARRIED -> Frame(sx, gy, u, st, Pose.DANGLE, mood, facing = facing)
                mode == Mode.CLIMB || mode == Mode.HANG -> Frame(sx, gy, u, st, Pose.HANG, mood, charging = chargingNow)
                mode == Mode.FALL -> Frame(sx, ty, u, st, ground, Mood.SURPRISE, Arms.UP, lift = ty - by)
                mode == Mode.EAT -> Frame(sx, by, u, st, Pose.EAT, Mood.HAPPY, facing = facing)
                mode == Mode.KICK -> Frame(sx, by, u, st, ground, Mood.LAUGH, kick = st >= 3, facing = facing)
                mode == Mode.DANCE -> Frame(sx, by, u, st, ground, mood, arms, moving = true, facing = facing)
                else -> Frame(sx, by, u, st, ground, mood, arms, moving, facing, lookX = look, charging = chargingNow)
            }
            val p = chibi.draw(c, frame, now)
            val b = bubble
            if (b != null && now < bubbleUntil) {
                // Asılıyken balon durum çubuğunun altında kalmasın diye yana çizilir.
                if (mode == Mode.HANG || mode == Mode.CLIMB) drawBubble(c, sx + (if (sx > w / 2) -1 else 1) * 50 * S, gy + 50 * S, b)
                else drawBubble(c, p.x, p.y, b)
            } else bubble = null
        }

        private fun drawBubble(c: Canvas, px: Float, py: Float, s: String) {
            textPaint.textSize = 15 * S
            val tw = textPaint.measureText(s)
            val pad = 9 * S
            val bh = 15 * S + pad * 1.2f
            val left = (px - tw / 2 - pad).coerceIn(6 * S, w - tw - 2 * pad - 6 * S)
            val bottom = max(py - 6 * S, sb + bh + 4 * S)
            fill.color = 0xF0FFFFFF.toInt()
            rect.set(left, bottom - bh, left + tw + 2 * pad, bottom)
            c.drawRoundRect(rect, bh / 2, bh / 2, fill)
            path.reset()
            val tx0 = px.coerceIn(left + bh / 2, left + tw + 2 * pad - bh / 2)
            path.moveTo(tx0 - 5 * S, bottom - 1); path.lineTo(tx0 + 5 * S, bottom - 1); path.lineTo(tx0, bottom + 6 * S); path.close()
            c.drawPath(path, fill)
            c.drawText(s, left + pad, bottom - bh / 2 + textPaint.textSize * 0.36f, textPaint)
        }

        // ---------- Yardımcılar ----------

        private fun floor() = h * if (isLocked()) FLOOR_LOCK else FLOOR_HOME

        private fun hitBaby(x: Float, y: Float): Boolean {
            val sx = sxOf(bx)
            val cy = when (mode) { Mode.HANG, Mode.CLIMB, Mode.CARRIED -> gy + 26 * u; else -> by - 18 * u }
            return abs(x - sx) < 20 * u && abs(y - cy) < 24 * u
        }

        private fun isLocked(): Boolean = (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
        private fun where(): String = if (isLocked()) "KİLİT EKRANI" else "ana ekran"
    }

    companion object {
        /** Duvar kağıdı ana ekran sayfaları boyunca genişliğin %30'u kadar kayar. */
        const val PARALLAX = 0.3f
        const val FLOOR_HOME = 0.80f
        const val FLOOR_LOCK = 0.955f
        const val BOWL = 0.16f
        const val BED = 0.40f
        const val HATCH_MS = 3500L
        val SPEED = floatArrayOf(34f, 46f, 62f, 80f, 96f, 112f)

        fun charging(ctx: Context): Boolean {
            val status = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }

        fun headphones(ctx: Context): Boolean {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val types = mutableSetOf(
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_USB_HEADSET,
            )
            if (Build.VERSION.SDK_INT >= 31) types += AudioDeviceInfo.TYPE_BLE_HEADSET
            return am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in types }
        }
    }
}
