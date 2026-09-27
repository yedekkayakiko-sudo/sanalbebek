package com.ortakyasam.spike

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.hypot

/**
 * Ana ekranda bebeği ikonların ÜSTÜNDE gösteren küçük pencere.
 * Duvar kağıdı ikonların arkasında kaldığı için, ikonla dolu ekranlarda bebek görünmüyordu.
 * Pencere sadece duvar kağıdı görünürken (yani ana ekrandayken) açılır; bir uygulama açılınca kapanır.
 * Kilit ekranında başka uygulamaların penceresi gösterilemez, orada bebek duvar kağıdında kalır.
 */
object OverlayPet {
    private var view: PetOverlayView? = null
    private var wm: WindowManager? = null
    private var lp: WindowManager.LayoutParams? = null
    private val wanting = HashSet<Int>()
    var permitted = false; private set

    val showing get() = view != null

    fun refreshPermission(ctx: Context) { permitted = Settings.canDrawOverlays(ctx) }

    /** Her duvar kağıdı yüzeyi (ana ekran, kilit ekranı, önizleme) pencereyi isteyip istemediğini söyler. */
    fun want(ctx: Context, key: Int, on: Boolean) {
        if (on) wanting.add(key) else wanting.remove(key)
        val should = wanting.isNotEmpty() && permitted && Baby.pet.overlayOn
        if (should && view == null) show(ctx) else if (!should && view != null) hide()
    }

    fun winW() = 100 * Baby.S
    fun winH() = 140 * Baby.S

    /** Pencerenin ekrandaki sol üst köşesi: normalde ayaklar altta, tutunurken eller üstte. */
    fun origin(): Pair<Float, Float> {
        val x = Baby.bx - winW() / 2
        val y = if (Baby.isGrip()) Baby.gy - 10 * Baby.S else Baby.by - winH() + 8 * Baby.S
        return Pair(x, y)
    }

    private fun show(ctx: Context) {
        val m = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val (x, y) = origin()
        val p = WindowManager.LayoutParams(
            winW().toInt(), winH().toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x.toInt(); this.y = y.toInt()
            if (Build.VERSION.SDK_INT >= 28) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            title = "bebek"
        }
        val v = PetOverlayView(ctx)
        try {
            m.addView(v, p)
            view = v; wm = m; lp = p
        } catch (e: Exception) {
            DiagLog.add(ctx, "ikonların üstünde gösterilemedi: ${e.javaClass.simpleName}")
            permitted = false
        }
    }

    fun hide() {
        val v = view ?: return
        try { wm?.removeView(v) } catch (_: Exception) { }
        view = null
    }

    fun place(x: Int, y: Int) {
        val p = lp ?: return
        if (p.x == x && p.y == y) return
        p.x = x; p.y = y
        try { wm?.updateViewLayout(view, p) } catch (_: Exception) { }
    }
}

@SuppressLint("ViewConstructor")
class PetOverlayView(ctx: Context) : View(ctx) {
    private var downX = 0f
    private var downY = 0f
    private var dragging = false
    private val loc = IntArray(2)

    override fun onDraw(c: Canvas) {
        val now = SystemClock.uptimeMillis()
        Baby.step(now)
        val (ox, oy) = OverlayPet.origin()
        OverlayPet.place(ox.toInt(), oy.toInt())
        // Pencerenin gerçek yeri (sistem bir kare geç taşıyabilir ya da kenarda sınırlayabilir) üzerinden çiz.
        getLocationOnScreen(loc)
        Baby.draw(c, now, loc[0].toFloat(), loc[1].toFloat(), overlay = true, winW = width.toFloat(), winH = height.toFloat())
        postInvalidateOnAnimation()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX = e.rawX; downY = e.rawY; dragging = false }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging && hypot(e.rawX - downX, e.rawY - downY) > 12 * Baby.S && Baby.canCarry()) { dragging = true; Baby.startCarry() }
                if (dragging) Baby.carryTo(e.rawX, e.rawY)
            }
            MotionEvent.ACTION_UP -> { if (dragging) Baby.drop() else Baby.tapBaby(); dragging = false }
            MotionEvent.ACTION_CANCEL -> { if (dragging) Baby.drop(); dragging = false }
        }
        return true
    }
}
