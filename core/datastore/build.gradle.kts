plugins {
    alias(libs.plugins.dragonfly.android.library)
    alias(libs.plugins.dragonfly.android.hilt)
}

android {
    namespace = "com.salco.dragonfly.core.datastore"
}

dependencies {
    api(project(":core:model"))
    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}
