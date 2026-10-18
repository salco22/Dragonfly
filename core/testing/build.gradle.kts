plugins {
    alias(libs.plugins.dragonfly.android.library)
}

android {
    namespace = "com.salco.dragonfly.core.testing"
}

dependencies {
    api(project(":core:data"))
    api(project(":core:model"))
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
