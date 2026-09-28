package com.ortakyasam.spike

import android.content.Context
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Karakterin kalıcı durumu. Ekran kapalıyken hiçbir şey çalışmaz:
 * görünür olunca son güncellemeden bu yana geçen süre bir kerede hesaplanır.
 *
 * Büyüme gerçek zamanlıdır: bebek uygulamanın ilk açıldığı gün doğar. Her gün "büyüme günü" toplar;
 * iyi bakılan gün tam gün sayılır, ihmal edilen gün yaklaşık üçte bir. Böylece ihmal edilen bebeğin
 * büyümesi birkaç gün gecikir. Dönem, toplanan büyüme günlerinden hesaplanır; elle seçilmez.
 */
class Pet(private val ctx: Context) {
    private val p = ctx.getSharedPreferences("pet", Context.MODE_PRIVATE)

    var name: String = p.getString("name", null) ?: "Minik"
    var hunger: Float = p.getFloat("hunger", 25f)
    var energy: Float = p.getFloat("energy", 80f)
    var joy: Float = p.getFloat("joy", 70f)                  // neşe: oynayınca artar
    var clean: Float = p.getFloat("clean", 85f)              // temizlik: banyoda artar
    var diaper: Float = p.getFloat("diaper", 0f)             // bez (büyüyünce tuvalet) ihtiyacı: 0 temiz, 100 çok kirli
    var sick: Boolean = p.getBoolean("sick", false)
    var asleep: Boolean = p.getBoolean("asleep", false)
    var hatched: Boolean = p.getBoolean("hatched", false)
    var coins: Int = p.getInt("coins", 100)
    var batteryX: Float = p.getFloat("batteryX", 0.9f)       // pil ikonunun yatay yeri (0 sol, 1 sağ)
    var overlayOn: Boolean = p.getBoolean("overlayOn", true) // ana ekranda ikonların üstünde görünsün
    var fedAt: Long = p.getLong("fedAt", 0L)                 // uygulamadan beslenince ana ekranda yemek yerken görünsün
    var bornAt: Long = p.getLong("bornAt", 0L)
    var growth: Float = p.getFloat("growth", 0f)             // toplanan büyüme günü
    private var sickRisk: Float = p.getFloat("sickRisk", 0f) // kötü bakımda geçen saat
    private var shownMilestone: Int = p.getInt("shownMilestone", -1)
    private var shownMonths: Int = p.getInt("shownMonths", 0)
    private var last: Long = p.getLong("last", System.currentTimeMillis())
    val inventory: MutableMap<String, Int> = parseInv(p.getString("inv", null) ?: "🍼:4;🍎:2;🥛:2")
    val look: Look

    val species: Int get() = look.species
    val isPet: Boolean get() = look.species != 0
    /** Karakter seçildi mi (ilk açılıştaki "bebek mi, evcil hayvan mı?" ekranı). */
    val chosen: Boolean get() = p.getBoolean("chosen", false)

    /** Dönem adı; hayvanlarda yavru dönemleri. */
    fun stageName(i: Int = stage): String = if (isPet) PET_STAGE_NAMES[i] else STAGE_NAMES[i]

    /**
     * Konuşma: insan bebek söyleneni söyler; hayvan kelime yerine kendi sesini çıkarır, emojiyi korur.
     * ("acıktım…" → "hav hav! 🍖" gibi değil, sadece "hav hav!"; "🤒 ıhh" → "🤒 ıın")
     */
    fun voice(s: String): String {
        if (!isPet) return s
        val emoji = s.filter { !it.isLetterOrDigit() && !it.isWhitespace() && it !in ".,!?…'\"-:°%" }.trim()
        val sound = when (species) {
            1 -> listOf("hav!", "hav hav!", "vuf!", "hıı?")
            2 -> listOf("miyav!", "mrr…", "miyuv?", "mırr")
            else -> listOf("fıs fıs!", "hıı?", "ıhık!")
        }[kotlin.math.abs(s.hashCode()) % 3]
        return if (emoji.isEmpty()) sound else "$emoji $sound"
    }

    /** 0 Yenidoğan … 5 Çocukluk; büyüme günlerinden hesaplanır. */
    val stage: Int get() = stageFor(growth)

