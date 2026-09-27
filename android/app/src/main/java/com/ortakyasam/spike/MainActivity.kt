package com.ortakyasam.spike

import android.app.Activity
import android.app.WallpaperInfo
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

/** Duvar kağıdını ayarlama, karakter ayarları ve test günlüğü ekranı. Bilerek sade: bağımlılık yok. */
class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var logView: TextView
    private lateinit var petInfo: TextView
    private lateinit var pet: Pet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.getBooleanExtra(EXTRA_FROM_WALLPAPER, false) == true) {
            DiagLog.add(this, "MainActivity AÇILDI (duvar kağıdındaki bebeğe dokunarak)")
        }
        pet = Pet(this)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }
        fun title(t: String) = root.addView(TextView(this).apply { text = t; textSize = 17f; setTypeface(null, Typeface.BOLD); setPadding(0, pad, 0, pad / 3) })

        title("1 · Kurulum")
        root.addView(TextView(this).apply {
            textSize = 14f
            text = "Aşağıdaki düğmeye bas, açılan ekranda \"Duvar kağıdı olarak ayarla\"ya dokun, sorarsa \"Ana ekran ve kilit ekranı\"nı seç."
        })
        root.addView(button("Duvar kağıdı olarak ayarla") { openPicker() })
        status = TextView(this).apply { textSize = 13f; setPadding(0, pad / 2, 0, pad / 2) }
        root.addView(status)

        title("2 · Karakter")
        petInfo = TextView(this).apply { textSize = 14f }
        root.addView(petInfo)
        val nameBox = EditText(this).apply { hint = "Adı"; setText(pet.name); setSingleLine() }
        root.addView(nameBox)
        root.addView(button("Adı kaydet") {
            pet.name = nameBox.text.toString().trim().ifEmpty { "Minik" }.take(16); pet.save(); refresh()
            Toast.makeText(this, "Kaydedildi", Toast.LENGTH_SHORT).show()
        })
        root.addView(TextView(this).apply { text = "Dönem (deneme için elle seç):"; textSize = 14f; setPadding(0, pad / 2, 0, 0) })
        for (row in listOf(0..2, 3..5)) {
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (i in row) line.addView(
                Button(this).apply { text = Pet.STAGE_NAMES[i]; textSize = 11f; setOnClickListener { pet.stage = i; pet.save(); refresh() } },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
            )
            root.addView(line)
        }
        root.addView(button("Karnını doyur ve uyandır") { pet.hunger = 10f; pet.energy = 90f; pet.asleep = false; pet.save(); refresh() })
        root.addView(button("Acıktır (kabı denemek için)") { pet.hunger = 80f; pet.asleep = false; pet.save(); refresh() })
        root.addView(button("Yumurtadan yeniden çıksın") { pet.hatched = false; pet.save(); refresh() })
        root.addView(button("Yeni görünüm (saç, göz, kıyafet)") {
            getSharedPreferences("pet", MODE_PRIVATE).edit().putInt("seed", 0).apply(); pet = Pet(this); refresh()
        })

        title("3 · Pil ikonunun yeri")
        root.addView(TextView(this).apply {
            textSize = 14f
            text = "Bebek, ana ekranda pil ikonunun hemen altına dokununca tırmanıp ona asılır. Pil ikonun ekranın neresindeyse çubuğu oraya getir (çoğu telefonda en sağ)."
        })
        val batteryLabel = TextView(this).apply { textSize = 13f }
        root.addView(batteryLabel)
        root.addView(SeekBar(this).apply {
            max = 100; progress = (pet.batteryX * 100).toInt()
            batteryLabel.text = "Pil: soldan %${progress}"
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(b: SeekBar, v: Int, fromUser: Boolean) { batteryLabel.text = "Pil: soldan %$v" }
                override fun onStartTrackingTouch(b: SeekBar) {}
                override fun onStopTrackingTouch(b: SeekBar) { pet.batteryX = b.progress / 100f; pet.save() }
            })
        })

        title("4 · Test günlüğü")
        root.addView(TextView(this).apply {
            textSize = 14f
            text = "Denedikten sonra \"Günlüğü paylaş\"a bas ve bana gönder. Hangi dokunuşun telefona ulaştığını buradan anlıyorum."
        })
        root.addView(button("Günlüğü yenile") { refresh() })
        root.addView(button("Günlüğü paylaş") { share() })
        root.addView(button("Günlüğü temizle") { DiagLog.clear(this); refresh() })
        logView = TextView(this).apply { textSize = 12f; typeface = Typeface.MONOSPACE; setTextIsSelectable(true) }
        root.addView(logView)
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        pet = Pet(this)
        refresh()
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply { text = label; setOnClickListener { onClick() } }

    private fun refresh() {
        val wm = WallpaperManager.getInstance(this)
        val home = wm.wallpaperInfo?.packageName == packageName
        status.text = "Cihaz: ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
            "Ana ekranda aktif: ${if (home) "evet" else "hayır"} · Kilit ekranında aktif: ${lockInfo(wm)}"
        pet.catchUp()
        petInfo.text = "${pet.name} · ${Pet.STAGE_NAMES[pet.stage]} · açlık %${pet.hunger.toInt()} · enerji %${pet.energy.toInt()}" +
            (if (pet.asleep) " · uyuyor" else "") + (if (!pet.hatched) " · henüz yumurtada" else "")
        logView.text = DiagLog.read(this)
    }

    /** Kilit ekranı duvar kağıdı bilgisi Android 14'ten itibaren okunabiliyor; eski sürümlerde "bilinmiyor". */
    private fun lockInfo(wm: WallpaperManager): String = try {
        val m = WallpaperManager::class.java.getMethod("getWallpaperInfo", Int::class.javaPrimitiveType)
        val info = m.invoke(wm, WallpaperManager.FLAG_LOCK) as? WallpaperInfo
        if (info?.packageName == packageName) "evet" else "hayır"
    } catch (e: Exception) {
        "bilinmiyor (gözle kontrol et)"
    }

    private fun openPicker() {
        val direct = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, BebekWallpaperService::class.java))
        try {
            startActivity(direct)
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }

    private fun share() {
        val text = "${status.text}\n\n${DiagLog.read(this)}"
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Test günlüğünü paylaş"))
    }

    companion object {
        const val EXTRA_FROM_WALLPAPER = "from_wallpaper"
    }
}
