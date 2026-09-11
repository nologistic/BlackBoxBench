package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { OrganicMapsApp() }
    }

    fun requestNotificationPermission() {
        // Reproduction intentionally uses no dangerous permissions.
    }
}
