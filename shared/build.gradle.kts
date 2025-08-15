
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    id("app.cash.sqldelight") version "2.0.1"
    id("io.mockative") version "3.0.1"

}

kotlin {

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization)
            implementation("io.ktor:ktor-client-core:2.3.4")
            implementation("io.ktor:ktor-client-cio:2.3.4") // Or android engine
            implementation("io.ktor:ktor-client-logging:2.3.4")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.4")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
            implementation("app.cash.sqldelight:runtime:2.0.1")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
            implementation("io.ktor:ktor-client-mock:2.3.4")
            implementation("app.cash.turbine:turbine:0.12.1")
            implementation("org.xerial:sqlite-jdbc:3.36.0.3")
            implementation("io.mockative:mockative:3.0.1")//
        }
        androidNativeTest.dependencies {
            implementation("junit:junit:4.13.2")
            implementation("io.mockk:mockk-common:1.13.10")
        }
        androidMain.dependencies {
            implementation("app.cash.sqldelight:android-driver:2.0.1")
            implementation("androidx.datastore:datastore-preferences:1.0.0")
        }
        jvmTest.dependencies {
            implementation("io.mockative:mockative:3.0.1")
        }

    }
}


android {
    namespace = "dev.drivemode.techtest"
    compileSdk = 34
    defaultConfig {
        minSdk = 31
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

