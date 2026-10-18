plugins {
    alias(libs.plugins.dragonfly.android.application)
    alias(libs.plugins.dragonfly.android.application.compose)
    alias(libs.plugins.dragonfly.android.hilt)
}

android {
    namespace = "com.salco.dragonfly"

    defaultConfig {
        applicationId = "com.salco.dragonfly"
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    //Core
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":core:work"))

    //Features
    implementation(project(":feature:accounts"))
    implementation(project(":feature:backup"))
    implementation(project(":feature:bills"))
    implementation(project(":feature:budgets"))
    implementation(project(":feature:classification"))
    implementation(project(":feature:home"))
    implementation(project(":feature:lock"))
    implementation(project(":feature:piggybanks"))
    implementation(project(":feature:rules"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:transactions"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //Compose
    implementation(libs.androidx.activity.compose)

    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)

}
