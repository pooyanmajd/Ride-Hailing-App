plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
}
kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)

    testImplementation(libs.junit.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.io.mockk.mockk)
}