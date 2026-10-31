plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "id.pbs.iptv"
    compileSdk = 35
    defaultConfig {
        applicationId = "id.pbs.iptv"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    signingConfigs {
        create("releaseKey") {
            val keyPath = providers.gradleProperty("IPTV_KEYSTORE_PATH").orNull
            if (!keyPath.isNullOrEmpty()) {
                storeFile = file(keyPath)
                storePassword = providers.gradleProperty("IPTV_STORE_PASSWORD").orNull
                keyAlias = providers.gradleProperty("IPTV_KEY_ALIAS").orNull
                keyPassword = providers.gradleProperty("IPTV_KEY_PASSWORD").orNull
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (providers.gradleProperty("IPTV_KEYSTORE_PATH").isPresent) signingConfig = signingConfigs.getByName("releaseKey")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
