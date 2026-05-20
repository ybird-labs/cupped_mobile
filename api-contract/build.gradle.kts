import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.openapi.generator)
}

val generatedOpenApiDir = layout.buildDirectory.dir("generated/openapi")
val generatedOpenApiSourcesDir = generatedOpenApiDir.map { it.dir("src/commonMain/kotlin") }

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    iosArm64()
    iosX64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            kotlin.srcDir(generatedOpenApiSourcesDir)
            dependencies {
                implementation(libs.kotlinx.serialization.json)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "cafe.cupped.app.api.contract"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

val brewerOpenApiSpec = rootProject.layout.projectDirectory.file("specs/brewer/openapi.json").asFile.absolutePath

openApiValidate {
    inputSpec.set(brewerOpenApiSpec)
}

openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set(brewerOpenApiSpec)
    outputDir.set(generatedOpenApiDir.get().asFile.absolutePath)
    packageName.set("cafe.cupped.app.api.generated")

    globalProperties.set(
        mapOf(
            "models" to "",
            "modelDocs" to "false",
            "modelTests" to "false",
            "apis" to "false",
            "apiDocs" to "false",
            "apiTests" to "false",
            "supportingFiles" to "false"
        )
    )

    additionalProperties.set(
        mapOf(
            "library" to "multiplatform",
            "dateLibrary" to "string",
            "modelMutable" to "false",
            "collectionType" to "list",
            "sourceFolder" to "src/commonMain/kotlin"
        )
    )
}

tasks.named("openApiGenerate") {
    dependsOn(tasks.named("openApiValidate"))
}

tasks.matching { task ->
    task.name.startsWith("compile") && task.name.contains("Kotlin")
}.configureEach {
    dependsOn(tasks.named("openApiGenerate"))
}
