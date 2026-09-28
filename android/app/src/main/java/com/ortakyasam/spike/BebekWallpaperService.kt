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
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.WindowManager
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

/**
 * Canlı duvar kağıdı: arka plan, ana ekrandaki kap/yatak/top ve kilit ekranındaki bebek.
 * Ana ekranda bebek, izin verildiyse ikonların üstündeki küçük pencerede (OverlayPet) çizilir;
 * izin yoksa burada, ikonların arkasında çizilir. Bebeğin durumu tek: Baby.
 * Ekran kapalıyken hiçbir şey çizilmez. Cihaz testi için günlüğe (DiagLog) yazar.
 */
class BebekWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = BebekEngine()

    inner class BebekEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val key = System.identityHashCode(this)
        private var visible = false
        private var sw = 0                  // yüzey boyutu
        private var sh = 0
        private var dw = 0                  // ekran boyutu
        private var S = 1f
        private var xOff = 0f
        private var lastLoggedOffset = -1f
        private var frames = 0
        private var visibleSince = 0L

        // Duvar kağıdına gelen sürükleme (ikonların üstündeki pencere kapalıyken)
        private var downX = 0f
        private var downY = 0f
        private var downOnBaby = false
        private var carrying = false
        private var touchMoves = 0
        private var lastDragEnd = 0L

        private val bg = Paint()
        private var bgHour = -1
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rect = RectF()
        private val stars = List(40) { floatArrayOf(Random.nextFloat() * 1.3f, Random.nextFloat() * 0.6f, 0.5f + Random.nextFloat()) }

        private val drawRunner = object : Runnable {
            override fun run() {
                drawFrame()
                if (!visible) return
                val slow = OverlayPet.showing && !Baby.locked            // bebek pencerede; burada sadece arka plan ve top
                val asleep = Baby.pet.asleep && Baby.mode == Baby.Mode.WALK
                handler.postDelayed(this, if (slow) 66L else if (asleep) 200L else 33L)
            }
        }

        private val powerReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, i: Intent) {
                val on = i.action == Intent.ACTION_POWER_CONNECTED
                if (!isPreview) DiagLog.add(applicationContext, if (on) "ŞARJA TAKILDI" else "şarjdan çıkarıldı")
                Baby.onPower(on)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            Baby.init(applicationContext)
            val f = IntentFilter().apply { addAction(Intent.ACTION_POWER_CONNECTED); addAction(Intent.ACTION_POWER_DISCONNECTED) }
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(powerReceiver, f, Context.RECEIVER_EXPORTED) else registerReceiver(powerReceiver, f)
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawRunner)
            OverlayPet.want(applicationContext, key, false)
            try { unregisterReceiver(powerReceiver) } catch (_: Exception) { }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            sw = width; sh = height
            val (w, h) = screenSize()
            dw = w
            S = w / 400f
            Baby.setScreen(w, h, statusBarHeight())
            bgHour = -1
            drawFrame()
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            val ctx = applicationContext
            if (v) {
                visibleSince = SystemClock.elapsedRealtime()
                frames = 0
                Baby.reload(ctx)
                Baby.charging = charging(ctx)
                Baby.setLocked(isLocked())
                OverlayPet.refreshPermission(ctx)
                val hp = headphones(ctx)
                val pet = Baby.pet
                if (!isPreview) DiagLog.add(ctx, "GÖRÜNÜR · ${where()} · şarj: ${Baby.charging} · kulaklık: $hp · ikon üstü izni: ${OverlayPet.permitted} · ${pet.stageName()} · açlık ${pet.hunger.toInt()} enerji ${pet.energy.toInt()}${if (pet.asleep) " · uyuyor" else ""}")
                if (!pet.hatched && !isPreview && Baby.hatchStart == 0L) Baby.hatchStart = SystemClock.uptimeMillis() + 600
                else if (hp) Baby.startDance()
                else Baby.sayHungryIfNeeded()
                syncOverlay()
                handler.removeCallbacks(drawRunner)
                handler.post(drawRunner)
            } else {
                handler.removeCallbacks(drawRunner)
                OverlayPet.want(ctx, key, false)
                if (carrying) { carrying = false; Baby.drop() }
                if (!isPreview) {
                    val secs = (SystemClock.elapsedRealtime() - visibleSince) / 1000
                    DiagLog.add(ctx, "gizlendi · $secs sn görünür kaldı · $frames kare çizildi")
                }
            }
        }

        /** Ana ekrandaysak (görünür ve kilitsiz) bebek ikonların üstündeki pencerede gösterilir. */
        private fun syncOverlay() = OverlayPet.want(applicationContext, key, visible && !isPreview && !Baby.locked)

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

        override fun onCommand(action: String?, x: Int, y: Int, z: Int, extras: Bundle?, resultRequested: Boolean): Bundle? {
            if (action == WallpaperManager.COMMAND_TAP && sw > 0 && SystemClock.uptimeMillis() - lastDragEnd > 400) {
                Baby.tapEmpty(x.toFloat(), y.toFloat())
            }
            return super.onCommand(action, x, y, z, extras, resultRequested)
        }

        override fun onTouchEvent(event: MotionEvent) {
            // Pencere açıkken bebeği o taşır. Açık değilse launcher sürüklemeyi iletirse burada taşınır.
            if (OverlayPet.showing) { super.onTouchEvent(event); return }
            val x = event.x - originX()
            val y = event.y
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { touchMoves = 0; downX = x; downY = y; downOnBaby = Baby.canCarry() && Baby.hit(x, y) }
                MotionEvent.ACTION_MOVE -> {
                    touchMoves++
                    if (downOnBaby && !carrying && hypot(x - downX, y - downY) > 14 * S) { carrying = true; Baby.startCarry() }
                    if (carrying) Baby.carryTo(x, y)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (carrying) { carrying = false; lastDragEnd = SystemClock.uptimeMillis(); Baby.drop() }
                    else if (touchMoves > 3 && !isPreview) DiagLog.add(applicationContext, "sürükleme olayı geldi · $touchMoves hareket")
                    downOnBaby = false
                }
            }
            super.onTouchEvent(event)
        }

        // ---------- Çizim ----------

        /** Yüzey ekrandan genişse (eski launcher'lar), görünen kısım sayfa kaydırmayla kayar. */
        private fun originX(): Float = if (sw > dw) (sw - dw) * xOff else 0f

        private fun drawFrame() {
            if (sw == 0 || sh == 0) return
            val holder = surfaceHolder
            val c: Canvas = try { holder.lockCanvas() } catch (e: Exception) { null } ?: return
            try {
                frames++
                val now = SystemClock.uptimeMillis()
                Baby.setLocked(isLocked())
                if (visible) syncOverlay()
                Baby.step(now)
                drawBackground(c)
                val ox = -originX()
                if (!Baby.locked) Baby.drawProps(c, ox)
                if (isPreview || Baby.locked || !OverlayPet.showing) Baby.draw(c, now, ox, 0f, overlay = false)
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
                bg.shader = LinearGradient(0f, 0f, 0f, sh.toFloat(), top, bottom, Shader.TileMode.CLAMP)
            }
            c.drawRect(0f, 0f, sw.toFloat(), sh.toFloat(), bg)
            val shift = xOff * 0.15f
            if (night) {
                fill.color = 0x99FFFFFF.toInt()
                for (s in stars) c.drawCircle((s[0] - shift) * sw, s[1] * sh, s[2] * 1.4f * S, fill)
            }
            fill.color = if (night) 0x33000000 else 0x22FFFFFF
            rect.set((-0.3f - shift) * sw, sh * 0.83f, (1.6f - shift) * sw, sh * 1.4f)
            c.drawOval(rect, fill)
        }

        // ---------- Yardımcılar ----------

        private fun screenSize(): Pair<Int, Int> {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            return if (Build.VERSION.SDK_INT >= 30) {
                val b = wm.maximumWindowMetrics.bounds
                Pair(b.width(), b.height())
            } else {
                val m = DisplayMetrics()
                @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(m)
                Pair(m.widthPixels, m.heightPixels)
            }
        }

        private fun statusBarHeight(): Float {
            val id = resources.getIdentifier("status_bar_height", "dimen", "android")
            return if (id > 0) resources.getDimensionPixelSize(id).toFloat() else 24 * resources.displayMetrics.density
        }

        private fun isLocked(): Boolean = (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
        private fun where(): String = if (isLocked()) "KİLİT EKRANI" else "ana ekran"
    }

    companion object {
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
