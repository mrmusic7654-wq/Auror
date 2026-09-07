plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "aura.orchestrator"
    compileSdk = 34

    defaultConfig {
        applicationId = "aura.orchestrator"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        vectorDrawables { useSupportLibrary = true }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Release keystore is provisioned by CI from secrets so the APK is signed
        // without a private key ever living in the repository.
        create("release") {
            storeFile = file(System.getenv("AURA_KEYSTORE_FILE") ?: "release.jks")
            storePassword = System.getenv("AURA_KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("AURA_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("AURA_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        getByName("release") {
            // Debug + JVM tests pass. Release minification (R8) is kept simple and
            // safe for this build: a signed, unobfuscated release APK is always
            // shippable; R8 rules can be tightened in a later milestone.
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = if (System.getenv("AURA_KEYSTORE_FILE") != null) {
                signingConfigs.getByName("release")
            } else null
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    kapt(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.security.crypto)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
