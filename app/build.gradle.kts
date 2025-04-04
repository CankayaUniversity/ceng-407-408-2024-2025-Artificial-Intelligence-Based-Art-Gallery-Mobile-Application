
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.navigation.safeargs.kotlin)

    id("com.google.gms.google-services")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    // "kotlin-android" is redundant when using libs.plugins.kotlin.android
}

android {
    namespace = "com.example.socialmediaapp"
    // Consider using compileSdk = 34 until Android 15 SDK (35) is officially released
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.socialmediaapp"
        minSdk = 24
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        dataBinding = true
        compose = true
    }
}

dependencies {
    // Core dependencies
    implementation("androidx.core:core-ktx:1.12.0")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.activity)

    // Compose dependencies
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Lifecycle components
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.7.0")
    implementation("androidx.activity:activity-ktx:1.10.0")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.6")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.3"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Coroutines - updated to consistent version
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // UI components
    implementation("com.rengwuxian.materialedittext:library:2.1.4")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.nhaarman.supertooltips:library:3.0.0")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

}