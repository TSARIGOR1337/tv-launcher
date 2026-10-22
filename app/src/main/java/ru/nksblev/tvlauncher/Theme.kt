package ru.nksblev.tvlauncher

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

data class ThemeSpec(
    val id: String,
    val name: String,
    val top: Color,
    val bottom: Color,
    val accent: Color,
)

val THEMES = listOf(
    ThemeSpec("graphite", "Графит", Color(0xFF1B2230), Color(0xFF07090D), Color(0xFFFFFFFF)),
    ThemeSpec("midnight", "Полночь", Color(0xFF2A1B4A), Color(0xFF080A1A), Color(0xFFC9B8FF)),
    ThemeSpec("ocean", "Океан", Color(0xFF123A4F), Color(0xFF040C12), Color(0xFF9AD8F5)),
    ThemeSpec("forest", "Лес", Color(0xFF1E3A2F), Color(0xFF06100C), Color(0xFFA6E3C4)),
    ThemeSpec("sunset", "Закат", Color(0xFF4A2320), Color(0xFF120809), Color(0xFFFFBC94)),
)

fun themeById(id: String): ThemeSpec = THEMES.firstOrNull { it.id == id } ?: THEMES[0]

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(), content = content)
}
