package com.ortakyasam.spike

import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

/**
 * Uygulama: bebeğin durumu, bakım, kurulum adımları ve ayarlar. Oyun burada değil, ekranda.
 * Bağımlılık yok; görünüm kodla kuruluyor.
 */
class MainActivity : Activity() {
    private lateinit var pet: Pet
    private lateinit var preview: PetPreviewView
    private lateinit var nameText: TextView
    private lateinit var stageText: TextView
    private lateinit var hungerBar: StatBar
    private lateinit var energyBar: StatBar
    private lateinit var step1: StepRow
    private lateinit var step2: StepRow
    private lateinit var overlaySwitch: Button
    private val stageButtons = ArrayList<Button>()
    private lateinit var logView: TextView
    private val density by lazy { resources.displayMetrics.density }
    private fun dp(v: Int) = (v * density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        if (Build.VERSION.SDK_INT >= 23) window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        pet = Pet(this)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(24)) }
        val scroll = ScrollView(this).apply { setBackgroundColor(BG); addView(root); isFillViewport = true }
        // Android 15 uygulamayı ekranın kenarına kadar çiziyor: durum çubuğu ve alt çubuk kadar boşluk bırak.
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            @Suppress("DEPRECATION")
            v.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
            insets
        }

        // ---- Bebek kartı ----
        val hero = card(root, 0xFFFFFFFF.toInt())
        preview = PetPreviewView(this)
        hero.addView(preview, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(210)))
        nameText = text(hero, "", 26f, INK, bold = true).apply { gravity = Gravity.CENTER; setPadding(0, dp(10), 0, 0) }
        stageText = text(hero, "", 15f, MUTED).apply { gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(10)) }
        hungerBar = StatBar(this, "Tokluk", 0xFFF4A26B.toInt()).also { hero.addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34))) }
        energyBar = StatBar(this, "Enerji", 0xFF7C9DC6.toInt()).also { hero.addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34))) }
        val care = row(hero)
        careButton(care, "🍼\nBesle") {
            when {
                pet.asleep -> toast("Uyuyor. Önce uyandır.")
                pet.feed() -> toast("Yedi! Ana ekrana dönünce yerken göreceksin.")
                else -> toast("Tok, istemiyor.")
            }
            refresh()
        }
        careButton(care, "😴\nUyut") { if (pet.sleep()) toast("Uyudu. İyi geceler.") else toast("Uykusu yok."); refresh() }
        careButton(care, "☀️\nUyandır") { if (pet.asleep) { pet.wake(); toast("Uyandı!") } else toast("Zaten uyanık."); refresh() }

        // ---- Kurulum ----
        section(root, "Ekranına koy")
        val setup = card(root, 0xFFFFFFFF.toInt())
        step1 = StepRow(this, 1, "Duvar kağıdı yap", "Bebek kilit ekranında ve ana ekranda yaşasın.", "Ayarla") { openPicker() }
        setup.addView(step1)
        step2 = StepRow(this, 2, "İkonların üstünde görünsün", "İkonla dolu ekranda da bebeği görmek için. Açılan listede \"Ortak Yaşam Test\"i bul ve izni aç.", "İzin ver") { openOverlayPermission() }
        setup.addView(step2)
        overlaySwitch = pillButton(setup, "") {
            pet.overlayOn = !pet.overlayOn; pet.save(); refresh()
        }

        // ---- Nasıl oynanır ----
        section(root, "Ana ekranda neler yapabilirsin")
        val how = card(root, 0xFFFFFFFF.toInt())
        for ((icon, line) in listOf(
            "👆" to "Boş bir yere dokun: oraya gelir.",
            "🤭" to "Üstüne dokun: gıdıklanır.",
            "✋" to "Parmağınla tutup sürükle: taşınır.",
            "🔋" to "Tutup sağ üstteki pilin yanına bırak: pile asılır. Şarja takınca kendisi koşar.",
            "👀" to "Bazen ekranın kenarına saklanır. Bulup dokun: \"ce-e!\"",
            "🍼" to "Aşağıdaki turuncu kaba dokun ya da onu kabın yanına bırak: yer.",
            "🎧" to "Kulaklık takıp ekranı aç: dans eder.",
        )) {
            val r = row(how).apply { setPadding(0, dp(6), 0, dp(6)); gravity = Gravity.CENTER_VERTICAL }
            text(r, icon, 20f, INK).apply { setPadding(0, 0, dp(12), 0) }
            text(r, line, 15f, INK)
        }

        // ---- Karakter ----
        section(root, "Karakter")
        val ch = card(root, 0xFFFFFFFF.toInt())
        text(ch, "Adı", 14f, MUTED)
        val nameRow = row(ch).apply { gravity = Gravity.CENTER_VERTICAL }
        val nameBox = EditText(this).apply {
            setText(pet.name); setSingleLine(); textSize = 17f; setTextColor(INK); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        nameRow.addView(nameBox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        pillButton(nameRow, "Kaydet") {
            pet.name = nameBox.text.toString().trim().ifEmpty { "Minik" }.take(16); pet.save(); refresh(); toast("Kaydedildi")
        }
        text(ch, "Dönem (denemek için elle seç)", 14f, MUTED).setPadding(0, dp(14), 0, dp(4))
        for (range in listOf(0..2, 3..5)) {
            val r = row(ch)
            for (i in range) {
                val b = Button(this).apply {
                    text = Pet.STAGE_NAMES[i]; isAllCaps = false; textSize = 13f
                    setOnClickListener { pet.stage = i; pet.save(); refresh() }
                }
                stageButtons.add(b)
                r.addView(b, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) })
            }
        }
        val looks = row(ch).apply { setPadding(0, dp(8), 0, 0) }
        pillButton(looks, "🎨 Yeni görünüm", weight = 1f) {
            getSharedPreferences("pet", MODE_PRIVATE).edit().putInt("seed", 0).apply(); pet = Pet(this); preview.reset(); refresh()
        }
        pillButton(looks, "🥚 Yeniden doğsun", weight = 1f) { pet.hatched = false; pet.save(); refresh(); toast("Ana ekrana dön: yumurtadan çıkacak") }

        // ---- Pil ----
        section(root, "Pil ikonu nerede?")
        val bat = card(root, 0xFFFFFFFF.toInt())
        text(bat, "Bebek pile asılınca tam altında dursun diye. Telefonunun üstündeki pil ikonu hangi taraftaysa çubuğu oraya kaydır. Çoğu telefonda en sağdadır.", 15f, INK)
        val batLabel = text(bat, "", 14f, MUTED)
        val seek = SeekBar(this).apply {
            max = 100; progress = (pet.batteryX * 100).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(b: SeekBar, v: Int, fromUser: Boolean) { batLabel.text = sideLabel(v) }
                override fun onStartTrackingTouch(b: SeekBar) {}
                override fun onStopTrackingTouch(b: SeekBar) { pet.batteryX = b.progress / 100f; pet.save() }
            })
        }
        batLabel.text = sideLabel(seek.progress)
        bat.addView(seek, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)))

        // ---- Test günlüğü ----
        section(root, "Test günlüğü")
        val lg = card(root, 0xFFFFFFFF.toInt())
        text(lg, "Denedikten sonra \"Paylaş\"a bas ve bana gönder. Hangi dokunuşun telefona ulaştığını buradan anlıyorum.", 15f, INK)
        val lr = row(lg).apply { setPadding(0, dp(8), 0, 0) }
        pillButton(lr, "📤 Paylaş", weight = 1f) { share() }
        pillButton(lr, "Göster / gizle", weight = 1f) { logView.visibility = if (logView.visibility == View.GONE) View.VISIBLE else View.GONE; refresh() }
        pillButton(lr, "Temizle", weight = 1f) { DiagLog.clear(this); refresh() }
        logView = TextView(this).apply { textSize = 11f; typeface = Typeface.MONOSPACE; setTextColor(INK); setTextIsSelectable(true); visibility = View.GONE; setPadding(0, dp(8), 0, 0) }
        lg.addView(logView)
        text(root, "Cihaz: ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}", 12f, MUTED).apply { gravity = Gravity.CENTER; setPadding(0, dp(16), 0, 0) }

        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        pet = Pet(this)
        pet.catchUp()
        preview.pet = pet
        refresh()
    }

    private fun refresh() {
        preview.pet = pet
        nameText.text = pet.name
        stageText.text = Pet.STAGE_NAMES[pet.stage] + when {
            !pet.hatched -> " · yumurtada 🥚"
            pet.asleep -> " · uyuyor 💤"
            pet.hunger > 70 -> " · acıkmış"
            pet.energy < 25 -> " · uykulu"
            else -> " · keyfi yerinde"
        }
        hungerBar.value = (100 - pet.hunger) / 100f
        energyBar.value = pet.energy / 100f
        stageButtons.forEachIndexed { i, b ->
            val on = i == pet.stage
            b.background = rounded(if (on) ACCENT else 0xFFF1E9E1.toInt(), dp(12).toFloat())
            b.setTextColor(if (on) 0xFFFFFFFF.toInt() else INK)
        }
        val wm = WallpaperManager.getInstance(this)
        val wallOn = wm.wallpaperInfo?.packageName == packageName
        step1.setDone(wallOn)
        val overlayOk = Settings.canDrawOverlays(this)
        step2.setDone(overlayOk)
        overlaySwitch.visibility = if (overlayOk) View.VISIBLE else View.GONE
        overlaySwitch.text = if (pet.overlayOn) "İkonların üstünde: AÇIK (kapatmak için dokun)" else "İkonların üstünde: KAPALI (açmak için dokun)"
        if (logView.visibility == View.VISIBLE) logView.text = DiagLog.read(this)
    }

    private fun sideLabel(v: Int) = when { v < 35 -> "Solda"; v > 65 -> "Sağda"; else -> "Ortada" } + " (%$v)"

    // ---------- Eylemler ----------

    private fun openPicker() {
        val direct = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, BebekWallpaperService::class.java))
        try { startActivity(direct) } catch (e: ActivityNotFoundException) { startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)) }
    }

    private fun openOverlayPermission() {
        try { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
        catch (e: ActivityNotFoundException) { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) }
    }

    private fun share() {
        val text = "Cihaz: ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
            "Duvar kağıdı: ${if (WallpaperManager.getInstance(this).wallpaperInfo?.packageName == packageName) "açık" else "kapalı"} · " +
            "ikon üstü izni: ${Settings.canDrawOverlays(this)} · açık: ${pet.overlayOn}\n\n${DiagLog.read(this)}"
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Test günlüğünü paylaş"))
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    // ---------- Görünüm yardımcıları ----------

    private fun rounded(color: Int, r: Float) = GradientDrawable().apply { setColor(color); cornerRadius = r }

    private fun card(parent: LinearLayout, color: Int): LinearLayout {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(color, dp(20).toFloat())
            setPadding(dp(16), dp(16), dp(16), dp(16))
            elevation = dp(2).toFloat()
        }
        parent.addView(c, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(4), 0, dp(8)) })
        return c
    }

    private fun section(parent: LinearLayout, title: String) =
        text(parent, title, 19f, INK, bold = true).apply { setPadding(dp(4), dp(18), 0, dp(6)) }

    private fun row(parent: LinearLayout): LinearLayout {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        parent.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return r
    }

    private fun text(parent: LinearLayout, s: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(this).apply { text = s; textSize = size; setTextColor(color); if (bold) setTypeface(typeface, Typeface.BOLD); setLineSpacing(0f, 1.15f) }
        val lp = when {
            parent.orientation != LinearLayout.HORIZONTAL -> LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            s.length > 3 -> LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)      // uzun yazı kalan yeri doldurur
            else -> LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        parent.addView(t, lp)
        return t
    }

    private fun pillButton(parent: LinearLayout, label: String, weight: Float = 0f, onClick: () -> Unit): Button {
        val b = Button(this).apply {
            text = label; isAllCaps = false; textSize = 14f; setTextColor(INK)
            background = rounded(0xFFF1E9E1.toInt(), dp(14).toFloat())
            setPadding(dp(12), 0, dp(12), 0)
            setOnClickListener { onClick() }
        }
        val lp = if (parent.orientation == LinearLayout.HORIZONTAL) LinearLayout.LayoutParams(if (weight > 0f) 0 else ViewGroup.LayoutParams.WRAP_CONTENT, dp(48), weight)
                 else LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48))
        lp.setMargins(dp(3), dp(6), dp(3), 0)
        parent.addView(b, lp)
        return b
    }

    private fun careButton(parent: LinearLayout, label: String, onClick: () -> Unit) {
        val b = Button(this).apply {
            text = label; isAllCaps = false; textSize = 15f; setTextColor(INK)
            background = rounded(0xFFFFF1E4.toInt(), dp(16).toFloat())
            setOnClickListener { onClick() }
        }
        parent.addView(b, LinearLayout.LayoutParams(0, dp(72), 1f).apply { setMargins(dp(4), dp(12), dp(4), 0) })
    }

    /** Kurulum adımı: numara (bitince ✓), başlık, açıklama, düğme. */
    inner class StepRow(ctx: Context, n: Int, title: String, desc: String, action: String, onClick: () -> Unit) : LinearLayout(ctx) {
        private val badge: TextView
        private val button: Button
        private val num = n.toString()
        private val actionLabel = action

        init {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
            badge = TextView(ctx).apply { gravity = Gravity.CENTER; textSize = 16f; setTypeface(typeface, Typeface.BOLD) }
            addView(badge, LayoutParams(dp(36), dp(36)).apply { setMargins(0, 0, dp(12), 0) })
            val col = LinearLayout(ctx).apply { orientation = VERTICAL }
            col.addView(TextView(ctx).apply { text = title; textSize = 16f; setTextColor(INK); setTypeface(typeface, Typeface.BOLD) })
            col.addView(TextView(ctx).apply { text = desc; textSize = 14f; setTextColor(MUTED) })
            addView(col, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            button = Button(ctx).apply {
                isAllCaps = false; textSize = 14f; setTextColor(0xFFFFFFFF.toInt())
                background = rounded(ACCENT, dp(14).toFloat()); setPadding(dp(14), 0, dp(14), 0)
                setOnClickListener { onClick() }
            }
            addView(button, LayoutParams(LayoutParams.WRAP_CONTENT, dp(44)).apply { setMargins(dp(8), 0, 0, 0) })
            setDone(false)
        }

        fun setDone(done: Boolean) {
            badge.text = if (done) "✓" else num
            badge.setTextColor(0xFFFFFFFF.toInt())
            badge.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(if (done) GOOD else ACCENT) }
            button.text = if (done) "Tamam" else actionLabel
            button.background = rounded(if (done) 0xFFE3F1E6.toInt() else ACCENT, dp(14).toFloat())
            button.setTextColor(if (done) GOOD else 0xFFFFFFFF.toInt())
        }
    }

    companion object {
        const val EXTRA_FROM_WALLPAPER = "from_wallpaper"
        val BG = 0xFFFBF4EC.toInt()
        val INK = 0xFF2D2230.toInt()
        val MUTED = 0xFF7A6B70.toInt()
        val ACCENT = 0xFFEC6E4C.toInt()
        val GOOD = 0xFF3E9B5B.toInt()
    }
}

