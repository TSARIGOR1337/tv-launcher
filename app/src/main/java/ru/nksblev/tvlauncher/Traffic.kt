package ru.nksblev.tvlauncher

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Yandex has no public HTTP API for the city traffic score, so we load the official
 * JS API 2.1 (needs the user's own free key) in an invisible WebView and read
 * `traffic.provider.Actual.state.level` (0..10) for Saint Petersburg.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TrafficProbe(apiKey: String, onLevel: (Int) -> Unit) {
    if (apiKey.isBlank()) return
    val callback = rememberUpdatedState(onLevel)
    val html = """
        <html><body style="margin:0;background:#000">
        <div id="m" style="width:480px;height:320px"></div>
        <script src="https://api-maps.yandex.ru/2.1/?apikey=$apiKey&lang=ru_RU"></script>
        <script>
          ymaps.ready(function () {
            var map = new ymaps.Map('m', {center: [59.94, 30.31], zoom: 10, controls: []});
            var p = new ymaps.traffic.provider.Actual({}, {infoLayerShown: false});
            p.setMap(map);
            function report() {
              var l = p.state.get('level');
              if (l !== undefined && l !== null) Android.level(String(l));
            }
            p.state.events.add('change', report);
            setTimeout(report, 4000);
            setInterval(report, 60000);
          });
        </script>
        </body></html>
    """.trimIndent()

    AndroidView(
        modifier = Modifier.size(2.dp).alpha(0f),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(960, 640)
                isFocusable = false
                isFocusableInTouchMode = false
                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun level(v: String) {
                        val n = v.toIntOrNull() ?: return
                        post { callback.value(n) }
                    }
                }, "Android")
                loadDataWithBaseURL("https://localhost/", html, "text/html", "utf-8", null)
            }
        },
    )
}
