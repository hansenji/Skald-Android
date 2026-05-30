plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.koin.compiler)
}


android {
    namespace = "dev.vikingsen.absclientapp.feature.androidauto"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:player"))
    implementation(project(":domain"))

    implementation(libs.media3.common)
    implementation(libs.media3.session)
    implementation(libs.androidx.core.ktx)
    implementation(libs.koin.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

