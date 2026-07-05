plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.course.imchat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.course.imchat"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "WS_URL", "\"ws://10.0.2.2:8080\"")
    }

    signingConfigs {
        create("release") {
            storeFile = project.findProperty("RELEASE_STORE_FILE")?.let { file(it) }
            storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as? String ?: ""
            keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as? String ?: ""
            keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as? String ?: ""
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "WS_URL", "\"ws://127.0.0.1:8080\"")
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("String", "WS_URL", "\"wss://your-server.com\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // ── Compose UI ──────────────────────────────────
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.material3)
    implementation(libs.material.icons.extended)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // ── Lifecycle ───────────────────────────────────
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)

    // ── Network ─────────────────────────────────────
    implementation(libs.okhttp)

    // ── Serialization (kotlinx replaces Gson gradually) ──
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.gson) // TODO: remove after full migration

    // ── Image loading ───────────────────────────────
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // ── Theme ───────────────────────────────────────
    implementation(libs.materialkolor)

    // ── Local storage ───────────────────────────────
    implementation(libs.preference.ktx)
}
