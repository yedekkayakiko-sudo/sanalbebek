package com.ortakyasam.spike

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * İlk açılış: "Kim olsun?" Bebek ya da kendi evcil hayvanın (köpek, kedi, tavşan).
 * Evcil hayvanda fotoğraf çekilir ya da seçilir; telefonun kendisi fotoğraftan tüy renklerini çıkarır.
 * Fotoğraf hiçbir yere gönderilmez ve saklanmaz; sadece renkler kaydedilir.
 */
class ChooseActivity : Activity() {
    private val density by lazy { resources.displayMetrics.density }
    private fun dp(v: Int) = (v * density).toInt()

    private var species = 1
    private var fur = 0xFFD9A066.toInt()
    private var fur2 = 0xFF8A5A3C.toInt()
    private var earStyle = 0
    private var pattern = 0
    private var photoColors = listOf<Int>()

    private lateinit var root: LinearLayout
    private lateinit var preview: LookPreview
    private lateinit var photoView: ImageView
    private lateinit var furRow: LinearLayout
    private lateinit var fur2Row: LinearLayout
    private lateinit var earRow: LinearLayout
    private lateinit var nameBox: EditText
    private val speciesButtons = ArrayList<Button>()
    private val earButtons = ArrayList<Button>()
    private val patternButtons = ArrayList<Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        showStart()
    }

    private fun frame(): LinearLayout {
        val r = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(16), dp(20), dp(28)) }
        val sv = ScrollView(this).apply { setBackgroundColor(BG); addView(r) }
        sv.setOnApplyWindowInsetsListener { v, insets ->
            @Suppress("DEPRECATION")
            v.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
            insets
        }
        setContentView(sv)
        return r
    }

    // ---------- 1. Kim olsun? ----------

    private fun showStart() {
        root = frame()
        label("Kim olsun?", 30f, bold = true).setPadding(0, dp(24), 0, dp(6))
        label("Bir kere seçiyorsun. Seçtiğin karakter bugün doğacak ve seninle birlikte büyüyecek.", 16f, color = MUTED)
        bigChoice("👶", "Bebek", "Minik bir bebek. Emekler, yürür, konuşmayı öğrenir.") { showBaby() }
        bigChoice("🐾", "Kendi evcil hayvanım", "Köpeğinin, kedinin ya da tavşanının fotoğrafını çek; ona benzeyen bir yavru doğsun.") { showPet() }
    }

    private fun bigChoice(emoji: String, title: String, desc: String, onClick: () -> Unit) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = rounded(0xFFFFFFFF.toInt(), dp(24).toFloat()); elevation = dp(3).toFloat()
            setPadding(dp(18), dp(20), dp(18), dp(20))
            setOnClickListener { onClick() }
        }
        card.addView(TextView(this).apply { text = emoji; textSize = 44f; setPadding(0, 0, dp(16), 0) })
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(TextView(this).apply { text = title; textSize = 20f; setTextColor(INK); setTypeface(typeface, Typeface.BOLD) })
        col.addView(TextView(this).apply { text = desc; textSize = 14f; setTextColor(MUTED) })
        card.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(18), 0, 0) })
    }

    // ---------- 2a. Bebek ----------

    private fun showBaby() {
        root = frame()
        back { showStart() }
        label("👶 Bebeğinin adı ne olsun?", 24f, bold = true).setPadding(0, dp(12), 0, dp(12))
        nameBox = nameField("Minik")
        val existing = Pet(this)
        primary("Doğsun! 👶") {
            val name = nameBox.text.toString().trim().ifEmpty { "Minik" }.take(16)
            if (!existing.isPet && existing.growth > 0f) {
                // Zaten bir bebek varsa sıfırlama; sadece adını güncelle.
                existing.name = name; existing.save(); existing.markChosen()
            } else Pet(this).rebirth(name, 0)
            done()
        }
    }

    // ---------- 2b. Evcil hayvan ----------

    private fun showPet() {
        root = frame()
        back { showStart() }
        label("🐾 Evcil hayvanın", 24f, bold = true).setPadding(0, dp(8), 0, dp(4))
        label("1. Hangisi?", 15f, bold = true).setPadding(0, dp(12), 0, dp(4))
        val sr = row()
        speciesButtons.clear()
        for ((i, pair) in listOf("🐶 Köpek", "🐱 Kedi", "🐰 Tavşan").withIndex()) {
            speciesButtons.add(chip(sr, pair) { species = i + 1; update() })
        }

        label("2. Fotoğrafını çek ya da seç", 15f, bold = true).setPadding(0, dp(16), 0, dp(4))
        label("Fotoğraf telefonundan çıkmaz ve saklanmaz. Sadece tüy renkleri alınır.", 13f, color = MUTED)
        val pr = row()
        chip(pr, "📷 Fotoğraf çek") { takePhoto() }
        chip(pr, "🖼️ Galeriden seç") { pickPhoto() }

        val pv = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        preview = LookPreview(this)
        pv.addView(preview, LinearLayout.LayoutParams(0, dp(220), 1f))
        photoView = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; visibility = View.GONE; clipToOutline = true; background = rounded(0xFFFFFFFF.toInt(), dp(16).toFloat()) }
        pv.addView(photoView, LinearLayout.LayoutParams(dp(96), dp(96)).apply { setMargins(dp(8), 0, 0, 0) })
        root.addView(pv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(12), 0, 0) })

        label("3. Renkleri düzelt (istersen)", 15f, bold = true).setPadding(0, dp(12), 0, dp(4))
        label("Ana tüy rengi", 13f, color = MUTED)
        furRow = row()
        label("İkinci renk (kulak, leke)", 13f, color = MUTED).setPadding(0, dp(6), 0, 0)
        fur2Row = row()
        earRow = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(earRow)
        earRow.addView(TextView(this).apply { text = "Kulakları"; textSize = 13f; setTextColor(MUTED); setPadding(0, dp(6), 0, 0) })
        val er = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        earRow.addView(er)
        earButtons.clear()
        earButtons.add(chip(er, "Sarkık") { earStyle = 0; update() })
        earButtons.add(chip(er, "Dik") { earStyle = 1; update() })
        label("Deseni", 13f, color = MUTED).setPadding(0, dp(6), 0, 0)
        val patr = row()
        patternButtons.clear()
        patternButtons.add(chip(patr, "Düz") { pattern = 0; update() })
        patternButtons.add(chip(patr, "Göz lekesi") { pattern = 1; update() })
        patternButtons.add(chip(patr, "Beyaz ağız") { pattern = 2; update() })

        label("4. Adı", 15f, bold = true).setPadding(0, dp(16), 0, dp(4))
        nameBox = nameField("")
        primary("Doğsun! 🐾") {
            val name = nameBox.text.toString().trim().ifEmpty { listOf("Karamel", "Pamuk", "Boncuk")[species - 1] }.take(16)
            Pet(this).rebirth(name, species, fur, fur2, earStyle, pattern)
            done()
        }
        update()
    }

    private fun update() {
        speciesButtons.forEachIndexed { i, b -> select(b, i + 1 == species) }
        earButtons.forEachIndexed { i, b -> select(b, i == earStyle) }
        patternButtons.forEachIndexed { i, b -> select(b, i == pattern) }
        earRow.visibility = if (species == 1) View.VISIBLE else View.GONE
        swatches(furRow, (photoColors + PALETTE).distinct().take(9), fur) { fur = it; update() }
        swatches(fur2Row, (photoColors.drop(1) + PALETTE2).distinct().take(9), fur2) { fur2 = it; update() }
        preview.look = Look(fur, fur2, 0xFF3A2A20.toInt(), fur, 0, species, fur2, earStyle, pattern)
    }

    private fun swatches(r: LinearLayout, colors: List<Int>, current: Int, onPick: (Int) -> Unit) {
        r.removeAllViews()
        for (col in colors) {
            val v = View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL; setColor(col)
                    setStroke(dp(if (col == current) 4 else 1), if (col == current) ACCENT else 0x33000000)
                }
                setOnClickListener { onPick(col) }
            }
            r.addView(v, LinearLayout.LayoutParams(dp(34), dp(34)).apply { setMargins(dp(3), dp(4), dp(3), dp(4)) })
        }
    }

    // ---------- Fotoğraf ----------

    private fun takePhoto() {
        try { startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQ_CAMERA) }
        catch (e: ActivityNotFoundException) { toast("Kamera açılamadı, galeriden seç.") }
    }

    private fun pickPhoto() {
        try { startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), REQ_GALLERY) }
        catch (e: ActivityNotFoundException) { toast("Galeri açılamadı.") }
    }

    @Deprecated("Activity sonucu; bağımlılık eklememek için eski API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        @Suppress("DEPRECATION")
        val thumb = data.extras?.get("data") as? Bitmap
        val bmp: Bitmap? = when (requestCode) {
            REQ_CAMERA -> thumb
            REQ_GALLERY -> data.data?.let { load(it) }
            else -> null
        }
        if (bmp == null) { toast("Fotoğraf okunamadı, galeriden dene."); return }
        photoView.setImageBitmap(bmp); photoView.visibility = View.VISIBLE
        photoColors = furColors(bmp)
        if (photoColors.isNotEmpty()) { fur = photoColors[0]; fur2 = photoColors.getOrElse(1) { darken(fur) } }
        toast("Renkler fotoğraftan alındı ✨")
        update()
    }

    private fun load(uri: Uri): Bitmap? = try {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
        var sample = 1
        while (max(o.outWidth, o.outHeight) / sample > 400) sample *= 2
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
    } catch (e: Exception) { null }

    /**
     * Fotoğrafın ortasındaki baskın renkler (hayvan genelde ortadadır). Ortaya yakın pikseller daha
     * ağır sayılır; çim yeşili ve çok koyu/çok açık arka plan daha az sayılır.
     */
    private fun furColors(b: Bitmap): List<Int> {
        val w = b.width; val h = b.height
        val step = max(1, min(w, h) / 70)
        val count = HashMap<Int, Float>(); val sumR = HashMap<Int, Float>(); val sumG = HashMap<Int, Float>(); val sumB = HashMap<Int, Float>()
        var y = (h * 0.15f).toInt()
        while (y < h * 0.9f) {
            var x = (w * 0.15f).toInt()
            while (x < w * 0.85f) {
                val px = b.getPixel(x, y)
                val r = (px shr 16) and 0xFF; val g = (px shr 8) and 0xFF; val bl = px and 0xFF
                val dx = (x - w / 2f) / (w / 2f); val dy = (y - h / 2f) / (h / 2f)
                var wt = max(0.15f, 1f - sqrt(dx * dx + dy * dy))
                if (g > r + 25 && g > bl + 15) wt *= 0.2f                      // çim
                val lum = (r + g + bl) / 3
                if (lum < 18 || lum > 245) wt *= 0.5f
                val key = ((r shr 5) shl 6) or ((g shr 5) shl 3) or (bl shr 5)
                count[key] = (count[key] ?: 0f) + wt
                sumR[key] = (sumR[key] ?: 0f) + r * wt; sumG[key] = (sumG[key] ?: 0f) + g * wt; sumB[key] = (sumB[key] ?: 0f) + bl * wt
                x += step
            }
            y += step
        }
        val sorted = count.entries.sortedByDescending { it.value }
        val out = ArrayList<Int>()
        for (e in sorted) {
            val n = e.value
            val c = (0xFF shl 24) or ((sumR[e.key]!! / n).toInt() shl 16) or ((sumG[e.key]!! / n).toInt() shl 8) or (sumB[e.key]!! / n).toInt()
            if (out.all { dist(it, c) > 70 }) out.add(c)
            if (out.size >= 4) break
        }
        return out
    }

    private fun dist(a: Int, b: Int): Int {
        val dr = ((a shr 16) and 0xFF) - ((b shr 16) and 0xFF); val dg = ((a shr 8) and 0xFF) - ((b shr 8) and 0xFF); val db = (a and 0xFF) - (b and 0xFF)
        return abs(dr) + abs(dg) + abs(db)
    }

    private fun darken(c: Int): Int {
        val r = ((c shr 16) and 0xFF) * 6 / 10; val g = ((c shr 8) and 0xFF) * 6 / 10; val b = (c and 0xFF) * 6 / 10
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    // ---------- Bitiş ----------

    private fun done() {
        toast("Doğdu! 🎉 Ana ekrana dönünce de göreceksin.")
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    @Deprecated("Geri tuşu")
    override fun onBackPressed() {
        if (Pet(this).chosen) super.onBackPressed() else finishAffinity()
    }

    // ---------- Görünüm yardımcıları ----------

    private fun rounded(color: Int, r: Float) = GradientDrawable().apply { setColor(color); cornerRadius = r }

    private fun label(s: String, size: Float, bold: Boolean = false, color: Int = INK): TextView {
        val t = TextView(this).apply { text = s; textSize = size; setTextColor(color); if (bold) setTypeface(typeface, Typeface.BOLD); setLineSpacing(0f, 1.15f) }
        root.addView(t)
        return t
    }

    private fun row(): LinearLayout {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        root.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return r
    }

    private fun chip(parent: LinearLayout, s: String, onClick: () -> Unit): Button {
        val b = Button(this).apply { text = s; isAllCaps = false; textSize = 14f; setTextColor(INK); setOnClickListener { onClick() } }
        select(b, false)
        parent.addView(b, LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(3), dp(4), dp(3), dp(4)) })
        return b
    }

    private fun select(b: Button, on: Boolean) {
        b.background = rounded(if (on) ACCENT else 0xFFF1E9E1.toInt(), dp(14).toFloat())
        b.setTextColor(if (on) 0xFFFFFFFF.toInt() else INK)
    }

    private fun back(onClick: () -> Unit) {
        label("‹ Geri", 17f, bold = true, color = ACCENT).apply { setPadding(0, dp(8), 0, dp(4)); setOnClickListener { onClick() } }
    }

    private fun nameField(value: String): EditText {
        val e = EditText(this).apply {
            setText(value); hint = "Adı"; setSingleLine(); textSize = 18f; setTextColor(INK)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        root.addView(e)
        return e
    }

    private fun primary(s: String, onClick: () -> Unit) {
        val b = Button(this).apply {
            text = s; isAllCaps = false; textSize = 18f; setTextColor(0xFFFFFFFF.toInt()); setTypeface(typeface, Typeface.BOLD)
            background = rounded(ACCENT, dp(20).toFloat()); setOnClickListener { onClick() }
        }
        root.addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)).apply { setMargins(0, dp(22), 0, 0) })
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    companion object {
        private const val REQ_CAMERA = 1
        private const val REQ_GALLERY = 2
        val BG = 0xFFFBF4EC.toInt()
        val INK = 0xFF2D2230.toInt()
        val MUTED = 0xFF7A6B70.toInt()
        val ACCENT = 0xFFEC6E4C.toInt()
        private val PALETTE = listOf(0xFFD9A066, 0xFFF3E3C8, 0xFF3B2B22, 0xFF8A5A3C, 0xFFB0B0B0, 0xFFF08A3C, 0xFFFFFFFF, 0xFF2A2A2A).map { it.toInt() }
        private val PALETTE2 = listOf(0xFF8A5A3C, 0xFF3B2B22, 0xFFFFFFFF, 0xFFD9A066, 0xFF6B6B6B, 0xFFB5532E, 0xFF2A2A2A).map { it.toInt() }
    }
}

/** Seçim ekranındaki canlı önizleme. */
@SuppressLint("ViewConstructor")
class LookPreview(ctx: Context) : View(ctx) {
    var look: Look? = null
        set(v) { field = v; chibi = v?.let { ChibiRenderer(it) } }
    private var chibi: ChibiRenderer? = null
    private val bg = Paint()
    private val rect = RectF()

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        bg.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0xFFBFE3F5.toInt(), 0xFFFBE3D0.toInt(), Shader.TileMode.CLAMP)
    }

    override fun onDraw(c: Canvas) {
        val r = 24f * resources.displayMetrics.density
        rect.set(0f, 0f, width.toFloat(), height.toFloat()); c.drawRoundRect(rect, r, r, bg)
        val t = SystemClock.uptimeMillis()
        val arms = if ((t / 2200) % 3 == 0L) Arms.WAVE else Arms.NONE
        chibi?.draw(c, Frame(width / 2f, height * 0.9f, height / 62f, 3, Pose.STAND, Mood.HAPPY, arms), t)
        postInvalidateOnAnimation()
    }
}
