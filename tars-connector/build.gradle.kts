plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.serialization.plugin)
    application
}

application { mainClass.set("br.com.jaquesprojetos.blog.connector.MainKt") }
kotlin {
    jvmToolchain(27)
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":shared-contracts"))
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines)
    runtimeOnly("ch.qos.logback:logback-classic:1.5.6")
}
