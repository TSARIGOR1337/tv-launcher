package ru.nksblev.tvlauncher

import androidx.compose.ui.graphics.ImageBitmap

enum class WallpaperMode { POSTER, THEME, CUSTOM }

enum class Ps5State { UNKNOWN, ON, STANDBY, OFFLINE }

data class Settings(
    val plexUrl: String = "",
    val plexToken: String = "",
    val plexMachineId: String = "",
    val themeId: String = "graphite",
    val wallpaperMode: WallpaperMode = WallpaperMode.POSTER,
    val wallpaperUrl: String = "",
    val pinned: List<String> = DEFAULT_PINNED,
    val yandexKey: String = "",
    val ps5Uri: String = HDMI_INPUTS[1].second,
) {
    companion object {
        val DEFAULT_PINNED = listOf(
            "app:com.plexapp.android",
            "input:ps5",
            "app:org.smarttube.stable",
            "app:com.fgl27.twitch",
            "app:ru.more.play",
            "app:com.yandex.tv.home",
        )

        /** HDMI inputs of the CVTE chassis, as exposed by Yandex TV ("live-tv" passthrough URIs). */
        val HDMI_INPUTS = listOf(
            "HDMI 1" to "live-tv://android.media.tv/passthrough/android.cultraview.tv.service%2F.tif.CtvHdmi1InputService%2FHW5",
            "HDMI 2" to "live-tv://android.media.tv/passthrough/android.cultraview.tv.service%2F.tif.CtvHdmi2InputService%2FHW6",
            "HDMI 3" to "live-tv://android.media.tv/passthrough/android.cultraview.tv.service%2F.tif.CtvHdmi3InputService%2FHW7",
        )
    }
}

data class AppInfo(val pkg: String, val label: String)

sealed class HomeItem {
    abstract val key: String
    abstract val title: String

    data class App(
        val pkg: String,
        override val title: String,
        val image: ImageBitmap?,
        val isBanner: Boolean,
    ) : HomeItem() {
        override val key: String get() = "app:$pkg"
    }

    data class Input(
        val id: String,
        override val title: String,
        val uri: String,
    ) : HomeItem() {
        override val key: String get() = "input:$id"
    }

    data class Media(
        val ratingKey: String,
        override val title: String,
        val subtitle: String,
        val summary: String,
        val thumb: String?,
        val poster: String?,
        val backdrop: String?,
        val progress: Float?,
    ) : HomeItem() {
        override val key: String get() = "plex:$ratingKey"
    }
}

data class Weather(val tempC: Int, val code: Int) {
    val description: String
        get() = when (code) {
            0 -> "Ясно"
            1, 2 -> "Малооблачно"
            3 -> "Пасмурно"
            45, 48 -> "Туман"
            in 51..57 -> "Морось"
            in 61..67 -> "Дождь"
            in 71..77 -> "Снег"
            in 80..82 -> "Ливень"
            in 85..86 -> "Снегопад"
            in 95..99 -> "Гроза"
            else -> ""
        }
}

data class PlexLink(val code: String, val id: Long)

data class UiState(
    val settings: Settings = Settings(),
    val pinned: List<HomeItem> = emptyList(),
    val allApps: List<AppInfo> = emptyList(),
    val onDeck: List<HomeItem.Media> = emptyList(),
    val recent: List<HomeItem.Media> = emptyList(),
    val plexStatus: String = "",
    val plexLink: PlexLink? = null,
    val hero: HomeItem? = null,
    val weather: Weather? = null,
    val traffic: Int? = null,
    val vpnOn: Boolean = false,
    val ps5: Ps5State = Ps5State.UNKNOWN,
    val settingsOpen: Boolean = false,
    val tileMenu: HomeItem? = null,
    val focusTick: Int = 0,
)
