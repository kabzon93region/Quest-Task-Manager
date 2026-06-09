plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.quest3.taskmanager"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.quest3.taskmanager"
        minSdk = 29
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"
    }

    // Optional release signing: create keystore.properties (see docs/GITHUB_PUBLISH.md)
    // signingConfigs {
    //     create("release") {
    //         val props = java.util.Properties()
    //         val file = rootProject.file("keystore.properties")
    //         if (file.exists()) {
    //             props.load(file.inputStream())
    //             storeFile = file(props["storeFile"] as String)
    //             storePassword = props["storePassword"] as String
    //             keyAlias = props["keyAlias"] as String
    //             keyPassword = props["keyPassword"] as String
    //         }
    //     }
    // }

    buildTypes {
        release {
            isMinifyEnabled = false
            // signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("dev.rikka.shizuku:api:12.2.0")
    implementation("dev.rikka.shizuku:provider:12.2.0")
}
