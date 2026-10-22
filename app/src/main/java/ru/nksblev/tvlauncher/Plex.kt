package ru.nksblev.tvlauncher

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class PlexRepository(private val clientId: String) {

    data class Server(val url: String, val token: String, val machineId: String, val name: String)

    private fun headers(token: String? = null): Map<String, String> {
        val h = linkedMapOf(
            "Accept" to "application/json",
            "X-Plex-Product" to "TV Launcher",
            "X-Plex-Version" to "0.1",
            "X-Plex-Client-Identifier" to clientId,
            "X-Plex-Device-Name" to "Tuvio TV",
            "X-Plex-Platform" to "Android",
        )
        if (!token.isNullOrBlank()) h["X-Plex-Token"] = token
        return h
    }

    // ---- Linking via plex.tv/link (no typing of tokens on the TV) ----

    fun createPin(): PlexLink {
        val j = JSONObject(Net.request("https://plex.tv/api/v2/pins?strong=false", "POST", headers()))
        return PlexLink(j.getString("code"), j.getLong("id"))
    }

    fun checkPin(id: Long): String? {
        val j = JSONObject(Net.request("https://plex.tv/api/v2/pins/$id", headers = headers()))
        return j.str("authToken")
    }

    fun findServer(userToken: String): Server? {
        val arr = JSONArray(
            Net.request(
                "https://plex.tv/api/v2/resources?includeHttps=0&includeRelay=0",
                headers = headers(userToken),
            )
        )
        for (i in 0 until arr.length()) {
            val r = arr.getJSONObject(i)
            if (r.str("provides")?.contains("server") != true) continue
            val conns = r.optJSONArray("connections") ?: continue
            var url: String? = null
            for (k in 0 until conns.length()) {
                val c = conns.getJSONObject(k)
                if (c.optBoolean("local") && !c.optBoolean("relay")) {
                    val address = c.str("address") ?: continue
                    url = "http://$address:${c.optInt("port", 32400)}"
                    break
                }
            }
            if (url == null && conns.length() > 0) url = conns.getJSONObject(0).str("uri")
            if (url == null) continue
            return Server(
                url = url,
                token = r.str("accessToken") ?: userToken,
                machineId = r.str("clientIdentifier") ?: "",
                name = r.str("name") ?: "Plex",
            )
        }
        return null
    }

    fun machineId(s: Settings): String {
        val j = JSONObject(Net.request("${s.plexUrl.trimEnd('/')}/identity", headers = headers(s.plexToken)))
        return j.optJSONObject("MediaContainer")?.str("machineIdentifier") ?: ""
    }

    // ---- Library ----

    fun onDeck(s: Settings): List<HomeItem.Media> = items(s, "/library/onDeck")

    fun recent(s: Settings): List<HomeItem.Media> =
        items(s, "/library/recentlyAdded?X-Plex-Container-Start=0&X-Plex-Container-Size=30")

    private fun items(s: Settings, path: String): List<HomeItem.Media> {
        val base = s.plexUrl.trimEnd('/')
        val j = JSONObject(Net.request(base + path, headers = headers(s.plexToken)))
        val arr = j.optJSONObject("MediaContainer")?.optJSONArray("Metadata") ?: return emptyList()
        val out = ArrayList<HomeItem.Media>(arr.length())
        for (i in 0 until arr.length()) {
            out += parse(arr.getJSONObject(i), base, s.plexToken)
        }
        return out
    }

    private fun parse(m: JSONObject, base: String, token: String): HomeItem.Media {
        val type = m.str("type") ?: ""
        val title = when (type) {
            "episode" -> m.str("grandparentTitle")
            "season" -> m.str("parentTitle")
            else -> m.str("title")
        } ?: ""
        val subtitle = when (type) {
            "episode" -> "Сезон ${m.optInt("parentIndex")}, серия ${m.optInt("index")} · ${m.str("title") ?: ""}"
            "season" -> m.str("title") ?: ""
            "movie" -> listOfNotNull(m.str("year"), m.str("contentRating")).joinToString(" · ")
            else -> m.str("year") ?: ""
        }
        val posterPath = when (type) {
            "episode" -> m.str("grandparentThumb")
            "season" -> m.str("parentThumb") ?: m.str("thumb")
            else -> m.str("thumb")
        }
        val thumbPath = when (type) {
            "episode" -> m.str("thumb")
            else -> m.str("art") ?: m.str("thumb")
        }
        val artPath = m.str("art") ?: m.str("grandparentArt") ?: m.str("parentArt")
        val duration = m.optLong("duration")
        val offset = m.optLong("viewOffset")
        return HomeItem.Media(
            ratingKey = m.str("ratingKey") ?: "",
            title = title,
            subtitle = subtitle,
            summary = m.str("summary") ?: "",
            thumb = thumbPath?.let { image(base, token, it, 480, 270) },
            poster = posterPath?.let { image(base, token, it, 300, 450) },
            backdrop = artPath?.let { image(base, token, it, 1280, 720) },
            progress = if (duration > 0 && offset > 0) (offset.toFloat() / duration).coerceIn(0f, 1f) else null,
        )
    }

    private fun image(base: String, token: String, path: String, w: Int, h: Int): String =
        "$base/photo/:/transcode?width=$w&height=$h&minSize=1&upscale=1" +
            "&url=${URLEncoder.encode(path, "UTF-8")}&X-Plex-Token=$token"
}

internal fun JSONObject.str(key: String): String? {
    if (!has(key) || isNull(key)) return null
    val v = optString(key, "")
    return v.takeIf { it.isNotBlank() }
}

fun openInPlex(ctx: Context, machineId: String, ratingKey: String) {
    if (machineId.isNotBlank() && ratingKey.isNotBlank()) {
        val candidates = listOf(
            "plex://server://$machineId/com.plexapp.plugins.library/library/metadata/$ratingKey",
            "https://app.plex.tv/desktop#!/server/$machineId/details?key=%2Flibrary%2Fmetadata%2F$ratingKey",
        )
        for (u in candidates) {
            try {
                ctx.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(u))
                        .setPackage("com.plexapp.android")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return
            } catch (e: Exception) {
                // try next
            }
        }
    }
    launchPackage(ctx, "com.plexapp.android")
}
