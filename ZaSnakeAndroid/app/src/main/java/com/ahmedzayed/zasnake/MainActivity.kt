package com.ahmedzayed.zasnake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.ahmedzayed.zasnake.platform.PlayGamesBridge
import com.ahmedzayed.zasnake.ui.ZaSnakeApp

private val ZaGreen = Color(0xFF00FF88)
private val ZaBackground = Color(0xFF050A0E)
private val ZaSurface = Color(0xFF0A1520)

class MainActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by viewModels()
    private lateinit var playGamesBridge: PlayGamesBridge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.rgb(5, 10, 14)
        window.navigationBarColor = android.graphics.Color.rgb(5, 10, 14)
        playGamesBridge = PlayGamesBridge(this)
        setContent {
            ZaSnakeTheme {
                ZaSnakeApp(
                    viewModel = gameViewModel,
                    playGamesBridge = playGamesBridge,
                    onExitApp = { finish() }
                )
            }
        }
    }
}

@Composable
private fun ZaSnakeTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = ZaGreen,
        onPrimary = Color(0xFF042014),
        background = ZaBackground,
        onBackground = Color(0xFFF2F7F4),
        surface = ZaSurface,
        onSurface = Color(0xFFE2EEE7),
        secondary = Color(0xFF00CFFF),
        tertiary = Color(0xFFFFD34E),
        error = Color(0xFFFF526F)
    )
    MaterialTheme(colorScheme = colors, content = content)
}
