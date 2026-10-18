plugins {
    alias(libs.plugins.dragonfly.android.feature)
}

android {
    namespace = "com.salco.dragonfly.feature.settings"
}

dependencies {
    // Reuses the biometric prompt to confirm the user before turning the lock on.
    implementation(project(":feature:lock"))
}
