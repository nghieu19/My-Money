plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.mymoney"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.mymoney"
        minSdk = 27
        targetSdk = 36
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ✅ Load .env file nếu có
        val envFile = rootProject.file(".env")
        if (envFile.exists()) {
            envFile.readLines().forEach { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size == 2 && !line.startsWith("#")) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    buildConfigField("String", key, "\"$value\"")
                }
            }
        } else {
            buildConfigField("String", "OPENROUTER_API_TOKEN", "\"\"")
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
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

    // --- AndroidX core components ---
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.fragment:fragment:1.8.2")
    implementation("androidx.activity:activity:1.9.1")

    // --- Material Design ---
    implementation("com.google.android.material:material:1.12.0")

    // --- Room Database ---
    implementation(libs.room.runtime)
    implementation(libs.annotations)
    annotationProcessor(libs.room.compiler)

    // --- ML Kit Text Recognition (OCR) ---
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // --- OpenCV ---
    implementation("org.opencv:opencv:4.12.0")

    // --- Retrofit & Gson ---
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // --- Chart Library ---
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // --- Env Config ---
    implementation("io.github.cdimascio:dotenv-java:3.0.0")

    // --- Testing ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
