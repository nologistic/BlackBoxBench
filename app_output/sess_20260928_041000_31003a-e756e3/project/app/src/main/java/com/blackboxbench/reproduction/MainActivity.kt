package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repo.now = LocalDateTime.now()
        Repo.init(applicationContext)
        setContent {
            BenchmarkAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    ReproducedApp()
                }
            }
        }
    }
}

@Composable
fun ReproducedApp() {
    var screen by remember { mutableStateOf(if (Repo.onboarded) "main" else "onboard") }
    var error by remember { mutableStateOf<String?>(null) }
    val shown = error
    if (shown != null) {
        Text(
            shown,
            fontSize = 9.sp,
            color = Color(0xFFB00020),
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        )
        return
    }
    when (screen) {
        "onboard" -> OnboardingScreen(
            onContinue = {
                try {
                    Repo.completeOnboarding()
                } catch (t: Throwable) {
                    error = "completeOnboarding: " + t.stackTraceToString()
                }
                if (error == null) screen = "main"
            },
            onAddAccount = { screen = "account" },
            onImport = { }
        )
        "account" -> AddAccountScreen(onBack = { screen = "onboard" })
        else -> MainScreen()
    }
}
