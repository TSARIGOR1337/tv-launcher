package ru.nksblev.tvlauncher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage

private val TileShape = RoundedCornerShape(16.dp)

// "Pseudo-glass": translucent tinted surface + hairline border. No live blur (too heavy for this TV).
private val TileColor = Color(0x24FFFFFF)
private val TileFocusedColor = Color(0xFF2B313B)

@Composable
private fun tileBorder() = CardDefaults.border(
    border = Border(border = BorderStroke(1.dp, Color(0x33FFFFFF)), shape = TileShape),
    focusedBorder = Border(border = BorderStroke(2.dp, Color(0xE6FFFFFF)), shape = TileShape),
)

/** Unfocused tiles are dimmed ("haze"), the focused one is fully visible. */
@Composable
private fun focusAlpha(focused: Boolean, dim: Float = 0.6f): Float =
    animateFloatAsState(if (focused) 1f else dim, tween(180), label = "tileAlpha").value

@Composable
private fun tileColors(native: Color? = null) = CardDefaults.colors(
    containerColor = TileColor,
    // Focused tile becomes fully opaque in the app's own colour.
    focusedContainerColor = native ?: TileFocusedColor,
)

private val PS5_BLUE = Color(0xFF0B3C8C)

val APP_TILE_W = 224.dp
val APP_TILE_H = 126.dp

@Composable
fun AppTile(
    item: HomeItem.App,
    modifier: Modifier,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .size(APP_TILE_W, APP_TILE_H)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            },
        shape = CardDefaults.shape(shape = TileShape),
        scale = CardDefaults.scale(focusedScale = 1.1f),
        border = tileBorder(),
        colors = tileColors(item.color?.let { Color(it) }),
    ) {
        Box(Modifier.fillMaxSize().alpha(focusAlpha(focused)), contentAlignment = Alignment.Center) {
            val img = item.image
            when {
                img != null && item.isBanner -> Image(
                    bitmap = img,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                img != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(bitmap = img, contentDescription = item.title, modifier = Modifier.size(56.dp))
                    Text(
                        item.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp, start = 8.dp, end = 8.dp),
                    )
                }

                else -> Text(item.title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InputTile(
    item: HomeItem.Input,
    ps5: Ps5State,
    modifier: Modifier,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .size(APP_TILE_W, APP_TILE_H)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            },
        shape = CardDefaults.shape(shape = TileShape),
        scale = CardDefaults.scale(focusedScale = 1.1f),
        border = tileBorder(),
        colors = tileColors(PS5_BLUE),
    ) {
        Box(Modifier.fillMaxSize().alpha(focusAlpha(focused)), contentAlignment = Alignment.Center) {
            Text(item.title, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(ps5Color(ps5)))
                Spacer(Modifier.width(6.dp))
                Text(ps5Label(ps5), color = Color(0xB3FFFFFF), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MediaTile(
    m: HomeItem.Media,
    wide: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    val w = if (wide) 256.dp else 140.dp
    val h = if (wide) 144.dp else 210.dp
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.width(w).alpha(focusAlpha(focused, 0.7f))) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .size(w, h)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocus()
                },
            shape = CardDefaults.shape(shape = TileShape),
            scale = CardDefaults.scale(focusedScale = 1.08f),
            border = tileBorder(),
            colors = tileColors(),
        ) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = if (wide) (m.thumb ?: m.backdrop) else (m.poster ?: m.thumb),
                    contentDescription = m.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                val p = m.progress
                if (p != null) {
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color(0x66000000))
                    ) {
                        Box(Modifier.fillMaxWidth(p).fillMaxHeight().background(Color.White))
                    }
                }
            }
        }
        if (wide) {
            Text(
                m.title,
                color = Color(0xCCFFFFFF),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp, start = 2.dp),
            )
        }
    }
}

fun ps5Label(s: Ps5State): String = when (s) {
    Ps5State.ON -> "включена"
    Ps5State.STANDBY -> "режим покоя"
    Ps5State.OFFLINE -> "не в сети"
    Ps5State.UNKNOWN -> "…"
}

fun ps5Color(s: Ps5State): Color = when (s) {
    Ps5State.ON -> Color(0xFF4ADE80)
    Ps5State.STANDBY -> Color(0xFFF5B942)
    Ps5State.OFFLINE -> Color(0xFF6B7280)
    Ps5State.UNKNOWN -> Color(0xFF6B7280)
}