    init {
        var seed = p.getInt("seed", 0)
        if (seed == 0) { seed = Random.nextInt(1, Int.MAX_VALUE); p.edit().putInt("seed", seed).apply() }
        val sp = p.getInt("species", 0)
        look = if (sp == 0) Look.fromSeed(seed) else {
            val fur = p.getInt("fur", 0xFFD9A066.toInt())
            Look(fur, p.getInt("fur2", 0xFF8A5A3C.toInt()), p.getInt("eyeColor", 0xFF3A2A20.toInt()), fur, 0, sp, p.getInt("fur2", 0xFF8A5A3C.toInt()), p.getInt("earStyle", 0), p.getInt("pattern", 0))
        }
        if (bornAt == 0L) {
            // İlk açılış (ya da gerçek zamanlı büyümeden önceki sürümden geçiş): bebek bugün doğar.
            bornAt = System.currentTimeMillis(); last = bornAt
            growth = 0f; hatched = false; asleep = false; sick = false
            hunger = 25f; energy = 80f; joy = 70f; clean = 85f; diaper = 0f
            save()
        }
    }

    /** Takvim yaşı (gün), bakımdan bağımsız. */
    fun ageDays(now: Long = System.currentTimeMillis()) = (now - bornAt) / 86_400_000f

    /** Boy ve oranlar: ilk 90 günde hızlı, sonra her ay biraz daha. */
    fun scale(): Float = 0.8f + 0.34f * min(growth / 90f, 1f) + min(0.45f, 0.03f * max(0f, (growth - 90f) / 30f))
    fun maturity(): Float = min(growth / 365f, 1f)

    /** Geçen süreyi uygula. */
    fun catchUp(now: Long = System.currentTimeMillis()) {
        var dtH = (now - last) / 3_600_000f
        last = now
        if (dtH <= 0f) return
        dtH = min(dtH, 72f)
        // 10 dakikalık adımlarla, uyku-uyanma geçişleri doğru olsun
        while (dtH > 0f) {
            val s = min(dtH, 1f / 6f)
            if (asleep) {
                hunger += 3.5f * s; energy += 16f * s; joy -= 1f * s; clean -= 1f * s; diaper += 2f * s
                if (energy >= 85f || hunger >= 88f) asleep = false
            } else {
                hunger += 9f * s; energy -= 8f * s; joy -= 5f * s; clean -= 3f * s; diaper += 6f * s
                if (energy <= 12f) asleep = true
            }
            if (diaper > 70f) { clean -= 3f * s; joy -= 2f * s }
            if (sick) { energy -= 3f * s; joy -= 3f * s }
            // Kötü bakım uzun sürerse hastalanır.
            if (hunger > 90f || clean < 15f || diaper > 95f) sickRisk += s else sickRisk = max(0f, sickRisk - s)
            if (!sick && sickRisk > 6f) sick = true
            // Büyüme: iyi bakılan saat tam, ihmal edilen saat üçte bir, hastayken çok az.
            val good = hunger < 75f && clean > 25f && diaper < 80f
            growth += s / 24f * when { sick -> 0.1f; good -> 1f; else -> 0.35f }
            hunger = hunger.coerceIn(0f, 100f); energy = energy.coerceIn(0f, 100f)
            joy = joy.coerceIn(0f, 100f); clean = clean.coerceIn(0f, 100f); diaper = diaper.coerceIn(0f, 100f)
            dtH -= s
        }
        save()
    }

    // ---------- Bakım ----------

    fun feed(): Boolean {
        if (asleep || hunger < 25f) return false
        hunger = max(0f, hunger - 60f); fedAt = System.currentTimeMillis(); save(); return true
    }

    /** Buzdolabından bir yiyecek. Sonuç: null yedi, aksi halde neden yemediği. */
    fun eatItem(food: String): String? {
        val f = FOODS[food] ?: return "?"
        if (asleep) return "zzz"
        if (stage <= 1 && !f.baby) return if (stage == 0) "ı-ıh" else "dişim yok!"
        if (hunger < 4f) return if (stage >= 4) "tokum!" else "ı-ıh"
        if ((inventory[food] ?: 0) <= 0) return "bitti"
        inventory[food] = (inventory[food] ?: 0) - 1
        hunger = max(0f, hunger - f.fill); joy = min(100f, joy + f.joy)
        fedAt = System.currentTimeMillis(); save()
        return null
    }

    fun buy(food: String): Boolean {
        val f = FOODS[food] ?: return false
        if (coins < f.price) return false
        coins -= f.price; inventory[food] = (inventory[food] ?: 0) + 1; save(); return true
    }

    fun gift(food: String, n: Int = 1) { inventory[food] = (inventory[food] ?: 0) + n; save() }
    fun earn(n: Int) { coins += n; save() }

