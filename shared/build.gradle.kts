@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
}

kotlin {
    androidTarget()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.coroutines.core)
                implementation(libs.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.coroutines.android)
                implementation(libs.serialization.json)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.room.runtime)
                implementation(libs.room.ktx)
                implementation(libs.work.runtime.ktx)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidUnitTest by getting
    }
}

android {
    namespace = "com.beast.shared"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
}
