package com.ortakyasam.spike

import android.content.Context
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

/**
 * Sunucusuz deneme beyni: yapay zekâ yerine kurallarla cevap verir. Amaç, "bebek beni tanıyor,
 * moralim bozukken yanımda" deneyimini sunucu olmadan görüp test edebilmek. Sunucu geldiğinde
 * aynı giriş-çıkışla (mesaj → cevap) gerçek yapay zekâya bağlanacak; bu sınıf yedek olarak kalır.
 *
 * Kurallar (skills/ klasöründeki dil-gelisimi, konusma-uslubu, kelime-ogrenme, guvenlik-sinirlari):
 *  - Konuşma döneme göre: yenidoğan ses ve hareket, bebek hece, "ilk kelimeler" 1-2 kelime, çocukluk cümle.
 *  - Evcil hayvan konuşmaz: kendi sesi ve hareketiyle cevap verir.
 *  - Sahibin sık kullandığı kelimeleri kapar (3 kez duyunca) ve kullanır. Küfür kapılmaz, "bip" olur.
 *  - Kriz belirtisinde (kendine zarar vb.) karakterden çıkar, güvendiği birine ve 112'ye yönlendirir.
 */
object LocalBrain {
    enum class Topic { CRISIS, SAD, LONELY, TIRED, STRESS, ANGRY, BORED, HAPPY, LOVE, THANKS, GREET, MORNING, NIGHT, NAME, HUNGRY_Q, HOW_ARE_YOU, WHAT_DOING, PROFANE, OTHER }
    class Reply(val text: String, val mood: Mood, val learned: String? = null, val system: Boolean = false)

    private val rnd = Random(System.nanoTime())
    private val TR = Locale("tr", "TR")

    private val CRISIS = listOf("intihar", "kendimi öldür", "ölmek istiyorum", "yaşamak istemiyorum", "kendime zarar", "canıma kıy", "bileklerimi", "her şeye son ver")
    private val KEYS = linkedMapOf(
        Topic.SAD to listOf("üzgün", "üzül", "moralim bozuk", "mutsuz", "ağla", "kötü hissed", "berbat", "canım sıkkın", "kalbim kırık", "ayrıldık", "kırıldım", "hüzün"),
        Topic.LONELY to listOf("yalnız", "kimsem yok", "kimse yok", "arkadaşım yok", "özledim"),
        Topic.TIRED to listOf("yorgun", "yoruldum", "uykum", "bitkin", "halsiz"),
        Topic.STRESS to listOf("stres", "sınav", "gergin", "endişe", "kaygı", "korkuyorum", "panik", "iş çok", "yetiştiremiyorum", "başaramayacağım"),
        Topic.ANGRY to listOf("sinir", "kızgın", "öfke", "delirdim", "bıktım"),
        Topic.BORED to listOf("sıkıldım", "canım sıkılıyor", "sıkıcı", "yapacak bir şey yok"),
        Topic.HAPPY to listOf("mutlu", "harika", "süper", "sevindim", "çok iyi", "başardım", "kazandım", "geçtim", "yaşasın", "mükemmel"),
        Topic.LOVE to listOf("seni seviyorum", "seviyorum", "canım benim", "tatlım", "bir tanem", "aşkım"),
        Topic.THANKS to listOf("teşekkür", "sağol", "sağ ol", "eyvallah"),
        Topic.MORNING to listOf("günaydın"),
        Topic.NIGHT to listOf("iyi geceler", "uyuyorum", "yatıyorum", "uyuyacağım"),
        Topic.GREET to listOf("merhaba", "selam", "naber", "hey", "slm"),
        Topic.NAME to listOf("adın ne", "ismin ne", "sen kimsin"),
        Topic.HUNGRY_Q to listOf("acıktın", "aç mısın", "karnın"),
        Topic.HOW_ARE_YOU to listOf("nasılsın", "iyi misin", "ne haber"),
        Topic.WHAT_DOING to listOf("ne yapıyorsun", "napıyorsun", "napıyon", "ne yapıyon"),
    )
    // Küfür listesi bilerek kısa ve kaba değil: sadece yakalamak için kökler.
    private val PROFANE = listOf("amk", "aq", "siktir", "sik", "orospu", "piç", "göt", "yarrak", "amına", "ananı", "kahpe", "pezevenk", "oç", "mk")
    private val STOP = setOf(
        "ve", "ile", "ama", "fakat", "çok", "daha", "bir", "bu", "şu", "o", "ben", "sen", "biz", "siz", "onlar", "de", "da", "ki", "mi", "mı", "mu", "mü",
        "ne", "neden", "nasıl", "gibi", "için", "kadar", "sonra", "önce", "şimdi", "bugün", "yarın", "dün", "evet", "hayır", "yok", "var", "iyi", "kötü",
        "beni", "seni", "bana", "sana", "benim", "senin", "olan", "oldu", "olur", "diye", "bunu", "şunu", "onu", "hem", "her", "hiç", "çünkü", "ise", "artık",
        "biraz", "gerçekten", "tamam", "peki", "yani", "hadi", "şey", "hep", "bile", "zaten", "işte",
        "merhaba", "günaydın", "selam", "naber", "nasılsın", "seviyorum", "teşekkür", "ederim",
    )

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("brain", Context.MODE_PRIVATE)

