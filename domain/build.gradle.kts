plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.example.domain"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":core"))

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.8.0") // Latest Mockito Core
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0") // Latest Mockito Kotlin
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3") // Coroutines testing
    testImplementation("com.google.truth:truth:1.1.5") // Assertion library
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")


    // Android Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.0")
    androidTestImplementation("androidx.arch.core:core-testing:2.1.0") // For LiveData

    // Mock Web Server for API tests
    testImplementation("com.squareup.okhttp3:mockwebserver:4.10.0")

    // Compose UI Test Manifest
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.0")
    testImplementation("io.mockk:mockk:1.13.5")

    // Hilt
    implementation(libs.hilt.android.v247)
    kapt(libs.hilt.android.compiler.v247)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}