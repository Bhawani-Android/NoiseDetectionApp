plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
}

android {
    // use camelCasing
    namespace = "com.example.noisedetectionapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.noisedetectionapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        // use latest version
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.8" // or your Compose Compiler version
    }
    // Packaging if needed (e.g., excludes for META-INF)
    packagingOptions {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {

    // remove if not required
    implementation (project(":domain"))
    implementation (project(":data"))
    implementation (project(":core"))
    implementation(project(":feature:recording"))

    // Hilt
    implementation (libs.hilt.android)
    kapt (libs.hilt.android.compiler)

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2023.06.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.hilt.navigation.compose)


    implementation(libs.androidx.compose.ui.ui)
    implementation(libs.androidx.compose.material3.material3)
    implementation(libs.androidx.compose.ui.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.ui.tooling)

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Activity Compose
    implementation(libs.androidx.activity.compose.v172)

    implementation (libs.accompanist.permissions)
    implementation (libs.androidx.compose.material3.material32)
    implementation ("androidx.compose.material3:material3-window-size-class:1.3.1")
    implementation("androidx.compose.material:material-icons-extended-android:1.7.5")
    implementation (libs.androidx.material3.adaptive.navigation.suite)

    implementation ("androidx.compose.material3:material3:1.3.1")
    implementation ("androidx.compose.material3:material3-window-size-class:1.3.1")
    implementation ("androidx.compose.material3:material3-adaptive-navigation-suite:1.4.0-alpha06")



    // Core Kotlin
    implementation(libs.kotlin.stdlib)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}