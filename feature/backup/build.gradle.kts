plugins {
    alias(libs.plugins.dragonfly.android.feature)
}

android {
    namespace = "com.salco.dragonfly.feature.backup"
}

dependencies {
    implementation(project(":core:work"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.documentfile)
}