    fun bathe(amount: Float) { clean = min(100f, clean + amount) }
    fun changeDiaper() { diaper = 0f; clean = min(100f, clean + 10f); joy = min(100f, joy + 5f); save() }
    fun cheer(amount: Float) { if (!asleep) { joy = min(100f, joy + amount); energy = max(0f, energy - amount * 0.1f) } }
    fun cure() { sick = false; sickRisk = 0f; joy = min(100f, joy + 10f); save() }
    fun temperature(): Float = if (sick) 38.6f + (sickRisk % 1f) * 0.6f else 36.6f

    fun wake() { asleep = false; if (energy < 40f) energy = 40f; save() }

    fun sleep(): Boolean {
        if (asleep || energy > 75f) return false
        asleep = true; save(); return true
    }

    fun play(): Boolean {
        if (asleep || energy < 15f) return false
        energy = max(0f, energy - 4f); joy = min(100f, joy + 8f); save(); return true
    }

    fun hatch() { hatched = true; save() }

    // ---------- Dönüm noktaları ----------

    /** Henüz kutlanmamış bir dönüm noktası varsa metnini döner ve kutlandı olarak işaretler. */
    fun takeMilestone(): String? {
        val i = MILESTONES.indexOfLast { it.first <= growth }
        if (i > shownMilestone) { shownMilestone = i; save(); return if (isPet) petMilestone(i) else MILESTONES[i].second }
        val months = (ageDays() / 30.44f).toInt()
        if (months > shownMonths) { shownMonths = months; save(); return "🎂 $name $months aylık oldu!" }
        return null
    }

    private fun petMilestone(i: Int): String {
        val sound = when (species) { 1 -> "havlama"; 2 -> "miyav"; else -> "zıplama" }
        return when (i) {
            0 -> "🐾 Dünyaya geldi!"
            6 -> "🎾 Top peşinde koşuyor"
            7 -> "🔔 Adını tanıyor"
            8 -> "💬 İlk $sound!"
            9 -> if (species == 2) "🪣 Kum kabını öğrendi" else "🚪 Tuvaleti öğrendi"
            10 -> "🌟 Artık kocaman oldu!"
            else -> MILESTONES[i].second
        }
    }

    /**
     * İlk açılıştaki seçim: karakter bugün yeniden doğar. Pil yeri ve ikon üstü ayarı korunur.
     * Fotoğraf saklanmaz; sadece ondan çıkarılan renkler kaydedilir.
     */
    fun rebirth(newName: String, species: Int, fur: Int = 0, fur2: Int = 0, earStyle: Int = 0, pattern: Int = 0) {
        val keepBattery = batteryX; val keepOverlay = overlayOn
        p.edit().clear()
            .putBoolean("chosen", true).putInt("species", species)
            .putInt("fur", fur).putInt("fur2", fur2).putInt("earStyle", earStyle).putInt("pattern", pattern)
            .putString("name", newName).putFloat("batteryX", keepBattery).putBoolean("overlayOn", keepOverlay)
            .putString("inv", if (species == 0) "🍼:4;🍎:2;🥛:2" else "🍼:4;🥛:2;${if (species == 2) "🐟" else if (species == 1) "🍖" else "🥕"}:2")
            .apply()
    }

    fun markChosen() { p.edit().putBoolean("chosen", true).apply() }

    /** Günde bir kez giriş hediyesi (altın). Verildiyse miktarı, verilmediyse 0 döner. */
    fun dailyBonus(): Int {
        val day = (System.currentTimeMillis() / 86_400_000L).toInt()
        if (p.getInt("bonusDay", -1) == day) return 0
        p.edit().putInt("bonusDay", day).apply()
        coins += 20; save(); return 20
    }

    /** Test için zamanı ileri alır (yaş ve büyüme birlikte). */
    fun devAddDays(days: Float) {
        growth += days; bornAt -= (days * 86_400_000L).toLong(); save()
    }

    fun devReset() {
        p.edit().clear().apply()
    }

    fun save() {
        p.edit()
            .putString("name", name)
            .putFloat("hunger", hunger).putFloat("energy", energy)
            .putBoolean("asleep", asleep).putBoolean("hatched", hatched)
            .putFloat("batteryX", batteryX).putLong("last", last)
            .putBoolean("overlayOn", overlayOn).putLong("fedAt", fedAt)
            .putFloat("joy", joy).putFloat("clean", clean).putFloat("diaper", diaper)
            .putBoolean("sick", sick).putFloat("sickRisk", sickRisk).putInt("coins", coins)
            .putLong("bornAt", bornAt).putFloat("growth", growth)
            .putInt("shownMilestone", shownMilestone).putInt("shownMonths", shownMonths)
            .putString("inv", inventory.entries.joinToString(";") { "${it.key}:${it.value}" })
            .apply()
    }

