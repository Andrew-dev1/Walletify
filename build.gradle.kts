// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.gradle) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}

buildscript {
    repositories {
        // Check that you have the following line (if not, add it):
        google()  // Google's Maven repository
        mavenCentral() // Include to import Plaid Link Android SDK
    }

}