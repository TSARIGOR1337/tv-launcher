package ru.nksblev.tvlauncher

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(s: UiState, hero: HomeItem?, wallpaper: String?, vm: HomeViewModel) {
    val theme = themeById(s.settings.themeId)
    val custom = wallpaper
    val backdrop: String? = when (s.settings.wallpaperMode) {
        WallpaperMode.POSTER -> (hero as? HomeItem.Media)?.backdrop ?: custom
        WallpaperMode.CUSTOM -> custom
        WallpaperMode.THEME -> null
    }
    val firstTile = remember { FocusRequester() }

    Box(Modifier.fillMaxSize()) {
        Backdrop(backdrop, theme)
        Column(Modifier.fillMaxSize().padding(top = 28.dp)) {
            Box(Modifier.padding(horizontal = 56.dp)) { TopBar(s, vm) }
            HeroInfo(hero, Modifier.weight(1f).fillMaxWidth().padding(horizontal = 56.dp))
            ContentRows(s, vm, firstTile, Modifier.fillMaxWidth().height(310.dp))
        }
        s.tileMenu?.let { TileMenu(it, vm) }
    }

    LaunchedEffect(s.pinned.isNotEmpty(), s.focusTick) {
        if (s.pinned.isNotEmpty()) {
            delay(150)
            runCatching { firstTile.requestFocus() }
        }
    }
}

@Composable
private fun Backdrop(url: String?, theme: ThemeSpec) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.top, theme.bottom)))
    ) {
        val context = LocalContext.current
        Crossfade(targetState = url, animationSpec = tween(500), label = "backdrop") { u ->
            if (u != null) {
                AsyncImage(
                    model = remember(u) { ImageRequest.Builder(context).data(u).size(1280, 720).build() },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        // Apple TV–style scrims: keep text and tiles readable over any poster.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x55000000),
                        0.35f to Color(0x11000000),
                        0.6f to Color(0x88000000),
                        1f to Color(0xF2000000),
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(0f to Color(0x99000000), 0.55f to Color.Transparent))
        )
    }
}

