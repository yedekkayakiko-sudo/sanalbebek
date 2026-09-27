package com.ortakyasam.spike

import android.content.Context
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Karakterin kalıcı durumu. Ekran kapalıyken hiçbir şey çalışmaz:
 * görünür olunca son güncellemeden bu yana geçen süre bir kerede hesaplanır.
 */
class Pet(private val ctx: Context) {
    private val p = ctx.getSharedPreferences("pet", Context.MODE_PRIVATE)

    var name: String = p.getString("name", null) ?: "Minik"
    var stage: Int = p.getInt("stage", 3)                 // 0 Yenidoğan … 5 Çocukluk (deneme için ayarlardan seçilir)
    var hunger: Float = p.getFloat("hunger", 30f)
    var energy: Float = p.getFloat("energy", 80f)
    var asleep: Boolean = p.getBoolean("asleep", false)
    var hatched: Boolean = p.getBoolean("hatched", false)
    var batteryX: Float = p.getFloat("batteryX", 0.9f)     // pil ikonunun yatay yeri (0 sol, 1 sağ)
    private var last: Long = p.getLong("last", System.currentTimeMillis())
    val look: Look

    init {
        var seed = p.getInt("seed", 0)
        if (seed == 0) { seed = Random.nextInt(1, Int.MAX_VALUE); p.edit().putInt("seed", seed).apply() }
        look = Look.fromSeed(seed)
    }

    /** Geçen süreyi uygula. Uyurken enerji dolar, uyanıkken açlık artar. */
    fun catchUp(now: Long = System.currentTimeMillis()) {
        var dtH = (now - last) / 3_600_000f
        last = now
        if (dtH <= 0f) return
        dtH = min(dtH, 72f)
        // 10 dakikalık adımlarla, uyku-uyanma geçişleri doğru olsun
        while (dtH > 0f) {
            val s = min(dtH, 1f / 6f)
            if (asleep) {
                hunger += 3.5f * s; energy += 16f * s
                if (energy >= 85f || hunger >= 88f) asleep = false
            } else {
                hunger += 9f * s; energy -= 8f * s
                if (energy <= 12f) asleep = true
            }
            hunger = hunger.coerceIn(0f, 100f); energy = energy.coerceIn(0f, 100f)
            dtH -= s
        }
        save()
    }

    fun feed(): Boolean {
        if (asleep || hunger < 25f) return false
        hunger = max(0f, hunger - 60f); save(); return true
    }

    fun sleep(): Boolean {
        if (asleep || energy > 75f) return false
        asleep = true; save(); return true
    }

    fun play(): Boolean {
        if (asleep || energy < 15f) return false
        energy = max(0f, energy - 4f); save(); return true
    }

    fun hatch() { hatched = true; save() }

    fun save() {
        p.edit()
            .putString("name", name).putInt("stage", stage)
            .putFloat("hunger", hunger).putFloat("energy", energy)
            .putBoolean("asleep", asleep).putBoolean("hatched", hatched)
            .putFloat("batteryX", batteryX).putLong("last", last)
            .apply()
    }

    companion object {
        val STAGE_NAMES = listOf("Yenidoğan", "Bebek", "Emekleme", "Yürüme", "İlk kelimeler", "Çocukluk")
    }
}

data class Look(val skin: Int, val hair: Int, val eye: Int, val outfit: Int, val ears: Int) {
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
