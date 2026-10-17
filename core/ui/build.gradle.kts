plugins {
    alias(libs.plugins.dragonfly.android.library)
    alias(libs.plugins.dragonfly.android.library.compose)
}

android {
    namespace = "com.salco.dragonfly.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    api(project(":core:model"))
}
