package ru.nksblev.tvlauncher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Application get() = getApplication()
    private val prefs = Prefs(app)
    private val apps = AppsRepository(app)
    private val plex = PlexRepository(prefs.clientId)

    private val _state = MutableStateFlow(UiState(settings = prefs.load()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var linkJob: Job? = null
    private var focusJob: Job? = null
    private var wallpaperJob: Job? = null

    /** Kept outside [UiState] so moving focus does not recompose the tile rows. */
    private val _hero = MutableStateFlow<HomeItem?>(null)
    val hero: StateFlow<HomeItem?> = _hero.asStateFlow()

    /** Direct image URL for the custom wallpaper (share links are resolved). */
    private val _wallpaper = MutableStateFlow<String?>(null)
    val wallpaper: StateFlow<String?> = _wallpaper.asStateFlow()

    init {
        reloadApps()
        resolveWallpaper(_state.value.settings.wallpaperUrl)
        viewModelScope.launch {
            while (isActive) {
                refreshStatus(); delay(15_000)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                loadWeather(); delay(15 * 60_000L)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                loadPlex(); delay(5 * 60_000L)
            }
        }
    }

    // ---- lifecycle / navigation ----

    fun onResume() {
        reloadApps()
        viewModelScope.launch { loadPlex() }
        viewModelScope.launch { refreshStatus() }
    }

    fun onHomePressed() {
        _state.update { it.copy(settingsOpen = false, tileMenu = null, focusTick = it.focusTick + 1) }
    }

    fun onBack() {
        _state.update {
            when {
                it.tileMenu != null -> it.copy(tileMenu = null)
                it.settingsOpen -> it.copy(settingsOpen = false)
                else -> it
            }
        }
    }

    fun toggleSettings() = _state.update { it.copy(settingsOpen = !it.settingsOpen, tileMenu = null) }
    fun openSettings() = _state.update { it.copy(settingsOpen = true, tileMenu = null) }
    fun closeSettings() = _state.update { it.copy(settingsOpen = false) }

    fun onFocus(item: HomeItem) {
        if (_hero.value?.key == item.key) return
        // Debounce: while the user is scrolling quickly, don't swap posters/text on every step.
        focusJob?.cancel()
        focusJob = viewModelScope.launch {
            delay(if (_hero.value == null) 0 else 120)
            _hero.value = item
        }
    }

    private fun resolveWallpaper(url: String) {
        wallpaperJob?.cancel()
        if (url.isBlank()) {
            _wallpaper.value = null
            return
        }
        wallpaperJob = viewModelScope.launch {
            val direct = withContext(Dispatchers.IO) { runCatching { resolveImageUrl(url) }.getOrNull() }
            _wallpaper.value = direct ?: url
        }
    }

    fun openTileMenu(item: HomeItem) = _state.update { it.copy(tileMenu = item) }
    fun closeTileMenu() = _state.update { it.copy(tileMenu = null) }

    fun open(item: HomeItem) {
        when (item) {
            is HomeItem.App -> launchPackage(ctx, item.pkg)
            is HomeItem.Input -> launchUri(ctx, item.uri)
            is HomeItem.Media -> openInPlex(ctx, _state.value.settings.plexMachineId, item.ratingKey)
        }
    }

    fun openPs5() {
        launchUri(ctx, _state.value.settings.ps5Uri)
    }

    // ---- settings ----

    fun updateSettings(transform: (Settings) -> Settings) {
        val old = _state.value.settings
        val new = transform(old)
        if (new == old) return
        prefs.save(new)
        _state.update { it.copy(settings = new) }
        if (new.pinned != old.pinned || new.ps5Uri != old.ps5Uri) reloadApps()
        if (new.wallpaperUrl != old.wallpaperUrl) resolveWallpaper(new.wallpaperUrl)
        if (new.plexUrl != old.plexUrl || new.plexToken != old.plexToken) {
            viewModelScope.launch { loadPlex() }
        }
    }

    fun togglePin(key: String) = updateSettings { s ->
        s.copy(pinned = if (key in s.pinned) s.pinned - key else s.pinned + key)
    }

    fun unpin(key: String) {
        updateSettings { s -> s.copy(pinned = s.pinned - key) }
        closeTileMenu()
    }

    fun moveTile(key: String, delta: Int) {
        updateSettings { s ->
            val list = s.pinned.toMutableList()
            val from = list.indexOf(key)
            val to = from + delta
            if (from < 0 || to !in list.indices) {
                s
            } else {
                list.removeAt(from)
                list.add(to, key)
                s.copy(pinned = list)
            }
        }
    }

    private fun reloadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val s = _state.value.settings
            val items = s.pinned.mapNotNull { key ->
                when {
                    key.startsWith("app:") -> apps.appItem(key.removePrefix("app:"))
                    key == "input:ps5" -> HomeItem.Input("ps5", "PS5", s.ps5Uri)
                    else -> null
                }
            }
            val all = apps.allApps()
            _state.update { it.copy(pinned = items, allApps = all) }
        }
    }

    // ---- status widgets ----

    private suspend fun refreshStatus() {
        val vpn = Status.vpnOn(ctx)
        val ps5 = Status.ps5(ctx)
        _state.update { it.copy(vpnOn = vpn, ps5 = ps5) }
    }

    fun toggleVpn() {
        Status.setTailscale(ctx, !_state.value.vpnOn)
        viewModelScope.launch {
            delay(3000)
            refreshStatus()
        }
    }

    private suspend fun loadWeather() {
        val w = withContext(Dispatchers.IO) { runCatching { Status.weather() }.getOrNull() }
        if (w != null) _state.update { it.copy(weather = w) }
    }

    fun onTraffic(level: Int) {
        if (_state.value.traffic != level) _state.update { it.copy(traffic = level) }
    }

    // ---- Plex ----

    private suspend fun loadPlex() {
        val s = _state.value.settings
        if (s.plexUrl.isBlank() || s.plexToken.isBlank()) return
        try {
            val result = withContext(Dispatchers.IO) {
                val mid = if (s.plexMachineId.isBlank()) runCatching { plex.machineId(s) }.getOrDefault("") else ""
                Triple(plex.onDeck(s), plex.recent(s), mid)
            }
            if (result.third.isNotBlank()) updateSettings { it.copy(plexMachineId = result.third) }
            _state.update {
                it.copy(onDeck = result.first, recent = result.second, plexStatus = "Подключено: ${s.plexUrl}")
            }
        } catch (e: Exception) {
            _state.update { it.copy(plexStatus = "Не удалось подключиться к Plex (${e.message})") }
        }
    }

    fun startPlexLink() {
        linkJob?.cancel()
        linkJob = viewModelScope.launch {
            try {
                val pin = withContext(Dispatchers.IO) { plex.createPin() }
                _state.update { it.copy(plexLink = pin, plexStatus = "") }
                var token: String? = null
                for (i in 0 until 150) {
                    delay(2000)
                    token = withContext(Dispatchers.IO) { runCatching { plex.checkPin(pin.id) }.getOrNull() }
                    if (token != null) break
                }
                val userToken = token
                if (userToken == null) {
                    _state.update { it.copy(plexLink = null, plexStatus = "Код устарел, попробуй ещё раз") }
                    return@launch
                }
                _state.update { it.copy(plexLink = null, plexStatus = "Вход выполнен, ищу сервер…") }
                val server = withContext(Dispatchers.IO) { plex.findServer(userToken) }
                if (server == null) {
                    updateSettings { it.copy(plexToken = userToken) }
                    _state.update { it.copy(plexStatus = "Сервер не найден — укажи адрес вручную") }
                    return@launch
                }
                updateSettings {
                    it.copy(plexUrl = server.url, plexToken = server.token, plexMachineId = server.machineId)
                }
                _state.update { it.copy(plexStatus = "Сервер: ${server.name} (${server.url})") }
            } catch (e: Exception) {
                _state.update { it.copy(plexLink = null, plexStatus = "Ошибка входа в Plex: ${e.message}") }
            }
        }
    }
}
