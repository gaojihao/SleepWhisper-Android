package com.lizhi1026.sleepwhisper.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var app: AppStateContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw under status bar + nav bar; Compose layer handles insets.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { RootRoute(app) }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        app.themeProvider.onSystemConfigurationChanged()
    }
}
