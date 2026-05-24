plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    `maven-publish`
}

android {
    namespace = "io.github.chisarabivorts.tessera.hilt"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    api(project(":tessera"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // --- Unit tests ---
    testImplementation(libs.junit)
}

apply(from = rootProject.file("gradle/publishing.gradle.kts"))
