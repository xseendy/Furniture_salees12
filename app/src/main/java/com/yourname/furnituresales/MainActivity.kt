package com.yourname.furnituresales

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.yourname.furnituresales.ui.FurnitureSalesApp
import com.yourname.furnituresales.ui.theme.FurnitureSalesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs = remember { getSharedPreferences("app_settings", MODE_PRIVATE) }
            var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("dark_theme", true)) }

            FurnitureSalesTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                FurnitureSalesApp(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { enabled ->
                        isDarkTheme = enabled
                        prefs.edit().putBoolean("dark_theme", enabled).apply()
                    }
                )
            }
        }
    }
}