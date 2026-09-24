package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val model = remember { AppModel(context) }
            var onboarded by remember { mutableStateOf(Store.isOnboarded(context)) }
            VinylApp(
                model = model,
                onboarding = !onboarded,
                finishOnboarding = {
                    Store.setOnboarded(context, true)
                    onboarded = true
                },
            )
        }
    }
}
