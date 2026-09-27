package com.ortakyasam.spike

import android.app.KeyguardManager
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin

/**
 * Cihaz testi için en basit canlı duvar kağıdı.
 * Cevaplamak istediğimiz sorular:
 *  1. Kilit ekranında görünüyor mu, orada da hareket ediyor mu?
 *  2. Ekran kapanınca çizim duruyor mu (pil)?
 *  3. Ana ekranda boş alana dokunma duvar kağıdına ulaşıyor mu?
 *  4. Sayfa kaydırma (onOffsetsChanged) geliyor mu? Bebek öbür sayfaya saklanabilir mi?
 *  5. Bebeğe dokununca uygulama açılabiliyor mu?
 *  6. Şarj ve kulaklık durumu, arka plan işi olmadan, görünür olunca okunabiliyor mu?
 */
class BebekWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = BebekEngine()

    inner class BebekEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var w = 0
        private var h = 0
        private var xOffset = 0f
        private var lastLoggedOffset = -1f
        private var babyX = 0.5f          // dünya koordinatı: 0..1.3 (ana ekran sayfaları boyunca)
        private var targetX = 0.5f
        private var waveUntil = 0L
        private var frames = 0
        private var visibleSince = 0L

        private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 26f; color = 0x55EC6E32 }
        private val skin = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD39C74.toInt() }
        private val limb = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD39C74.toInt(); style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF7FA38A.toInt() }
        private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2B1D14.toInt(); style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2B1D14.toInt() }
        private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x99FFFFFF.toInt(); textSize = 30f }

        private val drawRunner = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) handler.postDelayed(this, 33L)   // ~30 fps, sadece görünürken
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            w = width
            h = height
            drawFrame()
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            val ctx = applicationContext
            if (v) {
                // Gerçek üründe: burada sunucudan yeniden hesaplanmış durum istenir ve sinyal kaydedilir.
                visibleSince = SystemClock.elapsedRealtime()
                frames = 0
                DiagLog.add(ctx, "GÖRÜNÜR · ${where()} · şarj: ${charging(ctx)} · kulaklık: ${headphones(ctx)}${if (isPreview) " · önizleme" else ""}")
                handler.removeCallbacks(drawRunner)
                handler.post(drawRunner)
            } else {
                handler.removeCallbacks(drawRunner)
                val secs = (SystemClock.elapsedRealtime() - visibleSince) / 1000
                DiagLog.add(ctx, "gizlendi · $secs sn görünür kaldı · $frames kare çizildi")
            }
        }

        override fun onOffsetsChanged(
            xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, xPixelOffset: Int, yPixelOffset: Int,
        ) {
            this.xOffset = xOffset
            // Sadece sayfa değişince kaydet (kaydırma sırasında yüzlerce çağrı gelir).
            if (xOffsetStep > 0f && abs(xOffset - lastLoggedOffset) >= xOffsetStep * 0.99f) {
                lastLoggedOffset = xOffset
                DiagLog.add(applicationContext, "sayfa · xOffset=${"%.2f".format(xOffset)} · adım=${"%.2f".format(xOffsetStep)}")
            }
            if (!visible) drawFrame()
        }

        override fun onCommand(action: String?, x: Int, y: Int, z: Int, extras: Bundle?, resultRequested: Boolean): Bundle? {
            if (action == WallpaperManager.COMMAND_TAP && w > 0) {
                val s = w / 400f
                val base = h * 0.78f
                val nearBaby = abs(x - babyScreenX()) < 60 * s && y > base - 150 * s && y < base
                DiagLog.add(applicationContext, "dokunma · x=$x y=$y · ${if (nearBaby) "BEBEĞE" else "boş alana"}")
                if (nearBaby) tryOpenApp()
                else targetX = (x.toFloat() / w + xOffset * PARALLAX).coerceIn(0.05f, 1.25f)
                waveUntil = SystemClock.uptimeMillis() + 1500
            }
            return super.onCommand(action, x, y, z, extras, resultRequested)
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawRunner)
        }

        private fun tryOpenApp() {
            try {
                startActivity(
                    Intent(this@BebekWallpaperService, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(MainActivity.EXTRA_FROM_WALLPAPER, true),
                )
                DiagLog.add(applicationContext, "uygulamayı açma denendi (açıldıysa bir sonraki satır MainActivity AÇILDI olur)")
            } catch (e: Exception) {
                DiagLog.add(applicationContext, "uygulama açılamadı: ${e.javaClass.simpleName}")
            }
        }

        private fun isLocked(): Boolean = (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
        private fun where(): String = if (isLocked()) "KİLİT EKRANI" else "ana ekran"
        private fun babyScreenX(): Float = (babyX - xOffset * PARALLAX) * w

        private fun drawFrame() {
            if (w == 0 || h == 0) return
            val holder = surfaceHolder
            val c: Canvas = try { holder.lockCanvas() } catch (e: Exception) { null } ?: return
            try {
                frames++
                val locked = isLocked()
                val t = SystemClock.uptimeMillis()
                val s = w / 400f
                c.drawColor(0xFF141012.toInt())
                c.drawCircle(w * 1.05f - xOffset * PARALLAX * w, h * 0.2f, w * 0.6f, arcPaint)
                c.drawCircle(-w * 0.1f - xOffset * PARALLAX * w, h * 0.95f, w * 0.9f, arcPaint)
                c.drawCircle(w * 1.55f - xOffset * PARALLAX * w, h * 0.35f, w * 0.5f, arcPaint)

                val dx = targetX - babyX
                if (abs(dx) > 0.003f) babyX += sign(dx) * min(abs(dx), 0.004f)
                val cx = babyScreenX()
                val base = if (locked) h * 0.95f else h * 0.78f     // kilit: en alt, ana ekran: dock'un üstü
                val bob = sin(t / 350.0).toFloat() * 3f * s
                val walking = abs(dx) > 0.003f
                val step = if (walking) sin(t / 110.0).toFloat() * 7f * s else 0f

                limb.strokeWidth = 11f * s
                c.drawLine(cx - 11 * s, base - 32 * s, cx - 12 * s + step, base - 4 * s, limb)
                c.drawLine(cx + 11 * s, base - 32 * s, cx + 12 * s - step, base - 4 * s, limb)
                c.drawRoundRect(RectF(cx - 26 * s, base - 82 * s + bob, cx + 26 * s, base - 28 * s), 18 * s, 18 * s, body)
                limb.strokeWidth = 9f * s
                val waving = t < waveUntil
                val wave = if (waving) sin(t / 120.0).toFloat() * 10f * s else 0f
                c.drawLine(cx - 22 * s, base - 70 * s + bob, cx - 36 * s, base - 40 * s, limb)
                c.drawLine(cx + 22 * s, base - 70 * s + bob, cx + 36 * s + wave, if (waving) base - 110 * s else base - 40 * s, limb)
                val hy = base - 112 * s + bob
                c.drawCircle(cx, hy, 32 * s, skin)
                c.drawCircle(cx - 11 * s, hy - 2 * s, 4 * s, eyePaint)
                c.drawCircle(cx + 11 * s, hy - 2 * s, 4 * s, eyePaint)
                ink.strokeWidth = 2.5f * s
                c.drawArc(RectF(cx - 8 * s, hy + 4 * s, cx + 8 * s, hy + 16 * s), 20f, 140f, false, ink)

                c.drawText(if (locked) "kilit ekranı" else "ana ekran · offset ${"%.2f".format(xOffset)}", 40f * s, h * 0.5f, label)
            } finally {
                holder.unlockCanvasAndPost(c)
            }
        }
    }

    companion object {
        /** Duvar kağıdı ana ekran sayfaları boyunca genişliğin %30'u kadar kayar. */
        const val PARALLAX = 0.3f

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
