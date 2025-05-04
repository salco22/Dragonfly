plugins {
    alias(libs.plugins.dragonfly.android.library)
    alias(libs.plugins.dragonfly.android.library.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.dragonfly.android.hilt)
}

android {
    namespace = "com.salco.navigation"
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    api(libs.androidx.navigation.compose)

}