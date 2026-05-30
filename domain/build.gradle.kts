plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.koin.compiler)
}


android {
    namespace = "dev.vikingsen.absclientapp.domain"
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
    implementation(project(":core:model"))
    
    // Coroutines and Core APIs
    implementation(libs.androidx.core.ktx)
    implementation(libs.koin.android)
}
