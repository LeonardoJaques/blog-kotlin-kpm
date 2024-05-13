import com.varabyte.kobweb.gradle.worker.util.configAsKobwebWorker

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kobweb.worker)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
}

group = "br.com.jaquesprojetos.blogmultiplatform.worker"
version = "1.0-SNAPSHOT"

kotlin {
    configAsKobwebWorker()

    sourceSets {
        jsMain.dependencies {
            implementation(libs.kobweb.worker)
        }
    }
}
