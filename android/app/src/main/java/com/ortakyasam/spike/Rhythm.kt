package com.ortakyasam.spike

import android.content.Context
import java.util.Calendar

/**
 * Sahibin günlük düzeni, telefonun içinde öğrenilir (server/src/profile.ts'nin cihazdaki karşılığı).
 * Tek sinyal: ekran açılınca duvar kağıdı görünür olur. Her gün için ilk ve son bakış saati, bakış sayısı
 * ve saat saat dağılım tutulur. Gün 04:00'te başlar: gece yarısından sonra yatmak aynı güne sayılır.
 * Hiçbir şey telefondan çıkmaz; 21 günden eski günler silinir.
 */
object Rhythm {
    private const val DAY_START_MIN = 4 * 60
    private const val KEEP_DAYS = 21
    const val MIN_DAYS = 3

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("rhythm", Context.MODE_PRIVATE)

    class Day(var first: Int = -1, var last: Int = -1, var opens: Int = 0, val hours: IntArray = IntArray(24)) {
        fun encode() = "$first,$last,$opens," + hours.joinToString(" ")
        companion object {
            fun parse(s: String): Day? = try {
                val p = s.split(",")
                Day(p[0].toInt(), p[1].toInt(), p[2].toInt(), p[3].split(" ").map { it.toInt() }.toIntArray())
            } catch (e: Exception) { null }
        }
    }

    /** 04:00'e göre gün anahtarı (gün numarası). */
    private fun dayKey(now: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = now }
        val offset = c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET)
        return (now + offset - DAY_START_MIN * 60_000L) / 86_400_000L
    }

    /** Ekran açıldı (duvar kağıdı göründü). Bir dakika içindeki tekrarlar bir sayılır. */
    fun record(ctx: Context, now: Long = System.currentTimeMillis()) {
        val p = prefs(ctx)
        val c = Calendar.getInstance().apply { timeInMillis = now }
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val dm = (hour * 60 + c.get(Calendar.MINUTE) - DAY_START_MIN + 1440) % 1440
        val key = dayKey(now)
        val d = p.getString("d$key", null)?.let { Day.parse(it) } ?: Day()
        if (d.first < 0 || dm < d.first) d.first = dm
        if (dm > d.last) d.last = dm
        val e = p.edit()
        if (now - p.getLong("lastRec", 0L) > 60_000L) { d.opens++; d.hours[hour]++; e.putLong("lastRec", now) }
        e.putString("d$key", d.encode())
        for (k in p.all.keys) if (k.startsWith("d") && (k.drop(1).toLongOrNull() ?: key) < key - KEEP_DAYS) e.remove(k)
        e.apply()
    }

    /** Bugün hariç tamamlanmış günler (bugün yarım olduğu için ortalamayı bozar). */
    private fun pastDays(ctx: Context): List<Day> {
        val today = dayKey(System.currentTimeMillis())
        return prefs(ctx).all.mapNotNull { (k, v) ->
            val n = if (k.startsWith("d")) k.drop(1).toLongOrNull() else null
            if (n != null && n < today && v is String) Day.parse(v) else null
        }
    }

    class Profile(val days: Int, val wake: Int?, val sleep: Int?, val busy: Int?, val opensPerDay: Int, val lateNights: Int)

    private fun median(xs: List<Int>): Int? {
        if (xs.isEmpty()) return null
        val a = xs.sorted(); val m = a.size / 2
        return if (a.size % 2 == 1) a[m] else (a[m - 1] + a[m]) / 2
    }

    fun profile(ctx: Context): Profile {
        val days = pastDays(ctx).filter { it.opens >= 3 }
        val hours = IntArray(24)
        for (d in days) for (h in 0..23) hours[h] += d.hours[h]
        // En yoğun iki saatlik dilim
        var best = -1; var bestV = 0
        for (h in 0..23) { val v = hours[h] + hours[(h + 1) % 24]; if (v > bestV) { bestV = v; best = h } }
        return Profile(
            days = days.size,
            wake = median(days.map { it.first }),
            sleep = median(days.map { it.last }),
            busy = if (bestV >= days.size * 2) best else null,
            opensPerDay = if (days.isEmpty()) 0 else days.sumOf { it.opens } / days.size,
            lateNights = days.count { it.last > (24 * 60 + 30 - DAY_START_MIN) },   // 00:30'dan sonra
        )
    }

    /** 04:00'ten itibaren dakika → "07:40" (5 dakikaya yuvarlanır). */
    fun clock(dm: Int): String {
        val m = ((((dm + DAY_START_MIN) % 1440) + 2) / 5 * 5) % 1440
        return "%02d:%02d".format(m / 60, m % 60)
    }

    /** Uygulamadaki "Seni tanıyor" kartı için cümleler. */
    fun facts(ctx: Context): List<String> {
        val pr = profile(ctx)
        if (pr.days < MIN_DAYS) return listOf("🔍 Seni tanımaya çalışıyorum: ${pr.days}/$MIN_DAYS gün. Telefonunu her zamanki gibi kullan.")
        val out = ArrayList<String>()
        pr.wake?.let { out.add("⏰ Genelde ${clock(it)} civarı uyanıyorsun") }
        pr.sleep?.let { out.add("🌙 Genelde ${clock(it)} civarı uyuyorsun" + if (pr.lateNights * 2 > pr.days) " (biraz geç, uykuna dikkat 😴)" else "") }
        pr.busy?.let { out.add("📱 En çok %02d:00–%02d:00 arası telefondasın".format(it, (it + 2) % 24)) }
        if (pr.opensPerDay > 0) out.add("👀 Günde ortalama ${pr.opensPerDay} kez telefonuna bakıyorsun")
        return out
    }

    /** Şu an sahibin olağan uyanma saatinin ilk bir buçuk saati mi? (günaydın için) */
    fun isMorning(ctx: Context, now: Long = System.currentTimeMillis()): Boolean {
        val w = profile(ctx).takeIf { it.days >= MIN_DAYS }?.wake ?: return false
        val c = Calendar.getInstance().apply { timeInMillis = now }
        val dm = (c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE) - DAY_START_MIN + 1440) % 1440
        return dm in (w - 20)..(w + 90)
    }

    /** Olağan uyku saatini geçti mi? */
    fun isPastBedtime(ctx: Context, now: Long = System.currentTimeMillis()): Boolean {
        val s = profile(ctx).takeIf { it.days >= MIN_DAYS }?.sleep ?: return false
        val c = Calendar.getInstance().apply { timeInMillis = now }
        val dm = (c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE) - DAY_START_MIN + 1440) % 1440
        return dm > s + 30
    }

    /** Test için: geçmiş 5 güne örnek veri (07:30 kalkış, 00:45 yatış, akşam yoğun). */
    fun devFill(ctx: Context) {
        val e = prefs(ctx).edit()
        val today = dayKey(System.currentTimeMillis())
        for (i in 1..5) {
            val hours = IntArray(24)
            hours[7] = 4; hours[8] = 3; hours[12] = 3; hours[13] = 2; hours[18] = 3; hours[21] = 7; hours[22] = 8; hours[23] = 5; hours[0] = 3
            val first = (7 * 60 + 20 + i * 5) - DAY_START_MIN
            val last = (24 * 60 + 40 + i * 3) - DAY_START_MIN
            e.putString("d${today - i}", Day(first, last, hours.sum(), hours).encode())
        }
        e.apply()
    }
}
