plugins {
    alias(libs.plugins.dragonfly.android.library)
    alias(libs.plugins.dragonfly.android.hilt)
}

android {
    namespace = "com.salco.dragonfly.core.work"
}

dependencies {
    implementation(project(":core:data"))
    api(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    api(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
}
