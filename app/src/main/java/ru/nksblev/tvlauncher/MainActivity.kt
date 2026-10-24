package ru.nksblev.tvlauncher

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val vm: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                vm.onBack()
            }
        })
        setContent { LauncherRoot(vm) }
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vm.onHomePressed()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            vm.toggleSettings()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun LauncherRoot(vm: HomeViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val hero by vm.hero.collectAsStateWithLifecycle()
    val wallpaper by vm.wallpaper.collectAsStateWithLifecycle()
    LauncherTheme {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (s.settingsOpen) SettingsScreen(s, vm) else HomeScreen(s, hero, wallpaper, vm)
            TrafficProbe(s.settings.yandexKey, vm::onTraffic)
        }
    }
}
