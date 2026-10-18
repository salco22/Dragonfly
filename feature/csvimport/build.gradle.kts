plugins {
    alias(libs.plugins.dragonfly.android.feature)
}

android {
    namespace = "com.salco.dragonfly.feature.csvimport"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}
