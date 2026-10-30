package ir.smsforward

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        applyTheme(Prefs(this).themeMode)
    }

    companion object {
        /** 0=خودکار (طبق سیستم)، 1=روشن، 2=تاریک */
        fun applyTheme(mode: Int) = AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                1 -> AppCompatDelegate.MODE_NIGHT_NO
                2 -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}
