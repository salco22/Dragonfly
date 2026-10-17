plugins {
    alias(libs.plugins.dragonfly.android.library)
    alias(libs.plugins.dragonfly.android.library.compose)
}

android {
    namespace = "com.salco.dragonfly.core.designsystem"
}

dependencies {
    api(libs.androidx.ui)
    api(libs.androidx.ui.graphics)
    api(libs.androidx.material3)
    api(libs.androidx.compose.material.icons.extended)
}
