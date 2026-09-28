package com.ortakyasam.spike

import android.app.Activity
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
 * Ayarlar: telefona kurulum adımları, ikon üstü penceresinin durumu ve testi, karakter, pil yeri, test günlüğü.
 * Bağımlılık yok; görünüm kodla kuruluyor.
 */
class SettingsActivity : Activity() {
    private lateinit var pet: Pet
    private lateinit var overlayStatus: TextView
    private lateinit var growthInfo: TextView
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

        text(root, "‹ Geri", 17f, ACCENT, bold = true).apply { setPadding(dp(4), dp(8), 0, dp(4)); setOnClickListener { finish() } }
        text(root, "Ayarlar", 28f, INK, bold = true).setPadding(dp(4), 0, 0, 0)

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
        overlayStatus = text(setup, "", 13f, MUTED).apply { setPadding(0, dp(8), 0, 0) }
        val tr = row(setup)
        pillButton(tr, "🧪 Şimdi dene", weight = 1f) { toast(OverlayPet.selfTest(this)); refresh() }
        pillButton(tr, "Uygulama bilgisi", weight = 1f) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        text(setup, "İzin düğmesi gri ve açılmıyorsa (\"kısıtlı ayar\"): \"Uygulama bilgisi\"ne gir, sağ üstteki ⋮ menüsünden \"Kısıtlı ayarlara izin ver\"i seç, sonra izni tekrar aç.", 13f, MUTED).setPadding(0, dp(6), 0, 0)

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
        val looks = row(ch).apply { setPadding(0, dp(8), 0, 0) }
        pillButton(ch, "🔄 Karakteri değiştir (bebek / evcil hayvan)") {
            android.app.AlertDialog.Builder(this).setTitle("Karakteri değiştir")
                .setMessage("Yeni karakter bugün yeniden doğar. Şimdiki karakterin büyümesi ve eşyaları sıfırlanır.")
                .setPositiveButton("Değiştir") { _, _ -> startActivity(Intent(this, ChooseActivity::class.java)) }
                .setNegativeButton("Vazgeç", null).show()
        }
        pillButton(looks, "🎨 Yeni görünüm", weight = 1f) {
            getSharedPreferences("pet", MODE_PRIVATE).edit().putInt("seed", 0).apply(); pet = Pet(this); refresh()
        }

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

        // ---- Geliştirici ----
        section(root, "Test araçları")
        val dev = card(root, 0xFFFFFFFF.toInt())
        text(dev, "Bebek gerçek zamanla büyür; bunlar sadece denemek için. Ürünün kendisinde olmayacak.", 14f, MUTED)
        growthInfo = text(dev, "", 14f, INK).apply { setPadding(0, dp(8), 0, dp(4)) }
        val tr1 = row(dev)
        pillButton(tr1, "+1 gün", weight = 1f) { pet.catchUp(); pet.devAddDays(1f); refresh() }
        pillButton(tr1, "+7 gün", weight = 1f) { pet.catchUp(); pet.devAddDays(7f); refresh() }
        pillButton(tr1, "+30 gün", weight = 1f) { pet.catchUp(); pet.devAddDays(30f); refresh() }
        text(dev, "Döneme atla:", 14f, MUTED).setPadding(0, dp(10), 0, dp(2))
        for (range in listOf(0..2, 3..5)) {
            val r = row(dev)
            for (i in range) {
                val b = Button(this).apply {
                    text = Pet.STAGE_NAMES[i]; isAllCaps = false; textSize = 13f
                    setOnClickListener { pet.catchUp(); pet.devAddDays(Pet.STAGE_DAYS[i] + 0.01f - pet.growth); refresh() }
                }
                stageButtons.add(b)
                r.addView(b, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) })
            }
        }
        val tr2 = row(dev)
        pillButton(tr2, "🤒 Hasta yap", weight = 1f) { pet.sick = true; pet.save(); toast("Revire götür 🩺") }
        pillButton(tr2, "🍗 Acıktır", weight = 1f) { pet.hunger = 85f; pet.asleep = false; pet.save(); refresh() }
        pillButton(tr2, "🪙 +100", weight = 1f) { pet.earn(100); refresh() }
        pillButton(dev, "🧠 Örnek düzen verisi doldur (5 gün)") { Rhythm.devFill(this); toast("Konuş ekranındaki \"seni tanıyor\" kartına bak") }
        pillButton(dev, "🥚 Sıfırla: bugün yeniden doğsun") {
            pet.devReset(); pet = Pet(this); refresh(); toast("Yeniden doğdu. Ana ekrana dön ya da uygulamayı aç.")
        }

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
        refresh()
    }

    private fun refresh() {
        growthInfo.text = "Yaş: ${pet.ageDays().toInt()} gün · Büyüme: ${"%.1f".format(pet.growth)} gün · ${pet.stageName()}" +
            (if (pet.sick) " · hasta" else "") + " · 🪙 ${pet.coins}"
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
        overlayStatus.text = "Durum: " + when {
            !overlayOk -> "izin verilmedi"
            !pet.overlayOn -> "kapalı"
            else -> OverlayPet.status
        }
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
        val BG = 0xFFFBF4EC.toInt()
        val INK = 0xFF2D2230.toInt()
        val MUTED = 0xFF7A6B70.toInt()
        val ACCENT = 0xFFEC6E4C.toInt()
        val GOOD = 0xFF3E9B5B.toInt()
    }
}
