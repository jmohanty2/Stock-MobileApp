import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Read API keys from local.properties (git-ignored, never committed).
// CI writes this file from GitHub Secrets before building.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.joelwhissel.personalstocktracker"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.joelwhissel.personalstocktracker"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Available in Java as BuildConfig.FINNHUB_API_KEY and BuildConfig.ALPHA_VANTAGE_API_KEY
        buildConfigField(
            "String", "FINNHUB_API_KEY",
            "\"${localProps.getProperty("FINNHUB_API_KEY", "")}\""
        )
        buildConfigField(
            "String", "ALPHA_VANTAGE_API_KEY",
            "\"${localProps.getProperty("ALPHA_VANTAGE_API_KEY", "")}\""
        )
    }

    buildFeatures {
        buildConfig = true   // generates the BuildConfig class
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }

        release {
            optimization {
                enable = false   // keep off for v1.0.0 (Gson DTOs and Room would need keep rules)
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true   // android.* calls return defaults in JVM unit tests
    }
}

dependencies {
    // AndroidX and UI
    implementation(libs.activity.ktx)
    implementation(libs.annotation)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation(libs.swiperefreshlayout)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.process)

    // Room (Java: annotationProcessor, not kapt/ksp)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    // Background work
    implementation(libs.work.runtime)

    // Chart
    implementation(libs.mpandroidchart)

    // Unit tests (src/test)
    testImplementation(libs.junit)
    testImplementation(libs.core.testing)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockwebserver)

    // Instrumented tests (src/androidTest)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.work.testing)
    androidTestImplementation(libs.core.testing)
}