plugins {
    alias(libs.plugins.dragonfly.android.library)
    alias(libs.plugins.dragonfly.android.hilt)
    alias(libs.plugins.dragonfly.android.room)
}

android {
    namespace = "com.salco.dragonfly.core.database"
}

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.android)
}
