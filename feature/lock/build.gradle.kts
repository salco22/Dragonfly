plugins {
    alias(libs.plugins.dragonfly.android.feature)
}

android {
    namespace = "com.salco.dragonfly.feature.lock"
}

dependencies {
    api(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
}
