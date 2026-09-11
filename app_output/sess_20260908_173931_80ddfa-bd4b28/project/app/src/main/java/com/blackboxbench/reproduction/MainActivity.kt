package com.blackboxbench.reproduction

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(255, 250, 255)
        window.navigationBarColor = Color.rgb(255, 250, 255)
        setContent {
            BenchmarkAppTheme {
                GalleryApp()
            }
        }
    }
}
