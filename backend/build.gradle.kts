plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.serialization.plugin)
    application
}

group = "br.com.jaquesprojetos.blog"
version = "1.0.0"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

kotlin {
    jvmToolchain(27)
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(project(":shared-contracts"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.host.common)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation(libs.lettuce)
    implementation(libs.bcprov)
    implementation(libs.jakarta.mail)
    implementation(libs.nimbus.jose.jwt)
    runtimeOnly("ch.qos.logback:logback-classic:1.5.6")
    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
}

tasks.register<JavaExec>("authFlowsPostgresSmoke") {
    group = "verification"
    description = "Runs admin OIDC session and reader login flows against an isolated PostgreSQL database."
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("br.com.jaquesprojetos.blog.backend.AdminAuthStorePostgresSmoke")
}
