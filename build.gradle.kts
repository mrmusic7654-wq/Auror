// AURA Orchestrator - root build script.
// Plugins are declared here with `apply false` so module build scripts can
// apply specific versions without requiring re-resolution per module.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.jvm") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    id("org.jetbrains.kotlin.kapt") version "2.0.20" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}