    fun topicOf(text: String): Topic {
        val t = text.lowercase(TR)
        if (CRISIS.any { t.contains(it) }) return Topic.CRISIS
        for ((topic, keys) in KEYS) if (keys.any { t.contains(it) }) return topic
        if (words(t).any { w -> PROFANE.any { w == it || (it.length >= 4 && w.startsWith(it)) } }) return Topic.PROFANE
        return Topic.OTHER
    }

    private fun words(t: String) = t.split(Regex("[^a-zçğıöşü]+")).filter { it.isNotBlank() }

    /** Sık söylenen kelimeleri say; 3 kez duyulan kelime "kapılmış" olur. Yeni kapılanı döner. */
    private fun learn(ctx: Context, text: String): String? {
        val p = prefs(ctx)
        val counts = parse(p.getString("words", "") ?: "")
        var fresh: String? = null
        for (w in words(text.lowercase(TR))) {
            if (w.length < 3 || w in STOP || PROFANE.any { w == it || (it.length >= 4 && w.startsWith(it)) }) continue
            val n = (counts[w] ?: 0) + 1
            counts[w] = n
            if (n == 3) fresh = w
        }
        val top = counts.entries.sortedByDescending { it.value }.take(60)
        p.edit().putString("words", top.joinToString(";") { "${it.key}:${it.value}" }).apply()
        return fresh
    }

    private fun parse(s: String): MutableMap<String, Int> {
        val m = HashMap<String, Int>()
        for (part in s.split(";")) { val i = part.lastIndexOf(':'); if (i > 0) m[part.substring(0, i)] = part.substring(i + 1).toIntOrNull() ?: 0 }
        return m
    }

    /** Kapılmış kelimeler, en sık kullanılan önce. */
    fun learnedWords(ctx: Context): List<String> =
        parse(prefs(ctx).getString("words", "") ?: "").filter { it.value >= 3 }.entries.sortedByDescending { it.value }.map { it.key }

    /** Son 12 saatte üzgün/yalnız/stresli bir şey söyledi mi? (ana ekranda "iyi misin?" için) */
    fun recentlySad(ctx: Context): Boolean = System.currentTimeMillis() - prefs(ctx).getLong("sadAt", 0L) < 12 * 3_600_000L

    /** Ana ekranda bir kez "iyi misin?" desin diye: soruldu olarak işaretler. */
    fun takeComfortPrompt(ctx: Context): Boolean {
        val p = prefs(ctx)
        val sadAt = p.getLong("sadAt", 0L)
        if (!recentlySad(ctx) || p.getLong("comfortedAt", 0L) >= sadAt) return false
        p.edit().putLong("comfortedAt", System.currentTimeMillis()).apply(); return true
    }