/** Uygulamanın üstündeki canlı bebek. */
@SuppressLint("ViewConstructor")
class PetPreviewView(ctx: Context) : View(ctx) {
    var pet: Pet? = null
    private var chibi: ChibiRenderer? = null
    private val bg = Paint()
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3A2A20.toInt(); isFakeBoldText = true }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    fun reset() { chibi = null }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        bg.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0xFF4E7FAE.toInt(), 0xFFE7B899.toInt(), Shader.TileMode.CLAMP)
    }

    override fun onDraw(c: Canvas) {
        val p = pet ?: return
        if (chibi == null || chibi?.look != p.look) chibi = ChibiRenderer(p.look)
        val r = 28f * resources.displayMetrics.density
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        c.drawRoundRect(rect, r, r, bg)
        val t = SystemClock.uptimeMillis()
        val u = height / 60f
        val mood = when { p.asleep -> Mood.SLEEP; p.hunger > 70 -> Mood.HUNGRY; p.energy < 25 -> Mood.SLEEPY; else -> Mood.HAPPY }
        val pose = when { p.asleep -> Pose.LIE; p.stage <= 2 -> Pose.SIT; else -> Pose.STAND }
        val arms = if ((t / 2500) % 3 == 0L && !p.asleep) Arms.WAVE else Arms.NONE
        val frame = Frame(width / 2f, height * 0.9f, u, p.stage, if (!p.hatched) Pose.EGG else pose, mood, arms, hatch = if (p.hatched) 1f else 0f)
        val at = chibi!!.draw(c, frame, t)
        val say = when {
            !p.hatched -> "tık tık…"
            p.asleep -> "zzz"
            p.hunger > 70 -> if (p.stage >= 4) "acıktım!" else "mama?"
            p.energy < 25 -> "hıı…"
            else -> when (p.stage) { 0, 1 -> "agu!"; 2, 3 -> "ba-ba!"; else -> "merhaba!" }
        }
        text.textSize = 15f * resources.displayMetrics.density
        val tw = text.measureText(say); val pad = 10f * resources.displayMetrics.density; val bh = text.textSize + pad
        val left = (at.x - tw / 2 - pad).coerceIn(pad, width - tw - 3 * pad)
        val bottom = at.y.coerceAtLeast(bh + pad)
        fill.color = 0xF2FFFFFF.toInt()
        rect.set(left, bottom - bh, left + tw + 2 * pad, bottom)
        c.drawRoundRect(rect, bh / 2, bh / 2, fill)
        c.drawText(say, left + pad, bottom - bh / 2 + text.textSize * 0.36f, text)
        postInvalidateOnAnimation()
    }
}

/** Tokluk / enerji çubuğu. */
@SuppressLint("ViewConstructor")
class StatBar(ctx: Context, private val label: String, private val color: Int) : View(ctx) {
    var value = 0f
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2D2230.toInt() }
    private val rect = RectF()

    override fun onDraw(c: Canvas) {
        val d = resources.displayMetrics.density
        text.textSize = 14f * d
        val labelW = 70f * d
        c.drawText(label, 0f, height / 2f + text.textSize * 0.35f, text)
        val top = height / 2f - 6f * d; val bottom = height / 2f + 6f * d
        val right = width - 46f * d
        paint.color = 0xFFF1E9E1.toInt(); rect.set(labelW, top, right, bottom); c.drawRoundRect(rect, 6f * d, 6f * d, paint)
        paint.color = color; rect.set(labelW, top, labelW + (right - labelW) * value, bottom); c.drawRoundRect(rect, 6f * d, 6f * d, paint)
        c.drawText("%${(value * 100).toInt()}", right + 8f * d, height / 2f + text.textSize * 0.35f, text)
    }
}
