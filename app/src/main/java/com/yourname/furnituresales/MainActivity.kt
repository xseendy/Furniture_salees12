package com.yourname.furnituresales

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yourname.furnituresales.ui.FurnitureSalesApp
import com.yourname.furnituresales.ui.theme.FurnitureSalesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FurnitureSalesTheme {
                FurnitureSalesApp()
            }
        }
    }
}