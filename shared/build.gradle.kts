import org.jetbrains.kotlin.gradle.dsl.JvmTarget

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

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
                // Экспортируем Room для потребителей (androidApp), чтобы javac видел RoomDatabase
                api(libs.room.runtime)
                api(libs.room.ktx)
                implementation(libs.work.runtime.ktx)
                implementation(libs.androidx.datastore.preferences)
                implementation(libs.androidx.core.ktx)
            }
        }
        val commonTest by getting {
            dependencies { implementation(kotlin("test")) }
        }
        val androidUnitTest by getting
    }
}

android {
    namespace = "com.beast.shared"
    compileSdk = 35
    defaultConfig { minSdk = 24 }

    // Выровнять Java совместимость для задач Javac (было 1.8)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = false
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
}
