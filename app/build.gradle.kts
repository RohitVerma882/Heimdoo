plugins {
    alias(libs.plugins.application)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.materialthemebuilder)
}

android {
    namespace = "dev.rohitverma882.heimdoo"
    compileSdk = 34
    ndkVersion = "26.2.11394342"

    defaultConfig {
        applicationId = "dev.rohitverma882.heimdoo"
        minSdk = 24
        targetSdk = 34
        versionCode = 13
        versionName = "1.8"
        resourceConfigurations += "en"
        resValue("string", "app_version", "$versionName ($versionCode)")

        externalNativeBuild {
            cmake {
                arguments("-DANDROID_STL=none")
            }
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        prefab = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    materialThemeBuilder {
        themes {
            create("Heimdoo") {
                primaryColor = "#3551B5"

                lightThemeFormat = "Theme.Material3.Light.%s"
                lightThemeParent = "Theme.Material3.Light"
                darkThemeFormat = "Theme.Material3.Dark.%s"
                darkThemeParent = "Theme.Material3.Dark"
            }
        }
        generatePaletteAttributes = true
        generateTextColors = true
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity.ktx)
    implementation(libs.constraintlayout)
    implementation(libs.fragment.ktx)

    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.appiconloader)
    implementation(libs.rikkax.html.ktx)
    implementation(libs.rikkax.cxx)
    implementation(libs.app.update.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}