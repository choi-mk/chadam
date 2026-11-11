plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.pleosstarter"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.pleosstarter"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // 기능과 에뮬레이터 버전을 확정한 후 활성화하세요. docs/SETUP.md 참고.
    // implementation("ai.pleos.playground:Vehicle:2.3.1")
}

