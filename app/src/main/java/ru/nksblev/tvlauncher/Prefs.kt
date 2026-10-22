package ru.nksblev.tvlauncher

import android.content.Context
import java.util.UUID

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    fun load(): Settings {
        val d = Settings()
        val mode = sp.getString("wallpaperMode", null)
        return Settings(
            plexUrl = sp.getString("plexUrl", null) ?: d.plexUrl,
            plexToken = sp.getString("plexToken", null) ?: d.plexToken,
            plexMachineId = sp.getString("plexMachineId", null) ?: d.plexMachineId,
            themeId = sp.getString("themeId", null) ?: d.themeId,
            wallpaperMode = WallpaperMode.entries.firstOrNull { it.name == mode } ?: d.wallpaperMode,
            wallpaperUrl = sp.getString("wallpaperUrl", null) ?: d.wallpaperUrl,
            pinned = sp.getString("pinned", null)?.split(",")?.filter { it.isNotBlank() } ?: d.pinned,
            yandexKey = sp.getString("yandexKey", null) ?: d.yandexKey,
            ps5Uri = sp.getString("ps5Uri", null) ?: d.ps5Uri,
        )
    }

    fun save(s: Settings) {
        sp.edit()
            .putString("plexUrl", s.plexUrl)
            .putString("plexToken", s.plexToken)
            .putString("plexMachineId", s.plexMachineId)
            .putString("themeId", s.themeId)
            .putString("wallpaperMode", s.wallpaperMode.name)
            .putString("wallpaperUrl", s.wallpaperUrl)
            .putString("pinned", s.pinned.joinToString(","))
            .putString("yandexKey", s.yandexKey)
            .putString("ps5Uri", s.ps5Uri)
            .apply()
    }

    val clientId: String
        get() {
            val existing = sp.getString("clientId", null)
            if (existing != null) return existing
            val id = UUID.randomUUID().toString()
            sp.edit().putString("clientId", id).apply()
            return id
        }
}
