plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.vikingsen.absclientapp.core.preferences"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.koin.android)
}
