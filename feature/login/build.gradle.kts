plugins {
    alias(libs.plugins.dragonfly.android.library)
    alias(libs.plugins.dragonfly.android.library.compose)
    alias(libs.plugins.dragonfly.android.feature)
    alias(libs.plugins.dragonfly.android.hilt)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.salco.login"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.ui.tooling)

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
}