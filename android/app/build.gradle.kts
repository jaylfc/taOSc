plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.taosc.taosc"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.taosc.taosc"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation(libs.security.crypto)
    testImplementation("junit:junit:4.13.2")
    // org.json ships in android.jar as an unimplemented stub, so JSONObject throws
    // "not mocked" in JVM unit tests. Put a real implementation on the unit-test
    // classpath; the device still uses the platform's own org.json at runtime.
    // Deliberately NOT unitTests.returnDefaultValues = true - that would make the
    // stubs return nulls and the parsing tests would pass without parsing anything.
    testImplementation("org.json:json:20231013")
}

tasks.withType<Test> {
    testLogging {
        events("passed", "failed", "skipped")
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = true
    }
}
