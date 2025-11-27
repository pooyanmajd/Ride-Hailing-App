import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.veezutech.data"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 33

        val apiKey = gradleLocalProperties(
            rootDir,
            providers = providers,
        ).getProperty("MAPS_API_KEY", "")

        buildConfigField(
            type = "String",
            name = "MAPS_API_KEY",
            value = "\"$apiKey\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(19)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Platform services only (no Compose)
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)

    // Ktor client for Directions API
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(project(":domain"))
    implementation(project(":core:common"))
    testImplementation(project(":domain"))
    testImplementation(project(":core:common"))

    // Hilt + KSP
    implementation(libs.hilt.android)
    ksp(libs.dagger.hilt.compiler)

    // Tests
    testImplementation(libs.junit.junit4)
    testImplementation(libs.io.mockk.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.app.cash.turbine)
}