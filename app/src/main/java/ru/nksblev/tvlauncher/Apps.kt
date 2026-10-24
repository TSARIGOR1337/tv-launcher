package ru.nksblev.tvlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

class AppsRepository(private val ctx: Context) {
    private val pm: PackageManager = ctx.packageManager

    /** Apps whose system banner looks bad: show a clean text tile instead. */
    private val textTiles = mapOf(
        "com.yandex.tv.home" to "Яндекс",
    )

    /** Brand colours for tiles whose banner doesn't give a usable colour. */
    private val brandColors = mapOf(
        "com.yandex.tv.home" to 0xFFFC3F1D.toInt(),
        "org.smarttube.stable" to 0xFF2B2B2B.toInt(),
    )

    fun allApps(): List<AppInfo> {
        val result = LinkedHashMap<String, AppInfo>()
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (ri in pm.queryIntentActivities(intent, 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg == ctx.packageName || result.containsKey(pkg)) continue
                result[pkg] = AppInfo(pkg, textTiles[pkg] ?: ri.loadLabel(pm).toString())
            }
        }
        return result.values.sortedBy { it.label.lowercase() }
    }

    fun appItem(pkg: String): HomeItem.App? {
        val ai = try {
            pm.getApplicationInfo(pkg, 0)
        } catch (e: Exception) {
            return null
        }
        textTiles[pkg]?.let { return HomeItem.App(pkg, it, null, false, brandColors[pkg]) }
        val label = pm.getApplicationLabel(ai).toString()
        var banner: Drawable? = try {
            pm.getApplicationBanner(ai)
        } catch (e: Exception) {
            null
        }
        if (banner == null) {
            val li = pm.getLeanbackLaunchIntentForPackage(pkg)
            if (li != null) banner = try {
                pm.getActivityBanner(li)
            } catch (e: Exception) {
                null
            }
        }
        val bitmap = try {
            if (banner != null) banner.toBitmap(640, 360)
            else pm.getApplicationIcon(ai).toBitmap(192, 192)
        } catch (e: Exception) {
            null
        }
        val color = brandColors[pkg] ?: bitmap?.let { edgeColor(it) }
        return HomeItem.App(pkg, label, bitmap?.asImageBitmap(), banner != null, color)
    }

    /**
     * Average colour of the opaque pixels along the image border — for a banner that is
     * its background colour. Returns null if the border is mostly transparent.
     */
    private fun edgeColor(b: Bitmap): Int? {
        var r = 0L; var g = 0L; var bl = 0L; var n = 0; var total = 0
        fun take(x: Int, y: Int) {
            total++
            val c = b.getPixel(x, y)
            if (Color.alpha(c) < 200) return
            r += Color.red(c); g += Color.green(c); bl += Color.blue(c); n++
        }
        val stepX = maxOf(1, b.width / 40)
        val stepY = maxOf(1, b.height / 24)
        for (x in 0 until b.width step stepX) { take(x, 1); take(x, b.height - 2) }
        for (y in 0 until b.height step stepY) { take(1, y); take(b.width - 2, y) }
        if (total == 0 || n < total / 2) return null
        return Color.rgb((r / n).toInt(), (g / n).toInt(), (bl / n).toInt())
    }
}

fun launchPackage(ctx: Context, pkg: String): Boolean {
    val pm = ctx.packageManager
    val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg) ?: return false
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        ctx.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }
}

fun launchUri(ctx: Context, uri: String): Boolean = try {
    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (e: Exception) {
    false
}