    private fun sadCountToday(ctx: Context): Int {
        val p = prefs(ctx); val day = System.currentTimeMillis() / 86_400_000L
        return if (p.getLong("sadDay", -1) == day) p.getInt("sadCount", 0) else 0
    }

    private fun markSad(ctx: Context) {
        val p = prefs(ctx); val day = System.currentTimeMillis() / 86_400_000L
        val n = sadCountToday(ctx) + 1
        p.edit().putLong("sadAt", System.currentTimeMillis()).putLong("sadDay", day).putInt("sadCount", n).apply()
    }

    fun reply(ctx: Context, pet: Pet, text: String): Reply {
        val topic = topicOf(text)
        if (topic == Topic.CRISIS) {
            markSad(ctx)
            return Reply(
                "Bunu yazdığın için teşekkür ederim. Şu an çok zor bir şey yaşıyor olabilirsin ve yalnız kalmamalısın. " +
                    "Lütfen hemen güvendiğin birine (ailen, bir arkadaşın, öğretmenin) haber ver ya da 112'yi ara. " +
                    "Ben bir oyun karakteriyim; sana gerçekten yardım edebilecek insanlar var ve sen çok değerlisin 💛",
                Mood.SHY, system = true,
            )
        }
        if (topic in setOf(Topic.SAD, Topic.LONELY, Topic.STRESS, Topic.TIRED, Topic.ANGRY)) markSad(ctx)
        val fresh = if (pet.isPet) null else learn(ctx, text)
        val learned = learnedWords(ctx)
        pet.cheer(2f); pet.save()

        val mood = when (topic) {
            Topic.SAD, Topic.LONELY, Topic.STRESS, Topic.ANGRY -> Mood.SHY
            Topic.TIRED, Topic.NIGHT -> Mood.SLEEPY
            Topic.HAPPY, Topic.LOVE, Topic.THANKS, Topic.GREET, Topic.MORNING -> Mood.LAUGH
            Topic.PROFANE -> Mood.SURPRISE
            else -> Mood.HAPPY
        }
        val body = when {
            !pet.hatched -> "*yumurta hafifçe sallanıyor* 🥚"
            pet.isPet -> petReply(pet, topic)
            pet.stage <= 1 -> infantReply(topic)
            pet.stage <= 3 -> babbleReply(topic, learned)
            pet.stage == 4 -> firstWordsReply(topic, learned, pet)
            else -> childReply(ctx, topic, learned, pet)
        }
        val extra = if (fresh != null && pet.stage >= 2 && pet.hatched) "\n\n✨ Yeni kelime kaptı: \"$fresh\"" else ""
        // Aynı gün üçüncü kez üzgünse: sevdiği biriyle de konuşmasını nazikçe öner.
        val care = if (topic in setOf(Topic.SAD, Topic.LONELY) && sadCountToday(ctx) >= 3)
            "\n\n💛 Bugün birkaç kez üzgün olduğunu söyledin. Sevdiğin biriyle de konuşmak iyi gelebilir." else ""
        return Reply(body + extra + care, mood, fresh)
    }

    private fun pick(vararg xs: String) = xs[rnd.nextInt(xs.size)]

    private fun petReply(pet: Pet, t: Topic): String {
        val s = when (pet.species) { 1 -> "hav"; 2 -> "miyav"; else -> "fıs fıs" }
        val tail = when (pet.species) { 1 -> "kuyruğunu sallıyor"; 2 -> "mırlayarak sürtünüyor"; else -> "burnunu kıpırdatıyor" }
        return when (t) {
            Topic.SAD, Topic.LONELY, Topic.STRESS, Topic.ANGRY -> pick("*kucağına atlayıp yüzünü yalıyor* $s… 💗", "*yanına kıvrılıp başını dizine koyuyor* 🥺", "*sana sokuluyor, hiç ayrılmıyor* $s 💛")
            Topic.TIRED, Topic.NIGHT -> pick("*yanına kıvrılıp esniyor* 😴", "*gözlerini kırpıştırıyor* $s… zzz")
            Topic.HAPPY, Topic.LOVE, Topic.THANKS -> pick("*$tail* $s $s! 💗", "*etrafında zıplıyor* $s! 🎉")
            Topic.HUNGRY_Q -> if (pet.hunger > 50) "*mama kabına bakıp $s diyor* 🍖" else "*karnını gösteriyor, tok* $s 😌"
            Topic.PROFANE -> "*kulaklarını kapatıyor* 🙉"
            Topic.BORED -> "*top getiriyor* $s? ⚽"
            else -> pick("*başını yana eğiyor* $s? 🐾", "*$tail* $s!", "*sana bakıp gözlerini kırpıyor* 💗")
        }
    }

