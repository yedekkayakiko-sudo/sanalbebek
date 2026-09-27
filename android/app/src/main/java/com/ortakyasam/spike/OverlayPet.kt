package com.ortakyasam.spike

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.Display
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
    /** Uygulamada gösterilen son durum: neden açık, neden kapalı. */
    var status = "henüz denenmedi"; private set
    private var lastReason = ""

    val showing get() = view != null

    fun refreshPermission(ctx: Context) { permitted = Settings.canDrawOverlays(ctx) }

    /** Her duvar kağıdı yüzeyi (ana ekran, kilit ekranı, önizleme) pencereyi isteyip istemediğini söyler. */
    fun want(ctx: Context, key: Int, on: Boolean) {
        if (on) wanting.add(key) else wanting.remove(key)
        val reason = when {
            wanting.isEmpty() -> ""
            !permitted -> "izin yok (Ayarlar → Diğer uygulamaların üzerinde göster)"
            !Baby.pet.overlayOn -> "uygulamada kapalı"
            else -> "ok"
        }
        if (reason != lastReason && reason.isNotEmpty() && reason != "ok") {
            DiagLog.add(ctx, "ikon üstü penceresi açılmadı: $reason")
            status = "Açılmadı: $reason"
        }
        lastReason = reason
        if (reason == "ok" && view == null) show(ctx) else if (reason != "ok" && view != null) hide()
    }

    fun winW() = 100 * Baby.S
    fun winH() = 140 * Baby.S

    /** Pencerenin ekrandaki sol üst köşesi: normalde ayaklar altta, tutunurken eller üstte. */
    fun origin(): Pair<Float, Float> {
        val x = Baby.bx - winW() / 2
        val y = if (Baby.isGrip()) Baby.gy - 10 * Baby.S else Baby.by - winH() + 8 * Baby.S
        return Pair(x, y)
    }

    /**
     * Pencere eklemek için "pencere bağlamı". Servis bağlamından doğrudan eklemek bazı Android sürümlerinde
     * sessizce başarısız olabiliyor; Android 11+ için önerilen yol createWindowContext.
     */
    private fun windowContext(ctx: Context): Context {
        if (Build.VERSION.SDK_INT < 30) return ctx
        return try {
            val dm = ctx.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = dm.getDisplay(Display.DEFAULT_DISPLAY)
            ctx.createDisplayContext(display).createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
        } catch (e: Exception) {
            ctx
        }
    }

    private fun params(x: Int, y: Int, w: Int, h: Int) = WindowManager.LayoutParams(
        w, h,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        this.x = x; this.y = y
        if (Build.VERSION.SDK_INT >= 28) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        title = "bebek"
    }

    private fun show(ctx: Context) {
        val wctx = windowContext(ctx)
        val m = wctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val (x, y) = origin()
        val p = params(x.toInt(), y.toInt(), winW().toInt(), winH().toInt())
        val v = PetOverlayView(wctx)
        try {
            m.addView(v, p)
            view = v; wm = m; lp = p
            status = "Çalışıyor: bebek ana ekranda ikonların üstünde"
            DiagLog.add(ctx, "ikon üstü penceresi AÇILDI")
        } catch (e: Exception) {
            status = "Hata: ${e.javaClass.simpleName} ${e.message ?: ""}".take(160)
            DiagLog.add(ctx, "ikon üstü penceresi HATA: ${e.javaClass.simpleName} ${e.message ?: ""}".take(200))
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

    /**
     * Uygulamadaki "Dene" düğmesi: izin varsa ekranın ortasına 4 saniyeliğine bir test bebeği koyar.
     * Görünüyorsa bu telefonda ikon üstü pencere çalışıyor demektir. Sonucu metin olarak döner.
     */
    fun selfTest(ctx: Context): String {
        if (!Settings.canDrawOverlays(ctx)) return "İzin yok. Önce \"İzin ver\"e dokunup izni aç."
        return try {
            val wctx = windowContext(ctx)
            val m = wctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val d = ctx.resources.displayMetrics
            val size = (160 * d.density).toInt()
            val v = TestBabyView(wctx)
            m.addView(v, params((d.widthPixels - size) / 2, (d.heightPixels - size) / 2, size, size))
            v.postDelayed({ try { m.removeView(v) } catch (_: Exception) { } }, 4000)
            DiagLog.add(ctx, "ikon üstü testi: pencere eklendi")
            "Ekranın ortasında 4 saniye bir bebek görmelisin. Gördüysen bu telefonda çalışıyor."
        } catch (e: Exception) {
            DiagLog.add(ctx, "ikon üstü testi HATA: ${e.javaClass.simpleName} ${e.message ?: ""}".take(200))
            "Hata: ${e.javaClass.simpleName}. Günlüğü bana gönder."
        }
    }
}

/** Test penceresinde dönen küçük bebek. */
@SuppressLint("ViewConstructor")
class TestBabyView(ctx: Context) : View(ctx) {
    private val chibi = ChibiRenderer(Pet(ctx).look)
    override fun onDraw(c: Canvas) {
        c.drawColor(0x33FFFFFF)
        chibi.draw(c, Frame(width / 2f, height * 0.92f, width / 70f, 3, Pose.STAND, Mood.LAUGH, Arms.WAVE), SystemClock.uptimeMillis())
        postInvalidateOnAnimation()
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
