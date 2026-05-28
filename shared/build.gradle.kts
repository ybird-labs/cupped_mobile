import co.touchlab.skie.configuration.FlowInterop
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.skie)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("CuppedDatabase") {
            packageName.set("cafe.cupped.app.db")
            // SQLite dialect >= 3.24 is required for UPSERT (architecture §5/§17).
            dialect("app.cash.sqldelight:sqlite-3-24-dialect:${libs.versions.sqldelight.get()}")
            // Validate .sqm migrations against the schema snapshot under `check`.
            // NOTE: not airtight (research: false-negative history #1541/#4138/#3378) —
            // backed by an explicit migration test in commonTest.
            verifyMigrations.set(true)
            // Generate a numbered .db schema snapshot so migrations can be verified.
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
}

skie {
    features {
        // Disable SKIE Flow wrapping on ViewModel classes so that
        // KMP-ObservableViewModel's KVO/Observable mechanism works
        // directly without SKIE intercepting StateFlow access.
        group("cafe.cupped.app.viewmodel") {
            FlowInterop.Enabled(false)
        }
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosX64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    sourceSets {
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.sqlcipher.android)
        }
        commonMain.dependencies {
            implementation(projects.apiContract)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.napier)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.koin.core)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            api(libs.kmp.observableviewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        // JVM-only SQLite (JDBC) driver for schema/migration tests; runs under
        // :shared:testDebugUnitTest. Not in commonTest because it is JVM-only.
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
    }
}

android {
    namespace = "cafe.cupped.app.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    buildFeatures {
        buildConfig = true
    }
}
