import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.koin.compiler) apply false
  alias(libs.plugins.kover)
  alias(libs.plugins.versions)
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<DependencyUpdatesTask>().configureEach {
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

subprojects {
    plugins.withId("com.android.library") {
        apply(plugin = "org.jetbrains.kotlinx.kover")
    }
    plugins.withId("com.android.application") {
        apply(plugin = "org.jetbrains.kotlinx.kover")
    }




    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {

        compilerOptions {
            freeCompilerArgs.addAll("-Xexplicit-backing-fields")
        }
    }

    plugins.withId("io.insert-koin.compiler.plugin") {
        configure<org.koin.compiler.plugin.KoinGradleExtension> {
            compileSafety.set(false)
        }
    }

    plugins.withId("com.android.library") {
        configure<com.android.build.api.dsl.LibraryExtension> {
            lint {
                abortOnError = false
            }
            testOptions {
                unitTests {
                    isIncludeAndroidResources = true
                    isReturnDefaultValues = true
                }
            }
        }
        dependencies {
            add("testImplementation", libs.junit)
            add("testImplementation", libs.robolectric)
            add("testImplementation", libs.mockk)
            add("testImplementation", libs.kotlinx.coroutines.test)
        }
    }

    plugins.withId("com.android.application") {
        configure<com.android.build.api.dsl.ApplicationExtension> {
            lint {
                abortOnError = false
            }
            testOptions {
                unitTests {
                    isIncludeAndroidResources = true
                    isReturnDefaultValues = true
                }
            }
        }
        dependencies {
            add("testImplementation", libs.robolectric)
        }
    }
}