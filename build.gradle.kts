plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.gms.google-services")
}

// Review build only: activate with -PmelowaveReadonlyReview=true.
// The default production build retains its existing drive.file flow.
val melowaveReadonlyReview = providers.gradleProperty("melowaveReadonlyReview")
    .orNull?.equals("true", ignoreCase = true) == true

android {
    namespace = "duc_phi.music"
    compileSdk = 36

    defaultConfig {
        applicationId = "duc_phi.music"
        minSdk = 24
        targetSdk = 36
        versionCode = if (melowaveReadonlyReview) 39 else 38
        versionName = if (melowaveReadonlyReview) "2.1.8-oauth-review" else "2.1.8"

        buildConfigField(
            "boolean",
            "DRIVE_READONLY_REVIEW",
            melowaveReadonlyReview.toString()
        )
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt")
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    compileOptions {
        // Archive readers use java.nio/java.time APIs that are newer than minSdk 24.
        // The NIO desugaring library backports those APIs to older Android versions.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-opt-in=androidx.media3.common.util.UnstableApi"
            )
        }
    }
    packaging {
        // AGP 8.4.0 workaround for Android 16 KB memory pages:
        // compress native .so libraries in generated APKs so they are extracted
        // at install time instead of needing 16 KB ZIP alignment.
        // The 64-bit WebRTC .so files in versionCode 31 already have 16 KB ELF alignment.
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.0.1")

    // Firebase BOM và các dịch vụ Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-auth") // Firebase anonymous auth cho ket noi Internet

    // Jetpack Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // AndroidX Core & Activity
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.gms:play-services-auth:21.6.0")
    implementation("com.google.android.gms:play-services-cast-framework:22.0.0")
    implementation("androidx.fragment:fragment-ktx:1.7.1")
    implementation("androidx.mediarouter:mediarouter:1.7.0")
    // Image loading & Coroutines
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // Media3 (Audio & Video Transformer)
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-transformer:1.3.1")
    implementation("androidx.media3:media3-effect:1.3.1")
    // Google Play Billing: prepare MeloWare Premium (6-month and 12-month prepaid plans)
    implementation("com.android.billingclient:billing:9.1.0")
    // Google Play In-App Updates: kept for mandatory update on app launch
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    // MP3 encoder: Java LAME (LGPL-3.0); provides net.sourceforge.lame.mp3.Lame.
    // Add https://nexus.terrestris.de/repository/public/ to settings.gradle.kts dependencyResolutionManagement.repositories.
    implementation("com.github.nwaldispuehl:java-lame:3.98.4")
    // WebRTC DataChannel - ban stripped, doi package livekit.org.webrtc
    implementation("io.github.webrtc-sdk:android-prefixed-stripped:150.7871.01")
    implementation("com.google.android.gms:play-services-ads:23.0.0")

    // Public Google Drive archives: inspect and extract only the selected audio entry.
    implementation("org.apache.commons:commons-compress:1.28.0")
    // Commons Compress keeps XZ optional; 7Z files normally use LZMA/LZMA2.
    implementation("org.tukaani:xz:1.10")
    implementation("com.github.junrar:junrar:8.1.2")
    runtimeOnly("org.slf4j:slf4j-nop:2.0.17")
}
