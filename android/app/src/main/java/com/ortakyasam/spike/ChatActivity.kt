package com.ortakyasam.spike

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Bebekle konuşma. Şimdilik sunucusuz: LocalBrain kurallarla cevap verir.
 * Üstte canlı karakter ve "Seni tanıyor" kartı (öğrenilen düzen ve kapılan kelimeler).
 */
class ChatActivity : Activity() {
    private val density by lazy { resources.displayMetrics.density }
    private fun dp(v: Int) = (v * density).toInt()
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var pet: Pet
    private lateinit var face: ChatFace
    private lateinit var list: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var knows: TextView
    private var typing: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        window.navigationBarColor = 0xFFFFFFFF.toInt()
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        pet = Pet(this).also { it.catchUp() }

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BG) }
        root.setOnApplyWindowInsetsListener { v, insets ->
            @Suppress("DEPRECATION")
            v.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
            insets
        }

        // Üst: geri, karakter, ad
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(6), dp(12), dp(6)) }
        top.addView(TextView(this).apply { text = "‹"; textSize = 30f; setTextColor(ACCENT); setPadding(dp(4), 0, dp(12), 0); setOnClickListener { finish() } })
        face = ChatFace(this, pet)
        top.addView(face, LinearLayout.LayoutParams(dp(64), dp(64)))
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
        col.addView(TextView(this).apply { text = pet.name; textSize = 19f; setTextColor(INK); setTypeface(typeface, Typeface.BOLD) })
        col.addView(TextView(this).apply { text = "${pet.stageName()} · ${pet.ageDays().toInt()} günlük"; textSize = 13f; setTextColor(MUTED) })
        top.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(top)

        scroll = ScrollView(this).apply { isFillViewport = true }
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(4), dp(12), dp(12)) }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // "Seni tanıyor" kartı
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = rounded(0xFFFFFFFF.toInt(), dp(18).toFloat()); setPadding(dp(14), dp(12), dp(14), dp(12)); elevation = dp(1).toFloat()
        }
        card.addView(TextView(this).apply { text = "🧠 ${pet.name} seni tanıyor"; textSize = 15f; setTextColor(INK); setTypeface(typeface, Typeface.BOLD) })
        knows = TextView(this).apply { textSize = 14f; setTextColor(INK); setLineSpacing(0f, 1.2f); setPadding(0, dp(6), 0, 0) }
        card.addView(knows)
        card.addView(TextView(this).apply {
            text = "Deneme sürümü: gerçek yapay zekâ yok, bebek hazır kurallarla cevap veriyor. Hiçbir şey telefondan çıkmıyor."
            textSize = 11f; setTextColor(MUTED); setPadding(0, dp(8), 0, 0)
        })
        list.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(10)) })

        // Hazır cevaplar
        val chipsScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(8), dp(4), dp(8), dp(4)) }
        for (s in listOf("Seni seviyorum 💗", "Moralim bozuk 😔", "Sınavım var, stresliyim", "Bugün harika geçti!", "Çok yorgunum", "Acıktın mı?", "Ne yapıyorsun?", "Adın ne?")) {
            chips.addView(Button(this).apply {
                text = s; isAllCaps = false; textSize = 13f; setTextColor(INK); background = rounded(0xFFFFE3D6.toInt(), dp(16).toFloat())
                setPadding(dp(12), 0, dp(12), 0); setOnClickListener { send(s) }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        chipsScroll.addView(chips)
        root.addView(chipsScroll)

        // Yazma alanı
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(dp(10), dp(8), dp(10), dp(8)) }
        input = EditText(this).apply {
            hint = "${pet.name}'e bir şey yaz…"; textSize = 16f; setTextColor(INK); background = rounded(0xFFF4EEE8.toInt(), dp(22).toFloat())
            setPadding(dp(16), dp(10), dp(16), dp(10)); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_SEND
            setOnEditorActionListener { _, id, _ -> if (id == EditorInfo.IME_ACTION_SEND) { sendInput(); true } else false }
        }
        bar.addView(input, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(Button(this).apply {
            text = "➤"; textSize = 20f; setTextColor(0xFFFFFFFF.toInt()); background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(ACCENT) }
            setOnClickListener { sendInput() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)).apply { setMargins(dp(8), 0, 0, 0) })
        root.addView(bar)
        setContentView(root)

        // Geçmiş
        for (line in loadHistory()) addBubble(line.drop(2), line.startsWith("U|"), line.startsWith("S|"))
        if (list.childCount == 1) greet()
        refreshKnows()
    }

    private fun greet() {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val msg = LocalBrain.reply(this, pet, if (hour in 5..11) "günaydın" else "merhaba").text
        addBubble(msg, mine = false); save("B", msg)
    }

    private fun sendInput() {
        val s = input.text.toString().trim()
        if (s.isEmpty()) return
        input.setText("")
        send(s)
    }

    private fun send(s: String) {
        addBubble(s, mine = true); save("U", s)
        typing?.let { list.removeView(it) }
        typing = addBubble("…", mine = false)
        face.talk()
        // Düşünüyormuş gibi kısa bir bekleme
        handler.postDelayed({
            typing?.let { list.removeView(it) }; typing = null
            val r = LocalBrain.reply(this, pet, s)
            face.mood = r.mood
            addBubble(r.text, mine = false, system = r.system)
            save(if (r.system) "S" else "B", r.text)
            refreshKnows()
        }, 700L + (s.length * 12L).coerceAtMost(900L))
    }

    private fun addBubble(text: String, mine: Boolean, system: Boolean = false): TextView {
        val t = TextView(this).apply {
            this.text = text; textSize = 16f; setLineSpacing(0f, 1.15f)
            setTextColor(if (mine) 0xFFFFFFFF.toInt() else INK)
            background = rounded(when { system -> 0xFFFFF4C9.toInt(); mine -> ACCENT; else -> 0xFFFFFFFF.toInt() }, dp(18).toFloat())
            setPadding(dp(14), dp(10), dp(14), dp(10))
            maxWidth = (resources.displayMetrics.widthPixels * 0.78f).toInt()
        }
        list.addView(t, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = if (mine) Gravity.END else Gravity.START; setMargins(0, dp(4), 0, dp(4))
        })
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        return t
    }

    private fun refreshKnows() {
        val lines = ArrayList(Rhythm.facts(this))
        val words = LocalBrain.learnedWords(this)
        if (pet.isPet) lines.add("🐾 Sesini ve sevgini tanıyor; kelime öğrenmiyor, kendi diliyle cevap veriyor.")
        else if (words.isNotEmpty()) lines.add("💬 Senden kaptığı kelimeler: " + words.take(6).joinToString(", ") { "\"$it\"" })
        else lines.add("💬 Sık kullandığın kelimeleri kapacak (bir kelimeyi 3 kez yazınca).")
        if (LocalBrain.recentlySad(this)) lines.add("💛 Son saatlerde biraz üzgündün; ana ekranda sana sarılacak.")
        knows.text = lines.joinToString("\n")
    }

    // Son 60 mesaj
    private fun hist() = getSharedPreferences("chat", Context.MODE_PRIVATE)
    private fun loadHistory(): List<String> = (hist().getString("h", "") ?: "").split("\u0001").filter { it.length > 2 }
    private fun save(who: String, text: String) {
        val all = (loadHistory() + "$who|${text.replace("\u0001", " ")}").takeLast(60)
        hist().edit().putString("h", all.joinToString("\u0001")).apply()
    }

    private fun rounded(color: Int, r: Float) = GradientDrawable().apply { setColor(color); cornerRadius = r }

    override fun onPause() { super.onPause(); pet.save() }

    companion object {
        val BG = 0xFFFBF4EC.toInt()
        val INK = 0xFF2D2230.toInt()
        val MUTED = 0xFF7A6B70.toInt()
        val ACCENT = 0xFFEC6E4C.toInt()
    }
}

