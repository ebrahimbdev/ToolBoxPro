plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

val versionProps = java.util.Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}
val appVersionName: String = versionProps.getProperty("VERSION_NAME", "1.0.0")
val appVersionCode: Int = versionProps.getProperty("VERSION_CODE", "1").toInt()

android {
    namespace = "com.toolbox.pro"
    compileSdk = 35
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.toolbox.pro"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        // AdMob Test App ID
        manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += listOf(
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*",
                "/META-INF/INDEX.LIST",
                "/META-INF/io.netty.versions.properties",
                "org/fusesource/jansi/**",
                "**/*.dll",
                "**/*.dylib",
                "**/*.pdb",
                "**/DebugSymbols.proto"
            )
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.animation)
    debugImplementation(libs.androidx.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    // DataStore
    implementation(libs.datastore.preferences)

    // Ktor
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.kotlinx.json)

    // QR Code (zxing only — unused coil/mlkit/qrose removed for size & startup speed)
    implementation(libs.zxing.core)

    // TODO: Add AdMob, Billing, Firebase when Google Maven is accessible
    // implementation(libs.gms.ads)
    // implementation(libs.billing)
    // implementation(platform(libs.firebase.bom))
    // implementation(libs.firebase.analytics)
    // implementation(libs.firebase.crashlytics)
}
