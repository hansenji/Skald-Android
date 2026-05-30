// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.koin.compiler) apply false
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll("-Xexplicit-backing-fields")
        }
    }

    plugins.withId("io.insert-koin.compiler.plugin") {
        val extension = extensions.findByName("koinCompiler")
        if (extension != null) {
            val setter = extension.javaClass.methods.find { it.name == "setCompileSafety" }
            setter?.invoke(extension, true)
        }
    }
}