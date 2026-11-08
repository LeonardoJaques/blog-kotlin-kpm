import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication
import org.gradle.api.tasks.Copy
import kotlinx.html.link
import kotlinx.html.script

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.serialization.plugin)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kobweb.application)
    alias(libs.plugins.android.application) apply false

//    alias(libs.plugins.kobwebx.markdown)
}

group = "br.com.jaquesprojetos.blogmultiplatform"
version = "1.0-SNAPSHOT"

kobweb {
    app {
        index {
            description.set("Convergencia Blog — architecture, technology and ideas")
            faviconPath.set("/favicon.svg")
            head.add {
                script {
                    src =
                        "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"
                }
                link {
                    rel = "stylesheet"
                    href = "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css"
                }

                link(rel = "stylesheet", href = "/ljblog/fonts/face.css?v=2")
                link(rel = "stylesheet", href = "/ljblog/vintage-blog.css?v=2")

                script {
                    src =
                        "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.2.0/highlight.min.js"
                }
                link {
                    rel = "stylesheet"
                    href = "/ljblog/github-dark.css"
                }
            }

        }

    }
}

val versionProductionScript by tasks.registering(Copy::class) {
    dependsOn("jsBrowserProductionWebpack")
    outputs.upToDateWhen { false }
    from(layout.buildDirectory.file("kotlin-webpack/js/productionExecutable/blogmultiplatform.js"))
    from(layout.buildDirectory.file("kotlin-webpack/js/productionExecutable/blogmultiplatform.js.map"))
    into(layout.buildDirectory.dir("kotlin-webpack/js/productionExecutable"))
    rename { original -> original.replace("blogmultiplatform", "blogmultiplatform-vintage11") }
}

tasks.named("kobwebExport") {
    dependsOn(versionProductionScript)
    doLast {
        val exportedSite = layout.projectDirectory.dir(".kobweb/site").asFile
        exportedSite.walkTopDown()
            .filter { it.isFile && it.extension == "html" }
            .forEach { html ->
                val original = html.readText()
                val versioned = original.replace(
                    "/ljblog/blogmultiplatform.js",
                    "/ljblog/blogmultiplatform-vintage11.js"
                )
                if (versioned != original) html.writeText(versioned)
            }
    }
}

tasks.register("publishVersionedScript") {
    dependsOn("kobwebExport")
    doLast {
        val versionedScript = layout.buildDirectory.file("kotlin-webpack/js/productionExecutable/blogmultiplatform-vintage11.js").get().asFile
        val versionedMap = layout.buildDirectory.file("kotlin-webpack/js/productionExecutable/blogmultiplatform-vintage11.js.map").get().asFile
        val resourcesDirectory = layout.projectDirectory.dir(".kobweb/site/resources").asFile
        versionedScript.copyTo(resourcesDirectory.resolve("blogmultiplatform-vintage11.js"), overwrite = true)
        versionedMap.copyTo(resourcesDirectory.resolve("blogmultiplatform.js.map"), overwrite = true)
    }
}


kotlin {
    configAsKobwebApplication("blogmultiplatform", includeServer = false)

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(libs.kotlinx.serialization)

        }

        jsMain.dependencies {
            implementation(libs.kotlinx.coroutines)
            implementation(compose.html.core)
            implementation(libs.kobweb.core)
            implementation(libs.kobweb.silk)
            implementation(libs.silk.icons.fa)
            implementation(libs.kotlinx.serialization)
            implementation(project(":shared-contracts"))
            implementation(libs.compose.html.ext)
            implementation(project(":worker"))
//             implementation(libs.kobwebx.markdown)
        }
    }
}


