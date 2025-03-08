// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories{
        google()
        mavenCentral()
        maven("https://www.jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.22")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.8.6")
        classpath("com.google.gms:google-services:4.3.15")
    }

}


plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}