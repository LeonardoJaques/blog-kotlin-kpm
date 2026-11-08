plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.serialization.plugin)
}

kotlin {
    jvmToolchain(27)
    jvm {
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    js(IR) { browser() }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization)
        }
    }
}
