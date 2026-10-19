plugins {
    alias(libs.plugins.dragonfly.android.feature)
}

android {
    namespace = "com.salco.dragonfly.feature.sync"
}

dependencies {
    // Google sign-in through the system account picker.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
}
