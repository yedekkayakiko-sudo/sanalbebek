package com.ortakyasam.spike

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Test günlüğü: duvar kağıdı ne zaman görünür oldu, nerede (kilit/ana ekran), dokunma, sayfa kaydırma. */
object DiagLog {
    private const val PREF = "diag"
    private const val KEY = "lines"
    private val fmt = SimpleDateFormat("dd.MM HH:mm:ss", Locale("tr"))

    @Synchronized
    fun add(ctx: Context, line: String) {
        val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val old = (p.getString(KEY, "") ?: "").split('\n').filter { it.isNotBlank() }.takeLast(299)
        p.edit().putString(KEY, (old + "${fmt.format(Date())}  $line").joinToString("\n")).apply()
    }

    fun read(ctx: Context): String =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "")?.takeIf { it.isNotBlank() }
            ?: "Henüz kayıt yok. Duvar kağıdını ayarla, ekranı kilitle ve aç."

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}
