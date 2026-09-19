plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)


}


android {
    namespace = "com.example.moneywallpaperfilament"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.moneywallpaperfilament"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    // We use the .filamat extension for materials compiled with matc
    // Telling aapt to not compress them allows to load them efficiently
    aaptOptions {
        noCompress ("filamat", "ktx")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}



dependencies {
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation ("com.google.android.filament:filamat-android:1.75.1")
    implementation(libs.androidx.ui)
    implementation(libs.filament.android)
    implementation(libs.gltfio.android)
    implementation(libs.filament.utils.android)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.window)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}