package ru.nksblev.tvlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
        textTiles[pkg]?.let { return HomeItem.App(pkg, it, null, false) }
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
        val image = try {
            if (banner != null) banner.toBitmap(640, 360).asImageBitmap()
            else pm.getApplicationIcon(ai).toBitmap(192, 192).asImageBitmap()
        } catch (e: Exception) {
            null
        }
        return HomeItem.App(pkg, label, image, banner != null)
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