    private fun infantReply(t: Topic): String = when (t) {
        Topic.SAD, Topic.LONELY, Topic.STRESS, Topic.ANGRY -> pick("*minik elini sana uzatıyor* 🤲", "*sana bakıp gülümsüyor, sanki \"buradayım\" der gibi* 😊", "*parmağını sıkıca tutuyor* 💗")
        Topic.TIRED, Topic.NIGHT -> "*esniyor* ıhh… 😴"
        Topic.HAPPY, Topic.LOVE, Topic.THANKS, Topic.GREET, Topic.MORNING -> pick("agu! *gülüyor* 😆", "*ayaklarını sallıyor* agu agu! 💗")
        Topic.PROFANE -> "*şaşkın şaşkın bakıyor* 😮"
        else -> pick("agu? 👀", "*sesini duyunca başını çeviriyor* 😊", "ıngıı… agu!")
    }

    private fun babbleReply(t: Topic, learned: List<String>): String {
        val parrot = learned.firstOrNull()?.let { w -> val h = w.take(2); "$h-$h!" }
        return when (t) {
            Topic.SAD, Topic.LONELY, Topic.STRESS, Topic.ANGRY -> pick("*emekleyip kucağına tırmanıyor* ba-ba… 🤗", "*sana sarılıyor* ma-ma 💗", "*oyuncağını sana veriyor* 🧸")
            Topic.TIRED, Topic.NIGHT -> "*gözlerini ovuşturuyor* ııh… 😴"
            Topic.HAPPY, Topic.LOVE, Topic.THANKS -> pick("*alkışlıyor* ba-ba-ba! 👏", "*kahkaha atıyor* 😆")
            Topic.PROFANE -> "bip? 🙊"
            else -> parrot?.let { pick(it, "$it 😆", "ba-ba! $it") } ?: pick("ba-ba? 👀", "da-da! 😊", "*merakla bakıyor* 👀")
        }
    }

    private fun firstWordsReply(t: Topic, learned: List<String>, pet: Pet): String {
        val w = learned.firstOrNull()
        return when (t) {
            Topic.SAD, Topic.LONELY -> pick("sen üzgün? 🥺 sarıl!", "ağlama… ben burda 🤗", "öpücük! 😘")
            Topic.STRESS -> pick("sen güçlü! 💪", "nefes… 🌬️ tamam?", "yaparsın! ⭐")
            Topic.ANGRY -> pick("kızma… 🥺 sarıl?", "sakin… 🤗")
            Topic.TIRED, Topic.NIGHT -> pick("uyku! 😴", "nini… 🌙")
            Topic.HAPPY -> pick("yaşasın! 🎉", "süper! 👏")
            Topic.LOVE -> pick("seni seviyom! 💗", "ben de! 💗")
            Topic.THANKS -> "sağol! 😊"
            Topic.GREET, Topic.MORNING -> pick("merhaba! 👋", "geldin! 😆")
            Topic.NAME -> "ben ${pet.name}! 😊"
            Topic.HUNGRY_Q -> if (pet.hunger > 50) "mama! 🍼" else "tok! 😌"
            Topic.HOW_ARE_YOU -> if (pet.hunger > 70) "aç… 🥺" else "iyi! 😊"
            Topic.PROFANE -> pick("bip! 🙊", "bip bip! 😆")
            Topic.BORED -> "oyna? ⚽"
            else -> w?.let { pick("$it! 😆", "$it $it! 👏") } ?: pick("ne? 👀", "anlat! 😊")
        }
    }

