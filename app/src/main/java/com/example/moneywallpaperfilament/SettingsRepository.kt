package com.example.moneywallpaperfilament

import android.content.Context
import android.content.SharedPreferences


data class WallpaperSettings(
    val sunVisible: Boolean = true,
    val sunIntensity: Float = SunConfig.INTENSITY,
    val sunRadiusDeg: Float = SunConfig.RADIUS_DEG,
    val sunGlow: Float = SunConfig.GLOW,
    val billCount: Int = BillConfig.BILL_COUNT,
    val fallSpeed: Float = 1.0f,
    val swayIntensity: Float = 1.0f,
    val spinIntensity: Float = 1.0f,
    val bendIntensity: Float = 1.0f,
    val parallaxEnabled: Boolean = false,
    val parallaxSensitivity: Float = 1.0f,
    val maxFps: Int = 60
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): WallpaperSettings {
        val d = WallpaperSettings()
        return WallpaperSettings(
            sunVisible = prefs.getBoolean(KEY_SUN_VISIBLE, d.sunVisible),
            sunIntensity = prefs.getFloat(KEY_SUN_INTENSITY, d.sunIntensity),
            sunRadiusDeg = prefs.getFloat(KEY_SUN_RADIUS, d.sunRadiusDeg),
            sunGlow = prefs.getFloat(KEY_SUN_GLOW, d.sunGlow),
            billCount = prefs.getInt(KEY_BILL_COUNT, d.billCount),
            fallSpeed = prefs.getFloat(KEY_FALL_SPEED, d.fallSpeed),
            swayIntensity = prefs.getFloat(KEY_SWAY, d.swayIntensity),
            spinIntensity = prefs.getFloat(KEY_SPIN, d.spinIntensity),
            bendIntensity = prefs.getFloat(KEY_BEND, d.bendIntensity),
            parallaxEnabled = prefs.getBoolean(KEY_PARALLAX_ENABLED, d.parallaxEnabled),
            parallaxSensitivity = prefs.getFloat(KEY_PARALLAX_SENS, d.parallaxSensitivity),
            maxFps = prefs.getInt(KEY_MAX_FPS, d.maxFps).coerceIn(15, 120)
        )
    }

    fun save(s: WallpaperSettings) {
        prefs.edit()
            .putBoolean(KEY_SUN_VISIBLE, s.sunVisible)
            .putFloat(KEY_SUN_INTENSITY, s.sunIntensity)
            .putFloat(KEY_SUN_RADIUS, s.sunRadiusDeg)
            .putFloat(KEY_SUN_GLOW, s.sunGlow)
            .putInt(KEY_BILL_COUNT, s.billCount)
            .putFloat(KEY_FALL_SPEED, s.fallSpeed)
            .putFloat(KEY_SWAY, s.swayIntensity)
            .putFloat(KEY_SPIN, s.spinIntensity)
            .putFloat(KEY_BEND, s.bendIntensity)
            .putBoolean(KEY_PARALLAX_ENABLED, s.parallaxEnabled)
            .putFloat(KEY_PARALLAX_SENS, s.parallaxSensitivity)
            .putInt(KEY_MAX_FPS, s.maxFps)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "wallpaper_settings"
        const val KEY_SUN_VISIBLE = "sun_visible"
        const val KEY_SUN_INTENSITY = "sun_intensity"
        const val KEY_SUN_RADIUS = "sun_radius"
        const val KEY_SUN_GLOW = "sun_glow"
        const val KEY_BILL_COUNT = "bill_count"
        const val KEY_FALL_SPEED = "fall_speed"
        const val KEY_SWAY = "sway_intensity"
        const val KEY_SPIN = "spin_intensity"
        const val KEY_BEND = "bend_intensity"
        const val KEY_PARALLAX_ENABLED = "parallax_enabled"
        const val KEY_PARALLAX_SENS = "parallax_sensitivity"
        const val KEY_MAX_FPS = "max_fps"
    }
}