    class Food(val price: Int, val fill: Float, val joy: Float, val baby: Boolean)

    companion object {
        val STAGE_NAMES = listOf("Yenidoğan", "Bebek", "Emekleme", "Yürüme", "İlk kelimeler", "Çocukluk")
        val PET_STAGE_NAMES = listOf("Yeni doğmuş yavru", "Minik yavru", "Emekleyen yavru", "Oyuncu yavru", "Genç", "Yetişkin")
        /** Dönemlerin başladığı büyüme günü. Konuşma yaklaşık 2 ayda başlar. */
        val STAGE_DAYS = floatArrayOf(0f, 7f, 21f, 35f, 56f, 90f)

        fun stageFor(growth: Float): Int = STAGE_DAYS.indexOfLast { it <= growth }.coerceAtLeast(0)

        val FOODS = linkedMapOf(
            "🍼" to Food(6, 22f, 2f, true),
            "🥛" to Food(8, 18f, 2f, true),
            "🍎" to Food(5, 12f, 2f, false),
            "🍌" to Food(5, 14f, 2f, false),
            "🥕" to Food(4, 10f, 0f, false),
            "🍓" to Food(12, 12f, 8f, false),
            "🍪" to Food(10, 10f, 10f, false),
            "🍖" to Food(9, 18f, 6f, false),
            "🐟" to Food(9, 18f, 6f, false),
        )

        val MILESTONES = listOf(
            0f to "🐣 Dünyaya geldi!",
            2f to "😊 İlk gülümseme!",
            5f to "👀 Seni gözleriyle takip ediyor",
            7f to "🔄 Dönmeyi öğrendi",
            14f to "🪑 Kendi başına oturuyor",
            21f to "🐾 Emeklemeye başladı!",
            35f to "👣 İlk adımlar!",
            45f to "🗣️ İlk \"ba-ba\"",
            56f to "💬 İlk kelime!",
            70f to "🚽 Tuvalet eğitimi başladı",
            90f to "🎒 Çocukluk başladı!",
        )

        private fun parseInv(s: String): MutableMap<String, Int> {
            val m = linkedMapOf<String, Int>()
            for (part in s.split(";")) {
                val i = part.lastIndexOf(':')
                if (i > 0) m[part.substring(0, i)] = part.substring(i + 1).toIntOrNull() ?: 0
            }
            return m
        }
    }
}

/**
 * Görünüm. species 0 insan bebek, 1 köpek, 2 kedi, 3 tavşan. Hayvanlarda skin = ana tüy rengi,
 * fur2 = ikinci renk (kulak, leke), earStyle köpekte 0 sarkık / 1 dik, pattern 0 düz / 1 göz lekesi / 2 beyaz ağız.
 */
data class Look(
    val skin: Int, val hair: Int, val eye: Int, val outfit: Int, val ears: Int,
    val species: Int = 0, val fur2: Int = 0, val earStyle: Int = 0, val pattern: Int = 0,
) {
    companion object {
        private val SKIN = intArrayOf(0xFFF6D7C3.toInt(), 0xFFEBC0A0.toInt(), 0xFFD39C74.toInt(), 0xFFA8704A.toInt(), 0xFF6F4630.toInt())
        private val HAIR = intArrayOf(0xFF2B1D14.toInt(), 0xFF5A3A22.toInt(), 0xFF9C6A3A.toInt(), 0xFFD9B36C.toInt(), 0xFFB5532E.toInt())
        private val EYE = intArrayOf(0xFF4A3222.toInt(), 0xFF2F5E7A.toInt(), 0xFF4F6B3A.toInt(), 0xFF6B6E73.toInt())
        private val OUTFIT = intArrayOf(0xFF7FA38A.toInt(), 0xFFE1B24C.toInt(), 0xFF7C9DC6.toInt(), 0xFFE58FA8.toInt())

        /** 0 kulak yok, 1 kedi, 2 tavşan */
        fun fromSeed(seed: Int): Look {
            val r = Random(seed)
            return Look(SKIN[r.nextInt(SKIN.size)], HAIR[r.nextInt(HAIR.size)], EYE[r.nextInt(EYE.size)], OUTFIT[r.nextInt(OUTFIT.size)], intArrayOf(0, 0, 1, 2)[r.nextInt(4)])
        }
    }
}
