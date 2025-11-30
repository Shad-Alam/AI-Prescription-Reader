import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.aiprescriptionreader"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.aiprescriptionreader"
        minSdk = 21
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // ML Kit Text Recognition
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")

    // CameraX (for easy camera handling)
    val cameraxVersion = "1.3.0" // Use "val" instead of "def"
    implementation("androidx.camera:camera-camera2:$cameraxVersion") // Use double quotes for string interpolation
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion") // Use double quotes for string interpolation
    implementation("androidx.camera:camera-view:1.3.0")
}