/** Sohbetin üstündeki minik canlı yüz. */
@SuppressLint("ViewConstructor")
class ChatFace(ctx: Context, private val pet: Pet) : View(ctx) {
    var mood: Mood = Mood.HAPPY
        set(v) { field = v; moodUntil = SystemClock.uptimeMillis() + 3000 }
    private var moodUntil = 0L
    private var talkUntil = 0L
    private val chibi = ChibiRenderer(pet.look)
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    fun talk() { talkUntil = SystemClock.uptimeMillis() + 1200 }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        bg.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0xFFBFE3F5.toInt(), 0xFFFBE3D0.toInt(), Shader.TileMode.CLAMP)
    }

    override fun onDraw(c: Canvas) {
        rect.set(0f, 0f, width.toFloat(), height.toFloat()); c.drawOval(rect, bg)
        val t = SystemClock.uptimeMillis()
        val m = when { t < moodUntil -> mood; t < talkUntil -> Mood.SURPRISE; pet.asleep -> Mood.SLEEP; else -> Mood.HAPPY }
        c.save(); c.clipPath(android.graphics.Path().apply { addOval(rect, android.graphics.Path.Direction.CW) })
        // Sadece baş ve omuzlar görünsün diye büyük çizip aşağı kaydırıyoruz.
        chibi.draw(c, Frame(width / 2f, height * 1.55f, height / 34f, pet.stage, Pose.STAND, m, hatch = if (pet.hatched) 1f else 0f), t)
        c.restore()
        postInvalidateOnAnimation()
    }
}
