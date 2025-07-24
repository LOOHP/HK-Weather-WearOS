/*
 * This file is part of HKWeather.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.loohp.hkweatherwarnings"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.loohp.hkweatherwarnings"
        minSdk = 30
        targetSdk = 35
        versionCode = 149
        versionName = "1.3.8"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)

            freeCompilerArgs.addAll(listOf(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" + project.layout.buildDirectory.asFile.get().absolutePath + "/compose_metrics"))
            freeCompilerArgs.addAll(listOf(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination="  + project.layout.buildDirectory.asFile.get().absolutePath + "/compose_metrics"))
        }
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("com.google.guava:guava:33.4.8-android")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.percentlayout:percentlayout:1.0.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation(platform("androidx.compose:compose-bom:2025.07.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-util")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.2")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.wear.compose:compose-material:1.4.1")
    implementation("androidx.wear.compose:compose-foundation:1.4.1")
    implementation("androidx.wear:wear:1.3.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.wear.protolayout:protolayout-expression:1.3.0")
    implementation("androidx.wear.protolayout:protolayout:1.3.0")
    implementation("androidx.wear.protolayout:protolayout-material:1.3.0")
    implementation("androidx.wear.tiles:tiles:1.5.0")
    implementation("androidx.concurrent:concurrent-futures-ktx:1.3.0")
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.wear:wear-input:1.1.0")
    implementation("androidx.wear:wear-input-testing:1.1.0")
    implementation("androidx.wear:wear-ongoing:1.0.0")
    implementation("androidx.wear:wear-phone-interactions:1.1.0")
    implementation("androidx.wear:wear-remote-interactions:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.10.2")
    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.2.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("me.saket.telephoto:zoomable:0.16.0")
    implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
    implementation("com.google.firebase:firebase-analytics")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.07.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

apply(plugin = "com.google.gms.google-services")