@Composable
private fun HeroInfo(item: HomeItem?, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.BottomStart) {
        AnimatedContent(
            targetState = item,
            contentKey = { it?.key },
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
            label = "hero",
        ) { it ->
            if (it != null) {
                Column(Modifier.fillMaxWidth(0.6f).padding(bottom = 12.dp)) {
                    Text(
                        it.title,
                        color = Color.White,
                        fontSize = 44.sp,
                        lineHeight = 52.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val sub = when (it) {
                        is HomeItem.Media -> it.subtitle
                        is HomeItem.Input -> "Игровая приставка · HDMI"
                        is HomeItem.App -> ""
                    }
                    if (sub.isNotBlank()) {
                        Text(sub, color = Color(0xD9FFFFFF), fontSize = 18.sp, lineHeight = 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                    }
                    if (it is HomeItem.Media && it.summary.isNotBlank()) {
                        Text(
                            it.summary,
                            color = Color(0xB3FFFFFF),
                            fontSize = 15.sp,
                            lineHeight = 21.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContentRows(s: UiState, vm: HomeViewModel, firstTile: FocusRequester, modifier: Modifier) {
    CompositionLocalProvider(LocalBringIntoViewSpec provides SmoothScroll) { ContentRowsInner(s, vm, firstTile, modifier) }
}

/**
 * Smooth "pivot" scrolling like Apple TV / Google TV: the focused tile glides to a fixed
 * position near the start edge with an eased animation instead of jumping.
 */
@OptIn(ExperimentalFoundationApi::class)
private val SmoothScroll = object : BringIntoViewSpec {
    @Suppress("OVERRIDE_DEPRECATION")
    override val scrollAnimationSpec: AnimationSpec<Float> = tween(durationMillis = 200, easing = FastOutSlowInEasing)

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val pivot = containerSize * 0.058f
        return offset - pivot
    }
}

@Composable
private fun ContentRowsInner(s: UiState, vm: HomeViewModel, firstTile: FocusRequester, modifier: Modifier) {
    LazyColumn(
        modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {
        item(key = "apps") {
            LazyRow(
                modifier = Modifier.edgeFade(),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                contentPadding = PaddingValues(horizontal = 56.dp, vertical = 16.dp),
            ) {
                itemsIndexed(s.pinned, key = { _, it -> it.key }) { i, item ->
                    val mod = if (i == 0) Modifier.focusRequester(firstTile) else Modifier
                    when (item) {
                        is HomeItem.App -> AppTile(
                            item, mod,
                            onFocus = { vm.onFocus(item) },
                            onClick = { vm.open(item) },
                            onLongClick = { vm.openTileMenu(item) },
                        )

                        is HomeItem.Input -> InputTile(
                            item, s.ps5, mod,
                            onFocus = { vm.onFocus(item) },
                            onClick = { vm.open(item) },
                            onLongClick = { vm.openTileMenu(item) },
                        )

                        is HomeItem.Media -> Unit
                    }
                }
            }
        }
        if (s.onDeck.isNotEmpty()) {
            item(key = "ondeck") { MediaRow("Продолжить просмотр", s.onDeck, wide = true, vm = vm) }
        }
        if (s.recent.isNotEmpty()) {
            item(key = "recent") { MediaRow("Недавно добавлено", s.recent, wide = false, vm = vm) }
        }
    }
}

@Composable
private fun MediaRow(title: String, items: List<HomeItem.Media>, wide: Boolean, vm: HomeViewModel) {
    Column {
        Text(
            title,
            color = Color(0xE6FFFFFF),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 56.dp),
        )
        LazyRow(
            modifier = Modifier.edgeFade(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(horizontal = 56.dp, vertical = 14.dp),
        ) {
            itemsIndexed(items, key = { i, m -> "$i-${m.key}" }) { _, m ->
                MediaTile(m, wide, onFocus = { vm.onFocus(m) }, onClick = { vm.open(m) })
            }
        }
    }
}

/** "Row in haze": the row dissolves towards the left and right screen edges. */
private fun Modifier.edgeFade(): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.035f to Color.Black,
                0.80f to Color.Black,
                1f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

@Composable
private fun TopBar(s: UiState, vm: HomeViewModel) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(10_000)
        }
    }
    val ru = Locale("ru")
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(
                SimpleDateFormat("HH:mm", ru).format(now),
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                SimpleDateFormat("EEEE, d MMMM", ru).format(now).replaceFirstChar { it.uppercase() },
                color = Color(0xB3FFFFFF),
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.weight(1f))
        s.weather?.let { w ->
            val sign = if (w.tempC > 0) "+" else ""
            Chip("$sign${w.tempC}° ${w.description}")
        }
        s.traffic?.let { t -> Chip("Пробки $t", dot = trafficColor(t)) }
        Chip(
            if (s.vpnOn) "VPN" else "VPN выкл",
            dot = if (s.vpnOn) Color(0xFF4ADE80) else Color(0xFF6B7280),
            onClick = { vm.toggleVpn() },
        )
        Chip("PS5 · ${ps5Label(s.ps5)}", dot = ps5Color(s.ps5), onClick = { vm.openPs5() })
        Chip("Настройки", onClick = { vm.openSettings() })
    }
}

fun trafficColor(level: Int): Color = when {
    level <= 3 -> Color(0xFF4ADE80)
    level <= 6 -> Color(0xFFF5B942)
    else -> Color(0xFFEF4444)
}

@Composable
private fun Chip(text: String, dot: Color? = null, onClick: (() -> Unit)? = null) {
    Spacer(Modifier.width(10.dp))
    val content: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            if (dot != null) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, fontSize = 14.sp)
        }
    }
    val pill = RoundedCornerShape(50)
    if (onClick == null) {
        Box(Modifier.clip(pill).background(Color(0x33FFFFFF))) {
            CompositionLocalProvider(LocalContentColor provides Color.White) { content() }
        }
    } else {
        Surface(
            onClick = onClick,
            shape = ClickableSurfaceDefaults.shape(shape = pill),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color(0x33FFFFFF),
                contentColor = Color.White,
                focusedContainerColor = Color.White,
                focusedContentColor = Color.Black,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        ) {
            content()
        }
    }
}

@Composable
private fun TileMenu(item: HomeItem, vm: HomeViewModel) {
    val first = remember { FocusRequester() }
    Dialog(onDismissRequest = { vm.closeTileMenu() }) {
        Column(
            Modifier
                .width(380.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1B2028))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(item.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { vm.moveTile(item.key, -1) },
                modifier = Modifier.fillMaxWidth().focusRequester(first),
            ) { Text("← Переместить влево") }
            Button(
                onClick = { vm.moveTile(item.key, 1) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Переместить вправо →") }
            Button(
                onClick = { vm.unpin(item.key) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Убрать с главного экрана") }
        }
        LaunchedEffect(Unit) {
            delay(80)
            runCatching { first.requestFocus() }
        }
    }
}
