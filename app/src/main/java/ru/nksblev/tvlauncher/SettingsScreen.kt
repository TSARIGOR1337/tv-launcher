package ru.nksblev.tvlauncher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import androidx.compose.material3.Text as M3Text

@Composable
fun SettingsScreen(s: UiState, vm: HomeViewModel) {
    val first = remember { FocusRequester() }
    val st = s.settings
    Box(Modifier.fillMaxSize().background(Color(0xFF0B0E13))) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 96.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Настройки", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            }

            // ---- Plex ----
            item { Section("Медиатека Plex") }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { vm.startPlexLink() }, modifier = Modifier.focusRequester(first)) {
                        Text(if (st.plexToken.isBlank()) "Войти в Plex" else "Войти заново")
                    }
                    Spacer(Modifier.width(20.dp))
                    val link = s.plexLink
                    if (link != null) {
                        Column {
                            Text("Открой plex.tv/link на телефоне и введи код:", color = Color(0xCCFFFFFF), fontSize = 16.sp)
                            Text(link.code, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(s.plexStatus, color = Color(0xCCFFFFFF), fontSize = 16.sp)
                    }
                }
            }
            item {
                Field("Адрес сервера Plex (заполняется сам после входа)", st.plexUrl) { v ->
                    vm.updateSettings { it.copy(plexUrl = v.trim()) }
                }
            }

            // ---- PS5 ----
            item { Section("PS5 подключена к") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Settings.HDMI_INPUTS.forEach { (label, uri) ->
                        OptionButton(label, selected = st.ps5Uri == uri) {
                            vm.updateSettings { it.copy(ps5Uri = uri) }
                        }
                    }
                }
            }

            // ---- Theme ----
            item { Section("Тема") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    THEMES.forEach { t ->
                        ThemeSwatch(t, selected = t.id == st.themeId) {
                            vm.updateSettings { it.copy(themeId = t.id) }
                        }
                    }
                }
            }

            // ---- Wallpaper ----
            item { Section("Фон") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OptionButton("Постер выбранного", st.wallpaperMode == WallpaperMode.POSTER) {
                        vm.updateSettings { it.copy(wallpaperMode = WallpaperMode.POSTER) }
                    }
                    OptionButton("Только тема", st.wallpaperMode == WallpaperMode.THEME) {
                        vm.updateSettings { it.copy(wallpaperMode = WallpaperMode.THEME) }
                    }
                    OptionButton("Своя картинка", st.wallpaperMode == WallpaperMode.CUSTOM) {
                        vm.updateSettings { it.copy(wallpaperMode = WallpaperMode.CUSTOM) }
                    }
                }
            }
            item {
                Field("Ссылка на картинку для фона (http/https)", st.wallpaperUrl) { v ->
                    vm.updateSettings { it.copy(wallpaperUrl = v.trim()) }
                }
            }

            // ---- VPN ----
            item { Section("VPN (Tailscale)") }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { vm.toggleVpn() }) {
                        Text(if (s.vpnOn) "Отключить VPN" else "Включить VPN")
                    }
                    Spacer(Modifier.width(20.dp))
                    Text(
                        if (s.vpnOn) "Сейчас: включён" else "Сейчас: выключен",
                        color = Color(0xCCFFFFFF),
                        fontSize = 16.sp,
                    )
                }
            }

            // ---- Traffic ----
            item { Section("Пробки (Яндекс)") }
            item {
                Text(
                    "У Яндекса нет открытого API для балла пробок, поэтому используется официальный " +
                        "JavaScript API Карт. Нужен бесплатный ключ с developer.tech.yandex.ru. Без ключа виджет скрыт.",
                    color = Color(0x99FFFFFF),
                    fontSize = 14.sp,
                )
            }
            item {
                Field("Ключ JavaScript API Яндекс Карт", st.yandexKey) { v ->
                    vm.updateSettings { it.copy(yandexKey = v.trim()) }
                }
            }

            // ---- Pinned apps ----
            item { Section("Приложения на главном экране") }
            item {
                Text(
                    "Нажми на приложение, чтобы добавить или убрать его. Порядок меняется долгим нажатием на плитку.",
                    color = Color(0x99FFFFFF),
                    fontSize = 14.sp,
                )
            }
            item {
                AppToggle("PS5 (HDMI)", "input:ps5" in st.pinned) { vm.togglePin("input:ps5") }
            }
            items(s.allApps, key = { it.pkg }) { app ->
                val key = "app:${app.pkg}"
                AppToggle(app.label, key in st.pinned) { vm.togglePin(key) }
            }

            item {
                Spacer(Modifier.size(12.dp))
                Button(onClick = { vm.closeSettings() }) { Text("Готово") }
            }
        }
    }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { first.requestFocus() }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 18.dp),
    )
}

@Composable
private fun OptionButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(if (selected) "✓  $label" else label)
    }
}

@Composable
private fun AppToggle(label: String, on: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(0.7f),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF161B22),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text(if (on) "На главном ✓" else "—", fontSize = 14.sp)
        }
    }
}

@Composable
private fun ThemeSwatch(t: ThemeSpec, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        border = ClickableSurfaceDefaults.border(
            border = if (selected) Border(BorderStroke(2.dp, t.accent), shape = shape) else Border.None,
            focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = shape),
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = Color.Transparent,
            focusedContentColor = Color.White,
        ),
    ) {
        Column(
            Modifier
                .size(150.dp, 90.dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(t.top, t.bottom)))
                .padding(12.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(t.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (selected) Text("выбрана", color = t.accent, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Field(label: String, value: String, onSave: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onSave(it)
        },
        label = { M3Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(0.7f),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color(0xDDFFFFFF),
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color(0x55FFFFFF),
            focusedLabelColor = Color.White,
            unfocusedLabelColor = Color(0x99FFFFFF),
            cursorColor = Color.White,
        ),
    )
}
