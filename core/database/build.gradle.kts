plugins {
    alias(libs.plugins.dragonfly.android.library)
    alias(libs.plugins.dragonfly.android.hilt)
    alias(libs.plugins.dragonfly.android.room)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.salco.dragonfly.core.database"
    // Lets MigrationTestHelper read the exported schemas.
    sourceSets { named("androidTest") { assets.directories.add("$projectDir/schemas") } }
}

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.serialization.json)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
