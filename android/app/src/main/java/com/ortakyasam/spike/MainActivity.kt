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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Duvar kağıdını ayarlama ve test günlüğünü okuma/paylaşma ekranı. Bilerek sade: bağımlılık yok. */
class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.getBooleanExtra(EXTRA_FROM_WALLPAPER, false) == true) {
            DiagLog.add(this, "MainActivity AÇILDI (duvar kağıdındaki bebeğe dokunarak)")
        }
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }
        root.addView(TextView(this).apply {
            textSize = 15f
            text = "Cihaz testi\n\n1. \"Duvar kağıdı olarak ayarla\"ya bas, \"Ana ekran ve kilit ekranı\"nı seç.\n" +
                "2. Ekranı kilitle, aç: bebek kilit ekranında görünüyor mu, kıpırdıyor mu?\n" +
                "3. Ana ekranda boş alana dokun: bebek oraya yürüyor mu? Bebeğe dokun: bu uygulama açılıyor mu?\n" +
                "4. Sayfa kaydır: bebek kayıyor mu?\n" +
                "5. Günlüğü paylaş ve gönder."
        })
        status = TextView(this).apply { textSize = 14f; setPadding(0, pad / 2, 0, pad / 2) }
        root.addView(status)
        root.addView(button("Duvar kağıdı olarak ayarla") { openPicker() })
        root.addView(button("Günlüğü yenile") { refresh() })
        root.addView(button("Günlüğü paylaş") { share() })
        root.addView(button("Günlüğü temizle") { DiagLog.clear(this); refresh() })
        logView = TextView(this).apply { textSize = 12f; typeface = Typeface.MONOSPACE; setTextIsSelectable(true) }
        root.addView(ScrollView(this).apply { addView(logView) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply { text = label; setOnClickListener { onClick() } }

    private fun refresh() {
        val wm = WallpaperManager.getInstance(this)
        val home = wm.wallpaperInfo?.packageName == packageName
        status.text = "Cihaz: ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
            "Ana ekranda aktif: ${if (home) "evet" else "hayır"} · Kilit ekranında aktif: ${lockInfo(wm)}"
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
