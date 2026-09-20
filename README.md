**Money 3D** is an Android application featuring interactive 3D Live Wallpapers with floating and falling banknotes. Rendered using the Google Filament engine.

## Features

- **High-Performance 3D Renderer**: Powered by the Google Filament engine for realistic lighting and materials.
- **Dynamic Animations**: Falling, swaying, bending, and rotating bill effects.
- **Parallax Effect**: 3D spatial depth responsive to device movement.
- **Full Customization**: Adjust bill count, fall speed, sway/rotation intensity, sun settings, and parallax effect.
- **UI**: Preview and settings interface built with **Jetpack Compose**, supporting English and Russian localizations.

---

## Project Structure

```text
moneywallpaperfilament
└── app
    └── src
        └── main
            ├── assets
            │   └── materials
            │       ├── bill.mat          # Banknote shader material (GLSL/Filament)
            │       └── sun_skybox.mat    # Sun and skybox shader material
            ├── java/com/example/moneywallpaperfilament
            │   ├── ui/theme              
            │   ├── BillField.kt          # Banknote array geometry generation
            │   ├── IblLoader.kt          # Environment map loader
            │   ├── Io.kt                 # Data reading utilities
            │   ├── MainActivity.kt       # Main preview screen with UI
            │   ├── MeshLoader.kt         # .filamesh 3D mesh loader
            │   ├── MoneyLiveWallpaper.kt # Android Live Wallpaper service
            │   ├── ParallaxController.kt # Gyroscope processing for 3D parallax
            │   ├── SettingActivity.kt    # Dedicated settings screen
            │   ├── SettingsRepository.kt # Parameter persistence
            │   ├── SettingsScreen.kt     # Settings UI (Jetpack Compose)
            │   └── SunSkybox.kt          # Procedural sun rendering component

```

---

##  Filament Shader Compilation

 `.mat` material files must be precompiled into `.filamat` format using Filament's `matc` tool before running the application:

```bash
# Example material compilation command
\filament\bin\matc.exe -p mobile -a opengl -o app\src\main\assets\materials\sun_skybox.filamat app\src\main\assets\materials\sun_skybox.mat
```

## Preview

[Screen_recording_20260919_171844.webm](https://github.com/user-attachments/assets/eb2e5c9e-bbaa-434c-856d-8bbc5da4cf25)


