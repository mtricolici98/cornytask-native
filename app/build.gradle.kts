plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.nobadhabbits.cornytask"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.nobadhabbits.cornytask"
        minSdk = 26
        targetSdk = 36
        versionCode = 16
        versionName = "1.15"

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
        compose = true
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/google-api-client.properties"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("androidx.work:work-runtime-ktx:2.11.0")
    implementation("nl.dionsegijn:konfetti-compose:2.0.2")
    implementation(libs.androidx.compose.runtime.livedata)
    implementation("androidx.room:room-runtime:2.7.0")

    // Kotlin extensions (Coroutines + Flow support)
    implementation("androidx.room:room-ktx:2.7.0")

    // Annotation processor (KSP recommended)
    ksp("androidx.room:room-compiler:2.7.0")

    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    debugImplementation("androidx.glance:glance-appwidget-preview:1.0.0")

    // Glance
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Calendar
    implementation("com.kizitonwose.calendar:compose:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
    implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc13")
    implementation(libs.androidx.foundation)

    // Chart
    implementation("io.github.dautovicharis:charts:2.2.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}