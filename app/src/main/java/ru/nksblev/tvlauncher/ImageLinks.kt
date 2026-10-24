package ru.nksblev.tvlauncher

import org.json.JSONObject
import java.net.URLEncoder

/**
 * Turns "share" links from cloud drives into direct image URLs.
 * A Yandex Disk public link (disk.yandex.ru/i/…, yadi.sk/…) opens an HTML page, not the picture,
 * so we ask the public Disk API for the real download link.
 */
fun resolveImageUrl(url: String): String {
    val u = url.trim()
    val host = runCatching { java.net.URI(u).host.orEmpty().lowercase() }.getOrDefault("")
    return when {
        host.contains("disk.yandex.") || host == "yadi.sk" || host.endsWith(".yadi.sk") -> {
            val api = "https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key=" +
                URLEncoder.encode(u, "UTF-8")
            JSONObject(Net.request(api, headers = mapOf("Accept" to "application/json"))).getString("href")
        }

        host == "drive.google.com" -> {
            val id = Regex("/d/([A-Za-z0-9_-]+)").find(u)?.groupValues?.get(1)
                ?: Regex("[?&]id=([A-Za-z0-9_-]+)").find(u)?.groupValues?.get(1)
            if (id != null) "https://drive.google.com/uc?export=download&id=$id" else u
        }

        else -> u
    }
}
