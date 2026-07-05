plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
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
            // Signing info read from ~/.gradle/gradle.properties or local.properties
            // DO NOT commit real values. Example properties:
            //   RELEASE_STORE_FILE=imchat.keystore
            //   RELEASE_STORE_PASSWORD=...
            //   RELEASE_KEY_ALIAS=imchat
            //   RELEASE_KEY_PASSWORD=...
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
            // Enable resource shrinking for debug to reduce APK size
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
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    // Using specific icons from extended package for better APK size
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("com.squareup.okhttp3:okhttp:5.4.0")
    implementation("com.google.code.gson:gson:2.14.0")
    
    // SharedPreferences for simple caching
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Debug-only dependencies
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
