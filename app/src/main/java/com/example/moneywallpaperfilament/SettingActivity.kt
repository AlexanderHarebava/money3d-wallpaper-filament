package com.example.moneywallpaperfilament

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.moneywallpaperfilament.ui.theme.MoneywallpaperfilamentTheme

class SettingsActivity : ComponentActivity() {
    private lateinit var settingsRepo: SettingsRepository
    private var settings by mutableStateOf(WallpaperSettings())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepo = SettingsRepository(this)
        settings = settingsRepo.load()

        setContent {
            MoneywallpaperfilamentTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    SettingsDialog(
                        settings = settings,
                        onSettingsChange = {
                            settings = it
                            settingsRepo.save(it)
                        },
                        onBillCountCommit = { },
                        onReset = {
                            settings = WallpaperSettings()
                            settingsRepo.save(settings)
                        },
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }
}