    private fun childReply(ctx: Context, t: Topic, learned: List<String>, pet: Pet): String {
        val w = learned.getOrNull(rnd.nextInt(maxOf(1, minOf(3, learned.size))))
        val late = Rhythm.isPastBedtime(ctx) || Calendar.getInstance().get(Calendar.HOUR_OF_DAY) in 0..4
        return when (t) {
            Topic.SAD -> pick(
                "Üzülme… Gel sarılalım 🤗 İstersen anlat, seni dinliyorum.",
                "Bazen günler zor geçiyor. Ama sen benim en sevdiğimsin ve yarın daha güzel olabilir 💛",
                "Ağlamak da olur. Ben hep yanındayım, bir yere gitmiyorum 🥺💗",
            )
            Topic.LONELY -> pick("Yalnız değilsin, ben buradayım! 🤗 Hem belki bugün bir arkadaşına mesaj atarsın?", "Seni çok seviyorum. Birlikte oyun oynayalım mı? 🎈")
            Topic.STRESS -> pick(
                "Birlikte nefes alalım: içeri… dışarı… 🌬️ Bak, biraz daha iyi.",
                "Sen çok güçlüsün, daha önce de başardın! 💪 Adım adım gidelim.",
                "Sonuç ne olursa olsun, senin değerin ondan çok büyük ⭐",
            )
            Topic.ANGRY -> pick("Sinirlenmişsin… Biraz su içip on saniye bekleyelim mi? 🤗", "Kızgınken ben de sarılmak isterim. Sarılalım mı? 🧸")
            Topic.TIRED -> if (late) "Çok geç oldu, hadi uyuyalım! Yarın enerjik olursun 😴🌙" else pick("Yorulmuşsun… Biraz dinlen, ben seni beklerim 😴", "Bir mola ver, su iç! 💧")
            Topic.NIGHT -> "İyi geceler! Rüyanda beni gör 🌙💗"
            Topic.MORNING -> "Günaydın! ☀️ Bugün harika bir gün olacak!"
            Topic.BORED -> pick("Hadi oyun odasına gel, top oynayalım! ⚽", "Sıkıldıysan bana bir hikâye anlat! 📖")
            Topic.HAPPY -> pick("Yaşasııın! 🎉 Seninle gurur duyuyorum!", "Çok sevindim! Hadi dans edelim 💃")
            Topic.LOVE -> pick("Ben de seni çok seviyorum! 💗", "Sen benim en sevdiğimsin! 🥰")
            Topic.THANKS -> "Rica ederim! 😊"
            Topic.GREET -> if (late) "Selam! Ama çok geç oldu, uyumuyor musun? 😴" else pick("Selaaam! Seni özledim 😆", "Merhaba! Bugün nasıl geçti? 😊")
            Topic.NAME -> "Benim adım ${pet.name}! Sen de benim en sevdiğim insansın 😊"
            Topic.HUNGRY_Q -> if (pet.hunger > 50) "Evet, çok acıktım! Mutfağa gidelim mi? 🍎" else "Hayır, karnım tok! 😌"
            Topic.HOW_ARE_YOU -> when { pet.sick -> "Biraz hastayım… 🤒"; pet.hunger > 70 -> "Acıktım biraz 🥺"; else -> "İyiyim! Sen nasılsın? 😊" }
            Topic.WHAT_DOING -> pick("Seni bekliyordum! 😊", "Ekranında geziyordum! 🏃", "Pilinde asılıydım 🔋😆")
            Topic.PROFANE -> pick("Bip bip! 🙊 Öyle konuşma ama!", "Bip! 😆 Ben o kelimeyi bilmiyorum!")
            else -> w?.let { pick("Aynen, $it! 😆", "$it diyorsun hep, ben de öğrendim! 😄", "Hmm… $it! Anlat bakalım 👀") } ?: pick("Hmm, anlat bakalım! 👀", "Öyle mi? Çok ilginç! 😮", "Seni dinliyorum 😊")
        }
    }
}
