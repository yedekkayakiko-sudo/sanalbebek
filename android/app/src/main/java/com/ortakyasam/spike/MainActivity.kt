package com.ortakyasam.spike

import android.app.Activity
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View

/** Ana ekran: Pou tarzı odalar (RoomView). Kurulum ve test ayarları SettingsActivity'de. */
class MainActivity : Activity() {
    private lateinit var room: RoomView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Oda tüm ekranı kaplasın; durum çubuğu ve alt çubuk kadar boşluğu RoomView kendisi bırakır.
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        val pet = Pet(this).also { it.catchUp() }
        room = RoomView(this, pet) { startActivity(Intent(this, SettingsActivity::class.java)) }
        room.setOnApplyWindowInsetsListener { v, insets ->
            @Suppress("DEPRECATION")
            (v as RoomView).setInsets(insets.systemWindowInsetTop.toFloat(), insets.systemWindowInsetBottom.toFloat())
            insets
        }
        setContentView(room)
    }

    override fun onResume() {
        super.onResume()
        val pet = Pet(this).also { it.catchUp() }
        // İlk açılış: önce "Kim olsun?" (bebek mi, evcil hayvan mı?)
        if (!pet.chosen) { startActivity(Intent(this, ChooseActivity::class.java)); return }
        room.reloadPet(pet)
        val wallOn = WallpaperManager.getInstance(this).wallpaperInfo?.packageName == packageName
        room.needsSetup = !wallOn || (pet.overlayOn && !Settings.canDrawOverlays(this))
    }

    override fun onPause() {
        super.onPause()
        if (room.pet.chosen) room.pet.save()
    }
}
