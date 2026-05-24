plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

android {
    namespace = "io.github.chisarabivorts.tessera"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Compose: runtime only. Anything UI-specific stays in the consumer.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)

    // Navigation Compose: the library's core dependency.
    api(libs.androidx.navigation.compose)

    // Coroutines for Channel / Flow in Navigator.
    implementation(libs.kotlinx.coroutines.core)

    // --- Unit tests ---
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.navigation.testing)
}

// Apply strict explicit-API mode only to production source sets - tests are
// internal by definition and don't need explicit visibility modifiers.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    if (!name.contains("Test", ignoreCase = true)) {
        compilerOptions.freeCompilerArgs.add("-Xexplicit-api=strict")
    }
}

apply(from = rootProject.file("gradle/publishing.gradle.kts"))
