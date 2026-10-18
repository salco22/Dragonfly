package com.salco.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions

const val COMPILE_SDK = 37
const val TARGET_SDK = 37
const val MIN_SDK = 26

internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = COMPILE_SDK

        defaultConfig.minSdk = MIN_SDK

        compileOptions.sourceCompatibility = JavaVersion.VERSION_17
        compileOptions.targetCompatibility = JavaVersion.VERSION_17
    }
    configureKotlin(optInCoroutines = true)
}

/** Shared Kotlin compiler settings; AGP 9 compiles Kotlin itself, so no Kotlin Android plugin is applied. */
internal fun Project.configureKotlin(optInCoroutines: Boolean = false) {
    val warningsAsErrors = findProperty("warningsAsErrors")?.toString().toBoolean()
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        (compilerOptions as? KotlinJvmCompilerOptions)?.jvmTarget?.set(JvmTarget.JVM_17)
        compilerOptions {
            allWarningsAsErrors.set(warningsAsErrors)
            if (optInCoroutines) freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
    }
}
