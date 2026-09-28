package com.blackboxbench.reproduction

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val state = remember {
                AppState(context.getSharedPreferences("nonogram_state", Context.MODE_PRIVATE))
            }
            BenchmarkAppTheme {
                AppRoot(state)
            }
        }
    }
}
