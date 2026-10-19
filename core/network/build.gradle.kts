import java.util.Properties

plugins {
    alias(libs.plugins.dragonfly.android.library)
    alias(libs.plugins.dragonfly.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Supabase project address and publishable key, kept out of git in secrets.properties.
val secrets = Properties().apply {
    rootProject.file("secrets.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

android {
    namespace = "com.salco.dragonfly.core.network"
    buildFeatures { buildConfig = true }
    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"${secrets.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${secrets.getProperty("SUPABASE_KEY", "")}\"")
    }
}

dependencies {
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.serialization.json)
}
